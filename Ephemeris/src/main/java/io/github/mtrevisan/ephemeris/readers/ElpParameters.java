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
package io.github.mtrevisan.ephemeris.readers;


/**
 * Adjustable parameters of the ELP/MPP02 lunar theory and the derived
 * correction factors applied to the main-problem amplitudes.
 *
 * <p>The ELP/MPP02 theory provides two independent parameter sets,
 * obtained by fitting the series to either:</p>
 * <ul>
 *   <li>the <b>LLR</b> (Lunar Laser Ranging) observations collected since
 *       1970 — use {@link #LLR};</li>
 *   <li>the <b>DE405/DE406</b> JPL numerical ephemerides — use
 *       {@link #DE405}.</li>
 * </ul>
 *
 * <p>The two sets differ only in 21 adjustable parameters. The six
 * correction factors {@code fA}, {@code fB1}..{@code fB5} are computed
 * from those parameters and applied to the main-problem coefficients at
 * load time.</p>
 *
 * <p>Reference: Chapront &amp; Francou (2003), <i>The lunar theory ELP
 * revisited. Introduction of new planetary perturbations</i>,
 * A&amp;A 404, 735–742.</p>
 */
public final class ElpParameters{

	/* ======================================================================
	 *                          Constants
	 * ====================================================================== */

	/** Ratio of the mean motion of the Sun to that of the Moon. */
	private static final double AM = 0.074801329;

	/** Ratio of the semi-major axes of the Earth and Moon orbits. */
	private static final double ALPHA = 0.002571881;

	/** Arcsecond to radian conversion factor. */
	private static final double SEC = StrictMath.PI / 648000.;

	/**
	 * Numerical coefficients of the auxiliary B' polynomials used in the
	 * computation of the correction factors. Row i corresponds to
	 * B'_{2,i} and B'_{3,i}; row 0 holds the terms that appear in both.
	 * Reference: Chapront &amp; Francou (2003), Table 3.
	 */
	private static final double[][] BP = {
		{0.311079095, -0.103837907},
		{-0.004482398, 0.000668287},
		{-0.001102485, -0.001298072},
		{0.001056062, -0.000178028},
		{0.000050928, -0.000037342}
	};

	/* ======================================================================
	 *                          Adjustable parameters
	 * ====================================================================== */

	/** ΔW₁⁽⁰⁾ — constant correction to the Moon mean longitude [″]. */
	public final double Dw1_0;
	/** ΔW₂⁽⁰⁾ — constant correction to the Sun mean longitude [″]. */
	public final double Dw2_0;
	/** ΔW₃⁽⁰⁾ — constant correction to the lunar node longitude [″]. */
	public final double Dw3_0;
	/** ΔT⁽⁰⁾ — constant correction to the Earth mean longitude [″]. */
	public final double Deart_0;
	/** Δϖ′⁽⁰⁾ — constant correction to the solar perigee [″]. */
	public final double Dperi;
	/** ΔW₁⁽¹⁾ — linear correction to the Moon mean longitude [″/cy]. */
	public final double Dw1_1;
	/** ΔΓ — correction to the lunar secular acceleration [″]. */
	final double Dgam;
	/** ΔE — correction to the Earth eccentricity [″]. */
	final double De;
	/** ΔT⁽¹⁾ — linear correction to the Earth mean longitude [″/cy]. */
	public final double Deart_1;
	/** Δe′ — correction to the solar eccentricity [″]. */
	final double Dep;
	/** ΔW₂⁽¹⁾ — linear correction to the Sun mean longitude [″/cy]. */
	public final double Dw2_1;
	/** ΔW₃⁽¹⁾ — linear correction to the lunar node longitude [″/cy]. */
	public final double Dw3_1;
	/** ΔW₁⁽²⁾ — quadratic correction to the Moon mean longitude [″/cy²]. */
	public final double Dw1_2;
	/** ΔW₁⁽³⁾ — cubic correction to the Moon mean longitude [″/cy³]. */
	public final double Dw1_3;
	/** ΔW₁⁽⁴⁾ — quartic correction to the Moon mean longitude [″/cy⁴]. */
	public final double Dw1_4;
	/** ΔW₂⁽²⁾ — quadratic correction to the Sun mean longitude [″/cy²]. */
	public final double Dw2_2;
	/** ΔW₂⁽³⁾ — cubic correction to the Sun mean longitude [″/cy³]. */
	public final double Dw2_3;
	/** ΔW₃⁽²⁾ — quadratic correction to the lunar node longitude [″/cy²]. */
	public final double Dw3_2;
	/** ΔW₃⁽³⁾ — cubic correction to the lunar node longitude [″/cy³]. */
	public final double Dw3_3;

	/* ======================================================================
	 *                          Derived correction factors
	 * ====================================================================== */

