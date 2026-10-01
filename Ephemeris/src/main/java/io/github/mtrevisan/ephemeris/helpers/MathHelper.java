/**
 * Copyright (c) 2023 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
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


public final class MathHelper{

	public static final double TWO_PI = StrictMath.PI * 2.;


	private MathHelper(){}


	public static double frac(final double x){
		return x - (int)x;
	}

	/**
	 * Reduce a value to the range {@code 0} to {@code max}.
	 */
	public static double mod(final double x, final double max){
		return (x % max + max) % max;
	}

	/**
	 * Reduce a value to the range {@code min} to {@code max}.
	 */
	public static double mod(final double x, final double min, final double max){
		final double normalized = (x - min) % (max - min);
		return (normalized >= 0.? normalized + min: normalized + max);
	}

	/**
	 * Reduce an angle in radians to the range {@code 0} to {@code 2π}.
	 */
	public static double mod2pi(final double angle){
		return (angle % TWO_PI + TWO_PI) % TWO_PI;
	}

	/**
	 * Reduce an angle in radians to the range {@code -π} to {@code π}.
	 */
	public static double modpipi(final double angle){
		return (angle + StrictMath.PI) % TWO_PI - StrictMath.PI;
	}

	public static double toDegrees(final int degree, final int minute, final double second){
		return degree + (minute + second / JulianDate.MINUTES_PER_HOUR) / JulianDate.MINUTES_PER_HOUR;
	}

	public static double limitRangeHour(double degree){
		degree %= 24;
		return (degree < 0.? degree + JulianDate.HOURS_PER_DAY: degree);
	}

	public static double limitRangeDay(double value){
		value %= 1;
		return (value < 0.? value + 1.: value);
	}

	public static double degToHrs(final double degrees){
		return degrees / JulianDate.DEGREES_PER_HOUR;
	}

	/**
	 * Use Horner's method to compute and return the polynomial evaluated at {@code x}:<br/>
	 * {@code p[0] + p[1] * x^1 + p[2] * x^2 + ... + p[n-1] * x^n-1}
	 *
	 * @param x	The value at which to calculate the polynomial.
	 * @param p	The polynomial coefficients.
	 * @return	The value of the polynomial.
	 */
	public static double polynomial(final double x, final double[] p){
		double result = 0.;
		for(int i = p.length - 1; i >= 0; i --)
			result = p[i] + result * x;
		return result;
	}

}
