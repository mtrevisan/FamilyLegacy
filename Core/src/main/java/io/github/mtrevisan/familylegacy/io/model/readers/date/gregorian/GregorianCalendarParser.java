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
package io.github.mtrevisan.familylegacy.io.model.readers.date.gregorian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * High-fidelity polyglot parser for standard solar calendars (Gregorian, Julian, Reformed Julian).
 * Dynamically resolves month names (full or abbreviated) across all system locales and intercepts seasonal text markers.
 * Natively parses historical English dual-dating expressions (e.g., "1704/5") and maps them to historical timelines.
 */
public final class GregorianCalendarParser{

	private static final String SLASH = "/";

	private static final Map<String, Integer> SEASONS = new HashMap<>();
	static{
		// AUTOMATIC GLOBAL SEASON REGISTRATION
		// Sweep across all available system locales on startup and dynamically extract
		// localized strings for quarters/seasons to completely avoid manual code maintenance.
		for(final Locale locale : Locale.getAvailableLocales()){
			try{
				// 1. Extract Quarter-of-Year localized textual names from java.time (mapping onto astronomical seasons)
				// Q1 = Spring, Q2 = Summer, Q3 = Autumn, Q4 = Winter (Northern Civil/Astronomical baselines)
				final DateTimeFormatter quarterFormatterLong = new DateTimeFormatterBuilder()
					.appendPattern("qqqq")
					.toFormatter(locale);
				final DateTimeFormatter quarterFormatterShort = new DateTimeFormatterBuilder()
					.appendPattern("qqq")
					.toFormatter(locale);

				// Sample dummy date elements to extract localized strings safely
				LocalDate temporal = LocalDate.of(2000, 3, 21);
				registerSeasonString(quarterFormatterLong.format(temporal), CalendarType.SEASON_SPRING);
				registerSeasonString(quarterFormatterShort.format(temporal), CalendarType.SEASON_SPRING);

				temporal = LocalDate.of(2000, 6, 21);
				registerSeasonString(quarterFormatterLong.format(temporal), CalendarType.SEASON_SUMMER);
				registerSeasonString(quarterFormatterShort.format(temporal), CalendarType.SEASON_SUMMER);

				temporal = LocalDate.of(2000, 9, 21);
				registerSeasonString(quarterFormatterLong.format(temporal), CalendarType.SEASON_AUTUMN);
				registerSeasonString(quarterFormatterShort.format(temporal), CalendarType.SEASON_AUTUMN);

				temporal = LocalDate.of(2000, 12, 21);
				registerSeasonString(quarterFormatterLong.format(temporal), CalendarType.SEASON_WINTER);
				registerSeasonString(quarterFormatterShort.format(temporal), CalendarType.SEASON_WINTER);
			}
			catch(Exception ignored){}
		}

		// Hardcoded fallback anchors for core legacy and shorthand symbols
		SEASONS.put("aut", CalendarType.SEASON_AUTUMN);
		SEASONS.put("win", CalendarType.SEASON_WINTER);
		SEASONS.put("spr", CalendarType.SEASON_SPRING);
		SEASONS.put("sum", CalendarType.SEASON_SUMMER);
	}

	private static void registerSeasonString(final String text, final int flag){
		if(StringUtils.isNotBlank(text) && !StringUtils.isNumeric(text))
			SEASONS.put(text.toLowerCase(Locale.ROOT).trim(), flag);
	}

	/**
	 * Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets, and dual-dating fractions.
	 * Group 1: Optional Day. Group 2: Month name. Group 3: Civil Year. Group 4: Optional historical fraction string.
	 */
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\d+|[\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)(?:/(\\d+))?");

	/** RegEx pattern matching inverted textual strings like "August 2, 2000" or "August 2 2000" */
	private static final Pattern INVERTED_TEXTUAL_PATTERN = Pattern.compile("^([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)\\s*,?\\s*(\\d+)(?:/(\\d+))?$");

	private static final Pattern DUAL_DATE_PATTERN = Pattern.compile(".*\\d{4}/\\d{1,4}$");


	private GregorianCalendarParser(){}


	/**
	 * Parses a raw Gregorian text expression into a validated structural container using exact flags.
	 * Dynamically resolves month names (full or abbreviated) across all system locales.
	 *
	 * @param rawDate the string representing the Gregorian date (e.g., "15 Gennaio 1953", "Aug 2026", "Estate 1953")
	 * @return a configured GregorianDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format or unknown month
	 */
	public static GregorianDateInput parse(final String rawDate){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("Date string context cannot be empty.");

		final String normalized = normalize(rawDate);

		// Match via the consolidated structural regex pattern
		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Gregorian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int civilYear = Integer.parseInt(matcher.group(3));
		final String dualYearFraction = matcher.group(4);

		// Check if the token maps straight to a registered seasonal marker
		int month = 0;
		boolean isSeason = false;
		if(SEASONS.containsKey(monthName)){
			month = SEASONS.get(monthName);
			isSeason = true;
		}
		else
			// Resolve multi-lingual full or abbreviated months using internal Java internationalization tables
			month = resolvePolyglotMonth(monthName);

		// Operational fallback for standalone seasonal records
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		if(month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		// Calculate the final historical year mapping boundary
		int finalHistoricalYear = civilYear;
		// Dual dating strictly applies to transition months (January, February, March)
		if(dualYearFraction != null && !isSeason && month <= 3){
			if(dualYearFraction.length() <= 2){
				// Handles shorthand formats like "1704/5" -> extracts 1700 prefix + 5
				final int centuryPrefix = Math.floorDiv(civilYear, 100) * 100;
				finalHistoricalYear = centuryPrefix + Integer.parseInt(dualYearFraction);
			}
			else
				// Handles full explicit expansions like "1704/1705"
				finalHistoricalYear = Integer.parseInt(dualYearFraction);
		}

		return new GregorianDateInput(finalHistoricalYear, month, day);
	}

	private static String normalize(final String rawDate){
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

		// Extract the leading text token to check for season status
		final String[] textTokens = StringUtils.split(normalized);
		boolean isSeasonExpression = false;
		if(textTokens.length > 0){
			final String leadingToken = textTokens[0].toLowerCase(Locale.ROOT)
				.trim();
			if(SEASONS.containsKey(leadingToken))
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

	private static boolean isPureNumericDataset(final String[] tokens){
		for(int i = 0; i < 3; i ++)
			if(!StringUtils.isNumeric(tokens[i]))
				return false;
		return true;
	}

	/**
	 * Iterates over available network locales to map full or abbreviated text back to its calendar numeric month index.
	 */
	private static int resolvePolyglotMonth(final String token){
		if(StringUtils.isNumeric(token))
			return Integer.parseInt(token);

		for(final Locale locale : Locale.getAvailableLocales()){
			final int month = parseMonthWithLocale(token, locale);
			if(month != -1)
				return month;
		}

		throw new IllegalArgumentException("Unknown Gregorian month or seasonal descriptor across global locale parameters: " + token);
	}

	private static int parseMonthWithLocale(final String token, final Locale locale){
		try{
			// Build a dynamic parser instance configured to match full (MMMM), short (MMM), or numeric (M) textual components
			final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.appendPattern("[MMMM][MMM][M]")
				.toFormatter(locale);

			return formatter.parse(token)
				.get(ChronoField.MONTH_OF_YEAR);
		}
		catch(Exception e){
			return -1;
		}
	}

}
