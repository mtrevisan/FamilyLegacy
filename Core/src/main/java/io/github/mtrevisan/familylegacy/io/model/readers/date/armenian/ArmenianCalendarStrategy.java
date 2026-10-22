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
package io.github.mtrevisan.familylegacy.io.model.readers.date.armenian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class ArmenianCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final ArmenianDateInput input = ArmenianCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		final int day = input.day();

		if(month < 0){
			// Step A: Determine the absolute linear window of this shifting Armenian year.
			// The Great Armenian Era epoch starts on July 11, 552 CE Julian = JDN 1922868L.
			final long startOfYearJdn = 1922868L + (year - 1) * 365L;

			// Establish the middle of this specific shifting year block as an initial guess anchor
			final double approximateMiddleJdUT = startOfYearJdn + 182.5 - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-fidelity calculation to track the exact instant the Sun cross point occurs
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard arithmetic execution mapping path for typical regular Armenian months
			// 1922868: JDN of the Great Armenian Era epoch (11 July 552 CE Julian)
			return 1922868L + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final ArmenianDateInput input = ArmenianCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
