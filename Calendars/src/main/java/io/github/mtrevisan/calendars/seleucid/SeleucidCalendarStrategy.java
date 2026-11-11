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
package io.github.mtrevisan.calendars.seleucid;

import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Seleucid (Syro-Macedonian) calendar, the Julian one with the years counted from the Seleucid Era (1 October 312 BCE)
 * and the year starting on 1 October: the months are numbered from October (1) to September (12).
 */
public class SeleucidCalendarStrategy implements CalendarConverterPlugin{

	/** The months from October to December belong to the Julian year that is this number less than the year. */
	private static final int YEAR_OFFSET_AUTUMN = 312;
	/** The months from January to September belong to the Julian year that is this number less than the year. */
	private static final int YEAR_OFFSET_REST = 311;
	/** The last month (December) of the Julian year that is counted from October. */
	private static final int LAST_AUTUMN_MONTH = 3;
	private static final int MONTHS_IN_YEAR = 12;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = SeleucidCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// the year starts on 1 October of the Julian year Y - 312: the autumn equinox just before it and the winter fall in
			// that Julian year, the spring and the summer in the next one
			return SeasonalDates.ofAutumnYear("Seleucid", year - YEAR_OFFSET_AUTUMN, input.month());

		final int month = input.monthOrDefault();
		return JulianCalendarStrategy.toJdn(julianYear(year, month), julianMonth(month), input.dayOrDefault());
	}

	/**
	 * @param year  the Seleucid year
	 * @param month the Seleucid month (October = 1, ..., September = 12)
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

	/** @return the Julian month: October to December (10 to 12), then January to September (1 to 9) */
	private static int julianMonth(final int month){
		return (month <= LAST_AUTUMN_MONTH? month + 9: month - LAST_AUTUMN_MONTH);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = SeleucidCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
