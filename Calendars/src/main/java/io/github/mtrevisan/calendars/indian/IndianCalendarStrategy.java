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
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class IndianCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Indian National Calendar (Saka Samvat).
	 * Epoch: Saka 0 = 78 CE (Gregorian).
	 * Year starts on 1 Chaitra (March 21 in Gregorian leap years, March 22 in common years).
	 */
	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the updated Unicode text parser
		final IndianDateInput input = IndianCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = (input.month() != 0? input.month(): 1);
		// a missing day stands for the first day of the month
		final int day = (input.day() > 0? input.day(): 1);

		// Resolve true Gregorian temporal mapping baseline (Saka Epoch offset: +78 years)
		final int gregorianYear = year + 78;

		if(month < 0){
			// Step A: Calculate a dynamic baseline anchor for the center of the target year.
			// We calculate June 1st (Month 6) of that target Gregorian framework year.
			// Maps each unique traditional Ritu directly onto its target Gregorian month neighborhood
			final int m = switch(month){
				case IndianCalendarParser.RITU_VASANTA -> 3;  // Spring -> March
				case IndianCalendarParser.RITU_GRISHMA -> 6;  // Summer -> June
				case IndianCalendarParser.RITU_VARSHA -> 8;   // Monsoon -> August
				case IndianCalendarParser.RITU_SHARAD -> 9;   // Autumn -> September
				case IndianCalendarParser.RITU_HEMANTA -> 11; // Pre-Winter -> November
				case IndianCalendarParser.RITU_SHISHIRA -> 12;// Winter -> December
				default -> 6;
			};
			final long baselineApproxJdn = GregorianCalendarStrategy.toJdn(gregorianYear, m, 1);
			final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries.
			// Maps the six traditional Ritus to their precise tropical longitude cross-points or structural arc mid-points:
			// Sharad (Autumn = 𝜋), Shishira (Winter = 3𝜋/2), Vasanta (Spring = 0), Grishma (Summer = 𝜋/2)
			// Varsha and Hemanta correspond to the specific solar cross-points tracking mid-season peaks (𝜋/3 offsets).
			final double targetLongitude = getTargetLongitude(month);

			// Step C: Execute high-precision calculation to track the exact instant the Sun enters this season
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else{
			// Standard numeric execution path using the canonical algorithm for regular Saka solar months
			final boolean isLeap = isLeapYear(gregorianYear);
			final int chaitraStartDay = (isLeap? 21: 22);

			// Chaitra 1 falls on 22 March (21 March in Gregorian leap years)
			final long startOfYearJdn = GregorianCalendarStrategy.toJdn(gregorianYear, 3, chaitraStartDay);

			int elapsedDays = 0;
			for(int m = 1; m < month; m ++){
				elapsedDays += switch(m){
					case 1 -> (isLeap? 31: 30);
					case 2, 3, 4, 5, 6 -> 31;
					default -> 30;
				};
			}

			return startOfYearJdn + elapsedDays + (day - 1);
		}
	}

	private static double getTargetLongitude(final int month){
		return switch(month){
			// Vasanta / Spring (Vernal Equinox)
			case IndianCalendarParser.RITU_VASANTA -> 0.;
			// Grishma / Summer (Estival Solstice)
			case IndianCalendarParser.RITU_GRISHMA -> 0.5 * Math.PI;
			// Varsha / Monsoon cross-point shift peak
			case IndianCalendarParser.RITU_VARSHA -> (2. / 3.) * Math.PI;
			// Sharad / Autumn (Autumnal Equinox)
			case IndianCalendarParser.RITU_SHARAD -> Math.PI;
			// Hemanta / Pre-Winter cross-point shift peak
			case IndianCalendarParser.RITU_HEMANTA -> (11. / 6.) * Math.PI;
			// Shishira / Winter (Hibernal Solstice)
			case IndianCalendarParser.RITU_SHISHIRA -> 1.5 * Math.PI;
			default -> 0.;
		};
	}

	private static boolean isLeapYear(final int year){
		return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final IndianDateInput input = IndianCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
