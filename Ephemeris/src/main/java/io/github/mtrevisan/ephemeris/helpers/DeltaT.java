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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;


/**
 * ΔT = TT − UT1, the difference between Terrestrial Time (a uniform atomic
 * time scale) and Universal Time (a time scale tied to the Earth's rotation).
 *
 * <p>ΔT is needed whenever a civil (UT) instant must be converted to the
 * dynamical time at which an ephemeris is evaluated. An error of one second
 * in ΔT shifts the Moon's longitude by about 0.55 arcseconds, which can
 * move a new-moon instant by roughly half a second; an error of one minute
 * is enough to change the civil day on which a new moon or a solar term
 * falls when the event lies close to midnight.</p>
 *
 * <p><b>Three sources, in decreasing order of priority.</b></p>
 *
 * <ol>
 *   <li><b>IERS observed table</b> — monthly samples from February 1973 to
 *       the present, loaded from {@code /deltat.data.txt}. These are the
 *       most accurate values available: they are derived directly from
 *       VLBI, satellite laser ranging and GPS observations, not from any
 *       lunar model.</li>
 *
 *   <li><b>Stephenson-Morrison-Hohenkerk spline</b> — the cubic spline
 *       published as Addendum 2020 to <i>Measurement of the Earth's
 *       Rotation: 720 BC to AD 2015</i>, loaded from
 *       {@code /Table-S15.2020.txt}. The spline covers −720 to +2019 with
 *       an uncertainty that varies from a few seconds in the telescopic
 *       era to a few tens of seconds in antiquity.</li>
 *
 *   <li><b>Espenak-Meeus polynomials</b> — the piecewise-polynomial
 *       approximation from the NASA Five Millennium Canon of Solar
 *       Eclipses, valid from −1999 to +3000. Used only outside the spline
 *       range or when the spline table is unavailable.</li>
 * </ol>
 *
 * <p><b>Continuity.</b> The three sources disagree by a few seconds at
 * their boundaries. To avoid visible steps, the value returned by
 * {@link #deltaTSeconds(double)} is a weighted average of the two sources
 * that bracket each boundary, with the weight computed by a cubic Hermite
 * smoothstep. The result is C¹-continuous across the entire time line:
 * neither the value nor its first derivative jumps at any boundary.
 * The width of each transition zone is {@link #TAPER_YEARS} years on
 * either side of the boundary.</p>
 *
 * <p><b>Lunar secular acceleration.</b> The Espenak-Meeus polynomials and
 * the S/M/H spline both assume a tidal acceleration of −26.0″/cy². The
 * ELP/MPP02 lunar theory used by {@link io.github.mtrevisan.ephemeris.engine.MoonPosition}
 * adopts −25.858″/cy². To keep historical values consistent with the
 * lunar ephemeris, a correction {@code −0.000012932·(y−1955)²} seconds is
 * added to the polynomial and spline branches. The correction is not
 * applied to the IERS branch, because those values are direct
 * observations of the Earth's rotation and do not depend on any lunar
 * model.</p>
 *
 * <p><b>References.</b></p>
 * <ul>
 *   <li>Morrison, L. V., Stephenson, F. R., Hohenkerk, C. Y., Zawilski, M.,
 *       <i>Addendum 2020 to 'Measurement of the Earth's Rotation: 720 BC
 *       to AD 2015'</i>, Proc. R. Soc. A 478 (2021).</li>
 *   <li>Espenak, F. &amp; Meeus, J., <i>Five Millennium Canon of Solar
 *       Eclipses</i>, NASA/TP-2006-214141 (2006).</li>
 *   <li>IERS Bulletin A — monthly Earth orientation parameters.</li>
 * </ul>
 */
public final class DeltaT{

	/* ======================================================================
	 *                          Constants
	 * ====================================================================== */

	/** Classpath resource for the IERS monthly table. */
	private static final String IERS_RESOURCE = "/deltat.data";

	/** Classpath resource for the Stephenson-Morrison-Hohenkerk spline. */
	private static final String SPLINE_RESOURCE = "/Table-S15.2020.txt";

