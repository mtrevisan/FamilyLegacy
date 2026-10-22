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
package io.github.mtrevisan.familylegacy.io.model.readers.date.hebrew;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class HebrewCalendarStrategy implements CalendarConverterPlugin{

	@Override
	public long parseToJdn(final String rawDate){
		// Note: Traditional Hebrew days start at sunset. This structural JDN anchors to the civil day portion.
		// 1. Unpack structural items using the specialized text parser
		final HebrewDateInput input = HebrewCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		final int day = input.day();

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target Hebrew year.
			// We sample the start of the current year and estimate a middle point shift.
			final long startOfYearJdn = hebrewNewYear(year);
			final double approximateMiddleJdUT = startOfYearJdn + 177. - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries
			// Matching structural alignments: Autumn (-1) = 𝜋, Winter (-2) = 3𝜋/2, Spring (-3) = 0, Summer (-4) = 𝜋/2
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			// Step C: Execute high-precision calculation to track the exact instant the Sun enters this season
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using the rabbinic calculation algorithm for regular Hebrew dates
			return hebrewNewYear(year) + daysBeforeMonth(year, month) + (day - 1);
	}

	/**
	 * Computes the Julian Day Number for Rosh HaShanah (1 Tishrei) of a given Hebrew year.
	 * Strictly applies the historical Dehiyyot postponement rules in the correct mathematical sequence.
	 */
	private long hebrewNewYear(final int year){
		// 1. Calculate the elapsed months and parts since the epoch
		final long monthsElapsed = (235L * year - 234L) / 19L;
		final long partsElapsed = 204L + 793L * (monthsElapsed % 1080L);
		final long hoursElapsed = 11L + 12L * monthsElapsed + 793L * (monthsElapsed / 1080L) + partsElapsed / 1080L;

		// Dynamic day calculation from the raw astronomical Molad calculation
		long day = 1L + 29L * monthsElapsed + hoursElapsed / 24L;
		final long parts = (hoursElapsed % 24L) * 1080L + (partsElapsed % 1080L);

		// Convert to absolute JDN coordinates BEFORE evaluating the day of the week to fix the remainder bug
		long roshHaShanahJdn = day + 347997L;

		// Rule 1: Dehiyyat Molad Zaken (Postpone if Molad occurs at or after noon / 18 hours = 19440 parts)
		if(parts >= 19440L)
			roshHaShanahJdn ++;

		// 2. Identify the absolute day of the week (JDN % 7: 0 = Monday, 1 = Tuesday, ..., 5 = Saturday, 6 = Sunday)
		long dayOfWeek = roshHaShanahJdn % 7L;

		// Rule 3: Dehiyyat GATRAD
		// Postpone if it's a common year, Molad falls on Tuesday (dayOfWeek == 1), and parts >= 9h 204p (9924 parts)
		if(dayOfWeek == 1L && !isLeapYear(year) && parts >= 9924L)
			roshHaShanahJdn += 2L;
			// Rule 4: Dehiyyat BETUAGOT
			// Postpone if it follows a leap year, Molad falls on Monday (dayOfWeek == 0), and parts >= 15h 589p (16789 parts)
		else if(dayOfWeek == 0L && isLeapYear(year - 1) && parts >= 16789L)
			roshHaShanahJdn ++;

		// Re-evaluate day of the week for the final absolute check against prohibited days
		dayOfWeek = roshHaShanahJdn % 7L;

		// Rule 2: Dehiyyat ADU
		// Rosh HaShanah cannot fall on Sunday, Wednesday, or Friday.
		// Mapping JDN % 7: Sunday = 6, Wednesday = 2, Friday = 4
		if(dayOfWeek == 6L || dayOfWeek == 2L || dayOfWeek == 4L)
			roshHaShanahJdn ++;

		return roshHaShanahJdn;
	}

	private boolean isLeapYear(final int year){
		return ((7 * year + 1) % 19 < 7);
	}

	/**
	 * Computes the cumulative elapsed days in a Hebrew year before the given month.
	 * Dynamically adjusts for deficient, regular, and complete year lengths.
	 */
	private int daysBeforeMonth(final int year, final int month){
		// Resolve the absolute length of the current Hebrew year
		final long thisNY = hebrewNewYear(year);
		final long nextNY = hebrewNewYear(year + 1);
		final int yearLength = (int)(nextNY - thisNY);

		final boolean isLeap = isLeapYear(year);

		// Detect custom year configurations (Heshvan and Kislev dynamic lengths)
		// Deficient (353/383 days), Regular (354/384 days), Complete (355/385 days)
		final boolean longCheshvan = (yearLength == 355 || yearLength == 385);
		final boolean shortKislev = (yearLength == 353 || yearLength == 383);

		int days = 0;
		for(int m = 1; m < month; m ++){
			days += switch(m){
				// Tishrei
				case 1 -> 30;
				// Cheshvan
				case 2 -> (longCheshvan? 30: 29);
				// Kislev
				case 3 -> (shortKislev? 29: 30);
				// Tevet
				case 4 -> 29;
				// Shevat
				case 5 -> 30;
				// In a leap year, Month 6 is Adar I (30 days). In a common year, it is Adar (29 days).
				case 6 -> (isLeap? 30: 29);
				// In a leap year, Month 7 is Adar II (29 days). In a common year, it is Nisan (30 days).
				case 7 -> (isLeap? 29: 30);
				// In a leap year, Month 8 is Nisan (30 days). In a common year, it is Iyar (29 days).
				case 8 -> (isLeap? 30: 29);
				// In a leap year, Month 9 is Iyar (29 days). In a common year, it is Sivan (30 days).
				case 9 -> (isLeap? 29: 30);
				// In a leap year, Month 10 is Sivan (30 days). In a common year, it is Tammuz (29 days).
				case 10 -> (isLeap? 30: 29);
				// In a leap year, Month 11 is Tammuz (29 days). In a common year, it is Av (30 days).
				case 11 -> (isLeap? 29: 30);
				// In a leap year, Month 12 is Av (30 days). In a common year, it is Elul (29 days).
				case 12 -> (isLeap? 30: 29);
				// Elul in a leap year (Month 13)
				case 13 -> 29;
				default -> 0;
			};
		}
		return days;
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final HebrewDateInput input = HebrewCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
