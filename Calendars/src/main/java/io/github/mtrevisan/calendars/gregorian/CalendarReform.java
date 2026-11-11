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
	 * The Protestant states of Germany and Denmark-Norway ("Improved calendar"): 18 February 1700 was followed by
	 * 1 March 1700.
	 */
	public static final CalendarReform GERMANY_PROTESTANT = new CalendarReform(1700, 2, 18, 1700, 3, 1);
	/** Denmark-Norway: 18 February 1700 was followed by 1 March 1700. */
	public static final CalendarReform DENMARK = GERMANY_PROTESTANT;
	/** Tyrol, Further Austria, Carniola: 4 October 1583 was followed by 15 October 1583. */
	public static final CalendarReform ROME_1583 = new CalendarReform(1583, 10, 4, 1583, 10, 15);
	/** Brabant, Zeeland, Flanders, the States General: 14 December 1582 was followed by 25 December 1582. */
	public static final CalendarReform NETHERLANDS_BRABANT = new CalendarReform(1582, 12, 14, 1582, 12, 25);
	/** Gelderland (Netherlands): 30 June 1700 was followed by 12 July 1700. */
	public static final CalendarReform NETHERLANDS_GELDERLAND = new CalendarReform(1700, 6, 30, 1700, 7, 12);
	/** Overijssel and Utrecht (Netherlands): 30 November 1700 was followed by 12 December 1700. */
	public static final CalendarReform NETHERLANDS_UTRECHT = new CalendarReform(1700, 11, 30, 1700, 12, 12);
	/** Friesland and Groningen (Netherlands): 31 December 1700 was followed by 12 January 1701. */
	public static final CalendarReform NETHERLANDS_FRISIA = new CalendarReform(1700, 12, 31, 1701, 1, 12);
	/** Bulgaria: 31 March 1916 was followed by 14 April 1916. */
	public static final CalendarReform BULGARIA = new CalendarReform(1916, 3, 31, 1916, 4, 14);
	/** Russia (civil calendar, decree of 24 January 1918): 31 January 1918 was followed by 14 February 1918. */
	public static final CalendarReform RUSSIA = new CalendarReform(1918, 1, 31, 1918, 2, 14);
	/** Romania: 31 March 1919 was followed by 14 April 1919. */
	public static final CalendarReform ROMANIA = new CalendarReform(1919, 3, 31, 1919, 4, 14);
	/** Greece (civil calendar): 15 February 1923 was followed by 1 March 1923. */
	public static final CalendarReform GREECE = new CalendarReform(1923, 2, 15, 1923, 3, 1);
	/** Sweden (and Finland), return to the Gregorian calendar: 17 February 1753 was followed by 1 March 1753. */
	public static final CalendarReform SWEDEN = new CalendarReform(1753, 2, 17, 1753, 3, 1);


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
		if(isSkipped(year, month, day))
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
	 * @return whether the date is one of the days that never existed because of the reform
	 */
	public boolean isSkipped(final int year, final int month, final int day){
		return (!isGregorian(year, month, day)
			&& compare(year, month, day, lastJulianYear, lastJulianMonth, lastJulianDay) > 0);
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

	/** @return the Julian Day Number of the first Gregorian day */
	public long firstGregorianJdn(){
		return toJdn(firstGregorianYear, firstGregorianMonth, firstGregorianDay);
	}

	/**
	 * @param year the astronomical year
	 * @return whether the year has a 29 February (the reform never falls in a year when the two rules differ)
	 */
	public boolean isLeapYear(final int year){
		return (year > firstGregorianYear
			? GregorianCalendarStrategy.isLeapYear(year)
			: JulianCalendarStrategy.isLeapYear(year));
	}

	/**
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @return the number of days of the month (ignoring the days skipped by the reform)
	 */
	public int monthLength(final int year, final int month){
		return GregorianCalendarStrategy.monthLength(month, isLeapYear(year));
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
