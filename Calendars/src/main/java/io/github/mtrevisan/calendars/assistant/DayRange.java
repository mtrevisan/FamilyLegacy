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
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.regex.Pattern;


/**
 * A date as the researcher knows it: a day, or a range of days (a year only, an approximate or bounded date).
 *
 * @param minJdn          the earliest Julian Day Number
 * @param maxJdn          the latest Julian Day Number
 * @param countingCalendar the calendar in which years and months are counted (Julian or Gregorian)
 * @param label           the description of the date, for the explanations
 */
public record DayRange(long minJdn, long maxJdn, String countingCalendar, Message label){

	private static final Pattern YEAR_ONLY = Pattern.compile("-?\\d{1,4}");


	/**
	 * Reads a date.
	 *
	 * @param calendarCode the code of the calendar of the date
	 * @param text         the date, in the notation of the calendar, or a year only (e.g. "1500")
	 * @return the day, or the range of days of the year
	 * @throws IllegalArgumentException if the date cannot be parsed
	 */
	public static DayRange parse(final String calendarCode, final String text){
		if(StringUtils.isBlank(text))
			throw new AssistantInputException("date_assistant.error.event_date_empty");

		final String trimmed = text.trim();
		final long minJdn;
		final long maxJdn;
		final String countingCalendar;
		final boolean civil = (CivilDates.JULIAN.equals(calendarCode) || CivilDates.GREGORIAN.equals(calendarCode));
		Message label = Message.of("date_assistant.label.date_in_calendar", trimmed, Message.calendar(calendarCode));
		if(YEAR_ONLY.matcher(trimmed).matches() && civil){
			//a bare number is a civil year only for a civil calendar
			final int year = Integer.parseInt(trimmed);
			countingCalendar = calendarCode;
			if(CivilDates.GREGORIAN.equals(calendarCode) && year == CalendarReform.ROME.firstGregorianYear()){
				//the reform fell inside the year: it started on a Julian 1 January and ended on a Gregorian 31 December
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
			//the year of another calendar (e.g. the Hebrew 5786): from its first day to the day before the next year
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
		return new DayRange(minJdn, maxJdn, countingCalendar, label);
	}

	/** @return today, written as a Gregorian date */
	public static String today(){
		final LocalDate now = LocalDate.now();
		return CivilDates.formatGregorian(GregorianCalendarStrategy.toJdn(now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
	}


	/** @return whether the date is a single day */
	public boolean isDay(){
		return (minJdn == maxJdn);
	}

	/** The calendar in which years and months are counted: the one of the event, or the one in force if it is another one. */
	private static String countingCalendar(final String calendarCode, final long jdn){
		if(CivilDates.JULIAN.equals(calendarCode) || CivilDates.GREGORIAN.equals(calendarCode))
			return calendarCode;

		return CivilDates.calendarInForce(jdn, CalendarReform.ROME);
	}

}
