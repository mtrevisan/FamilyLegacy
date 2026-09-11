package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;


/**
 * Draws the horizontal time axis of the General Temporal Projection: the
 * strip at the top of the panel with tick marks and labels, and the faint
 * vertical grid lines that extend through the content area.
 * <p>
 * The renderer queries {@link TemporalAxis#computeTicks()} on every repaint,
 * so zoom and pan are reflected immediately without any caching.
 */
public final class TemporalAxisRenderer{

	/** Background color of the axis strip. */
	public static final Color COLOR_STRIP_BACKGROUND = new Color(248, 246, 240);

	/** Color of the axis baseline and of the tick marks. */
	public static final Color COLOR_AXIS_LINE = new Color(140, 130, 110);

	/** Color of the tick labels. */
	public static final Color COLOR_AXIS_LABEL = new Color(70, 60, 40);

	/** Color of the vertical grid lines. */
	public static final Color COLOR_GRID_MINOR = new Color(225, 220, 210);

	/** Font for the axis tick labels. */
	private static final Font FONT_TICK = new Font("Tahoma", Font.PLAIN, 11);


	private static final int STRIP_BASELINE_THICKNESS = 1;
	private static final int TICK_MARK_HEIGHT = 6;
	private static final int LABEL_PADDING = 4;


	private TemporalAxisRenderer(){
	}


	/**
	 * Draws the axis strip: background, baseline, tick marks and labels.
	 * <p>
	 * The tick font is applied for the whole tick loop and restored afterwards,
	 * so that the caller's graphics state is left unchanged.
	 *
	 * @param g             the graphics context
	 * @param axis          the temporal axis
	 * @param axisBounds    the rectangle reserved for the strip
	 * @param contentBounds the rectangle of the content area (used for the X origin)
	 */
	public static void drawAxisStrip(final Graphics2D g, final TemporalAxis axis,
		final Rectangle axisBounds, final Rectangle contentBounds){
		if(g == null || axis == null || axisBounds == null || contentBounds == null)
			return;

		// Background
		g.setColor(COLOR_STRIP_BACKGROUND);
		g.fillRect(axisBounds.x, axisBounds.y, axisBounds.width, axisBounds.height);

		// Baseline at the bottom of the strip
		g.setColor(COLOR_AXIS_LINE);
		g.setStroke(new BasicStroke(STRIP_BASELINE_THICKNESS));
		final int baselineY = axisBounds.y + axisBounds.height - 1;
		g.drawLine(axisBounds.x, baselineY, axisBounds.x + axisBounds.width - 1, baselineY);

		if(axis.isEmpty())
			return;

		final List<TemporalAxis.Tick> ticks = axis.computeTicks();

		final Font originalFont = g.getFont();
		final Paint originalPaint = g.getPaint();
		final Stroke originalStroke = g.getStroke();
		try{
			g.setFont(FONT_TICK);
			final FontMetrics fm = g.getFontMetrics();

			for(final TemporalAxis.Tick tick : ticks){
				final int x = contentBounds.x + tick.x();
				if(x < contentBounds.x || x > contentBounds.x + contentBounds.width)
					continue;

				// Tick mark
				g.setColor(COLOR_AXIS_LINE);
				g.drawLine(x, baselineY - TICK_MARK_HEIGHT, x, baselineY);

				// Label
				g.setColor(COLOR_AXIS_LABEL);
				final int labelWidth = fm.stringWidth(tick.label());
				final int labelX = x - labelWidth / 2;
				final int labelY = axisBounds.y + fm.getAscent() + LABEL_PADDING;
				g.drawString(tick.label(), labelX, labelY);
			}
		}
		finally{
			g.setFont(originalFont);
			g.setPaint(originalPaint);
			g.setStroke(originalStroke);
		}
	}

	/**
	 * Draws faint vertical grid lines aligned with the current ticks, over
	 * the whole content area.
	 *
	 * @param g             the graphics context
	 * @param axis          the temporal axis
	 * @param contentBounds the rectangle of the content area
	 */
	public static void drawVerticalGrid(final Graphics2D g, final TemporalAxis axis,
		final Rectangle contentBounds){
		if(g == null || axis == null || contentBounds == null || axis.isEmpty())
			return;

		g.setColor(COLOR_GRID_MINOR);
		g.setStroke(new BasicStroke(1f));

		final List<TemporalAxis.Tick> ticks = axis.computeTicks();
		for(final TemporalAxis.Tick tick : ticks){
			final int x = contentBounds.x + tick.x();
			if(x < contentBounds.x || x > contentBounds.x + contentBounds.width)
				continue;
			g.drawLine(x, contentBounds.y, x, contentBounds.y + contentBounds.height);
		}
	}

}
