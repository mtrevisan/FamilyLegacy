/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.Tile;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * Pseudo-3D globe view of the OpenStreetMap tile pyramid.
 * <p>
 * The panel reuses the same {@link DefaultTileFactory} as the 2D
 * {@code JXMapViewer}, so the tile cache is shared and no additional
 * network requests are issued. The projection is an orthographic
 * sphere: the viewer is conceptually at infinity along the +x axis of
 * a coordinate frame rotated so that the current camera center sits at
 * (1, 0, 0).
 * <p>
 * Each 256×256 tile is subdivided into an 8×8 grid of sub-quads. Every
 * sub-quad is projected individually and drawn with an affine transform
 * clipped to its destination parallelogram. At this subdivision level
 * the sphere curvature is visually smooth and the affine approximation
 * per sub-quad is imperceptible.
 */
public final class Pseudo3DGlobePanel extends JPanel{

	private static final int TILE_SIZE = 256;
	private static final int SUBDIVISIONS = 8;
	private static final int MAX_OSM_ZOOM = 7;

	private static final Color BACKGROUND = new Color(5, 10, 25);
	private static final Color LIMB_COLOR = new Color(30, 60, 100, 200);
	private static final Color MARKER_BORDER = new Color(20, 20, 20);
	private static final Color MARKER_LABEL = Color.WHITE;
	private static final Color MARKER_LABEL_SHADOW = new Color(0, 0, 0, 200);
	private static final int MARKER_RADIUS = 5;

	/**
	 * Pixels by which each projected sub-quad is expanded outward from its
	 * centroid before being clipped and drawn. Without this expansion,
	 * antialiased clip edges of adjacent sub-quads meet without overlap,
	 * and the panel background shows through as a thin dark line along
	 * every shared edge. The value is deliberately small: it is enough to
	 * cover the antialiased boundary, and small enough that the resulting
	 * geometric distortion is invisible.
	 */
	private static final double QUAD_EXPANSION_PX = 1.45;


	/**
	 * A marker rendered on the globe. Coordinates are in decimal
	 * degrees. The label may be {@code null} or empty.
	 */
	public record GlobeMarker(double latitude, double longitude, Color color, String label){}


	private final DefaultTileFactory tileFactory;
	private final int totalMapZoom;

	private double centerLatDeg = 45.;
	private double centerLonDeg = 12.;
	private double radiusPx = 300.;
	private final double minRadiusPx = 40.;
	private final double maxRadiusPx = 5000.;

	private Point dragStart;
	private double dragStartLat;
	private double dragStartLon;

	private List<GlobeMarker> markers = List.of();

	/** Tiles already subscribed for "loaded" events, to avoid duplicates. */
	private final Set<Tile> listenedTiles = Collections.newSetFromMap(new WeakHashMap<>());


