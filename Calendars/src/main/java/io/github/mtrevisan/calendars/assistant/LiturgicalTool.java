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
package io.github.mtrevisan.calendars.assistant;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.liturgical.Computus;

import java.util.ArrayList;
import java.util.List;


/** Easter, the movable feasts and the feast-day references. */
public final class LiturgicalTool{

	private record Movable(String name, int offset){}

	private static final List<Movable> MOVABLE_FEASTS = List.of(
		new Movable("Ash Wednesday", -46),
		new Movable("Palm Sunday", -7),
		new Movable("Maundy Thursday", -3),
		new Movable("Good Friday", -2),
		new Movable("Easter Sunday", 0),
		new Movable("Ascension", 39),
		new Movable("Pentecost", 49),
		new Movable("Trinity Sunday", 56),
		new Movable("Corpus Christi", 60));

	private static final int DAYS_IN_WEEK = 7;
	private static final int ADVENT_WEEKS_BEFORE_SUNDAY = 3;


	private LiturgicalTool(){}


	/**
	 * The movable feasts of a year, plus Epiphany and the first Sunday of Advent.
	 *
	 * @param year   the astronomical year
	 * @param reform the Gregorian reform in force, or {@code null} for the Julian calendar and computus (Orthodox usage)
	 * @return one suggestion per feast, in the order of the year
	 */
	public static List<DateSuggestion> feasts(final int year, final CalendarReform reform){
		final long easter = easterJdn(year, reform);
		final List<DateSuggestion> result = new ArrayList<>();
		result.add(feast(year, reform, "Epiphany", CivilDates.toJdn(year, 1, 6, reform), "fixed feast, 6 January", null));
		for(final Movable movable : MOVABLE_FEASTS)
			result.add(feast(year, reform, movable.name(), easter + movable.offset(),
				"Easter Sunday " + (movable.offset() >= 0? "+ ": "- ") + Math.abs(movable.offset()) + " days", easter));
		result.add(feast(year, reform, "First Sunday of Advent", advent(year, reform),
			"the fourth Sunday before Christmas", null));
		return result;
	}

	/**
	 * Resolves a feast-day reference (e.g. "Michaelmas 1498", "Giovedì Santo 1520", "tre giorni dopo Pasqua 1400").
	 *
	 * @param expression the expression, ending with the year, as accepted by the liturgical calendar
	 * @param reform     the Gregorian reform in force, or {@code null} for the Julian calendar and computus
	 * @return the suggestion
	 * @throws IllegalArgumentException if the expression cannot be parsed
	 */
	public static DateSuggestion resolveFeast(final String expression, final CalendarReform reform){
		final CalendarType type = (reform == null? CalendarType.LITURGICAL_JULIAN: CalendarType.LITURGICAL);
		final long jdn = type.parseToJdn(expression);
		return feast(null, reform, "\"" + expression + "\"", jdn, "liturgical expression \"" + expression + "\"", null);
	}

	/**
	 * A day at a distance from Easter.
	 *
	 * @param year   the astronomical year of Easter
	 * @param days   the days from Easter Sunday (negative for before)
	 * @param reform the Gregorian reform in force, or {@code null} for the Julian calendar and computus
	 * @return the suggestion
	 */
	public static DateSuggestion relativeToEaster(final int year, final long days, final CalendarReform reform){
		final long easter = easterJdn(year, reform);
		final long distance = Math.abs(days);
		final String title = distance + (distance == 1? " day ": " days ") + (days < 0? "before": "after") + " Easter " + year;
		return feast(year, reform, title, easter + days, "Easter Sunday " + (days >= 0? "+ ": "- ") + distance + " days", easter);
	}


	private static long easterJdn(final int year, final CalendarReform reform){
		return (reform == null? Computus.julianEasterJdn(year): Computus.easterJdn(year, reform));
	}

	private static long advent(final int year, final CalendarReform reform){
		final long christmas = CivilDates.toJdn(year, 12, 25, reform);
		//the Sunday before Christmas, then three weeks back
		final long sundayBefore = christmas - (CivilDates.weekday(christmas) + 1);
		return sundayBefore - (long)ADVENT_WEEKS_BEFORE_SUNDAY * DAYS_IN_WEEK;
	}

	private static DateSuggestion feast(final Integer year, final CalendarReform reform, final String name, final long jdn,
			final String rule, final Long easter){
		final String calendar = CivilDates.calendarInForce(jdn, reform);
		final String date = CivilDates.format(jdn, calendar);

		final List<String> assumptions = new ArrayList<>();
		assumptions.add(reform == null
			? "Julian calendar and Julian computus (Orthodox usage, or a Protestant country before its own reform)"
			: "Julian calendar and computus up to the Gregorian reform of " + reform.firstGregorianYear()
				+ ", Gregorian afterward");
		final List<String> steps = new ArrayList<>();
		if(easter != null){
			final String easterCalendar = CivilDates.calendarInForce(easter, reform);
			steps.add("Easter Sunday of " + year + " by the " + (CivilDates.JULIAN.equals(easterCalendar)? "Julian": "Gregorian")
				+ " computus: " + CivilDates.format(easter, easterCalendar) + " (Julian Day Number " + easter + ")");
		}
		steps.add("Rule: " + rule + ", Julian Day Number " + jdn);
		steps.add("Written in the " + calendar + " calendar: " + date + " (" + CivilDates.weekdayName(jdn) + ")");
		return new DateSuggestion(name, name + (year != null? " " + year: "") + " falls on " + date, assumptions, calendar, steps,
			date);
	}

}
