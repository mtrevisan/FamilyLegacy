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
		if(position == null || northWest == null || southEast == null){
			return true; // No spatial constraints defined, valid everywhere
		}

		final double lat = position.getLatitude();
		final double lon = position.getLongitude();

		final boolean latMatches = lat <= northWest.getLatitude() && lat >= southEast.getLatitude();
		final boolean lonMatches = lon >= northWest.getLongitude() && lon <= southEast.getLongitude();

		return latMatches && lonMatches;
	}

	/**
	 * Checks whether this map is valid for both the given timeline date and map center position.
	 */
	public boolean isMatching(final double currentJdn, final GeoPosition position){
		return isAvailableAt(currentJdn) && contains(position);
	}

}
