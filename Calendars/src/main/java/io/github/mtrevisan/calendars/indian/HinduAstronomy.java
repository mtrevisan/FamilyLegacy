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

import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;


/**
 * The astronomy of the modern (drik) Indian calendars: sidereal positions with the Lahiri (Chitrapaksha) ayanamsa of
 * the Indian Calendar Reform Committee, used by the Rashtriya Panchang, and the sunrise and sunset at a place.
 * <p>
 * The Lahiri ayanamsa is 23° 15′ 00.658″ on 21 March 1956, 0h TT (Indian Astronomical Ephemeris, 1985), a true value
 * (referred to the true equinox of date): the sidereal longitude is the apparent tropical longitude minus the mean
 * ayanamsa and the nutation in longitude.
 */
final class HinduAstronomy{

	/** Indian Standard Time [days]. */
	static final double IST_OFFSET = 5.5 / 24.;

	/** The Lahiri ayanamsa at the epoch [degrees]. */
	private static final double AYANAMSA_EPOCH_VALUE = 23. + 15. / 60. + 0.658 / 3600.;
	/** 21 March 1956, 0h TT. */
	private static final double AYANAMSA_EPOCH_JD = 2_435_553.5;
	private static final double J2000 = 2_451_545.;
	private static final double DAYS_PER_CENTURY = 36_525.;
	private static final double ARCSECONDS_PER_DEGREE = 3600.;
	private static final double DEGREES_PER_SIGN = 30.;
	/** Mean daily motion of the Moon from the Sun [degrees/day]. */
	private static final double MEAN_ELONGATION_RATE = 360. / 29.530588853;
	/** About 0.1 seconds of time. */
	private static final double ELONGATION_ACCURACY = 1.e-5;
	private static final int MAX_ITERATIONS = 30;


	private HinduAstronomy(){}


	static EphemerisEngine engine(){
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException("The Indian calendars require the Ephemeris astronomical plugin.");
		return engine;
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the true Lahiri ayanamsa [degrees]
	 */
	static double ayanamsa(final double jd){
		return AYANAMSA_EPOCH_VALUE + precession(jd) - precession(AYANAMSA_EPOCH_JD) + nutationInLongitude(jd)
			- nutationInLongitude(AYANAMSA_EPOCH_JD);
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the sidereal longitude of the Sun [degrees, 0 to 360)
	 */
	static double siderealSunLongitude(final double jd){
		return normalize(Math.toDegrees(engine().calculateApparentSunLongitude(jd)) - ayanamsa(jd));
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the sidereal longitude of the Moon [degrees, 0 to 360)
	 */
	static double siderealMoonLongitude(final double jd){
		return normalize(Math.toDegrees(engine().calculateApparentMoonLongitude(jd)) - ayanamsa(jd));
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the elongation of the Moon from the Sun [degrees, 0 to 360)
	 */
	static double elongation(final double jd){
		final EphemerisEngine engine = engine();
		return normalize(Math.toDegrees(engine.calculateApparentMoonLongitude(jd) - engine.calculateApparentSunLongitude(jd)));
	}

	/**
	 * Finds the instant the Sun enters a sidereal sign (sankranti).
	 *
	 * @param approximateJd a guess within a few days of the event
	 * @param sign          the sign (0 = Mesha, Aries, ... 11 = Mina, Pisces)
	 * @return the Julian Date (UT) of the sankranti
	 */
	static double sankranti(final double approximateJd, final int sign){
		final EphemerisEngine engine = engine();
		double jd = approximateJd;
		// the ayanamsa changes slowly: two iterations are enough
		for(int i = 0; i < 2; i ++){
			final double tropical = normalize(sign * DEGREES_PER_SIGN + ayanamsa(jd));
			jd = engine.findSolarLongitudeEvent(jd, Math.toRadians(tropical));
		}
		return jd;
	}

	/**
	 * Finds the instant the elongation of the Moon reaches a value, after a given instant.
	 *
	 * @param fromJd     the instant to search from
	 * @param elongation the elongation [degrees]
	 * @return the Julian Date (UT) of the event
	 */
	static double elongationEvent(final double fromJd, final double elongation){
		// the elongation always grows (between about 10° and 15° a day): the first step with the mean rate lands within
		// a few tens of degrees from the target, then the signed residual converges
		double remaining = normalize(elongation - elongation(fromJd));
		if(remaining == 0.)
			return fromJd;

		double jd = fromJd;
		for(int i = 0; i < MAX_ITERATIONS && Math.abs(remaining) > ELONGATION_ACCURACY; i ++){
			jd += remaining / MEAN_ELONGATION_RATE;
			remaining = -signedDelta(elongation(jd), elongation);
		}
		return jd;
	}

	/**
	 * @return the angle from the target to the value, in (-180, 180]
	 */
	private static double signedDelta(final double value, final double target){
		double delta = normalize(value - target);
		if(delta > 180.)
			delta -= 360.;
		return delta;
	}

	/**
	 * @param jdn       the civil day (Indian Standard Time)
	 * @param latitude  the latitude of the place [degrees]
	 * @param longitude the longitude of the place [degrees]
	 * @return the sunrise and sunset of the day (UT)
	 */
	static double[] sunriseSunset(final long jdn, final double latitude, final double longitude){
		final double localMidnight = jdn - 0.5 - IST_OFFSET;
		final SunlightPhase phase = engine().calculateHorizonEvent(EphemerisEngine.CelestialBody.SUN, localMidnight,
			latitude, longitude, Zenith.OFFICIAL);
		if(!(phase instanceof SunlightPhase.RegularDay(var sunrise, var transit, var sunset)) || sunrise == null
				|| sunset == null)
			throw new IllegalStateException("No sunrise or sunset at the place on JDN " + jdn);
		return new double[]{toJd(sunrise.toInstant().toEpochMilli()), toJd(sunset.toInstant().toEpochMilli())};
	}

	/**
	 * @param jd a Julian Date (UT)
	 * @return the civil day (Indian Standard Time) of the instant
	 */
	static long civilDay(final double jd){
		return (long)Math.floor(jd + IST_OFFSET + 0.5);
	}

	private static double toJd(final long epochMillis){
		return epochMillis / 86_400_000. + 2_440_587.5;
	}

	/** General precession in longitude (IAU 2006) [degrees]. */
	private static double precession(final double jd){
		final double t = (jd - J2000) / DAYS_PER_CENTURY;
		return (5028.796195 * t + 1.1054348 * t * t + 0.00007964 * t * t * t) / ARCSECONDS_PER_DEGREE;
	}

	/** Nutation in longitude, main terms (Meeus, Astronomical Algorithms, ch. 22, accuracy 0.5″) [degrees]. */
	private static double nutationInLongitude(final double jd){
		final double t = (jd - J2000) / DAYS_PER_CENTURY;
		final double omega = Math.toRadians(125.04452 - 1934.136261 * t);
		final double sunMeanLongitude = Math.toRadians(280.4665 + 36000.7698 * t);
		final double moonMeanLongitude = Math.toRadians(218.3165 + 481267.8813 * t);
		return (-17.20 * Math.sin(omega) - 1.32 * Math.sin(2. * sunMeanLongitude) - 0.23 * Math.sin(2. * moonMeanLongitude)
			+ 0.21 * Math.sin(2. * omega)) / ARCSECONDS_PER_DEGREE;
	}

	static double normalize(final double degrees){
		final double value = degrees % 360.;
		return (value < 0.? value + 360.: value);
	}

}
