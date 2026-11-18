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
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.helpers.SiderealTime;

import java.util.function.DoubleUnaryOperator;


/**
 * High-precision lunar horizon event calculator utilizing the ELP/MPP02 lunar theory,
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
	// Polar to equatorial radius ratio `b/a` of the Earth ellipsoid (flattening 1/298.257)
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
	 * @param midnightUtJd      The Julian Date representing local civil midnight converted to UT
	 * @param latitude          The observer's geodetic latitude in degrees (North positive)
	 * @param longitude         The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The Julian Date (UT) of the moonset event, or Double.NaN if the Moon does not set during that day (it is
	 * 	circumpolar, continuously below the horizon, or simply sets the day after, as happens about once a month)
	 */
	public static double calculateMoonset(final double midnightUtJd, final double latitude, final double longitude,
			final DoubleUnaryOperator ttCenturyFunction){
		return calculateEvents(midnightUtJd, latitude, longitude, ttCenturyFunction)
			.set();
	}

	/**
	 * Calculates the exact Julian Dates (Universal Time) of moonrise, upper transit and moonset for a specific civil day
	 * and location.
	 *
	 * @param midnightUtJd      The Julian Date representing local civil midnight converted to UT
	 * @param latitude          The observer's geodetic latitude in degrees (North positive)
	 * @param longitude         The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The events of the day; each one is Double.NaN if it does not happen during that day (the Moon rises,
	 * 	transits, and sets about 50 minutes later each day, so about once a month one of them falls on the day after)
	 */
	public static LunarDay calculateEvents(final double midnightUtJd, final double latitude, final double longitude,
			final DoubleUnaryOperator ttCenturyFunction){
		final Observer observer = new Observer(latitude, longitude);
		final MoonTrack track = new MoonTrack(midnightUtJd, observer, ttCenturyFunction);

		// 1. Scan the civil day to bracket the events: the height of the upper limb above the horizon changes sign at
		//    moonrise and moonset, the hour angle changes sign at the upper transit (the Moon moves by less than a degree
		//    per hour with respect to the stars, so an hourly step cannot skip an event)
		double rise = Double.NaN;
		double transit = Double.NaN;
		double set = Double.NaN;
		double previousTime = midnightUtJd;
		MoonState previous = moonState(previousTime, track);
		final MoonState noon = moonState(midnightUtJd + 0.5, track);
		double transitHeight = noon.height;
		for(double time = midnightUtJd + SCAN_STEP; time < midnightUtJd + 1. + SCAN_STEP / 2.; time += SCAN_STEP){
			final MoonState current = moonState(time, track);

			// 2. Refine the instants inside the bracketing steps
			if(Double.isNaN(rise) && previous.height <= 0. && current.height > 0.)
				rise = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> moonState(jd, track).height);
			if(Double.isNaN(set) && previous.height > 0. && current.height <= 0.)
				set = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> moonState(jd, track).height);
			// the hour angle increases through zero at the upper transit, while it jumps from +π to -π at the lower one
			if(Double.isNaN(transit) && previous.hourAngle < 0. && current.hourAngle >= 0.
					&& current.hourAngle - previous.hourAngle < StrictMath.PI){
				transit = IllinoisSolver.solve(previousTime, time, ACCURACY, MAX_ITERATIONS,
					jd -> moonState(jd, track).hourAngle);
				transitHeight = moonState(transit, track).height;
			}

			previousTime = time;
			previous = current;
		}
		return new LunarDay(rise, transit, set, transitHeight);
	}

	/**
	 * Computes the height of the upper limb of the Moon above the apparent horizon, applying parallax adjustments,
	 * refraction and the topocentric semidiameter, and its topocentric hour angle.
	 *
	 * @return the height [rad] (positive if the upper limb is visible, negative otherwise), and the hour angle [rad] in
	 * 	the interval {@code (-π, π]}
	 */
	private static MoonState moonState(final double utJd, final MoonTrack track){
		final Observer observer = track.observer;
		final double ttJc = track.ttCenturyFunction.applyAsDouble(utJd);

		// 1. Apparent geocentric equatorial coordinates, interpolated from the hourly positions
		final double rightAscension = track.interpolate(track.rightAscension, utJd);
		final double declination = track.interpolate(track.declination, utJd);
		final double distanceKm = track.interpolate(track.distance, utJd);

		// 2. Local hour angle from the Greenwich apparent sidereal time
		final double gast = SiderealTime.greenwichApparentSiderealTime(utJd, ttJc, track.deltaPsi, track.trueObliquity);
		final double hourAngle = gast + observer.longitude - rightAscension;

		// 3. Topocentric coordinates, removing the parallax (geocentric position of the observer on the ellipsoid)
		final double cosDeclination = StrictMath.cos(declination);
		final double deltaX = distanceKm * cosDeclination * StrictMath.cos(hourAngle)
			- EARTH_EQUATORIAL_RADIUS_KM * observer.rhoCosPhi;
		final double deltaY = distanceKm * cosDeclination * StrictMath.sin(hourAngle);
		final double deltaZ = distanceKm * StrictMath.sin(declination)
			- EARTH_EQUATORIAL_RADIUS_KM * observer.rhoSinPhi;
		final double topocentricDistanceKm = StrictMath.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
		final double topocentricDeclination = StrictMath.asin(deltaZ / topocentricDistanceKm);
		final double topocentricHourAngle = StrictMath.atan2(deltaY, deltaX);

		// 4. Altitude of the center (Meeus, Astronomical Algorithms, eq. 13.6)
		final double altitude = StrictMath.asin(observer.sinLatitude * StrictMath.sin(topocentricDeclination)
			+ observer.cosLatitude * StrictMath.cos(topocentricDeclination) * StrictMath.cos(topocentricHourAngle));

		// 5. The upper limb is on the horizon when the center is a semidiameter plus the refraction below it
		final double semidiameter = StrictMath.asin(MOON_TO_EARTH_RADIUS * EARTH_EQUATORIAL_RADIUS_KM
			/ topocentricDistanceKm);
		return new MoonState(altitude + HORIZON_REFRACTION + semidiameter, topocentricHourAngle);
	}


	/**
	 * The lunar events of a day.
	 *
	 * @param rise          the Julian Date (UT) of moonrise, or Double.NaN if the Moon does not rise during the day
	 * @param transit       the Julian Date (UT) of the upper transit, or Double.NaN if it does not happen during the day
	 * @param set           the Julian Date (UT) of moonset, or Double.NaN if the Moon does not set during the day
	 * @param transitHeight the height of the upper limb above the horizon at the upper transit (or at noon, if there is
	 * 	no transit during the day) [rad]
	 */
	public record LunarDay(double rise, double transit, double set, double transitHeight){}

	private record MoonState(double height, double hourAngle){}

	/**
	 * Apparent geocentric equatorial coordinates of the Moon at hourly nodes around the civil day (one node before and one
	 * after it, for the interpolation), interpolated with a quadratic through the three nearest nodes (Meeus, Astronomical
	 * Algorithms, ch. 3): the lunar series is evaluated once per node instead of once per root-finder iteration.
	 */
	private static final class MoonTrack{
		// the nodes are at midnight + (index - 1) hours
		private static final int NODES = 27;

		private final double midnightUtJd;
		private final Observer observer;
		private final DoubleUnaryOperator ttCenturyFunction;
		// right ascension (unwrapped), declination [rad], and distance [km] at the nodes
		private final double[] rightAscension = new double[NODES];
		private final double[] declination = new double[NODES];
		private final double[] distance = new double[NODES];
		// nutation in longitude and true obliquity at noon (their variation during a day is negligible) [rad]
		private final double deltaPsi;
		private final double trueObliquity;


		private MoonTrack(final double midnightUtJd, final Observer observer, final DoubleUnaryOperator ttCenturyFunction){
			this.midnightUtJd = midnightUtJd;
			this.observer = observer;
			this.ttCenturyFunction = ttCenturyFunction;

			final NutationCorrections noonNutation = NutationCorrections.calculate(
				ttCenturyFunction.applyAsDouble(midnightUtJd + 0.5));
			deltaPsi = noonNutation.getDeltaPsi();
			trueObliquity = NutationCorrections.meanObliquity(ttCenturyFunction.applyAsDouble(midnightUtJd + 0.5))
				+ noonNutation.getDeltaEpsilon();

			for(int i = 0; i < NODES; i ++){
				final double ttJc = ttCenturyFunction.applyAsDouble(midnightUtJd + (i - 1) * SCAN_STEP);

				// apparent geocentric ecliptic coordinates of date
				final EclipticCoordinates moon = MoonPosition.getInstanceLLR()
					.eclipticOfDate(ttJc);
				final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
				final double lambda = MoonPosition.computeApparentMoonLongitude(moon, nutation.getDeltaPsi());
				final double beta = moon.latitude();
				final double obliquity = NutationCorrections.meanObliquity(ttJc) + nutation.getDeltaEpsilon();

				// ecliptic to equatorial coordinates (Meeus, Astronomical Algorithms, eqs. 13.3 and 13.4)
				final double sinEpsilon = StrictMath.sin(obliquity);
				final double cosEpsilon = StrictMath.cos(obliquity);
				final double sinBeta = StrictMath.sin(beta);
				final double cosBeta = StrictMath.cos(beta);
				final double sinLambda = StrictMath.sin(lambda);
				double alpha = StrictMath.atan2(sinLambda * cosEpsilon * cosBeta - sinBeta * sinEpsilon,
					StrictMath.cos(lambda) * cosBeta);
				if(i > 0)
					// keep the right ascension continuous across 0h
					alpha = rightAscension[i - 1] + MathHelper.modpipi(alpha - rightAscension[i - 1]);
				rightAscension[i] = alpha;
				declination[i] = StrictMath.asin(sinBeta * cosEpsilon + cosBeta * sinEpsilon * sinLambda);
				distance[i] = moon.distance();
			}
		}

		private double interpolate(final double[] values, final double utJd){
			// nearest node with a neighbour on both sides
			final double position = (utJd - midnightUtJd) / SCAN_STEP + 1.;
			final int k = Math.clamp(Math.round(position), 1, NODES - 2);
			final double n = position - k;
			final double a = values[k] - values[k - 1];
			final double b = values[k + 1] - values[k];
			return values[k] + n / 2. * (a + b + n * (b - a));
		}
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
