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
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
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
import java.util.concurrent.ConcurrentHashMap;


/**
 * The names of the months and of the seasons in all the languages (the Java locales, the Latin forms of the parish
 * registers, and the {@code seasons.csv} resource), and the resolution of a word into a month.
 */
final class MonthNames{

	static final String RESOURCE = "seasons.csv";
	/** Fields of each line of {@link #RESOURCE}: the languages, then the words of autumn, winter, spring, and summer. */
	static final int SEASON_FIELDS = 5;
	/**
	 * Comment of {@link #RESOURCE} declaring that a season word is kept even if it is also the abbreviation of a month in
	 * another language (otherwise the month wins, and the collision is documented by a {@code # collision (month wins): }
	 * comment).
	 */
	static final String SEASON_WINS_PREFIX = "# collision (season wins): ";

	// Set of language tags that do not use the solar Gregorian/Julian calendar frameworks as their primary script
	static final Set<String> NON_SOLAR_LANGUAGES = new HashSet<>(Arrays.asList("th", "lo", "my", "dz", "bo",
		"am"));

	static final Map<String, Integer> MONTHS = new HashMap<>();
	static final Map<String, Integer> SEASONS = new HashMap<>();
	/** Words that are different seasons in different languages: without the language of the date they are rejected. */
	static final Set<String> AMBIGUOUS_SEASONS = new HashSet<>();
	/**
	 * All the months of each word, with the languages where the word has that meaning (a word can be a different month
	 * in different languages).
	 */
	static final Map<String, Map<Integer, Set<Locale>>> MONTH_MEANINGS = new HashMap<>();
	/**
	 * All the seasons of each word, with the languages where the word has that meaning, even if a month with the same
	 * name wins over them.
	 */
	static final Map<String, Map<Integer, Set<Locale>>> SEASON_MEANINGS = new HashMap<>();
	/** The names of each season (the full one first), by language. */
	static final Map<Locale, Map<Integer, List<String>>> SEASON_WORDS = new HashMap<>();

