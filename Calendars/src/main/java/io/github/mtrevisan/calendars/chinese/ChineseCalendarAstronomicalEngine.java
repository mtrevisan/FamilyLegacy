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


/**
 * Converter between the Chinese lunisolar calendar and the Gregorian
 * calendar.
 *
 * <p>The Chinese calendar used here is the <b>Shixian</b> (時憲曆) calendar
 * adopted in 1645 and still in use today (see {@link LunisolarAstronomicalEngine}
 * for its astronomical rules), computed for the time of China.</p>
 *
 * <p><b>Range of validity.</b> The astronomical ephemerides used here
 * cover the interval from roughly 2000 BC to 3000 AD. For dates before
 * 1645 the historical Shixian rules did not apply, and for dates before
 * the Ming dynasty (1368–1644, the Datong calendar) the modern rule set
 * was not in use at all. The converter nonetheless produces a plausible
 * extrapolation for earlier dates; treat the result as an approximation
 * outside the Shixian period.</p>
 *
 * <p><b>References.</b> See Helmer Aslaksen, <i>The Mathematics of the
 * Chinese Calendar</i>, and the Chinese Academy of Sciences' Purple
 * Mountain Observatory publications on the Shixian calendar.</p>
 */
public final class ChineseCalendarAstronomicalEngine{

	// JD for January 29, 1645 (Astronomical Shixian Calendar Reform boundary)
	private static final double JD1645_SHIXIAN = 2321912.;
	// JD for January 1, 1929 (Official adoption of modern Standard Time UTC+8)
	private static final double JD1929_STANDARD_TIME = 2425612.;

	private static final LunisolarAstronomicalEngine CHINA = new LunisolarAstronomicalEngine(
		ChineseCalendarAstronomicalEngine::getChinaUtcOffset);


	private ChineseCalendarAstronomicalEngine(){}


	/**
	 * Converts a Chinese Calendar date into the Julian Date (Universal Time) of the local midnight of that day.
	 */
	public static double toJulianDateUT(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
		return CHINA.toJulianDateUT(chineseYear, chineseMonth, isLeapMonth, chineseDay);
	}

	/**
	 * Converts a Chinese Calendar date into the Julian Day Number of the civil day (independent of the UTC offset).
	 */
	public static long toJdn(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
		return CHINA.toJdn(chineseYear, chineseMonth, isLeapMonth, chineseDay);
	}

	/**
	 * Determines the historical China UTC time offset for a given Julian Date.
	 * <ul>
	 *   <li><b>1929 to present:</b> Standard UTC+8 (120° E) time zone.</li>
	 *   <li><b>1645 to 1928:</b> Beijing Local Mean Time (116° 25' E = +7.761111h)
	 *       used during the Qing Dynasty Shixian calendar reform.</li>
	 *   <li><b>Before 1645:</b> Nanjing / Historical Imperial Observatories standard
	 *       approximated at UTC+7.9.</li>
	 * </ul>
	 *
	 * @param jd the Julian Date (Universal Time) to evaluate
	 * @return the local time offset from UTC in decimal hours
	 */
	private static double getChinaUtcOffset(final double jd){
		if(jd >= JD1929_STANDARD_TIME)
			// Standard UTC+8 (120° E) adopted in 1929
			return 8.;

		if(jd >= JD1645_SHIXIAN)
			// Beijing Local Mean Time (116° 25' E = 7h 45m 40s) for Qing Dynasty Shixian calendar
			return 7.761111;

		// Before 1645: Nanjing / Historical Imperial Observatories (~UTC+7.9)
		return 7.9;
	}

}
