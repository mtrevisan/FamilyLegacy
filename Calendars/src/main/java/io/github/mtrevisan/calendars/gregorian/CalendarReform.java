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


/**
 * The switch from the Julian to the Gregorian calendar in a country: the day after the last Julian day is the first
 * Gregorian day, and the days in between never existed.
 *
 * @param lastJulianYear     the year of the last Julian day
 * @param lastJulianMonth    the month of the last Julian day
 * @param lastJulianDay      the last Julian day
 * @param firstGregorianYear  the year of the first Gregorian day
 * @param firstGregorianMonth the month of the first Gregorian day
 * @param firstGregorianDay   the first Gregorian day
 */
public record CalendarReform(int lastJulianYear, int lastJulianMonth, int lastJulianDay,
		int firstGregorianYear, int firstGregorianMonth, int firstGregorianDay){

	/** Papal States, the Italian states, Spain, Portugal, Poland: 4 October 1582 was followed by 15 October 1582. */
	public static final CalendarReform ROME = new CalendarReform(1582, 10, 4, 1582, 10, 15);
	/** France: 9 December 1582 was followed by 20 December 1582. */
	public static final CalendarReform FRANCE = new CalendarReform(1582, 12, 9, 1582, 12, 20);
	/** Great Britain and its colonies: 2 September 1752 was followed by 14 September 1752. */
	public static final CalendarReform BRITAIN = new CalendarReform(1752, 9, 2, 1752, 9, 14);


	/**
	 * Converts a civil date (Julian before the reform, Gregorian after) into its Julian Day Number.
	 *
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return the Julian Day Number
	 * @throws IllegalArgumentException if the day was skipped by the reform, or does not exist in the month
	 */
	public long toJdn(final int year, final int month, final int day){
		final boolean gregorian = isGregorian(year, month, day);
		if(!gregorian && compare(year, month, day, lastJulianYear, lastJulianMonth, lastJulianDay) > 0)
			throw new IllegalArgumentException("The days from " + (lastJulianDay + 1) + "/" + lastJulianMonth + "/"
				+ lastJulianYear + " to " + (firstGregorianDay - 1) + "/" + firstGregorianMonth + "/" + firstGregorianYear
				+ " were skipped by the Gregorian reform");
		if(day < 1 || day > monthLength(year, month))
			throw new IllegalArgumentException("Day " + day + " does not exist in month " + month + " of year " + year);

		return (gregorian
			? GregorianCalendarStrategy.toJdn(year, month, day)
			: JulianCalendarStrategy.toJdn(year, month, day));
	}

	/**
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return whether the date belongs to the Gregorian calendar
	 */
	public boolean isGregorian(final int year, final int month, final int day){
		return (compare(year, month, day, firstGregorianYear, firstGregorianMonth, firstGregorianDay) >= 0);
	}

	/**
	 * @param year the astronomical year
	 * @return whether the year has a 29 February (the reform never falls in a year where the two rules differ)
	 */
	public boolean isLeapYear(final int year){
		return (year > firstGregorianYear
			? year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
			: Math.floorMod(year, 4) == 0);
	}

	/**
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @return the number of days of the month (ignoring the days skipped by the reform)
	 */
	public int monthLength(final int year, final int month){
		return switch(month){
			case 2 -> (isLeapYear(year)? 29: 28);
			case 4, 6, 9, 11 -> 30;
			default -> 31;
		};
	}

	private static int compare(final int year1, final int month1, final int day1,
			final int year2, final int month2, final int day2){
		if(year1 != year2)
			return Integer.compare(year1, year2);
		if(month1 != month2)
			return Integer.compare(month1, month2);
		return Integer.compare(day1, day2);
	}

}
