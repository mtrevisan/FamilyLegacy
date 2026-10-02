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


import org.apache.commons.lang3.Strings;


/**
 * A single normalized point in time used by the General Temporal Projection.
 * <p>
 * Normalization maps every FLEF {@code SingleDate} alternative (full date,
 * decade, century, with any calendar) onto a common numeric axis, so that
 * dates from different calendars and different precisions can be laid out
 * and compared on the same temporal axis.
 * <p>
 * The numeric axis is the <b>Julian Day Number</b> (JDN), an integer count
 * of days since noon on 1 January 4713 BC (Julian calendar). For reduced
 * precisions the JDN points to the <i>start</i> of the represented interval
 * (e.g. the 1st of January of the first year of a decade or century).
 * <p>
 * The record is immutable and safe to share across threads.
 */
public record NormalizedDate(
	long jdn,
	DatePrecision precision,
	String calendar,
	boolean approximate,
	String approximateBasis,
	String approximateMargin,
	String originalText
) implements Comparable<NormalizedDate>{

	/**
	 * Compact constructor with validation.
	 * <p>
	 * Invariants:
	 * <ul>
	 *   <li>{@code precision} must not be {@code null};</li>
	 *   <li>{@code calendar} must not be {@code null} or blank;</li>
	 *   <li>when {@code approximate} is {@code false}, both
	 *       {@code approximateBasis} and {@code approximateMargin} must be
	 *       {@code null}.</li>
	 * </ul>
	 */
	public NormalizedDate{
		if(precision == null)
			throw new IllegalArgumentException("Precision must not be null");
		if(calendar == null || calendar.isBlank())
			throw new IllegalArgumentException("Calendar must not be null or blank");
		if(!approximate){
			if(approximateBasis != null)
				throw new IllegalArgumentException("Approximate basis must be null when the date is not approximate");
			if(approximateMargin != null)
				throw new IllegalArgumentException("Approximate margin must be null when the date is not approximate");
		}
	}


	/**
	 * Factory method for an exact (non‑approximate) normalized date in the
	 * Gregorian calendar.
	 *
	 * @param jdn       the Julian Day Number
	 * @param precision the declared precision
	 * @return a normalized date
	 */
	public static NormalizedDate exact(final long jdn, final DatePrecision precision){
		return exact(jdn, precision, CalendarType.GREGORIAN.getCode());
	}

	/**
	 * Factory method for an exact (non‑approximate) normalized date.
	 *
	 * @param jdn       the Julian Day Number
	 * @param precision the declared precision
	 * @param calendar  the calendar name; use one of the {@code CALENDAR_*}
	 *                  constants for standard calendars
	 * @return a normalized date
	 */
	public static NormalizedDate exact(final long jdn, final DatePrecision precision, final String calendar){
		return new NormalizedDate(jdn, precision, calendar, false, null, null,
			null);
	}

	/**
	 * Factory method for an approximate normalized date.
	 *
	 * @param jdn       the Julian Day Number
	 * @param precision the declared precision
	 * @param calendar  the calendar name
	 * @param basis     the approximation basis; may be {@code null}
	 * @param margin    the approximation margin as an ISO 8601 duration
	 *                  (e.g. {@code "P2Y"}); may be {@code null}
	 * @return an approximate normalized date
	 */
	public static NormalizedDate approximated(final long jdn, final DatePrecision precision, final String calendar,
			final String basis, final String margin){
		return new NormalizedDate(jdn, precision, calendar, true, basis, margin, null);
	}


	/**
	 * Returns a copy of this date with the given source text attached.
	 *
	 * @param originalText the date expression exactly as it appears in the
	 *                     source; may be {@code null}
	 * @return a new normalized date
	 */
	public NormalizedDate withOriginalText(final String originalText){
		return new NormalizedDate(jdn, precision, calendar, approximate, approximateBasis, approximateMargin,
			originalText);
	}

	/**
	 * Returns whether this date belongs to the Gregorian calendar.
	 *
	 * @return {@code true} if the calendar is {@link CalendarType}
	 */
	public boolean isGregorian(){
		return Strings.CI.equals(calendar, CalendarType.GREGORIAN.getCode());
	}

	/**
	 * Returns whether this date has reduced precision (decade or century).
	 *
	 * @return {@code true} for {@link DatePrecision#DECADE} and
	 * {@link DatePrecision#CENTURY}
	 */
	public boolean isReduced(){
		return precision.isReduced();
	}

	/**
	 * Returns the approximate number of JDN days covered by this date's
	 * precision. Used to treat reduced‑precision points as intervals when
	 * checking containment.
	 *
	 * @return a positive number of days
	 */
	public long precisionSpanInDays(){
		return switch(precision){
			case DAY -> 1l;
			case MONTH -> 31l;
			case YEAR -> 366l;
			case DECADE -> 3653l;
			case CENTURY -> 36525l;
		};
	}


	/**
	 * Compares two normalized dates primarily by their JDN, and secondarily
	 * by their precision granularity (finer first). Different calendars are
	 * already mapped onto the same axis, so they are directly comparable.
	 */
	@Override
	public int compareTo(final NormalizedDate other){
		int cmp = Long.compare(jdn, other.jdn);
		if(cmp == 0)
			cmp = Integer.compare(precision.getGranularity(), other.precision.getGranularity());
		return cmp;
	}

	@Override
	public String toString(){
		final StringBuilder sb = new StringBuilder();
		sb.append("JDN ").append(jdn)
			.append(" (").append(precision);
		if(!isGregorian())
			sb.append(", ").append(calendar);
		sb.append(")");
		if(approximate){
			sb.append(" approx");
			if(approximateBasis != null)
				sb.append(" [").append(approximateBasis).append("]");
			if(approximateMargin != null)
				sb.append(" +/-").append(approximateMargin);
		}
		return sb.toString();
	}

}
