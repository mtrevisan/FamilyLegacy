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

import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;

import java.util.function.Function;


/**
 * High-precision sunrise and sunset calculator utilizing VSOP2013 planetary theory
 * and IAU 2000A nutation corrections.
 */
public final class SolarHorizonEvents{

	// Standard structural altitude for solar disk upper limb sunset:
	// -34 arcminutes (atmospheric refraction) - 16 arcminutes (solar semidiameter) = -50 arcminutes = -0.8333333 degrees
	private static final double STANDARD_SUNSET_ALTITUDE = StrictMath.toRadians(-50. / 60.);


	private SolarHorizonEvents(){}


	/**
	 * Calculates the exact Julian Date (Universal Time) of sunset for a specific civil day and location.
	 *
	 * @param midnightUtJd The Julian Date representing local civil midnight converted to UT (ending in .5)
	 * @param latitude     The observer's geodetic latitude in degrees (North positive)
	 * @param longitude    The observer's geodetic longitude in degrees (East positive)
	 * @param ttCenturyFunction Functional bridge to calculate Terrestrial Centuries from a Julian Date UT
	 * @return The Julian Date (UT) of the sunset event, or Double.NaN if the Sun is circumpolar (polar day/night)
	 */
	public static double calculateSunset(final double midnightUtJd, final double latitude, final double longitude,
			final Function<Double, Double> ttCenturyFunction){
		final double latRad = StrictMath.toRadians(latitude);

		// 1. Coarse estimation of the sunset time using standard geometric projections.
		// Approximate Greenwich Hour Angle calculation for solar noon.
		final double approximateNoonUt = midnightUtJd + 0.5 - longitude / 360.;
		final double jcNoon = ttCenturyFunction.apply(approximateNoonUt);

		// Fetch basic nutation and apparent solar coordinates to evaluate approximate declination
		final NutationCorrections nutationNoon = NutationCorrections.calculate(jcNoon);
		final double deltaPsiNoon = nutationNoon.getDeltaPsi();
		final double deltaEpsilonNoon = nutationNoon.getDeltaEpsilon();

		// Approximate true obliquity of date
		final double epsilon0Noon = StrictMath.toRadians(23.43929111 - 46.836769 * jcNoon / 3600.);
		final double trueObliquityNoon = epsilon0Noon + deltaEpsilonNoon;

		// Find approximate solar geometric position
		final double lambdaApparentNoon = SunPosition.apparentSunLongitude(jcNoon, deltaPsiNoon);
		final double declinationNoon = StrictMath.asin(StrictMath.sin(trueObliquityNoon)
			* StrictMath.sin(lambdaApparentNoon));

		// Calculate diurnal semi-arc hour angle (H0)
		final double cosH0 = (StrictMath.sin(STANDARD_SUNSET_ALTITUDE)
			- StrictMath.sin(latRad) * StrictMath.sin(declinationNoon))
			/ (StrictMath.cos(latRad) * StrictMath.cos(declinationNoon));

		if(cosH0 > 1. || cosH0 < -1.)
			// Sun is either continuously above or below the horizon (polar phenomena)
			return Double.NaN;

		final double h0Deg = StrictMath.toDegrees(StrictMath.acos(cosH0));

		// Coarse estimate of sunset in fractions of a day from UT midnight
		final double approximateSunsetFraction = (12. + (h0Deg - longitude) / 15.) / 24.;
		final double targetSunsetUtJd = midnightUtJd + approximateSunsetFraction;

		// 2. High-Precision Refinement Framework via Three-Point Inverse Interpolation.
		// We sample 3 tracking points: 1 hour before, exactly at, and 1 hour after the coarse estimate.
		final double hourInJd = 1. / 24.;
		final double t1 = targetSunsetUtJd - hourInJd;
		final double t2 = targetSunsetUtJd;
		final double t3 = targetSunsetUtJd + hourInJd;

		final double y1 = computeSolarAltitude(t1, latRad, longitude, ttCenturyFunction);
		final double y2 = computeSolarAltitude(t2, latRad, longitude, ttCenturyFunction);
		final double y3 = computeSolarAltitude(t3, latRad, longitude, ttCenturyFunction);

		// Calculate parabolic interpolation coefficients across the tracking window
		final double a = (y1 + y3) / 2. - y2;
		final double b = (y3 - y1) / 2.;
		final double c = y2 - STANDARD_SUNSET_ALTITUDE;

		// Solve the quadratic equation a*x^2 + b*x + c = 0 to isolate the boundary transition offset
		final double discriminant = b * b - 4. * a * c;
		if(discriminant < 0.)
			// Parabolic vertex does not intersect threshold; return coarse estimate as fallback
			return targetSunsetUtJd;

		// Interpolation correction factor (ranging from -1.0 to +1.0 hour)
		double correctionFactor = (-b + StrictMath.sqrt(discriminant)) / (2. * a);
		if(StrictMath.abs(correctionFactor) > 1.)
			correctionFactor = (-b - StrictMath.sqrt(discriminant)) / (2. * a);

		// Apply temporal correction factor mapping back onto the Julian Timeline
		return targetSunsetUtJd + correctionFactor * hourInJd;
	}

