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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Consistency of the season names of {@code gregorian/seasons.csv}, which share a single lookup table with the month
 * names of all the locales (a word that is also a month must be documented by a {@code # collision (season wins): } or
 * {@code # collision (month wins): } comment line).
 */
class SeasonsResourceTest{

	private static final String[] SEASONS = {"autumn", "winter", "spring", "summer"};
	/** Comment line documenting a word that is both a season and a month, and which one wins. */
	private static final Pattern COLLISION = Pattern.compile("^# collision \\((?:season|month) wins\\): (\\S+) ");


	@Test
	void wordsAreLowercaseAndWrittenInASingleScript() throws IOException{
		final List<String> errors = new ArrayList<>();
		for(final String[] fields : readLines())
			for(int season = 1; season < fields.length; season ++)
				for(final String word : fields[season].split(",")){
					if(word.isBlank() || !word.equals(word.strip()))
						errors.add(fields[0] + ": blank or padded word '" + word + "'");
					if(!word.equals(word.toLowerCase(Locale.ROOT)))
						errors.add(fields[0] + ": uppercase word " + word);
					final Set<Character.UnicodeScript> scripts = new HashSet<>();
					word.codePoints()
						.mapToObj(Character.UnicodeScript::of)
						.filter(script -> script != Character.UnicodeScript.COMMON && script != Character.UnicodeScript.INHERITED)
						.forEach(scripts::add);
					if(scripts.size() > 1)
						errors.add(fields[0] + ": word " + word + " mixes the scripts " + scripts);
				}

		Assertions.assertEquals(List.of(), errors);
	}

	@Test
	void wordsBelongToASingleSeasonOfEachLanguage() throws IOException{
		// a word can be a different season in different languages (e.g. "yaz", spring in Azerbaijani and summer in
		// Turkish): without the language of the date, the parser rejects it
		final Map<String, String> seasonByLanguageAndWord = new HashMap<>();
		final List<String> errors = new ArrayList<>();
		for(final String[] fields : readLines())
			for(final String languageTag : fields[0].split("/")){
				final String language = languageTag.split("-", 2)[0];
				for(int season = 1; season < fields.length; season ++)
					for(final String word : fields[season].split(",")){
						final String previous = seasonByLanguageAndWord.putIfAbsent(language + ":" + word, SEASONS[season - 1]);
						if(previous != null && !previous.equals(SEASONS[season - 1]))
							errors.add(languageTag + ": " + word + " is both " + previous + " and " + SEASONS[season - 1]);
					}
			}

		Assertions.assertEquals(List.of(), errors);
	}

	@Test
	void wordsAreNotMonthNames() throws IOException{
		// a season word would hide the month with the same name in another language
		final Map<String, String> months = new HashMap<>();
		for(final Locale locale : Locale.getAvailableLocales())
			for(final Month month : Month.values())
				for(final TextStyle style : new TextStyle[]{TextStyle.FULL, TextStyle.SHORT})
					months.putIfAbsent(month.getDisplayName(style, locale).toLowerCase(Locale.ROOT),
						month + " (" + locale.toLanguageTag() + ")");

		final Set<String> documentedCollisions = readDocumentedCollisions();
		final List<String> errors = new ArrayList<>();
		for(final String[] fields : readLines())
			for(int season = 1; season < fields.length; season ++)
				for(final String word : fields[season].split(","))
					if(months.containsKey(word) && !documentedCollisions.contains(word))
						errors.add(fields[0] + ": " + word + " is also the month " + months.get(word));

		Assertions.assertEquals(List.of(), errors);
	}


	private static Set<String> readDocumentedCollisions() throws IOException{
		final Set<String> words = new HashSet<>();
		for(final String line : readRawLines())
			COLLISION.matcher(line)
				.results()
				.forEach(result -> words.add(result.group(1)));
		return words;
	}

	private static List<String[]> readLines() throws IOException{
		final List<String[]> lines = new ArrayList<>();
		for(final String line : readRawLines())
			if(!line.isBlank() && !line.startsWith("#")){
				final String[] fields = line.split(";", -1);
				Assertions.assertEquals(5, fields.length, line);
				lines.add(fields);
			}
		return lines;
	}

	private static List<String> readRawLines() throws IOException{
		try(final InputStream is = GregorianCalendarParser.class.getResourceAsStream("seasons.csv");
				final BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
			return reader.lines()
				.toList();
		}
	}

}
