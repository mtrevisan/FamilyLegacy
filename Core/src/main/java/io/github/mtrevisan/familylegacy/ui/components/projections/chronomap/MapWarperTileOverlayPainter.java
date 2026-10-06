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

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.TileFactoryInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Overlay layer that renders a MapWarper map on top of the modern
 * OpenStreetMap base map.
 *
 * <p><b>Probing.</b> MapWarper does not expose an index of which zoom
 * levels it has tiles for, and answers HTTP 500 (not 404) when a tile
 * does not exist. Asking for the whole visible grid at a new zoom level
 * would therefore issue dozens of doomed requests. To avoid this, the
 * layer probes a single tile at the center of the map's bounding box
 * before rendering the grid. If the probe loads within a timeout, the
 * zoom is marked as confirmed and the full grid is requested on the
 * next paint. If the probe times out, the zoom is blacklisted for this
 * map: no further tile request is issued at that zoom, and the base
 * map remains visible underneath.</p>
 */
public final class MapWarperTileOverlayPainter implements ChronomapLayer{

	private static final Logger LOGGER = LoggerFactory.getLogger(MapWarperTileOverlayPainter.class);

	/**
	 * Lowest OSM zoom level for which MapWarper is expected to serve
	 * tiles. Below this level the server almost always answers with an
	 * HTTP error.
	 */
	private static final int MIN_OSM_ZOOM = 5;

	/**
	 * Highest OSM zoom level for which MapWarper is expected to serve
	 * tiles. Above this level the tile pyramid usually does not contain
	 * images because the source scan was georeferenced at a coarser
	 * resolution.
	 */
	private static final int MAX_OSM_ZOOM = 18;

	/**
	 * Milliseconds to wait for the probe tile before declaring the zoom
	 * level unavailable. Generous enough to accommodate a slow network
	 * and the single-request rate limiting that MapWarper applies to
	 * unauthenticated clients.
	 */
	private static final int PROBE_TIMEOUT_MS = 2500;


	private final String name;
	private final TileFactory tileFactory;

	private HistoricalMapMetaData metaData;
	private double currentTime;
	private float alpha = 0.9f;
	private boolean visible = true;
	private boolean alignmentChecked;

	/** OSM zoom levels that have been probed and found to have tiles. */
	private final Set<Integer> confirmedZooms = ConcurrentHashMap.newKeySet();

	/** OSM zoom levels where the probe failed: no further request is issued. */
	private final Set<Integer> blacklistedZooms = ConcurrentHashMap.newKeySet();

	/** OSM zoom levels for which a probe is currently in flight. */
	private final Set<Integer> probingZooms = ConcurrentHashMap.newKeySet();

	/**
	 * Tiles that already have a "loaded" listener attached. Prevents
	 * accumulating one listener per paint on slow-loading tiles, which
	 * would amplify the number of repaint passes when they finally
	 * finish.
	 */
	private final Set<Tile> listenedTiles = Collections.newSetFromMap(new WeakHashMap<>());


	public MapWarperTileOverlayPainter(final HistoricalMapMetaData metaData, final TileFactory tileFactory){
		this.metaData = metaData;
		this.name = metaData.name();
		this.tileFactory = tileFactory;
	}


	public void setMetaData(final HistoricalMapMetaData metaData){
		this.metaData = metaData;
	}

	/**
	 * Identifier of the MapWarper map this overlay renders. Used by the
	 * enclosing panel to match the overlay against the metadata list.
	 *
	 * @return the MapWarper map id, or {@code -1} when no metadata is set
	 */
	public int mapId(){
		return (metaData != null? metaData.mapId(): -1);
	}

	/**
	 * Returns the metadata of the map currently rendered by this overlay.
	 *
	 * @return the metadata, or {@code null} when none is set
	 */
	public HistoricalMapMetaData metadata(){
		return metaData;
	}

