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
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.temporal.TemporalConnection;
import io.github.mtrevisan.familylegacy.temporal.TemporalContextBand;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionLayout;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import io.github.mtrevisan.familylegacy.ui.helpers.TimeLabels;
import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.JulianFields;
import java.util.List;
import java.util.Map;


/**
 * Static hit‑testing and tooltip utilities for the General Temporal
 * Projection.
 * <p>
 * The handler is stateless: callers pass the current model, layout, axis and
 * content bounds on every query. Hit testing follows the visual stacking
 * order (connections first, then entries), so that the topmost element under
 * the cursor is returned.
 */
public final class TemporalInteractionHandler{

	/** Tolerance, in pixels, applied to connection hit testing. */
	private static final int CONNECTION_HIT_TOLERANCE = 4;
	/** Tolerance, in pixels, on each side of a band: a historic event with a single date is a line. */
	private static final int BAND_HIT_TOLERANCE = 2;


	private TemporalInteractionHandler(){
	}


	/**
	 * Returns the topmost projection element under the given point, or
	 * {@code null} if the point does not hit anything.
	 *
	 * @param x             the X coordinate, in content‑canvas pixels
	 * @param y             the Y coordinate, in content‑canvas pixels
	 * @param model         the projection model (must not be {@code null})
	 * @param layout        the projection layout (must not be {@code null})
	 * @param axis          the temporal axis (must not be {@code null})
	 * @param contentBounds the rectangle of the temporal content
	 * @return the hit element, or {@code null}
	 */
	public static TemporalProjectionRef hitTest(final int x, final int y,
			final TemporalProjectionModel model, final TemporalProjectionLayout layout,
			final TemporalAxis axis, final Rectangle contentBounds){
		if(model == null || layout == null || axis == null || contentBounds == null)
			return null;

		// 1. Entries (top layer for clicking purposes)
		final List<TemporalRow> rows = model.rows();
		for(final TemporalRow row : rows){
			final TemporalProjectionLayout.RowLayout rowLayout = layout.findRow(row.entity());
			if(rowLayout == null)
				continue;
			for(final Map.Entry<TemporalTrackType, TemporalProjectionLayout.TrackLayout> entry
				: rowLayout.tracks().entrySet()){
				final TemporalProjectionLayout.TrackLayout trackLayout = entry.getValue();
				for(final TemporalEntry candidate : trackLayout.entryLane().keySet()){
					final int entryY = contentBounds.y + trackLayout.entryY(candidate);
					final Rectangle rect = axis.spanToRect(candidate.span(), entryY,
						TemporalProjectionLayout.ENTRY_HEIGHT);
					if(rect == null)
						continue;
					rect.x += contentBounds.x;
					if(TemporalSpanRenderer.hitBounds(rect, candidate.span()).contains(x, y))
						return new TemporalProjectionRef.EntryRef(row.entity(), candidate);
				}
			}
		}

		// 2. Connections
		final List<TemporalConnection> connections = model.connections();
		for(final TemporalConnection connection : connections){
			if(hitConnection(x, y, connection, layout, axis, contentBounds))
				return new TemporalProjectionRef.ConnectionRef(connection);
		}

		// 3. Context bands (the background): the narrowest one if more overlap
		TemporalContextBand narrowest = null;
		long narrowestWidth = Long.MAX_VALUE;
		for(final TemporalContextBand band : model.bands()){
			final Rectangle rect = axis.spanToRect(band.span(), contentBounds.y, contentBounds.height);
			if(rect == null)
				continue;
			rect.x += contentBounds.x;
			rect.grow(BAND_HIT_TOLERANCE, 0);
			if(rect.contains(x, y) && rect.width < narrowestWidth){
				narrowest = band;
				narrowestWidth = rect.width;
			}
		}
		return (narrowest != null? new TemporalProjectionRef.BandRef(narrowest): null);
	}

