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
package io.github.mtrevisan.familylegacy.io.model.readers.date.persian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Persian (Solar Hijri) calendar dates.
 * <p>
 * Supported examples:
 * - ۲۲ بهمن ۱۳۵۷
 * - 22 Bahman 1357
 * - 1 Farvardin 1300
 * - ۱ فروردین ۱۳۰۰
 */
public final class PersianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Persian names
		MONTHS.put("فروردین", 1);
		MONTHS.put("اردیبهشت", 2);
		MONTHS.put("خرداد", 3);
		MONTHS.put("تیر", 4);
		MONTHS.put("مرداد", 5);
		MONTHS.put("شهریور", 6);
		MONTHS.put("مهر", 7);
		MONTHS.put("آبان", 8);
		MONTHS.put("آذر", 9);
		MONTHS.put("دی", 10);
		MONTHS.put("بهمن", 11);
		MONTHS.put("اسفند", 12);
		// Transliterations
		MONTHS.put("farvardin", 1);
		MONTHS.put("ordibehesht", 2);
		MONTHS.put("khordad", 3);
		MONTHS.put("tir", 4);
		MONTHS.put("mordad", 5);
		MONTHS.put("shahrivar", 6);
		MONTHS.put("mehr", 7);
		MONTHS.put("aban", 8);
		MONTHS.put("azar", 9);
		MONTHS.put("dey", 10);
		MONTHS.put("bahman", 11);
		MONTHS.put("esfand", 12);

		// Paeez (Autumn) -> Maps to Autumnal Equinox flag
		MONTHS.put("paeez", CalendarType.SEASON_AUTUMN);
		MONTHS.put("payiz", CalendarType.SEASON_AUTUMN);
		MONTHS.put("پاییز", CalendarType.SEASON_AUTUMN);
		// Zemestan (Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("zemestan", CalendarType.SEASON_WINTER);
		MONTHS.put("زمستان", CalendarType.SEASON_WINTER);
		// Bahar (Spring) -> Maps to Vernal Equinox flag
		MONTHS.put("bahar", CalendarType.SEASON_SPRING);
		MONTHS.put("بهار", CalendarType.SEASON_SPRING);
		// Tabestan (Summer) -> Maps to Estival Solstice flag
		MONTHS.put("tabestan", CalendarType.SEASON_SUMMER);
		MONTHS.put("تابستان", CalendarType.SEASON_SUMMER);
	}

	// Adaptive pattern supporting both standalone seasonal notations ("Bahar 1357")
	// and explicit three-token expressions ("22 Bahman 1357")
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([a-zA-Zأ-ی\\s-]+?)\\s+(\\d+)");


	private PersianCalendarParser(){}


	/**
	 * Parses a raw Persian text string into a validated structural model.
	 *
	 * @param rawDate the string representing the Persian date
	 * @return a configured PersianDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static PersianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Persian date: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Persian month or seasonal descriptor: " + monthName);

		// Operational default for pure seasonal entries
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		if(month > 0 && month <= 6 && (day < 1 || day > 31))
			throw new IllegalArgumentException("The first six Persian months consist strictly of 31 days.");
		if(month >= 7 && month <= 11 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Persian months 7 through 11 consist strictly of 30 days.");
		if(month == 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("The final Persian month cannot exceed 30 days.");

		return new PersianDateInput(year, month, day);
	}

	/**
	 * Converts Persian and Arabic-Indic digits to ASCII digits.
	 */
	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);

			if(c >= '۰' && c <= '微')
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				sb.append((char)('0' + c - '٠'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
