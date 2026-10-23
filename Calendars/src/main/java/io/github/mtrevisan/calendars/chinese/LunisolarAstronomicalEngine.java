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

import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleUnaryOperator;


/**
 * The astronomical rules of the East Asian lunisolar calendars derived from the Chinese Shixian calendar (true new
 * moons and true solar terms), computed for the meridian of a country:
 * <ol>
 *   <li>each month begins on the civil day of the new moon;</li>
 *   <li>the winter solstice always falls in the 11th month;</li>
 *   <li>when 13 months separate two 11th months, the first one without a principal term (a multiple of 30° of solar
 *   longitude) is the leap month, numbered as the month before it.</li>
 * </ol>
 * The Chinese, Korean, and Vietnamese calendars differ only for the local time used to decide the civil day of the
 * astronomical events.
 */
public final class LunisolarAstronomicalEngine{

	/** Mean synodic month length [days]. */
	private static final double MEAN_SYNODIC_MONTH = 29.530588853;
	/** Apparent solar longitude of the winter solstice. */
	private static final double WINTER_SOLSTICE_LONGITUDE = 3. * StrictMath.PI / 2.;
	private static final double PI_BY_6 = StrictMath.PI / 6.;
	/** Maximum number of New Moons needed to encompass a leap year plus the two months following the next Month 11. */
	private static final int MAX_NEW_MOONS = 17;

	/**
	 * Astronomical layout of a lunar year.
	 *
	 * @param newMoonsLocalMidnight the local midnights of the New Moons starting from the one before the Winter Solstice
	 *                              preceding the year
	 * @param month11Index          the index of the month containing the preceding Winter Solstice
	 * @param nextMonth11Index      the index of the month containing the following Winter Solstice
	 * @param leapMonthIndex        the index of the leap month, or {@code -1} if none
	 */
	private record LunarYear(double[] newMoonsLocalMidnight, int month11Index, int nextMonth11Index, int leapMonthIndex){}


	/** The local time offset from UT [hours] as a function of the Julian Date (UT). */
	private final DoubleUnaryOperator utcOffsetHours;
	/** Astronomical layout of each lunar year computed so far. */
	private final Map<Integer, LunarYear> lunarYears = new ConcurrentHashMap<>();


	/**
	 * @param utcOffsetHours the local time offset from UT [hours] of the calendar, as a function of the Julian Date (UT)
	 */
	public LunisolarAstronomicalEngine(final DoubleUnaryOperator utcOffsetHours){
		this.utcOffsetHours = utcOffsetHours;
	}


	/**
	 * Converts a lunisolar date into the Julian Date (Universal Time) of the local midnight of that day.
	 */
	public double toJulianDateUT(final int lunarYear, final int month, final boolean isLeapMonth, final int day){
		final double localMidnightJd = toLocalMidnightJd(lunarYear, month, isLeapMonth, day);
		return localMidnightJd - utcOffsetHours.applyAsDouble(localMidnightJd) / 24.;
	}

	/**
	 * Converts a lunisolar date into the Julian Day Number of the civil day.
	 *
	 * @param lunarYear   the lunar year (numbered as the Gregorian year in which it mostly falls)
	 * @param month       the month (1 to 12)
	 * @param isLeapMonth whether the month is the leap month following the month with the same number
	 * @param day         the day of the month (1 to 30)
	 * @return the Julian Day Number
	 * @throws IllegalArgumentException if the month does not exist in the year, or the day in the month
	 */
	public long toJdn(final int lunarYear, final int month, final boolean isLeapMonth, final int day){
		return (long)StrictMath.floor(toLocalMidnightJd(lunarYear, month, isLeapMonth, day) + 0.5);
	}

	/**
	 * @param lunarYear   the lunar year
	 * @param month       the month (1 to 12)
	 * @param isLeapMonth whether the month is the leap month
	 * @return the number of days of the month (29 or 30)
	 * @throws IllegalArgumentException if the month does not exist in the year
	 */
	public int monthLength(final int lunarYear, final int month, final boolean isLeapMonth){
		final long first = toJdn(lunarYear, month, isLeapMonth, 1);
		try{
			toJdn(lunarYear, month, isLeapMonth, 30);
			return 30;
		}
		catch(final IllegalArgumentException iae){
			return (int)(toJdn(lunarYear, month, isLeapMonth, 29) - first + 1);
		}
	}

	private double toLocalMidnightJd(final int lunarYear, final int month, final boolean isLeapMonth, final int day){
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("The lunisolar calendars require the optional Ephemeris astronomical plugin JAR in classpath.");

		final LunarYear layout = lunarYears.computeIfAbsent(lunarYear, year -> computeLunarYear(engine, year));
		final double[] newMoonsLocalMidnight = layout.newMoonsLocalMidnight();
		final int leapMonthIndex = layout.leapMonthIndex();

		// Months 11 and 12 of the year follow the winter solstice of the same Gregorian year (next cycle),
		// while months 1..10 belong to the cycle that starts at the preceding winter solstice.
		int i = layout.month11Index();
		int number = 11;
		final int end;
		if(month >= 11){
			i = layout.nextMonth11Index();
			end = layout.nextMonth11Index() + 2;
		}
		else{
			// skip month 11 and 12 (leap months do not advance the counter)
			while(number != 1){
				if(i != leapMonthIndex)
					number = (number == 12? 1: number + 1);
				i ++;
			}
			end = layout.nextMonth11Index();
		}

		int targetIndex = -1;
		for(; i < end; i ++){
			final boolean isCurrentLeap = (i == leapMonthIndex);
			// a leap month carries the number of the month that precedes it
			final int monthNumber = (isCurrentLeap? (number == 1? 12: number - 1): number);
			if(monthNumber == month && isCurrentLeap == isLeapMonth){
				targetIndex = i;
				break;
			}

			if(!isCurrentLeap)
				number = (number == 12? 1: number + 1);
		}
		if(targetIndex < 0)
			throw new IllegalArgumentException("The " + (isLeapMonth? "leap ": "") + "month " + month
				+ " does not exist in the lunar year " + lunarYear);

		final int monthLength = (int)StrictMath.round(newMoonsLocalMidnight[targetIndex + 1]
			- newMoonsLocalMidnight[targetIndex]);
		if(day < 1 || day > monthLength)
			throw new IllegalArgumentException("The " + (isLeapMonth? "leap ": "") + "month " + month + " of the lunar year "
				+ lunarYear + " has " + monthLength + " days");

		return newMoonsLocalMidnight[targetIndex] + (day - 1);
	}

