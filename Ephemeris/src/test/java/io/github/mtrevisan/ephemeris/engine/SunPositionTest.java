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
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SunPositionTest{

	@Test
	void test(){
		final double t = JulianDate.millenniumJ2000Of(2444269.5);
		final EclipticCoordinates coord = SunPosition.embTrueHeliocentricPosition(t);

		Assertions.assertEquals(3.182657571886453E-5, coord.getLatitude(), 0.000_000_1);
		Assertions.assertEquals(2.278426072988786, coord.getLongitude(), 0.000_000_1);
		Assertions.assertEquals(0.9851322735478121, coord.getDistance(), 0.000_000_1);
	}

	/**
	 * Control values of the VSOP2013 authors for the Earth-Moon barycenter, heliocentric rectangular coordinates in the
	 * dynamical equinox and ecliptic J2000 [AU], time in TDB.
	 *
	 * @see <a href="https://ftp.imcce.fr/pub/ephem/planets/vsop2013/ephemerides/VSOP2013_ctl.txt">IMCCE, VSOP2013_ctl.txt</a>
	 */
	@Test
	void embMatchesVsop2013ControlValues(){
		final double[][] controlValues = {
			{77432.5, -0.905226137161, 0.410894088359, 0.008767126179},
			{1173182.5, -0.711777362980, 0.683683561463, 0.005903500934},
			{1721057.5, -0.583146874950, 0.793490368407, 0.003650547997},
			{2268932.5, -0.437662445161, 0.880925943295, 0.000970542639},
			{2405730.5, 0.575808919682, -0.836447643940, -0.000224518820},
			{2542528.5, -0.704622827553, 0.689493625407, -0.000348981329},
			{2816124.5, -0.894436593655, 0.422248053256, -0.000680675867},
			{3364010.5, -0.903569304383, 0.401732967883, -0.001227717673}
		};
		for(final double[] controlValue : controlValues){
			final EclipticCoordinates coord = SunPosition.embTrueHeliocentricPosition(
				JulianDate.millenniumJ2000Of(controlValue[0]));

			final double cosLatitude = StrictMath.cos(coord.getLatitude());
			final String message = "JD " + controlValue[0];
			// 1e-10 AU = 15 m
			Assertions.assertEquals(controlValue[1], coord.getDistance() * cosLatitude * StrictMath.cos(coord.getLongitude()),
				1.e-10, message);
			Assertions.assertEquals(controlValue[2], coord.getDistance() * cosLatitude * StrictMath.sin(coord.getLongitude()),
				1.e-10, message);
			Assertions.assertEquals(controlValue[3], coord.getDistance() * StrictMath.sin(coord.getLatitude()),
				1.e-10, message);
		}
	}

	/**
	 * Apparent geocentric ecliptic longitude of the Sun, referred to the true ecliptic and equinox of date.
	 * <p>
	 * Reference: JPL Horizons (DE441), quantity 31 (ObsEcLon), geocenter, TT, given to 1e-7° (0.36"); the tolerance
	 * covers the rounding and the different nutation model (IAU 1980 in Horizons, IAU 2000A here).
	 * </p>
	 *
	 * @see <a href="https://ssd.jpl.nasa.gov/horizons/">JPL Horizons</a>
	 */
	@Test
	void apparentSunLongitudeMatchesJplHorizons(){
		final double[][] references = {
			// JD (TT), apparent longitude [°]
			{2448908.5, 199.9059841},
			{2451545.0, 280.3681519},
			{2460482.5, 90.1244325}
		};
		for(final double[] reference : references){
			final double jce = JulianDate.centuryJ2000Of(reference[0]);
			final double deltaPsi = NutationCorrections.calculate(jce).getDeltaPsi();

			final double longitude = SunPosition.apparentSunLongitude(jce, deltaPsi);

			Assertions.assertEquals(0., angularDifferenceArcsec(longitude, StrictMath.toRadians(reference[1])), 1.,
				"JD " + reference[0]);
		}
	}

	@Test
	void apparentSunLongitudeWithMoonPositionIsConsistent(){
		final double jce = JulianDate.centuryJ2000Of(2460482.5);
		final double deltaPsi = NutationCorrections.calculate(jce).getDeltaPsi();
		final double[] moon = MoonPosition.getInstanceLLR()
			.rectangular(jce);

		Assertions.assertEquals(SunPosition.apparentSunLongitude(jce, deltaPsi),
			SunPosition.apparentSunLongitude(jce, deltaPsi, moon));
	}

	/**
	 * The correction is −κ / R, with κ = 20.49552" the constant of aberration (IAU 1976 System of Astronomical Constants;
	 * Meeus, <i>Astronomical Algorithms</i>, 2nd ed., eq. 25.10).
	 */
	@Test
	void aberrationCorrection(){
		Assertions.assertEquals(-20.49552, SunPosition.aberrationCorrection(1.) * ARCSEC_PER_RADIAN, 1.e-4);
		Assertions.assertEquals(-20.49552 / 0.983, SunPosition.aberrationCorrection(0.983) * ARCSEC_PER_RADIAN, 1.e-4);
	}


	private static final double ARCSEC_PER_RADIAN = 180. * 3600. / StrictMath.PI;

	private static double angularDifferenceArcsec(final double angle, final double reference){
		return StrictMath.IEEEremainder(angle - reference, 2. * StrictMath.PI) * ARCSEC_PER_RADIAN;
	}

}