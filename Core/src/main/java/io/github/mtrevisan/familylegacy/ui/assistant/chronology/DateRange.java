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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The days a date may stand for, as Julian Day Numbers (both ends included).
 * <p>
 * A date is rarely exact: a year covers 366 days, an approximate date has a margin, a bounded date lies between two days, an open
 * one has an end missing ({@link Long#MIN_VALUE} / {@link Long#MAX_VALUE}). The chronology checks only report a problem that holds
 * for every day of the range, so an uncertain date never raises a false alarm.
 * </p>
 *
 * @param min the earliest possible day
 * @param max the latest possible day
 */
public record DateRange(long min, long max){

	/** The margin of an approximate date that does not state one: two years, as in the FLEF example (P2Y). */
	static final long DEFAULT_APPROXIMATE_MARGIN_DAYS = 2 * 366;

	private static final Pattern DURATION = Pattern.compile("P(?:(\\d+)Y)?(?:(\\d+)M)?(?:(\\d+)D)?");
	private static final int DAYS_IN_YEAR = 366;
	private static final int DAYS_IN_MONTH = 31;


	public DateRange{
		if(min > max)
			throw new IllegalArgumentException("Empty range: " + min + " > " + max);
	}


	/** @return a range of a single day */
	public static DateRange day(final long jdn){
		return new DateRange(jdn, jdn);
	}

	/**
	 * @param span a normalized date (can be {@code null})
	 * @return the days the date may stand for, or {@code null} if the date is missing or has no bound at all
	 */
	public static DateRange of(final TemporalSpan span){
		if(span == null || span.start() == null && span.end() == null)
			return null;

		final long min = (span.start() != null? span.start().jdn() - margin(span.start()): Long.MIN_VALUE);
		final long max;
		if(span.isPoint())
			max = span.start().jdn() + span.start().precisionSpanInDays() - 1 + margin(span.start());
		else if(span.end() != null)
			max = span.end().jdn() + span.end().precisionSpanInDays() - 1 + margin(span.end());
		else
			max = Long.MAX_VALUE;
		return new DateRange(min, Math.max(min, max));
	}

	/** @return the days of the margin of an approximate date, {@code 0} for an exact one */
	private static long margin(final NormalizedDate date){
		if(!date.approximate())
			return 0;

		final String margin = date.approximateMargin();
		if(margin != null){
			final Matcher matcher = DURATION.matcher(margin.trim());
			if(matcher.matches() && (matcher.group(1) != null || matcher.group(2) != null || matcher.group(3) != null))
				return (long)group(matcher, 1) * DAYS_IN_YEAR + (long)group(matcher, 2) * DAYS_IN_MONTH + group(matcher, 3);
		}
		return DEFAULT_APPROXIMATE_MARGIN_DAYS;
	}

	private static int group(final Matcher matcher, final int index){
		return (matcher.group(index) != null? Integer.parseInt(matcher.group(index)): 0);
	}


	/** @return the union of this range and another one */
	public DateRange hull(final DateRange other){
		return new DateRange(Math.min(min, other.min), Math.max(max, other.max));
	}

	/** @return whether the two ranges have a day in common */
	public boolean intersects(final DateRange other){
		return (min <= other.max && other.min <= max);
	}

	/** @return whether every day of this range is before every day of the other one */
	public boolean isBefore(final DateRange other){
		return (max < other.min);
	}

	/** @return {@code value} plus {@code days} without overflowing the open ends */
	static long plus(final long value, final long days){
		if(value == Long.MIN_VALUE || value == Long.MAX_VALUE)
			return value;

		return value + days;
	}

}
