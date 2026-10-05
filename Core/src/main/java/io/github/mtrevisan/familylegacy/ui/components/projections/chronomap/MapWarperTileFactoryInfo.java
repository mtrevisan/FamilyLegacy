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

import org.jxmapviewer.viewer.TileFactoryInfo;


/**
 * {@link TileFactoryInfo} for georeferenced historical maps hosted on
 * MapWarper.
 * <p>
 * <b>Zoom convention.</b> JXMapViewer passes to {@link #getTileUrl} the
 * <em>viewer</em> zoom (0 = most detailed), whereas MapWarper serves its
 * tiles using the standard XYZ scheme (z = 0 = whole world, z growing
 * with detail). The URL builder therefore inverts the zoom, exactly as
 * {@link org.jxmapviewer.OSMTileFactoryInfo} does.
 * <p>
 * <b>Alignment with the base map.</b> For the overlay to line up with
 * the modern base map, this factory MUST share the same
 * {@code totalMapZoom} (and tile size) as the base map's factory.
 * jxmapviewer2 computes world-pixel coordinates using the
 * {@code totalMapZoom} passed to the super constructor; if the two
 * factories disagree on it, the same viewer zoom maps to two different
 * pixel scales, and any overlay drawn using one factory's
 * {@code geoToPixel} together with the other factory's tiles will be
 * misaligned.
 * <p>
 * Use the parameterised constructor to match the base map factory's
 * {@code totalMapZoom}, which can be queried via
 * {@code map.getTileFactory().getInfo().getTotalMapZoom()}.
 */
public final class MapWarperTileFactoryInfo extends TileFactoryInfo{

	private static final int TILE_SIZE = 256;


	/**
	 * Creates a factory aligned with the given {@code totalMapZoom}.
	 *
	 * @param mapId         the numeric MapWarper map identifier
	 * @param totalMapZoom  the total zoom levels, matching the base map
	 *                      factory (query it via
	 *                      {@code map.getTileFactory().getInfo().getTotalMapZoom()})
	 */
	public MapWarperTileFactoryInfo(final int mapId, final int totalMapZoom){
		super(
			"MapWarper-" + mapId,
			0,
			totalMapZoom - 1,
			totalMapZoom,
			TILE_SIZE,
			true,
			true,
			"https://mapwarper.net/maps/tile/" + mapId,
			"x", "y", "z"
		);
	}


	/**
	 * Builds the MapWarper tile URL.
	 * <p>
	 * The viewer zoom is inverted into the XYZ z, matching the convention
	 * used by {@link org.jxmapviewer.OSMTileFactoryInfo}. Do not remove
	 * this inversion: without it, the tile fetched for a given viewer
	 * zoom belongs to a completely different scale, and the overlay
	 * either disappears (404 from MapWarper) or appears at the wrong
	 * place.
	 */
	@Override
	public String getTileUrl(final int x, final int y, final int zoom) {
		final int z = getTotalMapZoom() - zoom;
		return baseURL + "/" + z + "/" + x + "/" + y + ".png";
	}

}
