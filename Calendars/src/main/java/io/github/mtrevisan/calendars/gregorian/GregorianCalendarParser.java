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

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * High-fidelity polyglot parser for standard solar calendars (Gregorian, Julian, Reformed Julian).
 * Dynamically resolves month names (full or abbreviated) across all system locales and intercepts seasonal text markers.
 * Natively parses historical English dual-dating expressions (e.g., "1704/5") and maps them to historical timelines.
 */
public final class GregorianCalendarParser{

	private static final String SLASH = "/";

	private static final String RESOURCE = "seasons.csv";

	// Set of language tags that do not use the solar Gregorian/Julian calendar frameworks as their primary script
	private static final Set<String> NON_SOLAR_LANGUAGES = new HashSet<>(Arrays.asList("th", "lo", "my", "dz", "bo",
		"am"));

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		final Locale[] locales = Locale.getAvailableLocales();
		for(final Locale locale : locales){
			final String language = locale.getLanguage();

			// Guard clause against empty root configurations and non-solar language parameters
			if(language.isEmpty() || NON_SOLAR_LANGUAGES.contains(language))
				continue;

			// Extract standard solar months (1 to 12) across Full and Short text styles
			for(final Month month : Month.values()){
				MONTHS.put(month.getDisplayName(TextStyle.FULL, locale), month.getValue());
				MONTHS.put(month.getDisplayName(TextStyle.SHORT, locale), month.getValue());
			}
		}

		try(final InputStream is = GregorianCalendarParser.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null){
				if(line.isBlank() || line.startsWith("#"))
					continue;

				final String[] fields = StringUtils.split(line, ';');
				for(final String autumn : StringUtils.split(fields[1], ','))
					MONTHS.put(autumn, CalendarType.SEASON_AUTUMN);
				for(final String winter : StringUtils.split(fields[2], ','))
					MONTHS.put(winter, CalendarType.SEASON_WINTER);
				for(final String spring : StringUtils.split(fields[3], ','))
					MONTHS.put(spring, CalendarType.SEASON_SPRING);
				for(final String summer : StringUtils.split(fields[4], ','))
					MONTHS.put(summer, CalendarType.SEASON_SUMMER);
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
	}

