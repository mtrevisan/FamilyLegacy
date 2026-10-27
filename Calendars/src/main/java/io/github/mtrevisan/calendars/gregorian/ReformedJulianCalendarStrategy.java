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

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.DateInterpretation;

import java.util.List;
import java.util.Locale;


public class ReformedJulianCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.monthOrFirst();
		final int day = input.dayOrFirst();
		// century years are leap only when they give remainder 200 or 600 when divided by 900
		final int centuryRemainder = Math.floorMod(year, 900);
		input.validateLeapDay(Math.floorMod(year, 4) == 0
			&& (year % 100 != 0 || centuryRemainder == 200 || centuryRemainder == 600));
		Indiction.validate(input, year);

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target year.
			// We calculate June 1st (Month 6) of that target Reformed Julian year to center our search window.
			final double exactSeasonJdUT = exactSeasonJdUT(year, month);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// Standard numeric execution path using the Milanković algorithm for regular Reformed Julian dates
			final int y = (month <= 2? year - 1: year);
			final int m = (month <= 2? month + 12: month);

			return Math.floorDiv(365L * y, 1L)
				+ Math.floorDiv(y, 4L)
				- Math.floorDiv(y, 100L)
				+ Math.floorDiv(2L * Math.floorDiv(y, 100L) + 6, 9L)
				+ Math.floorDiv(153L * (m + 1), 5L)
				+ day + 1720997L;
		}
	}

	private static double exactSeasonJdUT(final int year, final int month){
		final int y = year;
		final int m = CalendarSeason.gregorianSeasonMonth(month);
		// Reformed Julian 900-year cycle mathematical calculation framework
		final long baselineApproxJdn = Math.floorDiv(365L * y, 1L)
			+ Math.floorDiv(y, 4L)
			- Math.floorDiv(y, 100L)
			+ Math.floorDiv(2L * Math.floorDiv(y, 100L) + 6, 9L)
			+ Math.floorDiv(153L * (m + 1), 5L)
			+ 1 + 1720997L;
		final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

		// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
		// Matching structural alignments: Autumn (-1) = 𝜋, Winter (-2) = 3𝜋/2, Spring (-3) = 0, Summer (-4) = 𝜋/2
		final double targetLongitude = CalendarSeason.getGeneralTargetLongitude(month);

		// Step C: Execute high-precision calculation to track the exact instant the Sun enters this season
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Reformed Julian calendar requires the Ephemeris plugin.");

		return engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);
	}

	@Override
	public List<DateInterpretation> getInterpretations(final String rawDate, final Locale locale){
		return GregorianCalendarParser.interpretations(rawDate, locale);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
