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

import java.util.ArrayList;
import java.util.List;


/** Adds or subtracts days or weeks to a date (e.g. "40 days after the birth"). */
public final class RelativeDateTool{

	/** The unit of the distance. */
	public enum Unit{
		DAYS(1, "date_assistant.distance.days"),
		WEEKS(7, "date_assistant.distance.weeks");

		private final int days;
		private final String distanceKey;

		Unit(final int days, final String distanceKey){
			this.days = days;
			this.distanceKey = distanceKey;
		}
	}


	private RelativeDateTool(){}


	/**
	 * Computes a date at a distance from another one.
	 *
	 * @param calendarCode the code of the calendar of the base date
	 * @param baseDate     the base date, in the notation of the calendar
	 * @param amount       the distance (not negative)
	 * @param unit         the unit of the distance
	 * @param after        {@code true} for a date after the base one, {@code false} for before
	 * @return the candidates (Julian and Gregorian)
	 * @throws IllegalArgumentException if the base date cannot be parsed
	 */
	public static List<DateSuggestion> offset(final String calendarCode, final String baseDate, final int amount, final Unit unit,
			final boolean after){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final long base = type.parseToJdn(baseDate);
		return offsetFromRange(base, base, Message.of("date_assistant.label.date_in_calendar", baseDate, Message.calendar(type.getCode())),
			amount, unit, after);
	}

	/**
	 * Computes a date at a distance from another one, that may be known only as a range (e.g. the date of an event that is
	 * approximate, bounded, or known to the year only).
	 *
	 * @param minJdn    the earliest Julian Day Number of the base date
	 * @param maxJdn    the latest Julian Day Number of the base date
	 * @param baseLabel the description of the base date, for the explanation
	 * @param amount    the distance (not negative)
	 * @param unit      the unit of the distance
	 * @param after     {@code true} for a date after the base one, {@code false} for before
	 * @return for a single day, the candidates (Julian and Gregorian); for a range, the range of the result
	 */
	public static List<DateSuggestion> offsetFromRange(final long minJdn, final long maxJdn, final Message baseLabel,
			final int amount, final Unit unit, final boolean after){
		if(amount < 0)
			throw new AssistantInputException("date_assistant.error.negative_distance", amount);

		final long days = (long)amount * unit.days;
		final long shift = (after? days: -days);

		final Message distance = Message.of(unit.distanceKey, amount);
		//the steps state the distance in days too, unless it is already in days
		final Message stepDistance = (unit.days == 1? distance: Message.of("date_assistant.distance.with_days", distance, days));
		final Message meaning = Message.of(after? "date_assistant.meaning.relative_after": "date_assistant.meaning.relative_before", distance,
			baseLabel);
		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.exclusive_count"));
		assumptions.add(Message.of("date_assistant.assumption.inclusive_count"));
		final List<Message> steps = new ArrayList<>();
		if(minJdn == maxJdn){
			steps.add(Message.of("date_assistant.step.base_date", baseLabel, minJdn));
			steps.add(Message.of(after? "date_assistant.step.added": "date_assistant.step.subtracted", stepDistance, minJdn + shift));
			return CalendarConversionTool.fromJdn(minJdn + shift, meaning, assumptions, steps);
		}

		assumptions.add(Message.of("date_assistant.assumption.range_base"));
		steps.add(Message.of("date_assistant.step.base_range", baseLabel, minJdn, maxJdn));
		steps.add(Message.of(after? "date_assistant.step.added_range": "date_assistant.step.subtracted_range", stepDistance,
			minJdn + shift, maxJdn + shift));
		final String calendar = CivilDates.calendarInForce(minJdn + shift, CalendarReform.ROME);
		final String notBefore = CivilDates.format(minJdn + shift, calendar);
		final String notAfter = CivilDates.format(maxJdn + shift, calendar);
		steps.add(Message.of("date_assistant.step.written_range", Message.calendar(calendar), notBefore, notAfter));
		return List.of(new DateSuggestion(Message.of("date_assistant.title.range"), meaning, assumptions, calendar, steps, null, notBefore,
			notAfter));
	}

}
