/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import org.apache.commons.lang3.StringUtils;


/**
 * An axis-aligned geographic bounding box.
 * <p>
 * The record is immutable. Latitude and longitude bounds are inclusive.
 */
public record GeoBounds(
	double minLatitude,
	double minLongitude,
	double maxLatitude,
	double maxLongitude
){

	public GeoBounds{
		if(minLatitude > maxLatitude)
			throw new IllegalArgumentException("minLatitude > maxLatitude");
		if(minLongitude > maxLongitude)
			throw new IllegalArgumentException("minLongitude > maxLongitude");
	}


	/**
	 * Returns a bounds centered on the given coordinate with the given
	 * span in degrees.
	 *
	 * @param center      the center coordinate
	 * @param spanDegrees the span in degrees
	 * @return the bounds
	 */
	public static GeoBounds around(final GeoCoordinate center, final double spanDegrees){
		final double half = Math.abs(spanDegrees) / 2.;
		final double minLat = Math.max(GeoCoordinate.MIN_LATITUDE, center.latitude() - half);
		final double maxLat = Math.min(GeoCoordinate.MAX_LATITUDE, center.latitude() + half);
		final double minLon = Math.max(GeoCoordinate.MIN_LONGITUDE, center.longitude() - half);
		final double maxLon = Math.min(GeoCoordinate.MAX_LONGITUDE, center.longitude() + half);
		return new GeoBounds(minLat, minLon, maxLat, maxLon);
	}

	/**
	 * Returns the bounds that encloses the given coordinates, with the
	 * given padding in degrees. Returns {@code null} if the input is
	 * empty.
	 *
	 * @param coords  the coordinates
	 * @param padding the padding in degrees
	 * @return the bounds, or {@code null} if no coordinate was provided
	 */
	public static GeoBounds enclosing(final Iterable<GeoCoordinate> coords, final double padding){
		double minLat = Double.POSITIVE_INFINITY;
		double minLon = Double.POSITIVE_INFINITY;
		double maxLat = Double.NEGATIVE_INFINITY;
		double maxLon = Double.NEGATIVE_INFINITY;
		boolean any = false;
		for(final GeoCoordinate c : coords){
			if(c == null)
				continue;
			any = true;
			minLat = Math.min(minLat, c.latitude());
			maxLat = Math.max(maxLat, c.latitude());
			minLon = Math.min(minLon, c.longitude());
			maxLon = Math.max(maxLon, c.longitude());
		}
		if(!any)
			return null;
		return new GeoBounds(
			Math.max(GeoCoordinate.MIN_LATITUDE, minLat - padding),
			Math.max(GeoCoordinate.MIN_LONGITUDE, minLon - padding),
			Math.min(GeoCoordinate.MAX_LATITUDE, maxLat + padding),
			Math.min(GeoCoordinate.MAX_LONGITUDE, maxLon + padding));
	}


	public double centerLatitude(){
		return (minLatitude + maxLatitude) / 2.;
	}

	public double centerLongitude(){
		return (minLongitude + maxLongitude) / 2.;
	}

	public double spanLatitude(){
		return maxLatitude - minLatitude;
	}

	public double spanLongitude(){
		return maxLongitude - minLongitude;
	}

	public GeoCoordinate center(){
		return new GeoCoordinate(centerLatitude(), centerLongitude(), null, StringUtils.EMPTY,
			GeoCoordinatePrecision.DEGREES);
	}

	/**
	 * Returns whether this bounds contains the given coordinate.
	 */
	public boolean contains(final GeoCoordinate c){
		return (c != null
			&& c.latitude() >= minLatitude && c.latitude() <= maxLatitude
			&& c.longitude() >= minLongitude && c.longitude() <= maxLongitude);
	}

}
