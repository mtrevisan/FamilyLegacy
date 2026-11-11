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

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.liturgical.Computus;
import io.github.mtrevisan.calendars.liturgical.LiturgicalCalendarStrategy;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/** Easter, the movable feasts and the feast-day references. */
public final class LiturgicalTool{

	private record Movable(String key, int offset){}

	private static final List<Movable> MOVABLE_FEASTS = List.of(
		new Movable("date_assistant.feast.ash_wednesday", -46),
		new Movable("date_assistant.feast.palm_sunday", -7),
		new Movable("date_assistant.feast.maundy_thursday", -3),
		new Movable("date_assistant.feast.good_friday", -2),
		new Movable("date_assistant.feast.easter_sunday", 0),
		new Movable("date_assistant.feast.ascension", 39),
		new Movable("date_assistant.feast.pentecost", 49),
		new Movable("date_assistant.feast.trinity_sunday", 56),
		new Movable("date_assistant.feast.corpus_christi", 60));

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
		result.add(feast(year, reform, Message.of("date_assistant.feast.epiphany"), CivilDates.toJdn(year, 1, 6, reform),
			Message.of("date_assistant.rule.epiphany"), null));
		for(final Movable movable : MOVABLE_FEASTS)
			result.add(feast(year, reform, Message.of(movable.key()), easter + movable.offset(), easterRule(movable.offset()), easter));
		result.add(feast(year, reform, Message.of("date_assistant.feast.advent"), advent(year, reform), Message.of("date_assistant.rule.advent"),
			null));
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
		// the strategy of the reform in force (not the Catholic one for every country)
		final long jdn = LiturgicalCalendarStrategy.forReform(reform).parseToJdn(expression);
		final String name = StringUtils.normalizeSpace(expression);
		return feast(null, reform, Message.text(name), jdn, Message.of("date_assistant.rule.expression", name), null);
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
		final Message title = Message.of(days < 0? "date_assistant.title.before_easter": "date_assistant.title.after_easter", distance, year);
		return feast(year, reform, title, easter + days, easterRule(days), easter);
	}


	private static Message easterRule(final long offset){
		return Message.of(offset >= 0? "date_assistant.rule.easter_plus": "date_assistant.rule.easter_minus", Math.abs(offset));
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

	private static DateSuggestion feast(final Integer year, final CalendarReform reform, final Message name, final long jdn,
			final Message rule, final Long easter){
		final String calendar = CivilDates.calendarInForce(jdn, reform);
		final String date = CivilDates.format(jdn, calendar);

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(reform == null
			? Message.of("date_assistant.assumption.liturgical_julian")
			: Message.of("date_assistant.assumption.liturgical_reform", reform.firstGregorianYear()));
		final List<Message> steps = new ArrayList<>();
		if(easter != null){
			final String easterCalendar = CivilDates.calendarInForce(easter, reform);
			steps.add(Message.of("date_assistant.step.easter", year, Message.calendar(easterCalendar), CivilDates.format(easter, easterCalendar),
				easter));
		}
		steps.add(Message.of("date_assistant.step.rule", rule, jdn));
		steps.add(Message.of("date_assistant.step.written", jdn, Message.calendar(calendar), date, CivilDates.dayOfWeek(jdn)));
		final Message meaning = (year != null
			? Message.of("date_assistant.meaning.feast_year", name, year, date)
			: Message.of("date_assistant.meaning.feast", name, date));
		return new DateSuggestion(name, meaning, assumptions, calendar, steps, date);
	}

}
