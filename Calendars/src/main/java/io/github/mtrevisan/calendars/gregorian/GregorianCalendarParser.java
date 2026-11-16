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

import io.github.mtrevisan.calendars.spi.DateInterpretation;
import io.github.mtrevisan.calendars.text.DateFormulas;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.CleanedDate;
import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.DATE_PATTERN;
import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.clean;
import static io.github.mtrevisan.calendars.gregorian.MonthInterpretations.meanings;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.MONTH_MAX_DAYS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASONS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASON_MEANINGS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.resolvePolyglotMonth;


/**
 * High-fidelity polyglot parser for standard solar calendars (Gregorian, Julian, Reformed Julian).
 * Dynamically resolves month names (full or abbreviated) across all system locales and intercepts seasonal text markers.
 * Natively parses historical English dual-dating expressions (e.g., "1704/5") and maps them to historical timelines.
 */
public final class GregorianCalendarParser{

	/** First day of March that belongs to the new year (25 March, the start of the year in the Incarnation styles). */
	private static final int DUAL_YEAR_END_DAY = 25;

	/** Maximum number of parsed dates kept (the cache is emptied when full). */
	private static final int CACHE_MAX_SIZE = 8192;
	/** The dates already parsed successfully (the failures are not kept, they throw each time). */
	private static final Map<ParseKey, GregorianDateInput> CACHE = new ConcurrentHashMap<>();

	private record ParseKey(String rawDate, int februaryMaxDays){}


	private GregorianCalendarParser(){}


	/**
	 * Parses a raw Gregorian text expression into a validated structural container using exact flags.
	 * Dynamically resolves month names (full or abbreviated) across all system locales.
	 *
	 * @param rawDate the string representing the Gregorian date (e.g., "15 Gennaio 1953", "Aug 2026", "1953", "15 March 44 BC")
	 * @return a configured GregorianDateInput instance; the year is astronomical (1 BC is the year 0)
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format or unknown month
	 */
	public static GregorianDateInput parse(final String rawDate){
		return parse(rawDate, MONTH_MAX_DAYS[1]);
	}

	/**
	 * Parses a raw Gregorian text expression, allowing a longer February (e.g. the Swedish 30 February 1712).
	 *
	 * @param rawDate          the string representing the date
	 * @param februaryMaxDays  the maximum number of days accepted for February
	 * @return a configured GregorianDateInput instance; the year is astronomical (1 BC is the year 0)
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format or unknown month
	 */
	public static GregorianDateInput parse(final String rawDate, final int februaryMaxDays){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("Date string context cannot be empty.");

		// The result depends only on the arguments (and it is immutable): the same dates are parsed over and over
		final ParseKey key = new ParseKey(rawDate, februaryMaxDays);
		final GregorianDateInput cached = CACHE.get(key);
		if(cached != null)
			return cached;

		final GregorianDateInput parsed = parseUncached(rawDate, februaryMaxDays);
		if(CACHE.size() >= CACHE_MAX_SIZE)
			CACHE.clear();
		CACHE.put(key, parsed);
		return parsed;
	}

	private static GregorianDateInput parseUncached(final String rawDate, final int februaryMaxDays){
		final CleanedDate cleaned = clean(rawDate);
		final int indiction = cleaned.indiction();
		final boolean beforeChrist = cleaned.beforeChrist();
		final String normalized = cleaned.normalized();

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new GregorianDateInput(toAstronomicalYear(Integer.parseInt(normalized), beforeChrist), 0, 0, false,
				indiction);

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

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Check if the token maps straight to a registered seasonal marker
		int month;
		if(cleaned.languages() != null){
			// The language of the date chooses among the meanings of the word (the day is checked below, also against
			// the longer February of some calendars, so only its presence is passed)
			final Set<Integer> meanings = meanings(monthName, (dayGroup != null? 1: 0), cleaned.languages())
				.keySet();
			if(meanings.size() != 1)
				throw new IllegalArgumentException("The word " + monthName + " has " + (meanings.isEmpty()? "no": "more than one")
					+ " meaning as a month or season in the language of the date: " + rawDate);
			month = meanings.iterator()
				.next();
		}
		else if(dayGroup != null){
			// When a day is present, strictly resolve as a month (seasons cannot have a day number)
			try{
				month = resolvePolyglotMonth(monthName);
			}
			catch(final IllegalArgumentException iae){
				if(SEASONS.containsKey(monthName) || SEASON_MEANINGS.containsKey(monthName))
					throw new IllegalArgumentException("A season cannot have a day: " + rawDate, iae);
				throw iae;
			}
		}
		else{
			final Integer season = SEASONS.get(monthName);
			if(season != null)
				month = season;
			else
				// Resolve multilingual full or abbreviated months using internal Java internationalization tables
				month = resolvePolyglotMonth(monthName);
		}

		final int maxDays = (month == 2? februaryMaxDays: (month > 0? MONTH_MAX_DAYS[month - 1]: 0));
		if(dayGroup != null && month > 0 && (day < 1 || day > maxDays))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		// Calculate the final historical year mapping boundary
		int finalHistoricalYear = civilYear;
		final boolean dualYear = (dualYearFraction != null);
		if(dualYear){
			validateDualYear(rawDate, civilYear, dualYearFraction, month, day);
			// the historical year is the one after the civil year (e.g. "1709/10" -> 1710, "1699/00" -> 1700)
			finalHistoricalYear = civilYear + 1;
		}

		return new GregorianDateInput(toAstronomicalYear(finalHistoricalYear, beforeChrist), month, day, dualYear,
			indiction);
	}

