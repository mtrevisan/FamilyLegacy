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
package io.github.mtrevisan.calendars.dateparsing;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.SeasonSolver;

import java.time.LocalDate;


/**
 * The day of an equinox or of a solstice, for the calendars whose dates can be expressed through the seasons.
 * <p>
 * The astronomical engine finds the event nearest to a guess, so the guess has to fall within a few weeks of the event of
 * the intended year: it is anchored on the Gregorian month of the season (March, June, September, December) of the right
 * year, never on the middle of the calendar year (that is about half a year from two consecutive events of the same
 * season, and could select either).
 * </p>
 */
public final class SeasonalDates{

	/** The day of the month on which the seasons fall, give or take a day. */
	private static final int SEASON_DAY = 21;
	/** Julian Day Number of 1970-01-01. */
	private static final long UNIX_EPOCH_JDN = 2440588L;


	private SeasonalDates(){}


	/**
	 * The season of a year that begins in autumn (or soon after): the autumn and the winter are in the Gregorian year in
	 * which the year begins, the spring and the summer in the following one.
	 *
	 * @param calendar        the name of the calendar, for the error message
	 * @param startGregorian  the Gregorian year in which the year of the calendar begins
	 * @param season          the season, as a {@link CalendarSeason} flag
	 * @return the Julian Day Number of the day of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long ofAutumnYear(final String calendar, final int startGregorian, final int season){
		final int year = (season == CalendarSeason.SEASON_AUTUMN || season == CalendarSeason.SEASON_WINTER
			? startGregorian
			: startGregorian + 1);
		return ofGregorianYear(calendar, year, season);
	}

	/**
	 * The season in a Gregorian year (the year of the calendar being the same one, or covering it).
	 *
	 * @param calendar  the name of the calendar, for the error message
	 * @param year      the Gregorian year of the event
	 * @param season    the season, as a {@link CalendarSeason} flag
	 * @return the Julian Day Number of the day of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long ofGregorianYear(final String calendar, final int year, final int season){
		return nearest(calendar, approximateJdn(year, season), season);
	}

	/**
	 * The first event of a season on or after the first day of a year (for the calendars whose year wanders through the
	 * seasons, as the Armenian and the Egyptian ones).
	 *
	 * @param calendar  the name of the calendar, for the error message
	 * @param startJdn  the Julian Day Number of the first day of the year
	 * @param season    the season, as a {@link CalendarSeason} flag
	 * @return the Julian Day Number of the day of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long onOrAfter(final String calendar, final long startJdn, final int season){
		final int year = LocalDate.ofEpochDay(startJdn - UNIX_EPOCH_JDN).getYear();
		final long first = approximateJdn(year, season);
		return nearest(calendar, (first >= startJdn? first: approximateJdn(year + 1, season)), season);
	}

	/**
	 * The event of a season nearest to a day.
	 *
	 * @param calendar         the name of the calendar, for the error message
	 * @param approximateJdn   the Julian Day Number near the event (within a couple of months)
	 * @param season           the season, as a {@link CalendarSeason} flag
	 * @return the Julian Day Number of the day of the event
	 * @throws UnsupportedOperationException if the Ephemeris plugin is not installed
	 */
	public static long nearest(final String calendar, final long approximateJdn, final int season){
		return SeasonSolver.jdnNear(approximateJdn, season, calendar);
	}

	/**
	 * @param year   the Gregorian year
	 * @param season the season, as a {@link CalendarSeason} flag
	 * @return the Julian Day Number of about the day of the season (the 21st of the Gregorian month of the season)
	 */
	public static long approximateJdn(final int year, final int season){
		return GregorianCalendarStrategy.toJdn(year, CalendarSeason.gregorianSeasonMonth(season), SEASON_DAY);
	}

}
