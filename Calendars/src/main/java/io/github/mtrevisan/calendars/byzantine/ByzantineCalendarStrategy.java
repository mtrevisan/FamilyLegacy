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
package io.github.mtrevisan.calendars.byzantine;

import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Byzantine calendar, the Julian one with the years counted from the creation of the world (1 September 5509 BCE) and
 * the year starting on 1 September: the months are numbered from September (1) to August (12).
 */
public class ByzantineCalendarStrategy implements CalendarConverterPlugin{

	/** The months from September to December belong to the Julian year that is this number less than the year. */
	private static final int YEAR_OFFSET_AUTUMN = 5509;
	/** The months from January to August belong to the Julian year that is this number less than the year. */
	private static final int YEAR_OFFSET_REST = 5508;
	/** The last month (December) of the Julian year that is counted from September. */
	private static final int LAST_AUTUMN_MONTH = 4;
	private static final int MONTHS_IN_YEAR = 12;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = ByzantineCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// the year starts on 1 September of the Julian year Y - 5509: the autumn and the winter fall in that Julian year,
			// the spring and the summer in the next one
			return SeasonalDates.ofAutumnYear("Byzantine", year - YEAR_OFFSET_AUTUMN, input.month());

		final int month = input.monthOrDefault();
		return JulianCalendarStrategy.toJdn(julianYear(year, month), julianMonth(month), input.dayOrDefault());
	}

	/**
	 * @param year  the Byzantine year
	 * @param month the Byzantine month (September = 1, ..., August = 12)
	 * @return the days of the month, or {@code 0} if the month does not exist
	 */
	public static int monthLength(final int year, final int month){
		if(month < 1 || month > MONTHS_IN_YEAR)
			return 0;

		return MonthLengths.julian(julianYear(year, month), julianMonth(month));
	}

	private static int julianYear(final int year, final int month){
		return (month <= LAST_AUTUMN_MONTH? year - YEAR_OFFSET_AUTUMN: year - YEAR_OFFSET_REST);
	}

	/** @return the Julian month: September to December (9 to 12), then January to August (1 to 8) */
	private static int julianMonth(final int month){
		return (month <= LAST_AUTUMN_MONTH? month + 8: month - LAST_AUTUMN_MONTH);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = ByzantineCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
