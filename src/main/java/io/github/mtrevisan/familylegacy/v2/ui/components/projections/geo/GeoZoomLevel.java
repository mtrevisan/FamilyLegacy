/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

/**
 * Zoom level of the map view. Determines the visible geographic extent and
 * the granularity of the graticule.
 */
public enum GeoZoomLevel{

	WORLD(360.),
	CONTINENT(60.),
	COUNTRY(15.),
	REGION(4.),
	PROVINCE(1.),
	MUNICIPALITY(0.25),
	LOCAL(0.05);


	private final double spanDegrees;


	GeoZoomLevel(final double spanDegrees){
		this.spanDegrees = spanDegrees;
	}


	/**
	 * Returns the approximate longitude span, in degrees, covered by the
	 * viewport at this zoom level.
	 *
	 * @return the span in degrees
	 */
	public double getSpanDegrees(){
		return spanDegrees;
	}

	/**
	 * Returns the narrower zoom level, or {@code this} if already at
	 * {@link #LOCAL}.
	 *
	 * @return the next narrower level
	 */
	public GeoZoomLevel zoomIn(){
		final GeoZoomLevel[] values = values();
		final int next = ordinal() + 1;
		return (next < values.length? values[next]: this);
	}

	/**
	 * Returns the wider zoom level, or {@code this} if already at
	 * {@link #WORLD}.
	 *
	 * @return the next wider level
	 */
	public GeoZoomLevel zoomOut(){
		final GeoZoomLevel[] values = values();
		final int previous = ordinal() - 1;
		return (previous >= 0? values[previous]: this);
	}

	/**
	 * Derives the most appropriate zoom level for the given longitude
	 * span, in degrees.
	 *
	 * @param spanDegrees the span to fit
	 * @return the closest matching zoom level
	 */
	public static GeoZoomLevel forSpan(final double spanDegrees){
		for(final GeoZoomLevel level : values())
			if(spanDegrees <= level.spanDegrees)
				return level;
		return WORLD;
	}

}
