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

import java.util.ArrayList;
import java.util.List;


/**
 * Narrows the date of birth of a person from several statements of the age (a death record, a census, a marriage...): the birth
 * must fall in the intersection of what each of them allows.
 */
public final class AgeCluesTool{

	/**
	 * An age stated at an event.
	 *
	 * @param event       the date of the event
	 * @param years       the age in years
	 * @param months      the months of the age, or {@code null} if not stated
	 * @param days        the days of the age, or {@code null} if not stated
	 * @param ordinalYear {@code true} if the age is the ordinal of the year of life ("in his 72nd year")
	 * @param eventName   the name of the event (e.g. "death"), for the explanation
	 */
	public record Clue(DayRange event, int years, Integer months, Integer days, boolean ordinalYear, String eventName){}


	private AgeCluesTool(){}


	/**
	 * Intersects the dates of birth that the clues allow.
	 *
	 * @param clues at least two statements of the age of the same person
	 * @return the estimated birth (a date or a range), or an informative suggestion if the clues contradict one another
	 * @throws IllegalArgumentException if fewer than two clues are given, or an age is not valid
	 */
	public static List<DateSuggestion> intersect(final List<Clue> clues){
		if(clues.size() < 2)
			throw new AssistantInputException("date_assistant.error.clues_few");

		final int count = clues.size();
		final long[] lowers = new long[count];
		final long[] uppers = new long[count];
		final List<Message> steps = new ArrayList<>();
		long narrowest = Long.MAX_VALUE;
		for(int i = 0; i < count; i ++){
			final Clue clue = clues.get(i);
			final DayRange event = clue.event();
			final long[] window = AgeEstimator.birthWindow(event.minJdn(), event.maxJdn(), event.countingCalendar(), clue.years(),
				clue.months(), clue.days(), clue.ordinalYear());
			lowers[i] = window[0];
			uppers[i] = window[1];
			narrowest = Math.min(narrowest, window[1] - window[0] + 1);

			final Message age = AgeEstimator.ageMessage(clue.ordinalYear()? clue.years() - 1: clue.years(), clue.months(), clue.days());
			final String earliest = CivilDates.format(window[0], event.countingCalendar());
			final String latest = CivilDates.format(window[1], event.countingCalendar());
			steps.add(window[0] == window[1]
				? Message.of("date_assistant.step.clue_day", i + 1, clue.eventName(), event.label(), age, earliest)
				: Message.of("date_assistant.step.clue", i + 1, clue.eventName(), event.label(), age, earliest, latest));
		}

		int lowestLimit = 0;
		int highestLimit = 0;
		for(int i = 1; i < count; i ++){
			if(lowers[i] > lowers[lowestLimit])
				lowestLimit = i;
			if(uppers[i] < uppers[highestLimit])
				highestLimit = i;
		}
		final long lower = lowers[lowestLimit];
		final long upper = uppers[highestLimit];

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.clues_same_person"));
		assumptions.add(Message.of("date_assistant.assumption.age_rounded"));

		if(lower > upper){
			steps.add(Message.of("date_assistant.step.clue_conflict", lowestLimit + 1, CivilDates.format(lower,
				CivilDates.calendarInForce(lower, CalendarReform.ROME)), highestLimit + 1,
				CivilDates.format(upper, CivilDates.calendarInForce(upper, CalendarReform.ROME)),
				lower - upper));
			return List.of(new DateSuggestion(Message.of("date_assistant.title.clues_conflict"),
				Message.of("date_assistant.meaning.clues_conflict", count), assumptions, null, steps, null));
		}

		final String calendar = CivilDates.calendarInForce(lower, CalendarReform.ROME);
		final String earliest = CivilDates.format(lower, calendar);
		final String latest = CivilDates.format(upper, calendar);
		steps.add(Message.of("date_assistant.step.clues_narrowing", narrowest, upper - lower + 1));
		steps.add(lower == upper
			? Message.of("date_assistant.step.written_plain", lower, Message.calendar(calendar), earliest)
			: Message.of("date_assistant.step.written_span", lower, upper, Message.calendar(calendar), earliest, latest));

		final Message meaning = Message.of("date_assistant.meaning.clues", count);
		if(lower == upper)
			return List.of(new DateSuggestion(Message.of("date_assistant.title.birth_date"), meaning, assumptions, calendar, steps,
				earliest));

		final int firstYear = CivilDates.fields(lower, calendar)[0];
		final int lastYear = CivilDates.fields(upper, calendar)[0];
		final Message birthYears = (firstYear == lastYear
			? Message.of("date_assistant.years.one", firstYear)
			: Message.of("date_assistant.years.two", firstYear, lastYear, lastYear - firstYear + 1));
		steps.add(Message.of("date_assistant.step.birth_falls", birthYears));
		return List.of(new DateSuggestion(Message.of("date_assistant.title.birth_range"),
			Message.of("date_assistant.meaning.born_range", meaning, birthYears), assumptions, calendar, steps, null, earliest, latest));
	}

}
