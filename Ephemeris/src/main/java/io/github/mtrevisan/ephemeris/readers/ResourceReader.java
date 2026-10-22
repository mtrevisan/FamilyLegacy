/**
 * Copyright (c) 2023 Mauro Trevisan
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public final class ResourceReader{

	/**
	 * Pre-compiled powers of 10 array to optimize scientific floating-point scaling.
	 */
	private static final double[] POWERS_OF_10;

	static{
		POWERS_OF_10 = new double[700];
		for(int i = 0; i < POWERS_OF_10.length; i ++)
			POWERS_OF_10[i] = StrictMath.pow(10., i - 350);
	}


	private ResourceReader(){}


	public enum VariableIndex{
		//semi-major axis [UA]
		A(1),
		//mean longitude [rad]
		L(2),
		//e * cos(w_bar), where e is the eccentricity, and w_bar = W + w is the longitude of the perihelion
		K(3),
		//e * sin(w_bar), where e is the eccentricity, and w_bar = W + w is the longitude of the perihelion
		H(4),
		//sin(i/2) * cos(W), where i is the inclination, and W is the longitude of the ascending node
		Q(5),
		//sin(i/2) * sin(W), where i is the inclination, and W is the longitude of the ascending node
		P(6);

		private final int index;

		VariableIndex(final int index){
			this.index = index;
		}

		public static VariableIndex of(final int idx){
			for(final VariableIndex vi : values())
				if(vi.index == idx)
					return vi;
			return null;
		}
	}

	/**
	 * A VSOP2013 series stored in flat primitive arrays.
	 * <p>
	 * Only the non-zero argument multipliers are kept: the multipliers of term {@code j} are
	 * {@code argumentMultiplier[k]} applied to argument {@code argumentIndex[k]}, for
	 * {@code k} in {@code [argumentStart[j], argumentStart[j + 1])}.
	 * </p>
	 */
	public static final class VSOP2013Data{
		public int timePower;
		public double[] sines;
		public double[] cosines;
		public int[] argumentStart;
		public byte[] argumentIndex;
		public int[] argumentMultiplier;
	}

	/** Column boundaries of the 17 argument multipliers in a VSOP2013 term line. */
	private static final int[] MULTIPLIER_COLUMNS = {5, 9, 12, 15, 18, 22, 25, 28, 31, 34, 39, 43, 47, 51, 58, 62, 65, 68};


	/**
	 * Parses a planetary perturbation text file into a contextual multi-variable data layout.
	 * Utilizes zero-allocation loops for all fixed-width integer and scientific double properties.
	 *
	 * @param filename the relative classpath data file path
	 * @return an EnumMap pairing variable indexes to parsed dataset lists
	 * @throws IOException if a stream error occurs during ingestion
	 */
	public static Map<VariableIndex, List<VSOP2013Data>> readData(final String filename) throws IOException{
		final ClassLoader classLoader = ResourceReader.class.getClassLoader();
		final InputStream is = classLoader.getResourceAsStream(filename);
		if(is == null)
			throw new IllegalArgumentException("file not found! " + filename);

		final Map<VariableIndex, List<VSOP2013Data>> datas = new EnumMap<>(VariableIndex.class);
		try(
			final InputStreamReader sr = new InputStreamReader(is, StandardCharsets.UTF_8);
			final BufferedReader reader = new BufferedReader(sr)){
			String line;
			while((line = reader.readLine()) != null){
				if(line.isBlank())
					continue;

				final String[] header = StringUtils.split(line);

				final VSOP2013Data data = new VSOP2013Data();
				final VariableIndex variable = VariableIndex.of(Integer.parseInt(header[2]));
				data.timePower = Integer.parseInt(header[3]);
				final int count = Integer.parseInt(header[4]);
				data.sines = new double[count];
				data.cosines = new double[count];
				data.argumentStart = new int[count + 1];
				final int maxArguments = count * (MULTIPLIER_COLUMNS.length - 1);
				final byte[] argumentIndex = new byte[maxArguments];
				final int[] argumentMultiplier = new int[maxArguments];
				int arguments = 0;
				for(int i = 0; i < count; i ++){
					line = reader.readLine();

					data.argumentStart[i] = arguments;
					for(int k = 0; k < MULTIPLIER_COLUMNS.length - 1; k ++){
						final int multiplier = parseIntFast(line, MULTIPLIER_COLUMNS[k], MULTIPLIER_COLUMNS[k + 1]);
						if(multiplier != 0){
							argumentIndex[arguments] = (byte)k;
							argumentMultiplier[arguments] = multiplier;
							arguments ++;
						}
					}

					data.sines[i] = parseScientificDoubleFast(line, 68, 88, 88, 92);
					data.cosines[i] = parseScientificDoubleFast(line, 92, 112, 112, 116);
				}
				data.argumentStart[count] = arguments;
				data.argumentIndex = Arrays.copyOf(argumentIndex, arguments);
				data.argumentMultiplier = Arrays.copyOf(argumentMultiplier, arguments);

				datas.computeIfAbsent(variable, v -> new ArrayList<>(20))
					.add(data);
			}

			return datas;
		}
	}

	/**
	 * Scans a fixed-width string slice converting character indices directly to primitive intergers.
	 * Mimics a virtual trim layer and checks arithmetic sign parameters safely.
	 */
	private static int parseIntFast(final String s, final int start, final int end){
		int result = 0;
		boolean negative = false;
		int i = start;

		// Skip leading whitespace characters (Virtual Trim)
		while(i < end && s.charAt(i) == ' ')
			i ++;
		if(i >= end)
			return 0;

		// Evaluate arithmetic sign configuration
		final char firstChar = s.charAt(i);
		if(firstChar == '-'){
			negative = true;

			i ++;
		}
		else if(firstChar == '+')
			i ++;

		// Construct the primitive base integer safely
		while(i < end){
			final char c = s.charAt(i);
			if(c >= '0' && c <= '9')
				result = result * 10 + (c - '0');
			else if(c == ' ')
				// Break if encountering standard column trailing spaces
				break;
			i ++;
		}

		return (negative? -result: result);
	}

	/**
	 * Parses scientific notations (Mantissa text segment combined with an Exponent segment)
	 * without performing string concatenations or using Double.parseDouble.
	 */
	private static double parseScientificDoubleFast(final String s,  final int mStart, final int mEnd, final int eStart,
			final int eEnd){
		long mantissa = 0;
		boolean mNegative = false;
		int mDecPlaces = -1;
		int i = mStart;

		// Virtual trim for the mantissa window
		while(i < mEnd && s.charAt(i) == ' ')
			i ++;

		if(i < mEnd){
			final char c = s.charAt(i);
			if(c == '-'){
				mNegative = true;

				i ++;
			}
			else if(c == '+')
				i ++;
		}

		// Process mantissa integer chain while recording the implicit decimal point position
		while(i < mEnd){
			final char c = s.charAt(i);
			if(c >= '0' && c <= '9'){
				mantissa = mantissa * 10 + (c - '0');
				if(mDecPlaces >= 0)
					mDecPlaces ++;
			}
			else if(c == '.')
				mDecPlaces = 0;
			else if(c == ' ')
				break;

			i ++;
		}

		// Read the associated scientific exponent slice
		final int exponent = parseIntFast(s, eStart, eEnd);

		// Rebalance the exponent tracker by subtracting the counted decimal fraction places
		final int finalExponent = exponent - Math.max(mDecPlaces, 0);

		double value = mantissa;
		// Lookup the pre-compiled floating-point scaling multiplier (Index 350 centers the zero-offset)
		final int lookupIndex = finalExponent + 350;
		if(lookupIndex >= 0 && lookupIndex < POWERS_OF_10.length)
			value *= POWERS_OF_10[lookupIndex];
		else
			value *= StrictMath.pow(10., finalExponent);

		return (mNegative? -value: value);
	}

}
