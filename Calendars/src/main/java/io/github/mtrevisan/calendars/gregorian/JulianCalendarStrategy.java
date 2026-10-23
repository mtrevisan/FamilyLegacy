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
package io.github.mtrevisan.calendars.gregorian;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class JulianCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.monthOrFirst();
		final int day = input.dayOrFirst();
		input.validateLeapDay(Math.floorMod(year, 4) == 0);
		Indiction.validate(input, year);

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target year.
			// We calculate June 1st (Month 6) of that target Julian year to center our search window.
			final int y = year;
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

			// Step C: Execute high-precision calculation to track the exact instant the Sun enters this season
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Julian calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using the canonical algorithm for regular Julian dates
			return toJdn(year, month, day);
	}

	/**
	 * Converts a proleptic Julian date into its Julian Day Number (Meeus, Astronomical Algorithms, ch. 7).
	 *
	 * @param year  the astronomical year (1 BCE = 0)
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return the Julian Day Number
	 */
	public static long toJdn(final int year, final int month, final int day){
		final int y = (month <= 2? year - 1: year);
		final int m = (month <= 2? month + 12: month);

		return Math.floorDiv(1461L * (y + 4716L), 4L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ day - 1524L;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
