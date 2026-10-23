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
package io.github.mtrevisan.calendars.eastasian;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for the East Asian dates counted in the years of an era, with Gregorian months and days.
 * <p>
 * Accepts the native layout ("明治45年7月29日", "民國50年3月1日", "단기 4290년 3월 1일"), Han numerals ("昭和四十五年七月"),
 * the first year of an era ("元年", "gannen"), and romanized or abbreviated layouts ("Meiji 45 7 29", "H1.1.8").
 */
public final class EastAsianEraDateParser{

	private static final Map<Character, Integer> DIGITS = new HashMap<>();
	static{
		registerDigit(0, '〇', '零');
		registerDigit(1, '一', '壱', '壹');
		registerDigit(2, '二', '弐', '貳');
		registerDigit(3, '三', '参', '參');
		registerDigit(4, '四');
		registerDigit(5, '五');
		registerDigit(6, '六');
		registerDigit(7, '七');
		registerDigit(8, '八');
		registerDigit(9, '九');
	}

	private static final Map<Character, Integer> MULTIPLIERS = Map.of('十', 10, '拾', 10, '百', 100, '千', 1000);
	private static final Map<Character, Integer> TENS = Map.of('廿', 20, '卅', 30);

	/** The first year of an era. */
	private static final String FIRST_YEAR = "元";
	private static final String FIRST_YEAR_ROMANIZED = "gannen";

	/**
	 * Named days and months, rewritten as numbers: New Year's Day (元日, 元旦), the first (朔日) and last (晦日) day of a
	 * month, the last day of the year (大晦日), and the traditional Japanese month names (正月, 睦月, ..., 師走).
	 */
	private static final Map<String, String> SPECIAL_NAMES = new LinkedHashMap<>();
	static{
		SPECIAL_NAMES.put("大晦日", "12月晦日");
		SPECIAL_NAMES.put("元日", "1月1日");
		SPECIAL_NAMES.put("元旦", "1月1日");
		SPECIAL_NAMES.put("朔日", "1日");
		SPECIAL_NAMES.put("朔", "1日");
		SPECIAL_NAMES.put("晦日", " 晦 ");
		SPECIAL_NAMES.put("正月", "1月");
		SPECIAL_NAMES.put("睦月", "1月");
		SPECIAL_NAMES.put("如月", "2月");
		SPECIAL_NAMES.put("弥生", "3月");
		SPECIAL_NAMES.put("卯月", "4月");
		SPECIAL_NAMES.put("皐月", "5月");
		SPECIAL_NAMES.put("水無月", "6月");
		SPECIAL_NAMES.put("文月", "7月");
		SPECIAL_NAMES.put("葉月", "8月");
		SPECIAL_NAMES.put("長月", "9月");
		SPECIAL_NAMES.put("神無月", "10月");
		SPECIAL_NAMES.put("霜月", "11月");
		SPECIAL_NAMES.put("師走", "12月");
		SPECIAL_NAMES.put("極月", "12月");
		SPECIAL_NAMES.put("臘月", "12月");
		SPECIAL_NAMES.put("腊月", "12月");
	}
	/** The last day of the month, after the rewriting of 晦日. */
	private static final String LAST_DAY_OF_MONTH = "晦";
	/** Leap month marker (閏, 闰, the Korean 윤, or the romanized "urū"/"yun"/"leap"). */
	private static final Pattern LEAP_MARKER = Pattern.compile("[閏闰윤]|(?<![\\p{L}])(?:uru|yun|leap)(?![\\p{L}])");

	/** Year, month, and day markers (Chinese, Japanese, Korean). */
	private static final Pattern UNIT_MARKERS = Pattern.compile("[年月日號号년월일]");
	private static final Pattern SEPARATORS = Pattern.compile("[\\s.,/-]+");
	/** Combining diacritical marks of the romanized era names (e.g. "Shōwa"). */
	private static final Pattern LATIN_DIACRITICS = Pattern.compile("[\\u0300-\\u036f]");

	private static void registerDigit(final int value, final char... symbols){
		for(final char symbol : symbols)
			DIGITS.put(symbol, value);
	}


	private EastAsianEraDateParser(){}


