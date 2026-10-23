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
package io.github.mtrevisan.calendars.liturgical;

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;

import java.time.MonthDay;


/**
 * The computus, the computation of the date of Easter Sunday (Meeus, Astronomical Algorithms, ch. 8).
 */
public final class Computus{

	private Computus(){}


	/**
	 * Computes Easter Sunday with the Julian computus (in use before the Gregorian reform, and still in use by the
	 * Orthodox churches).
	 *
	 * @param year the astronomical year
	 * @return the day of Easter, in the Julian calendar
	 */
	public static MonthDay julianEaster(final int year){
		final int a = Math.floorMod(year, 4);
		final int b = Math.floorMod(year, 7);
		final int c = Math.floorMod(year, 19);
		final int d = (19 * c + 15) % 30;
		final int e = (2 * a + 4 * b - d + 34) % 7;
		final int month = (d + e + 114) / 31;
		final int day = (d + e + 114) % 31 + 1;
		return MonthDay.of(month, day);
	}

	/**
	 * Computes Easter Sunday with the Gregorian computus (the "anonymous Gregorian algorithm").
	 *
	 * @param year the year (from 1583)
	 * @return the day of Easter, in the Gregorian calendar
	 */
	public static MonthDay gregorianEaster(final int year){
		final int a = year % 19;
		final int b = year / 100;
		final int c = year % 100;
		final int d = b / 4;
		final int e = b % 4;
		final int f = (b + 8) / 25;
		final int g = (b - f + 1) / 3;
		final int h = (19 * a + b - d - g + 15) % 30;
		final int i = c / 4;
		final int k = c % 4;
		final int l = (32 + 2 * e + 2 * i - h - k) % 7;
		final int m = (a + 11 * h + 22 * l) / 451;
		final int month = (h + l - 7 * m + 114) / 31;
		final int day = (h + l - 7 * m + 114) % 31 + 1;
		return MonthDay.of(month, day);
	}

	/**
	 * Computes the Julian Day Number of Easter Sunday with the Julian computus.
	 *
	 * @param year the astronomical year
	 * @return the Julian Day Number of Easter Sunday
	 */
	public static long julianEasterJdn(final int year){
		final MonthDay easter = julianEaster(year);
		return JulianCalendarStrategy.toJdn(year, easter.getMonthValue(), easter.getDayOfMonth());
	}

	/**
	 * Computes the Julian Day Number of Easter Sunday as celebrated where a Gregorian reform was adopted: with the Julian
	 * computus up to the year of the reform, with the Gregorian one from the year after (Easter never falls near a
	 * reform, which happened in the autumn or winter).
	 *
	 * @param year   the astronomical year
	 * @param reform the Gregorian reform in force
	 * @return the Julian Day Number of Easter Sunday
	 */
	public static long easterJdn(final int year, final CalendarReform reform){
		if(year <= reform.lastJulianYear())
			return julianEasterJdn(year);

		final MonthDay easter = gregorianEaster(year);
		return GregorianCalendarStrategy.toJdn(year, easter.getMonthValue(), easter.getDayOfMonth());
	}

}
