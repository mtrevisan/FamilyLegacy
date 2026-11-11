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
package io.github.mtrevisan.calendars.gregorian;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


public class ReformedJulianCalendarStrategy extends GregorianStyleCalendarStrategy{

	@Override
	public long parseToJdn(final String rawDate){
		final GregorianDateInput input = parseInput(rawDate);
		final int year = input.year();
		final int month = input.monthOrFirst();
		final int day = input.dayOrFirst();
		input.validateLeapDay(isLeapYear(year));
		Indiction.validate(input, year);

		if(month < 0)
			// the season starts near the first day of its month
			return SeasonSolver.jdnNear(toJdn(year, CalendarSeason.gregorianSeasonMonth(month), 1), month,
				"Reformed Julian");

		// Standard numeric execution path using the Milanković algorithm for regular Reformed Julian dates
		return toJdn(year, month, day);
	}

	/**
	 * @param year the astronomical year
	 * @return whether the year is a leap year of the Reformed Julian calendar: the century years are leap only when they
	 * 	give remainder 200 or 600 when divided by 900
	 */
	public static boolean isLeapYear(final int year){
		final int centuryRemainder = Math.floorMod(year, 900);
		return (Math.floorMod(year, 4) == 0 && (year % 100 != 0 || centuryRemainder == 200 || centuryRemainder == 600));
	}

	/**
	 * Converts a proleptic Reformed Julian (Milanković) date into its Julian Day Number.
	 *
	 * @param year  the astronomical year (1 BCE = 0)
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return the Julian Day Number
	 */
	public static long toJdn(final int year, final int month, final int day){
		final int y = (month <= 2? year - 1: year);
		final int m = (month <= 2? month + 12: month);

		return 365L * y
			+ Math.floorDiv(y, 4L)
			- Math.floorDiv(y, 100L)
			+ Math.floorDiv(2L * Math.floorDiv(y, 100L) + 6, 9L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ day + 1720997L;
	}

}
