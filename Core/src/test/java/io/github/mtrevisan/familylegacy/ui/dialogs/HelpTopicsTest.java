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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpTopics;
import io.github.mtrevisan.familylegacy.ui.dialogs.help.MarkdownToHtml;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;


/**
 * The help is documentation: it must not point to a page or to a heading that does not exist, and no page must be left out.
 */
class HelpTopicsTest{

	private static final Locale ENGLISH = Locale.ENGLISH;


	@Test
	void theIndexAndTheFirstStepsAreTopics(){
		final List<String> files = topics().stream().map(HelpTopics.Topic::file).toList();

		Assertions.assertEquals(HelpTopics.INDEX, files.getFirst());
		Assertions.assertTrue(files.contains(HelpTopics.GETTING_STARTED), files.toString());
	}

	@Test
	void everyFileOfTheListIsAPage(){
		final String list = HelpTopics.markdown("topics.txt", ENGLISH).orElseThrow();
		final long listed = list.lines().map(String::trim).filter(line -> !line.isEmpty() && !line.startsWith("#")).count();

		Assertions.assertEquals(listed, topics().size(), "a file of topics.txt does not exist");
	}

	@Test
	void everyPageIsListed() throws IOException, URISyntaxException{
		final URL folder = HelpTopics.class.getResource("/help/en/");
		Assumptions.assumeTrue(folder != null && "file".equals(folder.getProtocol()), "The pages are not in a folder");

		final Set<String> listed = new HashSet<>();
		for(final HelpTopics.Topic topic : topics())
			listed.add(topic.file());
		try(final Stream<Path> files = Files.list(Path.of(folder.toURI()))){
			for(final Path file : files.toList()){
				final String name = file.getFileName().toString();
				if(name.endsWith(".md"))
					Assertions.assertTrue(listed.contains(name), name + " is not in topics.txt");
			}
		}
	}

	@Test
	void everyPageHasATitleAndUniqueHeadings(){
		for(final HelpTopics.Topic topic : topics()){
			final String markdown = markdown(topic.file());

			Assertions.assertNotNull(MarkdownToHtml.title(markdown), topic.file() + " has no title");
			final List<String> anchors = MarkdownToHtml.anchors(markdown);
			Assertions.assertEquals(anchors.size(), new HashSet<>(anchors).size(), topic.file() + " has two headings with the same anchor");
		}
	}

	@Test
	void everyLinkLeadsToAnExistingPageAndHeading(){
		final Set<String> files = new HashSet<>();
		for(final HelpTopics.Topic topic : topics())
			files.add(topic.file());

		for(final HelpTopics.Topic topic : topics()){
			for(final String link : MarkdownToHtml.links(markdown(topic.file()))){
				if(link.startsWith("http://") || link.startsWith("https://") || link.startsWith("mailto:"))
					continue;

				final int hash = link.indexOf('#');
				final String file = (hash >= 0? link.substring(0, hash): link);
				final String anchor = (hash >= 0? link.substring(hash + 1): null);
				final String target = (file.isEmpty()? topic.file(): file);

				Assertions.assertTrue(files.contains(target), topic.file() + " links to the missing page " + link);
				if(anchor != null)
					Assertions.assertTrue(MarkdownToHtml.anchors(markdown(target)).contains(anchor),
						topic.file() + " links to the missing heading " + link);
			}
		}
	}

	@Test
	void everyPageCanBeReachedFromTheIndex(){
		final List<String> fromIndex = MarkdownToHtml.links(markdown(HelpTopics.INDEX));

		for(final HelpTopics.Topic topic : topics())
			if(!topic.file().equals(HelpTopics.INDEX))
				Assertions.assertTrue(fromIndex.stream().anyMatch(link -> link.equals(topic.file())),
					topic.file() + " is not linked from the index");
	}

	@Test
	void aLanguageWithoutPagesFallsBackToEnglish(){
		Assertions.assertEquals(topics(), HelpTopics.topics(Locale.forLanguageTag("zz")));
		Assertions.assertEquals(markdown(HelpTopics.INDEX), HelpTopics.markdown(HelpTopics.INDEX, Locale.forLanguageTag("zz")).orElseThrow());
	}

	@Test
	void aPageCannotLeaveItsFolder(){
		Assertions.assertTrue(HelpTopics.markdown("../en/index.md", ENGLISH).isEmpty());
		Assertions.assertTrue(HelpTopics.markdown("sub/index.md", ENGLISH).isEmpty());
		Assertions.assertTrue(HelpTopics.markdown("missing.md", ENGLISH).isEmpty());
	}


	private static List<HelpTopics.Topic> topics(){
		return HelpTopics.topics(ENGLISH);
	}

	private static String markdown(final String file){
		return HelpTopics.markdown(file, ENGLISH).orElseThrow(() -> new AssertionError(file + " does not exist"));
	}

}
