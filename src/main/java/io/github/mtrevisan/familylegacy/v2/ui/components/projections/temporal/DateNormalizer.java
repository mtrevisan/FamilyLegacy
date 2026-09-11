package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;


/**
 * Converts a FLEF {@code DateStructure} into a {@link TemporalSpan} ready
 * for the General Temporal Projection.
 * <p>
 * Handles all three alternatives of the {@code DateValue} union
 * ({@code point}, {@code bounded}, {@code spanning}) and all three
 * alternatives of {@code SingleDate} ({@code full_date}, {@code decade},
 * {@code century}). Approximation ({@code basis}, {@code margin}) and
 * non‑Gregorian calendars are preserved on the resulting
 * {@link NormalizedDate}.
 * <p>
 * The class does not mutate the FLEF model and is stateless.
 */
public final class DateNormalizer{

	// Tag paths inside a FLEF DateStructure.
	private static final String TAG_VALUE = "value";
	private static final String TAG_ORIGINAL = "original_text";
	private static final String TAG_POINT = "point";
	private static final String TAG_BOUNDED = "bounded";
	private static final String TAG_SPANNING = "spanning";
	private static final String TAG_FULL_DATE = "full_date";
	private static final String TAG_DECADE = "decade";
	private static final String TAG_CENTURY = "century";
	private static final String TAG_APPROXIMATE = "approximate";
	private static final String TAG_CALENDAR = "calendar";
	private static final String TAG_START_YEAR = "start_year";
	private static final String TAG_ORDINAL = "ordinal";
	private static final String TAG_PART = "part";
	private static final String TAG_NOT_BEFORE = "not_before";
	private static final String TAG_NOT_AFTER = "not_after";
	private static final String TAG_FROM = "from";
	private static final String TAG_TO = "to";
	private static final String TAG_BASIS = "basis";
	private static final String TAG_MARGIN = "margin";


	/**
	 * Normalizes a FLEF {@code DateStructure} into a temporal span.
	 *
	 * @param dateStructure the date structure record (may be {@code null})
	 * @return the normalized span, or {@code null} if the structure is absent
	 * or carries no recognizable date value
	 */
	public TemporalSpan normalize(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return null;

		final String originalText = FLEFRecordHelper.getChildValue(dateStructure, TAG_ORIGINAL);
		final FLEFRecord value = FLEFRecordHelper.findChild(dateStructure, TAG_VALUE);
		if(value == null)
			return null;

		final FLEFRecord point = FLEFRecordHelper.findChild(value, TAG_POINT);
		if(point != null)
			return normalizePoint(point, originalText);

		final FLEFRecord bounded = FLEFRecordHelper.findChild(value, TAG_BOUNDED);
		if(bounded != null)
			return normalizeBounded(bounded, originalText);

		final FLEFRecord spanning = FLEFRecordHelper.findChild(value, TAG_SPANNING);
		if(spanning != null)
			return normalizeSpanning(spanning, originalText);

		return null;
	}

	/**
	 * Returns the {@code original_text} of a date structure, when present.
	 *
	 * @param dateStructure the date structure record
	 * @return the original source expression, or {@code null}
	 */
	public String extractOriginalText(final FLEFRecord dateStructure){
		return (dateStructure != null? FLEFRecordHelper.getChildValue(dateStructure, TAG_ORIGINAL): null);
	}


	/* ======================================================================
	 *                       DateValue variants
	 * ====================================================================== */

	private TemporalSpan normalizePoint(final FLEFRecord point, final String originalText){
		final NormalizedDate date = normalizeSingleDate(point, originalText);
		return (date != null? TemporalSpan.point(date): null);
	}

	private TemporalSpan normalizeBounded(final FLEFRecord bounded, final String originalText){
		final NormalizedDate notBefore = normalizeSubSingleDate(bounded, TAG_NOT_BEFORE, originalText);
		final NormalizedDate notAfter = normalizeSubSingleDate(bounded, TAG_NOT_AFTER, originalText);
		if(notBefore == null && notAfter == null)
			return null;
		return TemporalSpan.bounded(notBefore, notAfter);
	}

	private TemporalSpan normalizeSpanning(final FLEFRecord spanning, final String originalText){
		final NormalizedDate from = normalizeSubSingleDate(spanning, TAG_FROM, originalText);
		final NormalizedDate to = normalizeSubSingleDate(spanning, TAG_TO, originalText);
		if(from == null && to == null)
			return null;
		return TemporalSpan.spanning(from, to);
	}