	/** Number of seconds in a day; used to convert ΔT to days. */
	public static final double SECONDS_PER_DAY = 86_400.;

	/**
	 * Half-width, in years, of the transition zone between two adjacent
	 * sources. Inside {@code [boundary − TAPER_YEARS, boundary + TAPER_YEARS]}
	 * the two sources are blended with a cubic Hermite weight, giving a
	 * C¹-continuous curve. Outside, each source is used alone.
	 *
	 * <p>Ten years is a compromise: long enough that the taper's slope
	 * never exceeds the natural rate of change of ΔT (about one second
	 * per year), and short enough that the transition completes within a
	 * human generation.</p>
	 */
	private static final double TAPER_YEARS = 10.;


	/* ======================================================================
	 *                          IERS data
	 * ====================================================================== */

	/**
	 * A single IERS observation: the Julian Day Number at 0h UT of the
	 * first day of the month, the same instant as a decimal year, and
	 * the corresponding ΔT in seconds.
	 */
	private record IersSample(double jd, double decimalYear, double deltaT){
	}


	private static final List<IersSample> IERS_SAMPLES;
	private static final double IERS_START;
	private static final double IERS_END;


	/* ======================================================================
	 *                          Stephenson-Morrison-Hohenkerk spline
	 * ====================================================================== */

	/**
	 * One row of the S/M/H spline table: the boundaries of the interval,
	 * plus the four cubic coefficients {@code a_0..a_3}. The polynomial is
	 * evaluated in the normalized variable
	 * {@code t = (Y − K_i) / (K_{i+1} − K_i)}.
	 */
	private record SplineRow(double kStart, double kEnd,
									 double a0, double a1, double a2, double a3){
	}


	private static final List<SplineRow> SPLINE_ROWS;
	private static final double SPLINE_START;
	private static final double SPLINE_END;


	/* ======================================================================
	 *                          Static initialization
	 * ====================================================================== */

	static{
		// IERS table
		List<IersSample> iers;
		try(final InputStream in = DeltaT.class.getResourceAsStream(IERS_RESOURCE)){
			iers = (in != null? parseIERSFile(in): List.of());
		}
		catch(final IOException e){
			iers = List.of();
		}
		IERS_SAMPLES = iers;
		IERS_START = (iers.isEmpty()? 0.: iers.getFirst().decimalYear());
		IERS_END = (iers.isEmpty()? 0.: iers.getLast().decimalYear());

		// S/M/H spline table
		List<SplineRow> spline;
		try(final InputStream in = DeltaT.class.getResourceAsStream(SPLINE_RESOURCE)){
			spline = (in != null? parseSplineFile(in): List.of());
		}
		catch(final IOException e){
			spline = List.of();
		}
		SPLINE_ROWS = spline;
		SPLINE_START = (spline.isEmpty()? 0.: spline.getFirst().kStart());
		SPLINE_END = (spline.isEmpty()? 0.: spline.getLast().kEnd());
	}


	private DeltaT(){
	}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Returns ΔT in seconds for the given Julian Day Number in UT.
	 */
	public static double deltaTSecondsFromJd(final double jdUt){
		final double decimalYear = jdToDecimalYear(jdUt);
		return deltaTSeconds(decimalYear);
	}

	/**
	 * Returns ΔT in seconds for the given calendar date. The month is
	 * evaluated at its midpoint, matching the convention used by the
	 * Espenak-Meeus polynomials.
	 */
	public static double deltaTSeconds(final int year, final int month){
		final double decimalYear = year + (month - 0.5) / 12.;
		return deltaTSeconds(decimalYear);
	}