	/**
	 * Computes the astronomical layout (new moons, months 11, leap month) of a lunar year; it depends only on the year,
	 * so it is cached.
	 */
	private LunarYear computeLunarYear(final EphemerisEngine engine, final int lunarYear){
		// 1. Estimate the Winter Solstice preceding the year (Meeus' polynomial approximation)
		final double y = (lunarYear - 1 - 2000.) / 1000.;
		final double approxWinterSolsticeJd = 2451900.05952
			+ (365242.74049 + (0.06223 + (-0.00823 - 0.00032 * y) * y) * y) * y;
		final double winterSolsticeUT = engine.findSolarLongitudeEvent(approxWinterSolsticeJd - 10.,
			WINTER_SOLSTICE_LONGITUDE);

		// 2. The local day of this Winter Solstice, always inside month 11 of the preceding year
		final double solsticeMidnightLocalJd = localMidnight(winterSolsticeUT);

		// 3. The next Winter Solstice
		final double yNext = y + 0.001;
		final double approxNextWinterSolsticeJd = 2451900.05952
			+ (365242.74049 + (0.06223 + (-0.00823 - 0.00032 * yNext) * yNext) * yNext) * yNext;
		final double nextWinterSolsticeUT = engine.findSolarLongitudeEvent(approxNextWinterSolsticeJd - 10.,
			WINTER_SOLSTICE_LONGITUDE);
		final double nextSolsticeMidnightLocalJd = localMidnight(nextWinterSolsticeUT);

		// 4. The sequential New Moons, from 35 days before the Winter Solstice, up to the month following the Month 11 of
		//    the next Winter Solstice (needed by months 11 and 12 of the target year)
		final double[] newMoonsLocalMidnight = new double[MAX_NEW_MOONS];
		int month11Index = -1;
		int nextMonth11Index = -1;
		double searchStartJd = winterSolsticeUT - 35.;
		for(int i = 0; i < MAX_NEW_MOONS && (nextMonth11Index < 0 || i < nextMonth11Index + 3); i ++){
			final double newMoonUT = engine.findNextNewMoon(searchStartJd);
			newMoonsLocalMidnight[i] = localMidnight(newMoonUT);
			searchStartJd = newMoonUT + MEAN_SYNODIC_MONTH - 5.;

			if(i == 0)
				continue;

			// 5. The months containing the two Winter Solstices (12 or 13 months apart)
			if(month11Index < 0){
				if(solsticeMidnightLocalJd >= newMoonsLocalMidnight[i - 1] && solsticeMidnightLocalJd < newMoonsLocalMidnight[i])
					month11Index = i - 1;
			}
			else if(nextMonth11Index < 0 && nextSolsticeMidnightLocalJd >= newMoonsLocalMidnight[i - 1]
					&& nextSolsticeMidnightLocalJd < newMoonsLocalMidnight[i])
				nextMonth11Index = i - 1;
		}

		// A leap year has 13 months between the two Month 11
		int leapMonthIndex = -1;
		if(nextMonth11Index - month11Index == 13){
			// the first month without a Major Solar Term (Zhongqi, a multiple of 30°)
			double nextLongitudeStart = Double.NaN;
			for(int i = month11Index + 1; i < nextMonth11Index; i ++){
				final double monthStartUT = newMoonsLocalMidnight[i] - utcOffsetHours.applyAsDouble(newMoonsLocalMidnight[i]) / 24.;
				final double nextMonthStartUT = newMoonsLocalMidnight[i + 1]
					- utcOffsetHours.applyAsDouble(newMoonsLocalMidnight[i + 1]) / 24.;

				final double longitudeStart = (Double.isNaN(nextLongitudeStart)
					? engine.calculateApparentSunLongitude(monthStartUT)
					: nextLongitudeStart);
				double longitudeEnd = engine.calculateApparentSunLongitude(nextMonthStartUT);
				nextLongitudeStart = longitudeEnd;
				if(longitudeEnd < longitudeStart)
					longitudeEnd += 2. * StrictMath.PI;
				if((int)StrictMath.floor(longitudeStart / PI_BY_6) == (int)StrictMath.floor(longitudeEnd / PI_BY_6)){
					leapMonthIndex = i;
					break;
				}
			}
		}

		return new LunarYear(newMoonsLocalMidnight, month11Index, nextMonth11Index, leapMonthIndex);
	}

	/**
	 * @return the Julian Date of the local midnight that starts the civil day of an instant
	 */
	private double localMidnight(final double jdUT){
		final double offset = utcOffsetHours.applyAsDouble(jdUT) / 24.;
		return StrictMath.floor(jdUT + offset + 0.5) - 0.5;
	}

}
