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
package io.github.mtrevisan.calendars.islamic;

import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Converter between the Islamic calendar and the Gregorian calendar using the Umm al-Qura criteria.
 * <p>
 * <b>Official table:</b> for 1300-1600 AH (1882-2174 CE) the month starts are those of the official Umm al-Qura table
 * (KACST, as published by umalqura.org.sa and distributed in the JDK Hijrah-umalqura data), embedded in the resource
 * {@code umm-al-qura.csv} and loaded by {@link UmmAlQuraTable}; they agree with it in all the 3612 months. Outside that
 * range (and therefore for the months before 1300 AH and after 1600 AH) the months are computed with the astronomical
 * criteria described below, which are an approximation of the official rules: before 1420 AH the official tables
 * were built with criteria that are not fully documented (the computed months agree with the table in about 84% of the
 * months), and from 1420 AH on the criteria reproduce the table but for rare months that differ by a day, so the dates
 * outside the table can be one day off.
 * </p>
 * <p>
 * The Umm al-Qura calendar (used in Saudi Arabia) determines the start of a lunar month based on 
 * astronomical calculations in Mecca (21.4225° N, 39.8262° E). A new month begins in the
 * evening when:
 * 1. The geocentric conjunction (new moon) occurs before sunset in Mecca.
 * 2. The moon sets after the sun in Mecca.
 * </p>
 * <p>
 * <b>Historical Timezone:</b> Mecca used Local Mean Time (LMT) based on its longitude until 1940, 
 * when UTC+3 was officially adopted. The engine accounts for this shift to ensure accuracy for 
 * historical dates.
 * </p>
 */
public final class IslamicCalendarAstronomicalEngine{

	// JDN of 1 Muharram 1 AH (16 July 622 CE Julian)
	private static final long ISLAMIC_EPOCH_JDN = 1948440L;

	private static final double MECCA_LATITUDE = 21.4225;
	private static final double MECCA_LONGITUDE = 39.8262;
	private static final double RIYADH_LATITUDE = 24.7136;
	private static final double RIYADH_LONGITUDE = 46.6753;

	// 1 January 1940 (Adoption of UTC+3), midnight
	private static final double SAUDI_ARABIA_STANDARD_TIME_START_JD = GregorianCalendarStrategy.toJdn(1940, 1, 1) - 0.5;
	// 1 Muharram 1420 AH (17 April 1999): from this month the moon must set after the sun at Mecca on the 29th
	private static final long UMM_AL_QURA_PHASE2_START_JDN = GregorianCalendarStrategy.toJdn(1999, 4, 17);
	// 1 Muharram 1423 AH (15 March 2002): from this month the conjunction must also precede the sunset at Mecca
	private static final long UMM_AL_QURA_PHASE3_START_JDN = GregorianCalendarStrategy.toJdn(2002, 3, 15);

	/** Mean synodic month length [days]. */
	private static final double MEAN_SYNODIC_MONTH = 29.530588853;
	/**
	 * Days before the mean start of a month from which the conjunction is searched: the mean lunation drifts from the
	 * actual one by a couple of days, and the next conjunction after this instant is the one of the month.
	 */
	private static final double SEARCH_BACK_DAYS = 16.;
	/** The age of the Moon at sunset in Riyadh needed by the criterion before 1420 AH [hours]. */
	private static final double MINIMUM_MOON_AGE_HOURS = 12.;

	// Julian Date of 1970-01-01T00:00Z
	private static final double UNIX_EPOCH_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 86_400_000.;

	/** The first day of each month computed so far, by month index since the epoch. */
	private static final Map<Long, Long> MONTH_STARTS = new ConcurrentHashMap<>();


	private IslamicCalendarAstronomicalEngine(){}


