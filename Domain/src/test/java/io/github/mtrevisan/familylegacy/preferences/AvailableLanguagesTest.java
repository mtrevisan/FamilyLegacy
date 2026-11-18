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
package io.github.mtrevisan.familylegacy.preferences;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;


class AvailableLanguagesTest{

	private static final List<String> BASE_NAMES = List.of("messages", "report_messages");
	private static final Map<Locale, String> LABELS = Map.of(Locale.ENGLISH, "English", Locale.ITALIAN, "Italiano", Locale.GERMAN, "Deutsch");
	private static final Function<Locale, String> NAMES = l -> LABELS.getOrDefault(l, l.toLanguageTag());


	@Test
	void theFileWithoutSuffixIsEnglish(){
		final Map<String, Locale> languages = AvailableLanguages.toLanguages(List.of("messages.properties"), BASE_NAMES, NAMES);

		Assertions.assertEquals(List.of(Locale.ENGLISH), List.copyOf(languages.values()));
	}

	@Test
	void languagesComeFromTheSuffixOfTheFiles(){
		final Map<String, Locale> languages = AvailableLanguages.toLanguages(
			List.of("report_messages.properties", "report_messages_it.properties", "messages_de.properties",
				"report_messages_zh_CN.properties", "readme.txt"),
			BASE_NAMES, NAMES);

		Assertions.assertEquals(List.of(Locale.ENGLISH, Locale.GERMAN, Locale.ITALIAN, Locale.of("zh", "CN")),
			List.copyOf(languages.values()));
		Assertions.assertEquals("Italiano", languages.entrySet().stream()
			.filter(e -> e.getValue().equals(Locale.ITALIAN))
			.findFirst().orElseThrow().getKey());
	}

	@Test
	void aLanguageInSeveralBundlesIsListedOnce(){
		final Map<String, Locale> languages = AvailableLanguages.toLanguages(
			List.of("messages_fr.properties", "report_messages_fr.properties"), BASE_NAMES, NAMES);

		Assertions.assertEquals(2, languages.size());
	}

	@Test
	void unrelatedFilesAreIgnored(){
		Assertions.assertNull(AvailableLanguages.toLocale("other_it.properties", BASE_NAMES));
		Assertions.assertNull(AvailableLanguages.toLocale("messages_it.txt", BASE_NAMES));
	}

	@Test
	void theBundlesOfTheApplicationAreFound(){
		final Map<String, Locale> languages = AvailableLanguages.find();

		Assertions.assertEquals(Locale.ENGLISH, languages.values().iterator().next());
		Assertions.assertTrue(languages.containsValue(Locale.ENGLISH));
		//the name comes from the language.name property of the messages
		Assertions.assertEquals("English", languages.keySet().iterator().next());
	}

}
