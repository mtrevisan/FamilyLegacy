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
package io.github.mtrevisan.ephemeris.engine.coordinates;

import io.github.mtrevisan.ephemeris.helpers.MathHelper;


public class Converter{

	/* ======================================================================
	 *                          Precession angles
	 * ====================================================================== */

	/**
	 * Polynomial coefficients of the precession angle {@code p}, from the
	 * ecliptic of date to the ecliptic and equinox of J2000. Units are
	 * radians.
	 */
	private static final double[] P_COEFFS = {
		0.,
		0.10180391e-4,
		0.47020439e-6,
		-0.5417367e-9,
		-0.2507948e-11,
		0.463486e-14
	};

	/**
	 * Polynomial coefficients of the precession angle {@code q}, companion
	 * to {@link #P_COEFFS} in the rotation to the J2000 frame.
	 */
	private static final double[] Q_COEFFS = {
		0.,
		-0.113469002e-3,
		0.12372674e-6,
		0.1265417e-8,
		-0.1371808e-11,
		-0.320334e-14
	};

	/**
	 * Polynomial coefficients of the general precession in longitude {@code p_A} [arcsec], IAU 2006 (Capitaine et al.
	 * 2003, P03; IERS Conventions 2010, eq. 5.39).
	 */
	private static final double[] GENERAL_PRECESSION_COEFFS = {
		0.,
		5028.796195,
		1.1054348,
		0.00007964,
		-0.000023857,
		-0.0000000383
	};

	private static final double ARCSEC_TO_RADIANS = StrictMath.PI / 648_000.;


	/**
	 * General precession in longitude {@code p_A}: the angle along the ecliptic of date from the departure point (the
	 * non-rotating origin moving with the ecliptic, used by the {@code P}, {@code Q} rotation) to the mean equinox of
	 * date.
	 *
	 * @param jce Julian Ephemeris Century of Terrestrial Time from J2000.0
	 * @return the general precession in longitude [rad]
	 */
	public static double generalPrecessionInLongitude(final double jce){
		return MathHelper.polynomial(jce, GENERAL_PRECESSION_COEFFS) * ARCSEC_TO_RADIANS;
	}


	/**
	 * Converts geocentric rectangular J2000.0 coordinates into rectangular
	 * coordinates referred to the mean ecliptic and equinox of date.
	 * <p>
	 * This method performs the forward transformation, rotating vectors from the
	 * <b>inertial mean ecliptic and dynamical equinox J2000.0</b> to the
	 * <b>mean ecliptic and equinox of date</b>. It applies the transpose of Chapront
	 * &amp; Francou's (2003) matrix to invert the rotation direction (bringing the
	 * vector onto the ecliptic of date, with longitudes counted from the departure
	 * point), then a rotation about the ecliptic pole by the general precession in
	 * longitude, to count longitudes from the mean equinox of date.
	 * </p>
	 *
	 * @param jce  Julian Ephemeris Century of Barycentric Dynamical Time (TDB) from J2000.0
	 * @param xJ2000 rectangular X coordinate in J2000.0 frame
	 * @param yJ2000 rectangular Y coordinate in J2000.0 frame
	 * @param zJ2000 rectangular Z coordinate in J2000.0 frame
	 * @return a double array containing the rectangular coordinates [x, y, z] of date
	 */
	public static double[] toRectangularFromJ2000ToDate(final double jce, final double xJ2000, final double yJ2000,
			final double zJ2000) {
		// Precession angles from the ecliptic of date to J2000
		final double p = MathHelper.polynomial(jce, P_COEFFS);
		final double q = MathHelper.polynomial(jce, Q_COEFFS);

		// Rotation matrix elements from Chapront & Francou (2003), eq. 26.
		final double oneMinus2P2 = 1. - 2. * p * p;
		final double oneMinus2Q2 = 1. - 2. * q * q;
		final double twoPQ = 2. * p * q;
		final double sqrt = StrictMath.sqrt(1. - p * p - q * q);
		final double twoPSqrt = 2. * p * sqrt;
		final double twoQSqrt = 2. * q * sqrt;

		// Transpose Matrix Application (Inverting the row/column layout to reverse the rotation)
		final double x = oneMinus2P2 * xJ2000 + twoPQ * yJ2000 - twoPSqrt * zJ2000;
		final double y = twoPQ * xJ2000 + oneMinus2Q2 * yJ2000 + twoQSqrt * zJ2000;
		final double z = twoPSqrt * xJ2000 - twoQSqrt * yJ2000 + (oneMinus2P2 - 2. * q * q) * zJ2000;

		// Rotation about the ecliptic pole from the departure point to the mean equinox of date
		final double precession = generalPrecessionInLongitude(jce);
		final double cosPrecession = StrictMath.cos(precession);
		final double sinPrecession = StrictMath.sin(precession);
		return new double[]{
			x * cosPrecession - y * sinPrecession,
			x * sinPrecession + y * cosPrecession,
			z
		};
	}

}
