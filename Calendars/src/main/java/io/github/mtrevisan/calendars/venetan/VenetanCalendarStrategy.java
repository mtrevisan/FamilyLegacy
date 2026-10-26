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
package io.github.mtrevisan.calendars.venetan;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.yearstart.YearStartStyleCalendarStrategy;


public class VenetanCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		if(month < 0){
			// Step A: Align the approximate year coordinate for the seasonal solver anchor.
			// Since more Veneto shifts January/February back, we anchor onto the real historical timeline.
			// June calculation baseline
			final long baselineApproxJdn = toJdn(year, 4, 1);
			final double exactEventJdUT = exactEventJdUT(baselineApproxJdn, month);

			return (long)Math.floor(exactEventJdUT + 0.5);
		}
		else{
			// the indiction is checked against the civil year (counted from 1 January)
			final int civilMonth = (month <= 10? month + 2: month - 10);
			Indiction.validate(input.indiction(), YearStartStyleCalendarStrategy.VENETAN.toCommonYear(year, civilMonth, day),
				(input.month() != 0? civilMonth: 0));
			return toJdn(year, month, day);
		}
	}

	private static double exactEventJdUT(final long baselineApproxJdn, final int month){
		final double approximateJdUT = baselineApproxJdn - 0.5;

		// Step B: Resolve exact longitude map assignments matching the parser flags
		final double targetLongitude = CalendarSeason.getGeneralTargetLongitude(month);

		// Step C: Trigger engine execution to track the exact solar coordinate transition point
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Venetan calendar requires the Ephemeris plugin.");

		return engine.findSolarLongitudeEvent(approximateJdUT, targetLongitude);
	}

	private static long toJdn(final int year, final int month, final int day){
		// Convert more Veneto months (1 = March, ..., 12 = February) to civil months: January and February (months 11
		// and 12) belong to the next civil year, which the year-start style takes care of.
		// The Republic of Venice adopted the Gregorian calendar in October 1582 (4 October was followed by 15 October),
		// while keeping the more Veneto start of the year
		final int civilMonth = (month <= 10? month + 2: month - 10);
		return YearStartStyleCalendarStrategy.VENETAN.toJdn(year, civilMonth, day);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
