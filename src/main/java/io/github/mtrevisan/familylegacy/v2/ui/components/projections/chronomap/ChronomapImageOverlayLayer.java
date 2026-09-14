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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.chronomap;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.viewer.GeoPosition;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;


/**
 * Image overlay layer.
 * <p>
 * Draws one or more georeferenced raster images on top of the map. Each
 * overlay is anchored to two {@link GeoPosition} corners (north-west and
 * south-east) and is rendered as a scaled, semi-transparent image. This
 * is the mechanism used to place historical maps, scanned parish
 * registers, or any other raster geodata that the user wants to compare
 * against the current map.
 * <p>
 * The layer is empty until the caller adds at least one overlay. The
 * overlays are stored in insertion order and drawn back-to-front.
 */
public final class ChronomapImageOverlayLayer implements ChronomapLayer{

	private final List<Overlay> overlays = new ArrayList<>();

	private boolean visible = true;


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Adds a raster overlay anchored to two corners.
	 *
	 * @param image     the raster image
	 * @param northWest the north-west corner, in geographic coordinates
	 * @param southEast the south-east corner, in geographic coordinates
	 * @param alpha     opacity in {@code [0, 1]}
	 */
	public void addOverlay(final Image image, final GeoPosition northWest, final GeoPosition southEast,
			final float alpha){
		if(image == null || northWest == null || southEast == null)
			return;

		overlays.add(new Overlay(image, northWest, southEast,
			Math.clamp(alpha, 0f, 1f)));
	}

	public void clearOverlays(){
		overlays.clear();
	}

	public int overlayCount(){
		return overlays.size();
	}

	@Override
	public String getName(){
		return "Image overlays";
	}

	@Override
	public boolean isVisible(){
		return visible;
	}

	@Override
	public void setVisible(final boolean visible){
		this.visible = visible;
	}


	/* ======================================================================
	 *                          Painting
	 * ====================================================================== */

	@Override
	public void paint(final Graphics2D g, final JXMapViewer map, final int w, final int h){
		if(overlays.isEmpty())
			return;

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);

		final Composite original = g.getComposite();
		try{
			for(final Overlay o : overlays){
				final Point2D nw = map.convertGeoPositionToPoint(o.northWest());
				final Point2D se = map.convertGeoPositionToPoint(o.southEast());

				final int x = (int)Math.min(nw.getX(), se.getX());
				final int y = (int)Math.min(nw.getY(), se.getY());
				final int width = (int)Math.abs(se.getX() - nw.getX());
				final int height = (int)Math.abs(se.getY() - nw.getY());
				if(width <= 0 || height <= 0)
					continue;

				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, o.alpha()));
				g.drawImage(o.image(), x, y, width, height, null);
			}
		}
		finally{
			g.setComposite(original);
		}
	}


	private record Overlay(Image image, GeoPosition northWest, GeoPosition southEast, float alpha){}

}
