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
package io.github.mtrevisan.ephemeris;

import io.github.mtrevisan.ephemeris.engine.MoonHorizonEvents;
import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SolarHorizonEvents;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.familylegacy.services.SunlightPhase;
import io.github.mtrevisan.familylegacy.services.Zenith;
import io.github.mtrevisan.familylegacy.services.ZenithInterface;
import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.DoubleUnaryOperator;


/**
 * Astronomical engine implementation strictly using VSOP2013 theory for solar position
 * and ELP2000-82B for lunar position.
 */
public final class AstronomicalEngineAdapter implements EphemerisEngine{

	// Precision target for root-finding (~0.08 seconds of day)
	private static final double ACCURACY_THRESHOLD = 1.e-6;
	private static final int MAX_ITERATIONS = 50;

	// Mean angular rates used to seed the secant iterations [rad/day]
	private static final double MEAN_ELONGATION_RATE = 2. * StrictMath.PI / 29.530588853;
	private static final double MEAN_SOLAR_RATE = 2. * StrictMath.PI / 365.242189;
	private static final int MAX_SECANT_ITERATIONS = 10;
	// A secant root farther than this from its starting guess is rejected in favour of the bracketing scan [day]
	private static final double MAX_SECANT_DISTANCE = 60.;


	@Override
	public boolean isAvailable(){
		return true;
	}

	/**
	 * Computes the exact Julian Date (Universal Time) of the New Moon closest to a target date.
	 *
	 * @param fromJd the starting guess in Julian Date UT (e.g., mid-month)
	 * @return the exact Julian Date UT of the conjunction
	 */
	@Override
	public double findNextNewMoon(final double fromJd){
		double x1 = fromJd;
		double f1 = evaluateNewMoonFunction(x1);

		// 1. Optimistic Step: extrapolate the elongation still to be covered with the mean synodic motion, then refine
		//	with a secant iteration (the function is smooth around the conjunction, the jump is at full moon)
		final double remainingElongation = (f1 < 0.? -f1: 2. * StrictMath.PI - f1);
		final double guess = fromJd + remainingElongation / MEAN_ELONGATION_RATE;
		final double root = solveSecant(guess, evaluateNewMoonFunction(guess), MEAN_ELONGATION_RATE,
			AstronomicalEngineAdapter::evaluateNewMoonFunction);
		if(root >= fromJd - ACCURACY_THRESHOLD && root - guess <= MAX_SECANT_DISTANCE
				&& guess - root <= MAX_SECANT_DISTANCE)
			return root;

		// 2. Fallback Step: scan forward day by day to bracket the conjunction
		final double step = 1.;
		for(int i = 0; i < 40; i ++){
			final double x2 = x1 + step;
			final double f2 = evaluateNewMoonFunction(x2);

			//the function crosses zero upward (negative -> non-negative) at conjunction,
			//while the opposite jump (+π -> -π) is the discontinuity at full moon
			if(f1 < 0. && f2 >= 0.)
				return IllinoisSolver.solve(x1, x2, ACCURACY_THRESHOLD, MAX_ITERATIONS,
					AstronomicalEngineAdapter::evaluateNewMoonFunction);

			x1 = x2;
			f1 = f2;
		}

		throw new IllegalArgumentException("Unable to find the next new moon starting from JD: " + fromJd);
	}

	private static double evaluateNewMoonFunction(final double jdUT){
		final double ttJc = getTerrestrialCenturies(jdUT);
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		// the Moon is computed once, and shared by the Earth-Moon barycenter correction of the Sun and the Moon longitude
		final EclipticCoordinates moonOfDate = MoonPosition.getInstanceLLR()
			.eclipticOfDate(ttJc);
		final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi,
			MoonPosition.rectangular(ttJc, moonOfDate));

		final double moonLng = MoonPosition.computeApparentMoonLongitude(moonOfDate, deltaPsi);