	/** Correction factor for the main-problem distance amplitudes. */
	final double fA;
	/** Correction factor for the B₁ coefficients (longitude and latitude). */
	final double fB1;
	/** Correction factor for the B₂ coefficients (longitude and latitude). */
	final double fB2;
	/** Correction factor for the B₃ coefficients (longitude and latitude). */
	final double fB3;
	/** Correction factor for the B₄ coefficients (longitude and latitude). */
	final double fB4;
	/** Correction factor for the B₅ coefficients (longitude and latitude). */
	final double fB5;

	/** Derived correction for W₂⁽¹⁾. */
	public final double Cw2_1;
	/** Derived correction for W₃⁽¹⁾. */
	public final double Cw3_1;


	/** Parameter set fitted to the Lunar Laser Ranging observations. */
	public static final ElpParameters LLR = new ElpParameters(
		-0.10525, 0.16826, -0.10760, -0.04012, -0.04854,
		-0.32311, 0.00069, 0.00005, 0.01442, 0.00226,
		0.08017, -0.04317, -0.03794, 0., 0.,
		0., 0., 0., 0.
	);

	/** Parameter set fitted to the JPL DE405/DE406 ephemerides. */
	public static final ElpParameters DE405 = new ElpParameters(
		-0.07008, 0.20794, -0.07215, -0.00033, -0.00749,
		-0.35106, 0.00085, -0.00006, 0.00732, 0.00224,
		0.08017, -0.04317, -0.03743, -0.00018865, -0.00001024,
		0.00470602, -0.00025213, -0.00261070, -0.00010712
	);


	/* ======================================================================
	 *                          Construction
	 * ====================================================================== */

	private ElpParameters(final double dw1_0, final double dw2_0, final double dw3_0,
		final double deart_0, final double dperi, final double dw1_1,
		final double dgam, final double de, final double deart_1,
		final double dep, final double dw2_1, final double dw3_1,
		final double dw1_2, final double dw1_3, final double dw1_4,
		final double dw2_2, final double dw2_3, final double dw3_2,
		final double dw3_3){
		this.Dw1_0 = dw1_0;
		this.Dw2_0 = dw2_0;
		this.Dw3_0 = dw3_0;
		this.Deart_0 = deart_0;
		this.Dperi = dperi;
		this.Dw1_1 = dw1_1;
		this.Dgam = dgam;
		this.De = de;
		this.Deart_1 = deart_1;
		this.Dep = dep;
		this.Dw2_1 = dw2_1;
		this.Dw3_1 = dw3_1;
		this.Dw1_2 = dw1_2;
		this.Dw1_3 = dw1_3;
		this.Dw1_4 = dw1_4;
		this.Dw2_2 = dw2_2;
		this.Dw2_3 = dw2_3;
		this.Dw3_2 = dw3_2;
		this.Dw3_3 = dw3_3;

		// ----- Derived angular corrections (Chapront & Francou 2003, §2) -----
		final double w11 = (1732559343.73604 + Dw1_1) * SEC;
		final double w21 = (14643420.3171 + Dw2_1) * SEC;
		final double w31 = (-6967919.5383 + Dw3_1) * SEC;

		final double x2 = w21 / w11;
		final double x3 = w31 / w11;
		final double y2 = AM * BP[0][0] + 2. * ALPHA / 3. * BP[4][0];
		final double y3 = AM * BP[0][1] + 2. * ALPHA / 3. * BP[4][1];

		final double d21 = x2 - y2;
		final double d22 = w11 * BP[1][0];
		final double d23 = w11 * BP[2][0];
		final double d24 = w11 * BP[3][0];
		final double d25 = y2 / AM;
		final double d31 = x3 - y3;
		final double d32 = w11 * BP[1][1];
		final double d33 = w11 * BP[2][1];
		final double d34 = w11 * BP[3][1];
		final double d35 = y3 / AM;

		this.Cw2_1 = d21 * Dw1_1 + d25 * Deart_1 + d22 * Dgam
			+ d23 * De + d24 * Dep;
		this.Cw3_1 = d31 * Dw1_1 + d35 * Deart_1 + d32 * Dgam
			+ d33 * De + d34 * Dep;

		// ----- Correction factors for the main-problem amplitudes -----
		final double delnuNu = (0.55604 + Dw1_1) * SEC / w11;
		final double dele = (0.01789 + De) * SEC;
		final double delg = (-0.08066 + Dgam) * SEC;
		final double delnpNu = (-0.06424 + Deart_1) * SEC / w11;
		final double delep = (-0.12879 + Dep) * SEC;

		this.fA = 1. - 2. / 3. * delnuNu;
		this.fB1 = -AM * delnuNu + delnpNu;
		this.fB2 = delg;
		this.fB3 = dele;
		this.fB4 = delep;
		this.fB5 = -2. * ALPHA / 3. * delnuNu + 2. * ALPHA / (3. * AM) * delnpNu;
	}

}