	static{
		// Guard clause against empty root configurations and non-solar language parameters
		final List<Locale> solarLocales = Arrays.stream(Locale.getAvailableLocales())
			.filter(locale -> !locale.getLanguage().isEmpty() && !NON_SOLAR_LANGUAGES.contains(locale.getLanguage()))
			.toList();
		// The names are read in parallel (it is the slow part), and then put in the tables always in the same order
		final String[][] localeNames = MonthNameLoader.load(solarLocales);
		for(int i = 0; i < localeNames.length; i ++){
			final Locale locale = solarLocales.get(i);
			final String[] words = localeNames[i];
			// Extract standard solar months (1 to 12) across Full and Short text styles (the lookups are in lowercase)
			for(int w = 0; w < words.length; w ++)
				putMonth(words[w], w / 2 + 1, locale);
		}

		// A season word does not hide a month with the same name, unless declared otherwise
		final Set<String> seasonWins = new HashSet<>();

		try(final InputStream is = MonthNames.class.getResourceAsStream(RESOURCE)){
			if(is == null)
				throw new IllegalStateException("Missing resource " + RESOURCE);

			final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			String line;
			int lineNumber = 0;
			while((line = reader.readLine()) != null){
				lineNumber ++;
				if(line.startsWith(SEASON_WINS_PREFIX)){
					final String[] collision = StringUtils.split(line.substring(SEASON_WINS_PREFIX.length()));
					if(collision == null || collision.length == 0)
						throw new IllegalStateException("Malformed collision comment at line " + lineNumber + " of " + RESOURCE);
					seasonWins.add(collision[0]);
				}
				if(line.isBlank() || line.startsWith("#"))
					continue;

				final String[] fields = StringUtils.splitPreserveAllTokens(line, ';');
				if(fields.length != SEASON_FIELDS)
					throw new IllegalStateException("Malformed line " + lineNumber + " of " + RESOURCE + ": expected "
						+ SEASON_FIELDS + " fields (language;autumn;winter;spring;summer) but found " + fields.length + ": " + line);
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

	static void putMonth(final String word, final int month, final Locale locale){
		MONTHS.put(word, month);
		putMeaning(MONTH_MEANINGS, word, month, languageOf(locale));
	}

	static void putSeason(final String languageTags, final String words, final int season,
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

	static void putMeaning(final Map<String, Map<Integer, Set<Locale>>> meanings, final String word,
			final int meaning, final Locale language){
		meanings.computeIfAbsent(word, k -> new LinkedHashMap<>())
			.computeIfAbsent(meaning, k -> new LinkedHashSet<>())
			.add(language);
	}

	/** The language (and script, e.g. Serbian in Latin script) of a locale, without the region. */
	static Locale languageOf(final Locale locale){
		return new Locale.Builder()
			.setLanguage(locale.getLanguage())
			.setScript(locale.getScript())
			.build();
	}

	/**
	 * Month names not covered by the Java locales: the Latin forms (nominative, genitive, and ablative) of the parish
	 * registers, and the numeral abbreviations of the last four months ("7bris", "8bre", "9ber", "Xbris").
	 */
	static final Map<String, Integer> HISTORICAL_MONTHS = new HashMap<>();
	/** The historical names of each month (the Latin nominative first). */
	static final Map<Integer, List<String>> HISTORICAL_NAMES = new HashMap<>();
	/** The language of the historical month names. */
	static final Locale LATIN = Locale.of("la");
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

	static final Locale[] WESTERN_LOCALES = {Locale.ITALIAN, Locale.ENGLISH, Locale.FRENCH, Locale.GERMAN,
		Locale.of("es"), Locale.of("pt"), Locale.of("nl")};

	/**
	 * The months of each word in the Western European languages, whose meanings win over the homonymous months of the
	 * other languages (e.g. English "may" over "may", November in Basaa and October in Duala).
	 */
	static final Map<String, Map<Integer, Set<Locale>>> WESTERN_MONTH_MEANINGS = new HashMap<>();
	static{
		for(final Locale locale : WESTERN_LOCALES)
			for(final Month month : Month.values())
				for(final TextStyle style : new TextStyle[]{TextStyle.FULL, TextStyle.SHORT})
					putMeaning(WESTERN_MONTH_MEANINGS, month.getDisplayName(style, locale).toLowerCase(Locale.ROOT),
						month.getValue(), locale);
	}

	static void registerHistoricalMonth(final int month, final String... names){
		for(final String name : names)
			HISTORICAL_MONTHS.put(name, month);
		HISTORICAL_NAMES.put(month, List.of(names));
	}

	/** Maximum number of days of each month (February in a leap year). */
	static final int[] MONTH_MAX_DAYS = {31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

	/**
	 * The names of a month or season in a language, the full one first.
	 */
	static List<String> names(final int meaning, final Locale language){
		if(meaning < 0)
			return SEASON_WORDS.getOrDefault(language, Map.of())
				.getOrDefault(meaning, List.of());
		if(LATIN.equals(language))
			return HISTORICAL_NAMES.get(meaning);

		final Month month = Month.of(meaning);
		return List.of(month.getDisplayName(TextStyle.FULL, language), month.getDisplayName(TextStyle.SHORT, language));
	}

	/**
	 * Resolves a month name of the Western European documents (Latin, and the Italian, English, French, German, Spanish,
	 * Portuguese, and Dutch names, full or abbreviated), without trying the other locales, so that the words of a free
	 * text are not taken for months.
	 *
	 * @param token the (lowercase) word
	 * @return the month (1 to 12), or {@code -1} if the word is not a month name
	 */
	static int parseWesternMonthName(final String token){
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
	static int resolvePolyglotMonth(final String token){
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

		final Map<Integer, Set<Locale>> meanings = MONTH_MEANINGS.get(token);
		if(meanings != null)
			return preferredMonth(meanings);

		final int fallback = FALLBACK_MONTHS.computeIfAbsent(token, MonthNames::resolveWithLocales);
		if(fallback != -1)
			return fallback;

		if(AMBIGUOUS_SEASONS.contains(token))
			throw new IllegalArgumentException("The word is a different season in different languages: " + token);
		throw new IllegalArgumentException("Unknown Gregorian month or seasonal descriptor across global locale parameters: " + token);
	}

	/**
	 * Picks the month of a word that is a different month in different languages, always in the same way: the month that
	 * the most languages give to the word, and, if the languages are equally many, the month of the language whose code
	 * comes first alphabetically (then the earliest month).
	 */
	static int preferredMonth(final Map<Integer, Set<Locale>> meanings){
		int best = -1;
		int bestLanguages = -1;
		String bestTag = null;
		for(final Map.Entry<Integer, Set<Locale>> entry : meanings.entrySet()){
			final int languages = entry.getValue().size();
			final String tag = entry.getValue().stream()
				.map(Locale::toLanguageTag)
				.min(Comparator.naturalOrder())
				.orElse(StringUtils.EMPTY);
			final boolean better = (languages != bestLanguages
				? languages > bestLanguages
				: (tag.equals(bestTag)? entry.getKey() < best: tag.compareTo(bestTag) < 0));
			if(better){
				best = entry.getKey();
				bestLanguages = languages;
				bestTag = tag;
			}
		}
		return best;
	}

	/** Months of the words that are not in the lookup tables, resolved by the locales (-1 if not a month). */
	static final Map<String, Integer> FALLBACK_MONTHS = new ConcurrentHashMap<>();
	/** Parsers of the month names, one for each locale. */
	static final Map<Locale, DateTimeFormatter> MONTH_FORMATTERS = new ConcurrentHashMap<>();

	static int resolveWithLocales(final String token){
		for(final Locale locale : SortedLocales.LOCALES){
			final int resolvedMonth = parseMonthWithLocale(token, locale);
			if(resolvedMonth != -1)
				return resolvedMonth;
		}
		return -1;
	}

	/** All the locales, always in the same order (sorted once: sorting by language tag is costly). */
	private static final class SortedLocales{
		private static final Locale[] LOCALES;
		static{
			final Locale[] locales = Locale.getAvailableLocales();
			Arrays.sort(locales, Comparator.comparing(Locale::toLanguageTag));
			LOCALES = locales;
		}
	}

	static int parseMonthWithLocale(final String token, final Locale locale){
		// A parser instance configured to match full (MMMM), short (MMM), or numeric (M) textual components
		final DateTimeFormatter formatter = MONTH_FORMATTERS.computeIfAbsent(locale, l -> new DateTimeFormatterBuilder()
			.parseCaseInsensitive()
			.appendPattern("[MMMM][MMM][M]")
			.toFormatter(l));

		//the words that are not a month are the large majority: no exception is thrown (and no stack trace is filled) for them
		final ParsePosition position = new ParsePosition(0);
		final TemporalAccessor parsed;
		try{
			parsed = formatter.parseUnresolved(token, position);
		}
		catch(final RuntimeException ignored){
			return -1;
		}
		if(parsed == null || position.getErrorIndex() >= 0 || position.getIndex() != token.length()
				|| !parsed.isSupported(ChronoField.MONTH_OF_YEAR))
			return -1;

		final long month = parsed.getLong(ChronoField.MONTH_OF_YEAR);
		return (month >= 1 && month <= 12? (int)month: -1);
	}


	private MonthNames(){}

}
