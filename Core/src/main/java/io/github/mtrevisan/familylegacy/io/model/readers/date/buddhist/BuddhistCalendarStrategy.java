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
package io.github.mtrevisan.familylegacy.io.model.readers.date.buddhist;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class BuddhistCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the specialized text parser
		final BuddhistDateInput input = BuddhistCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		// Resolve true Gregorian temporal mapping baseline (Epoch offset: -543 years)
		final int gregorianYear = year - 543;

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target year.
			final int m = switch(month){
				case CalendarType.SEASON_SPRING -> 3;  // March
				case CalendarType.SEASON_SUMMER -> 6;  // June
				case CalendarType.SEASON_AUTUMN -> 9;  // September
				case CalendarType.SEASON_WINTER -> 12; // December
				default -> 6;
			};
			final long baselineApproxJdn = GregorianCalendarStrategy.toJdn(gregorianYear, m, 1);
			final double approxMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			// Matching structural alignments: Autumn (-1) = 𝜋, Winter (-2) = 3𝜋/2, Spring (-3) = 0, Summer (-4) = 𝜋/2
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-precision calculation to track the exact instant the Sun enters this season
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approxMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using the canonical algorithm for regular Gregorian dates via gregorianYear
			return GregorianCalendarStrategy.toJdn(gregorianYear, month, day);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final BuddhistDateInput input = BuddhistCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
