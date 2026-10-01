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
import io.github.mtrevisan.ephemeris.helpers.MathHelper;
import io.github.mtrevisan.ephemeris.readers.NutationReader;
import io.github.mtrevisan.ephemeris.readers.NutationReader.NutationTerm;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


public final class NutationCorrections{

	private static final List<NutationTerm> NUTATION_PSI_TERMS;
	private static final List<NutationTerm> NUTATION_EPS_TERMS;
	static{
		List<NutationTerm> psiTerms;
		List<NutationTerm> epsTerms;
		try{
			psiTerms = NutationReader.readIau2000a("tab5.3a.txt");
			epsTerms = NutationReader.readIau2000a("tab5.3b.txt");
		}
		catch(final IOException e){
			psiTerms = Collections.emptyList();
			epsTerms = Collections.emptyList();
		}
		NUTATION_PSI_TERMS = psiTerms;
		NUTATION_EPS_TERMS = epsTerms;
	}

	/**
	 * Fundamental arguments IAU 2000 / IERS 2010 (in arcseconds).
	 * Order of coefficients: [t^0, t^1, t^2, t^3, t^4]
	 */
	// l: Mean anomaly of the Moon
	private static final double[] MOON_MEAN_ANOMALY = {
		485868.249036, 1717915923.2178, 31.8792, 0.051635, -0.00024470
	};
	// l': Mean anomaly of the Sun
	private static final double[] SUN_MEAN_ANOMALY = {
		1287102.740744, 129596581.0481, -0.5532, 0.000136, -0.00001149
	};
	// F: Mean argument of latitude of the Moon
	private static final double[] MOON_ARGUMENT_LATITUDE = {
		335779.526232, 1739527262.8478, -12.7512, -0.001037, 0.00000417
	};
	// D: Mean elongation of the Moon from the Sun
	public static final double[] MOON_ELONGATION_SUN = {
		1072260.703692, 1602961601.2090, -6.3706, 0.006593, -0.00003169
	};
	// Omega: Mean longitude of the ascending node of the Moon
	private static final double[] MOON_LONGITUDE_NODE = {
		450160.398036, -6962890.5431, 7.4722, 0.007702, -0.00005939
	};

	// Planetary arguments (indices 5 to 12) & General precession in longitude (index 13)
	// Rates in arcseconds per Julian century from J2000.0
	private static final double[][] PLANETARY_ARGUMENTS = {
		// Mercury mean longitude (L_Me)
		{440268522.46, 56011.},
		// Venus mean longitude (L_Ve)
		{75786630.34, 4399.},
		// Earth mean longitude (L_E)
		{0., 360007.6956},
		// Mars mean longitude (L_Ma)
		{30643793.85, 19140.},
		// Jupiter mean longitude (L_J)
		{9329500.58, 3034.9},
		// Saturn mean longitude (L_Sa)
		{3161470.20, 1222.2},
		// Uranus mean longitude (L_U)
		{1334915.45, 428.5},
		// Neptune mean longitude (L_Ne)
		{585448.05, 258.4},
		// General precession in longitude (p_A)
		{0., 5029.0966}
	};


	private final double deltaPsi;
	private final double deltaEpsilon;


	public static NutationCorrections calculate(final double jc){
		return new NutationCorrections(jc);
	}


