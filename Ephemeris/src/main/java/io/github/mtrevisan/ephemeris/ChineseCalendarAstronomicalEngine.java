package io.github.mtrevisan.ephemeris;

import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;


public final class ChineseCalendarAstronomicalEngine{

	// JD for January 29, 1645 (Astronomical Shixian Calendar Reform boundary)
	private static final double JD1645_SHIXIAN = 2321912.;

	// JD for January 1, 1929 (Official adoption of modern Standard Time UTC+8)
	private static final double JD1929_STANDARD_TIME = 2425612.;

	private final EphemerisEngine astronomy;


	private ChineseCalendarAstronomicalEngine(final EphemerisEngine astronomy){
		this.astronomy = astronomy;
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
	public double chineseDateToJulianDateUT(final int chineseYear, final int chineseMonth, final boolean isLeapMonth,
			final int chineseDay){
		// 1. Estimate the Julian Date of the Winter Solstice preceding the target Chinese Year.
		//    The Winter Solstice (Sun at 270° / 3pi/2 rad) always falls around December 21-22 of the previous Gregorian year.
		//    We convert the year to an approximate Julian Date base.
		final double approxWinterSolsticeGregorianJd = 1721425.5 + (chineseYear - 1) * 365.2425 + 355.;

		final double targetWinterSolsticeUT = findSolarTerm(approxWinterSolsticeGregorianJd,
			3. * StrictMath.PI / 2.0);
//		final double targetWinterSolsticeUT = astronomy.findSolarLongitudeEvent(approxWinterSolsticeGregorianJd, 3.0 * StrictMath.PI / 2.0);

		// 2. Find the Midnight boundary in China Local Time for this Winter Solstice.
		//    This Winter Solstice always falls inside Lunar Month 11 of the preceding year.
		final double solsticeOffset = getChinaUtcOffset(targetWinterSolsticeUT) / 24.0;
		final double solsticeMidnightLocalJd = StrictMath.floor(targetWinterSolsticeUT + solsticeOffset + 0.5) - 0.5;
//		final double solsticeOffset = astronomy.getLocalTimeOffset(targetWinterSolsticeUT) / 24.0;
//		final double solsticeMidnightLocalJd = StrictMath.floor(targetWinterSolsticeUT + solsticeOffset + 0.5) - 0.5;

		// 3. Scan and locate the sequential New Moons (up to 15 months to fully encompass any potential leap year)
		//    We start looking from 29 days before the Winter Solstice.
		final double[] newMoonsLocalMidnight = new double[15];
		double searchStartJd = targetWinterSolsticeUT - 29.;
		for(int i = 0; i < 15; i ++){
			final double nmUT = findNewMoon(searchStartJd);
			final double offset = getChinaUtcOffset(nmUT) / 24.;
			newMoonsLocalMidnight[i] = StrictMath.floor(nmUT + offset + 0.5) - 0.5;
			// Step to the next lunar cycle window
			searchStartJd = nmUT + 29.5;
		}
//		double searchStartJd = targetWinterSolsticeUT - 29.0;
//		double[] newMoonsLocalMidnight = new double[15];
//		for (int i = 0; i < 15; i++) {
//			double nmUT = astronomy.findNextNewMoon(searchStartJd);
//			double localOffset = astronomy.getLocalTimeOffset(nmUT) / 24.0;
//			newMoonsLocalMidnight[i] = StrictMath.floor(nmUT + localOffset + 0.5) - 0.5;
//			searchStartJd = nmUT + 29.5;
//		}

		// 4. Identify which month index corresponds to Month 11 (the one containing or matching the Winter Solstice)
		int month11Index = 0;
		for(int i = 0; i < 14; i ++)
			if(solsticeMidnightLocalJd >= newMoonsLocalMidnight[i]
					&& solsticeMidnightLocalJd < newMoonsLocalMidnight[i + 1]){
				month11Index = i;

				break;
			}

		// 5. Determine if the current leap-year cycle interval (between Month 11 of year-1 and Month 11 of year) contains 13 months
		//    If it contains 13 months, one of them completely lacks a Major Solar Term (Zhongqi) and is marked as Leap.
		final int nextMonth11Index = month11Index + 12; // Standard offset check boundary
		final boolean isLeapYear = (newMoonsLocalMidnight[nextMonth11Index] <= findSolarTerm(newMoonsLocalMidnight[nextMonth11Index],
			3. * StrictMath.PI / 2.));

		int leapMonthIndex = -1;
		if(isLeapYear){
			// Find the first month in the sequence that does not contain a Major Solar Term (Zhongqi)
			// Major Solar Terms occur at multiples of 30° (pi/6 radians), starting from 270° (Winter Solstice)
			for(int i = month11Index + 1; i < nextMonth11Index; i ++){
				final double currentMonthStartLocalJd = newMoonsLocalMidnight[i];
				final double nextMonthStartLocalJd = newMoonsLocalMidnight[i + 1];

				// Convert local midnights back to UT to feed the solar term scanner safely
				final double currentMonthStartUT = currentMonthStartLocalJd - getChinaUtcOffset(currentMonthStartLocalJd) / 24.;
				final double nextMonthStartUT = nextMonthStartLocalJd - getChinaUtcOffset(nextMonthStartLocalJd) / 24.;

				boolean hasZhongqi = false;
				for(int m = 0; m < 12; m ++){
					// Major Solar Term angles: 270, 300, 330, 0, 30, 60, 90, 120, 150, 180, 210, 240 degrees
					final double targetZhongqiAngle = MathHelper.mod2pi((3. * StrictMath.PI / 2.)
						+ m * (StrictMath.PI / 6.));
					final double zhongqiUT = findSolarTerm(currentMonthStartUT, targetZhongqiAngle);

					if(zhongqiUT >= currentMonthStartUT && zhongqiUT < nextMonthStartUT){
						hasZhongqi = true;

						break;
					}
				}

				if(!hasZhongqi){
					// This month has no Major Solar Term, it is the Leap Month!
					leapMonthIndex = i;

					break;
				}
			}
		}

		// 6. Match the requested Month and Leap flag against our resolved astronomical calendar string
		double targetMonthStartLocalMidnightJd = -1.;
		// Remember, our tracking array begins at Month 11 of the previous year
		int currentMonthNumber = 11;
		for(int i = month11Index; i < newMoonsLocalMidnight.length; i ++){
			boolean isCurrentLeap = (i == leapMonthIndex);
			if(currentMonthNumber == chineseMonth && isCurrentLeap == isLeapMonth){
				targetMonthStartLocalMidnightJd = newMoonsLocalMidnight[i];

				break;
			}

			// Increment the month number only if the current scanned item was NOT a leap month
			if(!isCurrentLeap){
				currentMonthNumber ++;
				if(currentMonthNumber > 12)
					currentMonthNumber = 1;
			}
		}

		if(targetMonthStartLocalMidnightJd == -1.)
			throw new IllegalArgumentException("The requested Chinese Calendar date does not exist in this dynamic astronomical cycle.");

		// 7. Advance to the specific target day and convert the final local midnight boundary back to Universal Time (UT)
		final double finalLocalMidnightJd = targetMonthStartLocalMidnightJd + (chineseDay - 1);
		final double finalUTOffset = getChinaUtcOffset(finalLocalMidnightJd) / 24.;

		return finalLocalMidnightJd - finalUTOffset;
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


	/**
	 * Computes the exact Julian Date (Universal Time) of the New Moon closest to a target date.
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT (e.g., mid-month)
	 * @return the exact Julian Date UT of the conjunction
	 */
	public static double findNewMoon(final double approximateJdUT){
		// Bracket the search window within +/- 3 days around the estimated guess
		final double lowerBound = approximateJdUT - 3.;
		final double upperBound = approximateJdUT + 3.;
		// ~8 milliseconds accuracy
		final double accuracyThreshold = 1.e-7;

		return IllinoisSolver.solve(lowerBound, upperBound, accuracyThreshold, 50, jdUT -> {
			final double dt = DeltaT.deltaTSecondsFromJd(jdUT);
			final double dtDays = DeltaT.deltaTDays(dt);
			final double ttJc = JulianDate.centuryJ2000Of(jdUT + dtDays);

			final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
			final double deltaPsi = nutation.getDeltaPsi();

			final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);
			final double moonLng = MoonPosition.computeApparentMoonLongitude(ttJc, deltaPsi);

			// Angular delta normalized to the range (-π, π] to avoid discontinuity slips
			return MathHelper.mod2pi(moonLng - sunLng + StrictMath.PI) - StrictMath.PI;
		});
	}

	/**
	 * Computes the exact Julian Date (Universal Time) of a specific Solar Term (Jieqi).
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @param targetTermAngle the targeted solar longitude step [rad] (e.g., 0 for Spring Equinox, π/6 for next term)
	 * @return the exact Julian Date UT when the Sun reaches the target angle
	 */
	public static double findSolarTerm(final double approximateJdUT, final double targetTermAngle){
		final double lowerBound = approximateJdUT - 5.;
		final double upperBound = approximateJdUT + 5.;
		// ~8 milliseconds accuracy
		final double accuracyThreshold = 1.e-7;

		return IllinoisSolver.solve(lowerBound, upperBound, accuracyThreshold, 50, jdUT -> {
			final double dt = DeltaT.deltaTSecondsFromJd(jdUT);
			final double dtDays = DeltaT.deltaTDays(dt);
			final double ttJc = JulianDate.centuryJ2000Of(jdUT + dtDays);

			final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
			final double deltaPsi = nutation.getDeltaPsi();

			final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);

			return MathHelper.mod2pi(sunLng - targetTermAngle + StrictMath.PI) - StrictMath.PI;
		});
	}

}