	/**
	 * Builds an HTML tooltip for the given element.
	 *
	 * @param ref the element (must not be {@code null})
	 * @return the tooltip text, or {@code null} if no text is available
	 */
	public static String buildTooltip(final TemporalProjectionRef ref){
		if(ref == null)
			return null;

		final StringBuilder sb = new StringBuilder("<html>");
		if(ref instanceof TemporalProjectionRef.EntryRef entryRef){
			final TemporalEntry entry = entryRef.entry();
			sb.append("<b>").append(escape(entry.label())).append("</b>");
			if(!entry.type().isEmpty())
				sb.append("<br>").append(I18N.l("tooltip.field.type")).append(StringUtils.SPACE).append(escape(entry.type()));
			if(entry.hasRole())
				sb.append("<br>").append(I18N.l("tooltip.field.role")).append(StringUtils.SPACE).append(escape(entry.role()));
			sb.append("<br>").append(escape(describeSpan(entry.span())));
			if(entry.hasNegativeEvidence())
				sb.append("<br><i>").append(I18N.t("text.tip.negative_evidence")).append("</i>");
			sb.append("<br><i>").append(I18N.t("text.tip.edit_double")).append("</i>");
			return sb.append("</html>").toString();
		}
		if(ref instanceof TemporalProjectionRef.ConnectionRef connectionRef){
			final TemporalConnection connection = connectionRef.connection();
			sb.append("<b>").append(escape(connection.relationshipType())).append("</b>");
			if(connection.hasRole())
				sb.append("<br>").append(I18N.l("tooltip.field.role")).append(StringUtils.SPACE).append(escape(connection.role()));
			sb.append("<br>").append(I18N.l("tooltip.field.status")).append(StringUtils.SPACE).append(escape(connection.span().status()));
			sb.append("<br>").append(escape(describeSpan(connection.span())));
			sb.append("<br><i>").append(I18N.t("text.tip.edit_double")).append("</i>");
			return sb.append("</html>").toString();
		}
		if(ref instanceof TemporalProjectionRef.BandRef bandRef){
			final TemporalContextBand band = bandRef.band();
			sb.append("<b>").append(escape(band.label().isEmpty()? band.kind(): band.label())).append("</b>");
			if(!band.kind().isEmpty() && !band.kind().equals(band.label()))
				sb.append("<br>").append(I18N.l("tooltip.field.type")).append(StringUtils.SPACE).append(escape(band.kind()));
			sb.append("<br>").append(escape(describeSpan(band.span())));
			sb.append("<br><i>").append(I18N.t("text.tip.edit_double")).append("</i>");
			return sb.append("</html>").toString();
		}
		if(ref instanceof TemporalProjectionRef.RowRef rowRef)
			return "<html><b>" + escape(rowRef.entity().displayLabel()) + "</b></html>";

		return null;
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static boolean hitConnection(final int x, final int y, final TemporalConnection connection,
		final TemporalProjectionLayout layout, final TemporalAxis axis, final Rectangle contentBounds){
		final int sourceY = layout.rowCenterY(connection.sourceRow());
		final int targetY = layout.rowCenterY(connection.targetRow());
		if(sourceY < 0 || targetY < 0)
			return false;

		final int screenSourceY = contentBounds.y + sourceY;
		final int screenTargetY = contentBounds.y + targetY;

		final long minJdn = connection.span().minJdn();
		final long maxJdn = connection.span().maxJdn();

		final int startX = (minJdn == Long.MIN_VALUE
			? contentBounds.x
			: contentBounds.x + axis.jdnToX(minJdn));
		final int endX = (maxJdn == Long.MAX_VALUE
			? contentBounds.x + contentBounds.width
			: contentBounds.x + axis.jdnToX(maxJdn));
		final int midX = (startX + endX) / 2;

		//the three segments of the polyline (the same drawn by the renderer), each expanded by the tolerance
		return (isNearHorizontal(x, y, startX, midX, screenSourceY)
			|| isNearVertical(x, y, midX, screenSourceY, screenTargetY)
			|| isNearHorizontal(x, y, midX, endX, screenTargetY));
	}

	private static boolean isNearHorizontal(final int x, final int y, final int x1, final int x2, final int lineY){
		return (Math.abs(y - lineY) <= CONNECTION_HIT_TOLERANCE
			&& x >= Math.min(x1, x2) - CONNECTION_HIT_TOLERANCE && x <= Math.max(x1, x2) + CONNECTION_HIT_TOLERANCE);
	}

	private static boolean isNearVertical(final int x, final int y, final int lineX, final int y1, final int y2){
		return (Math.abs(x - lineX) <= CONNECTION_HIT_TOLERANCE
			&& y >= Math.min(y1, y2) - CONNECTION_HIT_TOLERANCE && y <= Math.max(y1, y2) + CONNECTION_HIT_TOLERANCE);
	}

	private static String escape(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private static String describeSpan(final TemporalSpan span){
		if(span == null)
			return StringUtils.EMPTY;

		if(span.isPoint())
			return I18N.l("tooltip.field.date") + StringUtils.SPACE + formatDate(span.start());

		final String startText = (span.start() != null && span.start().jdn() != Long.MIN_VALUE? formatDate(span.start()): I18N.t("text.tip.open_ended"));
		final String endText = (span.end() != null && span.end().jdn() != Long.MAX_VALUE? formatDate(span.end()): I18N.t("text.tip.open_ended"));
		return span.kind() + ": " + startText + " .. " + endText + " [" + span.status() + "]";
	}

	/** @return	The date as it was written, if known, otherwise as the Gregorian date at its precision (year, month or day). */
	static String formatDate(final NormalizedDate date){
		String text = date.originalText();
		if(StringUtils.isBlank(text)){
			final long jdn = date.jdn();
			if(jdn <= -1_000_000_000L || jdn >= 1_000_000_000L)
				text = Long.toString(jdn);
			else{
				final LocalDate day = LocalDate.MIN.with(JulianFields.JULIAN_DAY, jdn);
				text = switch(date.precision()){
					case DAY -> day.toString();
					case MONTH -> YearMonth.from(day).toString();
					case YEAR -> Integer.toString(day.getYear());
					case DECADE -> (Math.floorDiv(day.getYear(), 10) * 10) + "s";
					case CENTURY -> TimeLabels.century(Math.floorDiv(day.getYear(), 100) + 1);
				};
			}
		}
		return text + (date.approximate()? " ~": StringUtils.EMPTY) + (date.isGregorian()? StringUtils.EMPTY: " (" + date.calendar() + ")");
	}

}
