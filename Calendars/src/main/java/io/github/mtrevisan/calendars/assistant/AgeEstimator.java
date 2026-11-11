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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;


/** Estimates the date of birth from the age a person had at an event (death, marriage, a census, ...). */
public final class AgeEstimator{

	private static final Pattern YEAR_ONLY = Pattern.compile("-?\\d{1,4}");
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
		if(StringUtils.isBlank(eventDate))
			throw new AssistantInputException("date_assistant.error.event_date_empty");

		final String trimmed = eventDate.trim();
		final long minJdn;
		final long maxJdn;
		final String countingCalendar;
		final boolean civil = (CivilDates.JULIAN.equals(calendarCode) || CivilDates.GREGORIAN.equals(calendarCode));
		Message label = Message.of("date_assistant.label.date_in_calendar", trimmed, Message.calendar(calendarCode));
		if(YEAR_ONLY.matcher(trimmed).matches() && civil){
			// a bare number is a civil year only for a civil calendar
			final int year = Integer.parseInt(trimmed);
			countingCalendar = calendarCode;
			if(CivilDates.GREGORIAN.equals(calendarCode) && year == CalendarReform.ROME.firstGregorianYear()){
				// the reform fell inside the year: it started on a Julian 1 January and ended on a Gregorian 31 December
				minJdn = CalendarReform.ROME.toJdn(year, 1, 1);
				maxJdn = CalendarReform.ROME.toJdn(year, 12, 31);
				label = Message.of("date_assistant.label.reform_year", label);
			}
			else{
				minJdn = CivilDates.toJdn(year, 1, 1, countingCalendar);
				maxJdn = CivilDates.toJdn(year, 12, 31, countingCalendar);
			}
		}
		else if(YEAR_ONLY.matcher(trimmed).matches()){
			// the year of another calendar (e.g. the Hebrew 5786): from its first day to the day before the next year
			final CalendarType type = CalendarType.fromCode(calendarCode);
			minJdn = type.parseToJdn(trimmed);
			maxJdn = type.parseToJdn(String.valueOf(Integer.parseInt(trimmed) + 1)) - 1;
			countingCalendar = countingCalendar(calendarCode, minJdn);
			label = Message.of("date_assistant.label.whole_year", label);
		}
		else{
			minJdn = CalendarType.fromCode(calendarCode).parseToJdn(trimmed);
			maxJdn = minJdn;
			countingCalendar = countingCalendar(calendarCode, minJdn);
		}

		return birthFromAge(minJdn, maxJdn, countingCalendar, label, years, months, days, ordinalYear, eventName);
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

		final long lower;
		final long upper;
		final Message age;
		if(days != null){
			age = Message.of("date_assistant.age.years_months_days", completedYears, completedMonths, days);
			lower = subtract(minJdn, completedYears, completedMonths, days, countingCalendar);
			upper = subtract(maxJdn, completedYears, completedMonths, days, countingCalendar);
			assumptions.add(Message.of("date_assistant.assumption.age_exact_day"));
		}
		else if(months != null){
			age = Message.of("date_assistant.age.years_months", completedYears, completedMonths);
			lower = subtract(minJdn, completedYears, completedMonths + 1, 0, countingCalendar) + 1;
			upper = subtract(maxJdn, completedYears, completedMonths, 0, countingCalendar);
			assumptions.add(Message.of("date_assistant.assumption.age_any_day_of_month"));
		}
		else{
			age = Message.of("date_assistant.age.years", completedYears);
			lower = subtract(minJdn, completedYears + 1, 0, 0, countingCalendar) + 1;
			upper = subtract(maxJdn, completedYears, 0, 0, countingCalendar);
			assumptions.add(Message.of("date_assistant.assumption.age_any_day_of_year"));
		}
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


	/** The calendar in which years and months are counted: the one of the event, or the one in force if it is another one. */
	private static String countingCalendar(final String calendarCode, final long jdn){
		if(CivilDates.JULIAN.equals(calendarCode) || CivilDates.GREGORIAN.equals(calendarCode))
			return calendarCode;

		return CivilDates.calendarInForce(jdn, CalendarReform.ROME);
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
