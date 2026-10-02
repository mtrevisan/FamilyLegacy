package io.github.mtrevisan.ephemeris;

/**
 * Core interface for raw lunisolar astronomical event tracking.
 * Operates purely on Julian Dates (Universal Time) and spatial positions.
 */
public interface EphemerisEngine{

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
	 * @param targetAngleRad  the target apparent solar longitude [rad] in the interval [0, 2π)
	 * @return the exact Julian Date UT of the event
	 */
	double findSolarLongitudeEvent(double approximateJdUT, double targetAngleRad);

	/**
	 * Retrieves the historical UTC hour offset for a specific Julian Date.
	 *
	 * @param jdUT the Julian Date (Universal Time) to evaluate
	 * @return the local time offset from UTC in decimal hours
	 */
	double getLocalTimeOffset(double jdUT);

}
