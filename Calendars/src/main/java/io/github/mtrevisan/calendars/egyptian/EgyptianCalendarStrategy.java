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
package io.github.mtrevisan.calendars.egyptian;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


public class EgyptianCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final EgyptianDateInput input = EgyptianCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		// Era of Nabonassar (Ptolemy, Almagest): 1 Thoth 1 = 26 February 747 BCE Julian = JDN 1448638.
		final long egyptianEpochJdn = 1448638L;

		if(month < 0){
			// Step A: Determine the absolute linear window of this shifting Egyptian year.
			final double exactSeasonJdUT = exactSeasonJdUT(egyptianEpochJdn, year, month);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			return egyptianEpochJdn + 365L * (year - 1L) + 30L * (month - 1L) + (day - 1L);
	}

	private static double exactSeasonJdUT(final long egyptianEpochJdn, final int year, final int month){
		final long startOfYearJdn = egyptianEpochJdn + (year - 1) * 365L;

		// Establish the middle of this specific shifting year block as an initial guess anchor
		final double approximateMiddleJdUT = startOfYearJdn + 182.5 - 0.5;

		// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
		// Note: Mapped to match standard seasonal definitions:
		// Akhet (Autumn = 𝜋), Peret (Winter = 3𝜋/2), Shemu (Summer = 𝜋/2)
		final double targetLongitude = CalendarSeason.getGeneralTargetLongitude(month);

		// Step C: Execute high-fidelity calculation to track the exact instant the transit occurs
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Egyptian calendar requires the Ephemeris plugin.");

		return engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final EgyptianDateInput input = EgyptianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
