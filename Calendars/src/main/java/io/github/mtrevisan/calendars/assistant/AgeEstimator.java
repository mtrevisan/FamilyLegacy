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
		final String trimmed = eventDate.trim();
		final long minJdn;
		final long maxJdn;
		final String countingCalendar;
		if(YEAR_ONLY.matcher(trimmed).matches()){
			final int year = Integer.parseInt(trimmed);
			final long january = CivilDates.toJdn(year, 1, 1, (CivilDates.JULIAN.equals(calendarCode)
				? CivilDates.JULIAN: CivilDates.GREGORIAN));
			countingCalendar = countingCalendar(calendarCode, january);
			minJdn = CivilDates.toJdn(year, 1, 1, countingCalendar);
			maxJdn = CivilDates.toJdn(year, 12, 31, countingCalendar);
		}
		else{
			minJdn = CalendarType.fromCode(calendarCode).parseToJdn(trimmed);
			maxJdn = minJdn;
			countingCalendar = countingCalendar(calendarCode, minJdn);
		}

		return birthFromAge(minJdn, maxJdn, countingCalendar, "\"" + trimmed + "\" (" + calendarCode + ")", years, months, days,
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
			final String eventLabel, final int years, final Integer months, final Integer days, final boolean ordinalYear,
			final String eventName){
		if(years < 0 || (ordinalYear && years < 1) || (months != null && (months < 0 || months >= MONTHS_IN_YEAR))
				|| (days != null && days < 0))
			throw new IllegalArgumentException("Not a valid age");

		final int completedYears = (ordinalYear? years - 1: years);
		final int completedMonths = (months != null? months: 0);

		final List<String> assumptions = new ArrayList<>();
		final List<String> steps = new ArrayList<>();
		steps.add("Date of the " + eventName + ": " + eventLabel + (minJdn == maxJdn
			? ", Julian Day Number " + minJdn
			: ", from Julian Day Number " + minJdn + " to " + maxJdn));
		if(ordinalYear){
			assumptions.add("\"In the " + years + "th year\" means " + completedYears + " years completed");
			steps.add("Age in the " + years + "th year of life = " + completedYears + " completed years");
		}
		else
			assumptions.add("The age is in completed years (the modern convention)");
		assumptions.add("Years and months are counted in the " + countingCalendar + " calendar; a day that does not exist in the "
			+ "month reached (e.g. 29 February) becomes the last day of the month");

		final long lower;
		final long upper;
		final String age;
		if(days != null){
			age = completedYears + " years, " + completedMonths + " months, " + days + " days";
			lower = subtract(minJdn, completedYears, completedMonths, days, countingCalendar);
			upper = subtract(maxJdn, completedYears, completedMonths, days, countingCalendar);
			assumptions.add("The age is exact to the day");
		}
		else if(months != null){
			age = completedYears + " years, " + completedMonths + " months";
			lower = subtract(minJdn, completedYears, completedMonths + 1, 0, countingCalendar) + 1;
			upper = subtract(maxJdn, completedYears, completedMonths, 0, countingCalendar);
			assumptions.add("The days of the age are not stated: the birth can be any day of that month of life");
		}
		else{
			age = completedYears + " years";
			lower = subtract(minJdn, completedYears + 1, 0, 0, countingCalendar) + 1;
			upper = subtract(maxJdn, completedYears, 0, 0, countingCalendar);
			assumptions.add("Only the years of the age are stated: the birth can be any day of that year of life");
		}
		assumptions.add("Ancient sources often round the age: widen the range if the age is approximate");
		steps.add("Date of the event minus " + age + (days == null? " (and the unknown rest of the last unit)": ""));

		final String meaning = "Born " + age + " before the " + eventName + " (" + eventLabel + ")";
		final String earliest = CivilDates.format(lower, countingCalendar);
		final String latest = CivilDates.format(upper, countingCalendar);
		steps.add("Julian Day Number " + lower + (lower == upper? "": " to " + upper) + " written in the " + countingCalendar
			+ " calendar: " + earliest + (lower == upper? "": " to " + latest));

		if(lower == upper)
			return List.of(new DateSuggestion("Estimated birth date", meaning, assumptions, countingCalendar, steps, earliest));

		final int firstYear = CivilDates.fields(lower, countingCalendar)[0];
		final int lastYear = CivilDates.fields(upper, countingCalendar)[0];
		final String years2 = (firstYear == lastYear? "the year " + firstYear: "the year " + firstYear + " or " + lastYear
			+ " (" + (lastYear - firstYear + 1) + " years if the event is a range)");
		steps.add("The birth falls in " + years2);
		return List.of(new DateSuggestion("Estimated birth range", meaning + ": in " + years2, assumptions, countingCalendar,
			steps, null, earliest, latest));
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
