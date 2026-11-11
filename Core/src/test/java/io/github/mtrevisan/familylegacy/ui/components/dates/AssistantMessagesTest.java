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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.calendars.assistant.AgeEstimator;
import io.github.mtrevisan.calendars.assistant.CalendarConversionTool;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.LiturgicalTool;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.assistant.RomanDateTool;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/** The assistant tools produce only keys: every key they use must have a text, and nothing untranslated may reach the user. */
class AssistantMessagesTest{

	private static final Pattern KEY = Pattern.compile("\"(date_assistant\\.[a-z_.]+)\"");


	@Test
	void everyKeyUsedByTheToolsHasAText() throws IOException{
		final Path sources = Path.of("..", "Calendars", "src", "main", "java", "io", "github", "mtrevisan", "calendars", "assistant");
		Assertions.assertTrue(Files.isDirectory(sources), sources.toAbsolutePath().toString());

		final TreeSet<String> keys = new TreeSet<>();
		try(final Stream<Path> files = Files.list(sources)){
			for(final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()){
				final Matcher matcher = KEY.matcher(Files.readString(file));
				while(matcher.find())
					keys.add(matcher.group(1));
			}
		}
		Assertions.assertFalse(keys.isEmpty());

		final List<String> missing = new ArrayList<>();
		for(final String key : keys)
			if(I18N.t(key, null) == null)
				missing.add(key);
		Assertions.assertTrue(missing.isEmpty(), missing.toString());
	}

	@Test
	void suggestionsAreTranslated(){
		final List<DateSuggestion> all = new ArrayList<>();
		all.addAll(CalendarConversionTool.convert("gregorian", "12 January 1400"));
		all.addAll(LiturgicalTool.feasts(2024, CalendarReform.ROME));
		all.add(LiturgicalTool.relativeToEaster(2024, -3, CalendarReform.ROME));
		all.addAll(RelativeDateTool.offset("gregorian", "28 February 2024", 2, RelativeDateTool.Unit.WEEKS, true));
		all.addAll(AgeEstimator.birthFromAge("julian", "1500", 72, 3, null, true, "death"));
		all.addAll(RomanDateTool.interpret("ante diem VI Kal. Mart.", 1504));

		for(final DateSuggestion suggestion : all){
			final String explanation = AssistantMessages.explanation(suggestion);
			Assertions.assertFalse(explanation.contains("date_assistant."), explanation);
			Assertions.assertFalse(explanation.contains("{"), explanation);
		}
		Assertions.assertTrue(AssistantMessages.explanation(all.getFirst()).contains("Interpreted as:"));
	}

}
