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

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
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
	/**
	 * Comment of {@link #RESOURCE} declaring that a season word is kept even if it is also the abbreviation of a month in
	 * another language (otherwise the month wins, and the collision is documented by a {@code # collision (month wins): }
	 * comment).
	 */
	private static final String SEASON_WINS_PREFIX = "# collision (season wins): ";

	// Set of language tags that do not use the solar Gregorian/Julian calendar frameworks as their primary script
	private static final Set<String> NON_SOLAR_LANGUAGES = new HashSet<>(Arrays.asList("th", "lo", "my", "dz", "bo",
		"am"));

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	private static final Map<String, Integer> SEASONS = new HashMap<>();
	/** Words that are different seasons in different languages: without the language of the date they are rejected. */
	private static final Set<String> AMBIGUOUS_SEASONS = new HashSet<>();
	/**
	 * All the months of each word, with the languages where the word has that meaning (a word can be a different month
	 * in different languages).
	 */
	private static final Map<String, Map<Integer, Set<Locale>>> MONTH_MEANINGS = new HashMap<>();
	/**
	 * All the seasons of each word, with the languages where the word has that meaning, even if a month with the same
	 * name wins over them.
	 */
	private static final Map<String, Map<Integer, Set<Locale>>> SEASON_MEANINGS = new HashMap<>();
	/** The names of each season (the full one first), by language. */
	private static final Map<Locale, Map<Integer, List<String>>> SEASON_WORDS = new HashMap<>();
	static{
		final Locale[] locales = Locale.getAvailableLocales();
		for(final Locale locale : locales){
			final String language = locale.getLanguage();

			// Guard clause against empty root configurations and non-solar language parameters
			if(language.isEmpty() || NON_SOLAR_LANGUAGES.contains(language))
				continue;

			// Extract standard solar months (1 to 12) across Full and Short text styles (the lookups are in lowercase)
			for(final Month month : Month.values()){
				putMonth(month.getDisplayName(TextStyle.FULL, locale).toLowerCase(Locale.ROOT), month.getValue(), locale);
				putMonth(month.getDisplayName(TextStyle.SHORT, locale).toLowerCase(Locale.ROOT), month.getValue(), locale);
			}
		}

		// A season word does not hide a month with the same name, unless declared otherwise
		final Set<String> seasonWins = new HashSet<>();

		try(final InputStream is = GregorianCalendarParser.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			while((line = reader.readLine()) != null){
				if(line.startsWith(SEASON_WINS_PREFIX))
					seasonWins.add(StringUtils.split(line.substring(SEASON_WINS_PREFIX.length()))[0]);
				if(line.isBlank() || line.startsWith("#"))
					continue;

				final String[] fields = StringUtils.split(line, ';');
				putSeason(fields[0], fields[1], CalendarSeason.SEASON_AUTUMN, seasonWins);
				putSeason(fields[0], fields[2], CalendarSeason.SEASON_WINTER, seasonWins);
				putSeason(fields[0], fields[3], CalendarSeason.SEASON_SPRING, seasonWins);
				putSeason(fields[0], fields[4], CalendarSeason.SEASON_SUMMER, seasonWins);
			}
		}
		catch(final IOException ioe){
			throw new UncheckedIOException(ioe);
		}
		for(final String word : AMBIGUOUS_SEASONS)
			SEASONS.remove(word);
	}

	private static void putMonth(final String word, final int month, final Locale locale){
		MONTHS.put(word, month);
		putMeaning(MONTH_MEANINGS, word, month, languageOf(locale));
	}

	private static void putSeason(final String languageTags, final String words, final int season,
			final Set<String> seasonWins){
		final String[] seasonWords = StringUtils.split(words, ',');
		final List<Locale> languages = new ArrayList<>();
		for(final String languageTag : StringUtils.split(languageTags, '/')){
			final Locale language = languageOf(Locale.forLanguageTag(languageTag));
			languages.add(language);
			SEASON_WORDS.computeIfAbsent(language, k -> new HashMap<>())
				.putIfAbsent(season, List.of(seasonWords));
		}

		for(final String word : seasonWords){
			for(final Locale language : languages)
				putMeaning(SEASON_MEANINGS, word, season, language);

			if(!MONTHS.containsKey(word) || seasonWins.contains(word)){
				final Integer previous = SEASONS.put(word, season);
				if(previous != null && previous != season)
					AMBIGUOUS_SEASONS.add(word);
			}
		}
	}

	private static void putMeaning(final Map<String, Map<Integer, Set<Locale>>> meanings, final String word,
			final int meaning, final Locale language){
		meanings.computeIfAbsent(word, k -> new LinkedHashMap<>())
			.computeIfAbsent(meaning, k -> new LinkedHashSet<>())
			.add(language);
	}

	/** The language (and script, e.g. Serbian in Latin script) of a locale, without the region. */
	private static Locale languageOf(final Locale locale){
		return new Locale.Builder()
			.setLanguage(locale.getLanguage())
			.setScript(locale.getScript())
			.build();
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
	/** The historical names of each month (the Latin nominative first). */
	private static final Map<Integer, List<String>> HISTORICAL_NAMES = new HashMap<>();
	/** The language of the historical month names. */
	private static final Locale LATIN = Locale.of("la");
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

	/**
	 * The months of each word in the Western European languages, whose meanings win over the homonymous months of the
	 * other languages (e.g. English "may" over "may", November in Basaa and October in Duala).
	 */
	private static final Map<String, Map<Integer, Set<Locale>>> WESTERN_MONTH_MEANINGS = new HashMap<>();
	static{
		for(final Locale locale : WESTERN_LOCALES)
			for(final Month month : Month.values())
				for(final TextStyle style : new TextStyle[]{TextStyle.FULL, TextStyle.SHORT})
					putMeaning(WESTERN_MONTH_MEANINGS, month.getDisplayName(style, locale).toLowerCase(Locale.ROOT),
						month.getValue(), locale);
	}

	private static void registerHistoricalMonth(final int month, final String... names){
		for(final String name : names)
			HISTORICAL_MONTHS.put(name, month);
		HISTORICAL_NAMES.put(month, List.of(names));
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

	/** Trailing language of the date, by name or code, between parentheses (e.g. "1 pay 2006 (asturiano)"). */
	private static final Pattern LANGUAGE_MARKER = Pattern.compile("\\s*\\(([^()]+)\\)\\s*$");


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
		else if(dayGroup != null)
			// When a day is present, strictly resolve as a month (seasons cannot have a day number)
			month = resolvePolyglotMonth(monthName);
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
		final DateWord dateWord = dateWord(text);
		if(dateWord == null)
			return List.of();

		final Map<Integer, Set<Locale>> meanings = meanings(dateWord.word(), dateWord.day(), dateWord.languages());
		if(meanings.size() < 2)
			return List.of();

		// a language declared by the date that does not resolve the word is replaced by the one of each reading
		final String unmarkedDate = removeLanguage(rawDate);
		final String unmarkedText = removeLanguage(text);
		final Pattern wordPattern = wordPattern(dateWord.word());
		final Matcher wordMatcher = wordPattern.matcher(unmarkedDate);
		final boolean written = wordMatcher.find();
		final String target = (written? unmarkedDate: dateWord.normalized());
		// the readings are checked without the parts that this parser does not know
		final String checkedTarget = (written && wordPattern.matcher(unmarkedText).find()? unmarkedText: dateWord.normalized());
		final WrittenWord writtenWord = new WrittenWord(wordPattern, scripts(dateWord.word()),
			(written && Character.isUpperCase(wordMatcher.group().codePointAt(0))));
		final List<DateInterpretation> readings = new ArrayList<>(meanings.size());
		meanings.forEach((meaning, languages) -> readings.add(
			reading(target, checkedTarget, writtenWord, meaning, languages, locale)));
		return readings;
	}

	/**
	 * Builds the reading of a date for one meaning of its month word.
	 *
	 * @param target        the date
	 * @param checkedTarget the date without the parts that this parser does not know, where the reading is checked
	 * @param writtenWord   the month word as written in the date
	 * @param meaning       the month (1 to 12), or the season (a negative number)
	 * @param languages     the languages where the word has that meaning
	 * @param locale        the language of the names of the languages, preferred among the languages of the meaning
	 * @return the date as written followed by its language, or with the word replaced if no language resolves it
	 */
	private static DateInterpretation reading(final String target, final String checkedTarget,
			final WrittenWord writtenWord, final int meaning, final Set<Locale> languages, final Locale locale){
		// the date is kept as written, and its language tells the meaning of the word
		for(final Locale language : sortLanguages(languages, locale)){
			final String marker = " (" + language.getDisplayLanguage(locale) + ")";
			if(hasSingleMeaning(checkedTarget + marker, meaning))
				return new DateInterpretation(target + marker, describe(meaning, Set.of(), locale));
		}

		// the language does not resolve the word (e.g. "hydref", October and autumn in Welsh)
		return new DateInterpretation(unambiguousReading(target, checkedTarget, writtenWord, meaning, languages, locale),
			describe(meaning, languages, locale));
	}

	/**
	 * Removes the language declared by a date, if any.
	 */
	private static String removeLanguage(final String date){
		final Matcher languageMatcher = LANGUAGE_MARKER.matcher(date);
		return (languageMatcher.find() && languagesOf(languageMatcher.group(1)) != null
			? date.substring(0, languageMatcher.start())
			: date);
	}

	/**
	 * The month word as written in a date.
	 *
	 * @param pattern     the pattern of the word
	 * @param scripts     the scripts of the word
	 * @param capitalized whether the word begins with an uppercase letter
	 */
	private record WrittenWord(Pattern pattern, Set<Character.UnicodeScript> scripts, boolean capitalized){}

	private static Set<Character.UnicodeScript> scripts(final String text){
		final Set<Character.UnicodeScript> scripts = new HashSet<>();
		text.codePoints()
			.mapToObj(Character.UnicodeScript::of)
			.filter(script -> script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED)
			.forEach(scripts::add);
		return scripts;
	}

	/**
	 * @param word       the month word, in lowercase
	 * @param day        the day, or {@code 0} if missing
	 * @param normalized the normalized date
	 * @param languages  the languages declared by the date, or {@code null} if missing
	 */
	private record DateWord(String word, int day, String normalized, Set<String> languages){}

	/**
	 * Extracts the month word, the day, and the language of a date.
	 *
	 * @param text the date expression
	 * @return the month word, the day, and the language, or {@code null} if the date has no month word, or cannot be
	 * 	parsed (the errors are reported by the parsing)
	 */
	private static DateWord dateWord(final String text){
		if(StringUtils.isBlank(text))
			return null;

		try{
			final CleanedDate cleaned = clean(text);
			final String normalized = cleaned.normalized();
			final Matcher matcher = DATE_PATTERN.matcher(normalized);
			if(StringUtils.isNumeric(normalized) || !matcher.matches())
				return null;

			final String dayGroup = matcher.group(1);
			final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);
			final String word = matcher.group(2)
				.toLowerCase(Locale.ROOT)
				.trim();
			return new DateWord(word, day, normalized, cleaned.languages());
		}
		catch(final IllegalArgumentException iae){
			return null;
		}
	}

	/**
	 * Collects the meanings (months from 1 to 12, or seasons as negative numbers) of a month word, the one chosen by
	 * {@link #parse(String)} first.
	 *
	 * @param word      the (lowercase) month word
	 * @param day       the day of the date, or {@code 0} if missing (a season has no days, and a month has a maximum day)
	 * @param languages the languages declared by the date, or {@code null} if missing
	 * @return the meanings of the word, each one with the languages where the word has that meaning
	 */
	private static Map<Integer, Set<Locale>> meanings(final String word, final int day, final Set<String> languages){
		if(languages != null)
			return meaningsInLanguages(word, day, languages);

		final Map<Integer, Set<Locale>> meanings = new LinkedHashMap<>();
		try{
			final Integer season = (day == 0? SEASONS.get(word): null);
			meanings.put((season != null? season: resolvePolyglotMonth(word)), new LinkedHashSet<>());
		}
		catch(final IllegalArgumentException ignored){
			// the word has no meaning chosen by the parser (e.g. a different season in different languages)
		}
		if(day == 0)
			addMeanings(meanings, SEASON_MEANINGS.get(word));
		// a month of the Latin or Western European documents hides the homonymous months of the other languages
		final Integer historicalMonth = HISTORICAL_MONTHS.get(word);
		final Map<Integer, Set<Locale>> westernMonths = WESTERN_MONTH_MEANINGS.get(word);
		if(historicalMonth != null || westernMonths != null){
			if(historicalMonth != null)
				addMeanings(meanings, Map.of(historicalMonth, Set.of(LATIN)));
			addMeanings(meanings, westernMonths);
		}
		else
			addMeanings(meanings, MONTH_MEANINGS.get(word));

		removeShorterMonths(meanings, day);
		return meanings;
	}

	/**
	 * Collects the meanings of a month word in the languages declared by the date.
	 *
	 * @param word      the (lowercase) month word
	 * @param day       the day of the date, or {@code 0} if missing
	 * @param languages the codes of the languages declared by the date
	 * @return the meanings of the word in those languages
	 */
	private static Map<Integer, Set<Locale>> meaningsInLanguages(final String word, final int day,
			final Set<String> languages){
		final Map<Integer, Set<Locale>> meanings = new LinkedHashMap<>();
		if(StringUtils.isNumeric(word)){
			// the number of a month is the same in every language
			try{
				meanings.put(resolvePolyglotMonth(word), new LinkedHashSet<>());
			}
			catch(final IllegalArgumentException ignored){
				// not a month
			}
			return meanings;
		}

		if(day == 0)
			addMeanings(meanings, SEASON_MEANINGS.get(word), languages);
		final Integer historicalMonth = HISTORICAL_MONTHS.get(word);
		if(historicalMonth != null && languages.contains(LATIN.getLanguage()))
			addMeanings(meanings, Map.of(historicalMonth, Set.of(LATIN)));
		addMeanings(meanings, MONTH_MEANINGS.get(word), languages);
		if(meanings.isEmpty())
			// forms of the month names not in the lookup tables
			for(final Locale locale : Locale.getAvailableLocales())
				if(languages.contains(locale.getLanguage())){
					final int month = parseMonthWithLocale(word, locale);
					if(month != -1)
						meanings.computeIfAbsent(month, k -> new LinkedHashSet<>())
							.add(languageOf(locale));
				}

		removeShorterMonths(meanings, day);
		return meanings;
	}

	/** A month shorter than the day is not a reading. */
	private static void removeShorterMonths(final Map<Integer, Set<Locale>> meanings, final int day){
		meanings.keySet()
			.removeIf(meaning -> day > 0 && (meaning < 1 || day > MONTH_MAX_DAYS[meaning - 1]));
	}

	private static void addMeanings(final Map<Integer, Set<Locale>> meanings, final Map<Integer, Set<Locale>> others){
		if(others != null)
			others.forEach((meaning, languages) -> meanings.computeIfAbsent(meaning, k -> new LinkedHashSet<>())
				.addAll(languages));
	}

	/**
	 * Adds the meanings that a word has in the given languages.
	 */
	private static void addMeanings(final Map<Integer, Set<Locale>> meanings, final Map<Integer, Set<Locale>> others,
			final Set<String> languages){
		if(others != null)
			others.forEach((meaning, meaningLanguages) -> {
				final Set<Locale> declaredLanguages = new LinkedHashSet<>();
				for(final Locale language : meaningLanguages)
					if(languages.contains(language.getLanguage()))
						declaredLanguages.add(language);
				if(!declaredLanguages.isEmpty())
					meanings.computeIfAbsent(meaning, k -> new LinkedHashSet<>())
						.addAll(declaredLanguages);
			});
	}

	/**
	 * Replaces the month word of a date with a name of one of its meanings that has no other meaning.
	 *
	 * @param target        the date
	 * @param checkedTarget the date without the parts that this parser does not know, where the readings are checked
	 * @param writtenWord   the month word as written in the date
	 * @param meaning       the month (1 to 12), or the season (a negative number)
	 * @param languages     the languages where the word has that meaning
	 * @param locale        the language preferred among the languages of the meaning, and the fallback one
	 * @return the date with the name of the month or season in the language (and script) of the word, otherwise in the
	 * 	language of the locale, in English, or as a number
	 */
	private static String unambiguousReading(final String target, final String checkedTarget,
			final WrittenWord writtenWord, final int meaning, final Set<Locale> languages, final Locale locale){
		for(final Locale language : sortLanguages(languages, locale))
			for(final String name : names(meaning, language))
				// the reading keeps the script of the word (e.g. the Turkish "yaz" does not become the Uyghur "ياز")
				if(scripts(name).equals(writtenWord.scripts())
						&& hasSingleMeaning(replace(checkedTarget, writtenWord, name), meaning))
					return replace(target, writtenWord, name);
		for(final Locale language : new Locale[]{Locale.of(locale.getLanguage()), Locale.ENGLISH})
			for(final String name : names(meaning, language))
				if(hasSingleMeaning(replace(checkedTarget, writtenWord, name), meaning))
					return replace(target, writtenWord, name);

		if(meaning < 0)
			throw new IllegalStateException("No unambiguous name for the season " + meaning);
		// the number of a month is never ambiguous
		return replace(target, writtenWord, String.valueOf(meaning));
	}

	private static String replace(final String target, final WrittenWord writtenWord, final String name){
		final String replacement = (writtenWord.capitalized()? StringUtils.capitalize(name): name);
		return writtenWord.pattern()
			.matcher(target)
			.replaceFirst(Matcher.quoteReplacement(replacement));
	}

	/**
	 * The names of a month or season in a language, the full one first.
	 */
	private static List<String> names(final int meaning, final Locale language){
		if(meaning < 0)
			return SEASON_WORDS.getOrDefault(language, Map.of())
				.getOrDefault(meaning, List.of());
		if(LATIN.equals(language))
			return HISTORICAL_NAMES.get(meaning);

		final Month month = Month.of(meaning);
		return List.of(month.getDisplayName(TextStyle.FULL, language), month.getDisplayName(TextStyle.SHORT, language));
	}

	private static boolean hasSingleMeaning(final String date, final int meaning){
		final DateWord dateWord = dateWord(date);
		return (dateWord != null
			&& meanings(dateWord.word(), dateWord.day(), dateWord.languages()).keySet().equals(Set.of(meaning)));
	}

	/**
	 * Orders the languages of a meaning: the language of the locale first, then Latin and the Western European languages,
	 * then the others.
	 */
	private static List<Locale> sortLanguages(final Set<Locale> languages, final Locale locale){
		final Set<String> westernLanguages = new HashSet<>();
		westernLanguages.add(LATIN.getLanguage());
		for(final Locale westernLocale : WESTERN_LOCALES)
			westernLanguages.add(westernLocale.getLanguage());

		final List<Locale> sorted = new ArrayList<>(languages);
		sorted.sort(Comparator.comparingInt(language -> (language.getLanguage().equals(locale.getLanguage())
			? 0
			: (westernLanguages.contains(language.getLanguage())? 1: 2))));
		return sorted;
	}

	/** Maximum number of languages listed in the description of a reading. */
	private static final int MAX_DESCRIBED_LANGUAGES = 3;

	/**
	 * Describes a meaning in the language of the locale, with the languages where the word has that meaning (e.g.
	 * "agosto (cornico)").
	 */
	private static String describe(final int meaning, final Set<Locale> languages, final Locale locale){
		final String name;
		if(meaning > 0)
			name = Month.of(meaning).getDisplayName(TextStyle.FULL_STANDALONE, locale);
		else{
			final List<String> seasonNames = names(meaning, Locale.of(locale.getLanguage()));
			name = (!seasonNames.isEmpty()? seasonNames: names(meaning, Locale.ENGLISH)).getFirst();
		}

		final List<String> languageNames = sortLanguages(languages, locale).stream()
			.map(language -> language.getDisplayLanguage(locale))
			.filter(StringUtils::isNotBlank)
			.distinct()
			.toList();
		if(languageNames.isEmpty())
			return name;

		final StringJoiner joiner = new StringJoiner(", ", name + " (", ")");
		languageNames.stream()
			.limit(MAX_DESCRIBED_LANGUAGES)
			.forEach(joiner::add);
		if(languageNames.size() > MAX_DESCRIBED_LANGUAGES)
			joiner.add("…");
		return joiner.toString();
	}

	/**
	 * Builds the pattern of a (possibly multi-word) month word as written in a date, whose words can be separated by
	 * spaces or by the delimiters that {@link #normalize(String)} turns into spaces.
	 */
	private static Pattern wordPattern(final String word){
		final StringJoiner joiner = new StringJoiner("[\\s./-]+");
		for(final String part : StringUtils.split(word))
			joiner.add(Pattern.quote(part));
		return Pattern.compile("(?iu)(?<![\\p{L}\\p{M}\\d])" + joiner + "(?![\\p{L}\\p{M}])");
	}

	/**
	 * @param normalized   the normalized date
	 * @param indiction    the indiction, or {@code 0} if missing
	 * @param beforeChrist whether the year is before Christ
	 * @param languages    the languages declared by the date (e.g. "1 pay 2006 (asturiano)"), or {@code null} if missing
	 */
	private record CleanedDate(String normalized, int indiction, boolean beforeChrist, Set<String> languages){}

	/**
	 * Removes from a date the language, the indiction, the formulas, and the era before Christ, and normalizes its
	 * layout.
	 */
	private static CleanedDate clean(final String rawDate){
		// The language of the date, if declared
		final Matcher languageMatcher = LANGUAGE_MARKER.matcher(rawDate);
		Set<String> languages = null;
		String dateText = rawDate;
		if(languageMatcher.find()){
			languages = languagesOf(languageMatcher.group(1));
			if(languages != null)
				dateText = rawDate.substring(0, languageMatcher.start());
		}

		// The indiction is checked against the year by the calendar, after the conversion
		final Indiction.Extraction extraction = Indiction.extract(dateText);
		final String text = removeFormulas(extraction.text());

		// Years before Christ: the historical year N BC is the astronomical year 1 - N (there is no year 0)
		final Matcher beforeChristMatcher = BEFORE_CHRIST_PATTERN.matcher(text);
		final boolean beforeChrist = beforeChristMatcher.find();
		final String normalized = normalize(beforeChrist? text.substring(0, beforeChristMatcher.start()): text);
		return new CleanedDate(normalized, extraction.indiction(), beforeChrist, languages);
	}

	/**
	 * Recognizes a language, by its name in any language (e.g. "asturiano", "Asturian") or by its code (e.g. "ast").
	 *
	 * @param name the name or code of the language
	 * @return the codes of the languages with that name, or {@code null} if the name is not a language
	 */
	private static Set<String> languagesOf(final String name){
		final String key = StringUtils.normalizeSpace(name)
			.toLowerCase(Locale.ROOT);
		final Set<String> languages = LanguageNames.LANGUAGES.get(key);
		if(languages != null)
			return languages;

		final String language = Locale.forLanguageTag(key)
			.getLanguage();
		return (LanguageNames.LANGUAGES.containsKey(language)? Set.of(language): null);
	}

	/**
	 * The names of the languages of the months and seasons, in all the languages, built only when a date declares its
	 * language.
	 */
	private static final class LanguageNames{
		/** The codes of the languages, by their (lowercase) names and codes. */
		private static final Map<String, Set<String>> LANGUAGES = new HashMap<>();
		static{
			final Set<String> codes = new HashSet<>();
			codes.add(LATIN.getLanguage());
			for(final Locale locale : Locale.getAvailableLocales())
				if(!locale.getLanguage().isEmpty())
					codes.add(locale.getLanguage());
			for(final Locale language : SEASON_WORDS.keySet())
				codes.add(language.getLanguage());

			for(final String code : codes){
				final Locale language = Locale.of(code);
				LANGUAGES.computeIfAbsent(code, k -> new HashSet<>())
					.add(code);
				for(final String displayCode : codes)
					LANGUAGES.computeIfAbsent(language.getDisplayLanguage(Locale.of(displayCode)).toLowerCase(Locale.ROOT),
							k -> new HashSet<>())
						.add(code);
			}
		}
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

		final Map<Integer, Set<Locale>> westernMonths = WESTERN_MONTH_MEANINGS.get(token);
		if(westernMonths != null)
			return westernMonths.keySet()
				.iterator()
				.next();

		final Integer month = MONTHS.get(token);
		if(month != null)
			return month;

		final Locale[] locales = Locale.getAvailableLocales();
		for(final Locale locale : locales){
			final int resolvedMonth = parseMonthWithLocale(token, locale);
			if(resolvedMonth != -1)
				return resolvedMonth;
		}

		if(AMBIGUOUS_SEASONS.contains(token))
			throw new IllegalArgumentException("The word is a different season in different languages: " + token);
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
