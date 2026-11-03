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
package io.github.mtrevisan.familylegacy.ui.dialogs.help;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * The pages of the help, written in Markdown and bundled with the application (the help works offline).
 * <p>
 * The pages of a language are in {@code /help/<language>/}, the file {@code topics.txt} lists them in the order they are shown
 * (one file name per line, {@code #} starts a comment). When a language has no pages, or lacks one, the English one is used.
 */
public final class HelpTopics{

	/** The language every other one falls back to. */
	public static final String DEFAULT_LANGUAGE = "en";
	/** The page shown first. */
	public static final String INDEX = "index.md";
	/** The page that guides the first steps. */
	public static final String GETTING_STARTED = "getting-started.md";

	private static final String ROOT = "/help/";
	private static final String TOPICS_FILE = "topics.txt";


	/** A page of the help. */
	public record Topic(String file, String title){}


	private HelpTopics(){}


	/** @return	the pages, in the order of the index, for the language of the locale. */
	public static List<Topic> topics(final Locale locale){
		final String list = read(language(locale), TOPICS_FILE)
			.or(() -> read(DEFAULT_LANGUAGE, TOPICS_FILE))
			.orElse(StringUtils.EMPTY);

		final List<Topic> topics = new ArrayList<>();
		for(final String line : (Iterable<String>)list.lines()::iterator){
			final String file = line.trim();
			if(file.isEmpty() || file.startsWith("#"))
				continue;

			final Optional<String> markdown = markdown(file, locale);
			if(markdown.isPresent())
				topics.add(new Topic(file, StringUtils.defaultIfBlank(MarkdownToHtml.title(markdown.get()), file)));
		}
		return topics;
	}

	/** @return	the Markdown of a page, in the language of the locale or else in English; empty if the page does not exist. */
	public static Optional<String> markdown(final String file, final Locale locale){
		return read(language(locale), file)
			.or(() -> read(DEFAULT_LANGUAGE, file));
	}


	private static String language(final Locale locale){
		return (locale != null && !locale.getLanguage().isEmpty()? locale.getLanguage(): DEFAULT_LANGUAGE);
	}

	private static Optional<String> read(final String language, final String file){
		//a page is a plain file name: no way out of the folder of the language
		if(file.contains("/") || file.contains("\\") || file.contains(".."))
			return Optional.empty();

		try(final InputStream stream = HelpTopics.class.getResourceAsStream(ROOT + language + "/" + file)){
			return (stream != null? Optional.of(new String(stream.readAllBytes(), StandardCharsets.UTF_8)): Optional.empty());
		}
		catch(final IOException ignored){
			return Optional.empty();
		}
	}

}
