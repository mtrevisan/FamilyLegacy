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
package io.github.mtrevisan.familylegacy.v2.io.model.readers.date;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.io.model.readers.DateReader;

import java.util.Locale;


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
 */
public final class DateNormalizer{

	private static final String[] MONTH_ABBREVIATIONS = {
		"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
		"JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
	};


	private DateNormalizer(){}


	/**
	 * Normalizes a FLEF {@code DateStructure} into a temporal span.
	 *
	 * @param dateStructure the date structure record (may be {@code null})
	 * @return the normalized span, or {@code null} if the structure is absent
	 * or carries no recognizable date value
	 */
	public static TemporalSpan normalize(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return null;

		final String originalText = DateReader.extractOriginalText(dateStructure);
		final FLEFRecord value = FLEFRecordHelper.findChild(dateStructure, DateReader.TAG_VALUE);
		if(value == null)
			return null;

		final FLEFRecord point = FLEFRecordHelper.findChild(value, DateReader.TAG_POINT);
		if(point != null)
			return normalizePoint(point, originalText);

		final FLEFRecord bounded = FLEFRecordHelper.findChild(value, DateReader.TAG_BOUNDED);
		if(bounded != null)
			return normalizeBounded(bounded, originalText);

		final FLEFRecord spanning = FLEFRecordHelper.findChild(value, DateReader.TAG_SPANNING);
		if(spanning != null)
			return normalizeSpanning(spanning, originalText);

		return null;
	}


	/* ======================================================================
	 *                       DateValue variants
	 * ====================================================================== */

	private static TemporalSpan normalizePoint(final FLEFRecord point, final String originalText){
		final NormalizedDate date = normalizeSingleDate(point, originalText);
		return (date != null? TemporalSpan.point(date): null);
	}

	private static TemporalSpan normalizeBounded(final FLEFRecord bounded, final String originalText){
		final NormalizedDate notBefore = normalizeSubSingleDate(bounded, DateReader.TAG_NOT_BEFORE, originalText);
		final NormalizedDate notAfter = normalizeSubSingleDate(bounded, DateReader.TAG_NOT_AFTER, originalText);
		if(notBefore == null && notAfter == null)
			return null;
		return TemporalSpan.bounded(notBefore, notAfter);
	}

	private static TemporalSpan normalizeSpanning(final FLEFRecord spanning, final String originalText){
		final NormalizedDate from = normalizeSubSingleDate(spanning, DateReader.TAG_FROM, originalText);
		final NormalizedDate to = normalizeSubSingleDate(spanning, DateReader.TAG_TO, originalText);
		if(from == null && to == null)
			return null;
		return TemporalSpan.spanning(from, to);
	}

	private static NormalizedDate normalizeSubSingleDate(final FLEFRecord parent, final String tag,
			final String originalText){
		final FLEFRecord child = FLEFRecordHelper.findChild(parent, tag);
		return (child != null? normalizeSingleDate(child, originalText): null);
	}


	/* ======================================================================
	 *                       SingleDate variants
	 * ====================================================================== */

	private static NormalizedDate normalizeSingleDate(final FLEFRecord singleDate, final String originalText){
		final FLEFRecord fullDate = FLEFRecordHelper.findChild(singleDate, DateReader.TAG_FULL_DATE);
		if(fullDate != null)
			return normalizeFullDate(fullDate, originalText);

		final FLEFRecord decade = FLEFRecordHelper.findChild(singleDate, DateReader.TAG_DECADE);
		if(decade != null)
			return normalizeDecade(decade, originalText);

		final FLEFRecord century = FLEFRecordHelper.findChild(singleDate, DateReader.TAG_CENTURY);
		if(century != null)
			return normalizeCentury(century, originalText);

		return null;
	}

