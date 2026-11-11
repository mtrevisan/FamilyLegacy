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
package io.github.mtrevisan.calendars.rumi;

import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Rumi (Ottoman fiscal) calendar: the Julian calendar with the year starting on 1 March (Mart = 1, ..., Kanun-i Evvel =
 * 10, Kanun-i Sani = 11, Subat = 12) and the years counted from the Hijra (584 years less than the Julian ones), until the
 * reform of 1917, when the Gregorian calendar was adopted and the year started on 1 January from 1334 (1918).
 */
public class RumiCalendarStrategy implements CalendarConverterPlugin{

	/** The difference between the Rumi years and the Julian or Gregorian ones. */
	private static final int YEAR_OFFSET = 584;
	/** The last Rumi year of the Julian calendar (the 1917 reform fell in its last month). */
	private static final int LAST_JULIAN_YEAR = 1332;
	/** The first Rumi year that starts on 1 January. */
	private static final int FIRST_JANUARY_YEAR = 1334;
	/** The last month (December) counted from March. */
	private static final int LAST_MARCH_MONTH = 10;
	private static final int MONTHS_IN_YEAR = 12;
	private static final int FEBRUARY_MONTH = 12;
	/** 15 Subat 1332 (Julian, 28 February 1917 Gregorian) was followed by 1 Mart 1333 (Gregorian, 1 March 1917). */
	private static final int REFORM_FEBRUARY_DAYS = 15;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = RumiCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// all the seasons are in the Gregorian year of the March: the year runs from March (or January) to the end of the
			// following February (or December)
			return SeasonalDates.ofGregorianYear("Rumi", year + YEAR_OFFSET, input.month());

		return toJdn(year, input.monthOrDefault(), input.dayOrDefault());
	}

	private static long toJdn(final int year, final int month, final int day){
		final int calendarYear = calendarYear(year, month);
		final int calendarMonth = calendarMonth(month);

		// 1917 reform: 15 Subat 1332 (Julian) was followed by 1 Mart 1333 (Gregorian)
		if(year > LAST_JULIAN_YEAR)
			return GregorianCalendarStrategy.toJdn(calendarYear, calendarMonth, day);

		return JulianCalendarStrategy.toJdn(calendarYear, calendarMonth, day);
	}

	/**
	 * @param year  the Rumi year
	 * @param month the Rumi month (Mart = 1, ..., Subat = 12)
	 * @return the days of the month, or {@code 0} if the month does not exist (the year 1333 ended in December, when the
	 * 	year 1334 started on 1 January)
	 */
	public static int monthLength(final int year, final int month){
		if(month < 1 || month > MONTHS_IN_YEAR)
			return 0;
		if(month > LAST_MARCH_MONTH && year == LAST_JULIAN_YEAR + 1)
			return 0;
		if(year == LAST_JULIAN_YEAR && month == FEBRUARY_MONTH)
			return REFORM_FEBRUARY_DAYS;

		final int calendarYear = calendarYear(year, month);
		final int calendarMonth = calendarMonth(month);
		return (year > LAST_JULIAN_YEAR
			? MonthLengths.gregorian(calendarYear, calendarMonth)
			: MonthLengths.julian(calendarYear, calendarMonth));
	}

	/** @return the Julian or Gregorian month: March to December (3 to 12), then January and February (1 and 2) */
	private static int calendarMonth(final int month){
		return (month > LAST_MARCH_MONTH? month - LAST_MARCH_MONTH: month + 2);
	}

	private static int calendarYear(final int year, final int month){
		final int calendarYear = year + YEAR_OFFSET;
		// until 1333 the year started in March, so January and February belong to the next Julian/Gregorian year; from 1334
		// (1918) the year starts in January
		return (month > LAST_MARCH_MONTH && year < FIRST_JANUARY_YEAR? calendarYear + 1: calendarYear);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = RumiCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