	private NormalizedDate normalizeSubSingleDate(final FLEFRecord parent, final String tag, final String originalText){
		final FLEFRecord child = FLEFRecordHelper.findChild(parent, tag);
		return (child != null? normalizeSingleDate(child, originalText): null);
	}


	/* ======================================================================
	 *                       SingleDate variants
	 * ====================================================================== */

	private NormalizedDate normalizeSingleDate(final FLEFRecord singleDate, final String originalText){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(singleDate, TAG_FULL_DATE);
		if(fullDate != null)
			return normalizeFullDate(fullDate, originalText);

		final FLEFRecord decade = FLEFRecordHelper.findChild(singleDate, TAG_DECADE);
		if(decade != null)
			return normalizeDecade(decade, originalText);

		final FLEFRecord century = FLEFRecordHelper.findChild(singleDate, TAG_CENTURY);
		if(century != null)
			return normalizeCentury(century, originalText);

		return null;
	}

	private NormalizedDate normalizeFullDate(final FLEFRecord fullDate, final String originalText){
		final String raw = FLEFRecordHelper.getChildValue(fullDate, TAG_VALUE);
		if(raw == null || raw.isBlank())
			return null;

		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(fullDate, TAG_CALENDAR));
		final int[] ymd = parseHistoricalDate(raw);
		if(ymd == null)
			return null;

		final DatePrecision precision = (ymd[2] > 0? DatePrecision.DAY
			: (ymd[1] > 0? DatePrecision.MONTH: DatePrecision.YEAR));
		final long jdn = CalendarConverter.toJdn(calendar, ymd[0], Math.max(1, ymd[1]), Math.max(1, ymd[2]));

