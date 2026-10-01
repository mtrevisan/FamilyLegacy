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

import java.time.LocalDate;


/**
 * Arithmetic converter between calendar-specific dates and the Julian Day
 * Number (JDN) used as the common axis of the General Temporal Projection.
 * <p>
 * The JDN is an integer day count; dates from different calendars are
 * mapped onto the same axis and can therefore be compared and laid out
 * directly.
 * <p>
 * Supported calendars (exact, arithmetic):
 * <ul>
 *   <li>{@code gregorian} — proleptic Gregorian, Fliegel–Van Flandern</li>
 *   <li>{@code julian} — proleptic Julian</li>
 *   <li>{@code islamic} — tabular Islamic (civil epoch, 16 July 622 Julian)</li>
 *   <li>{@code coptic} — Coptic (epoch 29 August 284 Julian)</li>
 *   <li>{@code ethiopian} — Ethiopian (epoch 29 August 8 Julian)</li>
 * </ul>
 * The remaining calendars declared by the FLEF protocol
 * ({@code hebrew}, {@code chinese}, {@code indian}, {@code buddhist},
 * {@code french_republican}, {@code soviet_eternal}, {@code mayan}) require
 * astronomical calculations, non-standard correlation constants, or have
 * ambiguous definitions; they raise {@link UnsupportedOperationException}
 * so that the extension point is explicit.
 */
public final class CalendarConverter{

	private CalendarConverter(){}


	/**
	 * Converts a JDN to a Gregorian calendar date.
	 *
	 * @param jdn the Julian Day Number
	 * @return the Gregorian date
	 */
	public static LocalDate jdnToGregorian(final long jdn){
		final long l = jdn + 68569L;
		final long n = 4L * l / 146097L;
		final long l1 = l - (146097L * n + 3L) / 4L;
		final long i = 4000L * (l1 + 1L) / 1461001L;
		final long l2 = l1 - 1461L * i / 4L + 31L;
		final long j = 80L * l2 / 2447L;

		final int day = (int)(l2 - 2447L * j / 80L);

		final long l3 = j / 11L;
		final int month = (int)(j + 2L - 12L * l3);

		final int year = (int)(100L * (n - 49L) + i + l3);

		return LocalDate.of(year, month, day);
	}

	/**
	 * Converts a calendar-specific date to its Julian Day Number.
	 *
	 * @param calendar the calendar name (see class Javadoc); a blank value
	 *                 is treated as Gregorian
	 * @param year     the year in the given calendar; may be negative
	 * @param month    the month (1‑based)
	 * @param day      the day of month (1‑based)
	 * @return the Julian Day Number
	 * @throws UnsupportedOperationException if the calendar is not arithmetic
	 */
	public static long toJdn(final String calendar, final int year, final int month, final int day){
		return CalendarType.fromCode(calendar)
			.toJdn(year, month, day);
	}

	/**
	 * Returns the JDN of the first day of the given year in the given
	 * calendar. Used for reduced‑precision dates (decade, century) where
	 * only the year is known.
	 *
	 * @param calendar the calendar name
	 * @param year     the year
	 * @return the Julian Day Number of 1 January of that year
	 */
	public static long toJdnStartOfYear(final String calendar, final int year){
		return toJdn(calendar, year, 1, 1);
	}

}
