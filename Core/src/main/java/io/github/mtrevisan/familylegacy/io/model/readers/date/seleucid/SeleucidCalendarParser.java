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
package io.github.mtrevisan.familylegacy.io.model.readers.date.seleucid;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Seleucid (Syro-Macedonian) calendar date expressions.
 * Supports Macedonian names, Syriac equivalents, and standard Latin translations.
 */
public final class SeleucidCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: October
		MONTHS.put("october", 1);
		MONTHS.put("hyperberetaios", 1);
		MONTHS.put("tishrin qdim", 1);

		// Month 2: November
		MONTHS.put("november", 2);
		MONTHS.put("dios", 2);
		MONTHS.put("tishrin hray", 2);

		// Month 3: December
		MONTHS.put("december", 3);
		MONTHS.put("apellaios", 3);
		MONTHS.put("kanun qdim", 3);

		// Month 4: January
		MONTHS.put("january", 4);
		MONTHS.put("audynaios", 4);
		MONTHS.put("kanun hray", 4);

		// Month 5: February
		MONTHS.put("february", 5);
		MONTHS.put("peritios", 5);
		MONTHS.put("shbat", 5);

		// Month 6: March
		MONTHS.put("march", 6);
		MONTHS.put("dystros", 6);
		MONTHS.put("adar", 6);

		// Month 7: April
		MONTHS.put("april", 7);
		MONTHS.put("xanthikos", 7);
		MONTHS.put("nisan", 7);

		// Month 8: May
		MONTHS.put("may", 8);
		MONTHS.put("artemisios", 8);
		MONTHS.put("iyar", 8);

		// Month 9: June
		MONTHS.put("june", 9);
		MONTHS.put("daisios", 9);
		MONTHS.put("haziran", 9);

		// Month 10: July
		MONTHS.put("july", 10);
		MONTHS.put("panemos", 10);
		MONTHS.put("tamuz", 10);

		// Month 11: August
		MONTHS.put("august", 11);
		MONTHS.put("loios", 11);
		MONTHS.put("ab", 11);

		// Month 12: September
		MONTHS.put("september", 12);
		MONTHS.put("gorpiose", 12);
		MONTHS.put("aylar", 12);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private SeleucidCalendarParser(){}


	/**
	 * Parses a raw Seleucid text expression into a validated structural container.
	 *
	 * @param rawDate the date string (e.g., "15 Nisan 315" or "1 October 500")
	 * @return a configured SeleucidDateInput instance
	 */
	public static SeleucidDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Seleucid date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Seleucid month entry: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		// Validation mapping onto Julian month limits
		if(day < 1 || day > 31)
			throw new IllegalArgumentException("Invalid day count for the targeted Julian boundary.");

		return new SeleucidDateInput(year, month, day);
	}

}
