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

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.mtrevisan.calendars.gregorian.MonthNames.LATIN;
import static io.github.mtrevisan.calendars.gregorian.MonthNames.SEASON_WORDS;


/** The language that a date can declare between parentheses (e.g. "1 pay 2006 (asturiano)"). */
final class DateLanguages{

	/** Trailing language of the date, by name or code, between parentheses (e.g. "1 pay 2006 (asturiano)"). */
	static final Pattern LANGUAGE_MARKER = Pattern.compile("\\s*\\(([^()]+)\\)\\s*$");

	/**
	 * Removes the language declared by a date between parentheses (e.g. "1 pay 2006 (asturiano)"), if it is the name or
	 * the code of a language.
	 *
	 * @param date the date expression
	 * @return the date without the declared language
	 */
	static String removeLanguage(final String date){
		final Matcher languageMatcher = LANGUAGE_MARKER.matcher(date);
		return (languageMatcher.find() && languagesOf(languageMatcher.group(1)) != null
			? date.substring(0, languageMatcher.start())
			: date);
	}

	/**
	 * Recognizes a language, by its name in any language (e.g. "asturiano", "Asturian") or by its code (e.g. "ast").
	 *
	 * @param name the name or code of the language
	 * @return the codes of the languages with that name, or {@code null} if the name is not a language
	 */
	static Set<String> languagesOf(final String name){
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
		private static final Map<String, Set<String>> LANGUAGES;
		static{
			final Set<String> codes = new HashSet<>();
			codes.add(LATIN.getLanguage());
			for(final Locale locale : Locale.getAvailableLocales())
				if(!locale.getLanguage().isEmpty())
					codes.add(locale.getLanguage());
			for(final Locale language : SEASON_WORDS.keySet())
				codes.add(language.getLanguage());

			LANGUAGES = languagesByName(codes);
		}
	}

	/** Names every language in every language (some hundreds of them, so the pairs are some hundreds of thousands). */
	private static Map<String, Set<String>> languagesByName(final Set<String> codes){
		final List<Locale> displayLocales = codes.stream()
			.map(Locale::of)
			.toList();
		final Map<String, Set<String>> languages = new HashMap<>();
		for(final String code : codes){
			final Locale language = Locale.of(code);
			languages.computeIfAbsent(code, k -> new HashSet<>())
				.add(code);
			for(final Locale displayLocale : displayLocales)
				languages.computeIfAbsent(language.getDisplayLanguage(displayLocale).toLowerCase(Locale.ROOT), k -> new HashSet<>())
					.add(code);
		}
		return languages;
	}


	private DateLanguages(){}

}
