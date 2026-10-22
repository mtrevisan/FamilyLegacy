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
package io.github.mtrevisan.familylegacy.io.model.readers.date.sovieteternal;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class SovietEternalCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Soviet Revolutionary Calendar (1929–1940).
	 * <p>
	 * The solar dates and leap year rules were identical to the Gregorian calendar.
	 * The 5 (or 6) national "intercalary/holiday days" outside the standard 5-day cycle
	 * map directly onto the corresponding solar days of the Gregorian system.
	 */
	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural components using the specialized text parser
		final SovietEternalDateInput input = SovietEternalCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		final int day = input.day();

		if(month < 0){
			// Step A: Determine an approximate baseline anchor for the year.
			// Soviet dates align completely with Gregorian absolute years. Gregorian year 1970-01-01 = JDN 2440588L.
			final int y = year;
			final int m = switch(month){
				case CalendarType.SEASON_SPRING -> 3;  // March
				case CalendarType.SEASON_SUMMER -> 6;  // June
				case CalendarType.SEASON_AUTUMN -> 9;  // September
				case CalendarType.SEASON_WINTER -> 12; // December
				default -> 6;
			};
			final long baselineApproxJdn = Math.floorDiv(48525L * y, 400L)
				- Math.floorDiv(y, 100L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ 1 + 1721423L;
			final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-precision calculation to track the exact instant the Sun cross point occurs
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// Standard numeric execution path using your existing arithmetic baseline formula for Soviet dates
			// Since the production cycles share solar boundaries, it maps directly back onto Gregorian JDN steps:
			final int y = (month <= 2? year - 1: year);
			final int m = (month <= 2? month + 12: month);

			return Math.floorDiv(48525L * y, 400L)
				- Math.floorDiv(y, 100L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ day + 1721423L;
		}
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final SovietEternalDateInput input = SovietEternalCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
