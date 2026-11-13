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

import java.util.ArrayList;
import java.util.List;


/** The time between two dates, in years, months and days (an age, if the first one is a birth). */
public final class IntervalTool{

	private static final int DAYS_IN_WEEK = 7;
	private static final int MONTHS_IN_YEAR = 12;


	private IntervalTool(){}


	/**
	 * Computes the time between two dates, in whichever order they are given.
	 *
	 * @param from the first date
	 * @param to   the second date
	 * @return the suggestion, informative (the result is in its title); if a date is not a single day, the closest and the
	 * 	farthest possible intervals
	 */
	public static List<DateSuggestion> between(final DayRange from, final DayRange to){
		final long lo = to.minJdn() - from.maxJdn();
		final long hi = to.maxJdn() - from.minJdn();
		//the second date is surely before the first one: the interval is counted backwards
		final boolean reversed = (hi < 0);
		final DayRange first = (reversed? to: from);
		final DayRange second = (reversed? from: to);
		final String calendar = first.countingCalendar();

		final long shortestStart;
		final long shortestEnd;
		final long longestStart;
		final long longestEnd;
		if(reversed || lo >= 0){
			shortestStart = first.maxJdn();
			shortestEnd = second.minJdn();
			longestStart = first.minJdn();
			longestEnd = second.maxJdn();
		}
		else{
			//the ranges overlap: the dates may be the same day
			shortestStart = first.minJdn();
			shortestEnd = first.minJdn();
			final boolean secondLast = (hi >= -lo);
			longestStart = (secondLast? first.minJdn(): second.minJdn());
			longestEnd = (secondLast? second.maxJdn(): first.maxJdn());
		}
		final long[] shortest = period(shortestStart, shortestEnd, calendar);
		final long[] longest = period(longestStart, longestEnd, calendar);

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.counting_calendar", Message.calendar(calendar)));
		final List<Message> steps = new ArrayList<>();
		steps.add(describe(from));
		steps.add(describe(to));
		final boolean exact = (from.isDay() && to.isDay());
		final Message title;
		if(exact){
			final long days = Math.abs(to.minJdn() - from.minJdn());
			steps.add(Message.of("date_assistant.step.total_days", days, days / DAYS_IN_WEEK));
			title = Message.of("date_assistant.title.interval", periodMessage(longest));
		}
		else{
			assumptions.add(Message.of("date_assistant.assumption.range_interval"));
			steps.add(Message.of("date_assistant.step.total_days_range", (shortestEnd - shortestStart), (longestEnd - longestStart)));
			steps.add(Message.of("date_assistant.step.interval_closest", periodMessage(shortest)));
			steps.add(Message.of("date_assistant.step.interval_farthest", periodMessage(longest)));
			title = Message.of("date_assistant.title.interval_range", periodMessage(shortest), periodMessage(longest));
		}
		final Message meaning = Message.of(reversed? "date_assistant.meaning.interval_reversed": "date_assistant.meaning.interval",
			from.label(), to.label());
		return List.of(new DateSuggestion(title, meaning, assumptions, calendar, steps, null));
	}

	private static Message describe(final DayRange date){
		return (date.isDay()
			? Message.of("date_assistant.step.base_date", date.label(), date.minJdn())
			: Message.of("date_assistant.step.base_range", date.label(), date.minJdn(), date.maxJdn()));
	}

	/**
	 * The years, months and days from a day to a later one, counted in a calendar (the days are those after the last whole month).
	 *
	 * @return the years, the months and the days
	 */
	static long[] period(final long startJdn, final long endJdn, final String calendar){
		final int[] start = CivilDates.fields(startJdn, calendar);
		final int[] end = CivilDates.fields(endJdn, calendar);
		int months = (end[0] * MONTHS_IN_YEAR + end[1]) - (start[0] * MONTHS_IN_YEAR + start[1]);
		long shifted = CivilDates.shiftMonths(startJdn, months, calendar);
		if(shifted > endJdn){
			months --;
			shifted = CivilDates.shiftMonths(startJdn, months, calendar);
		}
		return new long[]{months / MONTHS_IN_YEAR, months % MONTHS_IN_YEAR, endJdn - shifted};
	}

	/** The period as "39 years, 11 months, 30 days": the zero parts are left out. */
	static Message periodMessage(final long[] period){
		final List<Message> parts = new ArrayList<>(3);
		if(period[0] != 0)
			parts.add(Message.of("date_assistant.period.years", period[0]));
		if(period[1] != 0)
			parts.add(Message.of("date_assistant.period.months", period[1]));
		if(period[2] != 0 || parts.isEmpty())
			parts.add(Message.of("date_assistant.period.days", period[2]));
		return switch(parts.size()){
			case 1 -> Message.of("date_assistant.period.one", parts.toArray());
			case 2 -> Message.of("date_assistant.period.two", parts.toArray());
			default -> Message.of("date_assistant.period.three", parts.toArray());
		};
	}

}
