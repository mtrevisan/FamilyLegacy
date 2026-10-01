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
package io.github.mtrevisan.familylegacy.v2.services;


public interface AstronomicalEngine{

	/**
	 * Returns whether the astronomical calculation provider is fully functional.
	 */
	boolean isAvailable();

	/**
	 * Computes the JDN of the next new moon after a reference JDN.
	 *
	 * @param approxJdn approximate JDN (UT) from which the search starts
	 * @param utcOffset the UTC offset in hours at the target location
	 * @return JDN of the next new moon, rounded to the local day
	 */
	long getNextNewMoonJdn(final double approxJdn, final double utcOffset);

	/**
	 * Computes the JDN at which the Sun's apparent longitude reaches a
	 * target value within the given year.
	 *
	 * @param year          Gregorian year
	 * @param targetLongitude target apparent solar longitude [deg]
	 * @param utcOffset     UTC offset in hours at the target location
	 * @return JDN of the solar term, in the local timezone
	 */
	double getSolarLongitudeJdn(final int year, final double targetLongitude, final double utcOffset);

}
