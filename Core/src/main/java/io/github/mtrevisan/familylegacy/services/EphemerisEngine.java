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
package io.github.mtrevisan.familylegacy.services;


/**
 * Core interface for raw lunisolar astronomical event tracking.
 * Operates purely on Julian Dates (Universal Time) and spatial positions.
 */
public interface EphemerisEngine{

	/**
	 * Returns whether the astronomical calculation provider is fully functional.
	 */
	boolean isAvailable();

	/**
	 * Computes the exact Julian Date (UT) of the New Moon conjunction closest to a target date.
	 * Condition: Apparent Moon Longitude - Apparent Sun Longitude == 0
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @return the exact Julian Date UT of the conjunction
	 */
	double findNextNewMoon(double approximateJdUT);

	/**
	 * Computes the exact Julian Date (UT) when the Sun reaches a specific apparent longitude.
	 *
	 * @param approximateJdUT the starting guess in Julian Date UT
	 * @param targetLongitude  the target apparent solar longitude [rad] in the interval [0, 2π)
	 * @return the exact Julian Date UT of the event
	 */
	double findSolarLongitudeEvent(double approximateJdUT, double targetLongitude);

}
