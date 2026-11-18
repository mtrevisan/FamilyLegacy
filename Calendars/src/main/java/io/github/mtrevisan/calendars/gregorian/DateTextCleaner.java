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
package io.github.mtrevisan.calendars.gregorian;

import io.github.mtrevisan.calendars.text.DateFormulas;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.mtrevisan.calendars.gregorian.DateLanguages.LANGUAGE_MARKER;
import static io.github.mtrevisan.calendars.gregorian.DateLanguages.languagesOf;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.HISTORICAL_MONTHS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.MONTHS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASONS;


/** The cleaning of a Gregorian date expression: language, indiction, formulas, era, and layout. */
final class DateTextCleaner{

	static final String SLASH = "/";

	/**
	 * Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets, and dual-dating fractions.
	 * Group 1: Optional Day. Group 2: Month name (possibly a numeral abbreviation like "7bris"). Group 3: Civil Year.
	 * Group 4: Optional historical fraction string.
	 */
	static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\d+|\\d{0,2}[\\p{L}\\p{M}][\\p{L}\\p{M}\\s-]*?)\\s+(\\d+)(?:/(\\d+))?");

	/**
	 * RegEx pattern matching inverted textual strings like "August 2, 2000" or "August 2 2000" (the day must be
	 * separated from the year, so that "August 2000" is not split into day and year)
	 */
	static final Pattern INVERTED_TEXTUAL_PATTERN = Pattern.compile("^([\\p{L}\\p{M}\\s-]+?)\\s+(\\d{1,2})(?:\\s*,\\s*|\\s+)(\\d+)(?:/(\\d+))?$");

	static final Pattern DUAL_DATE_PATTERN = Pattern.compile(".*\\d{4}/\\d{1,4}$");

	/**
	 * @param normalized   the normalized date
	 * @param indiction    the indiction, or {@code 0} if missing
	 * @param beforeChrist whether the year is before Christ
	 * @param languages    the languages declared by the date (e.g. "1 pay 2006 (asturiano)"), or {@code null} if missing
	 */
	record CleanedDate(String normalized, int indiction, boolean beforeChrist, Set<String> languages){}

	/**
	 * Removes from a date the language, the indiction, the formulas, and the era before Christ, and normalizes its
	 * layout.
	 */
	static CleanedDate clean(final String rawDate){
		// The language of the date, if declared
		final String plainDate = DateFormulas.plainSpaces(rawDate);
		// the language is between parentheses at the end: without a closing one the pattern cannot match
		final Matcher languageMatcher = (StringUtils.stripEnd(plainDate, null).endsWith(")")
			? LANGUAGE_MARKER.matcher(plainDate)
			: null);
		Set<String> languages = null;
		String dateText = plainDate;
		if(languageMatcher != null && languageMatcher.find()){
			languages = languagesOf(languageMatcher.group(1));
			if(languages != null){
				dateText = plainDate.substring(0, languageMatcher.start());
				// a language (or a code like "(ca)") next to a year alone, with no month word, is not the language of a date
				if(StringUtils.isNumeric(StringUtils.deleteWhitespace(dateText)))
					languages = null;
			}
		}

		// The indiction is checked against the year by the calendar, after the conversion
		final Indiction.Extraction extraction = Indiction.extract(dateText);
		final String text = removeFormulas(extraction.text());

		// Years before Christ: the historical year N BC is the astronomical year 1 - N (there is no year 0)
		final DateFormulas.Era era = DateFormulas.extractEra(text);
		final String normalized = normalize(era.text());
		return new CleanedDate(normalized, extraction.indiction(), era.beforeChrist(), languages);
	}

	/**
	 * Removes the formulas of the documents that carry no date information: the Latin "die ... mensis ...",
	 * "anno (Domini)", the Venetan "adì", the Romance "de"/"del"/"di" between the components, "A.D.", and the ordinal
	 * suffixes of the day ("1st", "1°").
	 */
	static String removeFormulas(final String text){
		//a formula word that is also the name of a month or of a season in some language is not removed
		return DateFormulas.removeFormulas(text, word -> MONTHS.containsKey(word) || HISTORICAL_MONTHS.containsKey(word)
			|| SEASONS.containsKey(word));
	}

	static String normalize(final String rawDate){
		// Uniformly convert delimiters (-, ., /) to clean tracking whitespace
		String normalized = rawDate.replace('-', ' ')
			.replace('.', ' ');
		// If a slash is present, only treat it as a delimiter if it's not a dual-date boundary (e.g., "1704/5")
		if(normalized.contains(SLASH) && !DUAL_DATE_PATTERN.matcher(normalized).matches())
			normalized = normalized.replace('/', ' ');
		normalized = StringUtils.normalizeSpace(normalized);

		// Normalize pure numeric ISO formats: "2000 01 22" -> transform to "22 01 2000"
		final String[] numericTokens = StringUtils.split(normalized);
		if(numericTokens.length >= 3 && isPureNumericDataset(numericTokens) && numericTokens[0].length() == 4){
			// Swaps "yyyy MM dd" to "dd MM yyyy"
			normalized = numericTokens[2] + StringUtils.SPACE + numericTokens[1] + StringUtils.SPACE + numericTokens[0];
			// keep trailing dual-date fragments if present
			if(numericTokens.length == 4)
				normalized += SLASH + numericTokens[3];
		}
		// Normalize the reduced ISO format: "2000 01" -> transform to "01 2000"
		else if(numericTokens.length == 2 && StringUtils.isNumeric(numericTokens[0]) && StringUtils.isNumeric(numericTokens[1])
				&& numericTokens[0].length() == 4)
			normalized = numericTokens[1] + StringUtils.SPACE + numericTokens[0];

		// The inverted format ("August 2, 2000") starts with the month, a word: a date that starts with a digit is left as it is
		if(normalized.isEmpty() || Character.isDigit(normalized.charAt(0)))
			return normalized;

		// Extract the leading text token to check for season status
		final String[] textTokens = StringUtils.split(normalized);
		boolean isSeasonExpression = false;
		if(textTokens.length > 0){
			final String leadingToken = textTokens[0].toLowerCase(Locale.ROOT)
				.trim();
			if(SEASONS.containsKey(leadingToken) && !MONTHS.containsKey(leadingToken))
				isSeasonExpression = true;
		}

		// Conditional Inversion: ONLY rearrange inverted formats if the token is NOT a season
		if(!isSeasonExpression){
			// Normalize inverted textual formats: "August 2, 2000" -> transform to "2 August 2000"
			final Matcher invertedMatcher = INVERTED_TEXTUAL_PATTERN.matcher(normalized);
			if(invertedMatcher.matches()){
				normalized = invertedMatcher.group(2) + StringUtils.SPACE + invertedMatcher.group(1) + StringUtils.SPACE
					+ invertedMatcher.group(3);
				if(invertedMatcher.group(4) != null)
					normalized += SLASH + invertedMatcher.group(4);
			}
		}

		return normalized;
	}

	static boolean isPureNumericDataset(final String[] tokens){
		for(int i = 0; i < 3; i ++)
			if(!StringUtils.isNumeric(tokens[i]))
				return false;
		return true;
	}


	private DateTextCleaner(){}

}
