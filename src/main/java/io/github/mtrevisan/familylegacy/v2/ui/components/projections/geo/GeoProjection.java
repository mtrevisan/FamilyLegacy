/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import org.apache.commons.lang3.StringUtils;

import java.awt.geom.Point2D;

/**
 * Converts geographic coordinates to screen pixel coordinates and back.
 * <p>
 * The projection owns a source {@link GeoBounds} and a target pixel
 * rectangle. For a given input coordinate inside the bounds, the output
 * is a point inside the target rectangle. Inputs outside the bounds
 * produce outputs outside the rectangle, which callers are expected to
 * clip.
 * <p>
 * The projection is deterministic and (within numerical precision)
 * reversible through {@link #screenToGeo(Point2D)}.
 */
public final class GeoProjection{

	/** Maximum latitude accepted by the Mercator variants to avoid divergence. */
	public static final double MERCATOR_LAT_LIMIT = 85.05112878;

	private final GeoBounds bounds;
	private final java.awt.Rectangle viewport;
	private final GeoProjectionType type;


	public GeoProjection(final GeoBounds bounds, final java.awt.Rectangle viewport,
		final GeoProjectionType type){
		if(bounds == null)
			throw new IllegalArgumentException("Bounds must not be null");
		if(viewport == null || viewport.width <= 0 || viewport.height <= 0)
			throw new IllegalArgumentException("Viewport must be a positive rectangle");
		if(type == null)
			throw new IllegalArgumentException("Projection type must not be null");
		this.bounds = bounds;
		this.viewport = viewport;
		this.type = type;
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	public GeoBounds bounds(){ return bounds; }
	public java.awt.Rectangle viewport(){ return viewport; }
	public GeoProjectionType type(){ return type; }


	/* ======================================================================
	 *                          Forward
	 * ====================================================================== */

	/**
	 * Projects a geographic coordinate onto the screen.
	 *
	 * @param geo the coordinate (must not be {@code null})
	 * @return the screen point
	 */
	public Point2D geoToScreen(final GeoCoordinate geo){
		final double normalizedX = normalizeLongitude(geo.longitude());
		final double normalizedY = normalizeLatitude(geo.latitude());
		return new Point2D.Double(
			viewport.x + normalizedX * viewport.width,
			viewport.y + normalizedY * viewport.height);
	}


	/* ======================================================================
	 *                          Inverse
	 * ====================================================================== */

	/**
	 * Unprojects a screen point back to a geographic coordinate.
	 *
	 * @param screen the screen point
	 * @return the geographic coordinate
	 */
	public GeoCoordinate screenToGeo(final Point2D screen){
		final double normalizedX = (screen.getX() - viewport.x) / (double)viewport.width;
		final double normalizedY = (screen.getY() - viewport.y) / (double)viewport.height;
		final double lon = denormalizeLongitude(normalizedX);
		final double lat = denormalizeLatitude(normalizedY);
		return new GeoCoordinate(lat, lon, null, StringUtils.EMPTY, GeoCoordinatePrecision.DEGREES);
	}


	/* ======================================================================
	 *                          Normalization
	 * ====================================================================== */

	private double normalizeLongitude(final double lon){
		final double span = bounds.spanLongitude();
		if(span <= 0.)
			return 0.5;
		return (lon - bounds.minLongitude()) / span;
	}

	private double normalizeLatitude(final double lat){
		final double clamped = clampLatitude(lat);
		final double minLat = clampLatitude(bounds.minLatitude());
		final double maxLat = clampLatitude(bounds.maxLatitude());
		return switch(type){
			case EQUIRECTANGULAR -> (clamped - minLat) / Math.max(1e-9, maxLat - minLat);
			case MERCATOR, MERCATOR_WEB -> {
				final double y = mercatorY(clamped);
				final double yMin = mercatorY(minLat);
				final double yMax = mercatorY(maxLat);
				yield (y - yMin) / Math.max(1e-9, yMax - yMin);
			}
		};
	}

	private double denormalizeLongitude(final double normalized){
		return bounds.minLongitude() + normalized * bounds.spanLongitude();
	}

	private double denormalizeLatitude(final double normalized){
		final double minLat = clampLatitude(bounds.minLatitude());
		final double maxLat = clampLatitude(bounds.maxLatitude());
		return switch(type){
			case EQUIRECTANGULAR -> minLat + normalized * (maxLat - minLat);
			case MERCATOR, MERCATOR_WEB -> {
				final double y = mercatorY(minLat) + normalized * (mercatorY(maxLat) - mercatorY(minLat));
				yield inverseMercatorY(y);
			}
		};
	}

	/**
	 * Returns the Mercator normalized Y value of the given latitude, in
	 * the range [0, 1] with 0 = north pole (clamped) and 1 = south pole
	 * (clamped).
	 */
	private double mercatorY(final double latitude){
		final double latRad = Math.toRadians(clampLatitude(latitude));
		final double y = Math.log(Math.tan(Math.PI / 4. + latRad / 2.));
		final double yMax = Math.log(Math.tan(Math.PI / 4. + Math.toRadians(MERCATOR_LAT_LIMIT) / 2.));
		// y ranges from +yMax (north) to -yMax (south); we want 0 at north.
		return (yMax - y) / (2. * yMax);
	}

	private double inverseMercatorY(final double normalized){
		final double yMax = Math.log(Math.tan(Math.PI / 4. + Math.toRadians(MERCATOR_LAT_LIMIT) / 2.));
		final double y = yMax - normalized * 2. * yMax;
		return Math.toDegrees(2. * Math.atan(Math.exp(y)) - Math.PI / 2.);
	}

	private double clampLatitude(final double lat){
		if(!type.requiresLatitudeClamp())
			return lat;
		return Math.clamp(lat, -MERCATOR_LAT_LIMIT, MERCATOR_LAT_LIMIT);
	}

}