	/**
	 * Checks a dual year (e.g. "1704/5"): the second year must be the one after the first (written fully, or with its last
	 * 1 to 3 digits), and the date must fall before the start of the year on 25 March, i.e. in January, February, or in
	 * March before the 25th (a month without the day is accepted).
	 */
	private static void validateDualYear(final String rawDate, final int civilYear, final String fraction, final int month,
			final int day){
		if(month < 1 || month > 3 || month == 3 && day >= DUAL_YEAR_END_DAY)
			throw new IllegalArgumentException("A dual year is valid only from 1 January to 24 March: " + rawDate);

		final int length = fraction.length();
		final long next = civilYear + 1L;
		final boolean consistent = (length >= 4
			? Long.parseLong(fraction) == next
			: Long.parseLong(fraction) == Math.floorMod(next, (long)Math.pow(10, length)));
		if(!consistent)
			throw new IllegalArgumentException("The second year of a dual year must follow the first one (" + civilYear + "/"
				+ next + "): " + rawDate);
	}

	/**
	 * Lists the readings of a date whose month word has more than one meaning: a season in a language and a month in
	 * another (e.g. "est", summer in Italian and August in Cornish), different seasons (e.g. "yaz", spring in Azerbaijani
	 * and summer in Turkish), or different months (e.g. "listopad", November in Czech and October in Croatian).
	 * <p>
	 * Each reading is the date as written, followed by the name (in the language of the given locale) of a language where
	 * the word has that meaning (e.g. "1 pay 2006 (asturiano)", November, and "1 pay 2006 (nuer)", July). If the language
	 * does not resolve the word (e.g. "hydref", October and autumn in Welsh), the word is replaced, where it is written, by
	 * an unambiguous name of its meaning in that language, or in the language of the given locale, in English, or by the
	 * number of the month. The reading chosen by {@link #parse(String)} comes first.
	 * </p>
	 *
	 * @param rawDate the date expression
	 * @param locale  the language of the names of the languages and of the descriptions of the readings, preferred among
	 *                the languages of a meaning
	 * @return the readings of the date, or an empty list if it has only one reading (or cannot be parsed)
	 */
	public static List<DateInterpretation> interpretations(final String rawDate, final Locale locale){
		return interpretations(rawDate, rawDate, locale);
	}

	/**
	 * Lists the readings of a date whose month word has more than one meaning (see
	 * {@link #interpretations(String, Locale)}).
	 *
	 * @param rawDate the date expression, where the month word is replaced
	 * @param text    the date expression without the parts that this parser does not know (e.g. "avant Pâques"), where
	 *                the month word is searched
	 * @param locale  the language of the names of the languages and of the descriptions of the readings, preferred among
	 *                the languages of a meaning
	 * @return the readings of the date, or an empty list if it has only one reading (or cannot be parsed)
	 */
	public static List<DateInterpretation> interpretations(final String rawDate, final String text, final Locale locale){
		return MonthInterpretations.interpretationsOf(rawDate, text, locale);
	}

	/**
	 * Removes the language declared by a date between parentheses (e.g. "1 pay 2006 (asturiano)"), if it is the name or
	 * the code of a language.
	 *
	 * @param date the date expression
	 * @return the date without the declared language
	 */
	public static String removeLanguage(final String date){
		return DateLanguages.removeLanguage(date);
	}

	private static int toAstronomicalYear(final int historicalYear, final boolean beforeChrist){
		return DateFormulas.toAstronomicalYear(historicalYear, beforeChrist);
	}

	/**
	 * Resolves a month name of the Western European documents (Latin, and the Italian, English, French, German, Spanish,
	 * Portuguese, and Dutch names, full or abbreviated), without trying the other locales, so that the words of a free
	 * text are not taken for months.
	 *
	 * @param token the (lowercase) word
	 * @return the month (1 to 12), or {@code -1} if the word is not a month name
	 */
	public static int parseWesternMonthName(final String token){
		return MonthNames.parseWesternMonthName(token);
	}

}
