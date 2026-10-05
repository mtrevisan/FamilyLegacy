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
