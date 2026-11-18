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
package io.github.mtrevisan.calendars.dateparsing;

import java.util.function.IntPredicate;


/**
 * The length of the months of a year, used to validate the day of a date.
 * <p>
 * An implementation returns {@code 0} for a month that does not exist in the year (e.g. the second Adar of a common
 * Hebrew year, or the intercalary month of a calendar that has none).
 * </p>
 */
@FunctionalInterface
public interface MonthLengths{

	int GREGORIAN_MONTHS = 12;
	int FEBRUARY = 2;

	/**
	 * @param year  the year
	 * @param month the month, as numbered by the parser of the calendar
	 * @return the number of days of the month, or {@code 0} if the month does not exist in the year
	 */
	int length(int year, int month);


	/** @return whether the year is a leap year of the Julian calendar */
	static boolean isJulianLeap(final int year){
		return (Math.floorMod(year, 4) == 0);
	}

	/** @return whether the year is a leap year of the Gregorian calendar */
	static boolean isGregorianLeap(final int year){
		return (Math.floorMod(year, 4) == 0 && (Math.floorMod(year, 100) != 0 || Math.floorMod(year, 400) == 0));
	}

	/**
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @return the days of the month of the Julian calendar, or {@code 0} if the month is not valid
	 */
	static int julian(final int year, final int month){
		return civil(month, isJulianLeap(year));
	}

	/**
	 * @param year  the astronomical year
	 * @param month the month (1 to 12)
	 * @return the days of the month of the (proleptic) Gregorian calendar, or {@code 0} if the month is not valid
	 */
	static int gregorian(final int year, final int month){
		return civil(month, isGregorianLeap(year));
	}

	private static int civil(final int month, final boolean leap){
		if(month < 1 || month > GREGORIAN_MONTHS)
			return 0;
		if(month == FEBRUARY)
			return (leap? 29: 28);
		return switch(month){
			case 4, 6, 9, 11 -> 30;
			default -> 31;
		};
	}

	/**
	 * The lengths of a calendar of twelve months of thirty days, followed by the intercalary days of a thirteenth month.
	 *
	 * @param intercalaryDays the intercalary days of a common year (5)
	 * @param leapYear        the leap years, that have one more intercalary day ({@code null} if there are none)
	 * @return the month lengths
	 */
	static MonthLengths thirtyDayMonths(final int intercalaryDays, final IntPredicate leapYear){
		return (year, month) -> {
			if(month >= 1 && month <= 12)
				return 30;
			if(month == 13)
				return intercalaryDays + (leapYear != null && leapYear.test(year)? 1: 0);
			return 0;
		};
	}

}