	private static NormalizedDate normalizeFullDate(final FLEFRecord fullDate, final String originalText){
		final String raw = FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_VALUE);
		if(raw == null || raw.isBlank())
			return null;

		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(fullDate, DateReader.TAG_CALENDAR));
		final int[] ymd = parseHistoricalDate(raw);
		if(ymd == null)
			return null;

		final DatePrecision precision = (ymd[2] > 0? DatePrecision.DAY
			: (ymd[1] > 0? DatePrecision.MONTH: DatePrecision.YEAR));
		final long jdn = CalendarConverter.toJdn(calendar, ymd[0], Math.max(1, ymd[1]), Math.max(1, ymd[2]));

		return applyApproximation(fullDate, jdn, precision, calendar, originalText);
	}

	private static NormalizedDate normalizeDecade(final FLEFRecord decade, final String originalText){
		final String rawYear = FLEFRecordHelper.getChildValue(decade, DateReader.TAG_START_YEAR);
		if(rawYear == null)
			return null;
		final int startYear = Integer.parseInt(rawYear.trim());
		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(decade, DateReader.TAG_CALENDAR));
		final long jdn = CalendarConverter.toJdnStartOfYear(calendar, startYear);
		return applyApproximation(decade, jdn, DatePrecision.DECADE, calendar, originalText);
	}

	private static NormalizedDate normalizeCentury(final FLEFRecord century, final String originalText){
		final String rawOrdinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
		if(rawOrdinal == null)
			return null;
		final int ordinal = Integer.parseInt(rawOrdinal.trim());
		// Century N of the common era covers years (N-1)*100+1 .. N*100.
		final int startYear = (ordinal - 1) * 100 + 1;
		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(century, DateReader.TAG_CALENDAR));
		final long jdn = CalendarConverter.toJdnStartOfYear(calendar, startYear);
		return applyApproximation(century, jdn, DatePrecision.CENTURY, calendar, originalText);
	}


	/* ======================================================================
	 *                       Helpers
	 * ====================================================================== */

	private static NormalizedDate applyApproximation(final FLEFRecord container, final long jdn,
		final DatePrecision precision, final String calendar, final String originalText){
		final FLEFRecord approx = FLEFRecordHelper.findChild(container, DateReader.TAG_APPROXIMATE);
		if(approx == null)
			return NormalizedDate.exact(jdn, precision, calendar)
				.withOriginalText(originalText);

		final String basis = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_BASIS);
		final String margin = FLEFRecordHelper.getChildValue(approx, DateReader.TAG_MARGIN);
		return NormalizedDate.approximated(jdn, precision, calendar, basis, margin)
			.withOriginalText(originalText);
	}

	private static String defaultCalendar(final String raw){
		return (raw != null && !raw.isBlank()? raw.toLowerCase(): CalendarType.GREGORIAN.getCode());
	}

	/**
	 * Parses a historical date string into {@code [year, month, day]}.
	 * <p>
	 * Two syntaxes are accepted:
	 * <ul>
	 *   <li><b>ISO 8601</b>, possibly reduced to year or year-month:
	 *       {@code "1886-08-19"}, {@code "1886-08"}, {@code "1886"};</li>
	 *   <li><b>Legacy FLEF</b> as found in historical files:
	 *       {@code "19 AUG 1886"}, {@code "AUG 1886"}, {@code "19 AUG 1886 BC"},
	 *       with the month name abbreviated in English, case-insensitive.</li>
	 * </ul>
	 * Missing components are returned as 0. <b>The year is mandatory</b>: an
	 * expression that only carries a day and/or a month (e.g.
	 * {@code "19 AUG"} or {@code "AUG"}) is rejected, because without a year
	 * the date cannot be placed on a temporal axis and would produce
	 * misleading placements in every downstream view.
	 *
	 * @param raw the date expression
	 * @return the parsed triple, or {@code null} if the expression is
	 *         unparsable or lacks a year
	 */
	private static int[] parseHistoricalDate(final String raw){
		if(raw == null || raw.isBlank())
			return null;
		final String trimmed = raw.trim();

		// Try ISO 8601 first: it is unambiguous and does not need heuristics.
		final int[] iso = parseIso8601(trimmed);
		if(iso != null)
			return iso;

		// Try the legacy "DD MMM YYYY" / "MMM YYYY" form.
		return parseLegacyDate(trimmed);
	}

	/**
	 * Parses an ISO 8601 date, possibly reduced to year or year-month.
	 * Returns {@code null} when the expression is malformed or does not
	 * carry a year.
	 */
	private static int[] parseIso8601(final String raw){
		final String[] parts = raw.split("-", 4);
		if(parts.length == 0 || parts.length > 3)
			return null;
		try{
			final int year = Integer.parseInt(parts[0]);
			final int month = (parts.length > 1? Integer.parseInt(parts[1]): 0);
			final int day = (parts.length > 2? Integer.parseInt(parts[2]): 0);
			if(month < 0 || month > 12 || day < 0 || day > 31)
				return null;
			// The year is mandatory and must not be zero: an expression that
			// only carries a month and/or a day is unparsable as a date.
			if(year == 0)
				return null;
			return new int[]{year, month, day};
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}

	/**
	 * Parses a legacy FLEF date: "DD MMM YYYY", "MMM YYYY", "YYYY",
	 * optionally with a "BC" suffix. The month is matched against the
	 * English three-letter abbreviations, case-insensitively.
	 * <p>
	 * The year is mandatory: an expression that only carries a day and/or a
	 * month (e.g. {@code "19 AUG"}) is rejected.
	 */
	private static int[] parseLegacyDate(final String raw){
		final String[] tokens = raw.toUpperCase(Locale.ROOT)
			.trim()
			.split("\\s+");
		if(tokens.length == 0)
			return null;

		int year = 0;
		int month = 0;
		int day = 0;
		boolean bc = false;

		for(final String token : tokens){
			if("BC".equals(token) || "BCE".equals(token)){
				bc = true;
				continue;
			}

			final int monthIndex = monthIndex(token);
			if(monthIndex > 0 && month == 0){
				month = monthIndex;
				continue;
			}

			// Numeric token: could be a day or a year. A value greater than
			// 31 can only be a year; a value up to 31 is treated as the day
			// if the day is still empty, otherwise as the year.
			try{
				final int value = Integer.parseInt(token);
				if(value > 31)
					year = value;
				else if(day == 0)
					day = value;
				else if(year == 0)
					year = value;
			}
			catch(final NumberFormatException ignored){
				// Unrecognized token: fail the whole parse.
				return null;
			}
		}

		// The year is mandatory: without it, the date cannot be placed on a
		// temporal axis.
		if(year == 0)
			return null;
		if(bc)
			year = -Math.abs(year);
		return new int[]{year, month, day};
	}

	/**
	 * Returns the 1-based month index of an English three-letter month
	 * abbreviation, or 0 if the token is not a month name.
	 */
	private static int monthIndex(final String token){
		for(int i = 0; i < MONTH_ABBREVIATIONS.length; i ++)
			if(MONTH_ABBREVIATIONS[i].equals(token))
				return i + 1;
		return 0;
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
	public static TemporalSpan combineBounds(final FLEFRecord startStructure, final FLEFRecord endStructure,
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
	public static TemporalSpan combineBounds(final FLEFRecord startStructure, final FLEFRecord endStructure){
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
