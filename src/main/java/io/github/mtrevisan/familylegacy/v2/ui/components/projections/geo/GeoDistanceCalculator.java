/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Utilities for computing distances between geographic coordinates.
 * <p>
 * Uses the Haversine formula on a spherical Earth of mean radius
 * 6371 km, accurate to about 0.5% for points that are not antipodal and
 * sufficient for genealogical visualization.
 */
public final class GeoDistanceCalculator{

	/** Mean radius of the Earth in kilometers. */
	public static final double EARTH_RADIUS_KM = 6371.;


	private GeoDistanceCalculator(){}


	/**
	 * Returns the great-circle distance between two coordinates, in
	 * kilometers.
	 *
	 * @param a the first coordinate (must not be {@code null})
	 * @param b the second coordinate (must not be {@code null})
	 * @return the distance in kilometers
	 */
	public static double haversineKm(final GeoCoordinate a, final GeoCoordinate b){
		final double lat1 = Math.toRadians(a.latitude());
		final double lat2 = Math.toRadians(b.latitude());
		final double dLat = Math.toRadians(b.latitude() - a.latitude());
		final double dLon = Math.toRadians(b.longitude() - a.longitude());

		final double sinDLat = Math.sin(dLat / 2.);
		final double sinDLon = Math.sin(dLon / 2.);
		final double h = sinDLat * sinDLat + Math.cos(lat1) * Math.cos(lat2) * sinDLon * sinDLon;
		final double c = 2. * Math.atan2(Math.sqrt(h), Math.sqrt(1. - h));
		return EARTH_RADIUS_KM * c;
	}

	/**
	 * Returns the total distance covered by a sequence of coordinates.
	 *
	 * @param coords the sequence
	 * @return the total distance in kilometers
	 */
	public static double totalDistanceKm(final Iterable<GeoCoordinate> coords){
		double total = 0.;
		GeoCoordinate previous = null;
		for(final GeoCoordinate c : coords){
			if(c == null)
				continue;
			if(previous != null)
				total += haversineKm(previous, c);
			previous = c;
		}
		return total;
	}

}
