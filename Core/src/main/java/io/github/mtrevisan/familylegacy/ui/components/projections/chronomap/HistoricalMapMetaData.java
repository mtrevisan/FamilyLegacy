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

import org.jxmapviewer.viewer.GeoPosition;


/**
 * Represents metadata for a georeferenced historical map bounded by time and geographical limits.
 */
public record HistoricalMapMetaData(
	int mapId,
	String name,
	long startJdn,
	long endJdn,
	GeoPosition northWest,
	GeoPosition southEast
){
	/**
	 * Checks whether the map is valid for the given Julian Day Number.
	 */
	public boolean isAvailableAt(final double currentJdn){
		return currentJdn >= startJdn && currentJdn <= endJdn;
	}

	/**
	 * Checks whether a geographical position falls within the bounding box of this map.
	 */
	public boolean contains(final GeoPosition position){
		if(position == null || northWest == null || southEast == null)
			return true;

		final double lat = position.getLatitude();
		final double lon = position.getLongitude();

		final double minLat = Math.min(northWest.getLatitude(), southEast.getLatitude());
		final double maxLat = Math.max(northWest.getLatitude(), southEast.getLatitude());
		final double minLon = Math.min(northWest.getLongitude(), southEast.getLongitude());
		final double maxLon = Math.max(northWest.getLongitude(), southEast.getLongitude());

		return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
	}

	/**
	 * Checks whether this map is valid for both the given timeline date and map center position.
	 */
	public boolean isMatching(final double currentJdn, final GeoPosition position){
		return isAvailableAt(currentJdn) && contains(position);
	}

}
