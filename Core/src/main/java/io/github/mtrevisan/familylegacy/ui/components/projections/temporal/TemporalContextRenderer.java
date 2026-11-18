/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.temporal.TemporalContextBand;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.List;


/**
 * Draws the contextual layer of the timeline panel: the {@link TemporalContextBand} backgrounds, behind the row lifelines, one
 * translucent rectangle per historic event or cultural norm, spanning the full vertical extent of the content area.
 */
public final class TemporalContextRenderer{

	/** Fill color for historic event bands. */
	public static final Color COLOR_BAND_HISTORIC = new Color(210, 180, 140, 45);

	/** Fill color for cultural norm bands. */
	public static final Color COLOR_BAND_CULTURAL = new Color(170, 190, 220, 45);

	/** Border color shared by all bands. */
	public static final Color COLOR_BAND_BORDER = new Color(150, 140, 120, 90);

	/** Color of the label of a band. */
	public static final Color COLOR_BAND_LABEL = new Color(110, 100, 80);

	private static final int LABEL_MIN_WIDTH = 24;
	private static final int LABEL_LINES = 3;


	private TemporalContextRenderer(){
	}


	/**
	 * Draws the background bands. Call this before drawing rows so that the
	 * bands appear behind the entries.
	 *
	 * @param g             the graphics context
	 * @param bands         the bands to draw (may be {@code null} or empty)
	 * @param axis          the temporal axis
	 * @param contentBounds the rectangle of the temporal content
	 */
	public static void drawBands(final Graphics2D g, final List<TemporalContextBand> bands,
		final TemporalAxis axis, final Rectangle contentBounds){
		if(g == null || bands == null || bands.isEmpty() || axis == null || contentBounds == null)
			return;

		final Stroke originalStroke = g.getStroke();
		try{
			int index = 0;
			for(final TemporalContextBand band : bands){
				final Rectangle rect = axis.spanToRect(band.span(), contentBounds.y, contentBounds.height);
				if(rect == null)
					continue;
				rect.x += contentBounds.x;

				g.setColor(colorForBand(band));
				g.fillRect(rect.x, rect.y, rect.width, rect.height);

				g.setColor(COLOR_BAND_BORDER);
				g.setStroke(new BasicStroke(1f));
				g.drawRect(rect.x, rect.y, rect.width, rect.height);

				drawLabel(g, band, rect, index ++);
			}
		}
		finally{
			g.setStroke(originalStroke);
		}
	}

	/** The name of the band, at its top, on one of a few lines so that the names of bands that overlap can be read. */
	private static void drawLabel(final Graphics2D g, final TemporalContextBand band, final Rectangle rect, final int index){
		final String label = (band.label().isEmpty()? band.kind(): band.label());
		if(label.isEmpty() || rect.width < LABEL_MIN_WIDTH)
			return;

		final Font original = g.getFont();
		try{
			g.setFont(original.deriveFont(Font.ITALIC, 10f));
			final FontMetrics fm = g.getFontMetrics();
			final int maxWidth = rect.width - 6;
			String text = label;
			while(text.length() > 1 && fm.stringWidth(text) > maxWidth)
				text = text.substring(0, text.length() - 1);
			if(text.length() < label.length())
				text = text.substring(0, Math.max(0, text.length() - 1)) + I18N.t("dialog.ellipsis");

			g.setColor(COLOR_BAND_LABEL);
			g.drawString(text, rect.x + 3, rect.y + fm.getAscent() + 1 + (index % LABEL_LINES) * fm.getHeight());
		}
		finally{
			g.setFont(original);
		}
	}

	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static Color colorForBand(final TemporalContextBand band){
		return switch(band.entity().type()){
			case HISTORIC_EVENT -> COLOR_BAND_HISTORIC;
			case CULTURAL_NORM -> COLOR_BAND_CULTURAL;
			default -> COLOR_BAND_HISTORIC;
		};
	}

}
