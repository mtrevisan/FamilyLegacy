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
package io.github.mtrevisan.calendars.islamic;

import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


public class IslamicCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the specialized text parser
		final YearMonthDay input = IslamicCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = input.monthOrDefault();
		// a missing day stands for the first day of the month
		final int day = input.dayOrDefault();

		if(month < 0){
			// Step A: Dynamically determine the spatial window of this exact lunar year.
			// We sample the start of the current year and the start of the next year.
			final long startOfYearJdn = IslamicCalendarAstronomicalEngine.toJdn(year, 1, 1);
			final long startOfNextYearJdn = IslamicCalendarAstronomicalEngine.toJdn(year + 1, 1, 1);

			// the event nearest to the middle of this lunar year (which drifts through the seasons)
			final double middleJdUT = startOfYearJdn + ((startOfNextYearJdn - startOfYearJdn) / 2.) - 0.5;
			return SeasonSolver.jdn(middleJdUT, month, "Islamic");
		}
		else
			// Standard numerical execution mapping path for typical calendar months
			return IslamicCalendarAstronomicalEngine.toJdn(year, month, day);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = IslamicCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
