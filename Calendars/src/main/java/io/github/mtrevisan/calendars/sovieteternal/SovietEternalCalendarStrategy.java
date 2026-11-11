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
package io.github.mtrevisan.calendars.sovieteternal;

import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * Soviet Revolutionary Calendar (1929-1940).
 * <p>
 * The solar dates and leap year rules were identical to the Gregorian calendar. The five national holidays outside the
 * five-day weeks (Lenin Day, 22 January; the Labor Days, 1 and 2 May; the Industry Days, 7 and 8 November) and the leap day
 * (29 February) are named days that the parser maps to their Gregorian date.
 * </p>
 */
public class SovietEternalCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final YearMonthDay input = SovietEternalCalendarParser.parse(rawDate);

		if(input.isSeason())
			// the dates are the Gregorian ones: the seasons are those of the Gregorian year
			return SeasonalDates.ofGregorianYear("Soviet Eternal", input.year(), input.month());

		return GregorianCalendarStrategy.toJdn(input.year(), input.monthOrDefault(), input.dayOrDefault());
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = SovietEternalCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
