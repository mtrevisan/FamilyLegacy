/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import org.apache.commons.lang3.StringUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Draws the graticule (latitude and longitude grid) on top of the map.
 * <p>
 * The grid step is chosen automatically from the visible span, so that
 * between 4 and 12 meridians and parallels are drawn at any zoom level.
 * Labels are drawn along the top and the left edges of the viewport.
 */
public final class GeoGraticuleRenderer{

	/** Color of the graticule lines. */
	public static final Color COLOR_GRID = new Color(220, 218, 210);

	/** Color of the graticule labels. */
	public static final Color COLOR_LABEL = new Color(120, 110, 90);

	/** Font used for the labels. */
	private static final Font FONT_LABEL = new Font("Tahoma", Font.PLAIN, 10);

	private static final double[] STEP_CANDIDATES = {
		30., 20., 10., 5., 2., 1., 0.5, 0.25, 0.1, 0.05, 0.02, 0.01
	};


	private GeoGraticuleRenderer(){}


	/**
	 * Draws the graticule.
	 *
	 * @param g          the graphics context
	 * @param projection the projection
	 * @param viewport   the rectangle of the map area
	 */
	public static void draw(final Graphics2D g, final GeoProjection projection, final Rectangle viewport){
		if(g == null || projection == null || viewport == null)
			return;

		final double lonSpan = projection.bounds()
			.spanLongitude();
		final double step = chooseStep(lonSpan);

		drawMeridians(g, projection, viewport, step);
		drawParallels(g, projection, viewport, step);
	}


	private static double chooseStep(final double span){
		for(final double candidate : STEP_CANDIDATES)
			if(span / candidate >= 4.)
				return candidate;
		return STEP_CANDIDATES[STEP_CANDIDATES.length - 1];
	}


	private static void drawMeridians(final Graphics2D g, final GeoProjection projection,
		final Rectangle viewport, final double step){
		final GeoBounds bounds = projection.bounds();
		final double startLon = Math.ceil(bounds.minLongitude() / step) * step;
		final FontMetrics fm = g.getFontMetrics(FONT_LABEL);

		for(double lon = startLon; lon <= bounds.maxLongitude() + 1e-9; lon += step){
			final GeoCoordinate top = new GeoCoordinate(bounds.maxLatitude(), lon, null, StringUtils.EMPTY,
				GeoCoordinatePrecision.DEGREES);
			final GeoCoordinate bottom = new GeoCoordinate(bounds.minLatitude(), lon, null, StringUtils.EMPTY,
				GeoCoordinatePrecision.DEGREES);
			final Point2D p1 = projection.geoToScreen(top);
			final Point2D p2 = projection.geoToScreen(bottom);

			g.setColor(COLOR_GRID);
			g.setStroke(new BasicStroke(1f));
			g.draw(new Line2D.Double(p1, p2));

			final String label = formatLon(lon);
			final int textWidth = fm.stringWidth(label);
			g.setColor(COLOR_LABEL);
			g.setFont(FONT_LABEL);
			g.drawString(label, (int)p1.getX() - textWidth / 2, viewport.y + fm.getAscent() + 2);
		}
	}

	private static void drawParallels(final Graphics2D g, final GeoProjection projection,
		final Rectangle viewport, final double step){
		final GeoBounds bounds = projection.bounds();
		final double startLat = Math.ceil(bounds.minLatitude() / step) * step;
		final FontMetrics fm = g.getFontMetrics(FONT_LABEL);

		for(double lat = startLat; lat <= bounds.maxLatitude() + 1e-9; lat += step){
			final GeoCoordinate left = new GeoCoordinate(lat, bounds.minLongitude(), null, StringUtils.EMPTY,
				GeoCoordinatePrecision.DEGREES);
			final GeoCoordinate right = new GeoCoordinate(lat, bounds.maxLongitude(), null, StringUtils.EMPTY,
				GeoCoordinatePrecision.DEGREES);
			final Point2D p1 = projection.geoToScreen(left);
			final Point2D p2 = projection.geoToScreen(right);

			g.setColor(COLOR_GRID);
			g.setStroke(new BasicStroke(1f));
			g.draw(new Line2D.Double(p1, p2));

			final String label = formatLat(lat);
			g.setColor(COLOR_LABEL);
			g.setFont(FONT_LABEL);
			g.drawString(label, viewport.x + 2, (int)p1.getY() + fm.getAscent() / 2);
		}
	}


	private static String formatLon(final double lon){
		final char dir = (lon >= 0? 'E': 'W');
		return String.format(java.util.Locale.ROOT, "%.2f°%c", Math.abs(lon), dir);
	}

	private static String formatLat(final double lat){
		final char dir = (lat >= 0? 'N': 'S');
		return String.format(java.util.Locale.ROOT, "%.2f°%c", Math.abs(lat), dir);
	}

}
