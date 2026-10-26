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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import org.apache.commons.lang3.StringUtils;


/**
 * Converts a FLEF {@code DateStructure} into a {@link TemporalSpan} ready
 * for the General Temporal Projection.
 * <p>
 * Handles all three alternatives of the {@code DateValue} union
 * ({@code point}, {@code bounded}, {@code spanning}) and all three
 * alternatives of {@code SingleDate} value ({@code full_date}, {@code decade},
 * {@code century}). Approximation and calendar are now read from the
 * root {@code SingleDate} struct level.
 */
public final class DateNormalizer{

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
		final String calendar = defaultCalendar(FLEFRecordHelper.getChildValue(singleDate, DateReader.TAG_CALENDAR));

		final FLEFRecord valueContainer = FLEFRecordHelper.findChild(singleDate, DateReader.TAG_VALUE);
		if(valueContainer == null)
			return null;

		final FLEFRecord fullDate = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_FULL_DATE);
		if(fullDate != null)
			return normalizeFullDate(singleDate, fullDate, calendar, originalText);

		final FLEFRecord decade = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_DECADE);
		if(decade != null)
			return normalizeDecade(singleDate, decade, calendar, originalText);

		final FLEFRecord century = FLEFRecordHelper.findChild(valueContainer, DateReader.TAG_CENTURY);
		if(century != null)
			return normalizeCentury(singleDate, century, calendar, originalText);

		return null;
	}

	private static NormalizedDate normalizeFullDate(final FLEFRecord singleDate, final FLEFRecord fullDate,
			final String calendar, final String originalText){
		final String raw = fullDate.getValue();
		if(StringUtils.isBlank(raw))
			return null;

		// The full date is expressed in the notation of its calendar: its parser gives both the JDN and the components
		// actually present, from which the precision follows
		final CalendarType type = CalendarType.fromCode(calendar);
		try{
			final long jdn = type.parseToJdn(raw);
			final CalendarMetadata metadata = type.getMetadata(raw);
			final DatePrecision precision = (metadata.isComplete()? DatePrecision.DAY
				: (Boolean.TRUE.equals(metadata.nativeComponents().get("month"))? DatePrecision.MONTH: DatePrecision.YEAR));

			return applyApproximation(singleDate, jdn, precision, calendar, originalText);
		}
		catch(final IllegalArgumentException | UnsupportedOperationException ignored){
			// The date is not valid in the notation of its calendar, or the calendar needs the Ephemeris plugin
			return null;
		}
	}

	private static NormalizedDate normalizeDecade(final FLEFRecord singleDate, final FLEFRecord decade,
			final String calendar, final String originalText){
		final String rawYear = decade.getValue();
		if(rawYear == null)
			return null;
		final int startYear = Integer.parseInt(rawYear.trim());
		final Long jdn = startOfYearJdn(calendar, startYear);
		return (jdn != null? applyApproximation(singleDate, jdn, DatePrecision.DECADE, calendar, originalText): null);
	}

	private static NormalizedDate normalizeCentury(final FLEFRecord singleDate, final FLEFRecord century,
			final String calendar, final String originalText){
		final String rawOrdinal = FLEFRecordHelper.getChildValue(century, DateReader.TAG_ORDINAL);
		if(rawOrdinal == null)
			return null;
		final int ordinal = Integer.parseInt(rawOrdinal.trim());
		// Century N of the common era covers years (N-1)*100+1 .. N*100.
		final int startYear = (ordinal - 1) * 100 + 1;
		final Long jdn = startOfYearJdn(calendar, startYear);
		return (jdn != null? applyApproximation(singleDate, jdn, DatePrecision.CENTURY, calendar, originalText): null);
	}

	/**
	 * @return the JDN of the first day of the year in the given calendar, or {@code null} if the calendar cannot express
	 * 	a year alone, or needs the Ephemeris plugin
	 */
	private static Long startOfYearJdn(final String calendar, final int year){
		try{
			return CalendarType.fromCode(calendar)
				.parseToJdn(Integer.toString(year));
		}
		catch(final IllegalArgumentException | UnsupportedOperationException ignored){
			return null;
		}
	}


	/* ======================================================================
	 *                       Helpers
	 * ====================================================================== */

	private static NormalizedDate applyApproximation(final FLEFRecord singleDate, final long jdn,
			final DatePrecision precision, final String calendar, final String originalText){
		final FLEFRecord approx = FLEFRecordHelper.findChild(singleDate, DateReader.TAG_APPROXIMATE);
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
