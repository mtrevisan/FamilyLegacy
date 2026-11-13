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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/** Estimates the date of birth from the age a person had at an event (death, marriage, a census, ...). */
public final class AgeEstimator{

	private static final int MONTHS_IN_YEAR = 12;


	private AgeEstimator(){}


	/**
	 * Estimates the date of birth.
	 *
	 * @param calendarCode the code of the calendar of the date of the event
	 * @param eventDate    the date of the event, in the notation of the calendar, or a year only (e.g. "1500")
	 * @param years        the age in years
	 * @param months       the months of the age, or {@code null} if not stated
	 * @param days         the days of the age, or {@code null} if not stated
	 * @param ordinalYear  {@code true} if the age is the ordinal of the year of life ("in his 72nd year", that is 71 years
	 *                     completed), {@code false} if it is the completed years
	 * @param eventName    the name of the event (e.g. "death"), for the explanation
	 * @return the suggestions
	 * @throws IllegalArgumentException if the date cannot be parsed or the age is not valid
	 */
	public static List<DateSuggestion> birthFromAge(final String calendarCode, final String eventDate, final int years,
			final Integer months, final Integer days, final boolean ordinalYear, final String eventName){
		final DayRange event = DayRange.parse(calendarCode, eventDate);
		return birthFromAge(event.minJdn(), event.maxJdn(), event.countingCalendar(), event.label(), years, months, days,
			ordinalYear, eventName);
	}

	/**
	 * Estimates the date of birth.
	 *
	 * @param minJdn           the earliest Julian Day Number of the event
	 * @param maxJdn           the latest Julian Day Number of the event
	 * @param countingCalendar the calendar in which the years and months of the age are counted (Julian or Gregorian)
	 * @param eventLabel       the description of the date of the event, for the explanation
	 * @param years            the age in years
	 * @param months           the months of the age, or {@code null} if not stated
	 * @param days             the days of the age, or {@code null} if not stated
	 * @param ordinalYear      {@code true} if the age is the ordinal of the year of life
	 * @param eventName        the name of the event (e.g. "death"), for the explanation
	 * @return the suggestions
	 * @throws IllegalArgumentException if the age is not valid
	 */
	public static List<DateSuggestion> birthFromAge(final long minJdn, final long maxJdn, final String countingCalendar,
			final Message eventLabel, final int years, final Integer months, final Integer days, final boolean ordinalYear,
			final String eventName){
		if(years < 0 || (ordinalYear && years < 1) || (months != null && (months < 0 || months >= MONTHS_IN_YEAR))
				|| (days != null && days < 0))
			throw new AssistantInputException("date_assistant.error.invalid_age");

		final int completedYears = (ordinalYear? years - 1: years);
		final int completedMonths = (months != null? months: 0);
		final Message.Calendar calendar = Message.calendar(countingCalendar);

		final List<Message> assumptions = new ArrayList<>();
		final List<Message> steps = new ArrayList<>();
		steps.add(minJdn == maxJdn
			? Message.of("date_assistant.step.event_date", eventName, eventLabel, minJdn)
			: Message.of("date_assistant.step.event_date_range", eventName, eventLabel, minJdn, maxJdn));
		if(ordinalYear){
			assumptions.add(Message.of("date_assistant.assumption.ordinal_year", years, completedYears));
			steps.add(Message.of("date_assistant.step.ordinal_age", years, completedYears));
		}
		else
			assumptions.add(Message.of("date_assistant.assumption.completed_years"));
		assumptions.add(Message.of("date_assistant.assumption.counting_calendar", calendar));

		final long[] window = birthWindow(minJdn, maxJdn, countingCalendar, years, months, days, ordinalYear);
		final long lower = window[0];
		final long upper = window[1];
		final Message age = ageMessage(completedYears, months, days);
		if(days != null)
			assumptions.add(Message.of("date_assistant.assumption.age_exact_day"));
		else if(months != null)
			assumptions.add(Message.of("date_assistant.assumption.age_any_day_of_month"));
		else
			assumptions.add(Message.of("date_assistant.assumption.age_any_day_of_year"));
		assumptions.add(Message.of("date_assistant.assumption.age_rounded"));
		steps.add(Message.of(days == null? "date_assistant.step.minus_age_rest": "date_assistant.step.minus_age", age));

		final Message meaning = Message.of("date_assistant.meaning.born", age, eventName, eventLabel);
		final String earliest = CivilDates.format(lower, countingCalendar);
		final String latest = CivilDates.format(upper, countingCalendar);
		steps.add(lower == upper
			? Message.of("date_assistant.step.written_plain", lower, calendar, earliest)
			: Message.of("date_assistant.step.written_span", lower, upper, calendar, earliest, latest));

		if(lower == upper)
			return List.of(new DateSuggestion(Message.of("date_assistant.title.birth_date"), meaning, assumptions, countingCalendar, steps,
				earliest));

		final int firstYear = CivilDates.fields(lower, countingCalendar)[0];
		final int lastYear = CivilDates.fields(upper, countingCalendar)[0];
		final Message birthYears = (firstYear == lastYear
			? Message.of("date_assistant.years.one", firstYear)
			: Message.of("date_assistant.years.two", firstYear, lastYear, lastYear - firstYear + 1));
		steps.add(Message.of("date_assistant.step.birth_falls", birthYears));
		return List.of(new DateSuggestion(Message.of("date_assistant.title.birth_range"), Message.of("date_assistant.meaning.born_range", meaning,
			birthYears), assumptions, countingCalendar, steps, null, earliest, latest));
	}


