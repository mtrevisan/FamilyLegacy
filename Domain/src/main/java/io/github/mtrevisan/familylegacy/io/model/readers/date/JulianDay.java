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
package io.github.mtrevisan.familylegacy.io.model.readers.date;


/**
 * Conversions between the Julian day number and the (proleptic) Gregorian calendar.
 */
public final class JulianDay{

	private JulianDay(){}


	/**
	 * Reverse Gregorian conversion (Fliegel-Van Flandern), returning {@code [year, month, day]}.
	 */
	public static int[] toGregorian(final long jdn){
		final long a = jdn + 32044;
		final long b = (4 * a + 3) / 146097;
		final long c = a - (146097 * b) / 4;
		final long d = (4 * c + 3) / 1461;
		final long e = c - (1461 * d) / 4;
		final long m = (5 * e + 2) / 153;
		final int day = (int)(e - (153 * m + 2) / 5 + 1);
		final int month = (int)(m + 3 - 12 * (m / 10));
		final int year = (int)(100 * b + d - 4800 + m / 10);
		return new int[]{year, month, day};
	}

}
