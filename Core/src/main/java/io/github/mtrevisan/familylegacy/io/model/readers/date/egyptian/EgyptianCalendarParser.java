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
package io.github.mtrevisan.familylegacy.io.model.readers.date.egyptian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for ancient Egyptian civil calendar date expressions.
 * Supports standard month names and explicit epagomenal day markers.
 */
public final class EgyptianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Season 1: Akhet (Inundation)
		MONTHS.put("thoth", 1);
		MONTHS.put("phaophi", 2);
		MONTHS.put("athyr", 3);
		MONTHS.put("choiak", 4);

		// Season 2: Peret (Growing/Sowing)
		MONTHS.put("tybi", 5);
		MONTHS.put("mechir", 6);
		MONTHS.put("phamenoth", 7);
		MONTHS.put("pharmouthi", 8);

		// Season 3: Shemu (Harvest)
		MONTHS.put("pachons", 9);
		MONTHS.put("payni", 10);
		MONTHS.put("epiphi", 11);
		MONTHS.put("mesore", 12);

		// Intercalary period (5 Epagomenal Days)
		MONTHS.put("epagomenai", 13);
		MONTHS.put("epagomenal", 13);
		MONTHS.put("meskut", 13);

		// NATIVE SEASONAL FLAGS (Negative integers map to dynamic astronomical calculation scopes)
		// Akhet (Inundation / Autumn)
		MONTHS.put("akhet", CalendarType.SEASON_AUTUMN);
		// Peret (Emergence / Winter)
		MONTHS.put("peret", CalendarType.SEASON_WINTER);
		// Shemu (Low Water / Summer)
		MONTHS.put("shemu", CalendarType.SEASON_SPRING);
		MONTHS.put("shomu", CalendarType.SEASON_SPRING);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\S+)\\s+(\\d+)");


	private EgyptianCalendarParser(){}


	/**
	 * Parses a raw Egyptian text string into a validated data container.
	 *
	 * @param rawDate the date expression (e.g., "1 Thoth 1400" or "3 Meskut 1520")
	 * @return a structured EgyptianDateInput container
	 */
	public static EgyptianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Egyptian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Egyptian month or seasonal descriptor: " + monthName);

		// For seasonal tokens, day parameter acts as an internal flag or defaults to 1
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		// Standard Egyptian civil calendar parameter validation boundaries
		if(month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Egyptian months must have exactly 30 days.");
		if(month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Epagomenal period consists strictly of 5 days.");

		return new EgyptianDateInput(year, month, day);
	}

}