		return MathHelper.mod2pi(moonLng - sunLng + StrictMath.PI) - StrictMath.PI;
	}

	/**
	 * Computes the exact Julian Date (Universal Time) of a specific Solar Term or Season.
	 * Optimistically bypasses linear scans by attempting immediate 3-point Meeus interpolation.
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @param targetLongitude the targeted solar longitude step [rad] (e.g., 0 for Spring Equinox, π/6 for next term)
	 * @return the exact Julian Date UT when the Sun reaches the target angle
	 */
	@Override
	public double findSolarLongitudeEvent(final double approximateJdUT, final double targetLongitude){
		final DoubleUnaryOperator function = jdUT -> evaluateSolarLongitudeFunction(jdUT, targetLongitude);

		// 1. Optimistic Step: the function is smooth for months around the root, so a secant iteration seeded with the
		//	mean solar motion converges in a handful of evaluations
		final double root = solveSecant(approximateJdUT, function.applyAsDouble(approximateJdUT), MEAN_SOLAR_RATE,
			function);
		if(StrictMath.abs(root - approximateJdUT) <= MAX_SECANT_DISTANCE)
			return root;

		// 2. Fallback Step: Execute robust sequential scanning if the secant iteration fails
		// Widen fallback bracket backward to catch missed targets
		final double dayStep = 5.;
		double x1 = approximateJdUT - 15.;
		double f1 = evaluateSolarLongitudeFunction(x1, targetLongitude);

		// Scan forward in steps to locate an interval with a sign change
		for(int i = 0; i < 12; i ++){
			final double x2 = x1 + dayStep;
			final double f2 = evaluateSolarLongitudeFunction(x2, targetLongitude);

			if(f1 < 0. && f2 >= 0.)
				return IllinoisSolver.solve(x1, x2, ACCURACY_THRESHOLD, MAX_ITERATIONS, function);

			x1 = x2;
			f1 = f2;
		}

		throw new IllegalArgumentException("Unable to find the solar longitude event for approximate JD UT "
			+ approximateJdUT + " and target angle: " + targetLongitude);
	}

	/**
	 * Finds a root of a smooth function with the secant method, using the expected mean slope for the first step.
	 *
	 * @param x0        the starting guess
	 * @param y0        the function value at {@code x0}
	 * @param meanSlope the expected mean slope of the function [rad/day]
	 * @param function  the function
	 * @return the root, or {@code NaN} if the iteration does not converge
	 */
	private static double solveSecant(double x0, double y0, final double meanSlope, final DoubleUnaryOperator function){
		double x1 = x0 - y0 / meanSlope;
		for(int i = 0; i < MAX_SECANT_ITERATIONS; i ++){
			final double y1 = function.applyAsDouble(x1);
			if(y1 == y0)
				return (y1 == 0.? x1: Double.NaN);

			final double x2 = x1 - y1 * (x1 - x0) / (y1 - y0);
			if(StrictMath.abs(x2 - x1) < ACCURACY_THRESHOLD)
				return x2;

			x0 = x1;
			y0 = y1;
			x1 = x2;
		}
		return Double.NaN;
	}

	private double evaluateSolarLongitudeFunction(final double jdUT, final double targetLongitude){
		final double sunLng = calculateApparentSunLongitude(jdUT);
		return MathHelper.mod2pi(sunLng - targetLongitude + StrictMath.PI) - StrictMath.PI;
	}

	/**
	 * Computes the apparent ecliptic longitude of the Sun for a given Universal Time instant.
	 * Accounts for VSOP2013 planetary perturbations, annual aberration, and IAU 2000A nutation.
	 *
	 * @param jdUT the Julian Date in Universal Time (UT/UTC) scale
	 * @return the apparent solar longitude in radians, reduced to the interval [0, 2π)
	 */
	@Override
	public double calculateApparentSunLongitude(final double jdUT){
		final double ttJc = getTerrestrialCenturies(jdUT);
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		return SunPosition.apparentSunLongitude(ttJc, deltaPsi);
	}


	// TODO add test
	/**
	 * Computes the exact Julian Date (UT) of a horizon crossing event for a specific body, day and location.
	 * Consolidates parallax corrections, refraction, and dynamical semidiameter variations polimorphically.
	 *
	 * @param body         the targeted celestial body (SUN or MOON)
	 * @param midnightUtJd the Julian Date representing local civil midnight converted to UT (ending in .5)
	 * @param latitude     the observer's geodetic latitude in degrees (North positive)
	 * @param longitude    the observer's geodetic longitude in degrees (East positive)
	 * @param zenith       the solar zenith angle definition configuration (e.g. OFFICIAL, CIVIL)
	 * @return a fully resolved, localized structural instance of {@link SunlightPhase}
	 */
	@Override
	public SunlightPhase calculateHorizonEvent(final CelestialBody body, final double midnightUtJd,
			final double latitude, final double longitude, final ZenithInterface zenith){
		// 1. Establish the standardized temporal coordinate context (UTC fallback baseline)
		final ZoneId utcZone = ZoneOffset.UTC;
		final ZonedDateTime baselineMidnightZoned = JulianDate.dateTimeOf(midnightUtJd + 0.5)
			.atZone(utcZone);

		// 2. Calculate the precise geocentric solar transit time (Solar Noon) as our structural axis symmetry center
		final double transitJdUt = midnightUtJd + 0.5 - longitude / 360.;
		final ZonedDateTime transitZoned = addFractionOfDay(baselineMidnightZoned, transitJdUt - midnightUtJd);

		// 3. Delegate the downward horizon event (Sunset/Moonset) to the specialized three-point solvers
		final double targetSunsetJdUt = switch(body){
			case SUN -> SolarHorizonEvents.calculateSunset(midnightUtJd, latitude, longitude,
				AstronomicalEngineAdapter::getTerrestrialCenturies);

			case MOON -> MoonHorizonEvents.calculateMoonset(midnightUtJd, latitude, longitude,
				AstronomicalEngineAdapter::getTerrestrialCenturies);
		};

		// 4. Polar phenomenon check: If the engine returns NaN, the celestial body never crosses the horizon line
		if(Double.isNaN(targetSunsetJdUt)){
			final double currentSunLongitude = calculateApparentSunLongitude(transitJdUt);
			final boolean isSummerSolsticeHemisphere
				= (latitude > 0. && currentSunLongitude > 0. && currentSunLongitude < StrictMath.PI)
				|| (latitude < 0. && (currentSunLongitude > StrictMath.PI || currentSunLongitude < 0.));

			return isSummerSolsticeHemisphere ? new SunlightPhase.AlwaysDay(transitZoned) : new SunlightPhase.AlwaysNight(transitZoned);
		}

		// 5. MAP THE SYMMETRIC HOUR ANGLES FOR EXACT SUNRISE
		// The coarse geometric hour angle is perfectly symmetric relative to the solar noon transit line.
		// We mirror the localized sunset fraction backward across the transit axis to anchor our exact sunrise window.
		final double sunsetFractionFromTransit = targetSunsetJdUt - transitJdUt;
		final double approximateSunriseJdUt = transitJdUt - sunsetFractionFromTransit;

		// 6. Execute high-precision Three-Point Parabolic Interpolation specifically for the morning crossing
		final double hourInJd = 1. / 24.;
		final double t1 = approximateSunriseJdUt - hourInJd;
		final double t2 = approximateSunriseJdUt;
		final double t3 = approximateSunriseJdUt + hourInJd;

		final double latRad = StrictMath.toRadians(latitude);

		// Fetch the raw topocentric altitudes across the morning tracking window
		final double y1 = computeSolarAltitudeForSunrise(t1, latRad, longitude);
		final double y2 = computeSolarAltitudeForSunrise(t2, latRad, longitude);
		final double y3 = computeSolarAltitudeForSunrise(t3, latRad, longitude);

		// Standard structural altitude for solar disk upper limb: -50 arcminutes (-0.8333333°).
		// For custom twilights (CIVIL, NAUTICAL), we extract the target elevation dynamically from your Zenith token.
		final double targetAltitude = (zenith == Zenith.OFFICIAL
			? StrictMath.toRadians(-50. / 60.)
			: zenith.getElevation());

		// Calculate parabolic interpolation coefficients across the morning tracking window
		final double a = (y1 + y3) / 2. - y2;
		final double b = (y3 - y1) / 2.;
		final double c = y2 - targetAltitude;

		double sunriseCorrectionFactor = 0.;
		final double discriminant = b * b - 4. * a * c;
		if(discriminant >= 0. && StrictMath.abs(a) >= 1e-12){
			sunriseCorrectionFactor = (-b - StrictMath.sqrt(discriminant)) / (2. * a);
			if(StrictMath.abs(sunriseCorrectionFactor) > 1.)
				sunriseCorrectionFactor = (-b + StrictMath.sqrt(discriminant)) / (2. * a);
		}

		// Calculate the exact final structural sunrise Julian Date
		final double exactSunriseJdUt = approximateSunriseJdUt + (sunriseCorrectionFactor * hourInJd);

		// 7. Map the continuous absolute timelines into the polymorphic RegularDay container
		final ZonedDateTime exactSunriseZoned = addFractionOfDay(baselineMidnightZoned,
			exactSunriseJdUt - midnightUtJd);
		final ZonedDateTime exactSunsetZoned = addFractionOfDay(baselineMidnightZoned,
			targetSunsetJdUt - midnightUtJd);

		return new SunlightPhase.RegularDay(exactSunriseZoned, transitZoned, exactSunsetZoned);
	}

	/**
	 * Computes the geocentric/topocentric apparent local solar altitude tracking point for sunrise interpolation.
	 */
	private static double computeSolarAltitudeForSunrise(final double utJd, final double latRad, final double longitude){
		final double ttJc = getTerrestrialCenturies(utJd);
		final double jc = JulianDate.centuryJ2000Of(utJd);

		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double lambdaApparent = SunPosition.apparentSunLongitude(ttJc, nutation.getDeltaPsi());

		final double epsilon0 = StrictMath.toRadians(23.43929111 - 46.836769 * ttJc / 3600.);
		final double trueObliquity = epsilon0 + nutation.getDeltaEpsilon();

		final double rightAscension = MathHelper.mod2pi(StrictMath.atan2(
			StrictMath.sin(lambdaApparent) * StrictMath.cos(trueObliquity), StrictMath.cos(lambdaApparent)));
		final double declination = StrictMath.asin(StrictMath.sin(trueObliquity) * StrictMath.sin(lambdaApparent));

		// Derive GAST and Local Sidereal Time components
		final double utFraction = (utJd + 0.5) % 1.;
		double gmstArcseconds = 24110.54841 + (8640184.812866 + (0.093104 - 6.2e-6 * jc) * jc) * jc;
		gmstArcseconds += utFraction * 86400.0 * 1.002737909350795;
		final double gmst = MathHelper.mod2pi(StrictMath.toRadians(gmstArcseconds / 240.));
		final double gast = gmst + nutation.getDeltaPsi() * StrictMath.cos(trueObliquity);

		final double localSiderealTime = gast + StrictMath.toRadians(longitude);
		final double hourAngle = localSiderealTime - rightAscension;

		return StrictMath.asin(StrictMath.sin(latRad) * StrictMath.sin(declination)
			+ StrictMath.cos(latRad) * StrictMath.cos(declination) * StrictMath.cos(hourAngle));
	}

	private static ZonedDateTime addFractionOfDay(final ZonedDateTime date, final double fraction){
		// Secure standard day boundaries to prevent out-of-bounds adjustments
		final long totalMillisOffset = Math.round(fraction * JulianDate.MILLISECONDS_PER_DAY);
		return date.truncatedTo(java.time.temporal.ChronoUnit.DAYS)
			.plus(totalMillisOffset, java.time.temporal.ChronoUnit.MILLIS);
	}


	private static double getTerrestrialCenturies(final double jdUT){
		// TT = UT + ΔT
		final double deltaTSeconds = DeltaT.deltaTSecondsFromJd(jdUT);
		return JulianDate.centuryJ2000Of(jdUT + deltaTSeconds / JulianDate.SECONDS_PER_DAY);
	}

}
