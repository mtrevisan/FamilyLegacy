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


class MoonPositionTest{

	@Test
	void test(){
		final double t = JulianDate.centuryJ2000Of(2444269.5);
		final double[] xyz = MoonPosition.getInstanceDE405()
			.rectangular(t);

		Assertions.assertEquals(-186813.08162, xyz[0], 0.000_01);
		Assertions.assertEquals(349310.09818, xyz[1], 0.000_01);
		Assertions.assertEquals(-19003.33833, xyz[2], 0.000_01);
	}

	/**
	 * Geocentric position referred to the mean ecliptic and equinox of J2000, DE405 parameter set ("historical mode"),
	 * computed from the original Chapront &amp; Francou data files.
	 * <p>
	 * Reference: M. van der Sluys, ELP-MPP02 (Python/Fortran port of the authors' code), example for JD 2451545,
	 * mode 1: lon = −2.385534575256455 rad, lat = 0.09024868423130429 rad, dist = 402448.6385830673 km.
	 * </p>
	 *
	 * @see <a href="https://github.com/MarcvdSluys/ELP-MPP02">ELP-MPP02</a>
	 */
	@Test
	void rectangularMatchesReferenceImplementation(){
		final double[] xyz = MoonPosition.getInstanceDE405()
			.rectangular(JulianDate.centuryJ2000Of(2451545.));

		final double distance = StrictMath.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
		Assertions.assertEquals(-2.385534575256455, StrictMath.atan2(xyz[1], xyz[0]), 1.e-9);
		Assertions.assertEquals(0.09024868423130429, StrictMath.asin(xyz[2] / distance), 1.e-9);
		Assertions.assertEquals(402448.6385830673, distance, 0.001);
	}

	/**
	 * Geocentric position referred to the mean ecliptic and equinox of date.
	 * <p>
	 * Reference: J. Meeus, <i>Astronomical Algorithms</i>, 2nd ed., Example 47.a (1992 April 12, 0h TD):
	 * λ = 133.162655°, β = −3.229126°, Δ = 368409.7 km. Meeus uses a truncated ELP-2000/82 with a stated accuracy of
	 * 10" in longitude and 4" in latitude.
	 * </p>
	 */
	@Test
	void eclipticOfDateMatchesMeeus(){
		final EclipticCoordinates coord = MoonPosition.getInstanceLLR()
			.eclipticOfDate(JulianDate.centuryJ2000Of(2448724.5));

		Assertions.assertEquals(0., angularDifferenceArcsec(coord.getLongitude(), StrictMath.toRadians(133.162655)), 10.);
		Assertions.assertEquals(0., angularDifferenceArcsec(coord.getLatitude(), StrictMath.toRadians(-3.229126)), 4.);
		Assertions.assertEquals(368409.7, coord.getDistance(), 10.);
	}

	/**
	 * Reference: J. Meeus, <i>Astronomical Algorithms</i>, 2nd ed., Example 47.a: apparent λ = 133.167265°.
	 */
	@Test
	void apparentMoonLongitudeMatchesMeeus(){
		final double jce = JulianDate.centuryJ2000Of(2448724.5);
		final double deltaPsi = NutationCorrections.calculate(jce).getDeltaPsi();

		final double longitude = MoonPosition.computeApparentMoonLongitude(jce, deltaPsi);

		Assertions.assertEquals(0., angularDifferenceArcsec(longitude, StrictMath.toRadians(133.167265)), 10.);
	}

	/**
	 * Apparent geocentric ecliptic longitude of the Moon, referred to the true ecliptic and equinox of date.
	 * <p>
	 * Reference: JPL Horizons (DE441), quantity 31 (ObsEcLon), geocenter, TT, given to 1e-7° (0.36"); the tolerance
	 * covers the rounding, the different nutation model, and the light-time (~1.3 s, ~0.7"), not applied here.
	 * </p>
	 *
	 * @see <a href="https://ssd.jpl.nasa.gov/horizons/">JPL Horizons</a>
	 */
	@Test
	void apparentMoonLongitudeMatchesJplHorizons(){
		final double[][] references = {
			// JD (TT), apparent longitude [°]
			{2448724.5, 133.1667103},
			{2451545.0, 223.3148557},
			{2460482.5, 257.0701701}
		};
		for(final double[] reference : references){
			final double jce = JulianDate.centuryJ2000Of(reference[0]);
			final double deltaPsi = NutationCorrections.calculate(jce).getDeltaPsi();

			final double longitude = MoonPosition.computeApparentMoonLongitude(jce, deltaPsi);

			Assertions.assertEquals(0., angularDifferenceArcsec(longitude, StrictMath.toRadians(reference[1])), 2.,
				"JD " + reference[0]);
		}
	}

	@Test
	void overloadsWithPrecomputedPositionAreConsistent(){
		final double jce = JulianDate.centuryJ2000Of(2460482.5);
		final double deltaPsi = NutationCorrections.calculate(jce).getDeltaPsi();
		final MoonPosition moon = MoonPosition.getInstanceLLR();
		final EclipticCoordinates ofDate = moon.eclipticOfDate(jce);

		Assertions.assertArrayEquals(moon.rectangular(jce), MoonPosition.rectangular(jce, ofDate));
		Assertions.assertEquals(MoonPosition.computeApparentMoonLongitude(jce, deltaPsi),
			MoonPosition.computeApparentMoonLongitude(ofDate, deltaPsi));
	}


	private static final double ARCSEC_PER_RADIAN = 180. * 3600. / StrictMath.PI;

	private static double angularDifferenceArcsec(final double angle, final double reference){
		return StrictMath.IEEEremainder(angle - reference, 2. * StrictMath.PI) * ARCSEC_PER_RADIAN;
	}

}