	public boolean setCurrentTime(final double currentTime){
		if(Double.compare(this.currentTime, currentTime) != 0){
			final boolean wasAvailable = metaData != null && metaData.isAvailableAt(this.currentTime);
			this.currentTime = currentTime;
			final boolean isAvailable = metaData != null && metaData.isAvailableAt(this.currentTime);

			// Return true if the visibility state shifted across validity boundary
			return wasAvailable != isAvailable;
		}
		return false;
	}

	public void setAlpha(final float alpha){
		this.alpha = Math.clamp(alpha, 0.f, 1.f);
	}

	public float getAlpha(){
		return alpha;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public boolean isVisible(){
		return visible;
	}

	@Override
	public void setVisible(final boolean visible){
		this.visible = visible;
	}

	@Override
	public void setTotalMapZoom(final int totalMapZoom){}

	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int width, final int height){
		if(!visible || metaData == null || tileFactory == null)
			return;

		// Hide historical overlay if current time is outside the validity range
		if(currentTime > 0 && !metaData.isAvailableAt(currentTime))
			return;

		checkAlignment(map);

		final TileFactoryInfo info = tileFactory.getInfo();
		final int totalMapZoom = info.getTotalMapZoom();
		final int jxZoom = map.getZoom();
		final int osmZoom = totalMapZoom - jxZoom;

		if(osmZoom < MIN_OSM_ZOOM || osmZoom > MAX_OSM_ZOOM)
			return;

		if(blacklistedZooms.contains(osmZoom))
			return;

		if(!confirmedZooms.contains(osmZoom)){
			probeZoom(osmZoom, jxZoom, map);

			return;
		}

		final Graphics2D g2 = (Graphics2D)g.create();
		try{
			final Composite oldComposite = g2.getComposite();
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

			renderTiles(g2, map, jxZoom);

			g2.setComposite(oldComposite);
		}
		finally{
			g2.dispose();
		}
	}


	/* ======================================================================
	 *                          Probing
	 * ====================================================================== */

	/**
	 * Issues a single tile request to test whether the map has tiles at
	 * the given zoom level. The probe is anchored at the center of the
	 * map's bounding box, which by definition lies inside the map area
	 * and is therefore the most likely location to have a tile.
	 * <p>
	 * On success, the zoom is added to {@link #confirmedZooms} and the
	 * next paint will render the full grid. On timeout, the zoom is
	 * added to {@link #blacklistedZooms} and no further request is
	 * issued at that zoom.
	 */
	private void probeZoom(final int osmZoom, final int jxZoom, final JXMapViewer map){
		if(!probingZooms.add(osmZoom))
			return;

		final TileFactory mapFactory = map.getTileFactory();
		final int tileSize = mapFactory.getTileSize(jxZoom);

		final GeoPosition probePos = new GeoPosition(
			(metaData.northWest().getLatitude() + metaData.southEast().getLatitude()) / 2.,
			(metaData.northWest().getLongitude() + metaData.southEast().getLongitude()) / 2.
		);
		final Point2D probePixel = mapFactory.geoToPixel(probePos, jxZoom);
		final int tileX = (int)(probePixel.getX() / tileSize);
		final int tileY = (int)(probePixel.getY() / tileSize);

		final Tile probeTile = tileFactory.getTile(tileX, tileY, jxZoom);
		if(probeTile == null){
			probingZooms.remove(osmZoom);
			blacklistedZooms.add(osmZoom);

			LOGGER.debug("MapWarper map {} has no tiles at OSM zoom {} (null tile)", mapId(), osmZoom);

			return;
		}

		if(isTileGood(probeTile)){
			confirmedZooms.add(osmZoom);
			probingZooms.remove(osmZoom);

			SwingUtilities.invokeLater(map::repaint);

			return;
		}

		probeTile.addPropertyChangeListener("loaded", evt -> {
			if(Boolean.TRUE.equals(evt.getNewValue()) && isTileGood(probeTile)){
				confirmedZooms.add(osmZoom);
				probingZooms.remove(osmZoom);

				SwingUtilities.invokeLater(map::repaint);
			}
		});

		final Timer timer = new Timer(PROBE_TIMEOUT_MS, e -> {
			probingZooms.remove(osmZoom);
			if(isTileGood(probeTile)){
				confirmedZooms.add(osmZoom);

				SwingUtilities.invokeLater(map::repaint);
			}
			else{
				blacklistedZooms.add(osmZoom);

				LOGGER.debug("MapWarper map {} has no tiles at OSM zoom {} (probe timeout)", mapId(), osmZoom);
			}
		});
		timer.setRepeats(false);
		timer.start();
	}


