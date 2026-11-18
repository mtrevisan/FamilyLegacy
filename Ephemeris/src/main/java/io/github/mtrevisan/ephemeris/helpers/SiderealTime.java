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


/**
 * Greenwich sidereal time, IAU 2006 (IERS Conventions 2010, chapter 5; SOFA {@code iauEra00}, {@code iauGmst06}).
 */
public final class SiderealTime{

	/**
	 * Polynomial part of GMST, IERS Conventions 2010, eq. 5.32 [arcsec], in Julian centuries of TT from J2000.0.
	 */
	private static final double[] GMST_COEFFS = {
		0.014506,
		4612.156534,
		1.3915817,
		-0.00000044,
		-0.000029956,
		-0.0000000368
	};

	private static final double ARCSEC_TO_RADIANS = StrictMath.PI / 648_000.;


	private SiderealTime(){}


	/**
	 * Earth rotation angle, IERS Conventions 2010, eq. 5.15.
	 *
	 * @param jdUT the Julian Date in UT1
	 * @return the Earth rotation angle [rad], in the interval [0, 2π)
	 */
	public static double earthRotationAngle(final double jdUT){
		final double t = jdUT - JulianDate.J2000;
		// the integer part of the days contributes whole turns: keep only the fraction to preserve precision
		final double fraction = t - StrictMath.floor(t);
		return MathHelper.mod2pi(2. * StrictMath.PI * (fraction + 0.7790572732640 + 0.00273781191135448 * t));
	}

	/**
	 * Greenwich mean sidereal time, IERS Conventions 2010, eq. 5.32.
	 *
	 * @param jdUT the Julian Date in UT1
	 * @param ttJc Julian centuries of TT from J2000.0, at the same instant
	 * @return the Greenwich mean sidereal time [rad], in the interval [0, 2π)
	 */
	public static double greenwichMeanSiderealTime(final double jdUT, final double ttJc){
		return MathHelper.mod2pi(earthRotationAngle(jdUT) + MathHelper.polynomial(ttJc, GMST_COEFFS) * ARCSEC_TO_RADIANS);
	}

	/**
	 * Greenwich apparent sidereal time, with the equation of the equinoxes {@code Δψ·cos(ε)} (the complementary terms,
	 * below 3 mas, are neglected).
	 *
	 * @param jdUT          the Julian Date in UT1
	 * @param ttJc          Julian centuries of TT from J2000.0, at the same instant
	 * @param deltaPsi      nutation in longitude [rad]
	 * @param trueObliquity true obliquity of the ecliptic [rad]
	 * @return the Greenwich apparent sidereal time [rad], in the interval [0, 2π)
	 */
	public static double greenwichApparentSiderealTime(final double jdUT, final double ttJc, final double deltaPsi,
			final double trueObliquity){
		return MathHelper.mod2pi(greenwichMeanSiderealTime(jdUT, ttJc) + deltaPsi * StrictMath.cos(trueObliquity));
	}

}