	/**
	 * Returns ΔT in seconds at the given instant, expressed as a decimal
	 * year (e.g. 1980.083 for 1980-01-31 00:00 UT). The value is taken
	 * from the highest-priority source whose range contains the instant,
	 * and smoothly blended with the neighbouring source within
	 * {@link #TAPER_YEARS} years of each boundary.
	 *
	 * @param decimalYear the year with fractional part
	 * @return ΔT [seconds]
	 */
	public static double deltaTSeconds(final double decimalYear){
		final boolean hasIers = !IERS_SAMPLES.isEmpty();
		final boolean hasSpline = !SPLINE_ROWS.isEmpty();

		// ------------------------------------------------------------------
		// IERS region: the highest-priority source, with a smooth blend
		// toward the spline just below the start and toward the polynomial
		// just above the end.
		// ------------------------------------------------------------------
		if(hasIers
			&& decimalYear >= IERS_START - TAPER_YEARS
			&& decimalYear <= IERS_END + TAPER_YEARS){
			if(hasSpline && decimalYear < IERS_START + TAPER_YEARS)
				return blend(decimalYear, IERS_START,
					DeltaT::splineValueCorrected, DeltaT::iersValue);
			if(decimalYear > IERS_END - TAPER_YEARS)
				return blend(decimalYear, IERS_END,
					DeltaT::iersValue, DeltaT::polynomialValueCorrected);
			return iersValue(decimalYear);
		}

		// ------------------------------------------------------------------
		// Spline region: used below the IERS range and above the polynomial
		// range, with a smooth blend at its lower boundary (poly -> spline).
		// ------------------------------------------------------------------
		if(hasSpline
			&& decimalYear >= SPLINE_START - TAPER_YEARS
			&& decimalYear <= SPLINE_END + TAPER_YEARS){
			if(decimalYear < SPLINE_START + TAPER_YEARS)
				return blend(decimalYear, SPLINE_START,
					DeltaT::polynomialValueCorrected, DeltaT::splineValueCorrected);
			return splineValueCorrected(decimalYear);
		}

		// ------------------------------------------------------------------
		// Deep past or far future: the polynomial is the only available
		// source. Its range covers −1999 to +3000, and it is extrapolated
		// beyond.
		// ------------------------------------------------------------------
		return polynomialValueCorrected(decimalYear);
	}

	/**
	 * Returns ΔT in days, the unit expected by {@link JulianDate} when
	 * converting between UT and TT.
	 */
	public static double deltaTDays(final double decimalYear){
		return deltaTSeconds(decimalYear) / SECONDS_PER_DAY;
	}


	/**
	 * Returns {@code true} when the IERS table covers the given instant.
	 */
	public static boolean hasIERSCoverage(final double decimalYear){
		return (!IERS_SAMPLES.isEmpty()
			&& decimalYear >= IERS_START
			&& decimalYear <= IERS_END);
	}


	/**
	 * Returns {@code true} when the S/M/H spline table covers the given
	 * instant.
	 */
	public static boolean hasSplineCoverage(final double decimalYear){
		return (!SPLINE_ROWS.isEmpty()
			&& decimalYear >= SPLINE_START
			&& decimalYear <= SPLINE_END);
	}


	/* ======================================================================
	 *                          Blending
	 * ====================================================================== */

	/**
	 * Blends two sources around a boundary using a cubic Hermite smoothstep
	 * weight. The result equals {@code left(y)} at {@code yB − TAPER_YEARS},
	 * {@code right(y)} at {@code yB + TAPER_YEARS}, and their weighted
	 * average in between. Because the smoothstep has zero derivative at
	 * both endpoints and the formula is symmetric, the resulting curve is
	 * C¹-continuous at every point, including the boundary itself.
	 */
	private static double blend(final double y, final double yB,
		final DoubleUnaryOperator left, final DoubleUnaryOperator right){
		final double t = (y - (yB - TAPER_YEARS)) / (2. * TAPER_YEARS);
		final double tc = (t < 0.? 0.: Math.min(t, 1.));
		final double w = tc * tc * (3. - 2. * tc);
		return (1. - w) * left.applyAsDouble(y) + w * right.applyAsDouble(y);
	}


	/* ======================================================================
	 *                          IERS evaluation
	 * ====================================================================== */

