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
package io.github.mtrevisan.familylegacy.io.model.readers.date.venetan;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class VenetanCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		final int day = input.day();

		if(month < 0){
			// Step A: Align the approximate year coordinate for the seasonal solver anchor.
			// Since more Veneto shifts January/February back, we anchor onto the real historical timeline.
			// June calculation baseline
			final long baselineApproxJdn = toJdn(year, 4, 1);
			final double approximateJdUT = baselineApproxJdn - 0.5;

			// Step B: Resolve exact longitude map assignments matching the parser flags
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Trigger engine execution to track the exact solar coordinate transition point
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactEventJdUT = engine.findSolarLongitudeEvent(approximateJdUT, targetLongitude);
			return (long)Math.floor(exactEventJdUT + 0.5);
		}
		else
			return toJdn(year, month, day);
	}

	private static long toJdn(final int year, final int month, final int day){
		// Convert more Veneto coordinates to standard Julian calendar coordinates
		// Months 1 (March) to 10 (December) share the same chronological year.
		// Months 11 (January) and 12 (February) belong to the next Julian year.
		final int julianYear;
		final int julianMonth;
		if(month <= 10){
			julianYear = year;
			// March=1->3, Dec=10->12
			julianMonth = month + 2;
		}
		else{
			// January and February shift forward by 1 year
			julianYear = year + 1;
			// Jan=11->1, Feb=12->2
			julianMonth = month - 10;
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
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
