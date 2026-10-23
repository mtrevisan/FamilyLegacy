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
package io.github.mtrevisan.calendars.liturgical;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Dates expressed through the liturgical year (see {@link LiturgicalDateParser} for the accepted expressions).
 * <ul>
 *   <li>{@link #WESTERN}: the Catholic usage, with the Julian calendar and computus up to 4 October 1582 and the
 *   Gregorian ones from 15 October 1582;</li>
 *   <li>{@link #JULIAN}: the Julian calendar and computus, as in the Orthodox churches, and in the Protestant countries
 *   before their own Gregorian reform (e.g. England up to 1752).</li>
 * </ul>
 */
public class LiturgicalCalendarStrategy implements CalendarConverterPlugin{

	/** Catholic usage: Julian calendar and computus up to 1582, Gregorian afterwards. */
	public static final LiturgicalCalendarStrategy WESTERN = new LiturgicalCalendarStrategy(CalendarReform.ROME);
	/** Julian calendar and computus. */
	public static final LiturgicalCalendarStrategy JULIAN = new LiturgicalCalendarStrategy(null);

	/** The first Sunday of Advent is the Sunday from 27 November to 3 December. */
	private static final int ADVENT_EARLIEST_DAY = 27;
	private static final int SUNDAY = 6;
	private static final int DAYS_IN_WEEK = 7;


	/** The Gregorian reform in force, or {@code null} for the Julian calendar. */
	private final CalendarReform reform;


	private LiturgicalCalendarStrategy(final CalendarReform reform){
		this.reform = reform;
	}


	@Override
	public long parseToJdn(final String rawDate){
		final LiturgicalDateParser.LiturgicalDate date = LiturgicalDateParser.parse(rawDate);
		long jdn = feastJdn(date.feast(), date.year());

		// apply the relative positions from the one nearest to the feast outwards
		final List<LiturgicalDateParser.Shift> shifts = date.shifts();
		for(int i = shifts.size() - 1; i >= 0; i --)
			jdn = apply(shifts.get(i), jdn);
		return jdn;
	}

	/**
	 * Computes the Julian Day Number of a feast in a year.
	 *
	 * @param feast the feast
	 * @param year  the year
	 * @return the Julian Day Number of the feast
	 */
	public long feastJdn(final Feast feast, final int year){
		return switch(feast.kind()){
			case FIXED -> {
				final boolean shifted = (feast.leapShift() && isLeapYear(year));
				yield civilJdn(year, feast.month(), feast.day() + (shifted? 1: 0));
			}
			case EASTER -> easterJdn(year) + feast.offset();
			case ADVENT -> adventSundayJdn(year) + feast.offset();
		};
	}

	/**
	 * @param year the year
	 * @return the Julian Day Number of Easter Sunday
	 */
	public long easterJdn(final int year){
		return (reform != null? Computus.easterJdn(year, reform): Computus.julianEasterJdn(year));
	}

	private long adventSundayJdn(final int year){
		final long earliest = civilJdn(year, 11, ADVENT_EARLIEST_DAY);
		return earliest + Math.floorMod(SUNDAY - weekday(earliest), DAYS_IN_WEEK);
	}

	private long civilJdn(final int year, final int month, final int day){
		return (reform != null? reform.toJdn(year, month, day): JulianCalendarStrategy.toJdn(year, month, day));
	}

	private boolean isLeapYear(final int year){
		return (reform != null? reform.isLeapYear(year): Math.floorMod(year, 4) == 0);
	}

	private static long apply(final LiturgicalDateParser.Shift shift, final long jdn){
		final int count = shift.count();
		return switch(shift.kind()){
			case DAYS -> jdn + shift.days();
			case WEEKDAY_AFTER -> {
				final int delta = Math.floorMod(shift.weekday() - weekday(jdn), DAYS_IN_WEEK);
				yield jdn + (delta == 0? DAYS_IN_WEEK: delta) + (long)DAYS_IN_WEEK * (count - 1);
			}
			case WEEKDAY_BEFORE -> {
				final int delta = Math.floorMod(weekday(jdn) - shift.weekday(), DAYS_IN_WEEK);
				yield jdn - (delta == 0? DAYS_IN_WEEK: delta) - (long)DAYS_IN_WEEK * (count - 1);
			}
			case WEEKDAY_FROM -> jdn + Math.floorMod(shift.weekday() - weekday(jdn), DAYS_IN_WEEK)
				+ (long)DAYS_IN_WEEK * (count - 1);
		};
	}

	/**
	 * @param jdn the Julian Day Number
	 * @return the weekday, from {@code 0} (Monday) to {@code 6} (Sunday)
	 */
	private static int weekday(final long jdn){
		return (int)Math.floorMod(jdn, DAYS_IN_WEEK);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		// a liturgical date always identifies a day
		LiturgicalDateParser.parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", true);
		components.put("month", true);
		components.put("day", true);
		return new CalendarType.CalendarMetadata(components, true);
	}

}
