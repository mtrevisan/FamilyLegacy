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
package io.github.mtrevisan.calendars.indian;

import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Month names of a solar calendar of the Indian subcontinent, and the parser of its "day month year" dates.
 */
final class IndianMonthNames{

	/** A parsed solar date. */
	record SolarDate(int year, int month, int day){

		int monthOrFirst(){
			return (month != 0? month: 1);
		}

		int dayOrFirst(){
			return (day != 0? day: 1);
		}

	}

	/** Era markers and other words that carry no date information. */
	private static final Set<String> ERA_WORDS = Set.of("bs", "b", "s", "bangabda", "bongabdo", "bengali", "bangla", "san",
		"sal", "sana", "me", "ke", "km", "kollam", "kolla", "varsham", "kollavarsham", "era", "tamil", "thiruvalluvar",
		"tiruvalluvar", "malayalam", "of", "the", "year",
		TextNormalizer.normalize("বঙ্গাব্দ"), TextNormalizer.normalize("সাল"), TextNormalizer.normalize("সন"),
		TextNormalizer.normalize("കൊല്ലവർഷം"), TextNormalizer.normalize("ക"), TextNormalizer.normalize("വ"));
	private static final int MIN_YEAR = 100;
	private static final int MAX_DAY = 32;

	private final Map<String, Integer> months = new HashMap<>();


	IndianMonthNames register(final int month, final String... names){
		for(final String name : names)
			months.put(TextNormalizer.normalize(name), month);
		return this;
	}

	/**
	 * Parses a "day month year" date (each component but the year is optional).
	 *
	 * @param rawDate  the date expression
	 * @param calendar the name of the calendar, for the error messages
	 * @return the parsed date
	 * @throws IllegalArgumentException if the expression cannot be parsed
	 */
	SolarDate parse(final String rawDate, final String calendar){
		int year = 0;
		int month = 0;
		int day = 0;
		final String[] words = TextNormalizer.tokenize(rawDate);
		for(final String word : words){
			if(ERA_WORDS.contains(word))
				continue;

			final Integer named = months.get(word);
			if(named != null){
				if(month != 0)
					throw new IllegalArgumentException("More than one month in the " + calendar + " date: " + rawDate);
				month = named;
			}
			else if(StringUtils.isNumeric(word)){
				final int number = Integer.parseInt(word);
				if(number >= MIN_YEAR && year == 0)
					year = number;
				else if(number >= 1 && number <= MAX_DAY && day == 0)
					day = number;
				else
					throw new IllegalArgumentException("Invalid number '" + word + "' in the " + calendar + " date: " + rawDate);
			}
			else
				throw new IllegalArgumentException("Unknown " + calendar + " month '" + word + "': " + rawDate);
		}
		if(year == 0)
			throw new IllegalArgumentException("Missing year in the " + calendar + " date: " + rawDate);
		if(day != 0 && month == 0)
			throw new IllegalArgumentException("Missing month in the " + calendar + " date: " + rawDate);
		return new SolarDate(year, month, day);
	}

}
