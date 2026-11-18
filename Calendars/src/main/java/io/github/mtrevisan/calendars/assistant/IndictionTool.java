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

import io.github.mtrevisan.calendars.gregorian.Indiction;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;


/**
 * Checks the indiction a source gives for a date, and finds the years an indiction can belong to.
 * <p>
 * The indiction does not name a year by itself (it repeats every 15 years) and the style, that decides the day it changes, is not
 * written in the document, so every style is tried.
 * </p>
 */
public final class IndictionTool{

	private static final int CYCLE = 15;
	private static final int MAX_YEARS = 3_000;
	private static final int NEIGHBOURHOOD = 15;

	/** The styles of the indiction, with the day it changes (month 0 for a style that follows the civil year). */
	private enum Style{
		ROMAN_JANUARY("date_assistant.indiction_style.roman_january", 0, 0),
		ROMAN_CHRISTMAS("date_assistant.indiction_style.roman_christmas", 12, 25),
		BEDAN("date_assistant.indiction_style.bedan", 9, 24),
		GREEK("date_assistant.indiction_style.greek", 9, 1);

		private final String key;
		private final int month;
		private final int day;

		Style(final String key, final int month, final int day){
			this.key = key;
			this.month = month;
			this.day = day;
		}

		/** @return the indiction of a day, given the one of the civil year */
		int indiction(final int yearIndiction, final int month, final int day){
			final boolean next = (this.month != 0 && (month > this.month || (month == this.month && day >= this.day)));
			return (next? yearIndiction % CYCLE + 1: yearIndiction);
		}

		String key(){
			return key;
		}
	}


	private IndictionTool(){}


	/**
	 * Checks an indiction against a date.
	 *
	 * @param calendarCode the code of the calendar of the date
	 * @param date         the date, in the notation of the calendar, or a year only
	 * @param indiction    the indiction stated by the source (1 to 15)
	 * @return the suggestion, informative: whether the indiction is consistent with the date in some style and, if not, the indiction
	 * 	the date has and the years the stated indiction belongs to, around the date
	 * @throws IllegalArgumentException if the date cannot be parsed or the indiction is not from 1 to 15
	 */
	public static List<DateSuggestion> check(final String calendarCode, final String date, final int indiction){
		validate(indiction);

		final DayRange range = DayRange.parse(calendarCode, date);
		final String calendar = range.countingCalendar();

		final List<Message> steps = new ArrayList<>();
		steps.add(range.isDay()
			? Message.of("date_assistant.step.base_date", range.label(), range.minJdn())
			: Message.of("date_assistant.step.base_range", range.label(), range.minJdn(), range.maxJdn()));

		final TreeSet<Integer> allExpected = new TreeSet<>();
		boolean consistent = false;
		for(final Style style : Style.values()){
			final TreeSet<Integer> expected = new TreeSet<>();
			for(long jdn = range.minJdn(); jdn <= range.maxJdn(); jdn ++){
				final int[] fields = CivilDates.fields(jdn, calendar);
				expected.add(style.indiction(Indiction.of(fields[0]), fields[1], fields[2]));
			}
			allExpected.addAll(expected);
			final boolean matches = expected.contains(indiction);
			consistent |= matches;
			steps.add(Message.of(matches? "date_assistant.step.indiction_style_ok": "date_assistant.step.indiction_style",
				Message.of(style.key()), join(expected)));
		}

		final Message title;
		if(consistent)
			title = Message.of("date_assistant.title.indiction_ok", indiction);
		else{
			title = Message.of("date_assistant.title.indiction_wrong", indiction, join(allExpected));
			final int year = CivilDates.fields(range.minJdn(), calendar)[0];
			final List<Integer> years = new ArrayList<>();
			for(int candidate = year - NEIGHBOURHOOD; candidate <= year + NEIGHBOURHOOD; candidate ++)
				if(Indiction.of(candidate) == indiction)
					years.add(candidate);
			steps.add(Message.of("date_assistant.step.indiction_neighbours", indiction, join(years)));
		}

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.indiction_styles"));
		final Message meaning = Message.of("date_assistant.meaning.indiction", indiction, range.label());
		return List.of(new DateSuggestion(title, meaning, assumptions, calendar, steps, null));
	}

	/**
	 * Finds the years an indiction can belong to.
	 *
	 * @param indiction the indiction (1 to 15)
	 * @param fromYear  the first year of the interval, counted from 1 January
	 * @param toYear    the last year of the interval
	 * @return the suggestion, informative: the years of the interval whose indiction is the given one (the indiction repeats every
	 * 	15 years), with the days it lasts in each style
	 * @throws IllegalArgumentException if the indiction is not from 1 to 15 or the interval is empty or too wide
	 */
	public static List<DateSuggestion> years(final int indiction, final int fromYear, final int toYear){
		validate(indiction);
		if(fromYear > toYear || (long)toYear - fromYear > MAX_YEARS)
			throw new AssistantInputException("date_assistant.error.year_range", MAX_YEARS);

		final List<Integer> years = new ArrayList<>();
		final List<Message> steps = new ArrayList<>();
		//the year after the interval is included: its indiction already starts in the last months of the interval, in some styles
		for(int year = fromYear; year <= toYear + 1; year ++)
			if(Indiction.of(year) == indiction){
				if(year <= toYear)
					years.add(year);
				steps.add(Message.of("date_assistant.step.indiction_year", indiction, year, year - 1));
			}

		final List<Message> assumptions = new ArrayList<>();
		assumptions.add(Message.of("date_assistant.assumption.indiction_cycle"));
		assumptions.add(Message.of("date_assistant.assumption.indiction_styles"));
		final Message meaning = Message.of("date_assistant.meaning.indiction_years", indiction, fromYear, toYear);
		final Message title = (years.isEmpty() && steps.isEmpty()
			? Message.of("date_assistant.title.indiction_years_none", indiction)
			: Message.of("date_assistant.title.indiction_years", indiction, join(years)));
		return List.of(new DateSuggestion(title, meaning, assumptions, null, steps, null));
	}


	private static void validate(final int indiction){
		if(indiction < 1 || indiction > CYCLE)
			throw new AssistantInputException("date_assistant.error.invalid_indiction", indiction);
	}

	private static Message join(final Iterable<Integer> values){
		final StringBuilder sb = new StringBuilder();
		for(final Integer value : values){
			if(!sb.isEmpty())
				sb.append(", ");
			sb.append(value);
		}
		return Message.text(sb.toString());
	}

}
