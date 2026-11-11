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

import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;


public class IndianCalendarStrategy implements CalendarConverterPlugin{

	/**
	 * The Gregorian month in which each Ritu starts, indexed by -flag - 1 (Sharad, Shishira, Vasanta, Grishma, Varsha, Hemanta),
	 * given the flags of {@link IndianCalendarParser}: Sharad 21 Sep, Shishira 21 Jan (of the next Gregorian year), Vasanta
	 * 21 Mar, Grishma 21 May, Varsha 21 Jul, Hemanta 21 Nov.
	 */
	private static final int[] RITU_ANCHOR_MONTHS = {9, 1, 3, 5, 7, 11};
	private static final int RITU_ANCHOR_DAY = 21;

	/**
	 * Indian National Calendar (Saka Samvat).
	 * Epoch: Saka 0 = 78 CE (Gregorian).
	 * Year starts on 1 Chaitra (March 21 in Gregorian leap years, March 22 in common years).
	 */
	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural items using the updated Unicode text parser
		final YearMonthDay input = IndianCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = input.monthOrDefault();
		// a missing day stands for the first day of the month
		final int day = input.dayOrDefault();

		// Resolve true Gregorian temporal mapping baseline (Saka Epoch offset: +78 years)
		final int gregorianYear = year + 78;

		if(month < 0){
			// Anchor on a day near the beginning of the Ritu: each Ritu starts when the Sun enters a multiple of 60 degrees
			// of tropical longitude (Vasanta at the vernal equinox, then every two solar months)
			final int ritu = -month - 1;
			final int anchorMonth = RITU_ANCHOR_MONTHS[ritu];
			final int anchorYear = (month == IndianCalendarParser.RITU_SHISHIRA? gregorianYear + 1: gregorianYear);
			final long baselineApproxJdn = GregorianCalendarStrategy.toJdn(anchorYear, anchorMonth, RITU_ANCHOR_DAY);
			return SeasonSolver.jdnAtLongitude(baselineApproxJdn - 0.5, getTargetLongitude(month), "Indian");
		}
		else{
			// Standard numeric execution path using the canonical algorithm for regular Saka solar months
			final boolean isLeap = GregorianCalendarStrategy.isLeapYear(gregorianYear);
			final int chaitraStartDay = (isLeap? 21: 22);

			if(day > monthLength(month, isLeap))
				throw new IllegalArgumentException("The month " + month + " of the Saka year " + year + " has "
					+ monthLength(month, isLeap) + " days: " + rawDate);

			// Chaitra 1 falls on 22 March (21 March in Gregorian leap years)
			final long startOfYearJdn = GregorianCalendarStrategy.toJdn(gregorianYear, 3, chaitraStartDay);

			int elapsedDays = 0;
			for(int m = 1; m < month; m ++)
				elapsedDays += monthLength(m, isLeap);

			return startOfYearJdn + elapsedDays + (day - 1);
		}
	}

	/**
	 * @param month  the month (1 to 12)
	 * @param isLeap whether the Gregorian year in which the Saka year starts is a leap one
	 * @return the number of days of the month: Chaitra has 30 days (31 in a leap year), the next five months 31, the others 30
	 */
	private static int monthLength(final int month, final boolean isLeap){
		return switch(month){
			case 1 -> (isLeap? 31: 30);
			case 2, 3, 4, 5, 6 -> 31;
			default -> 30;
		};
	}

	/**
	 * @param month the Ritu flag
	 * @return the tropical longitude [rad] at which the Ritu starts
	 */
	private static double getTargetLongitude(final int month){
		return switch(month){
			case IndianCalendarParser.RITU_VASANTA -> 0.;
			case IndianCalendarParser.RITU_GRISHMA -> Math.PI / 3.;
			case IndianCalendarParser.RITU_VARSHA -> 2. * Math.PI / 3.;
			case IndianCalendarParser.RITU_SHARAD -> Math.PI;
			case IndianCalendarParser.RITU_HEMANTA -> 4. * Math.PI / 3.;
			case IndianCalendarParser.RITU_SHISHIRA -> 5. * Math.PI / 3.;
			default -> throw new IllegalArgumentException("Unknown Ritu: " + month);
		};
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = IndianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
