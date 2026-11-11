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
package io.github.mtrevisan.calendars.egyptian;

import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Egyptian civil calendar of the Era of Nabonassar: twelve months of thirty days and five epagomenal days, with no
 * leap year, so that the year wanders through the seasons.
 */
public class EgyptianCalendarStrategy implements CalendarConverterPlugin{

	/** Era of Nabonassar (Ptolemy, Almagest): 1 Thoth 1 = 26 February 747 BCE Julian = JDN 1448638. */
	private static final long EPOCH_JDN = 1448638L;
	private static final long DAYS_IN_YEAR = 365L;
	private static final long DAYS_IN_MONTH = 30L;


	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = EgyptianCalendarParser.parse(rawDate);

		final long startOfYear = EPOCH_JDN + DAYS_IN_YEAR * (input.year() - 1L);
		if(input.isSeason())
			// the year wanders through the seasons: the first event of the season on or after the start of the year
			return SeasonalDates.onOrAfter("Egyptian", startOfYear, input.month());

		return startOfYear + DAYS_IN_MONTH * (input.monthOrDefault() - 1L) + (input.dayOrDefault() - 1L);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = EgyptianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
