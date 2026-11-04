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
package io.github.mtrevisan.familylegacy.ui.tools.reports;

import io.github.mtrevisan.familylegacy.reports.renderers.ReportRenderers;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.imageio.ImageIO;
import javax.xml.parsers.SAXParserFactory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;


/** The static website: a directory of pages that link to each other, with the images in a folder, in the language of the report. */
class SiteReportRendererTest{

	private static ReportRenderer site(){
		return ReportRenderers.available().stream()
			.filter(renderer -> "site".equals(renderer.id()))
			.findFirst()
			.orElseThrow();
	}

	private static String read(final Path file) throws IOException{
		return Files.readString(file);
	}


	@Test
	void theSiteIsADirectoryOfLinkedPagesWithTheImages(@TempDir final Path directory) throws Exception{
		final Path imageFile = directory.resolve("photo.png");
		ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", imageFile.toFile());
		final ReportDocument document = new ReportDocument("A <report> & more", "Subtitle", List.of(
			new ReportSection.Heading(1, "People"),
			new ReportSection.Paragraph("See [Anna Rossi](@I1@), **bold**; a ghost [Nobody](@I99@)."),
			new ReportSection.Heading(2, "Anna Rossi [I1]"),
			new ReportSection.Image(imageFile, "A \"photo\""),
			new ReportSection.Image(directory.resolve("missing.png"), "Lost"),
			new ReportSection.Heading(1, "Sources")), "it", "Indice");
		final Path target = directory.resolve("report_site");

		Assertions.assertEquals("_site", site().extension());
		site().render(document, target);

		for(final String name : List.of("index.html", "chapter-001.html", "chapter-002.html", "style.css", "images/image-1.png"))
			Assertions.assertTrue(Files.exists(target.resolve(name)), name);
		Assertions.assertFalse(Files.exists(target.resolve("chapter-003.html")));
		Assertions.assertFalse(Files.exists(target.resolve("images/image-2.png")), "the missing image is not copied");

		final String home = read(target.resolve("index.html"));
		Assertions.assertTrue(home.contains("<html lang=\"it\">") && !home.contains("dir=\"rtl\""), home);
		Assertions.assertTrue(home.contains("<h1>A &lt;report&gt; &amp; more</h1>"), home);
		Assertions.assertTrue(home.contains("<p class=\"subtitle\">Subtitle</p>"), home);
		Assertions.assertTrue(home.contains("<h2>Indice</h2>"), "the contents are in the language of the report: " + home);
		Assertions.assertTrue(home.contains("<li><a href=\"chapter-001.html\">People</a></li>"), home);
		Assertions.assertTrue(home.contains("<li><a href=\"chapter-002.html\">Sources</a></li>"), home);
		Assertions.assertTrue(home.contains("<a rel=\"next\" href=\"chapter-001.html\">People"), home);

		final String people = read(target.resolve("chapter-001.html"));
		Assertions.assertTrue(people.contains("<a href=\"chapter-001.html#I1\">Anna Rossi</a>"), people);
		Assertions.assertTrue(people.contains("<a id=\"I1\"></a>"), people);
		Assertions.assertTrue(people.contains("Nobody") && !people.contains("#I99"), people);
		Assertions.assertTrue(people.contains("<img src=\"images/image-1.png\" alt=\"A &quot;photo&quot;\"/>"), people);
		Assertions.assertTrue(people.contains("<p>Lost</p>"), people);
		Assertions.assertTrue(people.contains("<a rel=\"prev\" href=\"index.html\">") && people.contains("<a rel=\"next\" href=\"chapter-002.html\">"),
			people);
		Assertions.assertTrue(people.contains("aria-current=\"page\">People</a>"), "the page is marked in the navigation: " + people);

		//the pages are well formed (XHTML subset)
		for(final String page : List.of("index.html", "chapter-001.html", "chapter-002.html"))
			SAXParserFactory.newInstance().newSAXParser()
				.parse(new InputSource(new StringReader(read(target.resolve(page)).replace("<!DOCTYPE html>", ""))), new DefaultHandler());
	}

	@Test
	void renderingAgainRemovesTheOldPagesAndKeepsTheOtherFiles(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("again_site");
		final ReportDocument long_ = new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "One"),
			new ReportSection.Heading(1, "Two"), new ReportSection.Heading(1, "Three")));
		final ReportDocument short_ = new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "One")));

		site().render(long_, target);
		Files.writeString(target.resolve("mine.txt"), "keep");
		site().render(short_, target);

		try(final Stream<Path> files = Files.list(target)){
			Assertions.assertEquals(List.of("chapter-001.html", "index.html", "mine.txt", "style.css"),
				files.map(file -> file.getFileName().toString()).sorted().toList());
		}
	}

	@Test
	void aRightToLeftLanguageSetsTheDirectionOfTheSite(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("arabic_site");

		site().render(new ReportDocument("عنوان", "", List.of(new ReportSection.Heading(1, "فصل")), "ar", "المحتويات"), target);

		Assertions.assertTrue(read(target.resolve("index.html")).contains("<html lang=\"ar\" dir=\"rtl\">"));
		Assertions.assertTrue(read(target.resolve("chapter-001.html")).contains("dir=\"rtl\""));
	}

}
