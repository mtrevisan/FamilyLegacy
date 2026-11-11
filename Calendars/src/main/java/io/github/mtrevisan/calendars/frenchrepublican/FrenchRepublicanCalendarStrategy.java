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
package io.github.mtrevisan.calendars.frenchrepublican;

import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The French Republican calendar, with the arithmetic rule of the four-year cycle of the decree: twelve months of thirty
 * days and five complementary days (Sansculottides), six in the sextile years III, VII, XI, XV, ... (the year whose number
 * leaves 3 in the division by 4). The rule of Romme (the sextile years IV, VIII, ... with the corrections of the
 * centuries) was never adopted, and the calendar was abolished in the year XIV, so the years after it are an extrapolation.
 */
public class FrenchRepublicanCalendarStrategy implements CalendarConverterPlugin{

	/** Julian Day Number of 1 Vendemiaire of the year I (22 September 1792 Gregorian). */
	private static final long EPOCH_JDN = 2375840L;
	private static final long DAYS_IN_YEAR = 365L;
	private static final long DAYS_IN_MONTH = 30L;
	private static final int LEAP_CYCLE = 4;
	private static final int LEAP_REMAINDER = 3;
	/** The year I started in the Gregorian year 1792: the Gregorian year of the start is this number plus the year. */
	private static final int GREGORIAN_OFFSET = 1791;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = FrenchRepublicanCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// the year starts at the autumn equinox of the Gregorian year Y + 1791: the autumn and the winter fall in that
			// year, the spring and the summer in the next one
			return SeasonalDates.ofAutumnYear("French Republican", year + GREGORIAN_OFFSET, input.month());

		return EPOCH_JDN + (year - 1L) * DAYS_IN_YEAR
			+ Math.floorDiv(year, LEAP_CYCLE)
			+ (input.monthOrDefault() - 1L) * DAYS_IN_MONTH
			+ (input.dayOrDefault() - 1L);
	}

	/**
	 * @param year the year
	 * @return whether the year is sextile, with six complementary days
	 */
	public static boolean isLeapYear(final int year){
		return (Math.floorMod(year, LEAP_CYCLE) == LEAP_REMAINDER);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = FrenchRepublicanCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