	/**
	 * Converts an Islamic date to its Julian Day Number using the Umm al-Qura criteria.
	 *
	 * @param year  the Islamic year
	 * @param month the Islamic month (1 to 12)
	 * @param day   the day of the month (1 to 30)
	 * @return the Julian Day Number
	 * @throws IllegalArgumentException if the day does not exist in the month (the day 30 of a month of 29 days)
	 */
	public static long toJdn(final int year, final int month, final int day){
		if(month < 1 || month > 12)
			throw new IllegalArgumentException("The Islamic month must be between 1 and 12: " + month);
		if(day < 1)
			throw new IllegalArgumentException("The day of the Islamic month must be at least 1: " + day);

		final long startJdn = monthStartJdn(year, month);
		// the lunar months are 29 or 30 days long, known only from the start of the next one
		if(day > 29){
			final int length = monthLength(year, month);
			if(day > length)
				throw new IllegalArgumentException("The month " + month + " of the Islamic year " + year + " has " + length + " days");
		}

		return startJdn + (day - 1);
	}

	/**
	 * @param year  the Islamic year
	 * @param month the Islamic month (1 to 12)
	 * @return the number of days of the month (29 or 30), from the start of the next month
	 */
	public static int monthLength(final int year, final int month){
		if(UmmAlQuraTable.covers(year))
			return UmmAlQuraTable.monthLength(year, month);

		final long nextStart = (month == 12? monthStartJdn(year + 1, 1): monthStartJdn(year, month + 1));
		return (int)(nextStart - monthStartJdn(year, month));
	}

	/**
	 * @param year  the Islamic year
	 * @param month the Islamic month (1 to 12)
	 * @return the Julian Day Number of the first day of the month
	 */
	public static long monthStartJdn(final int year, final int month){
		if(UmmAlQuraTable.covers(year))
			return UmmAlQuraTable.monthStartJdn(year, month);

		final long monthIndex = (year - 1L) * 12L + (month - 1L);
		final Long cached = MONTH_STARTS.get(monthIndex);
		if(cached != null)
			return cached;

		final long start = computeMonthStartJdn(monthIndex);
		MONTH_STARTS.put(monthIndex, start);
		return start;
	}

	private static long computeMonthStartJdn(final long monthIndex){
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Islamic astronomical calendar requires the Ephemeris plugin.");

		// the conjunction (new moon) of the month: the first one after a few days before the mean start of the month
		final double approximateStartJd = ISLAMIC_EPOCH_JDN + monthIndex * MEAN_SYNODIC_MONTH;
		final double newMoonUtJd = engine.findNextNewMoon(approximateStartJd - SEARCH_BACK_DAYS);

		// Determine the historical timezone offset for Mecca at this specific date, and the local civil day of the conjunction
		final double meccaUtcOffset = getMeccaUtcOffset(newMoonUtJd) / 24.;
		final double newMoonLocalMeccaJd = newMoonUtJd + meccaUtcOffset;
		final long localConjunctionCivilJdn = (long)Math.floor(newMoonLocalMeccaJd + 0.5);
		final double testMidnightUtJd = localConjunctionCivilJdn - 0.5 - meccaUtcOffset;

		//the criteria are chosen by the start of the month (not by the conjunction), from the most recent: a month
		//that starts before the change of a criterion follows the previous one
		final long startPhase3 = startUmmAlQura2002(engine, newMoonUtJd, localConjunctionCivilJdn, testMidnightUtJd);
		if(startPhase3 >= UMM_AL_QURA_PHASE3_START_JDN)
			return startPhase3;

		final long startPhase2 = startUmmAlQura1999(engine, localConjunctionCivilJdn, testMidnightUtJd);
		if(startPhase2 >= UMM_AL_QURA_PHASE2_START_JDN)
			return startPhase2;

		return startBefore1999(engine, newMoonUtJd, localConjunctionCivilJdn, testMidnightUtJd);
	}

