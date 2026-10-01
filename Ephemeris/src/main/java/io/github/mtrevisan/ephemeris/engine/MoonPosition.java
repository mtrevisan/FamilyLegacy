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

import io.github.mtrevisan.ephemeris.engine.coordinates.EclipticCoordinate;
import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.readers.ElpCoefficients;
import io.github.mtrevisan.ephemeris.readers.ElpParameters;
import io.github.mtrevisan.ephemeris.readers.ElpSeriesReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * Geocentric lunar position computed from the ELP/MPP02 truncated series.
 *
 * <p>The class is a singleton: the 14 coefficient files are read once and
 * kept in memory. Two parameter sets are available, fitted respectively to
 * the LLR observations and to the JPL DE405/DE406 ephemerides; the default
 * is {@code LLR}, which is the more accurate set for recent centuries.</p>
 *
 * <p><b>Coordinate system.</b> The natural output of the ELP/MPP02 theory
 * is the geocentric ecliptic longitude V, latitude U and distance r
 * referred to the <em>mean ecliptic of date</em>. The
 * {@link #rectangular(double)} method rotates the result to the
 * <em>mean ecliptic and equinox of J2000.0</em>, which is the frame used
 * by VSOP2013 and by the rest of the project.</p>
 *
 * <p><b>Time argument.</b> The series are evaluated at the Julian
 * century of Barycentric Dynamical Time (TDB) from J2000:
 * {@code T = (JD − 2451545.) / 36525}. For civil dates, TDB is
 * approximated by Terrestrial Time, which in turn is obtained from UT by
 * adding ΔT.</p>
 *
 * <p><b>Accuracy.</b> With the full series loaded (no truncation), the
 * agreement with DE405/DE406 is better than 0.03″ in longitude and
 * latitude and 30 m in distance over the interval −3000 to +3000.</p>
 *
 * <p><b>Reference.</b> Chapront &amp; Francou (2003), <i>The lunar theory
 * ELP revisited. Introduction of new planetary perturbations</i>,
 * A&amp;A 404, 735–742.</p>
 */
public final class MoonPosition{

	/* ======================================================================
	 *                          Constants
	 * ====================================================================== */

	/** Arcsecond-to-radian conversion factor. */
	private static final double SEC = StrictMath.PI / 648000.;

	/** Two times π, cached for angle reduction. */
	private static final double TWO_PI = 2. * StrictMath.PI;

	/**
	 * Ratio of the ELP semi-major axis to the DE405 semi-major axis,
	 * applied to the distance series so that the output is in the same
	 * length unit as the JPL ephemerides (km).
	 */
	private static final double RA0 = 384747.961370173 / 384747.980674318;

	/* ======================================================================
	 *                          Argument array layout
	 * ====================================================================== */

	/**
	 * The argument array carries the 13 fundamental arguments of the
	 * ELP/MPP02 theory followed by the Moon mean longitude W1. The layout
	 * matches the order of the multipliers in the perturbation files, so
	 * {@code args[i]} can be used directly with {@code multipliers[i]}
	 * for {@code i} in {@code [0, 12]}.
	 */
	private static final int ARG_D    = 0;   // mean elongation of the Moon from the Sun
	private static final int ARG_F    = 1;   // mean argument of latitude of the Moon
	private static final int ARG_L    = 2;   // mean anomaly of the Moon
	private static final int ARG_LP   = 3;   // mean anomaly of the Sun
	private static final int ARG_ME   = 4;   // Mercury mean longitude
	private static final int ARG_VE   = 5;   // Venus mean longitude
	private static final int ARG_EM   = 6;   // Earth mean longitude
	private static final int ARG_MA   = 7;   // Mars mean longitude
	private static final int ARG_JU   = 8;   // Jupiter mean longitude
	private static final int ARG_SA   = 9;   // Saturn mean longitude
	private static final int ARG_UR   = 10;  // Uranus mean longitude
	private static final int ARG_NE   = 11;  // Neptune mean longitude
	private static final int ARG_ZETA = 12;  // precession argument
	private static final int ARG_W1   = 13;  // Moon mean longitude
	private static final int ARG_COUNT = 14;

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

	/* ======================================================================
	 *                          Fundamental arguments (eqs. 5–22)
	 * ====================================================================== */

	/* W1 — Moon mean longitude */
	private static final double[] W1_COEFFS = {
		MathHelper.toDegrees(218, 18, 59.95571) * JulianDate.SECONDS_PER_HOUR,
		1732559343.73604, -6.8084, 0.006604, -0.00003169
	};
	/* W2 — Sun mean longitude */
	private static final double[] W2_COEFFS = {
		MathHelper.toDegrees(83, 21, 11.67475) * JulianDate.SECONDS_PER_HOUR,
		14643420.3171, -38.2631, -0.045047, 0.00021301
	};
	/* W3 — Mean longitude of the lunar ascending node */
	private static final double[] W3_COEFFS = {
		MathHelper.toDegrees(125, 2, 40.39816) * JulianDate.SECONDS_PER_HOUR,
		-6967919.5383, 6.359, 0.007625, -0.00003586
	};
	/* Ea — Earth mean longitude */
	private static final double[] EA_COEFFS = {
		MathHelper.toDegrees(100, 27, 59.13885) * JulianDate.SECONDS_PER_HOUR,
		129597742.293, -0.0202, 9e-6, 1.5e-7
	};
	/* perigee — Solar perigee */
	private static final double[] PERIGEE_COEFFS = {
		MathHelper.toDegrees(102, 56, 14.45766) * JulianDate.SECONDS_PER_HOUR,
		1161.24342, 0.529265, -1.1814e-4, 1.1379e-5
	};
	/* ζ — Precession argument */
	private static final double[] ZETA_COEFFS = {
		0., 5028.79695, 0., 0., 0.
	};

	/* ----- Planetary mean longitudes (eqs. 14–21) ------------------- */

	private static final double[] ME_COEFFS = {
		MathHelper.toDegrees(252, 15, 3.216919) * JulianDate.SECONDS_PER_HOUR, 538101628.66888, 0., 0., 0.
	};
	private static final double[] VE_COEFFS = {
		MathHelper.toDegrees(181, 58, 44.758419) * JulianDate.SECONDS_PER_HOUR, 210664136.45777, 0., 0., 0.
	};
	private static final double[] EM_COEFFS = {
		MathHelper.toDegrees(100, 27, 59.13885) * JulianDate.SECONDS_PER_HOUR, 129597742.293, 0., 0., 0.
	};
	private static final double[] MA_COEFFS = {
		MathHelper.toDegrees(355, 26, 3.642778) * JulianDate.SECONDS_PER_HOUR, 68905077.65936, 0., 0., 0.
	};
	private static final double[] JU_COEFFS = {
		MathHelper.toDegrees(34, 21, 5.379392) * JulianDate.SECONDS_PER_HOUR, 10925660.57335, 0., 0., 0.
	};
	private static final double[] SA_COEFFS = {
		MathHelper.toDegrees(50, 4, 38.902495) * JulianDate.SECONDS_PER_HOUR, 4399609.33632, 0., 0., 0.
	};
	private static final double[] UR_COEFFS = {
		MathHelper.toDegrees(314, 3, 4.354234) * JulianDate.SECONDS_PER_HOUR, 1542482.57845, 0., 0., 0.
	};
	private static final double[] NE_COEFFS = {
		MathHelper.toDegrees(304, 20, 56.808371) * JulianDate.SECONDS_PER_HOUR, 786547.897, 0., 0., 0.
	};

	/* ======================================================================
	 *                          Singleton
	 * ====================================================================== */

	private static final class SingletonHelperLLR{
		private static final MoonPosition INSTANCE = new MoonPosition(ElpParameters.LLR,
			loadFromClasspath(ElpParameters.LLR));
	}
	private static final class SingletonHelperDE405{
		private static final MoonPosition INSTANCE = new MoonPosition(ElpParameters.DE405,
			loadFromClasspath(ElpParameters.DE405));
	}

	/** The chosen parameter set. */
	private final ElpParameters params;
	/** The loaded coefficients. */
	private final ElpCoefficients coefs;

	/*
	 * Pre-computed polynomial coefficient arrays. These carry the
	 * corrections of the chosen parameter set and are immutable after
	 * construction, so they are computed once here rather than on every
	 * call to {@link #arguments(double)}.
	 */
	private final double[] w1Corr;
	private final double[] w2Corr;
	private final double[] w3Corr;
	private final double[] eaCorr;
	private final double[] perigeeCorr;


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */


	/** LLR */
	public static MoonPosition getInstanceLLR(){
		return SingletonHelperLLR.INSTANCE;
	}

	/** DE405/DE406 */
	public static MoonPosition getInstanceDE405(){
		return SingletonHelperDE405.INSTANCE;
	}

	private MoonPosition(final ElpParameters params, final ElpCoefficients coefs){
		this.params = params;
		this.coefs = coefs;

		// One-time construction of the corrected coefficient arrays. The
		// base arrays are the static constants defined above; each clone
		// applies the parameter-set corrections to its first five entries.
		this.w1Corr = corr(W1_COEFFS,
			params.Dw1_0, params.Dw1_1, params.Dw1_2,
			params.Dw1_3, params.Dw1_4);
		this.w2Corr = corr(W2_COEFFS,
			params.Dw2_0, params.Dw2_1 + params.Cw2_1,
			params.Dw2_2, params.Dw2_3, 0.);
		this.w3Corr = corr(W3_COEFFS,
			params.Dw3_0, params.Dw3_1 + params.Cw3_1,
			params.Dw3_2, params.Dw3_3, 0.);
		this.eaCorr = corr(EA_COEFFS,
			params.Deart_0, params.Deart_1, 0., 0., 0.);
		this.perigeeCorr = corr(PERIGEE_COEFFS,
			params.Dperi, 0., 0., 0., 0.);
	}


	private static ElpCoefficients loadFromClasspath(final ElpParameters params){
		return new ElpCoefficients(
			readMainResource("elp_main.long", params),
			readMainResource("elp_main.lat", params),
			readMainResource("elp_main.dist", params),
			readPertResource("elp_pert.longT0"),
			readPertResource("elp_pert.longT1"),
			readPertResource("elp_pert.longT2"),
			readPertResource("elp_pert.longT3"),
			readPertResource("elp_pert.latT0"),
			readPertResource("elp_pert.latT1"),
			readPertResource("elp_pert.latT2"),
			readPertResource("elp_pert.distT0"),
			readPertResource("elp_pert.distT1"),
			readPertResource("elp_pert.distT2"),
			readPertResource("elp_pert.distT3"));
	}

	private static List<ElpCoefficients.MainTerm> readMainResource(final String name, final ElpParameters params){
		try(final InputStream in = MoonPosition.class.getResourceAsStream("/" + name)){
			if(in == null)
				throw new IllegalStateException("Missing ELP coefficient resource: " + name);

			return ElpSeriesReader.readMain(in, params);
		}
		catch(final IOException e){
			throw new IllegalStateException("Cannot read ELP coefficient resource: " + name, e);
		}
	}

	private static List<ElpCoefficients.PertTerm> readPertResource(final String name){
		try(final InputStream in = MoonPosition.class.getResourceAsStream("/" + name)){
			if(in == null)
				throw new IllegalStateException("Missing ELP coefficient resource: " + name);

			return ElpSeriesReader.readPert(in);
		}
		catch(final IOException e){
			throw new IllegalStateException("Cannot read ELP coefficient resource: " + name, e);
		}
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Geocentric ecliptic position referred to the <em>mean ecliptic of
	 * date</em>. This is the natural output of the theory.
	 *
	 * @param t Julian centuries of TDB from J2000:
	 *          {@code (JD_TDB − 2451545.) / 36525}
	 * @return the position, never {@code null}
	 */
	public EclipticCoordinate eclipticOfDate(final double t){
		// One argument array is built and shared by the three coordinate
		// computations. This is the only allocation in the hot path.
		final double[] args = arguments(t);
		return new EclipticCoordinate(
			latitudeOfDate(t, args),
			longitudeOfDate(t, args),
			distance(t, args));
	}

	/**
	 * Geocentric rectangular coordinates referred to the <em>mean ecliptic
	 * and equinox of J2000.0</em>. This is the frame used by VSOP2013.
	 *
	 * @param t Julian centuries of TDB from J2000
	 * @return a three-element array {@code [X, Y, Z]}, in kilometres
	 */
	public double[] rectangular(final double t){
		final EclipticCoordinate sph = eclipticOfDate(t);

		// Precession angles from the ecliptic of date to J2000
		final double p = MathHelper.polynomial(t, P_COEFFS);
		final double q = MathHelper.polynomial(t, Q_COEFFS);

		final double rCosVcosU = sph.getDistance() * StrictMath.cos(sph.getLongitude())
			* StrictMath.cos(sph.getLatitude());
		final double rSinVcosU = sph.getDistance() * StrictMath.sin(sph.getLongitude())
			* StrictMath.cos(sph.getLatitude());
		final double rSinU = sph.getDistance() * StrictMath.sin(sph.getLatitude());

		// Rotation matrix from Chapront & Francou (2003), eq. 26.
		final double oneMinus2P2 = 1. - 2. * p * p;
		final double oneMinus2Q2 = 1. - 2. * q * q;
		final double twoPQ = 2. * p * q;
		final double sqrt = StrictMath.sqrt(1. - p * p - q * q);
		final double twoPSqrt = 2. * p * sqrt;
		final double twoQSqrt = 2. * q * sqrt;

		final double x = oneMinus2P2 * rCosVcosU + twoPQ * rSinVcosU + twoPSqrt * rSinU;
		final double y = twoPQ * rCosVcosU + oneMinus2Q2 * rSinVcosU - twoQSqrt * rSinU;
		final double z = -twoPSqrt * rCosVcosU + twoQSqrt * rSinVcosU + (oneMinus2P2 - 2. * q * q) * rSinU;

		return new double[]{x, y, z};
	}

	/**
	 * Geocentric ecliptic longitude of the Moon referred to the mean
	 * ecliptic of date.
	 *
	 * @param t Julian centuries of TDB from J2000
	 * @return longitude [rad], in the interval [0, 2π)
	 */
	public double longitudeOfDate(final double t){
		return longitudeOfDate(t, arguments(t));
	}

	/**
	 * Geocentric ecliptic latitude of the Moon referred to the mean
	 * ecliptic of date.
	 *
	 * @param t Julian centuries of TDB from J2000
	 * @return latitude [rad]
	 */
	public double latitudeOfDate(final double t){
		return latitudeOfDate(t, arguments(t));
	}

	/**
	 * Geocentric distance of the Moon.
	 *
	 * @param t Julian centuries of TDB from J2000
	 * @return distance [km]
	 */
	public double distance(final double t){
		return distance(t, arguments(t));
	}


	/* ======================================================================
	 *                          Fundamental arguments
	 * ====================================================================== */

	/**
	 * Evaluates the thirteen fundamental arguments plus the Moon mean
	 * longitude at the given time.
	 *
	 * <p>The result is a 14-element array. Its layout is documented by the
	 * {@code ARG_*} constants: indices 0..12 are the fundamental arguments
	 * in the same order as the perturbation multipliers, index 13 is the
	 * Moon mean longitude {@code W1}.</p>
	 * <p>Order: {@code [D, F, l, l', Me, Ve, EM, Ma, Ju, Sa, Ur, Ne, ζ]}.
	 * All values are in radians, reduced to [0, 2π).</p>
	 */
	private double[] arguments(final double t){
		final double[] a = new double[ARG_COUNT];

		// ----- Mean longitudes (with parameter corrections) --------------
		final double w1 = MathHelper.mod2pi(MathHelper.polynomial(t, w1Corr) * SEC);
		final double w2 = MathHelper.mod2pi(MathHelper.polynomial(t, w2Corr) * SEC);
		final double w3 = MathHelper.mod2pi(MathHelper.polynomial(t, w3Corr) * SEC);
		final double ea = MathHelper.mod2pi(MathHelper.polynomial(t, eaCorr) * SEC);
		final double perigee = MathHelper.mod2pi(MathHelper.polynomial(t, perigeeCorr) * SEC);

		// ----- Delaunay arguments (eqs. 10–13) --------------------------
		a[ARG_D]  = MathHelper.mod2pi(w1 - ea + StrictMath.PI);
		a[ARG_F]  = MathHelper.mod2pi(w1 - w3);
		a[ARG_L]  = MathHelper.mod2pi(w1 - w2);
		a[ARG_LP] = MathHelper.mod2pi(ea - perigee);

		// ----- Planetary mean longitudes --------------------------------
		a[ARG_ME] = MathHelper.mod2pi(MathHelper.polynomial(t, ME_COEFFS) * SEC);
		a[ARG_VE] = MathHelper.mod2pi(MathHelper.polynomial(t, VE_COEFFS) * SEC);
		a[ARG_EM] = MathHelper.mod2pi(MathHelper.polynomial(t, EM_COEFFS) * SEC);
		a[ARG_MA] = MathHelper.mod2pi(MathHelper.polynomial(t, MA_COEFFS) * SEC);
		a[ARG_JU] = MathHelper.mod2pi(MathHelper.polynomial(t, JU_COEFFS) * SEC);
		a[ARG_SA] = MathHelper.mod2pi(MathHelper.polynomial(t, SA_COEFFS) * SEC);
		a[ARG_UR] = MathHelper.mod2pi(MathHelper.polynomial(t, UR_COEFFS) * SEC);
		a[ARG_NE] = MathHelper.mod2pi(MathHelper.polynomial(t, NE_COEFFS) * SEC);

		// ----- Precession argument (eq. 22) -----------------------------
		a[ARG_ZETA] = MathHelper.mod2pi(w1 + MathHelper.polynomial(t, ZETA_COEFFS) * SEC);

		// ----- Moon mean longitude --------------------------------------
		a[ARG_W1] = w1;

		return a;
	}

	/**
	 * Returns a copy of {@code base} whose first five coefficients are
	 * incremented by the given corrections.
	 */
	private static double[] corr(final double[] base, final double c0, final double c1, final double c2, final double c3,
			final double c4){
		final double[] out = base.clone();
		out[0] += c0;
		out[1] += c1;
		out[2] += c2;
		out[3] += c3;
		out[4] += c4;
		return out;
	}


	/* ======================================================================
	 *                          Coordinate computation
	 * ====================================================================== */

	/**
	 * Longitude {@code V = W1 + main series + perturbation series}.
	 *
	 * <p>The three perturbation series are summed, then combined by Horner
	 * evaluation. No temporary array is allocated: the four coefficients
	 * are held in local variables.</p>
	 */
	private double longitudeOfDate(final double t, final double[] args){
		final double p0 = pertSum(coefs.pertLongT0, args);
		final double p1 = pertSum(coefs.pertLongT1, args);
		final double p2 = pertSum(coefs.pertLongT2, args);
		final double p3 = pertSum(coefs.pertLongT3, args);
		final double pert = p0 + t * (p1 + t * (p2 + t * p3));

		return MathHelper.mod2pi(args[ARG_W1]
			+ mainSum(coefs.mainLongitude, args, true)
			+ pert);
	}

	private double latitudeOfDate(final double t, final double[] args){
		final double p0 = pertSum(coefs.pertLatT0, args);
		final double p1 = pertSum(coefs.pertLatT1, args);
		final double p2 = pertSum(coefs.pertLatT2, args);
		final double pert = p0 + t * (p1 + t * p2);

		return mainSum(coefs.mainLatitude, args, true) + pert;
	}

	private double distance(final double t, final double[] args){
		final double p0 = pertSum(coefs.pertDistT0, args);
		final double p1 = pertSum(coefs.pertDistT1, args);
		final double p2 = pertSum(coefs.pertDistT2, args);
		final double p3 = pertSum(coefs.pertDistT3, args);
		final double pert = p0 + t * (p1 + t * (p2 + t * p3));

		return RA0 * (mainSum(coefs.mainDistance, args, false) + pert);
	}


	/* ======================================================================
	 *                          Series evaluation
	 * ====================================================================== */

	/**
	 * Sums a main-problem series. The argument of each term is
	 * {@code i1·D + i2·F + i3·l + i4·l'}; the amplitude has already been
	 * corrected at load time. Longitude and latitude are sine series,
	 * distance is a cosine series.
	 */
	private static double mainSum(final List<ElpCoefficients.MainTerm> terms, final double[] args, final boolean sine){
		final double d = args[ARG_D];
		final double f = args[ARG_F];
		final double l = args[ARG_L];
		final double lp = args[ARG_LP];

		double sum = 0.;
		for(int i = 0, n = terms.size(); i < n; i ++){
			final ElpCoefficients.MainTerm term = terms.get(i);
			final double arg = term.i1() * d + term.i2() * f
				+ term.i3() * l + term.i4() * lp;
			sum += term.amplitude() * (sine? StrictMath.sin(arg): StrictMath.cos(arg));
		}
		return sum;
	}

	/**
	 * Sums a perturbation series. The argument of each term is
	 * {@code Σ iⱼ·argⱼ + φ₀}; the amplitude is multiplied by
	 * {@code sin(argument)}.
	 */
	private static double pertSum(final List<ElpCoefficients.PertTerm> terms, final double[] args){
		double sum = 0.;
		for(int k = 0, n = terms.size(); k < n; k ++){
			final ElpCoefficients.PertTerm term = terms.get(k);
			final int[] m = term.multipliers();
			double arg = term.phase();
			for(int j = 0; j < 13; j ++)
				arg += m[j] * args[j];
			sum += term.amplitude() * StrictMath.sin(arg);
		}
		return sum;
	}


	public static void main(final String[] args){
		final double t = JulianDate.centuryJ2000Of(2444269.5);
		final double[] xyz = MoonPosition.getInstanceDE405()
			.rectangular(t);
		System.out.println("x: " + xyz[0] + " y: " + xyz[1] + " z: " + xyz[2]);
		System.out.println("x: -186813.08162 y: 349310.09818 z: -19003.33833");
	}

}
