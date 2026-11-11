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
package io.github.mtrevisan.calendars.ethiopian;

import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Ethiopian calendar: twelve months of thirty days and a month of five days, six in the year before a Julian leap year
 * (the year whose number leaves 3 in the division by 4), so that the year starts on 29 August of the Julian calendar (30
 * August after a Julian leap day).
 */
public class EthiopianCalendarStrategy implements CalendarConverterPlugin{

	/** Incarnation epoch, Amete Mihret (29 August 8 CE Julian). */
	private static final long EPOCH_JDN = 1724221L;
	private static final long DAYS_IN_YEAR = 365L;
	private static final long DAYS_IN_MONTH = 30L;
	private static final int LEAP_CYCLE = 4;
	/** The year number that, in the division by 4, leaves the remainder of the leap years. */
	private static final int LEAP_REMAINDER = 3;
	/** The Julian year in which the year starts is this number more than the year of the calendar. */
	private static final int JULIAN_YEAR_OFFSET = 7;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = EthiopianCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// the year starts at the end of August of the Julian year Y + offset: the autumn and the winter fall in that
			// Julian year, the spring and the summer in the next one
			return SeasonalDates.ofAutumnYear("Ethiopian", year + JULIAN_YEAR_OFFSET, input.month());

		// 365 days per year flat, plus a leap day every 4 years
		return EPOCH_JDN + (year - 1L) * DAYS_IN_YEAR
			+ Math.floorDiv(year, LEAP_CYCLE)
			+ (input.monthOrDefault() - 1L) * DAYS_IN_MONTH
			+ input.dayOrDefault() - 1L;
	}

	/**
	 * @param year the year
	 * @return whether the year has six intercalary days (it is the one before a Julian leap year)
	 */
	public static boolean isLeapYear(final int year){
		return (Math.floorMod(year, LEAP_CYCLE) == LEAP_REMAINDER);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = EthiopianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
