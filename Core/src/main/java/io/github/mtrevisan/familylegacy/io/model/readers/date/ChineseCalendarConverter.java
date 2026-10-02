package io.github.mtrevisan.familylegacy.io.model.readers.date;

import io.github.mtrevisan.familylegacy.services.AstronomicalEngine;
import io.github.mtrevisan.familylegacy.services.AstronomicalEngineFactory;


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
public final class ChineseCalendarConverter{

	//[day]
	private static final double MEAN_TROPICAL_YEAR_LENGTH = 365.2422;

	/** Mean synodic month length [days]. */
	private static final double MEAN_SYNODIC_MONTH = 29.530588853;
	// Safety step to push search query well past the current new moon towards the next lunation
	private static final double MIN_LUNATION_STEP_DAYS = MEAN_SYNODIC_MONTH / 2. + 1.;

	/**
	 * Apparent solar longitude that defines the winter solstice (Dongzhi, 冬至).
	 */
	private static final double WINTER_SOLSTICE_DEG = 270.;


	private ChineseCalendarConverter(){}


	/**
	 * Converts a Chinese calendar date to Julian Day Number (JDN).
	 *
	 * @param year  The Gregorian equivalent year of the Chinese New Year cycle.
	 * @param month The 1-based Chinese lunar month index (1..12).
	 * @param day   The 1-based day of the lunar month (1..30).
	 * @return The corresponding Julian Day Number (JDN).
	 */
	public static double toJdn(final int year, final int month, final int day){
		final AstronomicalEngine engine = AstronomicalEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Chinese calendar requires the optional Ephemeris astronomical plugin JAR in classpath.");

		final double utcOffset = getChinaUtcOffset(year);

		System.out.printf("%n=== DEBUG CONVERSION: Year=%d, Month=%d, Day=%d (UTC Offset=%.2f) ===%n", year, month, day, utcOffset);

		// 1. Calculate December Winter Solstice JDN of (year - 1)
		final int solsticeYear = (month >= 11? year: year - 1);
		final double winterSolsticeJdn = engine.getSolarLongitudeJdn(solsticeYear, WINTER_SOLSTICE_DEG, utcOffset);

		// 2. Locate Month 11 New Moon (on or before Winter Solstice)
		final double month11StartJdn = getNewMoonJdnOnOrBefore(engine, winterSolsticeJdn, utcOffset);

		System.out.printf("[Solstice %d] Solstice JDN=%.2f, Month 11 NewMoon JDN=%f%n", solsticeYear, winterSolsticeJdn, month11StartJdn);

		// 3. Calculate December Winter Solstice JDN of (year) to check total lunations in this Chinese astronomical year
		final double nextWinterSolsticeJdn = engine.getSolarLongitudeJdn(solsticeYear + 1, WINTER_SOLSTICE_DEG, utcOffset);
		final double nextMonth11StartJdn = getNewMoonJdnOnOrBefore(engine, nextWinterSolsticeJdn, utcOffset);

		// Count total lunations between consecutive Month 11s
		int totalLunations = 0;
		double lunationCheck = month11StartJdn;
		while(lunationCheck < nextMonth11StartJdn - 1.){
			totalLunations ++;
			final double nextNm = engine.getNextNewMoonJdn(lunationCheck + MIN_LUNATION_STEP_DAYS, utcOffset);
			lunationCheck = Math.max(nextNm, lunationCheck + 1.);
		}
		final boolean isLeapYear = (totalLunations >= 13);

		System.out.printf("[Leap Year Check] Total Lunations between Solstices: %d (IsLeapYear=%b)%n", totalLunations, isLeapYear);

		// 4. Traverse lunations from Month 11 of previous year to locate the target month
		double currentNewMoon = month11StartJdn;
		int currentMonthIndex = 11;
		boolean leapMonthPassed = false;
		double targetMonthStartJdn = -1.;
		for(int i = 0; i < 15; i ++){
			// If current lunation corresponds to target month, record start JDN
			if(currentMonthIndex == month && (month >= 11 || i >= 2)){
				targetMonthStartJdn = currentNewMoon;

				System.out.printf("  --> MATCH FOUND at Lunation JDN %f for Month %d%n", targetMonthStartJdn, month);

				break;
			}

			final double nextNewMoon = engine.getNextNewMoonJdn(currentNewMoon + MIN_LUNATION_STEP_DAYS,
				utcOffset);

			// Check if this lunation contains a Major Solar Term (Zhongqi)
			final boolean hasZhongqi = containsMajorSolarTerm(engine, currentNewMoon, nextNewMoon, utcOffset);

			System.out.printf("  Iter %2d: Lunation JDN [%f -> %f], MonthIndex=%2d, HasZhongqi=%b%n",
				i, currentNewMoon, nextNewMoon, currentMonthIndex, hasZhongqi);

			if(isLeapYear && !hasZhongqi && !leapMonthPassed && currentMonthIndex != 11 && currentMonthIndex != 12){
				// First month without a Major Solar Term is intercalary; month counter does NOT increment
				leapMonthPassed = true;

				System.out.printf("      [LEAP MONTH IDENTIFIED] Intercalary month after month %d%n", currentMonthIndex);
			}
			else
				currentMonthIndex = (currentMonthIndex % 12) + 1;

			currentNewMoon = Math.max(nextNewMoon, currentNewMoon + 1.);
		}

		// Return JDN adding 1-based day offset
//		return targetMonthStartJdn + day - 1;
		final double resultJdn = targetMonthStartJdn + day - 1;
		System.out.printf("FINAL RESULT JDN = %f%n", resultJdn);
		return resultJdn;
	}

	private static double getNewMoonJdnOnOrBefore(final AstronomicalEngine engine, final double jdn,
			final double utcOffset){
		final double targetLocalJdn = jdn + utcOffset / 24.;

		final double searchJdn = jdn - MEAN_SYNODIC_MONTH;
		double nextNewMoon = engine.getNextNewMoonJdn(searchJdn, utcOffset);

		double candidate = nextNewMoon;
		while(nextNewMoon <= targetLocalJdn){
			candidate = nextNewMoon;
			nextNewMoon = engine.getNextNewMoonJdn(candidate + MIN_LUNATION_STEP_DAYS, utcOffset);
		}

		return candidate;
	}

	private static boolean containsMajorSolarTerm(final AstronomicalEngine engine, final double startJdn,
			final double endJdn, final double utcOffset){
		// Major Solar Terms (Zhongqi) occur at multiples of 30 deg solar longitude
		final int approxYear = (int)Math.floor((startJdn - 1721425.5) / 365.2425);

		// Major Solar Terms (Zhongqi) occur at 30-degree solar longitude intervals
		for(int deg = 0; deg < 360; deg += 30)
			for(int y = approxYear - 1; y <= approxYear + 1; y ++){
				final double termJdnDouble = engine.getSolarLongitudeJdn(y, deg, utcOffset);
				final double termLocalJdn = Math.floor(termJdnDouble + utcOffset / 24.);
				if(termLocalJdn >= startJdn && termLocalJdn < endJdn)
					return true;
			}
		return false;
	}

	private static double getChinaUtcOffset(final int gregorianYear){
		if(gregorianYear >= 1929)
			// Standard UTC+8 (120° E) adopted in 1929
			return 8.;

		if(gregorianYear >= 1645)
			// Beijing Local Mean Time (116° 25' E = 7h 45m 40s) for Qing Dynasty Shixian calendar
			return 7.761111;

		// Before 1645: Nanjing / Historical Imperial Observatories (~UTC+7.9)
		return 7.9;
	}

}
