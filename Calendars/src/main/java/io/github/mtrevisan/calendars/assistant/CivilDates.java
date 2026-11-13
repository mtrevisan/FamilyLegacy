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
package io.github.mtrevisan.calendars.assistant;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import org.apache.commons.lang3.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;


/** Writes a Julian Day Number as a date of the Julian or of the (proleptic) Gregorian calendar. */
final class CivilDates{

	static final String JULIAN = CalendarType.JULIAN.getCode();
	static final String GREGORIAN = CalendarType.GREGORIAN.getCode();


	private CivilDates(){}


	static String formatGregorian(final long jdn){
		final LocalDate date = CalendarType.jdnToProlepticGregorianLocalDate(jdn);
		return format(date.getYear(), date.getMonth(), date.getDayOfMonth());
	}

	static String formatJulian(final long jdn){
		final int[] fields = julianFields(jdn);
		return format(fields[0], Month.of(fields[1]), fields[2]);
	}

	/** @return the year, month and day of the Julian calendar */
	static int[] julianFields(final long jdn){
		final long b = jdn + 1524;
		final long c = (long)Math.floor((b - 122.1) / 365.25);
		final long d = (long)Math.floor(365.25 * c);
		final long e = (long)Math.floor((b - d) / 30.6001);
		final int day = (int)(b - d - (long)Math.floor(30.6001 * e));
		final int month = (int)(e < 14? e - 1: e - 13);
		final int year = (int)(month > 2? c - 4716: c - 4715);
		return new int[]{year, month, day};
	}

	/** @return the year, month and day of the (proleptic) Gregorian calendar */
	static int[] gregorianFields(final long jdn){
		final LocalDate date = CalendarType.jdnToProlepticGregorianLocalDate(jdn);
		return new int[]{date.getYear(), date.getMonthValue(), date.getDayOfMonth()};
	}

	static int[] fields(final long jdn, final String calendarCode){
		return (JULIAN.equals(calendarCode)? julianFields(jdn): gregorianFields(jdn));
	}

	static long toJdn(final int year, final int month, final int day, final String calendarCode){
		return (JULIAN.equals(calendarCode)
			? JulianCalendarStrategy.toJdn(year, month, day)
			: GregorianCalendarStrategy.toJdn(year, month, day));
	}

	static int monthLength(final int year, final int month, final String calendarCode){
		if(month == 2){
			final boolean leap = (JULIAN.equals(calendarCode)
				? Math.floorMod(year, 4) == 0
				: Math.floorMod(year, 4) == 0 && (Math.floorMod(year, 100) != 0 || Math.floorMod(year, 400) == 0));
			return (leap? 29: 28);
		}
		return Month.of(month).length(false);
	}

	/**
	 * The calendar in force at a date where a Gregorian reform was adopted.
	 *
	 * @param jdn    the day
	 * @param reform the reform, or {@code null} if the Julian calendar is always used
	 * @return the code of the calendar
	 */
	static String calendarInForce(final long jdn, final CalendarReform reform){
		if(reform == null)
			return JULIAN;

		return (jdn < reform.firstGregorianJdn()? JULIAN: GREGORIAN);
	}

	static String format(final long jdn, final String calendarCode){
		return (JULIAN.equals(calendarCode)? formatJulian(jdn): formatGregorian(jdn));
	}

	/** The Julian Day Number of a day of a year, in the calendar in force. */
	static long toJdn(final int year, final int month, final int day, final CalendarReform reform){
		return (reform == null? JulianCalendarStrategy.toJdn(year, month, day): reform.toJdn(year, month, day));
	}

	/**
	 * Adds months to a day, keeping the day of the month (a day that does not exist in the month reached becomes its last day).
	 *
	 * @param months	the months to add (negative to subtract)
	 */
	static long shiftMonths(final long jdn, final int months, final String calendarCode){
		final int[] fields = fields(jdn, calendarCode);
		final int total = fields[0] * 12 + (fields[1] - 1) + months;
		final int year = Math.floorDiv(total, 12);
		final int month = Math.floorMod(total, 12) + 1;
		return toJdn(year, month, Math.min(fields[2], monthLength(year, month, calendarCode)), calendarCode);
	}

	/** @return {@code 0} for Monday to {@code 6} for Sunday */
	static int weekday(final long jdn){
		return (int)Math.floorMod(jdn, 7L);
	}

	static DayOfWeek dayOfWeek(final long jdn){
		return DayOfWeek.of(weekday(jdn) + 1);
	}


	private static String format(final int year, final Month month, final int day){
		//the astronomical years 0, -1, ... are the years 1 BC, 2 BC, ... (there is no year 0)
		final String written = (year > 0? String.valueOf(year): (1 - year) + " BC");
		return day + StringUtils.SPACE + month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + StringUtils.SPACE + written;
	}

}