	/**
	 * The days the birth can fall in.
	 *
	 * @return the earliest and the latest Julian Day Number of the birth
	 * @throws IllegalArgumentException if the age is not valid
	 */
	public static long[] birthWindow(final long minJdn, final long maxJdn, final String countingCalendar, final int years,
			final Integer months, final Integer days, final boolean ordinalYear){
		if(years < 0 || (ordinalYear && years < 1) || (months != null && (months < 0 || months >= MONTHS_IN_YEAR))
				|| (days != null && days < 0))
			throw new AssistantInputException("date_assistant.error.invalid_age");

		final int completedYears = (ordinalYear? years - 1: years);
		final int completedMonths = (months != null? months: 0);
		if(days != null)
			return new long[]{subtract(minJdn, completedYears, completedMonths, days, countingCalendar),
				subtract(maxJdn, completedYears, completedMonths, days, countingCalendar)};
		if(months != null)
			return new long[]{subtract(minJdn, completedYears, completedMonths + 1, 0, countingCalendar) + 1,
				subtract(maxJdn, completedYears, completedMonths, 0, countingCalendar)};
		return new long[]{subtract(minJdn, completedYears + 1, 0, 0, countingCalendar) + 1,
			subtract(maxJdn, completedYears, 0, 0, countingCalendar)};
	}

	/** @return the age, as years, months and days (as many as are stated) */
	static Message ageMessage(final int completedYears, final Integer months, final Integer days){
		if(days != null)
			return Message.of("date_assistant.age.years_months_days", completedYears, (months != null? months: 0), days);
		if(months != null)
			return Message.of("date_assistant.age.years_months", completedYears, months);
		return Message.of("date_assistant.age.years", completedYears);
	}

	/** Subtracts years and months (keeping the day if possible), then days. */
	private static long subtract(final long jdn, final int years, final int months, final int days, final String calendar){
		final int[] fields = CivilDates.fields(jdn, calendar);
		final int total = fields[0] * MONTHS_IN_YEAR + (fields[1] - 1) - (years * MONTHS_IN_YEAR + months);
		final int year = Math.floorDiv(total, MONTHS_IN_YEAR);
		final int month = Math.floorMod(total, MONTHS_IN_YEAR) + 1;
		final int day = Math.min(fields[2], CivilDates.monthLength(year, month, calendar));
		return CivilDates.toJdn(year, month, day, calendar) - days;
	}

}
