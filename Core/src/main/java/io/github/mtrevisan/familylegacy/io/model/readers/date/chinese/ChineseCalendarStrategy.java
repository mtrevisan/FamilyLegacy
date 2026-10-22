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
package io.github.mtrevisan.familylegacy.io.model.readers.date.chinese;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarConverterPlugin;
import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


public class ChineseCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Chinese Lunisolar Calendar.
	 * Uses Sunset astronomical engine for precise solar longitude (Zhongqi)
	 * and lunar conjunctions at Beijing mean time (UTC+8).
	 */
	@Override
	public long parseToJdn(final String rawDate){
		// Expected native format: "光绪二十五年 庚子年 正月 初一" or "2020年 庚子年 正月 初一"
		final ChineseDateInput input = ChineseCalendarParser.parse(rawDate);
		final int year = input.year();
		final int month = input.month();
		final boolean leapMonth = input.leapMonth();
		final int day = input.day();

		if(month < 0){
			// Step A: Dynamically determine the spatial window of this exact solar year.
			// Instead of a fixed June anchor which misses February (Lìchūn) or November (Lìdōng),
			// we anchor onto the specific calendar month space corresponding to the season.
			final int approximateTargetGregorianMonth = switch(month) {
				case CalendarType.SEASON_SPRING -> 2;  // Lìchūn -> Feb
				case CalendarType.SEASON_SUMMER -> 5;  // Lìxià  -> May
				case CalendarType.SEASON_AUTUMN -> 8;  // Lìqiū  -> Aug
				case CalendarType.SEASON_WINTER -> 11; // Lìdōng -> Nov
				default -> 6;
			};

			final long baselineApproxJdn = Math.floorDiv(48525L * year, 400L)
				- Math.floorDiv(year, 100L)
				+ Math.floorDiv(153L * (approximateTargetGregorianMonth + 1), 5L)
				+ 1 + 1721423L;
			final double approximateMiddleJdUT = baselineApproxJdn - 0.5;

			// Step B: Map negative flag parameters directly onto true solar ecliptic target boundaries.
			// Maps the 4 primary Solar Terms boundaries to their precise tropical longitude radian coordinates:
			// Qiū / Lìqiū (-1)  = 135° = 3𝜋 / 4
			// Dōng / Lìdōng (-2) = 225° = 5𝜋 / 4
			// Chūn / Lìchūn (-3) = 315° = 7𝜋 / 4
			// Xià / Lìxià (-4)  = 45°  = 𝜋 / 4
			final double targetLongitude = getTargetLongitude(month);

			// Step C: Trigger exact calculation to find the point where the Sun enters this solar term
			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateMiddleJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}
		else
			// Standard numeric execution path using your existing Chinese astronomical engine rules
			return ChineseCalendarAstronomicalEngine.toJdn(year, month, leapMonth, day);
	}

	private static double getTargetLongitude(final int month){
		return switch(month){
			case CalendarType.SEASON_AUTUMN -> 0.75 * Math.PI;   // Lìqiū (Start of Autumn)
			case CalendarType.SEASON_WINTER -> 1.25 * Math.PI;   // Lìdōng (Start of Winter)
			case CalendarType.SEASON_SPRING -> 1.75 * Math.PI;   // Lìchūn (Start of Spring)
			case CalendarType.SEASON_SUMMER -> 0.25 * Math.PI;   // Lìxià (Start of Summer)
			default -> 0.;
		};
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate) {
		final ChineseDateInput input = ChineseCalendarParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
