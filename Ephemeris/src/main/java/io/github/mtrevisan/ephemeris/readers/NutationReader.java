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

import io.github.mtrevisan.ephemeris.helpers.JulianDate;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public final class NutationReader{

	public static final class NutationTerm{
		public final int j; // 0 = constant term, 1 = linear term with time
		public final int[] arguments = new int[14]; // l, l', F, D, Om, L_Me, L_Ve, L_E, L_Ma, L_J, L_Sa, L_U, L_Ne, p_A
		public final double bSin; // B"_i (sin component)
		public final double bCos; // B_i (cos component)

		public NutationTerm(final int j, final int[] arguments, final double bSin, final double bCos){
			this.j = j;
			System.arraycopy(arguments, 0, this.arguments, 0, arguments.length);

			// Convert microarcseconds (uas) to radians
			final double uasToRad = StrictMath.PI / (180. * JulianDate.SECONDS_PER_HOUR * 1_000_000.);
			this.bSin = bSin * uasToRad;
			this.bCos = bCos * uasToRad;
		}
	}


	private NutationReader(){}


	public static List<NutationTerm> readIau2000a(final String filename) throws IOException{
		final ClassLoader classLoader = NutationReader.class.getClassLoader();
		final InputStream is = classLoader.getResourceAsStream(filename);
		if(is == null)
			throw new IllegalArgumentException("Nutation file not found: " + filename);

		final List<NutationTerm> terms = new ArrayList<>();
		try(final InputStreamReader sr = new InputStreamReader(is, StandardCharsets.UTF_8);
			 final BufferedReader reader = new BufferedReader(sr)){

			String line;
			int currentJ = 0; // Default to j=0 until specified
			while((line = reader.readLine()) != null){
				line = line.trim();

				// Detect section headers for j = 0 or j = 1
				if(line.startsWith("j =")){
					if(line.contains("j = 0"))
						currentJ = 0;
					else if(line.contains("j = 1"))
						currentJ = 1;

					continue;
				}

				// Skip comments, headers, dashes, and empty lines
				if(line.isEmpty() || line.startsWith("#") || line.startsWith("-") || line.startsWith("i")
						|| line.startsWith("("))
					continue;

				final String[] parts = StringUtils.split(line);
				if(parts.length < 17)
					continue;

				try{
					final double bSin = Double.parseDouble(parts[1]);
					final double bCos = Double.parseDouble(parts[2]);

					final int[] arguments = new int[14];
					for(int i = 0; i < 14; i ++)
						arguments[i] = Integer.parseInt(parts[3 + i]);

					terms.add(new NutationTerm(currentJ, arguments, bSin, bCos));
				}
				catch(final NumberFormatException ignored){
					// Skip lines that fail parsing (header artifacts)
				}
			}
		}
		return terms;
	}

}
