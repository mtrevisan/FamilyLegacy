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

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;


/** Checks the day of the week a source gives for a date ("Sunday 31 December 1853"): a mismatch is a transcription clue. */
public final class WeekdayTool{

	private static final int NEAREST_RADIUS = 3;


	private WeekdayTool(){}


	/**
	 * Checks whether a date fell on the weekday the source states.
	 *
	 * @param calendarCode the code of the calendar of the date
	 * @param date         the date, in the notation of the calendar (a single day)
	 * @param claimed      the weekday stated by the source
	 * @return the suggestion, informative: whether the weekday matches and, if not, the nearest day with that weekday and whether the
	 * 	other civil calendar would have matched
	 * @throws IllegalArgumentException if the date cannot be parsed, or it is not a single day
	 */
	public static List<DateSuggestion> check(final String calendarCode, final String date, final DayOfWeek claimed){
		final DayRange day = DayRange.parse(calendarCode, date);
		if(!day.isDay())
			throw new AssistantInputException("date_assistant.error.not_a_day", date);

		final long jdn = day.minJdn();
		final DayOfWeek actual = CivilDates.dayOfWeek(jdn);
		final Message.Calendar calendar = Message.calendar(calendarCode);

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.weekday_continuous"));
		final List<Message> steps = new ArrayList<>();
		steps.add(Message.of("date_assistant.step.read", date, calendar, jdn));
		steps.add(Message.of("date_assistant.step.weekday", jdn, actual));

		final Message title;
		if(actual == claimed)
			title = Message.of("date_assistant.title.weekday_ok", actual);
		else{
			title = Message.of("date_assistant.title.weekday_wrong", actual, claimed);
			for(int offset = -NEAREST_RADIUS; offset <= NEAREST_RADIUS; offset ++)
				if(CivilDates.dayOfWeek(jdn + offset) == claimed){
					steps.add(Message.of(offset < 0? "date_assistant.step.weekday_nearest_before": "date_assistant.step.weekday_nearest_after",
						claimed, CivilDates.format(jdn + offset, day.countingCalendar()), Math.abs(offset)));
					break;
				}

			//the writer may have used the other civil calendar (a Julian date read as a Gregorian one, or the other way round)
			if(CivilDates.JULIAN.equals(calendarCode) || CivilDates.GREGORIAN.equals(calendarCode)){
				final String other = (CivilDates.JULIAN.equals(calendarCode)? CivilDates.GREGORIAN: CivilDates.JULIAN);
				try{
					final DayRange otherDay = DayRange.parse(other, date);
					if(otherDay.isDay() && CivilDates.dayOfWeek(otherDay.minJdn()) == claimed)
						steps.add(Message.of("date_assistant.step.weekday_other_calendar", Message.calendar(other), date, claimed,
							otherDay.minJdn()));
				}
				catch(final RuntimeException ignored){
					//the same text is not a date of the other calendar
				}
			}
		}

		final Message meaning = Message.of("date_assistant.meaning.weekday", day.label(), claimed);
		return List.of(new DateSuggestion(title, meaning, assumptions, day.countingCalendar(), steps, null));
	}

}
