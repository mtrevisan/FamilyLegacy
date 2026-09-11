/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * A geographic coordinate expressed in decimal degrees.
 * <p>
 * Latitude is positive north of the equator, negative south. Longitude is
 * positive east of the prime meridian, negative west. The altitude is
 * optional and expressed in meters.
 * <p>
 * The record is immutable.
 */
public record GeoCoordinate(
	double latitude,
	double longitude,
	Double altitude,
	String originalFormat,
	GeoCoordinatePrecision precision
){

	public static final double MIN_LATITUDE = -90.;
	public static final double MAX_LATITUDE = 90.;
	public static final double MIN_LONGITUDE = -180.;
	public static final double MAX_LONGITUDE = 180.;


	public GeoCoordinate{
		if(latitude < MIN_LATITUDE || latitude > MAX_LATITUDE)
			throw new IllegalArgumentException("Latitude out of range: " + latitude);
		if(longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE)
			throw new IllegalArgumentException("Longitude out of range: " + longitude);
		if(precision == null)
			precision = GeoCoordinatePrecision.DEGREES;
		if(originalFormat == null)
			originalFormat = StringUtils.EMPTY;
	}


	/**
	 * Creates a coordinate from decimal degrees.
	 */
	public static GeoCoordinate of(final double latitude, final double longitude){
		return new GeoCoordinate(latitude, longitude, null, StringUtils.EMPTY, GeoCoordinatePrecision.DEGREES);
	}

	/**
	 * Returns whether this coordinate has an altitude.
	 */
	public boolean hasAltitude(){
		return (altitude != null);
	}

	/**
	 * Returns a formatted representation suitable for tooltips.
	 */
	public String format(){
		return String.format(Locale.ROOT, "%.4f°, %.4f°",
			latitude, longitude);
	}

	@Override
	public String toString(){
		return format();
	}

}
