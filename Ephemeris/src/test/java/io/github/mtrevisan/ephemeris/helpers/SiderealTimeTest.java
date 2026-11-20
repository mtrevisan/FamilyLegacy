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
package io.github.mtrevisan.ephemeris.helpers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class SiderealTimeTest{

	@Test
	void earthRotationAngleAtJ2000(){
		//ERA(J2000.0) = 2π·0.7790572732640 = 280.4606184°
		final double era = StrictMath.toDegrees(SiderealTime.earthRotationAngle(JulianDate.J2000));

		Assertions.assertEquals(280.4606184, era, 1.e-6);
	}

	@Test
	void greenwichMeanSiderealTimeAtJ2000(){
		//GMST at J2000.0 is 18h 41m 50.5484s = 280.46061837°
		final double gmst = StrictMath.toDegrees(SiderealTime.greenwichMeanSiderealTime(JulianDate.J2000, 0.));

		Assertions.assertEquals(280.46061837, gmst, 1.e-4);
	}

	@Test
	void greenwichMeanSiderealTimeMeeusExample(){
		//Meeus, Astronomical Algorithms, example 12.a: 1987-04-10 0h UT, GMST = 13h 10m 46.3668s
		final double jd = 2_446_895.5;
		final double ttJc = JulianDate.centuryJ2000Of(jd + 55. / JulianDate.SECONDS_PER_DAY);
		final double gmstHours = StrictMath.toDegrees(SiderealTime.greenwichMeanSiderealTime(jd, ttJc)) / 15.;

		Assertions.assertEquals(MathHelper.toDegrees(13, 10, 46.3668), gmstHours, 1.e-5);
	}

	@Test
	void apparentAddsEquationOfEquinoxes(){
		final double mean = SiderealTime.greenwichMeanSiderealTime(JulianDate.J2000, 0.);
		final double apparent = SiderealTime.greenwichApparentSiderealTime(JulianDate.J2000, 0., 1.e-4, 0.4090928);

		Assertions.assertEquals(1.e-4 * StrictMath.cos(0.4090928), apparent - mean, 1.e-12);
	}

	@Test
	void resultIsAlwaysInRange(){
		for(double jd = 2_400_000.; jd < 2_500_000.; jd += 1234.567){
			final double angle = SiderealTime.earthRotationAngle(jd);

			Assertions.assertTrue(angle >= 0. && angle < MathHelper.TWO_PI);
		}
	}

}
