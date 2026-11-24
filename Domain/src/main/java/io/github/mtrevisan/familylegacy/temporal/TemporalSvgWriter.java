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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Draws a {@link TemporalProjectionModel} as an SVG document: the same rows, tracks, bars and background bands that the temporal timelines
 * show on the screen, laid out by {@link TemporalProjectionLayout}, on a fixed axis that goes from the start to the end of the model.
 * <p>
 * The SVG has no script and no external reference, so that it opens in a browser, a vector editor or a document, and prints to a PDF from
 * any of them. Each bar has a {@code <title>}, the tooltip of its label and dates.
 */
public final class TemporalSvgWriter{

	private static final long UNIX_EPOCH_JDN = 2_440_588L;
	private static final int DEFAULT_WIDTH = 1200;
	private static final int MIN_WIDTH = 480;
	private static final int RIGHT_MARGIN = 24;
	private static final int AXIS_HEIGHT = 34;
	private static final int BOTTOM_MARGIN = 16;
	private static final int MIN_TICK_SPACING = 64;
	private static final int[] TICK_STEPS = {1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500, 1000, 2000, 5000};
	private static final int BAND_LABEL_LINES = 3;
	private static final double CHAR_WIDTH = 0.55;

	private static final String COLOR_LIFESPAN = "#8a7a5c";
	private static final String COLOR_EVENT = "#4678be";
	private static final String COLOR_ATTRIBUTE = "#509660";
	private static final String COLOR_CONTEXT = "#be8246";
	private static final String COLOR_MEMBERSHIP = "#8c8c8c";
	private static final String COLOR_BAND_HISTORIC = "#d2b48c";
	private static final String COLOR_BAND_CULTURAL = "#aabedc";
	private static final String COLOR_HEADER = "#f4f0e8";
	private static final String COLOR_BORDER = "#d6d0c4";
	private static final String COLOR_TEXT = "#32281e";
	private static final String COLOR_GRID = "#e4e0d8";


	private TemporalSvgWriter(){}


	/**
	 * @param model	The model to draw.
	 * @return	The SVG document, 1200 pixels wide.
	 */
	public static String write(final TemporalProjectionModel model){
		return write(model, DEFAULT_WIDTH);
	}

	/**
	 * @param model	The model to draw.
	 * @param width	The width of the document in pixels (at least 480).
	 * @return	The SVG document: an empty one, with no rows, if the model is empty.
	 */
	public static String write(final TemporalProjectionModel model, final int width){
		final int totalWidth = Math.max(MIN_WIDTH, width);
		final TemporalProjectionLayout layout = new TemporalProjectionLayout();
		layout.compute(model);

		final int headerWidth = TemporalProjectionLayout.ROW_HEADER_WIDTH;
		final int contentWidth = totalWidth - headerWidth - RIGHT_MARGIN;
		final int contentHeight = layout.totalHeight();
		final int totalHeight = AXIS_HEIGHT + Math.max(contentHeight, 20) + BOTTOM_MARGIN;

		final StringBuilder svg = new StringBuilder(8192)
			.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
			.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(totalWidth).append("\" height=\"").append(totalHeight)
			.append("\" viewBox=\"0 0 ").append(totalWidth).append(' ').append(totalHeight)
			.append("\" font-family=\"Tahoma, Verdana, sans-serif\">\n")
			.append("<rect width=\"100%\" height=\"100%\" fill=\"#ffffff\"/>\n");
		if(model == null || model.isEmpty() || !model.hasDomain()){
			svg.append("</svg>\n");
			return svg.toString();
		}

		final long domainStart = model.domainStart().jdn();
		final long domainEnd = Math.max(model.domainEnd().jdn(), domainStart + 1);
		//a little room at both sides, so that the first and the last bars are not on the edge
		final long padding = Math.max(30L, (domainEnd - domainStart) / 50L);
		final Axis axis = new Axis(domainStart - padding, domainEnd + padding, headerWidth, contentWidth);

		drawGrid(svg, axis, contentHeight);
		drawBands(svg, model.bands(), axis, contentHeight);
		for(final TemporalProjectionLayout.RowLayout rowLayout : layout.rows()){
			final TemporalRow row = model.findRow(rowLayout.entity());
			if(row != null)
				drawRow(svg, row, rowLayout, axis, headerWidth);
		}
		drawAxis(svg, axis);

		return svg.append("</svg>\n")
			.toString();
	}


	/* ======================================================================
	 *                          Drawing
	 * ====================================================================== */

