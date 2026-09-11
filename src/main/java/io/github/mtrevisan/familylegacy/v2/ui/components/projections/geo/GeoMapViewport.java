/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.awt.Rectangle;

/**
 * Mutable viewport of the map view. Owns the current visible bounds and
 * the current pixel size, and produces a {@link GeoProjection} on demand.
 * <p>
 * The viewport is not thread-safe and is meant to be used from the Swing
 * Event Dispatch Thread.
 */
public final class GeoMapViewport{

	private GeoBounds bounds;
	private final Rectangle pixelBounds;
	private GeoZoomLevel zoomLevel;
	private GeoProjectionType projectionType;


	public GeoMapViewport(final GeoProjectionType projectionType){
		if(projectionType == null)
			throw new IllegalArgumentException("Projection type must not be null");
		this.projectionType = projectionType;
		this.bounds = new GeoBounds(-60., -180., 80., 180.);
		this.pixelBounds = new Rectangle(0, 0, 1, 1);
		this.zoomLevel = GeoZoomLevel.WORLD;
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public GeoBounds bounds(){ return bounds; }
	public GeoZoomLevel zoomLevel(){ return zoomLevel; }
	public GeoProjectionType projectionType(){ return projectionType; }
	public Rectangle pixelBounds(){ return new Rectangle(pixelBounds); }
	public int width(){ return pixelBounds.width; }
	public int height(){ return pixelBounds.height; }


	/* ======================================================================
	 *                          Mutation
	 * ====================================================================== */

	/**
	 * Sets the pixel area reserved for the map. Called by the panel on
	 * resize and during initial layout.
	 *
	 * @param width  the width in pixels (clamped to at least 1)
	 * @param height the height in pixels (clamped to at least 1)
	 */
	public void setPixelSize(final int width, final int height){
		pixelBounds.width = Math.max(1, width);
		pixelBounds.height = Math.max(1, height);
	}

	/**
	 * Sets the visible geographic bounds directly.
	 *
	 * @param bounds the new bounds (must not be {@code null})
	 */
	public void setBounds(final GeoBounds bounds){
		if(bounds == null)
			throw new IllegalArgumentException("Bounds must not be null");
		this.bounds = bounds;
		this.zoomLevel = GeoZoomLevel.forSpan(bounds.spanLongitude());
	}

	/**
	 * Sets the projection type.
	 *
	 * @param type the new type (must not be {@code null})
	 */
	public void setProjectionType(final GeoProjectionType type){
		if(type == null)
			throw new IllegalArgumentException("Projection type must not be null");
		this.projectionType = type;
	}

	/**
	 * Fits the viewport to the given geographic bounds, with a small
	 * padding in degrees.
	 *
	 * @param target  the bounds to fit; if {@code null}, the viewport is
	 *                reset to the whole world
	 * @param padding the padding in degrees
	 */
	public void fitTo(final GeoBounds target, final double padding){
		if(target == null){
			setBounds(new GeoBounds(-60., -180., 80., 180.));
			return;
		}
		final double pad = Math.max(0., padding);
		final double minLat = Math.max(GeoCoordinate.MIN_LATITUDE, target.minLatitude() - pad);
		final double maxLat = Math.min(GeoCoordinate.MAX_LATITUDE, target.maxLatitude() + pad);
		final double minLon = Math.max(GeoCoordinate.MIN_LONGITUDE, target.minLongitude() - pad);
		final double maxLon = Math.min(GeoCoordinate.MAX_LONGITUDE, target.maxLongitude() + pad);
		setBounds(new GeoBounds(minLat, minLon, maxLat, maxLon));
	}

	/**
	 * Pans the visible bounds by the given amount, in degrees.
	 *
	 * @param deltaLon the longitude offset
	 * @param deltaLat the latitude offset
	 */
	public void pan(final double deltaLon, final double deltaLat){
		final double minLat = clampLat(bounds.minLatitude() + deltaLat);
		final double maxLat = clampLat(bounds.maxLatitude() + deltaLat);
		final double minLon = wrapLon(bounds.minLongitude() + deltaLon);
		final double maxLon = wrapLon(bounds.maxLongitude() + deltaLon);
		if(minLat >= maxLat)
			return;
		this.bounds = new GeoBounds(minLat, minLon, maxLat, maxLon);
	}

	/**
	 * Zooms in one level, keeping the center of the viewport fixed.
	 */
	public void zoomIn(){
		final GeoZoomLevel next = zoomLevel.zoomIn();
		if(next == zoomLevel)
			return;
		zoomLevel = next;
		applyZoom();
	}

	/**
	 * Zooms out one level, keeping the center of the viewport fixed.
	 */
	public void zoomOut(){
		final GeoZoomLevel previous = zoomLevel.zoomOut();
		if(previous == zoomLevel)
			return;
		zoomLevel = previous;
		applyZoom();
	}

	private void applyZoom(){
		final GeoCoordinate center = bounds.center();
		final double span = zoomLevel.getSpanDegrees();
		final double minLat = clampLat(center.latitude() - span / 2.);
		final double maxLat = clampLat(center.latitude() + span / 2.);
		final double minLon = wrapLon(center.longitude() - span / 2.);
		final double maxLon = wrapLon(center.longitude() + span / 2.);
		this.bounds = new GeoBounds(minLat, minLon, maxLat, maxLon);
	}


	/* ======================================================================
	 *                          Projection factory
	 * ====================================================================== */

	/**
	 * Creates a projection for the current state of the viewport.
	 *
	 * @return a new projection
	 */
	public GeoProjection createProjection(){
		return new GeoProjection(bounds, new Rectangle(pixelBounds), projectionType);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static double clampLat(final double lat){
		return Math.max(GeoCoordinate.MIN_LATITUDE, Math.min(GeoCoordinate.MAX_LATITUDE, lat));
	}

	private static double wrapLon(final double lon){
		double result = lon;
		while(result < GeoCoordinate.MIN_LONGITUDE)
			result += 360.;
		while(result > GeoCoordinate.MAX_LONGITUDE)
			result -= 360.;
		return result;
	}

}
