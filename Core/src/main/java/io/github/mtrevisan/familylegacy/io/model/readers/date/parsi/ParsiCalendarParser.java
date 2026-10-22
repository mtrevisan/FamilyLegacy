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
package io.github.mtrevisan.familylegacy.io.model.readers.date.parsi;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Zoroastrian Parsi (Yezdegirdi) calendar dates.
 * <p>
 * Supported examples:
 * - 1 Fravardin 1390
 * - ۲۲ فروردین ۱۳۹۰
 * - 3 Gatha 1385
 * </p>
 */
public final class ParsiCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Parsi Zoroastrian month names (Shenshai/Kadmi traditional script)
		MONTHS.put("fravardin", 1);
		MONTHS.put("ardibehest", 2);
		MONTHS.put("khordad", 3);
		MONTHS.put("tir", 4);
		MONTHS.put("amardad", 5);
		MONTHS.put("sharivar", 6);
		MONTHS.put("meher", 7);
		MONTHS.put("ava", 8);
		MONTHS.put("adar", 9);
		MONTHS.put("dae", 10);
		MONTHS.put("bahman", 11);
		MONTHS.put("aspandarmad", 12);

		// Intercalary holy days (Gatha / Panje days)
		MONTHS.put("gatha", 13);
		MONTHS.put("panje", 13);

		// Traditional Gujarati script names (highly common in Parsi records)
		MONTHS.put("ફરવરદીન", 1);
		MONTHS.put("અર્દિબહેશ્ત", 2);
		MONTHS.put("ખોરદાદ", 3);
		MONTHS.put("તીર", 4);
		MONTHS.put("અમરદાદ", 5);
		MONTHS.put("શહરીવર", 6);
		MONTHS.put("મેહર", 7);
		MONTHS.put("આવાં", 8);
		MONTHS.put("આદર", 9);
		MONTHS.put("દએ", 10);
		MONTHS.put("બહમન", 11);
		MONTHS.put("અસ્પંદારમદ", 12);
		MONTHS.put("ગાથા", 13);
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(\\S+)\\s+(\\S+)\\s+(\\S+)");


	private ParsiCalendarParser(){}


	/**
	 * Parses a raw Parsi text string into a structured model.
	 *
	 * @param rawDate the string representing the Parsi date
	 * @return a container with year, month, and day fields
	 */
	public static ParsiDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Parsi date format: " + rawDate);

		final int day = Integer.parseInt(matcher.group(1));
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Parsi month or holy term: " + monthName);

		final int year = Integer.parseInt(matcher.group(3));

		// Validation boundaries
		if(month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Parsi months must have exactly 30 days.");
		if(month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Gatha intercalary period must be between 1 and 5 days.");

		return new ParsiDateInput(year, month, day);
	}

	/**
	 * Converts Gujarati, Persian, and Arabic digits to standard ASCII numbers.
	 */
	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);

			if(c >= '۰' && c <= '۹')
				// Persian-Arabic
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				// Indic
				sb.append((char)('0' + c - '٠'));
			else if(c >= '૦' && c <= '૯')
				// Gujarati native numbers
				sb.append((char)('0' + c - '૦'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
