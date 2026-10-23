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
package io.github.mtrevisan.calendars.buddhist;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * The Thai solar calendar, with the years of the Buddhist era (BE, พ.ศ., the Gregorian year + 543), or of the
 * Rattanakosin era (RS, ร.ศ., the Gregorian year − 1781, used from 1889 to 1912), e.g. "1 มกราคม 2567", "๑ มกราคม ๒๔๘๔".
 * <p>
 * The solar calendar with Western months was adopted on 1 April 1889 (RS 108), with the year starting on 1 April; from
 * 1941 the year starts on 1 January, so that 2483 BE lasted from 1 April to 31 December 1940. The earlier dates are
 * converted proleptically (the documents of the time used the lunar calendar).
 */
public class BuddhistCalendarStrategy implements CalendarConverterPlugin{

	private static final int BUDDHIST_OFFSET = 543;
	private static final int RATTANAKOSIN_OFFSET = 1781;
	/** The first Gregorian year starting on 1 January (2484 BE). */
	private static final int FIRST_JANUARY_NEW_YEAR = 1941;
	private static final int APRIL = 4;

	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the specialized text parser
		final BuddhistDateInput input = BuddhistCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		// Resolve true Gregorian temporal mapping baseline (Epoch offset: -543 years, or +1781 for the Rattanakosin era); up to
		// 2483 BE the year started on 1 April, so January to March belong to the next Gregorian year
		final int startYear = (input.rattanakosin()? year + RATTANAKOSIN_OFFSET: year - BUDDHIST_OFFSET);
		final boolean aprilNewYear = (startYear < FIRST_JANUARY_NEW_YEAR);
		if(aprilNewYear && input.month() >= 1 && input.month() <= 3 && startYear + 1 >= FIRST_JANUARY_NEW_YEAR)
			throw new IllegalArgumentException("The year 2483 BE lasted from 1 April to 31 December 1940: " + rawDate);
		if(aprilNewYear && input.month() == 0)
			return GregorianCalendarStrategy.toJdn(startYear, APRIL, 1);
		final int gregorianYear = (aprilNewYear && month >= 1 && month < APRIL? startYear + 1: startYear);

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
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Buddhist calendar requires the Ephemeris plugin.");

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