	/* ======================================================================
	 *                          Rendering
	 * ====================================================================== */

	private void renderTiles(final Graphics2D g2, final JXMapViewer map, final int jxZoom){
		final TileFactory mapFactory = map.getTileFactory();
		final int tileSize = mapFactory.getTileSize(jxZoom);
		final Rectangle viewportBounds = map.getViewportBounds();

		final Point2D nwPixel = mapFactory.geoToPixel(metaData.northWest(), jxZoom);
		final Point2D sePixel = mapFactory.geoToPixel(metaData.southEast(), jxZoom);

		final double mapMinX = Math.min(nwPixel.getX(), sePixel.getX());
		final double mapMaxX = Math.max(nwPixel.getX(), sePixel.getX());
		final double mapMinY = Math.min(nwPixel.getY(), sePixel.getY());
		final double mapMaxY = Math.max(nwPixel.getY(), sePixel.getY());

		// Skip rendering if map overlay is outside the current viewport
		if(mapMaxX < viewportBounds.getX() || mapMinX > viewportBounds.getX() + viewportBounds.getWidth()
				|| mapMaxY < viewportBounds.getY() || mapMinY > viewportBounds.getY() + viewportBounds.getHeight())
			return;

		final int minX = (int)Math.floor(mapMinX / tileSize);
		final int maxX = (int)Math.ceil(mapMaxX / tileSize);
		final int minY = (int)Math.floor(mapMinY / tileSize);
		final int maxY = (int)Math.ceil(mapMaxY / tileSize);

		for(int x = minX; x <= maxX; x ++)
			for(int y = minY; y <= maxY; y ++){
				final Tile tile = tileFactory.getTile(x, y, jxZoom);
				if(tile == null)
					continue;

				if(isTileGood(tile)){
					final BufferedImage image = tile.getImage();
					final int px = (int)(x * tileSize - viewportBounds.getX());
					final int py = (int)(y * tileSize - viewportBounds.getY());
					g2.drawImage(image, px, py, tileSize, tileSize, null);
				}
				else if(listenedTiles.add(tile))
					tile.addPropertyChangeListener("loaded", evt -> {
						listenedTiles.remove(tile);

						if(Boolean.TRUE.equals(evt.getNewValue()))
							SwingUtilities.invokeLater(map::repaint);
					});
			}
	}

	/**
	 * A tile is considered usable only when it is marked as loaded and
	 * carries a non-empty image. Some servers respond with an empty body
	 * without raising an exception, which leaves the tile in a state
	 * where {@code isLoaded()} is true but {@code getImage()} is null or
	 * zero-sized.
	 */
	private static boolean isTileGood(final Tile tile){
		if(tile == null || !tile.isLoaded())
			return false;

		final BufferedImage image = tile.getImage();
		return image != null && image.getWidth() > 0 && image.getHeight() > 0;
	}

	private void checkAlignment(final JXMapViewer map){
		if(alignmentChecked)
			return;

		alignmentChecked = true;

		final TileFactoryInfo baseInfo = map.getTileFactory().getInfo();
		final TileFactoryInfo overlayInfo = tileFactory.getInfo();

		if(baseInfo.getTotalMapZoom() != overlayInfo.getTotalMapZoom())
			throw new IllegalStateException("MapWarper overlay and base map use different totalMapZoom");
	}

}
