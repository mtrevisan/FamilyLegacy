/**
 * Copyright (c) 2026 Mauro Trevisan
 * ... (license header unchanged)
 */
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 * Draws place, event, residence and migration-stop markers on the map.
 * <p>
 * Marker shapes and colors are determined by {@link GeoMarkerType}:
 * <ul>
 *   <li>{@link GeoMarkerType#PLACE} — filled circle in dark blue;</li>
 *   <li>{@link GeoMarkerType#EVENT} — smaller filled circle in
 *       {@link #COLOR_EVENT};</li>
 *   <li>{@link GeoMarkerType#RESIDENCE} — square in
 *       {@link #COLOR_RESIDENCE};</li>
 *   <li>{@link GeoMarkerType#MIGRATION_STOP} — triangle in
 *       {@link #COLOR_MIGRATION};</li>
 *   <li>{@link GeoMarkerType#HISTORIC_EVENT} — hollow circle in
 *       {@link #COLOR_HISTORIC}.</li>
 * </ul>
 * Labels are drawn next to the marker when the visible zoom level is
 * sufficiently narrow, to avoid clutter at world scale.
 */
public final class GeoMarkerRenderer{

	/** Color of place markers. */
	public static final Color COLOR_PLACE = new Color(40, 70, 130);

	/** Color of event markers. */
	public static final Color COLOR_EVENT = new Color(180, 60, 60);

	/** Color of residence markers. */
	public static final Color COLOR_RESIDENCE = new Color(60, 130, 70);

	/** Color of migration-stop markers. */
	public static final Color COLOR_MIGRATION = new Color(210, 130, 40);

	/** Color of historic-event markers. */
	public static final Color COLOR_HISTORIC = new Color(140, 90, 160);

	/** Color of the cluster markers. */
	public static final Color COLOR_CLUSTER = new Color(70, 70, 70);

	/** Color of the cluster label text. */
	public static final Color COLOR_CLUSTER_TEXT = Color.WHITE;

	/** Color of the marker labels. */
	public static final Color COLOR_LABEL = new Color(40, 40, 40);

	/** Color of a selected marker. */
	public static final Color COLOR_SELECTED = new Color(220, 100, 60);

	/** Minimum zoom level at which labels are drawn. */
	private static final GeoZoomLevel LABEL_ZOOM_THRESHOLD = GeoZoomLevel.REGION;

	private static final Font FONT_LABEL = new Font("Tahoma", Font.PLAIN, 11);
	private static final Font FONT_CLUSTER = new Font("Tahoma", Font.BOLD, 11);

	private static final int PLACE_RADIUS = 6;
	private static final int EVENT_RADIUS = 4;
	private static final int RESIDENCE_HALF = 5;
	private static final int MIGRATION_RADIUS = 5;
	private static final int HISTORIC_RADIUS = 7;
	private static final int SELECTED_EXTRA = 3;


	private GeoMarkerRenderer(){}


	/* ======================================================================
	 *                          Places
	 * ====================================================================== */

	public static void drawPlace(final Graphics2D g, final GeoPlaceRef place, final Point2D center,
		final boolean selected, final boolean drawLabel){
		if(g == null || place == null || center == null)
			return;
		final Color fill = (selected? COLOR_SELECTED: COLOR_PLACE);
		final int r = PLACE_RADIUS + (selected? SELECTED_EXTRA: 0);
		g.setColor(fill);
		g.fill(new Ellipse2D.Double(center.getX() - r, center.getY() - r, 2. * r, 2. * r));
		g.setColor(darken(fill, 0.7f));
		g.setStroke(new BasicStroke(1f));
		g.draw(new Ellipse2D.Double(center.getX() - r, center.getY() - r, 2. * r, 2. * r));

		if(drawLabel)
			drawLabel(g, center, place.displayName(), r);
	}


	/* ======================================================================
	 *                          Events
	 * ====================================================================== */

	public static void drawEvent(final Graphics2D g, final GeoEventRef event, final Point2D center,
		final boolean selected, final boolean drawLabel){
		if(g == null || event == null || center == null)
			return;

		final GeoMarkerType type = markerTypeOf(event);
		switch(type){
			case RESIDENCE -> drawSquareMarker(g, center, COLOR_RESIDENCE, selected);
			case MIGRATION_STOP -> drawTriangleMarker(g, center, COLOR_MIGRATION, selected);
			case HISTORIC_EVENT -> drawHollowMarker(g, center, COLOR_HISTORIC, selected);
			default -> drawCircleMarker(g, center, COLOR_EVENT, EVENT_RADIUS, selected);
		}

		if(drawLabel)
			drawLabel(g, center, event.kind()
				.getDisplayLabel(), markerRadiusOf(type));
	}


	/* ======================================================================
	 *                          Clusters
	 * ====================================================================== */

	public static void drawCluster(final Graphics2D g, final GeoLayout.Cluster cluster, final boolean selected){
		if(g == null || cluster == null)
			return;

		final Color fill = (selected? COLOR_SELECTED: COLOR_CLUSTER);
		final int r = cluster.radius();
		final Point2D c = cluster.center();
		g.setColor(fill);
		g.fill(new Ellipse2D.Double(c.getX() - r, c.getY() - r, 2. * r, 2. * r));

		g.setColor(COLOR_CLUSTER_TEXT);
		g.setFont(FONT_CLUSTER);
		final FontMetrics fm = g.getFontMetrics();
		final String text = Integer.toString(cluster.size());
		final int tw = fm.stringWidth(text);
		g.drawString(text, (int)c.getX() - tw / 2, (int)c.getY() + fm.getAscent() / 2 - 1);
	}


	/* ======================================================================
	 *                          Shape helpers
	 * ====================================================================== */

	private static void drawCircleMarker(final Graphics2D g, final Point2D center, final Color color,
		final int radius, final boolean selected){
		final int r = radius + (selected? SELECTED_EXTRA: 0);
		g.setColor(selected? COLOR_SELECTED: color);
		g.fill(new Ellipse2D.Double(center.getX() - r, center.getY() - r, 2. * r, 2. * r));
		g.setColor(darken(color, 0.7f));
		g.draw(new Ellipse2D.Double(center.getX() - r, center.getY() - r, 2. * r, 2. * r));
	}

	private static void drawSquareMarker(final Graphics2D g, final Point2D center, final Color color,
		final boolean selected){
		final int h = RESIDENCE_HALF + (selected? SELECTED_EXTRA: 0);
		g.setColor(selected? COLOR_SELECTED: color);
		g.fillRect((int)center.getX() - h, (int)center.getY() - h, 2 * h, 2 * h);
		g.setColor(darken(color, 0.7f));
		g.drawRect((int)center.getX() - h, (int)center.getY() - h, 2 * h, 2 * h);
	}

	private static void drawTriangleMarker(final Graphics2D g, final Point2D center, final Color color,
		final boolean selected){
		final int r = MIGRATION_RADIUS + (selected? SELECTED_EXTRA: 0);
		final java.awt.geom.Path2D path = new java.awt.geom.Path2D.Double();
		path.moveTo(center.getX(), center.getY() - r);
		path.lineTo(center.getX() + r, center.getY() + r);
		path.lineTo(center.getX() - r, center.getY() + r);
		path.closePath();
		g.setColor(selected? COLOR_SELECTED: color);
		g.fill(path);
		g.setColor(darken(color, 0.7f));
		g.draw(path);
	}

	private static void drawHollowMarker(final Graphics2D g, final Point2D center, final Color color,
		final boolean selected){
		final int r = HISTORIC_RADIUS + (selected? SELECTED_EXTRA: 0);
		g.setColor(selected? COLOR_SELECTED: color);
		g.setStroke(new BasicStroke(2f));
		g.draw(new Ellipse2D.Double(center.getX() - r, center.getY() - r, 2. * r, 2. * r));
	}


	/* ======================================================================
	 *                          Classification
	 * ====================================================================== */

	/**
	 * Returns the marker type of the given event, based on its migration
	 * kind.
	 */
	public static GeoMarkerType markerTypeOf(final GeoEventRef event){
		return switch(event.kind()){
			case RESIDENCE -> GeoMarkerType.RESIDENCE;
			case IMMIGRATION, EMIGRATION -> GeoMarkerType.MIGRATION_STOP;
			default -> GeoMarkerType.EVENT;
		};
	}

	/**
	 * Returns whether the given zoom level is sufficiently narrow to draw
	 * labels.
	 */
	public static boolean shouldDrawLabels(final GeoZoomLevel zoom){
		return (zoom.ordinal() >= LABEL_ZOOM_THRESHOLD.ordinal());
	}

	private static int markerRadiusOf(final GeoMarkerType type){
		return switch(type){
			case PLACE -> PLACE_RADIUS;
			case EVENT -> EVENT_RADIUS;
			case RESIDENCE -> RESIDENCE_HALF;
			case MIGRATION_STOP -> MIGRATION_RADIUS;
			case HISTORIC_EVENT -> HISTORIC_RADIUS;
		};
	}


	/* ======================================================================
	 *                          Utility
	 * ====================================================================== */

	private static void drawLabel(final Graphics2D g, final Point2D center, final String text, final int radius){
		if(text == null || text.isEmpty())
			return;
		g.setFont(FONT_LABEL);
		g.setColor(COLOR_LABEL);
		final int x = (int)center.getX() + radius + 3;
		final int y = (int)center.getY() + 4;
		g.drawString(text, x, y);
	}

	private static Color darken(final Color color, final float factor){
		return new Color(
			Math.max(0, (int)(color.getRed() * factor)),
			Math.max(0, (int)(color.getGreen() * factor)),
			Math.max(0, (int)(color.getBlue() * factor)),
			color.getAlpha());
	}

}