	/**
	 * Parses a date counted in the years of an era.
	 *
	 * @param rawDate    the date expression
	 * @param eraAliases the (lowercase) names of each era, mapped to the era identifier
	 * @param defaultEra the era to use when the expression has no era, or {@code null} if the era is mandatory
	 * @return the parsed components
	 * @throws IllegalArgumentException if the expression cannot be parsed
	 */
	public static EastAsianDateInput parse(final String rawDate, final Map<String, String> eraAliases, final String defaultEra){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("Date string context cannot be empty.");

		String normalized = Normalizer.normalize(rawDate, Normalizer.Form.NFKC)
			.toLowerCase(Locale.ROOT)
			.trim();
		normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
		normalized = Normalizer.normalize(LATIN_DIACRITICS.matcher(normalized).replaceAll(StringUtils.EMPTY), Normalizer.Form.NFC);

		String era = null;
		String rest = normalized;
		final List<String> aliases = eraAliases.keySet().stream()
			.sorted(Comparator.comparingInt(String::length).reversed())
			.toList();
		for(final String alias : aliases)
			if(normalized.startsWith(alias) && !continuesWord(normalized, alias)){
				era = eraAliases.get(alias);
				rest = normalized.substring(alias.length());
				break;
			}
		if(era == null){
			if(defaultEra == null)
				throw new IllegalArgumentException("Unknown or missing era: " + rawDate);

			era = defaultEra;
		}

		for(final Map.Entry<String, String> special : SPECIAL_NAMES.entrySet())
			rest = rest.replace(special.getKey(), special.getValue());
		final Matcher leapMatcher = LEAP_MARKER.matcher(rest);
		final boolean leapMonth = leapMatcher.find();
		if(leapMonth)
			rest = leapMatcher.replaceAll(StringUtils.SPACE);

		rest = UNIT_MARKERS.matcher(rest).replaceAll(StringUtils.SPACE)
			.replace(FIRST_YEAR_ROMANIZED, FIRST_YEAR);
		final String[] tokens = StringUtils.split(SEPARATORS.matcher(rest).replaceAll(StringUtils.SPACE));
		if(tokens.length == 0 || tokens.length > 3)
			throw new IllegalArgumentException("Invalid era date format: " + rawDate);

		final int year = parseNumber(tokens[0], rawDate);
		final int month = (tokens.length > 1? parseNumber(tokens[1], rawDate): 0);
		final int day = (tokens.length > 2
			? (tokens[2].equals(LAST_DAY_OF_MONTH)? EastAsianDateInput.LAST_DAY: parseNumber(tokens[2], rawDate))
			: 0);
		if(leapMonth && month == 0)
			throw new IllegalArgumentException("Leap marker without a month: " + rawDate);
		if(year < 1)
			throw new IllegalArgumentException("The year of an era starts from 1: " + rawDate);
		if(month < 0 || month > 12)
			throw new IllegalArgumentException("Month must be between 1 and 12: " + rawDate);

		return new EastAsianDateInput(era, year, month, leapMonth, day);
	}

	/** Whether a romanized alias is only the beginning of a longer word (e.g. "s" in "september"). */
	private static boolean continuesWord(final String text, final String alias){
		return (alias.length() < text.length() && Character.isLetter(alias.charAt(alias.length() - 1))
			&& alias.charAt(alias.length() - 1) < 0x80 && Character.isLetter(text.charAt(alias.length())));
	}

	/**
	 * Parses an Arabic or a Han numeral, either positional ("二〇二四") or with multipliers ("四十五", "廿九").
	 */
	static int parseNumber(final String token, final String rawDate){
		if(StringUtils.isNumeric(token))
			return Integer.parseInt(token);
		if(token.equals(FIRST_YEAR))
			return 1;

		final boolean positional = token.chars()
			.noneMatch(c -> MULTIPLIERS.containsKey((char)c) || TENS.containsKey((char)c));
		int total = 0;
		int current = -1;
		for(int i = 0; i < token.length(); i ++){
			final char c = token.charAt(i);
			final Integer digit = DIGITS.get(c);
			if(digit != null){
				if(positional)
					total = total * 10 + digit;
				else
					current = digit;
			}
			else if(TENS.containsKey(c))
				total += TENS.get(c);
			else if(MULTIPLIERS.containsKey(c)){
				total += (current < 0? 1: current) * MULTIPLIERS.get(c);
				current = -1;
			}
			else
				throw new IllegalArgumentException("Invalid number '" + token + "' in era date: " + rawDate);
		}
		return total + Math.max(current, 0);
	}

}