	/**
	 * Computes the apparent topocentric local altitude of the Sun for a given time and location.
	 */
	private static double computeSolarAltitude(final double utJd, final double latRad, final double longitude,
			final Function<Double, Double> ttCenturyFunction){
		final double ttJc = ttCenturyFunction.apply(utJd);
		final double jc = JulianDate.centuryJ2000Of(utJd);

		// 1. Calculate nutation parameters for the current date centurial slice
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		final double deltaEpsilon = nutation.getDeltaEpsilon();

		// 2. Extract apparent ecliptic longitude from the VSOP2013 analytical series
		// Note: apparentSunLongitude requires Julian Ephemeris Centuries from J2000
		final double lambdaApparent = SunPosition.apparentSunLongitude(ttJc, deltaPsi);

		// 3. Compute true obliquity of date
		final double epsilon0 = StrictMath.toRadians(23.43929111 - 46.836769 * ttJc / 3600.);
		final double trueObliquity = epsilon0 + deltaEpsilon;

		// 4. Convert ecliptic apparent coordinates to geocentric equatorial coordinates
		final double sinAlphaNum = StrictMath.sin(lambdaApparent) * StrictMath.cos(trueObliquity);
		final double cosAlphaDen = StrictMath.cos(lambdaApparent);
		final double rightAscension = MathHelper.mod2pi(StrictMath.atan2(sinAlphaNum, cosAlphaDen));
		final double declination = StrictMath.asin(StrictMath.sin(trueObliquity) * StrictMath.sin(lambdaApparent));

		// 5. Derive the Apparent Greenwich Sidereal Time (GAST) to link Earth rotation position
		final double gmst = computeGMST(utJd, jc);
		final double gast = gmst + deltaPsi * StrictMath.cos(trueObliquity);

		// 6. Calculate Local Hour Angle (LHA) for the spatial coordinate translation
		final double localSiderealTime = gast + StrictMath.toRadians(longitude);
		final double hourAngle = localSiderealTime - rightAscension;

		// 7. Map geocentric equatorial coordinates to the local horizontal coordinate plane
		return StrictMath.asin(StrictMath.sin(latRad) * StrictMath.sin(declination)
			+ StrictMath.cos(latRad) * StrictMath.cos(declination) * StrictMath.cos(hourAngle));
	}

	/**
	 * Computes the Greenwich Mean Sidereal Time (GMST) matching IAU 1982 definitions.
	 */
	private static double computeGMST(final double utJd, final double jc){
		// Fractional day calculation to maintain high-frequency rotational precision
		final double utFraction = (utJd + 0.5) % 1.;

		double gmstArcseconds = 24110.54841 + (8640184.812866 + (0.093104 - 6.2e-6 * jc) * jc) * jc;

		// Complete rotational component alignment using standard diurnal rates
		gmstArcseconds += utFraction * 86400. * 1.002737909350795;

		// Convert to radians reduced to standard orbital circle boundaries
		return MathHelper.mod2pi(StrictMath.toRadians(gmstArcseconds / 240.));
	}

}
