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
package io.github.mtrevisan.calendars.quaker;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * Quaker (Society of Friends) dating in Britain and its colonies, with numbered months and days to avoid the pagan
 * names.
 * <ul>
 *   <li>Up to 1751 the first month is March, and January and February are the 11th and 12th months; the year starts on
 *   25 March, so that the 11th month 1702 is January 1703 in modern reckoning (unless a dual year, e.g. "1702/3",
 *   already gives it).</li>
 *   <li>From 1752 the first month is January, and the year starts on 1 January.</li>
 *   <li>The Julian calendar is in force up to 2 September 1752, followed by the Gregorian calendar from 14 September 1752.</li>
 * </ul>
 */
public class QuakerCalendarStrategy implements CalendarConverterPlugin{

	/** First year whose first month is January (Calendar Act of 1750). */
	private static final int CALENDAR_ACT_YEAR = 1752;
	private static final int REFORM_MONTH = 9;
	/** The last day of the Julian calendar (2 September 1752) */
	private static final int JULIAN_LAST_DAY = 2;
	/** The first day of the Gregorian calendar (14 September 1752) */
	private static final int GREGORIAN_FIRST_DAY = 14;
	/** Lady Day, the start of the year up to 1751. */
	private static final int LADY_DAY = 25;

	private static final int[] MONTH_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};


	@Override
	public long parseToJdn(final String rawDate){
		final QuakerDateInput input = QuakerCalendarParser.parse(rawDate);
		final int year = input.year();

		final int civilMonth;
		final int day;
		final int civilYear;
		if(year < CALENDAR_ACT_YEAR){
			if(input.month() == 0){
				// a year alone starts on Lady Day
				civilMonth = 3;
				day = LADY_DAY;
			}
			else{
				civilMonth = (input.month() <= 10? input.month() + 2: input.month() - 10);
				day = (input.day() > 0? input.day(): 1);
			}

			final boolean beforeLadyDay = (civilMonth < 3 || civilMonth == 3 && day < LADY_DAY);
			if(!beforeLadyDay)
				civilYear = year;
			else
				civilYear = (input.dualYear() != 0? input.dualYear(): year + 1);
		}
		else{
			civilMonth = (input.month() != 0? input.month(): 1);
			day = (input.day() > 0? input.day(): 1);
			civilYear = year;
		}

		return toJdn(civilYear, civilMonth, day, rawDate);
	}

	private static long toJdn(final int year, final int month, final int day, final String rawDate){
		final boolean gregorian = (year > CALENDAR_ACT_YEAR
			|| year == CALENDAR_ACT_YEAR && (month > REFORM_MONTH || month == REFORM_MONTH && day >= GREGORIAN_FIRST_DAY));
		if(year == CALENDAR_ACT_YEAR && month == REFORM_MONTH && day > JULIAN_LAST_DAY && day < GREGORIAN_FIRST_DAY)
			throw new IllegalArgumentException("The days from 3 to 13 September 1752 were skipped by the Gregorian reform: "
				+ rawDate);

		final boolean leapYear = (gregorian
			? year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
			: Math.floorMod(year, 4) == 0);
		final int maxDays = MONTH_DAYS[month - 1] + (month == 2 && leapYear? 1: 0);
		if(day > maxDays)
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries: " + rawDate);

		return (gregorian
			? GregorianCalendarStrategy.toJdn(year, month, day)
			: JulianCalendarStrategy.toJdn(year, month, day));
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final QuakerDateInput input = QuakerCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
