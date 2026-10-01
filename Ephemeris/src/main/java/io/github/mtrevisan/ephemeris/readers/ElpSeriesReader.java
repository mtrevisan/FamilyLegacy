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

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Reader for the 14 ELP/MPP02 coefficient files distributed with the
 * {@code ytliu0/ElpMpp02} C++ implementation.
 *
 * <p>The files come in two flavours:</p>
 *
 * <ul>
 *   <li><b>Main problem</b> ({@code elp_main.long}, {@code elp_main.lat},
 *       {@code elp_main.dist}): each data line holds four integer
 *       multipliers of the Delaunay arguments, followed by seven
 *       floating-point values {@code A, B1, B2, B3, B4, B5, B6}. The
 *       effective amplitude is computed from these values using the
 *       correction factors in {@link ElpParameters}:</li>
 * </ul>
 * <pre>
 *   À = fA·A + fB1·B1 + fB2·B2 + fB3·B3 + fB4·B4 + fB5·B5
 * </pre>
 *
 * <ul>
 *   <li><b>Perturbations</b> ({@code elp_pert.*}): each data line holds
 *       thirteen integer multipliers of the fundamental arguments,
 *       followed by two floating-point values: the amplitude and the
 *       initial phase.</li>
 * </ul>
 *
 * <p>The first line of every file is the number of terms in that file.</p>
 */
public final class ElpSeriesReader{

	/** Number of Delaunay multipliers in a main-problem term. */
	static final int MAIN_ARG_COUNT = 4;
	/** Number of fundamental-argument multipliers in a perturbation term. */
	static final int PERT_ARG_COUNT = 13;


	private ElpSeriesReader(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Reads one of the three main-problem files.
	 *
	 * @param input  the file to read
	 * @param params the parameter set providing the correction factors
	 * @return the corrected list of main-problem terms
	 */
	public static List<ElpCoefficients.MainTerm> readMain(final InputStream input, final ElpParameters params)
			throws IOException{
		final List<ElpCoefficients.MainTerm> out = new ArrayList<>();
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))){
			final int count = readCount(reader);
			for(int i = 0; i < count; i ++){
				final String line = reader.readLine();
				if(line == null || line.isBlank())
					break;

				final String[] tokens = StringUtils.split(line);
				if(tokens.length < 11)
					continue;

				final int i1 = Integer.parseInt(tokens[0]);
				final int i2 = Integer.parseInt(tokens[1]);
				final int i3 = Integer.parseInt(tokens[2]);
				final int i4 = Integer.parseInt(tokens[3]);
				final double a = Double.parseDouble(tokens[4]);
				final double b1 = Double.parseDouble(tokens[5]);
				final double b2 = Double.parseDouble(tokens[6]);
				final double b3 = Double.parseDouble(tokens[7]);
				final double b4 = Double.parseDouble(tokens[8]);
				final double b5 = Double.parseDouble(tokens[9]);
				// B6 is read but not used in the computation.
				final double amplitude = params.fA * a
					+ params.fB1 * b1 + params.fB2 * b2 + params.fB3 * b3
					+ params.fB4 * b4 + params.fB5 * b5;
				out.add(new ElpCoefficients.MainTerm(i1, i2, i3, i4, amplitude));
			}
		}
		return out;
	}


	/**
	 * Reads one of the eleven perturbation files.
	 *
	 * @param input the file to read
	 * @return the list of perturbation terms
	 */
	public static List<ElpCoefficients.PertTerm> readPert(final InputStream input) throws IOException{
		final List<ElpCoefficients.PertTerm> out = new ArrayList<>();
		try(final BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))){
			final int count = readCount(reader);
			for(int i = 0; i < count; i ++){
				final String line = reader.readLine();
				if(line == null || line.isBlank())
					break;

				final String[] tokens = StringUtils.split(line);
				if(tokens.length < PERT_ARG_COUNT + 2)
					continue;

				final int[] multipliers = new int[PERT_ARG_COUNT];
				for(int j = 0; j < PERT_ARG_COUNT; j ++)
					multipliers[j] = Integer.parseInt(tokens[j]);
				final double amplitude = Double.parseDouble(tokens[PERT_ARG_COUNT]);
				final double phase = Double.parseDouble(tokens[PERT_ARG_COUNT + 1]);
				out.add(new ElpCoefficients.PertTerm(multipliers, amplitude, phase));
			}
		}
		return out;
	}


	/**
	 * Convenience overload that opens a file from the filesystem.
	 */
	public static List<ElpCoefficients.MainTerm> readMain(final Path path, final ElpParameters params)
			throws IOException{
		try(final InputStream in = Files.newInputStream(path)){
			return readMain(in, params);
		}
	}

	/**
	 * Convenience overload that opens a file from the filesystem.
	 */
	public static List<ElpCoefficients.PertTerm> readPert(final Path path) throws IOException{
		try(final InputStream in = Files.newInputStream(path)){
			return readPert(in);
		}
	}


	/* ======================================================================
	 *                          Internals
	 * ====================================================================== */

	private static int readCount(final BufferedReader reader) throws IOException{
		String line;
		while((line = reader.readLine()) != null){
			final String trimmed = line.trim();
			if(!trimmed.isEmpty())
				return Integer.parseInt(trimmed);
		}
		return 0;
	}

}
