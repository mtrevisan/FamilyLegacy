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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class NutationCorrectionsTest{

	@Test
	void sofaReference(){
		// SOFA test suite (t_sofa_c.c, t_nut06a): TT = 2400000.5 + 53736.0
		final double jc = (2400000.5 + 53736.0 - 2451545.) / 36525.;
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		// tolerance 1e-10 rad = 0.02 mas (the IERS tables are truncated at 0.1 µas per term)
		Assertions.assertEquals(-0.9630912025820308797e-5, corrections.getDeltaPsi(), 1.e-10);
		Assertions.assertEquals(0.4063238496887249798e-4, corrections.getDeltaEpsilon(), 1.e-10);
	}

	/**
	 * Reference: J. Meeus, <i>Astronomical Algorithms</i>, 2nd ed., Example 22.a (1987 April 10, 0h TD):
	 * Δψ = −3.788", Δε = +9.443". Meeus uses the IAU 1980 theory, which differs from IAU 2000A by some mas.
	 */
	@Test
	void meeusExample(){
		final double jc = JulianDate.centuryJ2000Of(2446895.5);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		final double arcsecPerRadian = 180. * 3600. / StrictMath.PI;
		Assertions.assertEquals(-3.788, corrections.getDeltaPsi() * arcsecPerRadian, 0.02);
		Assertions.assertEquals(9.443, corrections.getDeltaEpsilon() * arcsecPerRadian, 0.02);
	}

	/**
	 * Reference: SOFA test suite (t_sofa_c.c, t_faom03): Ω(t = 0.8) = −5.973618440951302183 rad, here reduced to
	 * [0, 2π).
	 */
	@Test
	void moonLongitudeAscendingNodeMatchesSofa(){
		Assertions.assertEquals(-5.973618440951302183 + 2. * StrictMath.PI,
			NutationCorrections.moonLongitudeAscendingNode(0.8), 1.e-12);
	}

	@Test
	void past(){
		final double jd = JulianDate.of(1950, 1, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(-1.6014287318429332e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(4.035167807923474e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(0.21141530376878137, mlan, 0.000_000_1);
	}

	@Test
	void present(){
		final double jd = JulianDate.of(2026, 10, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(4.007550241476701e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(3.9786410628155436e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(5.719669064760243, mlan, 0.000_000_1);
	}

	@Test
	void future(){
		final double jd = JulianDate.of(2050, 1, 1);
		final double jc = JulianDate.centuryJ2000Of(jd);
		final NutationCorrections corrections = NutationCorrections.calculate(jc);

		Assertions.assertEquals(7.355338029843851e-5, corrections.getDeltaPsi(), 0.000_000_001);
		Assertions.assertEquals(-2.583918113248748e-5, corrections.getDeltaEpsilon(), 0.000_000_001);


		final double mlan = NutationCorrections.moonLongitudeAscendingNode(jc);
		Assertions.assertEquals(4.153481202550509, mlan, 0.000_000_1);
	}

}