	/**
	 * Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets, and dual-dating fractions.
	 * Group 1: Optional Day. Group 2: Month name (possibly a numeral abbreviation like "7bris"). Group 3: Civil Year.
	 * Group 4: Optional historical fraction string.
	 */
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\d+|\\d{0,2}[\\p{L}\\p{M}][\\p{L}\\p{M}\\s-]*?)\\s+(\\d+)(?:/(\\d+))?");

	/**
	 * Month names not covered by the Java locales: the Latin forms (nominative, genitive, and ablative) of the parish
	 * registers, and the numeral abbreviations of the last four months ("7bris", "8bre", "9ber", "Xbris").
	 */
	private static final Map<String, Integer> HISTORICAL_MONTHS = new HashMap<>();
	static{
		registerHistoricalMonth(1, "ianuarius", "ianuarii", "ianuario", "januarius", "januarii", "januario", "ian", "jan");
		registerHistoricalMonth(2, "februarius", "februarii", "februario", "febr");
		registerHistoricalMonth(3, "martius", "martii", "martio");
		registerHistoricalMonth(4, "aprilis", "aprili");
		registerHistoricalMonth(5, "maius", "maii", "maio", "majus", "maji", "majo");
		registerHistoricalMonth(6, "iunius", "iunii", "iunio", "junius", "junii", "junio");
		registerHistoricalMonth(7, "iulius", "iulii", "iulio", "julius", "julii", "julio");
		registerHistoricalMonth(8, "augustus", "augusti", "augusto");
		registerHistoricalMonth(9, "september", "septembris", "septembri", "7bris", "7bre", "7ber", "7mbris");
		registerHistoricalMonth(10, "october", "octobris", "octobri", "8bris", "8bre", "8ber");
		registerHistoricalMonth(11, "november", "novembris", "novembri", "9bris", "9bre", "9ber", "9mbris");
		registerHistoricalMonth(12, "december", "decembris", "decembri", "10bris", "10bre", "10ber", "xbris", "xbre", "xber", "xmbris");
	}

	private static final Locale[] WESTERN_LOCALES = {Locale.ITALIAN, Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
		Locale.of("es"), Locale.of("pt"), Locale.of("nl")};

	private static void registerHistoricalMonth(final int month, final String... names){
		for(final String name : names)
			HISTORICAL_MONTHS.put(name, month);
	}

	/**
	 * RegEx pattern matching inverted textual strings like "August 2, 2000" or "August 2 2000" (the day must be
	 * separated from the year, so that "August 2000" is not split into day and year)
	 */
	private static final Pattern INVERTED_TEXTUAL_PATTERN = Pattern.compile("^([\\p{L}\\p{M}\\s-]+?)\\s+(\\d{1,2})(?:\\s*,\\s*|\\s+)(\\d+)(?:/(\\d+))?$");

	/** Maximum number of days of each month (February in a leap year). */
	private static final int[] MONTH_MAX_DAYS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	private static final Pattern DUAL_DATE_PATTERN = Pattern.compile(".*\\d{4}/\\d{1,4}$");

	/** Words of the date formulas that carry no information (e.g. "die 5 mensis martii anno Domini 1450", "5 de marzo de 1950"). */
	private static final Pattern FORMULA_WORDS = Pattern.compile(
		"(?iu)(?<![\\p{L}])(?:die|mensis|anno|domini|nativitatis|incarnationis|adi|adì|addì|a dì|the|of|de|del|di|du|des)(?![\\p{L}])");
	/** "A.D." or "AD" next to the year. */
	private static final Pattern ANNO_DOMINI = Pattern.compile("(?i)(?:(?<![\\p{L}])a\\.\\s?d\\.?|(?<![\\p{L}])ad)(?=\\s*\\d)|(?<=\\d)\\s*(?:a\\.\\s?d\\.?|ad)$");
	/** Ordinal suffix of the day (e.g. "1st", "2nd", "1°", "1º"). */
	private static final Pattern DAY_ORDINAL_SUFFIX = Pattern.compile("(?i)(?<![\\p{L}\\d])(\\d{1,2})(?:st|nd|rd|th|°|º)(?![\\p{L}])");

	/** Trailing era marker for the years before Christ: "BC", "BCE", "B.C.", "B.C.E.", or the Italian "a.C.". */
	private static final Pattern BEFORE_CHRIST_PATTERN = Pattern.compile("\\s*(?:(?i:b\\.?\\s?c\\.?(?:\\s?e\\.?)?)|a\\.\\s?C\\.?)\\s*$");


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

		// The indiction is checked against the year by the calendar, after the conversion
		final Indiction.Extraction extraction = Indiction.extract(rawDate);
		final int indiction = extraction.indiction();
		final String text = removeFormulas(extraction.text());

		// Years before Christ: the historical year N BC is the astronomical year 1 - N (there is no year 0)
		final Matcher beforeChristMatcher = BEFORE_CHRIST_PATTERN.matcher(text);
		final boolean beforeChrist = beforeChristMatcher.find();
		final String normalized = normalize(beforeChrist? text.substring(0, beforeChristMatcher.start()): text);

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

		// Check if the token maps straight to a registered seasonal marker
		int month = 0;
		final Integer season = MONTHS.get(monthName);
		if(season != null && season < 0)
			month = season;
		else
			// Resolve multi-lingual full or abbreviated months using internal Java internationalization tables
			month = resolvePolyglotMonth(monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		final int maxDays = (month == 2? februaryMaxDays: (month > 0? MONTH_MAX_DAYS[month - 1]: 0));
		if(dayGroup != null && month > 0 && (day < 1 || day > maxDays))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		// Calculate the final historical year mapping boundary
		int finalHistoricalYear = civilYear;
		// Dual dating strictly applies to transition months (January, February, March)
		final boolean dualYear = (dualYearFraction != null && month > 0 && month <= 3);
		if(dualYear){
			if(dualYearFraction.length() < 4){
				// Handles shorthand formats like "1704/5" or "1731/2", replacing the last digits of the civil year:
				// the historical year is the one after the civil year (e.g. "1709/10" -> 1710, "1699/00" -> 1700)
				final int modulus = (dualYearFraction.length() == 1? 10: (dualYearFraction.length() == 2? 100: 1000));
				finalHistoricalYear = civilYear - Math.floorMod(civilYear, modulus) + Integer.parseInt(dualYearFraction);
				if(finalHistoricalYear <= civilYear)
					finalHistoricalYear += modulus;
			}
			else
				// Handles full explicit expansions like "1704/1705"
				finalHistoricalYear = Integer.parseInt(dualYearFraction);
		}

		return new GregorianDateInput(toAstronomicalYear(finalHistoricalYear, beforeChrist), month, day, dualYear,
			indiction);
	}

	/**
	 * Removes the formulas of the documents that carry no date information: the Latin "die ... mensis ...",
	 * "anno (Domini)", the Venetian "adì", the Romance "de"/"del"/"di" between the components, "A.D.", and the ordinal
	 * suffixes of the day ("1st", "1°").
	 */
	private static String removeFormulas(final String text){
		String result = FORMULA_WORDS.matcher(text).replaceAll(StringUtils.SPACE);
		result = ANNO_DOMINI.matcher(result).replaceAll(StringUtils.SPACE);
		result = DAY_ORDINAL_SUFFIX.matcher(result).replaceAll("$1");
		return StringUtils.normalizeSpace(result);
	}

	private static int toAstronomicalYear(final int historicalYear, final boolean beforeChrist){
		if(beforeChrist && historicalYear < 1)
			throw new IllegalArgumentException("There is no year " + historicalYear + " before Christ");

		return (beforeChrist? 1 - historicalYear: historicalYear);
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
		// Normalize the reduced ISO format: "2000 01" -> transform to "01 2000"
		else if(numericTokens.length == 2 && StringUtils.isNumeric(numericTokens[0]) && StringUtils.isNumeric(numericTokens[1])
				&& numericTokens[0].length() == 4)
			normalized = numericTokens[1] + StringUtils.SPACE + numericTokens[0];

		// Extract the leading text token to check for season status
		final String[] textTokens = StringUtils.split(normalized);
		boolean isSeasonExpression = false;
		if(textTokens.length > 0){
			final String leadingToken = textTokens[0].toLowerCase(Locale.ROOT)
				.trim();
			if(MONTHS.containsKey(leadingToken))
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
	 * Resolves a month name of the Western European documents (Latin, and the Italian, English, French, German, Spanish,
	 * Portuguese, and Dutch names, full or abbreviated), without trying the other locales, so that the words of a free
	 * text are not taken for months.
	 *
	 * @param token the (lowercase) word
	 * @return the month (1 to 12), or {@code -1} if the word is not a month name
	 */
	public static int parseWesternMonthName(final String token){
		if(StringUtils.isBlank(token) || StringUtils.isNumeric(token))
			return -1;

		final Integer historicalMonth = HISTORICAL_MONTHS.get(token);
		if(historicalMonth != null)
			return historicalMonth;

		for(final Locale locale : WESTERN_LOCALES){
			final int month = parseMonthWithLocale(token, locale);
			if(month != -1)
				return month;
		}
		return -1;
	}

	/**
	 * Iterates over available network locales to map full or abbreviated text back to its calendar numeric month index.
	 */
	private static int resolvePolyglotMonth(final String token){
		if(StringUtils.isNumeric(token)){
			final int month = Integer.parseInt(token);
			if(month < 1 || month > 12)
				throw new IllegalArgumentException("Month must be between 1 and 12: " + token);

			return month;
		}

		final Integer historicalMonth = HISTORICAL_MONTHS.get(token);
		if(historicalMonth != null)
			return historicalMonth;

		final Locale[] locales = Locale.getAvailableLocales();
		for(final Locale locale : locales){
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