	/**
	 * From 1423 AH: the month starts the day after the evening of the 29th on which the conjunction precedes the sunset
	 * and the moon sets after the sun at Mecca; otherwise two days after the conjunction day.
	 */
	private static long startUmmAlQura2002(final EphemerisEngine engine, final double newMoonUtJd,
			final long localConjunctionCivilJdn, final double testMidnightUtJd){
		final double sunsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
			MECCA_LATITUDE, MECCA_LONGITUDE);
		final double moonsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.MOON, testMidnightUtJd,
			MECCA_LATITUDE, MECCA_LONGITUDE);

		final boolean conjunctionBeforeSunset = (newMoonUtJd < sunsetUtJd);
		final boolean moonsetAfterSunset = (moonsetUtJd > sunsetUtJd);
		return (conjunctionBeforeSunset && moonsetAfterSunset? localConjunctionCivilJdn + 1: localConjunctionCivilJdn + 2);
	}

	/**
	 * From 1420 to 1422 AH: the month starts the day after the evening on which the moon sets after the sun at Mecca;
	 * otherwise two days after the conjunction day.
	 */
	private static long startUmmAlQura1999(final EphemerisEngine engine, final long localConjunctionCivilJdn,
			final double testMidnightUtJd){
		final double sunsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
			MECCA_LATITUDE, MECCA_LONGITUDE);
		final double moonsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.MOON, testMidnightUtJd,
			MECCA_LATITUDE, MECCA_LONGITUDE);

		return (moonsetUtJd > sunsetUtJd? localConjunctionCivilJdn + 1: localConjunctionCivilJdn + 2);
	}

	/**
	 * Before 1420 AH: the month starts the day after the evening on which the Moon is at least 12 hours old at the
	 * sunset in Riyadh; otherwise two days after the conjunction day (the same convention of the other criteria: the
	 * first day is the one after the evening on which the crescent is seen).
	 */
	private static long startBefore1999(final EphemerisEngine engine, final double newMoonUtJd,
			final long localConjunctionCivilJdn, final double testMidnightUtJd){
		final double sunsetRiyadhUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
			RIYADH_LATITUDE, RIYADH_LONGITUDE);

		final double moonAgeHours = (sunsetRiyadhUtJd - newMoonUtJd) * 24.;
		return (moonAgeHours >= MINIMUM_MOON_AGE_HOURS? localConjunctionCivilJdn + 1: localConjunctionCivilJdn + 2);
	}

	/**
	 * Determines the historical Mecca UTC time offset for a given Julian Date.
	 * <ul>
	 *   <li><b>Before 1940:</b> Local Mean Time (LMT) based on Mecca's longitude (approx UTC+2:39).</li>
	 *   <li><b>1940 to present:</b> Arabia Standard Time (UTC+3).</li>
	 * </ul>
	 *
	 * @param jd the Julian Date (Universal Time) to evaluate
	 * @return the local time offset from UTC in decimal hours
	 */
	private static double getMeccaUtcOffset(final double jd){
		if(jd >= SAUDI_ARABIA_STANDARD_TIME_START_JD)
			// Standard Arabia Standard Time (UTC+3)
			return 3.;

		// Historical Local Mean Time (LMT) based on the precise longitude of Mecca
		return MECCA_LONGITUDE / 15.;
	}

	/**
	 * Computes the setting (upper limb on the apparent horizon) of a celestial body.
	 *
	 * @param engine       the ephemeris engine
	 * @param body         the celestial body
	 * @param midnightUtJd the Julian Date of the local civil midnight, in UT
	 * @param latitude     the observer's geodetic latitude in degrees (North positive)
	 * @param longitude    the observer's geodetic longitude in degrees (East positive)
	 * @return the Julian Date (UT) of the setting, or {@link Double#NaN} if the body does not set during that day
	 */
	private static double calculateSetting(final EphemerisEngine engine, final EphemerisEngine.CelestialBody body,
			final double midnightUtJd, final double latitude, final double longitude){
		final SunlightPhase phase = engine.calculateHorizonEvent(body, midnightUtJd, latitude, longitude, Zenith.OFFICIAL);
		return (phase instanceof final SunlightPhase.RegularDay regularDay && regularDay.sunset() != null
			? toJulianDate(regularDay.sunset())
			: Double.NaN);
	}

	private static double toJulianDate(final ZonedDateTime dateTime){
		return dateTime.toInstant().toEpochMilli() / MILLISECONDS_PER_DAY + UNIX_EPOCH_JD;
	}

}
