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
package io.github.mtrevisan.calendars.sovieteternal;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


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
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Determine an approximate baseline anchor for the year.
			// Soviet dates align completely with Gregorian absolute years. Gregorian year 1970-01-01 = JDN 2440588L.
			final int m = CalendarSeason.gregorianSeasonMonth(month);
			final long baselineApproxJdn = GregorianCalendarStrategy.toJdn(year, m, 1);
			final double exactSeasonJdUT = exactSeasonJdUT(baselineApproxJdn, month);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using your existing arithmetic baseline formula for Soviet dates
			// Since the production cycles share solar boundaries, it maps directly back onto Gregorian JDN steps:
			return GregorianCalendarStrategy.toJdn(year, month, day);
	}

	private static double exactSeasonJdUT(final long baselineApproxJdn, final int month){
		final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

		// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
		final double targetLongitude = CalendarSeason.getGeneralTargetLongitude(month);

		// Step C: Execute high-precision calculation to track the exact instant the Sun cross point occurs
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Sovietic Eternal calendar requires the Ephemeris plugin.");

		return engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final SovietEternalDateInput input = SovietEternalCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