	public Pseudo3DGlobePanel(final DefaultTileFactory tileFactory){
		this.tileFactory = tileFactory;
		this.totalMapZoom = tileFactory.getInfo()
			.getTotalMapZoom();

		setBackground(BACKGROUND);
		setOpaque(true);
		setPreferredSize(new Dimension(800, 600));
		installListeners();
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public void setCenter(final double latDeg, final double lonDeg){
		this.centerLatDeg = clamp(latDeg, -89.99, 89.99);
		this.centerLonDeg = normalizeLon(lonDeg);

		repaint();
	}

	public void setRadius(final double radiusPx){
		this.radiusPx = clamp(radiusPx, minRadiusPx, maxRadiusPx);

		repaint();
	}

	public void setMarkers(final List<GlobeMarker> markers){
		this.markers = (markers != null? List.copyOf(markers): List.of());

		repaint();
	}

	public double getCenterLat(){
		return centerLatDeg;
	}

	public double getCenterLon(){
		return centerLonDeg;
	}

	public double getRadius(){
		return radiusPx;
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(!(g instanceof Graphics2D g2))
			return;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		final int w = getWidth();
		final int h = getHeight();
		final double cx = w / 2.;
		final double cy = h / 2.;

		// 1. Panel background.
		g2.setColor(BACKGROUND);
		g2.fillRect(0, 0, w, h);

		// 2. Ocean disk. The Web Mercator tile pyramid stops at ±85.0511°,
		//    so the polar caps have no tiles. Filling the visible hemisphere
		//    with the ocean color before drawing the tiles makes the poles
		//    appear as a flat "no data" area instead of showing the dark
		//    panel background through the gaps.
//		g2.setColor(OCEAN_COLOR);
//		g2.fill(new Ellipse2D.Double(cx - radiusPx, cy - radiusPx, 2 * radiusPx, 2 * radiusPx));

		// 3. Pre-compute the rotation constants once per frame.
		final int osmZoom = computeZoomForRadius(radiusPx);
		final double lonc = Math.toRadians(centerLonDeg);
		final double phic = Math.toRadians(centerLatDeg);
		final double cosL = Math.cos(lonc), sinL = Math.sin(lonc);
		final double cosP = Math.cos(phic), sinP = Math.sin(phic);

		// 4. Only iterate over the tiles that can possibly intersect the
		//    visible portion of the globe. At high zoom levels this reduces
		//    the number of iterations from tens of thousands to a few dozen.
		final int[] range = computeVisibleTileRange(osmZoom, w, h);
		final int txMin = range[0];
		final int txMax = range[1];
		final int tyMin = range[2];
		final int tyMax = range[3];

		for(int ty = tyMin; ty <= tyMax; ty ++)
			for(int tx = txMin; tx <= txMax; tx ++)
				drawTile(g2, tx, ty, osmZoom, cosL, sinL, cosP, sinP, cx, cy);

		// 5. Sphere limb: a thin ring at the outer edge.
		g2.setColor(LIMB_COLOR);
		g2.setStroke(new BasicStroke(1.5f));
		g2.draw(new Ellipse2D.Double(cx - radiusPx, cy - radiusPx, 2 * radiusPx, 2 * radiusPx));

		// 6. Markers on top.
		drawMarkers(g2, cosL, sinL, cosP, sinP, cx, cy);
	}


	/* ======================================================================
	 *                          Tile drawing
	 * ====================================================================== */

	private void drawTile(final Graphics2D g2, final int tx, final int ty, final int osmZoom,
			final double cosL, final double sinL, final double cosP, final double sinP,
			final double cx, final double cy){
		final int jxZoom = totalMapZoom - osmZoom;
		final Tile tile = tileFactory.getTile(tx, ty, jxZoom);
		if(tile == null)
			return;

		if(!tile.isLoaded()){
			if(listenedTiles.add(tile))
				tile.addPropertyChangeListener("loaded", evt -> {
					listenedTiles.remove(tile);

					if(Boolean.TRUE.equals(evt.getNewValue()))
						SwingUtilities.invokeLater(this::repaint);
				});
			return;
		}

		final BufferedImage img = tile.getImage();
		if(img == null)
			return;

		final int sub = TILE_SIZE / SUBDIVISIONS;
		final double pxBase = tx * (double)TILE_SIZE;
		final double pyBase = ty * (double)TILE_SIZE;

		for(int j = 0; j < SUBDIVISIONS; j ++)
			for(int i = 0; i < SUBDIVISIONS; i ++){
				final double sx0 = i * (double)sub;
				final double sy0 = j * (double)sub;
				final double sx1 = sx0 + sub;
				final double sy1 = sy0 + sub;

				final double[] llTL = pixelToLatLon(pxBase + sx0, pyBase + sy0, osmZoom);
				final double[] llTR = pixelToLatLon(pxBase + sx1, pyBase + sy0, osmZoom);
				final double[] llBR = pixelToLatLon(pxBase + sx1, pyBase + sy1, osmZoom);
				final double[] llBL = pixelToLatLon(pxBase + sx0, pyBase + sy1, osmZoom);

				final double[] tl = projectToScreen(llTL[0], llTL[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx);
				final double[] tr = projectToScreen(llTR[0], llTR[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx);
				final double[] br = projectToScreen(llBR[0], llBR[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx);
				final double[] bl = projectToScreen(llBL[0], llBL[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx);

				// Count how many corners are on the visible side.
				int visible = 0;
				if(tl != null) visible ++;
				if(tr != null) visible ++;
				if(br != null) visible ++;
				if(bl != null) visible ++;

				// All four corners on the far side: the sub-quad is completely
				// behind the globe and contributes nothing.
				if(visible == 0)
					continue;

				// Some corners are on the far side: project them onto the limb
				// so the visible portion of the sub-quad still reaches the edge
				// of the disk, instead of being dropped entirely.
				final double[] tlF = (tl != null? tl: projectToLimb(llTL[0], llTL[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx));
				final double[] trF = (tr != null? tr: projectToLimb(llTR[0], llTR[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx));
				final double[] brF = (br != null? br: projectToLimb(llBR[0], llBR[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx));
				final double[] blF = (bl != null? bl: projectToLimb(llBL[0], llBL[1], cosL, sinL, cosP, sinP, cx, cy, radiusPx));

				if(tlF == null || trF == null || brF == null || blF == null)
					continue;

				drawTexturedQuad(g2, img, sx0, sy0, sx1, sy1, tlF, trF, brF, blF);
			}
	}

	/**
	 * Draws the sub-rectangle {@code (sx0, sy0)-(sx1, sy1)} of the given
	 * image onto the parallelogram defined by the four projected corners.
	 * <p>
	 * The transform is an affine approximation built from three corners
	 * (top-left, top-right, bottom-left); the fourth corner
	 * (bottom-right) is only used as the clip boundary. With sub-quads
	 * small enough, the residual projective error is invisible.
	 * <p>
	 * The destination quad is expanded outward from its centroid by
	 * {@link #QUAD_EXPANSION_PX} before the transform and the clip are
	 * computed. This makes adjacent sub-quads overlap slightly, so the
	 * antialiased boundaries of neighboring quads do not leave a
	 * background-colored seam between them.
	 */
	private static void drawTexturedQuad(final Graphics2D g2, final BufferedImage img,
		final double sx0, final double sy0, final double sx1, final double sy1,
		final double[] tl, final double[] tr, final double[] br, final double[] bl){
		final double dw = sx1 - sx0;
		final double dh = sy1 - sy0;
		if(dw <= 0. || dh <= 0.)
			return;

		// Expand the destination quad outward from its centroid. The source
		// rectangle is left unchanged, so the image content is scaled up
		// very slightly — imperceptible at the expansion magnitude used here.
		final double[][] quad = expandQuad(tl, tr, br, bl, QUAD_EXPANSION_PX);
		final double[] eTL = quad[0];
		final double[] eTR = quad[1];
		final double[] eBR = quad[2];
		final double[] eBL = quad[3];

		final double m00 = (eTR[0] - eTL[0]) / dw;
		final double m10 = (eTR[1] - eTL[1]) / dw;
		final double m01 = (eBL[0] - eTL[0]) / dh;
		final double m11 = (eBL[1] - eTL[1]) / dh;
		final double b0 = eTL[0] - m00 * sx0 - m01 * sy0;
		final double b1 = eTL[1] - m10 * sx0 - m11 * sy0;

		final AffineTransform at = new AffineTransform(m00, m10, m01, m11, b0, b1);

		final Path2D clipQuad = new Path2D.Double();
		clipQuad.moveTo(eTL[0], eTL[1]);
		clipQuad.lineTo(eTR[0], eTR[1]);
		clipQuad.lineTo(eBR[0], eBR[1]);
		clipQuad.lineTo(eBL[0], eBL[1]);
		clipQuad.closePath();

		final Shape oldClip = g2.getClip();
		final AffineTransform oldTransform = g2.getTransform();
		try{
			g2.clip(clipQuad);
			g2.transform(at);
			g2.drawImage(img,
				(int)sx0, (int)sy0, (int)sx1, (int)sy1,
				(int)sx0, (int)sy0, (int)sx1, (int)sy1,
				null);
		}
		finally{
			g2.setTransform(oldTransform);
			g2.setClip(oldClip);
		}
	}

	/**
	 * Expands a quadrilateral outward from its centroid by the given number
	 * of pixels. Each corner is moved along the ray that connects the
	 * centroid to the corner itself, so the shape stays similar to the
	 * original and no corner crosses another.
	 *
	 * @param tl       top-left corner
	 * @param tr       top-right corner
	 * @param br       bottom-right corner
	 * @param bl       bottom-left corner
	 * @param expandPx expansion distance, in pixels; may be zero
	 * @return the four expanded corners, in the order
	 *         {@code [tl, tr, br, bl]}
	 */
	private static double[][] expandQuad(final double[] tl, final double[] tr,
		final double[] br, final double[] bl, final double expandPx){
		final double cx = (tl[0] + tr[0] + br[0] + bl[0]) / 4.;
		final double cy = (tl[1] + tr[1] + br[1] + bl[1]) / 4.;

		return new double[][]{
			expandCorner(tl, cx, cy, expandPx),
			expandCorner(tr, cx, cy, expandPx),
			expandCorner(br, cx, cy, expandPx),
			expandCorner(bl, cx, cy, expandPx),
		};
	}

	/**
	 * Moves a single corner outward from the given center by the given
	 * number of pixels. When the corner coincides with the center (a
	 * degenerate quad), it is returned unchanged.
	 */
	private static double[] expandCorner(final double[] p, final double cx, final double cy,
		final double expandPx){
		final double dx = p[0] - cx;
		final double dy = p[1] - cy;
		final double len = Math.hypot(dx, dy);
		if(len < 1.e-9)
			return p;

		final double scale = (len + expandPx) / len;
		return new double[]{cx + dx * scale, cy + dy * scale};
	}


	/* ======================================================================
	 *                          Markers
	 * ====================================================================== */

	private void drawMarkers(final Graphics2D g2, final double cosL, final double sinL,
		final double cosP, final double sinP, final double cx, final double cy){
		for(final GlobeMarker m : markers){
			final double[] screen = projectToScreen(m.latitude(), m.longitude(),
				cosL, sinL, cosP, sinP, cx, cy, radiusPx);
			if(screen == null)
				continue;

			final double x = screen[0];
			final double y = screen[1];

			g2.setColor(m.color());
			g2.fill(new Ellipse2D.Double(x - MARKER_RADIUS, y - MARKER_RADIUS,
				MARKER_RADIUS * 2, MARKER_RADIUS * 2));
			g2.setColor(MARKER_BORDER);
			g2.draw(new Ellipse2D.Double(x - MARKER_RADIUS, y - MARKER_RADIUS,
				MARKER_RADIUS * 2, MARKER_RADIUS * 2));

			final String label = m.label();
			if(label != null && !label.isEmpty()){
				g2.setFont(g2.getFont().deriveFont(11f));
				final int tx = (int)x + MARKER_RADIUS + 3;
				final int ty = (int)y + 4;
				g2.setColor(MARKER_LABEL_SHADOW);
				g2.drawString(label, tx + 1, ty + 1);
				g2.setColor(MARKER_LABEL);
				g2.drawString(label, tx, ty);
			}
		}
	}


	/* ======================================================================
	 *                          Projection
	 * ====================================================================== */

	/**
	 * Projects a point on the sphere to screen coordinates, using an
	 * orthographic projection. Returns {@code null} when the point lies
	 * on the far hemisphere.
	 * <p>
	 * The rotation brings the camera center to {@code (1, 0, 0)} in the
	 * sphere frame, so that the visible hemisphere is exactly
	 * {@code x2 > 0}. Screen X is the rotated Y axis, screen Y is the
	 * negated rotated Z axis (so the globe's north is up).
	 */
	private static double[] projectToScreen(final double latDeg, final double lonDeg,
			final double cosL, final double sinL, final double cosP, final double sinP,
			final double cx, final double cy, final double R){
		final double phi = Math.toRadians(latDeg);
		final double lambda = Math.toRadians(lonDeg);
		final double cosPhi = Math.cos(phi);

		final double x = cosPhi * Math.cos(lambda);
		final double y = cosPhi * Math.sin(lambda);
		final double z = Math.sin(phi);

		// Rotation around z by -centerLon, then around y by +centerLat.
		final double x1 = cosL * x + sinL * y;
		final double y1 = -sinL * x + cosL * y;
		final double z1 = z;

		final double x2 = cosP * x1 + sinP * z1;
		final double y2 = y1;
		final double z2 = -sinP * x1 + cosP * z1;

		if(x2 <= 0.)
			return null;

		return new double[]{cx + R * y2, cy - R * z2};
	}

	/**
	 * Projects a point on the far hemisphere onto the visible limb.
	 * <p>
	 * Used by the tile renderer to keep sub-quads that straddle the edge
	 * of the visible disk. When one or more corners of a sub-quad lie on
	 * the far side, projecting them onto the limb preserves the shape of
	 * the visible portion, so the tile reaches the edge of the globe
	 * instead of being dropped.
	 * <p>
	 * The limb is the great circle {@code x2 = 0} in the rotated frame.
	 * The limb point in the direction of the given geographic location is
	 * obtained by normalizing the {@code (y2, z2)} component.
	 *
	 * @return the screen coordinates of the limb point, or {@code null}
	 *         when the direction is degenerate
	 */
	private static double[] projectToLimb(final double latDeg, final double lonDeg,
			final double cosL, final double sinL, final double cosP, final double sinP,
			final double cx, final double cy, final double R){
		final double phi = Math.toRadians(latDeg);
		final double lambda = Math.toRadians(lonDeg);
		final double cosPhi = Math.cos(phi);

		final double x = cosPhi * Math.cos(lambda);
		final double y = cosPhi * Math.sin(lambda);
		final double z = Math.sin(phi);

		final double x1 = cosL * x + sinL * y;
		final double y1 = -sinL * x + cosL * y;
		final double z1 = z;

		final double y2 = y1;
		final double z2 = -sinP * x1 + cosP * z1;

		final double norm = Math.hypot(y2, z2);
		if(norm < 1.e-9)
			return null;

		return new double[]{cx + R * (y2 / norm), cy - R * (z2 / norm)};
	}

	/**
	 * Web Mercator inverse: world-pixel coordinates at the given OSM zoom
	 * level to {@code [latDeg, lonDeg]}.
	 */
	private static double[] pixelToLatLon(final double px, final double py, final int osmZoom){
		final double worldSize = TILE_SIZE * (double)(1 << osmZoom);
		final double lon = (px / worldSize) * 360. - 180.;
		final double n = Math.PI - 2. * Math.PI * py / worldSize;
		final double lat = Math.toDegrees(Math.atan(Math.sinh(n)));
		return new double[]{lat, lon};
	}

	/**
	 * Converts a longitude to a tile column index at the given OSM zoom.
	 */
	private static int lonToTileX(final double lon, final int osmZoom){
		final double worldSize = TILE_SIZE * (double)(1 << osmZoom);
		final double px = (lon + 180.) / 360. * worldSize;
		return (int)Math.floor(px / TILE_SIZE);
	}

	/**
	 * Converts a latitude to a tile row index at the given OSM zoom.
	 * Higher latitude means lower row index (tiles are indexed top-down).
	 * The latitude is clamped to the Web Mercator valid range.
	 */
	private static int latToTileY(final double lat, final int osmZoom){
		final double worldSize = TILE_SIZE * (double)(1 << osmZoom);
		final double latRad = Math.toRadians(Math.clamp(lat, -85.0511, 85.0511));
		final double py = (1. - Math.log(Math.tan(latRad) + 1. / Math.cos(latRad)) / Math.PI) / 2. * worldSize;
		return (int)Math.floor(py / TILE_SIZE);
	}

	/**
	 * Computes the inclusive range of tile indices that can possibly
	 * intersect the visible portion of the globe.
	 * <p>
	 * The visible portion of the sphere is a spherical cap centered on the
	 * current camera center. The cap's angular radius is derived from the
	 * ratio between the maximum screen distance from the panel center and
	 * the globe radius. That cap is then converted to a conservative
	 * lat/lon bounding box, which in turn is converted to tile indices at
	 * the requested zoom.
	 * <p>
	 * The bounding box is deliberately over-inclusive (one tile of margin
	 * on each side, and the longitude extent is computed at the latitude
	 * closest to the pole), so no tile that should be drawn is excluded.
	 * The result is an array {@code [txMin, txMax, tyMin, tyMax]}.
	 *
	 * @param osmZoom the OSM zoom level
	 * @param w       the panel width
	 * @param h       the panel height
	 * @return the tile range, never {@code null}
	 */
	private int[] computeVisibleTileRange(final int osmZoom, final int w, final int h){
		final int numTiles = 1 << osmZoom;

		// Full range: no filtering needed at low zoom levels, where the
		// number of tiles is small.
		if(osmZoom <= 3)
			return new int[]{0, numTiles - 1, 0, numTiles - 1};

		final double cx = w / 2.;
		final double cy = h / 2.;
		final double maxDx = Math.max(cx, w - cx);
		final double maxDy = Math.max(cy, h - cy);
		final double dMax = Math.hypot(maxDx, maxDy);

		// Angular radius of the visible cap, in degrees.
		final double sinDelta = Math.min(1., dMax / radiusPx);
		final double deltaDeg = Math.toDegrees(Math.asin(sinDelta));

		// If the cap spans almost the entire visible hemisphere, skip the
		// filter: it would not reduce the count.
		if(deltaDeg >= 89.){
			return new int[]{0, numTiles - 1, 0, numTiles - 1};
		}

		// One tile of margin, in degrees, on each side.
		final double tileDeg = 360. / numTiles;

		final double latMin = Math.max(-85.05, centerLatDeg - deltaDeg - tileDeg);
		final double latMax = Math.min(85.05, centerLatDeg + deltaDeg + tileDeg);

		// Longitude extent: use the pole-facing latitude of the cap, where
		// the cap spans the widest longitude range.
		final double polewardLat = Math.min(89., Math.abs(centerLatDeg) + deltaDeg);
		final double cosPoleward = Math.max(1.e-3, Math.cos(Math.toRadians(polewardLat)));
		final double deltaLon = deltaDeg / cosPoleward + tileDeg;

		final int tyMin = Math.max(0, latToTileY(latMax, osmZoom));
		final int tyMax = Math.min(numTiles - 1, latToTileY(latMin, osmZoom));

		int txMin;
		int txMax;
		if(deltaLon >= 180.){
			// The cap wraps around the pole, covering all longitudes.
			txMin = 0;
			txMax = numTiles - 1;
		}
		else{
			final double lonMin = centerLonDeg - deltaLon;
			final double lonMax = centerLonDeg + deltaLon;
			if(lonMin < -180. || lonMax > 180.){
				// Wraps around the antimeridian. Conservative: take the
				// full range, which is safe and rarely encountered.
				txMin = 0;
				txMax = numTiles - 1;
			}
			else{
				txMin = Math.max(0, lonToTileX(lonMin, osmZoom));
				txMax = Math.min(numTiles - 1, lonToTileX(lonMax, osmZoom));
			}
		}

		return new int[]{txMin, txMax, tyMin, tyMax};
	}


	/* ======================================================================
	 *                          Interaction
	 * ====================================================================== */

	private void installListeners(){
		final MouseAdapter adapter = new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(!SwingUtilities.isLeftMouseButton(e))
					return;

				dragStart = e.getPoint();
				dragStartLat = centerLatDeg;
				dragStartLon = centerLonDeg;
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseReleased(final MouseEvent e){
				dragStart = null;
				setCursor(Cursor.getDefaultCursor());
			}

			@Override
			public void mouseDragged(final MouseEvent e){
				if(dragStart == null)
					return;

				final int dx = e.getX() - dragStart.x;
				final int dy = e.getY() - dragStart.y;

				// 90° of arc = one radius on screen, so degrees-per-pixel
				// is 90 / R. This makes the globe feel "sticky" at any size.
				final double degPerPx = 90. / radiusPx;

				centerLonDeg = normalizeLon(dragStartLon - dx * degPerPx);
				centerLatDeg = clamp(dragStartLat + dy * degPerPx, -89.99, 89.99);

				repaint();
			}

			@Override
			public void mouseWheelMoved(final MouseWheelEvent e){
				final double factor = Math.pow(1.15, -e.getWheelRotation());
				radiusPx = clamp(radiusPx * factor, minRadiusPx, maxRadiusPx);

				repaint();
			}
		};
		addMouseListener(adapter);
		addMouseMotionListener(adapter);
		addMouseWheelListener(adapter);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	/**
	 * Chooses the OSM zoom level that best matches the current globe
	 * radius. The equator on screen is about {@code 2πR} pixels long;
	 * the tile pyramid at zoom Z is {@code 256 · 2^Z} pixels long. The
	 * chosen zoom is the closest power of two. Capped at
	 * {@link #MAX_OSM_ZOOM} to keep the number of tiles manageable.
	 */
	private static int computeZoomForRadius(final double radiusPx){
		final double target = 2. * Math.PI * radiusPx;
		final int zoom = (int)Math.round(Math.log(target / TILE_SIZE) / Math.log(2));
		return Math.clamp(zoom, 0, MAX_OSM_ZOOM);
	}

	private static double clamp(final double v, final double min, final double max){
		return Math.clamp(v, min, max);
	}

	private static double normalizeLon(final double lon){
		double l = lon;
		while(l < -180.)
			l += 360.;
		while(l > 180.)
			l -= 360.;
		return l;
	}

}