	private static void drawGrid(final StringBuilder svg, final Axis axis, final int contentHeight){
		final int step = tickStep(axis);
		for(int year = firstTick(axis, step); jdnOfYear(year) <= axis.end; year += step){
			final double x = axis.x(jdnOfYear(year));
			if(x < axis.left)
				continue;

			svg.append("<line x1=\"").append(f(x)).append("\" y1=\"").append(AXIS_HEIGHT).append("\" x2=\"").append(f(x))
				.append("\" y2=\"").append(AXIS_HEIGHT + contentHeight).append("\" stroke=\"").append(COLOR_GRID).append("\"/>\n");
		}
	}

	private static void drawAxis(final StringBuilder svg, final Axis axis){
		svg.append("<line x1=\"").append(axis.left).append("\" y1=\"").append(AXIS_HEIGHT).append("\" x2=\"")
			.append(axis.left + axis.width).append("\" y2=\"").append(AXIS_HEIGHT).append("\" stroke=\"").append(COLOR_TEXT)
			.append("\"/>\n");
		final int step = tickStep(axis);
		for(int year = firstTick(axis, step); jdnOfYear(year) <= axis.end; year += step){
			final double x = axis.x(jdnOfYear(year));
			if(x < axis.left)
				continue;

			svg.append("<line x1=\"").append(f(x)).append("\" y1=\"").append(AXIS_HEIGHT - 5).append("\" x2=\"").append(f(x))
				.append("\" y2=\"").append(AXIS_HEIGHT).append("\" stroke=\"").append(COLOR_TEXT).append("\"/>\n")
				.append("<text x=\"").append(f(x)).append("\" y=\"").append(AXIS_HEIGHT - 9)
				.append("\" font-size=\"11\" text-anchor=\"middle\" fill=\"").append(COLOR_TEXT).append("\">").append(year)
				.append("</text>\n");
		}
	}

	private static void drawBands(final StringBuilder svg, final List<TemporalContextBand> bands, final Axis axis, final int contentHeight){
		int index = 0;
		for(final TemporalContextBand band : bands){
			final double x1 = axis.x(band.span().minJdn());
			final double x2 = axis.x(Lifetime.lastDay(band.span()));
			final double width = Math.max(x2 - x1, 2);
			final boolean historic = (band.entity().type() != TemporalEntityType.CULTURAL_NORM);
			final String label = (band.label().isEmpty()? band.kind(): band.label());

			svg.append("<g><title>").append(escape(label + " (" + band.span() + ")")).append("</title>\n")
				.append("<rect x=\"").append(f(x1)).append("\" y=\"").append(AXIS_HEIGHT).append("\" width=\"").append(f(width))
				.append("\" height=\"").append(Math.max(contentHeight, 20)).append("\" fill=\"")
				.append(historic? COLOR_BAND_HISTORIC: COLOR_BAND_CULTURAL).append("\" fill-opacity=\"0.25\" stroke=\"")
				.append(COLOR_BORDER).append("\"/>\n");
			if(!label.isEmpty() && width >= 24)
				svg.append("<text x=\"").append(f(x1 + 3)).append("\" y=\"").append(AXIS_HEIGHT + 11 + (index % BAND_LABEL_LINES) * 11)
					.append("\" font-size=\"10\" font-style=\"italic\" fill=\"#6e6450\">").append(escape(fit(label, width - 6, 10)))
					.append("</text>\n");
			svg.append("</g>\n");
			index ++;
		}
	}

	private static void drawRow(final StringBuilder svg, final TemporalRow row, final TemporalProjectionLayout.RowLayout rowLayout,
			final Axis axis, final int headerWidth){
		final int top = AXIS_HEIGHT + rowLayout.y();
		svg.append("<rect x=\"0\" y=\"").append(top).append("\" width=\"").append(headerWidth).append("\" height=\"")
			.append(rowLayout.height()).append("\" fill=\"").append(COLOR_HEADER).append("\" stroke=\"").append(COLOR_BORDER)
			.append("\"/>\n")
			.append("<text x=\"6\" y=\"").append(top + rowLayout.height() / 2 + 5).append("\" font-size=\"13\" fill=\"")
			.append(COLOR_TEXT).append("\">").append(escape(fit(row.entity().displayLabel(), headerWidth - 12, 13))).append("</text>\n");

		for(final Map.Entry<TemporalTrackType, TemporalProjectionLayout.TrackLayout> trackEntry : rowLayout.tracks().entrySet()){
			final TemporalTrackType type = trackEntry.getKey();
			final TemporalProjectionLayout.TrackLayout track = trackEntry.getValue();
			for(final TemporalEntry entry : track.entryLane().keySet())
				drawEntry(svg, entry, type, AXIS_HEIGHT + track.entryY(entry), axis);
		}
	}

