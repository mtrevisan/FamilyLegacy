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
package io.github.mtrevisan.calendars.spi;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;


/**
 * Resolves a season (the instant of the equinox or solstice that starts it) into a Julian Day Number, so that the
 * calendars that accept a season in place of a month share the same code.
 */
public final class SeasonSolver{

	private SeasonSolver(){}


	/**
	 * Finds the day of the start of a season near an anchor.
	 *
	 * @param anchorJdUT   the Julian Date (UT) near the event, i.e. the middle of the anchor day (the first day of the
	 *                     month of the season, converted into the calendar, minus 0.5)
	 * @param season       the season ({@link CalendarSeason#SEASON_SPRING}, {@link CalendarSeason#SEASON_SUMMER},
	 *                     {@link CalendarSeason#SEASON_AUTUMN}, or {@link CalendarSeason#SEASON_WINTER})
	 * @param calendarName the name of the calendar, for the error message
	 * @return the Julian Day Number of the day when the season starts
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long jdn(final double anchorJdUT, final int season, final String calendarName){
		return jdnAtLongitude(anchorJdUT, CalendarSeason.getGeneralTargetLongitude(season), calendarName);
	}

	/**
	 * Finds the day when the Sun reaches a tropical longitude, for the calendars that define the seasons differently from
	 * {@link CalendarSeason} (the Chinese solar terms, the Indian Ritu).
	 *
	 * @param anchorJdUT      the Julian Date (UT) near the event
	 * @param targetLongitude the tropical longitude [rad]
	 * @param calendarName    the name of the calendar, for the error message
	 * @return the Julian Day Number of the day of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long jdnAtLongitude(final double anchorJdUT, final double targetLongitude, final String calendarName){
		return (long)Math.floor(instantAtLongitude(anchorJdUT, targetLongitude, calendarName) + 0.5);
	}

	/**
	 * Finds the instant when the Sun reaches a tropical longitude, for the calendars that must turn it into a civil day in
	 * their own time zone (the Chinese solar terms).
	 *
	 * @param anchorJdUT      the Julian Date (UT) near the event
	 * @param targetLongitude the tropical longitude [rad]
	 * @param calendarName    the name of the calendar, for the error message
	 * @return the Julian Date (UT) of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static double instantAtLongitude(final double anchorJdUT, final double targetLongitude, final String calendarName){
		final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
		if(!engine.isAvailable())
			throw new UnsupportedOperationException(calendarName + " calendar requires the Ephemeris plugin.");

		return engine.findSolarLongitudeEvent(anchorJdUT, targetLongitude);
	}

	/**
	 * Finds the day of the start of a season near an anchor day.
	 *
	 * @param anchorJdn    the Julian Day Number of the anchor day: the first day of the month of the season (see
	 *                     {@link CalendarSeason#gregorianSeasonMonth(int)}), converted by the calendar
	 * @param season       the season
	 * @param calendarName the name of the calendar, for the error message
	 * @return the Julian Day Number of the day when the season starts
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long jdnNear(final long anchorJdn, final int season, final String calendarName){
		return jdn(anchorJdn - 0.5, season, calendarName);
	}

}
