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
package io.github.mtrevisan.familylegacy.io.model.readers.date.rumi;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Ottoman Rumi calendar expressions.
 * Supports classical Ottoman terms and modern Turkish equivalents.
 */
public final class RumiCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Traditional Ottoman Rumi Month Maps (March-anchored structure fallback)
		MONTHS.put("mart", 1);          // March
		MONTHS.put("nisan", 2);         // April
		MONTHS.put("mayis", 3);         // May
		MONTHS.put("haziran", 4);       // June
		MONTHS.put("temmuz", 5);        // July
		MONTHS.put("agustos", 6);       // August
		MONTHS.put("eylul", 7);         // September
		MONTHS.put("tesrin-i evvel", 8); // October (Ottoman)
		MONTHS.put("ekim", 8);          // October (Modern)
		MONTHS.put("tesrin-i sani", 9);  // November (Ottoman)
		MONTHS.put("kasim", 9);         // November (Modern)
		MONTHS.put("kanun-i evvel", 10); // December (Ottoman)
		MONTHS.put("aralik", 10);       // December (Modern)
		MONTHS.put("kanun-i sani", 11);  // January (Ottoman)
		MONTHS.put("ocak", 11);         // January (Modern)
		MONTHS.put("subat", 12);        // February
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private RumiCalendarParser(){}


	/**
	 * Parses a raw Rumi string expression into a validated architectural container.
	 */
	public static RumiDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Rumi date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2).toLowerCase(Locale.ROOT);

		Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Rumi month descriptor: " + monthName);

		int year = Integer.parseInt(matcher.group(3));

		// If processing a date after the 1917 reform (Year 1333+), normalize months to standard Gregorian order
		if(year > 1332 && monthName.matches("ocak|kanun-i sani"))
			month = 1; // January becomes Month 1 after reform alignment

		if(day < 1 || day > 31)
			throw new IllegalArgumentException("Day variable breaches Rumi monthly limits.");

		return new RumiDateInput(year, month, day);
	}

}