	/**
	 * Calculate IAU 2000B Nutation corrections.
	 *
	 * @param jc Julian Century of Terrestrial Time from J2000.0 (t = (JD - 2451545.) / 36525).
	 */
	private NutationCorrections(final double jc){
		// Calculate Delaunay fundamental arguments in radians
		final double arcsecToRad = StrictMath.PI / (180. * JulianDate.SECONDS_PER_HOUR);

		// 1. Calculate Lunisolar fundamental arguments [rad]
		final double[] fundArgs = new double[14];
		// L
		fundArgs[0] = MathHelper.polynomial(jc, MOON_MEAN_ANOMALY) * arcsecToRad;
		// L'
		fundArgs[1] = MathHelper.polynomial(jc, SUN_MEAN_ANOMALY) * arcsecToRad;
		// F
		fundArgs[2] = MathHelper.polynomial(jc, MOON_ARGUMENT_LATITUDE) * arcsecToRad;
		// D
		fundArgs[3] = MathHelper.polynomial(jc, MOON_ELONGATION_SUN) * arcsecToRad;
		// Ω
		fundArgs[4] = MathHelper.polynomial(jc, MOON_LONGITUDE_NODE) * arcsecToRad;

		// 2. Calculate Planetary and Precession arguments (indices 5 to 16) [rad]
		for(int i = 0; i < PLANETARY_ARGUMENTS.length; i ++){
			final double val = PLANETARY_ARGUMENTS[i][0] + PLANETARY_ARGUMENTS[i][1] * jc;
			fundArgs[5 + i] = StrictMath.toRadians(val / JulianDate.SECONDS_PER_HOUR);
		}

		this.deltaPsi = evaluateSeries(NUTATION_PSI_TERMS, fundArgs, jc);
		this.deltaEpsilon = evaluateSeries(NUTATION_EPS_TERMS, fundArgs, jc);
	}

	private static double evaluateSeries(final List<NutationTerm> terms, final double[] fundArgs, final double jc){
		double sum = 0.;

		// 3. Evaluate IAU 2000A series terms
		for(int i = 0; i < terms.size(); i ++){
			final NutationTerm term = terms.get(i);

			// Argument = i1 × L + i2 × L' + i3 × F + i4 × D + i5 × Ω
			double arg = 0.;
			for(int k = 0; k < 14; k ++)
				if(term.arguments[k] != 0)
					arg += term.arguments[k] * fundArgs[k];

			final double sinArg = StrictMath.sin(arg);
			final double cosArg = StrictMath.cos(arg);

			// Component evaluation (bSin corresponds to B", bCos corresponds to B)
			double termVal = term.bCos * cosArg + term.bSin * sinArg;
			// If j = 1, multiply by time (jc)
			if(term.j == 1)
				termVal *= jc;

			// In table 5.3b, values apply to nutation in obliquity (or longitude depending on the table file).
			sum += termVal;
		}

		return sum;
	}


	public static double moonLongitudeAscendingNode(final double jc){
		final double arcsecToRad = StrictMath.PI / (180. * JulianDate.SECONDS_PER_HOUR);
		return MathHelper.mod2pi(MathHelper.polynomial(jc, MOON_LONGITUDE_NODE) * arcsecToRad);
	}

	/** @return Nutation in longitude (Delta psi) [rad]. */
	public double getDeltaPsi(){
		return deltaPsi;
	}

	/** @return Nutation in obliquity (Delta epsilon) [rad]. */
	public double getDeltaEpsilon(){
		return deltaEpsilon;
	}


	public static void main(final String[] args) {
		final double[] testJDs = {
			JulianDate.of(1950, 1, 1), // Past
			JulianDate.of(2026, 10, 1), // Present
			JulianDate.of(2050, 1, 1)  // Future
		};

//		JD: 2433282.5
//			Δψ: -3.302812674004184"
//			Δε: 8.32293516840701"
//		JD: 2461314.5
//			Δψ: 8.26609072996109"
//			Δε: 8.206697931009534"
//		JD: 2469807.5
//			Δψ: 15.17124377504393"
//			Δε: -5.330057528055957"

		for(final double jd : testJDs){
			final double jc = JulianDate.centuryJ2000Of(jd);
			final NutationCorrections corrections = NutationCorrections.calculate(jc);
//			System.out.println("JD: " + jd + "\n"
//				+ "Δψ: " + corrections.getDeltaPsi() + " rad\n"
//				+ "Δε: " + corrections.getDeltaEpsilon() + " rad");
			System.out.println("JD: " + jd + "\n"
				+ "Δψ: " + (corrections.getDeltaPsi() * JulianDate.SECONDS_PER_HOUR * 180. / Math.PI) + "\"\n"
				+ "Δε: " + (corrections.getDeltaEpsilon() * JulianDate.SECONDS_PER_HOUR * 180. / Math.PI) + "\"");
		}
	}

}
