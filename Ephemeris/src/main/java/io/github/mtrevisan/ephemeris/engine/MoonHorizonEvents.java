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

import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.SiderealTime;

import java.util.function.Function;


/**
 * High-precision lunar horizon event calculator utilizing ELP/MPP02 planetary theory,
 * topocentric parallax adjustments, and dynamical semidiameter variations.
 * <p>
 * The moonset is the instant the upper limb of the Moon touches the horizon, with the standard refraction of 34', as
 * in the U.S. Naval Observatory tables.
 * </p>
 */
public final class MoonHorizonEvents{

	// Standard atmospheric refraction at the horizon: 34 arcminutes
	private static final double HORIZON_REFRACTION = StrictMath.toRadians(34. / 60.);

	// Earth equatorial radius in kilometers (IAU 1976, as in Meeus, Astronomical Algorithms, ch. 11)
	private static final double EARTH_EQUATORIAL_RADIUS_KM = 6378.14;
	// Polar to equatorial radius ratio b/a of the Earth ellipsoid (flattening 1/298.257)
	private static final double EARTH_AXIS_RATIO = 0.99664719;
	// Ratio of the lunar radius to the Earth equatorial radius (IAU)
	private static final double MOON_TO_EARTH_RADIUS = 0.2725076;

	// The day is scanned with this step to bracket the moonset [day]
	private static final double SCAN_STEP = 1. / 24.;
	// Precision of the moonset [day] (~0.01 s)
	private static final double ACCURACY = 1.e-7;
	private static final int MAX_ITERATIONS = 50;


	private MoonHorizonEvents(){}


	/**
	 * Calculates the exact Julian Date (Universal Time) of moonset for a specific civil day and location.
	 *
	 * @param midnightUtJd      The Julian Date representing local civil midnight converted to UT (ending in .5)
	 * @param latitude          The observer's geodetic latitude in degrees (North positive)
	 * @param longitude         The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The Julian Date (UT) of the moonset event, or Double.NaN if the Moon does not set during that day (it is
	 * 	circumpolar, continuously below the horizon, or simply sets the day after, as happens about once a month)
	 */
	public static double calculateMoonset(final double midnightUtJd, final double latitude, final double longitude,
			final Function<Double, Double> ttCenturyFunction){
		final Observer observer = new Observer(latitude, longitude);

		// 1. Scan the civil day to bracket the moonset: the height of the upper limb above the horizon changes sign
		//    from positive to negative (the Moon moves by less than a degree per hour with respect to the stars, so an
		//    hourly step cannot skip a setting)
		double previousTime = midnightUtJd;
		double previousHeight = upperLimbHeight(previousTime, observer, ttCenturyFunction);
		for(double time = midnightUtJd + SCAN_STEP; time < midnightUtJd + 1. + SCAN_STEP / 2.; time += SCAN_STEP){
			final double height = upperLimbHeight(time, observer, ttCenturyFunction);
			if(previousHeight > 0. && height <= 0.)
				// 2. Refine the instant inside the bracketing step
				return IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> upperLimbHeight(jd, observer, ttCenturyFunction));

			previousTime = time;
			previousHeight = height;
		}
		return Double.NaN;
	}

	/**
	 * Computes the height of the upper limb of the Moon above the apparent horizon, applying parallax adjustments,
	 * refraction and the topocentric semidiameter.
	 *
	 * @return the height [rad]: positive if the upper limb is visible, negative otherwise
	 */
	private static double upperLimbHeight(final double utJd, final Observer observer,
			final Function<Double, Double> ttCenturyFunction){
		final double ttJc = ttCenturyFunction.apply(utJd);

		// 1. Apparent geocentric ecliptic coordinates of date
		final EclipticCoordinates moon = MoonPosition.getInstanceLLR()
			.eclipticOfDate(ttJc);
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		final double lambda = MoonPosition.computeApparentMoonLongitude(moon, deltaPsi);
		final double beta = moon.getLatitude();
		final double distanceKm = moon.getDistance();
		final double trueObliquity = NutationCorrections.meanObliquity(ttJc) + nutation.getDeltaEpsilon();

		// 2. Ecliptic to equatorial coordinates (Meeus, Astronomical Algorithms, eqs. 13.3 and 13.4)
		final double sinEpsilon = StrictMath.sin(trueObliquity);
		final double cosEpsilon = StrictMath.cos(trueObliquity);
		final double sinBeta = StrictMath.sin(beta);
		final double cosBeta = StrictMath.cos(beta);
		final double sinLambda = StrictMath.sin(lambda);
		final double rightAscension = StrictMath.atan2(sinLambda * cosEpsilon * cosBeta - sinBeta * sinEpsilon,
			StrictMath.cos(lambda) * cosBeta);
		final double declination = StrictMath.asin(sinBeta * cosEpsilon + cosBeta * sinEpsilon * sinLambda);

		// 3. Local hour angle from the Greenwich apparent sidereal time
		final double gast = SiderealTime.greenwichApparentSiderealTime(utJd, ttJc, deltaPsi, trueObliquity);
		final double hourAngle = gast + observer.longitude - rightAscension;

		// 4. Topocentric coordinates, removing the parallax (geocentric position of the observer on the ellipsoid)
		final double cosDeclination = StrictMath.cos(declination);
		final double deltaX = distanceKm * cosDeclination * StrictMath.cos(hourAngle)
			- EARTH_EQUATORIAL_RADIUS_KM * observer.rhoCosPhi;
		final double deltaY = distanceKm * cosDeclination * StrictMath.sin(hourAngle);
		final double deltaZ = distanceKm * StrictMath.sin(declination)
			- EARTH_EQUATORIAL_RADIUS_KM * observer.rhoSinPhi;
		final double topocentricDistanceKm = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		final double topocentricDeclination = StrictMath.asin(deltaZ / topocentricDistanceKm);
		final double topocentricHourAngle = StrictMath.atan2(deltaY, deltaX);

		// 5. Altitude of the center (Meeus, Astronomical Algorithms, eq. 13.6)
		final double altitude = StrictMath.asin(observer.sinLatitude * StrictMath.sin(topocentricDeclination)
			+ observer.cosLatitude * StrictMath.cos(topocentricDeclination) * StrictMath.cos(topocentricHourAngle));

		// 6. The upper limb is on the horizon when the center is a semidiameter plus the refraction below it
		final double semidiameter = StrictMath.asin(MOON_TO_EARTH_RADIUS * EARTH_EQUATORIAL_RADIUS_KM
			/ topocentricDistanceKm);
		return altitude + HORIZON_REFRACTION + semidiameter;
	}


	/**
	 * The observer, with the geocentric coordinates of the place at sea level (Meeus, Astronomical Algorithms,
	 * ch. 11).
	 */
	private static final class Observer{
		private final double longitude;
		private final double sinLatitude;
		private final double cosLatitude;
		private final double rhoSinPhi;
		private final double rhoCosPhi;

		private Observer(final double latitude, final double longitude){
			final double latRad = StrictMath.toRadians(latitude);
			this.longitude = StrictMath.toRadians(longitude);
			sinLatitude = StrictMath.sin(latRad);
			cosLatitude = StrictMath.cos(latRad);

			final double u = StrictMath.atan(EARTH_AXIS_RATIO * StrictMath.tan(latRad));
			rhoSinPhi = EARTH_AXIS_RATIO * StrictMath.sin(u);
			rhoCosPhi = StrictMath.cos(u);
		}
	}

}
