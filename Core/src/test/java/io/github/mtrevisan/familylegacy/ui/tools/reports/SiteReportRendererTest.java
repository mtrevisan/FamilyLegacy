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
import java.util.Arrays;
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

		for(final String name : List.of("index.html", "people.html", "sources.html", "style.css", "images/image-1.png"))
			Assertions.assertTrue(Files.exists(target.resolve(name)), name);
		Assertions.assertFalse(Files.exists(target.resolve("chapter-003.html")));
		Assertions.assertFalse(Files.exists(target.resolve("images/image-2.png")), "the missing image is not copied");

		final String home = read(target.resolve("index.html"));
		Assertions.assertTrue(home.contains("<html lang=\"it\">") && !home.contains("dir=\"rtl\""), home);
		Assertions.assertTrue(home.contains("<h1>A &lt;report&gt; &amp; more</h1>"), home);
		Assertions.assertTrue(home.contains("<p class=\"subtitle\">Subtitle</p>"), home);
		Assertions.assertTrue(home.contains("<h2>Indice</h2>"), "the contents are in the language of the report: " + home);
		Assertions.assertTrue(home.contains("<li><a href=\"people.html\">People</a></li>"), home);
		Assertions.assertTrue(home.contains("<li><a href=\"sources.html\">Sources</a></li>"), home);
		Assertions.assertTrue(home.contains("<a rel=\"next\" href=\"people.html\">People"), home);

		final String people = read(target.resolve("people.html"));
		Assertions.assertTrue(people.contains("<a href=\"people.html#I1\">Anna Rossi</a>"), people);
		Assertions.assertTrue(people.contains("<a id=\"I1\"></a>"), people);
		Assertions.assertTrue(people.contains("Nobody") && !people.contains("#I99"), people);
		Assertions.assertTrue(people.contains("<img src=\"images/image-1.png\" alt=\"A &quot;photo&quot;\"/>"), people);
		Assertions.assertTrue(people.contains("<p>Lost</p>"), people);
		Assertions.assertTrue(people.contains("<a rel=\"prev\" href=\"index.html\">") && people.contains("<a rel=\"next\" href=\"sources.html\">"),
			people);
		Assertions.assertTrue(people.contains("aria-current=\"page\">People</a>"), "the page is marked in the navigation: " + people);

		//the pages are well formed (XHTML subset)
		for(final String page : List.of("index.html", "people.html", "sources.html"))
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
			Assertions.assertEquals(List.of(".site-manifest", "index.html", "mine.txt", "one.html", "style.css"),
				files.map(file -> file.getFileName().toString()).sorted().toList());
		}
	}

	@Test
	void onlyTheFilesOfThePreviousRenderingAreRemoved(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("safe_site");
		Files.createDirectories(target.resolve("images"));
		Files.writeString(target.resolve("chapter-099.html"), "not ours");
		Files.writeString(target.resolve("images/image-9.png"), "not ours");

		site().render(new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "One"))), target);
		site().render(new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "One"))), target);

		Assertions.assertEquals("not ours", read(target.resolve("chapter-099.html")));
		Assertions.assertEquals("not ours", read(target.resolve("images/image-9.png")));
	}

	@Test
	void anImageOfAnOldRenderingDoesNotSurviveAndNoTemporaryFileIsLeft(@TempDir final Path directory) throws Exception{
		final Path imageFile = directory.resolve("photo.png");
		ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", imageFile.toFile());
		final Path target = directory.resolve("images_site");

		site().render(new ReportDocument("T", "", List.of(new ReportSection.Image(imageFile, "x"))), target);
		Assertions.assertTrue(Files.exists(target.resolve("images/image-1.png")));
		site().render(new ReportDocument("T", "", List.of(new ReportSection.Paragraph("no image"))), target);

		Assertions.assertFalse(Files.exists(target.resolve("images")), "the folder emptied by the new rendering is removed");
		try(final Stream<Path> files = Files.list(target)){
			Assertions.assertTrue(files.noneMatch(file -> file.getFileName().toString().startsWith(".render-")));
		}
	}

	@Test
	void aManifestThatPointsOutsideTheSiteIsNotTrusted(@TempDir final Path directory) throws Exception{
		final Path outside = directory.resolve("precious.txt");
		Files.writeString(outside, "keep");
		final Path target = directory.resolve("trap_site");
		Files.createDirectories(target);
		Files.writeString(target.resolve(".site-manifest"), "../precious.txt\n");

		site().render(new ReportDocument("T", "", List.of()), target);

		Assertions.assertEquals("keep", read(outside));
	}

	@Test
	void aDocumentWithoutHeadingsIsASinglePage(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("single_site");

		site().render(new ReportDocument("Only", "", List.of(new ReportSection.Paragraph("text"))), target);

		final String home = read(target.resolve("index.html"));
		Assertions.assertFalse(Files.exists(target.resolve("chapter-001.html")));
		Assertions.assertTrue(home.contains("<p>text</p>") && !home.contains("rel=\"next\"") && !home.contains("rel=\"prev\""), home);
	}

	@Test
	void thePagesAnnounceThePreviousAndTheNextOnes(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("links_site");

		site().render(new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "One"), new ReportSection.Heading(1, "Two"))), target);

		final String middle = read(target.resolve("one.html"));
		Assertions.assertTrue(middle.contains("<link rel=\"prev\" href=\"index.html\"/>")
			&& middle.contains("<link rel=\"next\" href=\"two.html\"/>"), middle);
		Assertions.assertFalse(read(target.resolve("index.html")).contains("<link rel=\"prev\""));
	}

	@Test
	void theArrowsOfThePagerFollowTheDirectionOfTheLanguage(@TempDir final Path directory) throws Exception{
		final ReportDocument latin = new ReportDocument("T", "", List.of(new ReportSection.Heading(1, "A"), new ReportSection.Heading(1, "B")));
		final ReportDocument arabic = new ReportDocument("ع", "", List.of(new ReportSection.Heading(1, "أ"), new ReportSection.Heading(1, "ب")),
			"ar", "المحتويات");

		site().render(latin, directory.resolve("ltr_site"));
		site().render(arabic, directory.resolve("rtl_site"));

		final String ltr = read(directory.resolve("ltr_site/a.html"));
		final String rtl = read(directory.resolve("rtl_site/chapter-001.html"));
		Assertions.assertTrue(ltr.contains("\">‹ ") && ltr.contains(" ›</a>"), ltr);
		Assertions.assertTrue(rtl.contains("\">› ") && rtl.contains(" ‹</a>"), rtl);
	}

	private static ReportDocument chapters(final String... titles){
		return new ReportDocument("T", "", Arrays.stream(titles).<ReportSection>map(title -> new ReportSection.Heading(1, title)).toList());
	}

	@Test
	void thePagesAreNamedAfterTheirTitleWithAFallbackForTheOnesThatCannot(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("names_site");

		site().render(chapters("Società & Più!", "Società & Più!", "Index", "Style", "عنوان", "   "), target);

		for(final String name : List.of("societa-piu.html", "societa-piu-2.html", "chapter-003.html", "chapter-004.html", "chapter-005.html",
				"chapter-006.html"))
			Assertions.assertTrue(Files.exists(target.resolve(name)), name);
		Assertions.assertTrue(read(target.resolve("index.html")).contains("href=\"societa-piu.html\""));
	}

	@Test
	void theAddressesDoNotChangeWhenAChapterIsAdded(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("stable_site");

		site().render(chapters("Alpha", "Beta"), target);
		site().render(chapters("Alpha", "New", "Beta"), target);

		Assertions.assertTrue(Files.exists(target.resolve("alpha.html")) && Files.exists(target.resolve("beta.html")));
	}

	@Test
	void aReportWithoutTitleIsNamedAfterItsFolder(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("Rossi family_site");

		site().render(new ReportDocument("", "", List.of(new ReportSection.Paragraph("x"))), target);

		final String home = read(target.resolve("index.html"));
		Assertions.assertTrue(home.contains("<title>Rossi family</title>") && home.contains("<h1>Rossi family</h1>"), home);
	}

	@Test
	void aLongSiteListsOnlyTheChaptersAroundThePageInTheNavigation(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("long_site");
		final String[] titles = new String[60];
		for(int i = 0; i < titles.length; i ++)
			titles[i] = "Chapter number " + (char)('a' + i / 26) + (char)('a' + i % 26);

		site().render(chapters(titles), target);

		final String page = read(target.resolve("chapter-number-ba.html"));	//the 27th chapter
		final String nav = page.substring(page.indexOf("<nav"));
		Assertions.assertTrue(nav.contains("href=\"index.html\"") && nav.contains("aria-current=\"page\">Chapter number ba<"), nav);
		Assertions.assertTrue(nav.contains("Chapter number aq<") && nav.contains("Chapter number bk<"), "the neighbours: " + nav);
		Assertions.assertFalse(nav.contains("Chapter number aa<") || nav.contains("Chapter number bz<"), "the far ones are left out: " + nav);
		Assertions.assertTrue(nav.contains("…"), nav);
		Assertions.assertTrue(read(target.resolve("index.html")).contains("Chapter number bz<"), "the home page lists all of them");
	}

	@Test
	void theContentComesBeforeTheNavigation(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("order_site");

		site().render(chapters("One"), target);

		final String page = read(target.resolve("one.html"));
		Assertions.assertTrue(page.indexOf("<main>") < page.indexOf("<nav"), page);
	}

	@Test
	void aSiteWrittenBeforeTheManifestIsReplacedWithoutLeavingTheOldPages(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("legacy_site");
		Files.createDirectories(target.resolve("images"));
		Files.writeString(target.resolve("index.html"), "<nav class=\"site-nav\"></nav><div class=\"pager\"></div>");
		Files.writeString(target.resolve("chapter-001.html"), "old");
		Files.writeString(target.resolve("images/image-1.png"), "old");
		Files.writeString(target.resolve("mine.txt"), "keep");

		site().render(chapters("One"), target);

		Assertions.assertFalse(Files.exists(target.resolve("chapter-001.html")));
		Assertions.assertFalse(Files.exists(target.resolve("images")));
		Assertions.assertEquals("keep", read(target.resolve("mine.txt")));
		Assertions.assertTrue(Files.exists(target.resolve("one.html")));
	}

	@Test
	void aFolderThatIsNotASiteIsLeftAlone(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("other_site");
		Files.createDirectories(target);
		Files.writeString(target.resolve("index.html"), "my own home page");
		Files.writeString(target.resolve("chapter-001.html"), "my own chapter");

		site().render(chapters("One"), target);

		Assertions.assertEquals("my own chapter", read(target.resolve("chapter-001.html")));
	}

	@Test
	void anSvgWithAScriptIsNotCopied(@TempDir final Path directory) throws Exception{
		final Path safe = directory.resolve("safe.svg");
		final Path unsafe = directory.resolve("unsafe.svg");
		Files.writeString(safe, "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"1\" height=\"1\"/></svg>");
		Files.writeString(unsafe, "<svg xmlns=\"http://www.w3.org/2000/svg\"><SCRIPT>alert(1)</SCRIPT></svg>");
		final Path target = directory.resolve("svg_site");

		site().render(new ReportDocument("T", "", List.of(new ReportSection.Image(safe, "ok"), new ReportSection.Image(unsafe, "bad"))), target);

		final String home = read(target.resolve("index.html"));
		Assertions.assertTrue(Files.exists(target.resolve("images/image-1.svg")) && !Files.exists(target.resolve("images/image-2.svg")));
		Assertions.assertTrue(home.contains("<img src=\"images/image-1.svg\"") && home.contains("<p>bad</p>"), home);
	}

	@Test
	void aRightToLeftLanguageSetsTheDirectionOfTheSite(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("arabic_site");

		site().render(new ReportDocument("عنوان", "", List.of(new ReportSection.Heading(1, "فصل")), "ar", "المحتويات"), target);

		Assertions.assertTrue(read(target.resolve("index.html")).contains("<html lang=\"ar\" dir=\"rtl\">"));
		Assertions.assertTrue(read(target.resolve("chapter-001.html")).contains("dir=\"rtl\""));
	}

}
