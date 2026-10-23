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

import java.time.ZonedDateTime;


/**
 * Converter between the Islamic calendar and the Gregorian calendar using the Umm al-Qura criteria.
 * <p>
 * The Umm al-Qura calendar (used in Saudi Arabia) determines the start of a lunar month based on 
 * astronomical calculations in Mecca (21.4225° N, 39.8262° E). A new month begins on the 
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

	private static final double SAUDI_ARABIA_STANDARD_TIME_START_JD = 2429617.5; // 1 January 1940 (Adoption of UTC+3)
	private static final double UMM_AL_QURA_PHASE2_START_JD = 2451286.5;         // 18 April 1999 (1 Muḥarram 1420 AH)
	private static final double UMM_AL_QURA_PHASE3_START_JD = 2452348.5;         // 15 March 2002 (1 Muḥarram 1423 AH)

	// Julian Date of 1970-01-01T00:00Z
	private static final double UNIX_EPOCH_JD = 2440587.5;
	private static final double MILLISECONDS_PER_DAY = 86_400_000.;


	private IslamicCalendarAstronomicalEngine(){}


	/**
	 * Converts an Islamic date to its Julian Day Number using the Umm al-Qura criteria.
	 *
	 * @param year  the Islamic year
	 * @param month the Islamic month (1 to 12)
	 * @param day   the day of the month (1 to 30)
	 * @return the Julian Day Number
	 */
	public static long toJdn(final int year, final int month, final int day){
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("Islamic astronomical calendar requires the Ephemeris plugin.");

		// 1. Approximate JDN of the start of the month using the arithmetic formula
		final long approxStartJdn = ISLAMIC_EPOCH_JDN + (year - 1L) * 354L
			+ Math.floorDiv(3 + 11L * year, 30L) + (month - 1L) * 29L;

		// 2. Find the conjunction (new moon) closest to the approximate start
		// Search window: 5 days before the approximate start
		final double searchStartJd = approxStartJdn - 5.;
		final double newMoonUtJd = engine.findNextNewMoon(searchStartJd);

		// 3. Determine the historical timezone offset for Mecca at this specific date
		final double meccaUtcOffset = getMeccaUtcOffset(newMoonUtJd) / 24.;

		// 4. Apply the Umm al-Qura criteria
		// Convert the conjunction time to Mecca local time
		final double newMoonLocalMeccaJd = newMoonUtJd + meccaUtcOffset;

		// Find the local civil day of the conjunction
		final long localConjunctionCivilJdn = (long)Math.floor(newMoonLocalMeccaJd + 0.5);
		final double testMidnightUtJd = localConjunctionCivilJdn - 0.5 - meccaUtcOffset;

		final long startJdn;
		if(newMoonUtJd >= UMM_AL_QURA_PHASE3_START_JD){
			// Find the sunset and moonset for the local day (in UT)
			final double sunsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
				MECCA_LATITUDE, MECCA_LONGITUDE);
			final double moonsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.MOON, testMidnightUtJd,
				MECCA_LATITUDE, MECCA_LONGITUDE);

			// criteria
			final boolean conjunctionBeforeSunset = (newMoonUtJd < sunsetUtJd);
			final boolean moonsetAfterSunset = (moonsetUtJd > sunsetUtJd);

			// The month starts on the civil day following the evening when both criteria are met.
			if(conjunctionBeforeSunset && moonsetAfterSunset)
				// Month starts tomorrow (the day after the conjunction day)
				startJdn = localConjunctionCivilJdn + 1;
			else
				// Month starts the day after tomorrow
				startJdn = localConjunctionCivilJdn + 2;
		}
		else if(newMoonUtJd >= UMM_AL_QURA_PHASE2_START_JD){
			final double sunsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
				MECCA_LATITUDE, MECCA_LONGITUDE);
			final double moonsetUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.MOON, testMidnightUtJd,
				MECCA_LATITUDE, MECCA_LONGITUDE);

			// criteria
			final boolean moonsetAfterSunset = (moonsetUtJd > sunsetUtJd);

			if(moonsetAfterSunset)
				startJdn = localConjunctionCivilJdn + 1;
			else
				startJdn = localConjunctionCivilJdn + 2;
		}
		else {
			final double sunsetRiyadhUtJd = calculateSetting(engine, EphemerisEngine.CelestialBody.SUN, testMidnightUtJd,
				RIYADH_LATITUDE, RIYADH_LONGITUDE);

			// criteria
			final double moonAgeAtSunsetDays = sunsetRiyadhUtJd - newMoonUtJd;
			final double moonAgeHours = moonAgeAtSunsetDays * 24.;

			if(moonAgeHours >= 12.)
				startJdn = localConjunctionCivilJdn;
			else
				startJdn = localConjunctionCivilJdn + 1;
		}

		return startJdn + (day - 1);
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
