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
package io.github.mtrevisan.calendars.frenchrepublican;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class FrenchRepublicanCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural components using the specialized text parser
		final FrenchRepublicanDateInput input = FrenchRepublicanCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Determine an approximate baseline anchor for the year.
			// Year 1 began at JDN 2375840L (September 22, 1792).
			final long startOfYearJdn = 2375840L + (long)((year - 1) * 365.2422);
			final double approximateMiddleJdUT = startOfYearJdn + 182.5 - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the Sun cross point occurs
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("French Republican calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// Standard arithmetic execution mapping path for regular months (Romme arithmetic rule alignment)
			final long epochJdn = 2375840L; // JDN of September 22, 1792

			// Days elapsed = (years * 365) + leap corrections + month days + current day offset
			final long daysToAdd = (year - 1) * 365L
				// Simplified Romme variant leap steps
				+ (year / 4)
				+ (month - 1) * 30L
				+ (day - 1);

			return epochJdn + daysToAdd;
		}
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final FrenchRepublicanDateInput input = FrenchRepublicanCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
