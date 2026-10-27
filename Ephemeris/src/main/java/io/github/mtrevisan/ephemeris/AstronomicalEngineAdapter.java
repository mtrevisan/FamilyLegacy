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

import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.SunlightPhase;
import io.github.mtrevisan.calendars.astronomy.Zenith;
import io.github.mtrevisan.calendars.astronomy.ZenithInterface;
import io.github.mtrevisan.ephemeris.engine.MoonHorizonEvents;
import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SolarHorizonEvents;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinates;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;

import java.time.Instant;
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
	// The solar longitude event nearest to a guess is at most half a tropical year away from it [day]
	private static final double HALF_TROPICAL_YEAR = 365.242189 / 2.;

	// Julian Date of 1970-01-01T00:00Z
	private static final double UNIX_EPOCH_JD = 2_440_587.5;


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
	 * Computes the apparent (geocentric) ecliptic longitude of the Moon for a given Universal Time instant.
	 * Uses the ELP2000-82B lunar theory and IAU 2000A nutation.
	 *
	 * @param jdUT the Julian Date in Universal Time (UT/UTC) scale
	 * @return the apparent lunar longitude in radians, reduced to the interval [0, 2π)
	 */
	@Override
	public double calculateApparentMoonLongitude(final double jdUT){
		final double ttJc = getTerrestrialCenturies(jdUT);
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final EclipticCoordinates moonOfDate = MoonPosition.getInstanceLLR()
			.eclipticOfDate(ttJc);
		return MathHelper.mod2pi(MoonPosition.computeApparentMoonLongitude(moonOfDate, nutation.getDeltaPsi()));
	}


	/**
	 * Computes the exact Julian Date (Universal Time) of a specific Solar Term or Season.
	 * Optimistically bypasses linear scans by attempting immediate 3-point Meeus interpolation.
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @param targetLongitude the targeted solar longitude step [rad] (e.g., 0 for Spring Equinox, π/6 for next term)
	 * @return the exact Julian Date UT when the Sun reaches the target angle, nearest to the guess (within half a year)
	 */
	@Override
	public double findSolarLongitudeEvent(final double approximateJdUT, final double targetLongitude){
		final DoubleUnaryOperator function = jdUT -> evaluateSolarLongitudeFunction(jdUT, targetLongitude);

		// 1. Optimistic Step: the function is smooth for months around the root, so a secant iteration seeded with the
		//	mean solar motion converges in a handful of evaluations
		final double root = solveSecant(approximateJdUT, function.applyAsDouble(approximateJdUT), MEAN_SOLAR_RATE,
			function);
		if(StrictMath.abs(root - approximateJdUT) <= HALF_TROPICAL_YEAR)
			return root;

		// 2. Fallback Step: Execute robust sequential scanning if the secant iteration fails
		// Scan the whole year around the guess
		final double dayStep = 5.;
		double x1 = approximateJdUT - HALF_TROPICAL_YEAR;
		double f1 = evaluateSolarLongitudeFunction(x1, targetLongitude);

		// Scan forward in steps to locate an interval with a sign change
		final int steps = (int)StrictMath.ceil(2. * HALF_TROPICAL_YEAR / dayStep);
		for(int i = 0; i < steps; i ++){
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


	/**
	 * Computes the horizon events (rise, upper transit, set) of a specific body, for a civil day and location.
	 * <p>
	 * For the Sun, rise and set are the crossings of the altitude given by the zenith (the upper limb on the horizon with
	 * standard refraction for {@link Zenith#OFFICIAL}, the center of the Sun below the horizon for the twilights). For the
	 * Moon, they are the crossings of the upper limb with the horizon (with refraction and topocentric parallax), and the
	 * zenith is ignored.
	 * </p>
	 *
	 * @param body         the targeted celestial body (SUN or MOON)
	 * @param midnightUtJd the Julian Date representing local civil midnight converted to UT
	 * @param latitude     the observer's geodetic latitude in degrees (North positive)
	 * @param longitude    the observer's geodetic longitude in degrees (East positive)
	 * @param zenith       the solar zenith angle definition configuration (e.g. OFFICIAL, CIVIL)
	 * @return a {@link SunlightPhase.RegularDay} with the events in UTC (an event that does not happen during the day,
	 * 	as for the Moon about once a month, is {@code null}), or {@link SunlightPhase.AlwaysDay}/{@link SunlightPhase.AlwaysNight}
	 * 	if the body neither rises nor sets during the day
	 */
	@Override
	public SunlightPhase calculateHorizonEvent(final CelestialBody body, final double midnightUtJd,
			final double latitude, final double longitude, final ZenithInterface zenith){
		final double rise;
		final double transit;
		final double set;
		final boolean aboveHorizon;
		switch(body){
			case SUN -> {
				final double targetAltitude = (zenith == Zenith.OFFICIAL
					? SolarHorizonEvents.STANDARD_SUNSET_ALTITUDE
					: zenith.getElevation());
				final SolarHorizonEvents.SolarDay day = SolarHorizonEvents.calculateEvents(midnightUtJd, latitude,
					longitude, targetAltitude, AstronomicalEngineAdapter::getTerrestrialCenturies);
				rise = day.rise();
				transit = day.transit();
				set = day.set();
				aboveHorizon = (day.transitAltitude() > targetAltitude);
			}
			case MOON -> {
				final MoonHorizonEvents.LunarDay day = MoonHorizonEvents.calculateEvents(midnightUtJd, latitude, longitude,
					AstronomicalEngineAdapter::getTerrestrialCenturies);
				rise = day.rise();
				transit = day.transit();
				set = day.set();
				aboveHorizon = (day.transitHeight() > 0.);
			}
			default -> throw new IllegalArgumentException("Unsupported celestial body: " + body);
		}

		final ZonedDateTime transitTime = toZonedDateTime(transit);
		// Polar phenomenon: the body never crosses the horizon during the day
		if(Double.isNaN(rise) && Double.isNaN(set))
			return (aboveHorizon
				? new SunlightPhase.AlwaysDay(transitTime)
				: new SunlightPhase.AlwaysNight(transitTime));

		return new SunlightPhase.RegularDay(toZonedDateTime(rise), transitTime, toZonedDateTime(set));
	}

	/**
	 * @return the UTC date-time of a Julian Date (UT), or {@code null} if it is {@code NaN}
	 */
	private static ZonedDateTime toZonedDateTime(final double jdUT){
		if(Double.isNaN(jdUT))
			return null;

		final long epochMillis = StrictMath.round((jdUT - UNIX_EPOCH_JD) * JulianDate.MILLISECONDS_PER_DAY);
		return Instant.ofEpochMilli(epochMillis)
			.atZone(ZoneOffset.UTC);
	}

	private static double getTerrestrialCenturies(final double jdUT){
		// TT = UT + ΔT
		final double deltaTSeconds = DeltaT.deltaTSecondsFromJd(jdUT);
		return JulianDate.centuryJ2000Of(jdUT + deltaTSeconds / JulianDate.SECONDS_PER_DAY);
	}


//	/**
//	 * Calculator utility to handle medieval time conversions based on astronomical data.
//	 */
//	public final class MedievalTimeCalculator {
//
//		/**
//		 * Computes the exact UTC instant corresponding to "1 hour after sunset" as referenced in medieval documents.
//		 *
//		 * @param engine        The AstronomicalEngineAdapter instance
//		 * @param midnightUtJd  The Julian Date representing local civil midnight converted to UT
//		 * @param latitude      The observer's geodetic latitude in degrees (North positive)
//		 * @param longitude     The observer's geodetic longitude in degrees (East positive)
//		 * @param isEqualHours  true if assuming equal hours (clock time), false for unequal hours (seasonal/temporal)
//		 * @return The ZonedDateTime in UTC representing 1 hour after sunset
//		 */
//		public static ZonedDateTime calculateOneHourAfterSunset(final AstronomicalEngineAdapter engine,
//				final double midnightUtJd, final double latitude, final double longitude, final boolean isEqualHours,
//				final double hours){
//			// 1. Compute the sunset of the current day (Apparent/True Solar Time)
//			SunlightPhase currentDayPhase = engine.calculateHorizonEvent(
//				CelestialBody.SUN, midnightUtJd, latitude, longitude, Zenith.OFFICIAL);
//
//			if(!(currentDayPhase instanceof SunlightPhase.RegularDay regularDay))
//				throw new IllegalStateException("Cannot compute sunset: polar phenomenon in progress.");
//
//			ZonedDateTime sunsetUtc = regularDay.set();
//			if(sunsetUtc == null)
//				throw new IllegalStateException("The sun does not set on this date at the specified coordinates.");
//
//			// 2. Compute the hourly component based on the medieval time system
//			if(isEqualHours)
//				// Equal Hours: Add exactly 60 standard modern minutes
//				return sunsetUtc.plusHours(hours);
//			else{
//				// Unequal Hours: Calculate the night duration (from current sunset to next day's sunrise)
//				// Compute the sunrise for the next day by adding 1.0 day to the starting Julian Date
//				double nextDayMidnightUtJd = midnightUtJd + hours;
//				SunlightPhase nextDayPhase = engine.calculateHorizonEvent(
//					CelestialBody.SUN, nextDayMidnightUtJd, latitude, longitude, Zenith.OFFICIAL);
//
//				if(!(nextDayPhase instanceof SunlightPhase.RegularDay nextRegularDay))
//					throw new IllegalStateException("Cannot determine the sunrise for the following day.");
//
//				ZonedDateTime nextSunriseUtc = nextRegularDay.rise();
//				if(nextSunriseUtc == null)
//					throw new IllegalStateException("The sun does not rise on the following day at the specified coordinates.");
//
//				// Calculate the total duration of the night in milliseconds
//				long nightDurationMillis = ChronoUnit.MILLIS.between(sunsetUtc, nextSunriseUtc);
//
//				// A single medieval night hour is defined as exactly 1/12th of the total night duration
//				long medievalHourMillis = nightDurationMillis / 12;
//
//				// Add 1 medieval hour to the current sunset timestamp
//				return sunsetUtc.plus(medievalHourMillis, ChronoUnit.MILLIS);
//			}
//		}
//	}

}
