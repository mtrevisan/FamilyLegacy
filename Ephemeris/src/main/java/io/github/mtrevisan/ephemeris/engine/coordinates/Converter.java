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
	 * Converts geocentric spherical ecliptic coordinates from a specific epoch
	 * into rectangular J2000.0 coordinates.
	 * <p>
	 * This method performs the backward transformation, rotating vectors from the
	 * <b>mean ecliptic and equinox of date</b> back to the <b>inertial mean ecliptic
	 * and dynamical equinox J2000.0</b> using Chapront & Francou's (2003) optimized
	 * rotation parameters (p, q).
	 * </p>
	 *
	 * @param jce    Julian Ephemeris Century of Barycentric Dynamical Time (TDB) from J2000.0
	 * @param coords the spherical ecliptic coordinates (longitude, latitude, distance) of date
	 * @return a double array containing the rectangular J2000.0 coordinates [x, y, z]
	 */
	public static double[] toRectangularFromDateToJ2000(final double jce, final EclipticCoordinates coords){
		// Precession angles from the ecliptic of date to J2000
		final double p = MathHelper.polynomial(jce, P_COEFFS);
		final double q = MathHelper.polynomial(jce, Q_COEFFS);

		final double rCosVcosU = coords.getDistance() * StrictMath.cos(coords.getLongitude())
			* StrictMath.cos(coords.getLatitude());
		final double rSinVcosU = coords.getDistance() * StrictMath.sin(coords.getLongitude())
			* StrictMath.cos(coords.getLatitude());
		final double rSinU = coords.getDistance() * StrictMath.sin(coords.getLatitude());

		// Rotation matrix from Chapront & Francou (2003), eq. 26.
		final double oneMinus2P2 = 1. - 2. * p * p;
		final double oneMinus2Q2 = 1. - 2. * q * q;
		final double twoPQ = 2. * p * q;
		final double sqrt = StrictMath.sqrt(1. - p * p - q * q);
		final double twoPSqrt = 2. * p * sqrt;
		final double twoQSqrt = 2. * q * sqrt;

		// Original matrix layout
		final double x = oneMinus2P2 * rCosVcosU + twoPQ * rSinVcosU + twoPSqrt * rSinU;
		final double y = twoPQ * rCosVcosU + oneMinus2Q2 * rSinVcosU - twoQSqrt * rSinU;
		final double z = -twoPSqrt * rCosVcosU + twoQSqrt * rSinVcosU + (oneMinus2P2 - 2. * q * q) * rSinU;

		return new double[]{x, y, z};
	}

	/**
	 * Converts geocentric rectangular J2000.0 coordinates into rectangular
	 * coordinates referred to the mean ecliptic and equinox of date.
	 * <p>
	 * This method performs the forward transformation, rotating vectors from the
	 * <b>inertial mean ecliptic and dynamical equinox J2000.0</b> to the
	 * <b>mean ecliptic and equinox of date</b>. It applies the transpose of Chapront
	 * &amp; Francou's (2003) matrix to invert the rotation direction.
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

		return new double[]{x, y, z};
	}

}
