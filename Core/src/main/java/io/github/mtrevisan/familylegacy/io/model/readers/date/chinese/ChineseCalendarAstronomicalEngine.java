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

import io.github.mtrevisan.familylegacy.services.EphemerisEngine;
import io.github.mtrevisan.familylegacy.services.EphemerisEngineFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Converter between the Chinese lunisolar calendar and the Gregorian
 * calendar.
 *
 * <p>The Chinese calendar used here is the <b>Shixian</b> (時憲曆) calendar
 * adopted in 1645 and still in use today. It is defined by the following
 * astronomical rules:</p>
 *
 * <ol>
 *   <li>Each lunar month begins on the day of the astronomical new moon
 *       (conjunction in apparent ecliptic longitude) as observed from the
 *       meridian of Beijing (UTC+8).</li>
 *   <li>The winter solstice (apparent solar longitude 270°) always falls
 *       in month 11 of the Chinese year.</li>
 *   <li>Chinese New Year (month 1) is the second new moon after the
 *       winter solstice, unless a leap month is inserted before it, in
 *       which case it is the third.</li>
 *   <li>If there are 13 lunar months between successive month-11's, the
 *       first month that does not contain a <em>zhongqi</em> (major solar
 *       term, a multiple of 30° of apparent solar longitude) is
 *       designated as a leap month, taking the same number as the month
 *       before it (e.g. 閏四月 "leap-4" is the extra month after
 *       month 4).</li>
 * </ol>
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

	/** Mean synodic month length [days]. */
	private static final double MEAN_SYNODIC_MONTH = 29.530588853;

	/**
	 * Apparent solar longitude that defines the winter solstice (Dongzhi, 冬至).
	 */
	private static final double WINTER_SOLSTICE_LONGITUDE = 3. * StrictMath.PI / 2.;

	private static final double PI_BY_6 = StrictMath.PI / 6.;

	/** Maximum number of New Moons needed to encompass a leap year plus the two months following the next Month 11. */
	private static final int MAX_NEW_MOONS = 17;

	/** Astronomical layout of each Chinese year computed so far, keyed by Chinese year. */
	private static final Map<Integer, LunarYear> LUNAR_YEARS = new ConcurrentHashMap<>();

	/**
	 * Astronomical layout of a Chinese year.
	 *
	 * @param newMoonsLocalMidnight the local midnights of the New Moons starting from the one before the Winter Solstice
	 *                              preceding the year
	 * @param month11Index          the index of the month containing the preceding Winter Solstice
	 * @param nextMonth11Index      the index of the month containing the following Winter Solstice
	 * @param leapMonthIndex        the index of the leap month, or {@code -1} if none
	 */
	private record LunarYear(double[] newMoonsLocalMidnight, int month11Index, int nextMonth11Index, int leapMonthIndex){}


	private ChineseCalendarAstronomicalEngine(){}


	/**
	 * Converts a Chinese Calendar date into the Julian Date (Universal Time) of the local midnight of that day.
	 */
	public static double toJulianDateUT(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
		final double localMidnightJd = toLocalMidnightJd(chineseYear, chineseMonth, isLeapMonth, chineseDay);

		return localMidnightJd - getChinaUtcOffset(localMidnightJd) / 24.;
	}

	/**
	 * Converts a Chinese Calendar date into the Julian Day Number of the civil day (independent of the UTC offset).
	 */
	public static long toJdn(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
		final double localMidnightJd = toLocalMidnightJd(chineseYear, chineseMonth, isLeapMonth, chineseDay);

//System.out.println("localMidnightJd=" + localMidnightJd + ", returning=" + (long)StrictMath.floor(localMidnightJd + 0.5));

		// convert jd to jdn
		return (long)StrictMath.floor(localMidnightJd + 0.5);
	}

	/**
	 * Converts a Chinese Calendar date into the corresponding Julian Date (Universal Time) at Local Midnight.
	 *
	 * @param chineseYear  the Chinese cycle year or historical year (e.g., 1980)
	 * @param chineseMonth the Chinese month number (1 to 12)
	 * @param isLeapMonth  true if the requested month is an intercalary leap month (Runyue), false otherwise
	 * @param chineseDay   the day of the Chinese month (1 to 30)
	 * @return the Julian Date (Universal Time) corresponding to the local midnight of the requested day
	 */
	private static double toLocalMidnightJd(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
//System.out.println("year=" + chineseYear + ", month=" + chineseMonth + ", leap=" + isLeapMonth + ", day=" + chineseDay);
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Chinese calendar requires the optional Ephemeris astronomical plugin JAR in classpath.");

		final LunarYear lunarYear = LUNAR_YEARS.computeIfAbsent(chineseYear, year -> computeLunarYear(engine, year));
		final double[] newMoonsLocalMidnight = lunarYear.newMoonsLocalMidnight();
		final int month11Index = lunarYear.month11Index();
		final int nextMonth11Index = lunarYear.nextMonth11Index();
		final int leapMonthIndex = lunarYear.leapMonthIndex();

		// 6. Match the requested month and leap flag.
		// Months 11 and 12 of Chinese year Y follow the winter solstice of year Y (next cycle),
		// while months 1..10 belong to the cycle that starts at the preceding winter solstice.
		int i = month11Index;
		int number = 11;
		final int end;
		if(chineseMonth >= 11){
			i = nextMonth11Index;
			end = nextMonth11Index + 2;
		}
		else{
			// skip month 11 and 12 (leap months do not advance the counter)
			while(number != 1){
//System.out.println("renumber: i=" + i + ", number=" + number + ", leap=" + (i == leapMonthIndex));
				if(i != leapMonthIndex)
					number = (number == 12? 1: number + 1);
				i ++;
			}
			end = nextMonth11Index;
		}

		double targetMonthStartLocalMidnightJd = -1.;
		for(; i < end; i ++){
			final boolean isCurrentLeap = (i == leapMonthIndex);
			// a leap month carries the number of the month that precedes it
			final int monthNumber = (isCurrentLeap? (number == 1? 12: number - 1): number);
//System.out.println("targetMonthStartLocalMidnightJd=" + targetMonthStartLocalMidnightJd);
//System.out.println("monthNumber=" + monthNumber);
//System.out.println("isCurrentLeap=" + isCurrentLeap);
			if(monthNumber == chineseMonth && isCurrentLeap == isLeapMonth){
				targetMonthStartLocalMidnightJd = newMoonsLocalMidnight[i];

//System.out.println("FOUND monthStart=" + targetMonthStartLocalMidnightJd);
				break;
			}

			if(!isCurrentLeap)
				number = (number == 12? 1: number + 1);
		}
		if(targetMonthStartLocalMidnightJd == -1.)
			throw new IllegalArgumentException("The requested Chinese Calendar date does not exist in this dynamic astronomical cycle.");

		// 7. Advance to the specific target day (local midnight, JD ending in .5)
//System.out.println("rawResult=" + (targetMonthStartLocalMidnightJd + (chineseDay - 1)));
		return targetMonthStartLocalMidnightJd + (chineseDay - 1);
	}

	/**
	 * Computes the astronomical layout (new moons, months 11, leap month) of a Chinese year.
	 * <p>
	 * This is by far the most expensive part of a conversion, and depends only on the year, so it is cached.
	 * </p>
	 */
	private static LunarYear computeLunarYear(final EphemerisEngine engine, final int chineseYear){
		// 1. Estimate the Julian Date of the Winter Solstice preceding the target Chinese Year precisely using Meeus' polynomial approximation.
		final double y = (chineseYear - 1 - 2000.) / 1000.;
		final double approxWinterSolsticeGregorianJd = 2451900.05952
			+ (365242.74049 + (0.06223 + (-0.00823 - 0.00032 * y) * y) * y) * y;
//System.out.println("approxWinterSolsticeGregorianJd: " + approxWinterSolsticeGregorianJd);

		// Anticipate the base date further back (subtracting 10 days) ensures the forward scanner
		//	always brackets the winter solstice correctly without missing it.
		final double targetWinterSolsticeUT = engine.findSolarLongitudeEvent(
			approxWinterSolsticeGregorianJd - 10., WINTER_SOLSTICE_LONGITUDE);
//System.out.println("targetWinterSolsticeUT: " + targetWinterSolsticeUT);

		// 2. Find the Midnight boundary in China Local Time for this Winter Solstice.
		//    This Winter Solstice always falls inside Lunar Month 11 of the preceding year.
		final double solsticeOffset = getChinaUtcOffset(targetWinterSolsticeUT) / 24.;
		final double solsticeMidnightLocalJd = StrictMath.floor(targetWinterSolsticeUT + solsticeOffset + 0.5) - 0.5;
//System.out.println("solsticeMidnightLocalJd: " + solsticeMidnightLocalJd);

		// 3. Determine next Winter Solstice
		// Approximate next year (+1 year in millennia scale)
		final double yNext = y + 0.001;
		final double approxNextWinterSolsticeJd = 2451900.05952
			+ (365242.74049 + (0.06223 + (-0.00823 - 0.00032 * yNext) * yNext) * yNext) * yNext;
		final double nextWinterSolsticeUT = engine.findSolarLongitudeEvent(
			approxNextWinterSolsticeJd - 10., WINTER_SOLSTICE_LONGITUDE);
//System.out.println("nextWinterSolsticeUT: " + nextWinterSolsticeUT);

		final double nextSolsticeOffset = getChinaUtcOffset(nextWinterSolsticeUT) / 24.;
		final double nextSolsticeMidnightLocalJd = StrictMath.floor(nextWinterSolsticeUT + nextSolsticeOffset + 0.5)
			- 0.5;

		// 4. Scan and locate the sequential New Moons (up to 17 to fully encompass any potential leap year)
		//    We start looking from 35 days before the Winter Solstice, and stop at the month following the Month 11 of
		//    the next Winter Solstice (needed by months 11 and 12 of the target year), since every New Moon is expensive.
		final double[] newMoonsLocalMidnight = new double[MAX_NEW_MOONS];
		int month11Index = -1;
		int nextMonth11Index = -1;
		double searchStartJd = targetWinterSolsticeUT - 35.;
		for(int i = 0; i < MAX_NEW_MOONS && (nextMonth11Index < 0 || i < nextMonth11Index + 2); i ++){
			final double newMoonUT = engine.findNextNewMoon(searchStartJd);
			final double offset = getChinaUtcOffset(newMoonUT) / 24.;
//System.out.println("newMoonUT=" + newMoonUT);
//System.out.println("offset=" + offset);
//System.out.println("localJD=" + (newMoonUT + offset));
			newMoonsLocalMidnight[i] = StrictMath.floor(newMoonUT + offset + 0.5) - 0.5;
			// Step to the next lunar cycle window
			searchStartJd = newMoonUT + MEAN_SYNODIC_MONTH - 5.;

			if(i == 0)
				continue;

			// 5. Identify which month index corresponds to Month 11, and the one of the next winter solstice (can be 12
			//    or 13 months later)
			if(month11Index < 0){
				if(solsticeMidnightLocalJd >= newMoonsLocalMidnight[i - 1]
						&& solsticeMidnightLocalJd < newMoonsLocalMidnight[i])
					month11Index = i - 1;
			}
			else if(nextSolsticeMidnightLocalJd >= newMoonsLocalMidnight[i - 1]
					&& nextSolsticeMidnightLocalJd < newMoonsLocalMidnight[i])
				nextMonth11Index = i - 1;
		}
//System.out.println("newMoonsLocalMidnight: " + Arrays.toString(newMoonsLocalMidnight));
//System.out.println("month11Index: " + month11Index);

		// A leap year occurs if there are 13 months between the two Month 11 instances
		final boolean isLeapYear = (nextMonth11Index - month11Index == 13);
//System.out.println("nextSolsticeMidnightLocalJd: " + nextSolsticeMidnightLocalJd);
//System.out.println("isLeapYear: " + isLeapYear);

		int leapMonthIndex = -1;
		if(isLeapYear){
			// Find the first month in the sequence that does not contain a Major Solar Term (Zhongqi)
			// Major Solar Terms occur at multiples of 30° (pi/6 radians), starting from 270° (Winter Solstice)
			double nextLonStart = Double.NaN;
			for(int i = month11Index + 1; i < nextMonth11Index; i ++){
//System.out.println("-----");
//System.out.println("monthIndex=" + i);
//System.out.println("monthStart=" + newMoonsLocalMidnight[i]);
//System.out.println("nextMonthStart=" + newMoonsLocalMidnight[i + 1]);
				final double currentMonthStartLocalJd = newMoonsLocalMidnight[i];
				final double nextMonthStartLocalJd = newMoonsLocalMidnight[i + 1];

				// Convert local midnights back to UT to feed the solar term scanner safely
				final double currentMonthStartUT = currentMonthStartLocalJd
					- getChinaUtcOffset(currentMonthStartLocalJd) / 24.;
				final double nextMonthStartUT = nextMonthStartLocalJd
					- getChinaUtcOffset(nextMonthStartLocalJd) / 24.;

				// Fetch apparent longitudes safely via the interface proxy layer (the end of a month is the start of the
				// next one, so its longitude is reused)
				final double lonStart = (Double.isNaN(nextLonStart)
					? engine.calculateApparentSunLongitude(currentMonthStartUT)
					: nextLonStart);
				double lonEnd = engine.calculateApparentSunLongitude(nextMonthStartUT);
				nextLonStart = lonEnd;
				if(lonEnd < lonStart)
					lonEnd += 2. * StrictMath.PI;
				final int codeStart = (int)StrictMath.floor(lonStart / PI_BY_6);
				final int codeEnd = (int)StrictMath.floor(lonEnd / PI_BY_6);
				if(codeStart == codeEnd){
					leapMonthIndex = i;

					break;
				}
			}
		}
//System.out.println("leapMonthIndex: " + leapMonthIndex);

		return new LunarYear(newMoonsLocalMidnight, month11Index, nextMonth11Index, leapMonthIndex);
	}

	/**
	 * Determines the historical China UTC time offset for a given Julian Date.
	 * <p>
	 * This method automatically extracts the approximate calendar year from the
	 * provided Julian Date to apply the correct historical timekeeping framework:
	 * <ul>
	 *   <li><b>1929 to present:</b> Standard UTC+8 (120° E) time zone.</li>
	 *   <li><b>1645 to 1928:</b> Beijing Local Mean Time (116° 25' E = +7.761111h)
	 *       used during the Qing Dynasty Shixian calendar reform.</li>
	 *   <li><b>Before 1645:</b> Nanjing / Historical Imperial Observatories standard
	 *       approximated at UTC+7.9.</li>
	 * </ul>
	 * </p>
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
