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
package io.github.mtrevisan.calendars.persian;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


public class PersianCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * Years in which the 33-year leap pattern restarts, so that the arithmetic calendar follows the official astronomical one
	 * (vernal equinox before or after noon at Tehran) within the years -61 to 3177 AP.
	 *
	 * @see <a href="https://doi.org/10.1007/BF00055188">K. M. Borkowski, The Persian calendar for 3000 years, Earth, Moon and Planets
	 * 74, 1996</a>
	 */
	private static final int[] BREAKS = {-61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192, 2262, 2324,
		2394, 2456, 3178};


	/** The difference between the Gregorian year in which the Persian year starts (March) and the Persian year. */
	private static final int GREGORIAN_OFFSET = 621;
	private static final int ESFAND = 12;
	private static final int COMMON_YEAR_ESFAND_DAYS = 29;
	private static final int LEAP_YEAR_DAYS = 366;


	@Override
	public long parseToJdn(final String rawDate){
		// Expected native format: "۱ فروردین ۱۳۰۰" or "۱۰ رمضان ۱۲۶۸ هـ.ق"
		final YearMonthDay date = PersianCalendarParser.parse(rawDate);
		final int year = date.year();
		// a missing month stands for the first month of the year
		final int month = date.monthOrDefault();
		// a missing day stands for the first day of the month
		final int day = date.dayOrDefault();

		if(month < 0){
			// the year starts at the vernal equinox of the Gregorian year + 621: the four seasons fall in that same Gregorian year
			// anchored on the first day of the Gregorian month of the season: the event nearest to it is the one searched
			final long anchorJdn = GregorianCalendarStrategy.toJdn(year + GREGORIAN_OFFSET, CalendarSeason.gregorianSeasonMonth(month), 1);
			return SeasonSolver.jdnNear(anchorJdn, month, "Persian");
		}

		// Esfand has 30 days only in a leap year
		if(month == ESFAND && day == COMMON_YEAR_ESFAND_DAYS + 1 && toJdn(year + 1, 1, 1) - toJdn(year, 1, 1) != LEAP_YEAR_DAYS)
			throw new IllegalArgumentException("Esfand of the Persian year " + year + " has " + COMMON_YEAR_ESFAND_DAYS + " days: " + rawDate);

		// Standard numeric execution path using the arithmetic baseline formula for Persian dates
		return toJdn(year, month, day);
	}

	private static long toJdn(final int year, final int month, final int day){
		if(year < BREAKS[0] || year >= BREAKS[BREAKS.length - 1])
			return toJdnArithmetic(year, month, day);

		// Find the Gregorian day of March on which 1 Farvardin falls (Borkowski algorithm)
		final int gregorianYear = year + 621;
		int leapJ = -14;
		int jp = BREAKS[0];
		int jump = 0;
		for(int i = 1; i < BREAKS.length; i ++){
			final int jm = BREAKS[i];
			jump = jm - jp;
			if(year < jm)
				break;

			leapJ += (jump / 33) * 8 + (jump % 33) / 4;
			jp = jm;
		}
		final int n = year - jp;
		leapJ += (n / 33) * 8 + (n % 33 + 3) / 4;
		if(jump % 33 == 4 && jump - n == 4)
			leapJ ++;
		final int leapG = gregorianYear / 4 - ((gregorianYear / 100 + 1) * 3) / 4 - 150;
		final int march = 20 + leapJ - leapG;

		return GregorianCalendarStrategy.toJdn(gregorianYear, 3, march) + daysBeforeMonth(month) + (day - 1);
	}

	private static long toJdnArithmetic(final int year, final int month, final int day){
		// Persian epoch base year
		final long epBase = year - (year >= 0? 474: 473);

		// Year inside the 2820-year cycle
		final long epYear = 474 + Math.floorMod(epBase, 2820);

		// Julian Day Number
		return day + daysBeforeMonth(month) + (epYear * 682 - 110) / 2816 + (epYear - 1) * 365
			+ Math.floorDiv(epBase, 2820) * 1_029_983L + 1_948_320L;
	}

	private static long daysBeforeMonth(final int month){
		// The first six months have 31 days, the next five 30 days, and Esfand 29 or 30 days
		return (month <= 7
			? (month - 1) * 31L
			: (month - 1) * 30L + 6L);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = PersianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
