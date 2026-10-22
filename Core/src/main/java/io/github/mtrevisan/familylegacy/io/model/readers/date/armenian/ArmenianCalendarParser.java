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
package io.github.mtrevisan.familylegacy.io.model.readers.date.armenian;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for traditional Armenian calendar date expressions.
 * Supports classical transliterations and explicit intercalary boundaries.
 */
public final class ArmenianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Classical Armenian month names
		MONTHS.put("navasard", 1);
		MONTHS.put("hori", 2);
		MONTHS.put("sahmi", 3);
		MONTHS.put("tre", 4);
		MONTHS.put("khaghots", 5);
		MONTHS.put("arats", 6);
		MONTHS.put("mehekan", 7);
		MONTHS.put("areg", 8);
		MONTHS.put("ahekan", 9);
		MONTHS.put("mareri", 10);
		MONTHS.put("margats", 11);
		MONTHS.put("hrotits", 12);

		// Intercalary 5 epagomenal days
		MONTHS.put("avelyats", 13);
		MONTHS.put("aveliats", 13);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private ArmenianCalendarParser(){}


	/**
	 * Parses a raw Armenian text expression into a validated structural container.
	 *
	 * @param rawDate the date string (e.g., "1 Navasard 1400" or "3 Avelyats 1450")
	 * @return a configured ArmenianDateInput instance
	 */
	public static ArmenianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Armenian date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2).toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Armenian month or intercalary marker: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		// Defensive boundary checks matching physical constraints
		if(month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Armenian months must have exactly 30 days.");
		if(month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Avelyats intercalary period consists strictly of 5 days.");

		return new ArmenianDateInput(year, month, day);
	}

}
