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
package io.github.mtrevisan.ephemeris.engine;

import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.helpers.SiderealTime;

import java.util.function.Function;


/**
 * High-precision sunrise and sunset calculator utilizing VSOP2013 planetary theory
 * and IAU 2000A nutation corrections.
 */
public final class SolarHorizonEvents{

	// Standard structural altitude for solar disk upper limb sunset:
	// -34 arcminutes (atmospheric refraction) - 16 arcminutes (solar semidiameter) = -50 arcminutes = -0.8333333 degrees
	public static final double STANDARD_SUNSET_ALTITUDE = StrictMath.toRadians(-50. / 60.);

	// The day is scanned with this step to bracket the events [day]
	private static final double SCAN_STEP = 1. / 24.;
	// Precision of the events [day] (~0.01 s)
	private static final double ACCURACY = 1.e-7;
	private static final int MAX_ITERATIONS = 50;


	private SolarHorizonEvents(){}


	/**
	 * Calculates the exact Julian Date (Universal Time) of sunset for a specific civil day and location.
	 *
	 * @param midnightUtJd The Julian Date representing local civil midnight converted to UT
	 * @param latitude     The observer's geodetic latitude in degrees (North positive)
	 * @param longitude    The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The Julian Date (UT) of the sunset event, or Double.NaN if the Sun does not set during that day (polar
	 * 	day/night)
	 */
	public static double calculateSunset(final double midnightUtJd, final double latitude, final double longitude,
			final Function<Double, Double> ttCenturyFunction){
		return calculateEvents(midnightUtJd, latitude, longitude, STANDARD_SUNSET_ALTITUDE, ttCenturyFunction)
			.set();
	}

