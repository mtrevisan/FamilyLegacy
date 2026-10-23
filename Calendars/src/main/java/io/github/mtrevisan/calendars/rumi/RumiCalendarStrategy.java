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

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class RumiCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// Isolate components via the adaptive Rumi parser
		final RumiDateInput input = RumiCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Determine the absolute temporal anchor window for this year.
			// Rumi Year + 584 gives the Gregorian/Julian year parameter space.
			final long startOfYearJdn = toJdn(year, 1, 1);
			final double approximateMiddleJdUT = startOfYearJdn + 182.5 - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the transit occurs
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Rumi calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using your dual-era Rumi calculation engine logic
			return toJdn(year, month, day);
	}

	private static long toJdn(final int year, final int month, final int day){
		// Rumi months are numbered from March (Mart = 1, ..., Kânûn-ı Evvel = 10, Kânûn-ı Sânî = 11, Şubat = 12), year difference is
		// 584 years
		// March=1->3, Dec=10->12
		int calendarMonth = month + 2;
		int calendarYear = year + 584;
		if(month > 10){
			// Jan=11->1, Feb=12->2
			calendarMonth = month - 10;
			// Until 1333 the year started in March, so January and February belong to the next Julian/Gregorian year;
			// from 1334 (1918) the year starts in January
			if(year < 1334)
				calendarYear ++;
		}

		// 1917 reform: 15 Şubat 1332 (Julian) was followed by 1 Mart 1333 (Gregorian)
		if(year > 1332)
			return GregorianCalendarStrategy.toJdn(calendarYear, calendarMonth, day);

		// PRE-REFORM LOGIC: Pure Julian Calendar tracking with March start
		final int y = (calendarMonth <= 2? calendarYear - 1: calendarYear);
		final int m = (calendarMonth <= 2? calendarMonth + 12: calendarMonth);

		return Math.floorDiv(1461L * (y + 4716L), 4L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ day - 1524L;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final RumiDateInput input = RumiCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
