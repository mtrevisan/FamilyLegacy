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
package io.github.mtrevisan.calendars.chinese;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


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
			// Step A: anchor on the first day of the Gregorian month of the season (the solar term, e.g. Lichun, falls
			// within a month before it: the event nearest to the anchor is searched)
			final int approximateTargetGregorianMonth = CalendarSeason.gregorianSeasonMonth(month);

			final long baselineApproxJdn = GregorianCalendarStrategy.toJdn(year, approximateTargetGregorianMonth, 1);
			final double exactSeasonJdUT = SeasonSolver.instantAtLongitude(baselineApproxJdn - 0.5, getTargetLongitude(month),
				"Chinese");

			// the civil day in the time zone of the calendar (the same used for the months), not the UT one
			return ChineseCalendarAstronomicalEngine.civilDayOf(exactSeasonJdUT);
		}
		else
			// Standard numeric execution path using your existing Chinese astronomical engine rules
			return ChineseCalendarAstronomicalEngine.toJdn(year, month, leapMonth, day);
	}

	private static double getTargetLongitude(final int month){
		return switch(month){
			case CalendarSeason.SEASON_AUTUMN -> 0.75 * Math.PI;		// Lìqiū (Start of Autumn)
			case CalendarSeason.SEASON_WINTER -> 1.25 * Math.PI;		// Lìdōng (Start of Winter)
			case CalendarSeason.SEASON_SPRING -> 1.75 * Math.PI;		// Lìchūn (Start of Spring)
			case CalendarSeason.SEASON_SUMMER -> 0.25 * Math.PI;		// Lìxià (Start of Summer)
			default -> 0.;
		};
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final ChineseDateInput input = ChineseCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
