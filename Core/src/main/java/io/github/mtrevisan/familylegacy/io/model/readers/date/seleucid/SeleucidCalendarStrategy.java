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
package io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class SeleucidCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final SeleucidDateInput input = SeleucidCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Determine the absolute temporal anchor window for this year.
			// The Seleucid Era (Syro-Macedonian) epoch starts roughly at JDN 1607739L.
			// We calculate an approximate middle point for that specific calendar year.
			final long startOfYearJdn = toJdn(year, 1, 1);
			final double approximateMiddleJdUT = startOfYearJdn + 182.5 - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the transit occurs
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using your existing Julian shifting logic
			return toJdn(year, month, day);
	}

	private static long toJdn(final int year, final int month, final int day){
		// Convert Seleucid year and month to standard Julian calendar coordinates
		// October (1) to December (3) -> Julian year = Seleucid year - 312
		// January (4) to September (12) -> Julian year = Seleucid year - 311
		final int julianYear;
		final int julianMonth;

		if(month <= 3){
			julianYear = year - 312;
			// Oct=10, Nov=11, Dec=12
			julianMonth = month + 9;
		}
		else{
			julianYear = year - 311;
			// Jan=1, Feb=2, ..., Sep=9
			julianMonth = month - 3;
		}

		// Standard Julian Calendar JDN conversion logic (Meeus framework)
		final int y = (julianMonth <= 2? julianYear - 1: julianYear);
		final int m = (julianMonth <= 2? julianMonth + 12: julianMonth);

		return Math.floorDiv(1461L * (y + 4716L), 4L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ day - 1524L;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final SeleucidDateInput input = SeleucidCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
