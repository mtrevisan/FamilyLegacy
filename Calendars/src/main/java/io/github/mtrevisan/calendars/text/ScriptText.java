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
package io.github.mtrevisan.calendars.text;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Normalization of the names of months and seasons written in any script, and the shared scaffold of the
 * "[day] name year [suffix]" date expressions of the parsers of the non-Gregorian calendars.
 * <p>
 * Unlike {@link TextNormalizer}, it keeps the combining marks (the vowel signs of the Indic scripts, the Thai vowels)
 * and unifies the variant forms of the Arabic-script letters (the Persian yeh U+06CC, the Arabic yeh U+064A and the alef
 * maqsura U+0649; the Persian kaf U+06A9 and the Arabic kaf U+0643; the alef with madda or hamza), so that a key and a
 * text typed with different keyboards are equal. Both the keys of the tables and the text must be normalized with
 * {@link #normalize(String)}.
 * </p>
 */
public final class ScriptText{

	/** The characters that can compose the name of a month (letters, marks, apostrophes, dots of the abbreviations). */
	public static final String NAME_CHARACTERS = "\\p{L}\\p{M}'.\\s-";

	private static final Pattern DAY_NAME_YEAR = Pattern.compile("(?:(\\d+)\\s+)?([" + NAME_CHARACTERS + "]+?)\\s+(\\d+)");
	private static final Pattern DAY_NAME_YEAR_SUFFIX = Pattern.compile("(?:(\\d+)\\s+)?([" + NAME_CHARACTERS + "]+?)\\s+(\\d+)"
		+ "(?:\\s+([\\p{L}\\p{M}]+))?");
	private static final Pattern APOSTROPHES = Pattern.compile("[’‘ʻʼʿʾ`´ʹʽ]");
	/** The Arabic vowel marks (harakat), the dagger alef, and the tatweel. */
	private static final Pattern ARABIC_MARKS = Pattern.compile("[ً-ٰٟـ]");
	private static final Pattern JOINERS = Pattern.compile("[‌‍‎‏]");

	/** The day (or {@code null}), the name, the year, and the suffix (or {@code null}) of a date expression. */
	public record Match(Integer day, String name, int year, String suffix){}

	/**
	 * A date expression resolved against a table of months: a year alone has the month {@code 0} and no day.
	 *
	 * @param year   the year
	 * @param month  the month (negative for a season), or {@code 0} if the expression is a year alone
	 * @param day    the day, or {@code null} if missing
	 * @param suffix the word after the year (e.g. the reckoning), or {@code null}
	 */
	public record Parsed(int year, int month, Integer day, String suffix){

		/** @return the day, or {@code 0} if missing */
		public int dayOrZero(){
			return (day != null? day: 0);
		}

	}


	private ScriptText(){}


	/**
	 * Replaces the numerals of every script with the ASCII ones, composes (NFC) and lowercases the text, unifies the
	 * apostrophes and the variant letters of the Arabic script, and removes the zero-width joiners and the Arabic
	 * vowel marks.
	 *
	 * @param text	the text
	 * @return	the normalized text, with single spaces between the words
	 */
	public static String normalize(final String text){
		if(text == null)
			return StringUtils.EMPTY;

		String normalized = Normalizer.normalize(TextNormalizer.asciiDigits(text), Normalizer.Form.NFC)
			.toLowerCase(Locale.ROOT);
		normalized = JOINERS.matcher(normalized)
			.replaceAll(StringUtils.EMPTY);
		normalized = ARABIC_MARKS.matcher(normalized)
			.replaceAll(StringUtils.EMPTY);
		normalized = APOSTROPHES.matcher(normalized)
			.replaceAll("'");
		normalized = normalized
			//yeh and alef maqsura -> Persian yeh
			.replace('ي', 'ی')
			.replace('ى', 'ی')
			//Arabic kaf -> Persian kaf
			.replace('ك', 'ک')
			//alef with madda or hamza -> alef
			.replace('آ', 'ا')
			.replace('أ', 'ا')
			.replace('إ', 'ا');
		return StringUtils.normalizeSpace(normalized);
	}

	/**
	 * Registers a name in a table, normalized as the text will be.
	 *
	 * @param table	the table
	 * @param name	the name
	 * @param value	the value
	 */
	public static void register(final Map<String, Integer> table, final String name, final int value){
		table.put(normalize(name), value);
	}

	/**
	 * Matches a normalized "[day] name year" expression.
	 *
	 * @param normalized	the text normalized with {@link #normalize(String)}
	 * @param withSuffix	whether a word can follow the year (e.g. the reckoning "Kadmi")
	 * @return	the components, or {@code null} if the text does not have that layout
	 * @throws IllegalArgumentException	if a number is too large
	 */
	public static Match match(final String normalized, final boolean withSuffix){
		final Matcher matcher = (withSuffix? DAY_NAME_YEAR_SUFFIX: DAY_NAME_YEAR).matcher(normalized);
		if(!matcher.matches())
			return null;

		final String day = matcher.group(1);
		final String name = matcher.group(2)
			.trim();
		final String suffix = (withSuffix? matcher.group(4): null);
		return new Match(day != null? parseNumber(day): null, name, parseNumber(matcher.group(3)), suffix);
	}

	/**
	 * Parses a "[day] name year [suffix]" expression, or a year alone (that stands for the first day of the year), whose
	 * name is looked up in the table of the months and of the seasons of a calendar. The checks of the range of the day, that
	 * depend on the calendar, are left to the caller.
	 *
	 * @param rawDate    the text
	 * @param calendar   the name of the calendar, for the error messages
	 * @param months     the months and the seasons (negative) by normalized name
	 * @param withSuffix whether a word can follow the year (e.g. the reckoning "Kadmi")
	 * @return the components
	 * @throws IllegalArgumentException if the layout is wrong, the name is unknown, or a season is given with a day
	 */
	public static Parsed parse(final String rawDate, final String calendar, final Map<String, Integer> months,
			final boolean withSuffix){
		return parseNormalized(normalize(rawDate), rawDate, calendar, months, withSuffix);
	}

	/**
	 * As {@link #parse(String, String, Map, boolean)}, for a text already normalized (e.g. after removing the era markers).
	 *
	 * @param normalized the text normalized with {@link #normalize(String)}
	 * @param rawDate    the original text, for the error messages
	 * @param calendar   the name of the calendar, for the error messages
	 * @param months     the months and the seasons (negative) by normalized name
	 * @param withSuffix whether a word can follow the year
	 * @return the components
	 * @throws IllegalArgumentException if the layout is wrong, the name is unknown, or a season is given with a day
	 */
	public static Parsed parseNormalized(final String normalized, final String rawDate, final String calendar,
			final Map<String, Integer> months, final boolean withSuffix){
		if(StringUtils.isNumeric(normalized))
			return new Parsed(parseNumber(normalized), 0, null, null);

		final Match match = match(normalized, withSuffix);
		if(match == null)
			throw new IllegalArgumentException("Invalid " + calendar + " date format: " + rawDate);

		final Integer month = months.get(match.name());
		if(month == null)
			throw new IllegalArgumentException("Unknown " + calendar + " month or seasonal descriptor: " + match.name());
		rejectDayWithSeason(match.day(), month, rawDate);

		return new Parsed(match.year(), month, match.day(), match.suffix());
	}

	/**
	 * @param number	the numeral
	 * @return	the number
	 * @throws IllegalArgumentException	if the number is too large
	 */
	public static int parseNumber(final String number){
		if(number.length() > 9)
			throw new IllegalArgumentException("The number is too large: " + number);

		return Integer.parseInt(number);
	}

	/**
	 * Checks that a seasonal expression (negative value) is not given with a day, e.g. "5 winter 1500".
	 *
	 * @param day	the day, or {@code null}
	 * @param value	the value of the name (negative for the seasons)
	 * @param text	the original text
	 * @throws IllegalArgumentException	if a day is given with a season
	 */
	public static void rejectDayWithSeason(final Integer day, final int value, final String text){
		if(day != null && value < 0)
			throw new IllegalArgumentException("A season cannot be given with a day: " + text);
	}

}
