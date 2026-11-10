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
package io.github.mtrevisan.familylegacy.ui.assistant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The findings of the same kind are listed together under a heading with the name of the kind: every kind of finding has one, so that a
 * new check cannot reach the palette with a heading made of its identifier.
 */
class AssistantKindLabelsTest{

	private static final String KIND_PREFIX = "assistant.kind.";
	/** The findings: their title is {@code <kind>.title} (the drafts and the comparisons are not findings). */
	private static final Pattern FINDING_TITLE = Pattern.compile(
		"^((?:chronology|conclusion|consanguinity|context|gap|identity|place|planner|relationship)\\.[a-z_.]+)\\.title$");
	/** The kinds that are titles of the same finding as another one (the titles of the kinds of record of the identity findings). */
	private static final Set<String> NOT_KINDS = Set.of("identity.possible_duplicate_kind");


	private static Properties messages(){
		final Properties messages = new Properties();
		//the file is in the Domain module, whose resources the class loader of this module does not see
		final Path path = Path.of("..", "Domain", "src", "main", "resources", "i18n", "ui", "messages.properties");
		try(final Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)){
			messages.load(reader);
		}
		catch(final IOException ioe){
			throw new IllegalStateException(ioe);
		}
		return messages;
	}


	@Test
	void everyKindOfFindingHasALabel(){
		final Properties messages = messages();
		final Set<String> missing = new TreeSet<>();
		for(final String key : messages.stringPropertyNames()){
			final Matcher matcher = FINDING_TITLE.matcher(key);
			if(matcher.matches()){
				final String kind = matcher.group(1);
				if(!kind.contains(".draft") && !kind.contains(".compare") && !NOT_KINDS.contains(kind)
						&& !messages.containsKey(KIND_PREFIX + kind))
					missing.add(kind);
			}
		}

		Assertions.assertTrue(missing.isEmpty(), "Add assistant.kind.<kind> to the messages for: " + missing);
	}

	@Test
	void everyLabelIsOfAKindThatExists(){
		final Properties messages = messages();
		final Set<String> unknown = new TreeSet<>();
		for(final String key : messages.stringPropertyNames())
			if(key.startsWith(KIND_PREFIX) && !messages.containsKey(key.substring(KIND_PREFIX.length()) + ".title"))
				unknown.add(key);

		Assertions.assertTrue(unknown.isEmpty(), "These labels are of no finding: " + unknown);
	}

}