	private static void drawEntry(final StringBuilder svg, final TemporalEntry entry, final TemporalTrackType type, final int y,
			final Axis axis){
		final TemporalSpan span = entry.span();
		final double x1 = axis.x(span.minJdn());
		final double x2 = axis.x(Lifetime.lastDay(span));
		//a day or a month is a few pixels wide: it has to be seen
		final double width = Math.max(x2 - x1, span.isPoint()? 6: 3);
		final String label = (entry.label().isEmpty()? entry.type(): entry.label());
		final int height = TemporalProjectionLayout.ENTRY_HEIGHT;
		final boolean approximate = span.isApproximate();

		svg.append("<g><title>").append(escape(label + " (" + span + ")")).append("</title>\n")
			.append("<rect x=\"").append(f(x1)).append("\" y=\"").append(y).append("\" width=\"").append(f(width))
			.append("\" height=\"").append(height).append("\" rx=\"2\" fill=\"").append(colorOf(type)).append('"')
			.append(approximate? " fill-opacity=\"0.55\" stroke-dasharray=\"3 2\" stroke=\"" + colorOf(type) + '"': StringUtils.EMPTY)
			.append("/>\n");
		if(type == TemporalTrackType.LIFESPAN){
			if(width > 40)
				svg.append("<text x=\"").append(f(x1 + 4)).append("\" y=\"").append(y + 11).append("\" font-size=\"10\" fill=\"#ffffff\">")
					.append(escape(fit(label, width - 8, 10))).append("</text>\n");
		}
		else if(!label.isEmpty())
			svg.append("<text x=\"").append(f(x1 + width + 3)).append("\" y=\"").append(y + 11).append("\" font-size=\"10\" fill=\"")
				.append(COLOR_TEXT).append("\">").append(escape(label)).append("</text>\n");
		svg.append("</g>\n");
	}

	private static String colorOf(final TemporalTrackType type){
		return switch(type){
			case LIFESPAN -> COLOR_LIFESPAN;
			case EVENT -> COLOR_EVENT;
			case ATTRIBUTE -> COLOR_ATTRIBUTE;
			case CONTEXT -> COLOR_CONTEXT;
			case MEMBERSHIP -> COLOR_MEMBERSHIP;
		};
	}


	/* ======================================================================
	 *                          Axis
	 * ====================================================================== */

	/** The year step of the ticks: the smallest one that keeps the labels apart. */
	private static int tickStep(final Axis axis){
		final double pixelsPerYear = axis.width / Math.max(1.0, (axis.end - axis.start) / 365.2425);
		for(final int step : TICK_STEPS)
			if(step * pixelsPerYear >= MIN_TICK_SPACING)
				return step;
		return TICK_STEPS[TICK_STEPS.length - 1];
	}

	private static int firstTick(final Axis axis, final int step){
		final int year = LocalDate.ofEpochDay(axis.start - UNIX_EPOCH_JDN).getYear();
		return Math.floorDiv(year, step) * step;
	}

	private static long jdnOfYear(final int year){
		return LocalDate.of(year, 1, 1).toEpochDay() + UNIX_EPOCH_JDN;
	}


	/**
	 * The horizontal axis: the days are mapped on the width linearly, and an open side (the largest and the smallest day) goes to the edge.
	 */
	private record Axis(long start, long end, int left, int width){

		double x(final long jdn){
			if(jdn == Long.MIN_VALUE)
				return left;
			if(jdn == Long.MAX_VALUE)
				return left + width;

			final double ratio = (double)(jdn - start) / (double)(end - start);
			return left + Math.max(0.0, Math.min(1.0, ratio)) * width;
		}

	}


	/* ======================================================================
	 *                          Text
	 * ====================================================================== */

	/** @return	The text cut to fit in the width (an estimate: the SVG has no font metrics), with an ellipsis if it was cut. */
	private static String fit(final String text, final double width, final int fontSize){
		final int maxChars = (int)Math.max(1, width / (fontSize * CHAR_WIDTH));
		if(text.length() <= maxChars)
			return text;

		return text.substring(0, Math.max(0, maxChars - 1)) + "…";
	}

	private static String escape(final String text){
		final StringBuilder sb = new StringBuilder(text.length() + 8);
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			switch(c){
				case '&' -> sb.append("&amp;");
				case '<' -> sb.append("&lt;");
				case '>' -> sb.append("&gt;");
				case '"' -> sb.append("&quot;");
				case '\'' -> sb.append("&apos;");
				default -> {
					//the characters that XML 1.0 does not allow are dropped
					if(c >= 0x20 || c == '\t' || c == '\n' || c == '\r')
						sb.append(c);
				}
			}
		}
		return sb.toString();
	}

	private static String f(final double value){
		return String.format(Locale.ROOT, "%.1f", value);
	}

}
