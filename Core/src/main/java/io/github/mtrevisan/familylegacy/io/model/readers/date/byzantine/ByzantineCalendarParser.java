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
package io.github.mtrevisan.familylegacy.io.model.readers.date.byzantine;

import io.github.mtrevisan.familylegacy.io.model.readers.date.UniversalDateConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Byzantine calendar date expressions.
 * Supports structural Greek names, Latin transliterations, and localized numerical scripts.
 */
public final class ByzantineCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Byzantine year starts in September
		// Latin / Ecclesiastical Transliterations
		MONTHS.put("september", 1);
		MONTHS.put("october", 2);
		MONTHS.put("november", 3);
		MONTHS.put("december", 4);
		MONTHS.put("january", 5);
		MONTHS.put("february", 6);
		MONTHS.put("march", 7);
		MONTHS.put("april", 8);
		MONTHS.put("may", 9);
		MONTHS.put("june", 10);
		MONTHS.put("july", 11);
		MONTHS.put("august", 12);

		// Greek Classical/Medieval Script names
		MONTHS.put("σεπτέμβριος", 1);
		MONTHS.put("οκτώβριος", 2);
		MONTHS.put("νοέμβριος", 3);
		MONTHS.put("δεκέμβριος", 4);
		MONTHS.put("ιανουάριος", 5);
		MONTHS.put("φεβρουάριος", 6);
		MONTHS.put("μάρτιος", 7);
		MONTHS.put("απρίλιος", 8);
		MONTHS.put("μάιος", 9);
		MONTHS.put("ιούνιος", 10);
		MONTHS.put("ιούλιος", 11);
		MONTHS.put("αύγουστος", 12);

		// Phthinoporo (Autumn)
		MONTHS.put("φθινόπωρο", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("phthinoporo", UniversalDateConverter.SEASON_AUTUMN);
		// Cheimonas (Winter)
		MONTHS.put("χειμώνας", UniversalDateConverter.SEASON_WINTER);
		MONTHS.put("cheimonas", UniversalDateConverter.SEASON_WINTER);
		// Anoixi (Spring)
		MONTHS.put("άνοιξη", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("anoixi", UniversalDateConverter.SEASON_SPRING);
		// Theros / Kalokairi (Summer)
		MONTHS.put("θέρος", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("theros", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("καλοκαίρι", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("kalokairi", UniversalDateConverter.SEASON_SUMMER);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\S+)\\s+(\\d+)");


	private ByzantineCalendarParser(){}


	/**
	 * Parses a raw Byzantine text expression into a validated structural DTO.
	 *
	 * @param rawDate the date string (e.g., "1 Σεπτέμβριος 7000" or "15 September 6500" or "Theros 6508")
	 * @return a configured ByzantineDateInput instance
	 * @throws IllegalArgumentException if string validations or regex checks fail
	 */
	public static ByzantineDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Byzantine date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Byzantine month or seasonal context: " + monthName);

		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		// Basic sanity boundary check (Julian structure mapping)
		if(month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Invalid day count for the targeted Julian boundary.");

		return new ByzantineDateInput(year, month, day);
	}

}