	/**
	 * Calculates the exact Julian Dates (Universal Time) of the crossings of a given altitude by the Sun, and of its
	 * upper transit, for a specific civil day and location.
	 *
	 * @param midnightUtJd      The Julian Date representing local civil midnight converted to UT
	 * @param latitude          The observer's geodetic latitude in degrees (North positive)
	 * @param longitude         The observer's geodetic longitude in degrees (East positive)
	 * @param targetAltitude    The altitude of the center of the Sun [rad] (e.g. {@link #STANDARD_SUNSET_ALTITUDE} for
	 * 	sunrise and sunset, -6° for the civil twilight)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The events of the day; rise and set are Double.NaN if they do not happen during that day
	 */
	public static SolarDay calculateEvents(final double midnightUtJd, final double latitude, final double longitude,
			final double targetAltitude, final Function<Double, Double> ttCenturyFunction){
		final SunTrack track = new SunTrack(midnightUtJd, latitude, longitude, ttCenturyFunction);

		// 1. Scan the civil day to bracket the events: the altitude crosses the target at rise and set, the hour angle
		//    changes sign at the upper transit
		double rise = Double.NaN;
		double transit = Double.NaN;
		double set = Double.NaN;
		double previousTime = midnightUtJd;
		double previousHeight = track.altitude(previousTime) - targetAltitude;
		double previousHourAngle = track.hourAngle(previousTime);
		for(double time = midnightUtJd + SCAN_STEP; time < midnightUtJd + 1. + SCAN_STEP / 2.; time += SCAN_STEP){
			final double height = track.altitude(time) - targetAltitude;
			final double hourAngle = track.hourAngle(time);

			// 2. Refine the instants inside the bracketing steps
			if(Double.isNaN(rise) && previousHeight <= 0. && height > 0.)
				rise = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> track.altitude(jd) - targetAltitude);
			if(Double.isNaN(set) && previousHeight > 0. && height <= 0.)
				set = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> track.altitude(jd) - targetAltitude);
			// the hour angle increases through zero at the upper transit, while it jumps from +π to -π at the lower one
			if(Double.isNaN(transit) && previousHourAngle < 0. && hourAngle >= 0.
					&& hourAngle - previousHourAngle < StrictMath.PI)
				transit = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS, track::hourAngle);

			previousTime = time;
			previousHeight = height;
			previousHourAngle = hourAngle;
		}

		final double transitAltitude = track.altitude(Double.isNaN(transit)? midnightUtJd + 0.5: transit);
		return new SolarDay(rise, transit, set, transitAltitude);
	}


	/**
	 * The solar events of a day.
	 *
	 * @param rise            the Julian Date (UT) of the rising crossing, or Double.NaN if it does not happen during the day
	 * @param transit         the Julian Date (UT) of the upper transit, or Double.NaN if it does not happen during the day
	 * @param set             the Julian Date (UT) of the setting crossing, or Double.NaN if it does not happen during the
	 * 	day
	 * @param transitAltitude the altitude of the center of the Sun at the upper transit (or at noon, if there is no
	 * 	transit during the day) [rad]
	 */
	public record SolarDay(double rise, double transit, double set, double transitAltitude){}


	/**
	 * Apparent equatorial coordinates of the Sun during a day, interpolated from three positions (Meeus, Astronomical
	 * Algorithms, ch. 15), and the corresponding horizontal coordinates for an observer.
	 */
	private static final class SunTrack{
		private final double midnightUtJd;
		private final double sinLatitude;
		private final double cosLatitude;
		private final double longitude;
		private final Function<Double, Double> ttCenturyFunction;

		// right ascension (unwrapped) and declination at midnight, noon, and the next midnight [rad]
		private final double[] rightAscension = new double[3];
		private final double[] declination = new double[3];
		// nutation in longitude and true obliquity at noon (their variation during a day is negligible) [rad]
		private final double deltaPsi;
		private final double trueObliquity;


		private SunTrack(final double midnightUtJd, final double latitude, final double longitude,
				final Function<Double, Double> ttCenturyFunction){
			this.midnightUtJd = midnightUtJd;
			final double latRad = StrictMath.toRadians(latitude);
			sinLatitude = StrictMath.sin(latRad);
			cosLatitude = StrictMath.cos(latRad);
			this.longitude = StrictMath.toRadians(longitude);
			this.ttCenturyFunction = ttCenturyFunction;

			double noonDeltaPsi = 0.;
			double noonTrueObliquity = 0.;
			for(int i = 0; i < 3; i ++){
				final double ttJc = ttCenturyFunction.apply(midnightUtJd + i * 0.5);
				final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
				final double lambda = SunPosition.apparentSunLongitude(ttJc, nutation.getDeltaPsi());
				final double obliquity = NutationCorrections.meanObliquity(ttJc) + nutation.getDeltaEpsilon();

				// ecliptic to equatorial coordinates (the latitude of the Sun is below 1")
				double alpha = MathHelper.mod2pi(StrictMath.atan2(StrictMath.sin(lambda) * StrictMath.cos(obliquity),
					StrictMath.cos(lambda)));
				if(i > 0)
					// keep the right ascension continuous across 0h
					alpha = rightAscension[i - 1] + MathHelper.modpipi(alpha - rightAscension[i - 1]);
				rightAscension[i] = alpha;
				declination[i] = StrictMath.asin(StrictMath.sin(obliquity) * StrictMath.sin(lambda));

				if(i == 1){
					noonDeltaPsi = nutation.getDeltaPsi();
					noonTrueObliquity = obliquity;
				}
			}
			deltaPsi = noonDeltaPsi;
			trueObliquity = noonTrueObliquity;
		}

		/**
		 * @return the altitude of the center of the Sun, without refraction [rad]
		 */
		private double altitude(final double utJd){
			final double dec = interpolate(declination, utJd);
			return StrictMath.asin(sinLatitude * StrictMath.sin(dec)
				+ cosLatitude * StrictMath.cos(dec) * StrictMath.cos(rawHourAngle(utJd)));
		}

		/**
		 * @return the local hour angle of the Sun in the interval [-π, π) [rad]
		 */
		private double hourAngle(final double utJd){
			return MathHelper.modpipi(rawHourAngle(utJd));
		}

		private double rawHourAngle(final double utJd){
			final double gast = SiderealTime.greenwichApparentSiderealTime(utJd, ttCenturyFunction.apply(utJd), deltaPsi,
				trueObliquity);
			return gast + longitude - interpolate(rightAscension, utJd);
		}

		/** Quadratic interpolation through the values at midnight, noon, and the next midnight. */
		private double interpolate(final double[] values, final double utJd){
			// position in half days from noon
			final double n = 2. * (utJd - midnightUtJd) - 1.;
			final double a = values[1] - values[0];
			final double b = values[2] - values[1];
			return values[1] + n / 2. * (a + b + n * (b - a));
		}
	}

}
