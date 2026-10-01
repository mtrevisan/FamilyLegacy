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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public final class ResourceReader{

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

		private int index;
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

	public static final class VSOP2013Data{
		public int timePower;
		public VSOP2013Coeffs[] coeffs;
	}

	public static final class VSOP2013Coeffs{
		public final int[] iphi = new int[17];
		public double sine;
		public double cosine;
	}


	public static Map<VariableIndex, List<VSOP2013Data>> readData(final String filename) throws IOException{
		final ClassLoader classLoader = ResourceReader.class.getClassLoader();
		final InputStream is = classLoader.getResourceAsStream(filename);
		if(is == null)
			throw new IllegalArgumentException("file not found! " + filename);

		final Map<VariableIndex, List<VSOP2013Data>> datas = new HashMap<>();
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
				data.coeffs = new VSOP2013Coeffs[count];
				for(int i = 0; i < count; i ++){
					line = reader.readLine();

					final VSOP2013Coeffs coeffs = new VSOP2013Coeffs();
					coeffs.iphi[0] = Integer.parseInt(line.substring(5, 9).trim());
					coeffs.iphi[1] = Integer.parseInt(line.substring(9, 12).trim());
					coeffs.iphi[2] = Integer.parseInt(line.substring(12, 15).trim());
					coeffs.iphi[3] = Integer.parseInt(line.substring(15, 18).trim());

					coeffs.iphi[4] = Integer.parseInt(line.substring(18, 22).trim());
					coeffs.iphi[5] = Integer.parseInt(line.substring(22, 25).trim());
					coeffs.iphi[6] = Integer.parseInt(line.substring(25, 28).trim());
					coeffs.iphi[7] = Integer.parseInt(line.substring(28, 31).trim());
					coeffs.iphi[8] = Integer.parseInt(line.substring(31, 34).trim());

					coeffs.iphi[9] = Integer.parseInt(line.substring(34, 39).trim());
					coeffs.iphi[10] = Integer.parseInt(line.substring(39, 43).trim());
					coeffs.iphi[11] = Integer.parseInt(line.substring(43, 47).trim());
					coeffs.iphi[12] = Integer.parseInt(line.substring(47, 51).trim());

					coeffs.iphi[13] = Integer.parseInt(line.substring(51, 58).trim());

					coeffs.iphi[14] = Integer.parseInt(line.substring(58, 62).trim());
					coeffs.iphi[15] = Integer.parseInt(line.substring(62, 65).trim());
					coeffs.iphi[16] = Integer.parseInt(line.substring(65, 68).trim());

					final String sinMantissa = line.substring(68, 88).trim();
					final int sinExp = Integer.parseInt(line.substring(88, 92).trim());
					coeffs.sine = Double.parseDouble(sinMantissa + "e" + sinExp);

					final String cosMantissa = line.substring(92, 112).trim();
					final int cosExp = Integer.parseInt(line.substring(112, 116).trim());
					coeffs.cosine = Double.parseDouble(cosMantissa + "e" + cosExp);

					data.coeffs[i] = coeffs;
				}

				datas.computeIfAbsent(variable, v -> new ArrayList<>(1))
					.add(data);
			}

			return datas;
		}
	}

	public static Map<String, List<double[]>> read(final String filename) throws IOException{
		final ClassLoader classLoader = ResourceReader.class.getClassLoader();
		final InputStream is = classLoader.getResourceAsStream(filename);
		if(is == null)
			throw new IllegalArgumentException("file not found! " + filename);

		try(
			final InputStreamReader sr = new InputStreamReader(is, StandardCharsets.UTF_8);
			final BufferedReader reader = new BufferedReader(sr)){
			final Map<String, List<double[]>> result = new HashMap<>(0);
			String key = null;
			String line;
			List<double[]> values = new LinkedList<>();
			while((line = reader.readLine()) != null){
				if(line.isEmpty()){
					result.put(key, values);
					key = null;

					continue;
				}
				if(key == null){
					key = line;
					values = new LinkedList<>();

					continue;
				}

				final String[] parameters = StringUtils.split(line, ' ');
				final double[] array = new double[parameters.length];
				for(int i = 0; i < parameters.length; i ++)
					array[i] = Double.parseDouble(parameters[i]);
				values.add(array);
			}
			if(key != null)
				result.put(key, values);
			return result;
		}
	}

	public static List<double[]> readPlain(final String filename) throws IOException{
		final ClassLoader classLoader = ResourceReader.class.getClassLoader();
		final InputStream is = classLoader.getResourceAsStream(filename);
		if(is == null)
			throw new IllegalArgumentException("file not found! " + filename);

		try(
			final InputStreamReader sr = new InputStreamReader(is, StandardCharsets.UTF_8);
			final BufferedReader reader = new BufferedReader(sr)){
			String line;
			List<double[]> values = new LinkedList<>();
			while((line = reader.readLine()) != null){
				final String[] parameters = StringUtils.split(line, ' ');
				final double[] array = new double[parameters.length];
				for(int i = 0; i < parameters.length; i ++)
					array[i] = Double.parseDouble(parameters[i]);
				values.add(array);
			}
			return values;
		}
	}

}
