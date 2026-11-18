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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * The words that join the pieces of a text of the assistant are messages (see {@link Sentences}), not characters of the code: the order of
 * the sentences and the separator of a list are what a language decides. This keeps the code from going back to joining them by hand.
 */
class LanguageNeutralTextTest{

	private static final Path SOURCES = Path.of("src", "main", "java", "io", "github", "mtrevisan", "familylegacy", "ui", "assistant");
	/** What joins the pieces of a text in the language of the code. */
	private static final List<String> FORBIDDEN = List.of("String.join(\", \"", "String.join(\"; \"", "String.join(\" · \"", "SPACE + I18N.",
		"\" (\" + I18N.");


	@Test
	void noTextOfTheAssistantIsJoinedByHand() throws IOException{
		final List<String> found = new ArrayList<>();
		try(final Stream<Path> files = Files.walk(SOURCES)){
			for(final Path file : files.filter(f -> f.toString().endsWith(".java")).toList()){
				//the helper is the one place that joins them
				if(file.getFileName().toString().equals("Sentences.java"))
					continue;

				final List<String> lines = Files.readAllLines(file);
				for(int i = 0; i < lines.size(); i ++)
					for(final String forbidden : FORBIDDEN)
						if(lines.get(i).contains(forbidden))
							found.add(SOURCES.relativize(file) + ":" + (i + 1) + " " + forbidden);
			}
		}

		Assertions.assertTrue(found.isEmpty(), "Use Sentences (list, items, join) or a message with the placeholders: " + found);
	}

}