		return applyApproximation(fullDate, jdn, precision, calendar, originalText);
	}

	private NormalizedDate normalizeDecade(final FLEFRecord decade, final String originalText){
		final String rawYear = FLEFRecordHelper.getChildValue(decade, TAG_START_YEAR);
		if(rawYear == null)
			return null;
		final int startYear = Integer.parseInt(rawYear.trim());
		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(decade, TAG_CALENDAR));
		final long jdn = CalendarConverter.toJdnStartOfYear(calendar, startYear);
		return applyApproximation(decade, jdn, DatePrecision.DECADE, calendar, originalText);
	}

	private NormalizedDate normalizeCentury(final FLEFRecord century, final String originalText){
		final String rawOrdinal = FLEFRecordHelper.getChildValue(century, TAG_ORDINAL);
		if(rawOrdinal == null)
			return null;
		final int ordinal = Integer.parseInt(rawOrdinal.trim());
		// Century N of the common era covers years (N-1)*100+1 .. N*100.
		final int startYear = (ordinal - 1) * 100 + 1;
		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(century, TAG_CALENDAR));
		final long jdn = CalendarConverter.toJdnStartOfYear(calendar, startYear);
		return applyApproximation(century, jdn, DatePrecision.CENTURY, calendar, originalText);
	}


	/* ======================================================================
	 *                       Helpers
	 * ====================================================================== */

	private static NormalizedDate applyApproximation(final FLEFRecord container, final long jdn,
		final DatePrecision precision, final String calendar, final String originalText){
		final FLEFRecord approx = FLEFRecordHelper.findChild(container, TAG_APPROXIMATE);
		if(approx == null)
			return NormalizedDate.exact(jdn, precision, calendar)
				.withOriginalText(originalText);

		final String basis = FLEFRecordHelper.getChildValue(approx, TAG_BASIS);
		final String margin = FLEFRecordHelper.getChildValue(approx, TAG_MARGIN);
		return NormalizedDate.approximated(jdn, precision, calendar, basis, margin)
			.withOriginalText(originalText);
	}

	private static String defaultCalendar(final String raw){
		return (raw != null && !raw.isBlank()? raw.toLowerCase(): NormalizedDate.CALENDAR_GREGORIAN);
	}

	/**
	 * Parses a reduced‑precision {@code HistoricalDate} string into
	 * {@code [year, month, day]}. Missing components are returned as 0.
	 * <p>
	 * Accepted forms: {@code YYYY}, {@code YYYY-MM}, {@code YYYY-MM-DD}.
	 * Negative years are not part of the FLEF reduced‑precision grammar and
	 * are rejected.
	 *
	 * @param raw the ISO 8601 reduced‑precision expression
	 * @return the parsed triple, or {@code null} if unparsable
	 */
	private static int[] parseHistoricalDate(final String raw){
		final String trimmed = raw.trim();
		final String[] parts = trimmed.split("-", 3);
		try{
			final int year = Integer.parseInt(parts[0]);
			final int month = (parts.length > 1? Integer.parseInt(parts[1]): 0);
			final int day = (parts.length > 2? Integer.parseInt(parts[2]): 0);
			return new int[]{year, month, day};
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/**
	 * Combines two FLEF {@code DateStructure} records (a start and an end)
	 * into a single temporal span, correctly handling {@code point},
	 * {@code bounded} and {@code spanning} values in either bound.
	 * <p>
	 * The result kind is:
	 * <ul>
	 *   <li>{@link TemporalSpanKind#SPANNING} when every present sub-span is
	 *       an exact point (absent sub-spans are treated as open-ended and do
	 *       not disqualify the span);</li>
	 *   <li>{@link TemporalSpanKind#BOUNDED} otherwise, signalling that at
	 *       least one bound is uncertain;</li>
	 *   <li>{@code null} when neither structure yields a usable bound.</li>
	 * </ul>
	 * <p>
	 * Special care is taken for the case where a sub-span carries only a
	 * one-sided constraint (e.g. {@code valid_from.bounded.not_after.X}):
	 * the resulting span uses that constraint as the corresponding outer
	 * bound, so that the connection remains renderable.
	 *
	 * @param startStructure the start date structure; may be {@code null}
	 * @param endStructure   the end date structure; may be {@code null}
	 * @param status         the span status ({@code active}, {@code ended},
	 *                       {@code unknown})
	 * @return the combined span, or {@code null}
	 */
	public TemporalSpan combineBounds(final FLEFRecord startStructure, final FLEFRecord endStructure,
		final String status){
		final TemporalSpan startSpan = normalize(startStructure);
		final TemporalSpan endSpan = normalize(endStructure);
		if(startSpan == null && endSpan == null)
			return null;

		final NormalizedDate lower = extractLowerBound(startSpan);
		final NormalizedDate upper = extractUpperBound(endSpan);

		// Both outer bounds unknown: fall back to the one-sided constraints
		// carried by the sub-spans (not_after on the start, not_before on the
		// end) so that at least one side of the span is anchored.
		if(lower == null && upper == null){
			final NormalizedDate fallbackUpper = extractUpperBound(startSpan);
			final NormalizedDate fallbackLower = extractLowerBound(endSpan);
			if(fallbackUpper != null && fallbackLower != null)
				return TemporalSpan.bounded(fallbackLower, fallbackUpper, status);
			if(fallbackUpper != null)
				return TemporalSpan.bounded(null, fallbackUpper, status);
			if(fallbackLower != null)
				return TemporalSpan.bounded(fallbackLower, null, status);
			return null;
		}

		// SPANNING only when every present constraint is an exact point.
		// Absent sub-spans are open-ended and do not disqualify SPANNING.
		final boolean exactStart = (startSpan == null || startSpan.isPoint());
		final boolean exactEnd = (endSpan == null || endSpan.isPoint());
		if(exactStart && exactEnd)
			return TemporalSpan.spanning(lower, upper, status);

		return TemporalSpan.bounded(lower, upper, status);
	}

	/**
	 * Convenience overload using {@link TemporalSpan#STATUS_UNKNOWN}.
	 */
	public TemporalSpan combineBounds(final FLEFRecord startStructure, final FLEFRecord endStructure){
		return combineBounds(startStructure, endStructure, TemporalSpan.STATUS_UNKNOWN);
	}

	/**
	 * Returns the lower bound of a span, or {@code null} if unknown.
	 * <ul>
	 *   <li>{@code POINT}: the point itself.</li>
	 *   <li>{@code BOUNDED}: the {@code not_before} bound (may be {@code null}).</li>
	 *   <li>{@code SPANNING}: the {@code from} bound (may be {@code null}).</li>
	 * </ul>
	 */
	private static NormalizedDate extractLowerBound(final TemporalSpan span){
		return (span != null? span.start(): null);
	}

	/**
	 * Returns the upper bound of a span, or {@code null} if unknown.
	 * <ul>
	 *   <li>{@code POINT}: the point itself.</li>
	 *   <li>{@code BOUNDED}: the {@code not_after} bound (may be {@code null}).</li>
	 *   <li>{@code SPANNING}: the {@code to} bound (may be {@code null}).</li>
	 * </ul>
	 */
	private static NormalizedDate extractUpperBound(final TemporalSpan span){
		return (span != null? span.effectiveEnd(): null);
	}

}
