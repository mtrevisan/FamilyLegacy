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
	 * Descriptor flags to identify the targeted spatial celestial body for horizon events.
	 */
	enum CelestialBody{
		SUN,
		MOON
	}


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

	/**
	 * Computes the apparent ecliptic longitude of the Sun for a given Universal Time instant.
	 * Accounts for VSOP2013 planetary perturbations, annual aberration, and IAU 2000A nutation.
	 *
	 * @param jdUT the Julian Date in Universal Time (UT/UTC) scale
	 * @return the apparent solar longitude in radians, reduced to the interval [0, 2π)
	 */
	double calculateApparentSunLongitude(double jdUT);

	/**
	 * Computes the exact Julian Date (UT) of a horizon crossing event for a specific body, day and location.
	 * Consolidates parallax corrections, refraction, and dynamical semidiameter variations polimorphically.
	 *
	 * @param body         the targeted celestial body (SUN or MOON)
	 * @param midnightUtJd the Julian Date representing local civil midnight converted to UT (ending in .5)
	 * @param latitude     the observer's geodetic latitude in degrees (North positive)
	 * @param longitude    the observer's geodetic longitude in degrees (East positive)
	 * @return the Julian Date (UT) of the horizon event, or {@code Double.NaN} if the body is circumpolar
	 */
	double calculateHorizonEvent(CelestialBody body, double midnightUtJd, double latitude, double longitude);

}
