package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;


/**
 * Draws a single {@link TemporalSpan} with the exhaustive visual encoding
 * required by the FLEF protocol.
 * <p>
 * Encoding rules:
 * <ul>
 *   <li>{@link TemporalSpanKind#POINT} with fine precision
 *       ({@link DatePrecision#DAY}, {@link DatePrecision#MONTH},
 *       {@link DatePrecision#YEAR}) — a filled diamond marker at the
 *       midpoint of the rectangle.</li>
 *   <li>{@link TemporalSpanKind#POINT} with reduced precision
 *       ({@link DatePrecision#DECADE}, {@link DatePrecision#CENTURY}) —
 *       a shaded bar with a symmetric gradient from the center outwards.</li>
 *   <li>{@link TemporalSpanKind#BOUNDED} — a bar with faded extremities
 *       (uncertainty about the exact bounds).</li>
 *   <li>{@link TemporalSpanKind#SPANNING} — a bar with sharp extremities
 *       (actual duration).</li>
 *   <li>{@link TemporalSpan#isApproximate()} — a soft halo behind the
 *       marker or bar.</li>
 *   <li>{@link TemporalSpan#status()} — the fill alpha and the outline
 *       stroke vary: {@code active} solid, {@code ended} dashed,
 *       {@code unknown} dotted.</li>
 * </ul>
 */
public final class TemporalSpanRenderer{

	/** Color of a selected span. */
	public static final Color COLOR_SELECTED = new Color(220, 100, 60);

	/** Halo color for approximate spans. */
	public static final Color COLOR_APPROX_HALO = new Color(255, 200, 100, 70);


	private static final int MARKER_MIN_SIZE = 6;
	private static final int CORNER_ARC = 3;
	private static final int HALO_PADDING = 4;
	private static final int FADE_WIDTH = 12;


	private TemporalSpanRenderer(){
	}


	/**
	 * Draws a single span into the given screen rectangle.
	 *
	 * @param g         the graphics context
	 * @param rect      the screen rectangle (already translated to absolute coordinates)
	 * @param span      the span to draw (must not be {@code null})
	 * @param baseColor the base color, chosen by the caller based on the track
	 * @param selected  whether the span is currently selected
	 */
	public static void drawSpan(final Graphics2D g, final Rectangle rect, final TemporalSpan span,
		final Color baseColor, final boolean selected){
		if(g == null || rect == null || span == null || baseColor == null)
			return;

		final Color color = (selected? COLOR_SELECTED: baseColor);

		final Paint originalPaint = g.getPaint();
		final Stroke originalStroke = g.getStroke();
		try{
			// 1. Approximate halo (behind everything)
			if(span.isApproximate())
				drawApproximationHalo(g, rect);

			// 2. Body
			switch(span.kind()){
				case POINT -> {
					if(span.start().isReduced())
						drawReducedPrecisionBar(g, rect, color);
					else
						drawPointMarker(g, rect, color);
				}
				case BOUNDED -> drawBoundedBar(g, rect, color);
				case SPANNING -> drawSpanningBar(g, rect, color);
			}
		}
		finally{
			g.setPaint(originalPaint);
			g.setStroke(originalStroke);
		}
	}


	/* ======================================================================
	 *                          Bodies
	 * ====================================================================== */

	private static void drawPointMarker(final Graphics2D g, final Rectangle rect, final Color color){
		final int cx = rect.x + rect.width / 2;
		final int cy = rect.y + rect.height / 2;
		final int half = Math.max(MARKER_MIN_SIZE, rect.height / 2);

		final Path2D diamond = new Path2D.Double();
		diamond.moveTo(cx, cy - half);
		diamond.lineTo(cx + half, cy);
		diamond.lineTo(cx, cy + half);
		diamond.lineTo(cx - half, cy);
		diamond.closePath();

		g.setColor(color);
		g.fill(diamond);
	}

	private static void drawReducedPrecisionBar(final Graphics2D g, final Rectangle rect, final Color color){
		// Symmetric gradient: transparent at the ends, solid at the center.
		final GradientPaint gradient = new GradientPaint(
			rect.x, 0, withAlpha(color, 60),
			rect.x + rect.width / 2f, 0, color,
			true);
		g.setPaint(gradient);
		g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);

		g.setColor(color);
		g.setStroke(new BasicStroke(1f));
		g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);
	}

	private static void drawSpanningBar(final Graphics2D g, final Rectangle rect, final Color color){
		g.setColor(color);
		g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);

		g.setColor(darken(color, 0.7f));
		g.setStroke(new BasicStroke(1f));
		g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);
	}

	private static void drawBoundedBar(final Graphics2D g, final Rectangle rect, final Color color){
		// Solid body, then fade both ends to suggest uncertainty.
		g.setColor(color);
		g.fillRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);

		final int fade = Math.min(FADE_WIDTH, Math.max(2, rect.width / 3));

		// Left fade
		final GradientPaint leftFade = new GradientPaint(
			rect.x, 0, withAlpha(color, 0),
			rect.x + fade, 0, withAlpha(color, 255));
		g.setPaint(leftFade);
		g.fillRect(rect.x, rect.y, fade, rect.height);

		// Right fade
		final GradientPaint rightFade = new GradientPaint(
			rect.x + rect.width - fade, 0, withAlpha(color, 255),
			rect.x + rect.width, 0, withAlpha(color, 0));
		g.setPaint(rightFade);
		g.fillRect(rect.x + rect.width - fade, rect.y, fade, rect.height);

		// Dashed outline to reinforce the "uncertainty" reading.
		g.setColor(darken(color, 0.7f));
		g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
			10f, new float[]{4f, 3f}, 0f));
		g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, CORNER_ARC, CORNER_ARC);
	}

	private static void drawApproximationHalo(final Graphics2D g, final Rectangle rect){
		g.setColor(COLOR_APPROX_HALO);
		g.fill(new Ellipse2D.Double(
			rect.x - HALO_PADDING,
			rect.y - HALO_PADDING,
			rect.width + 2. * HALO_PADDING,
			rect.height + 2. * HALO_PADDING));
	}


	/* ======================================================================
	 *                          Color helpers
	 * ====================================================================== */

	static Color withAlpha(final Color color, final int alpha){
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	static Color darken(final Color color, final float factor){
		return new Color(
			Math.max(0, (int)(color.getRed() * factor)),
			Math.max(0, (int)(color.getGreen() * factor)),
			Math.max(0, (int)(color.getBlue() * factor)),
			color.getAlpha());
	}

}