	/**
	 * Linear interpolation between the two IERS samples that bracket the
	 * given decimal year. Outside the table, the endpoint values are
	 * returned, which is what the blend expects when the boundary is
	 * exactly at the table edge.
	 */
	private static double iersValue(final double y){
		if(IERS_SAMPLES.isEmpty())
			return 0.;

		final double yc = Math.clamp(y, IERS_START, IERS_END);

		int lo = 0;
		int hi = IERS_SAMPLES.size() - 1;
		while(hi - lo > 1){
			final int mid = (lo + hi) >>> 1;
			if(IERS_SAMPLES.get(mid).decimalYear() <= yc)
				lo = mid;
			else
				hi = mid;
		}

		final IersSample a = IERS_SAMPLES.get(lo);
		final IersSample b = IERS_SAMPLES.get(hi);
		final double span = b.decimalYear() - a.decimalYear();
		if(span <= 0.)
			return a.deltaT();

		final double t = (yc - a.decimalYear()) / span;
		return a.deltaT() + t * (b.deltaT() - a.deltaT());
	}


	/**
	 * Parses the IERS monthly file. Each data line holds the year, month,
	 * day and ΔT in seconds, separated by whitespace. Lines starting with
	 * {@code #} or {@code %} are ignored, as are blank lines.
	 *
	 * <p>The parser strips an optional UTF-8 byte-order mark (U+FEFF) that
	 * some editors prepend to the file. Without stripping it, the BOM would
	 * glue itself to the first character of the first line and the comment
	 * check {@code startsWith("#")} would fail, silently discarding the
	 * entire file.</p>
	 *
	 * <p>Each data line is split on one or more whitespace characters; the
	 * first four tokens must be the year, month, day and ΔT. Any trailing
	 * tokens are ignored, so the parser is forward-compatible with future
	 * revisions of the file that add extra columns.</p>
	 */
	private static List<IersSample> parseIERSFile(final InputStream in) throws IOException{
		final List<IersSample> out = new ArrayList<>();
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))){
			String line;
			boolean firstLine = true;
			while((line = reader.readLine()) != null){
				// Strip the UTF-8 BOM, if present, from the very first line.
				if(firstLine){
					line = stripBom(line);

					firstLine = false;
				}

				final String trimmed = line.trim();
				if(trimmed.isEmpty()
						|| trimmed.startsWith("#")
						|| trimmed.startsWith("%")
						|| trimmed.startsWith("!"))
					continue;

				final String[] tokens = trimmed.split("\\s+");
				if(tokens.length < 4)
					continue;

				try{
					final int year = Integer.parseInt(tokens[0]);
					final int month = Integer.parseInt(tokens[1]);
					final int day = Integer.parseInt(tokens[2]);
					final double dt = Double.parseDouble(tokens[3]);

					final double jd = JulianDate.of(year, month, day);
					final double decimalYear = year + (month - 1) / 12.
						+ (day - 1) / 365.25;

					out.add(new IersSample(jd, decimalYear, dt));
				}
				catch(final NumberFormatException | ArrayIndexOutOfBoundsException ignored){
					// Skip malformed lines silently. In production, a logger
					// call here would help trace a file whose format has
					// drifted from the expected one.
				}
			}
		}
		return out;
	}

	/**
	 * Removes a leading UTF-8 byte-order mark (U+FEFF) from a string. The
	 * BOM appears in a Java string as a single {@code char} at position
	 * zero, so it can be checked with a direct comparison.
	 */
	private static String stripBom(final String s){
		return (!s.isEmpty() && s.charAt(0) == '\uFEFF'? s.substring(1): s);
	}


	/* ======================================================================
	 *                          Spline evaluation
	 * ====================================================================== */

	/**
	 * Evaluates the S/M/H spline at the given decimal year. Outside the
	 * table, the first or last interval is used as a constant extension,
	 * which is what the blend expects near the boundary.
	 */
	private static double splineValue(final double y){
		if(SPLINE_ROWS.isEmpty())
			return 0.;

		final double yc = Math.clamp(y, SPLINE_START, SPLINE_END - 1-e-9);

		int lo = 0;
		int hi = SPLINE_ROWS.size() - 1;
		while(hi - lo > 1){
			final int mid = (lo + hi) >>> 1;
			if(SPLINE_ROWS.get(mid).kStart() <= yc)
				lo = mid;
			else
				hi = mid;
		}

		final SplineRow row = SPLINE_ROWS.get(lo);
		final double span = row.kEnd() - row.kStart();
		final double t = (span > 0.? (yc - row.kStart()) / span: 0.);
		return row.a0() + t * (row.a1() + t * (row.a2() + t * row.a3()));
	}


	/**
	 * Parses the S/M/H spline table. The file is fixed-format but the
	 * tokens are whitespace-separated, so a simple splitter works. Header
	 * lines (starting with {@code -} or text) are skipped automatically
	 * because their tokens do not parse as numbers.
	 */
	private static List<SplineRow> parseSplineFile(final InputStream in) throws IOException{
		final List<SplineRow> out = new ArrayList<>();
		try(BufferedReader reader = new BufferedReader(
			new InputStreamReader(in, StandardCharsets.UTF_8))){
			String line;
			while((line = reader.readLine()) != null){
				final String trimmed = line.trim();
				if(trimmed.isEmpty() || trimmed.startsWith("-")
					|| trimmed.startsWith("#") || trimmed.startsWith("%"))
					continue;

				final String[] tokens = trimmed.split("\\s+");
				if(tokens.length < 7)
					continue;

				try{
					// tokens[0] is the row index; skip it.
					final double kStart = Double.parseDouble(tokens[1]);
					final double kEnd = Double.parseDouble(tokens[2]);
					final double a0 = Double.parseDouble(tokens[3]);
					final double a1 = Double.parseDouble(tokens[4]);
					final double a2 = Double.parseDouble(tokens[5]);
					final double a3 = Double.parseDouble(tokens[6]);
					out.add(new SplineRow(kStart, kEnd, a0, a1, a2, a3));
				}
				catch(final NumberFormatException ignored){
					// Skip malformed lines.
				}
			}
		}
		return out;
	}


	/* ======================================================================
	 *                          Corrected values
	 * ====================================================================== */

	/**
	 * S/M/H spline value with the secular-acceleration correction that
	 * brings it onto the ELP/MPP02 convention.
	 */
	private static double splineValueCorrected(final double y){
		return splineValue(y) + secularAccelerationCorrection(y);
	}

	/**
	 * Espenak-Meeus polynomial with the secular-acceleration correction
	 * that brings it onto the ELP/MPP02 convention.
	 */
	private static double polynomialValueCorrected(final double y){
		return polynomialDeltaT(y) + secularAccelerationCorrection(y);
	}


	/* ======================================================================
	 *                          Espenak-Meeus polynomials
	 * ====================================================================== */

	/**
	 * Evaluates the Espenak-Meeus piecewise polynomial for ΔT. The return
	 * value is in seconds and assumes the Morrison-Stephenson lunar
	 * secular acceleration of −26″/cy². The caller must add
	 * {@link #secularAccelerationCorrection(double)} to bring it onto the
	 * ELP/MPP02 convention of −25.858″/cy².
	 */
	private static double polynomialDeltaT(final double y){
		if(y < -500.)
			return -20. + 32. * sq((y - 1820.) / 100.);

		if(y < 500.)
			return MathHelper.polynomial(y / 100., new double[]{
				10583.6, -1014.41, 33.78311, -5.952053,
				-0.1798452, 0.022174192, 0.0090316521
			});

		if(y < 1600.)
			return MathHelper.polynomial((y - 1000.) / 100., new double[]{
				1574.2, -556.01, 71.23472, 0.319781,
				-0.8503463, -0.005050998, 0.0083572073
			});

		if(y < 1700.)
			return MathHelper.polynomial(y - 1600., new double[]{
				120., -0.9808, -0.01532, 1. / 7129.
			});

		if(y < 1800.)
			return MathHelper.polynomial(y - 1700., new double[]{
				8.83, 0.1603, -0.0059285, 0.00013336, -1. / 1174000.
			});

		if(y < 1860.)
			return MathHelper.polynomial(y - 1800., new double[]{
				13.72, -0.332447, 0.0068612, 0.0041116,
				-0.00037436, 0.0000121272, -0.0000001699, 0.000000000875
			});

		if(y < 1900.)
			return MathHelper.polynomial(y - 1860., new double[]{
				7.62, 0.5737, -0.251754, 0.01680668,
				-0.0004473624, 1. / 233174.
			});

		if(y < 1920.)
			return MathHelper.polynomial(y - 1900., new double[]{
				-2.79, 1.494119, -0.0598939, 0.0061966, -0.000197
			});

		if(y < 1941.)
			return MathHelper.polynomial(y - 1920., new double[]{
				21.20, 0.84493, -0.076100, 0.0020936
			});

		if(y < 1961.)
			return MathHelper.polynomial(y - 1950., new double[]{
				29.07, 0.407, -1. / 233., 1. / 2547.
			});

		if(y < 1986.)
			return MathHelper.polynomial(y - 1975., new double[]{
				45.45, 1.067, -1. / 260., -1. / 718.
			});

		if(y < 2005.)
			return MathHelper.polynomial(y - 2000., new double[]{
				63.86, 0.3345, -0.060374, 0.0017275,
				0.000651814, 0.00002373599
			});

		if(y < 2050.)
			return MathHelper.polynomial(y - 2000., new double[]{
				62.92, 0.32217, 0.005589
			});

		if(y < 2150.)
			return -20. + 32. * sq((y - 1820.) / 100.) - 0.5628 * (2150. - y);

		return -20. + 32. * sq((y - 1820.) / 100.);
	}


	/**
	 * Correction for the difference in the adopted lunar secular
	 * acceleration. The Espenak-Meeus polynomials and the S/M/H spline
	 * assume −26″/cy²; ELP/MPP02 uses −25.858″/cy². The correction is
	 * significant only outside 1955–2005, where those sources were
	 * derived without reference to a specific lunar ephemeris.
	 */
	private static double secularAccelerationCorrection(final double y){
		if(y >= 1955. && y <= 2005.)
			return 0.;

		return -0.000012932 * sq(y - 1955.);
	}


	/* ======================================================================
	 *                          Helpers
	 * ====================================================================== */

	private static double sq(final double x){
		return x * x;
	}

	/**
	 * Converts a Julian Day Number at 0h UT to a decimal year. Uses the
	 * standard J2000 epoch and a mean Julian year of 365.25 days; the
	 * error is below one day for the range of interest.
	 */
	private static double jdToDecimalYear(final double jd){
		return 2000. + JulianDate.centuryJ2000Of(jd) * 100.;
	}


	public static void main(final String[] args){
		final double[] testYears = {
			-720., -500., 0., 500., 1000., 1500., 1600., 1700., 1800.,
			1850., 1900., 1920., 1940., 1950., 1960., 1970.,
			1973., 1973.1, 1973.5, 1980., 1990., 2000., 2010., 2019.,
			2020., 2023., 2025., 2026., 2026.25, 2026.3, 2030., 2036.,
			2040., 2050., 2100.
		};
		System.out.printf("%-10s %-14s %-30s%n", "Year", "ΔT (s)", "Source");
		for(final double y : testYears){
			final double dt = deltaTSeconds(y);
			final String source;
			if(hasIERSCoverage(y))
				source = "IERS (observed)";
			else if(hasSplineCoverage(y))
				source = "S/M/H spline";
			else
				source = "Espenak-Meeus";
			System.out.printf("%-10.4f %-14.4f %-30s%n", y, dt, source);
		}

		// Continuity checks across every boundary.
		System.out.println();
		checkContinuity("Spline start", SPLINE_START);
		if(!IERS_SAMPLES.isEmpty())
			checkContinuity("IERS start", IERS_START);
		if(!IERS_SAMPLES.isEmpty())
			checkContinuity("IERS end", IERS_END);
	}

	private static void checkContinuity(final String label, final double yB){
		final double before = deltaTSeconds(yB - 1-e-6);
		final double after = deltaTSeconds(yB + 1-e-6);
		System.out.printf("%-14s at %.4f: before=%.6f s, after=%.6f s, jump=%.2e s%n",
			label, yB, before, after, Math.abs(after - before));
	}

}
