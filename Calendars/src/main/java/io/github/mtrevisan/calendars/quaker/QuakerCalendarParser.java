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
package io.github.mtrevisan.calendars.quaker;

import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for the Quaker dates, where the months (and the days) are numbered instead of named, e.g.
 * "4th day 11th month 1702/3", "4 da 11 mo 1702", "the fourth of the eleventh month 1702", "11mo 4 1702", "11th mo 1702".
 */
public final class QuakerCalendarParser{

	private static final Map<Pattern, String> ORDINAL_PATTERNS = new LinkedHashMap<>();
	private static final Map<String, Integer> ORDINALS = new LinkedHashMap<>();
	static{
		final String[] units = {"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth"};
		// the compound ordinals come first, so they are replaced before their last word
		for(int i = 0; i < units.length; i ++){
			ORDINALS.put("twenty " + units[i], 21 + i);
			ORDINALS.put("twenty-" + units[i], 21 + i);
		}
		ORDINALS.put("thirty first", 31);
		ORDINALS.put("thirty-first", 31);
		final String[] simple = {"tenth", "eleventh", "twelfth", "thirteenth", "fourteenth", "fifteenth", "sixteenth",
			"seventeenth", "eighteenth", "nineteenth", "twentieth"};
		for(int i = 0; i < simple.length; i ++)
			ORDINALS.put(simple[i], 10 + i);
		ORDINALS.put("thirtieth", 30);
		for(int i = 0; i < units.length; i ++)
			ORDINALS.put(units[i], 1 + i);

		for(final Map.Entry<String, Integer> ordinal : ORDINALS.entrySet())
			ORDINAL_PATTERNS.put(Pattern.compile("\\b" + Pattern.quote(ordinal.getKey()) + "\\b"),
				StringUtils.SPACE + ordinal.getValue() + StringUtils.SPACE);
	}

	private static final Set<String> MONTH_MARKERS = Set.of("month", "months", "mo", "mon", "mth", "m");
	private static final Set<String> FILLERS = Set.of("the", "of", "day", "da", "d", "st", "nd", "rd", "th", "anno", "in",
		"on", "year", "yr");

	/** Trailing year, optionally dual (e.g. "1702/3" or "1702/1703"). */
	private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{3,4})(?:\\s*/\\s*(\\d{1,4}))?$");
	private static final Pattern DIGIT_LETTER_BOUNDARY = Pattern.compile("(?<=\\d)(?=\\p{L})|(?<=\\p{L})(?=\\d)");


	private QuakerCalendarParser(){}


	/**
	 * Parses a Quaker date.
	 *
	 * @param rawDate the date expression
	 * @return the parsed components
	 * @throws IllegalArgumentException if the expression cannot be parsed
	 */
	public static QuakerDateInput parse(final String rawDate){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("Date string context cannot be empty.");

		String normalized = StringUtils.normalizeSpace(rawDate.toLowerCase(Locale.ROOT)
			.replace('.', ' ')
			.replace(',', ' '));
		for(final Map.Entry<Pattern, String> ordinal : ORDINAL_PATTERNS.entrySet())
			normalized = ordinal.getKey().matcher(normalized).replaceAll(ordinal.getValue());
		normalized = StringUtils.normalizeSpace(normalized);

		final Matcher yearMatcher = YEAR_PATTERN.matcher(normalized);
		if(!yearMatcher.find())
			throw new IllegalArgumentException("Missing year in Quaker date: " + rawDate);

		final int year = Integer.parseInt(yearMatcher.group(1));
		final int dualYear = (yearMatcher.group(2) != null? completeDualYear(year, yearMatcher.group(2)): 0);

		final String[] tokens = StringUtils.split(DIGIT_LETTER_BOUNDARY.matcher(normalized.substring(0, yearMatcher.start()))
			.replaceAll(StringUtils.SPACE));
		int month = 0;
		int day = 0;
		for(int i = 0; i < tokens.length; i ++){
			final String token = tokens[i];
			if(StringUtils.isNumeric(token)){
				// skip an ordinal suffix to look for the month marker
				int next = i + 1;
				if(next < tokens.length && Set.of("st", "nd", "rd", "th", "d").contains(tokens[next])
						&& next + 1 < tokens.length && MONTH_MARKERS.contains(tokens[next + 1]))
					next ++;
				final boolean isMonth = (next < tokens.length && MONTH_MARKERS.contains(tokens[next]));
				if(isMonth && month == 0)
					month = Integer.parseInt(token);
				else if(!isMonth && day == 0)
					day = Integer.parseInt(token);
				else
					throw new IllegalArgumentException("Ambiguous Quaker date: " + rawDate);
			}
			else if(!FILLERS.contains(token) && !MONTH_MARKERS.contains(token))
				throw new IllegalArgumentException("Unknown token '" + token + "' in Quaker date: " + rawDate);
		}

		if(day != 0 && month == 0)
			throw new IllegalArgumentException("Missing month in Quaker date: " + rawDate);
		if(month < 0 || month > 12)
			throw new IllegalArgumentException("Month must be between 1 and 12: " + rawDate);
		if(day < 0 || day > 31)
			throw new IllegalArgumentException("Day must be between 1 and 31: " + rawDate);

		return new QuakerDateInput(year, month, day, dualYear);
	}

	/** Completes a shorthand dual year (e.g. "1709/10" -> 1710, "1699/00" -> 1700). */
	private static int completeDualYear(final int year, final String fraction){
		if(fraction.length() >= 4)
			return Integer.parseInt(fraction);

		final int modulus = (fraction.length() == 1? 10: (fraction.length() == 2? 100: 1000));
		int dualYear = year - Math.floorMod(year, modulus) + Integer.parseInt(fraction);
		if(dualYear <= year)
			dualYear += modulus;
		return dualYear;
	}

}
