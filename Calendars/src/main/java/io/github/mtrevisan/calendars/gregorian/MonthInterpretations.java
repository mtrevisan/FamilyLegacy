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
import org.apache.commons.lang3.StringUtils;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.mtrevisan.calendars.gregorian.DateLanguages.removeLanguage;
import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.CleanedDate;
import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.DATE_PATTERN;
import static io.github.mtrevisan.calendars.gregorian.DateTextCleaner.clean;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.HISTORICAL_MONTHS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.LATIN;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.MONTH_MAX_DAYS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.MONTH_MEANINGS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASONS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASON_MEANINGS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.WESTERN_LOCALES;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.WESTERN_MONTH_MEANINGS;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.languageOf;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.names;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.parseMonthWithLocale;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.resolvePolyglotMonth;


/**
 * The meanings of the month word of a date (a month or a season, in one or more languages) and the readings of a date
 * whose word has more than one meaning.
 */
final class MonthInterpretations{

	/** See {@link GregorianCalendarParser#interpretations(String, String, Locale)}. */
	static List<DateInterpretation> interpretationsOf(final String rawDate, final String text, final Locale locale){
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
		meanings.forEach((meaning, languages) -> {
			try{
				readings.add(reading(target, checkedTarget, writtenWord, meaning, languages, locale));
			}
			catch(final IllegalStateException | NoSuchElementException | IllegalArgumentException ignored){
				// a meaning that cannot be written unambiguously (e.g. a season with no name in any language) is not offered
			}
		});
		return (readings.size() < 2? List.of(): readings);
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
	static DateInterpretation reading(final String target, final String checkedTarget,
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
	 * The month word as written in a date.
	 *
	 * @param pattern     the pattern of the word
	 * @param scripts     the scripts of the word
	 * @param capitalized whether the word begins with an uppercase letter
	 */
	record WrittenWord(Pattern pattern, Set<Character.UnicodeScript> scripts, boolean capitalized){}

	static Set<Character.UnicodeScript> scripts(final String text){
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
	record DateWord(String word, int day, String normalized, Set<String> languages){}

	/**
	 * Extracts the month word, the day, and the language of a date.
	 *
	 * @param text the date expression
	 * @return the month word, the day, and the language, or {@code null} if the date has no month word, or cannot be
	 * 	parsed (the errors are reported by the parsing)
	 */
	static DateWord dateWord(final String text){
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
	 * {@link GregorianCalendarParser#parse(String)} first.
	 *
	 * @param word      the (lowercase) month word
	 * @param day       the day of the date, or {@code 0} if missing (a season has no days, and a month has a maximum day)
	 * @param languages the languages declared by the date, or {@code null} if missing
	 * @return the meanings of the word, each one with the languages where the word has that meaning
	 */
	static Map<Integer, Set<Locale>> meanings(final String word, final int day, final Set<String> languages){
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
	static Map<Integer, Set<Locale>> meaningsInLanguages(final String word, final int day,
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
	static void removeShorterMonths(final Map<Integer, Set<Locale>> meanings, final int day){
		meanings.keySet()
			.removeIf(meaning -> day > 0 && (meaning < 1 || day > MONTH_MAX_DAYS[meaning - 1]));
	}

	static void addMeanings(final Map<Integer, Set<Locale>> meanings, final Map<Integer, Set<Locale>> others){
		if(others != null)
			others.forEach((meaning, languages) -> meanings.computeIfAbsent(meaning, k -> new LinkedHashSet<>())
				.addAll(languages));
	}

	/**
	 * Adds the meanings that a word has in the given languages.
	 */
	static void addMeanings(final Map<Integer, Set<Locale>> meanings, final Map<Integer, Set<Locale>> others,
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
	static String unambiguousReading(final String target, final String checkedTarget,
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

	static String replace(final String target, final WrittenWord writtenWord, final String name){
		final String replacement = (writtenWord.capitalized()? StringUtils.capitalize(name): name);
		return writtenWord.pattern()
			.matcher(target)
			.replaceFirst(Matcher.quoteReplacement(replacement));
	}

	static boolean hasSingleMeaning(final String date, final int meaning){
		final DateWord dateWord = dateWord(date);
		return (dateWord != null
			&& meanings(dateWord.word(), dateWord.day(), dateWord.languages()).keySet().equals(Set.of(meaning)));
	}

	/**
	 * Orders the languages of a meaning: the language of the locale first, then Latin and the Western European languages,
	 * then the others.
	 */
	static List<Locale> sortLanguages(final Set<Locale> languages, final Locale locale){
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
	static final int MAX_DESCRIBED_LANGUAGES = 3;

	/**
	 * Describes a meaning in the language of the locale, with the languages where the word has that meaning (e.g.
	 * "agosto (cornico)").
	 */
	static String describe(final int meaning, final Set<Locale> languages, final Locale locale){
		final String name;
		if(meaning > 0)
			name = Month.of(meaning).getDisplayName(TextStyle.FULL_STANDALONE, locale);
		else{
			final List<String> seasonNames = names(meaning, Locale.of(locale.getLanguage()));
			final List<String> available = (!seasonNames.isEmpty()? seasonNames: names(meaning, Locale.ENGLISH));
			name = (!available.isEmpty()? available.getFirst(): String.valueOf(meaning));
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
	 * spaces or by the delimiters that {@link DateTextCleaner#normalize(String)} turns into spaces.
	 */
	static Pattern wordPattern(final String word){
		final StringJoiner joiner = new StringJoiner("[\\s./-]+");
		for(final String part : StringUtils.split(word))
			joiner.add(Pattern.quote(part));
		return Pattern.compile("(?iu)(?<![\\p{L}\\p{M}\\d])" + joiner + "(?![\\p{L}\\p{M}])");
	}


	private MonthInterpretations(){}

}
