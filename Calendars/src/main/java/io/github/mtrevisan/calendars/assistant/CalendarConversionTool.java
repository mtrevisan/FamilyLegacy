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
import java.util.Set;


/** Converts a date of any supported calendar into the Julian and the Gregorian ones. */
public final class CalendarConversionTool{

	/** The calendars whose year starts on a day other than 1 January. */
	private static final Set<CalendarType> YEAR_START_STYLES = Set.of(CalendarType.VENETAN, CalendarType.FLORENTINE,
		CalendarType.PISAN, CalendarType.NATIVITY, CalendarType.BYZANTINE_STYLE, CalendarType.EASTER_STYLE);


	private CalendarConversionTool(){}


	/**
	 * Converts a date.
	 *
	 * @param calendarCode the code of the calendar of the date
	 * @param rawDate      the date, in the notation of the calendar
	 * @return the candidates, the calendar most likely in force at the date first
	 * @throws IllegalArgumentException if the date cannot be parsed
	 */
	public static List<DateSuggestion> convert(final String calendarCode, final String rawDate){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final long jdn = type.parseToJdn(rawDate);

		final List<String> steps = new ArrayList<>();
		steps.add("Read \"" + rawDate + "\" as a " + type.getCode() + " date: Julian Day Number " + jdn);
		final List<String> assumptions = new ArrayList<>();
		if(YEAR_START_STYLES.contains(type))
			assumptions.add("The year number of the source follows the " + type.getCode()
				+ " style, whose year does not start on 1 January");
		return fromJdn(jdn, "The " + type.getCode() + " date \"" + rawDate + "\"", assumptions, steps);
	}

	/**
	 * The Julian and Gregorian dates of a day.
	 *
	 * @param jdn         the Julian Day Number
	 * @param meaning     the interpreted meaning to show
	 * @param assumptions the assumptions made so far
	 * @param steps       the steps done so far
	 * @return the candidates, the calendar in force in Rome at the date first
	 */
	static List<DateSuggestion> fromJdn(final long jdn, final String meaning, final List<String> assumptions,
			final List<String> steps){
		final boolean julianFirst = CivilDates.JULIAN.equals(CivilDates.calendarInForce(jdn, CalendarReform.ROME));

		final DateSuggestion julian = candidate("Julian calendar", jdn, meaning, assumptions, steps, CivilDates.JULIAN,
			(julianFirst? null: "The Julian calendar was no longer in use in Catholic countries at this date"));
		final DateSuggestion gregorian = candidate("Gregorian calendar", jdn, meaning, assumptions, steps, CivilDates.GREGORIAN,
			(julianFirst
				? "Before 15 October 1582 the Gregorian date is proleptic: it was not used at the time"
				: "Countries adopted the Gregorian calendar at different dates (e.g. Great Britain in 1752)"));
		return (julianFirst? List.of(julian, gregorian): List.of(gregorian, julian));
	}

	private static DateSuggestion candidate(final String title, final long jdn, final String meaning,
			final List<String> assumptions, final List<String> steps, final String calendarCode, final String note){
		final String date = CivilDates.format(jdn, calendarCode);

		final List<String> allAssumptions = new ArrayList<>(assumptions);
		if(note != null)
			allAssumptions.add(note);
		final List<String> allSteps = new ArrayList<>(steps);
		allSteps.add("Julian Day Number " + jdn + " written in the " + calendarCode + " calendar: " + date
			+ " (" + CivilDates.weekdayName(jdn) + ")");
		return new DateSuggestion(title, meaning, allAssumptions, calendarCode, allSteps, date);
	}

}
