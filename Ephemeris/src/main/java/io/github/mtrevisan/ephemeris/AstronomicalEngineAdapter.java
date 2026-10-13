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

import io.github.mtrevisan.ephemeris.engine.MoonPosition;
import io.github.mtrevisan.ephemeris.engine.NutationCorrections;
import io.github.mtrevisan.ephemeris.engine.SunPosition;
import io.github.mtrevisan.ephemeris.helpers.DeltaT;
import io.github.mtrevisan.ephemeris.helpers.IllinoisSolver;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.familylegacy.services.EphemerisEngine;


/**
 * Astronomical engine implementation strictly using VSOP2013 theory for solar position
 * and ELP2000-82B for lunar position.
 */
public final class AstronomicalEngineAdapter implements EphemerisEngine{

	// Precision target for root-finding (~0.08 seconds of day)
	private static final double ACCURACY_THRESHOLD = 1.e-6;
	private static final int MAX_ITERATIONS = 50;


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

		final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);
		final double moonLng = MoonPosition.computeApparentMoonLongitude(ttJc, deltaPsi);

		return MathHelper.mod2pi(moonLng - sunLng + StrictMath.PI) - StrictMath.PI;
	}

	/**
	 * Computes the exact Julian Date (Universal Time) of a specific Solar Term (Jieqi).
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @param targetLongitude the targeted solar longitude step [rad] (e.g., 0 for Spring Equinox, π/6 for next term)
	 * @return the exact Julian Date UT when the Sun reaches the target angle
	 */
	@Override
	public double findSolarLongitudeEvent(final double approximateJdUT, final double targetLongitude){
		double x1 = approximateJdUT;
		double f1 = evaluateSolarLongitudeFunction(x1, targetLongitude);

		// Scan forward in steps to locate an interval with a sign change
		final double step = 5.;
		for(int i = 0; i < 80; i ++){
			final double x2 = x1 + step;
			final double f2 = evaluateSolarLongitudeFunction(x2, targetLongitude);

			if(f1 < 0. && f2 >= 0.)
				return IllinoisSolver.solve(x1, x2, ACCURACY_THRESHOLD, MAX_ITERATIONS,
					jdUT -> evaluateSolarLongitudeFunction(jdUT, targetLongitude));

			x1 = x2;
			f1 = f2;
		}

		throw new IllegalArgumentException("Unable to find the solar longitude event for approximate JD UT "
			+ approximateJdUT + " and target angle: " + targetLongitude);
	}

	private static double evaluateSolarLongitudeFunction(final double jdUT, final double targetLongitude){
		final double ttJc = getTerrestrialCenturies(jdUT);
		final NutationCorrections nutation = NutationCorrections.calculate(ttJc);
		final double deltaPsi = nutation.getDeltaPsi();
		final double sunLng = SunPosition.apparentSunLongitude(ttJc, deltaPsi);

		return MathHelper.mod2pi(sunLng - targetLongitude + StrictMath.PI) - StrictMath.PI;
	}

	private static double getTerrestrialCenturies(final double jdUT){
		final double dt = DeltaT.deltaTSecondsFromJd(jdUT);
		return JulianDate.centuryJ2000Of(jdUT + DeltaT.deltaTDays(dt));
	}

}
