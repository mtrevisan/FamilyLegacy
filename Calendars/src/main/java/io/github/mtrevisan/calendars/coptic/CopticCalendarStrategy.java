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
package io.github.mtrevisan.calendars.coptic;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class CopticCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the specialized text parser
		final CopticDateInput input = CopticCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target year.
			// Coptic Year + 283 gives the approximate Julian timeline year.
			// We calculate June 1st (Month 6) of that target year on the underlying Julian framework.
			final int y = year + 283;
			final int m = switch(month){
				case CalendarType.SEASON_SPRING -> 3;  // March
				case CalendarType.SEASON_SUMMER -> 6;  // June
				case CalendarType.SEASON_AUTUMN -> 9;  // September
				case CalendarType.SEASON_WINTER -> 12; // December
				default -> 6;
			};
			final long baselineApproxJdn = Math.floorDiv(1461L * (y + 4716L), 4L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ 1 - 1524L;
			final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			// Matching structural alignments: Autumn (-1) = 𝜋, Winter (-2) = 3𝜋/2, Spring (-3) = 0, Summer (-4) = 𝜋/2
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the Sun enters this season
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Coptic calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// Standard arithmetic execution mapping path for typical regular Coptic months (fixed Julian translation)
			final long copticEpochJdn = 1825030L; // Diocletian Era / Era of Martyrs

			// Arithmetic tracking: 365 days per year flat + leap years added every 4 years
			return copticEpochJdn + (year - 1) * 365L
				+ Math.floorDiv(year, 4)
				+ (month - 1) * 30L
				+ day - 1;
		}
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final CopticDateInput input = CopticCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
