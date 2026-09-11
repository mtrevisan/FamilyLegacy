package io.github.mtrevisan.familylegacy.v2.ui.components.projections.geo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Draws migration routes as polylines with numbered stops.
 * <p>
 * Each stop is drawn as a small filled circle with its chronological
 * index, on top of the polyline that connects consecutive stops. The line
 * uses a slightly transparent color so it does not obscure the markers
 * below.
 */
public final class GeoRouteRenderer{

	/** Color of the route polyline. */
	public static final Color COLOR_ROUTE = new Color(60, 90, 150, 200);

	/** Color of the route stop markers. */
	public static final Color COLOR_STOP = new Color(30, 60, 120);

	/** Color of the stop index text. */
	public static final Color COLOR_STOP_TEXT = Color.WHITE;

	private static final float ROUTE_THICKNESS = 2.f;
	private static final int STOP_RADIUS = 8;
	private static final Font FONT_INDEX = new Font("Tahoma", Font.BOLD, 10);


	private GeoRouteRenderer(){}


	/**
	 * Draws a migration route.
	 *
	 * @param g          the graphics context
	 * @param route      the route to draw
	 * @param projection the projection
	 */
	public static void draw(final Graphics2D g, final GeoMigrationRoute route, final GeoProjection projection){
		if(g == null || route == null || projection == null || route.isEmpty())
			return;

		final List<GeoEventRef> stops = route.stops();

		// 1. Polyline.
		g.setColor(COLOR_ROUTE);
		g.setStroke(new BasicStroke(ROUTE_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Point2D previous = null;
		for(final GeoEventRef stop : stops){
			if(!stop.hasPlace())
				continue;
			final Point2D current = projection.geoToScreen(stop.placeRef()
				.coordinate());
			if(previous != null)
				g.draw(new Line2D.Double(previous, current));
			previous = current;
		}

		// 2. Numbered stops.
		int index = 1;
		for(final GeoEventRef stop : stops){
			if(!stop.hasPlace())
				continue;
			final Point2D center = projection.geoToScreen(stop.placeRef()
				.coordinate());
			drawStop(g, center, index);
			index ++;
		}
	}


	private static void drawStop(final Graphics2D g, final Point2D center, final int index){
		g.setColor(COLOR_STOP);
		g.fill(new Ellipse2D.Double(
			center.getX() - STOP_RADIUS, center.getY() - STOP_RADIUS,
			2. * STOP_RADIUS, 2. * STOP_RADIUS));

		g.setFont(FONT_INDEX);
		g.setColor(COLOR_STOP_TEXT);
		final FontMetrics fm = g.getFontMetrics();
		final String text = Integer.toString(index);
		final int w = fm.stringWidth(text);
		g.drawString(text, (int)center.getX() - w / 2, (int)center.getY() + fm.getAscent() / 2 - 1);
	}

}
