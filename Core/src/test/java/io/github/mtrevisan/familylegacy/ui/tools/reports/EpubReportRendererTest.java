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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/** The EPUB book: a ZIP that starts with the stored mimetype, made of well-formed XHTML, whose links between chapters resolve. */
class EpubReportRendererTest{

	private static ReportRenderer epub(){
		return ReportRenderers.available().stream()
			.filter(renderer -> "epub".equals(renderer.id()))
			.findFirst()
			.orElseThrow();
	}

	private static String read(final ZipFile zip, final String name) throws IOException{
		final ZipEntry entry = zip.getEntry(name);
		Assertions.assertNotNull(entry, name);
		try(final InputStream in = zip.getInputStream(entry)){
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void assertWellFormed(final String xml, final String name) throws Exception{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try{
			factory.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		}
		catch(final Exception e){
			throw new AssertionError(name + " is not well formed: " + e.getMessage() + "\n" + xml, e);
		}
	}


	@Test
	void theBookIsAnEpubWithChaptersLinksAndImages(@TempDir final Path directory) throws Exception{
		final Path imageFile = directory.resolve("photo.png");
		ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", imageFile.toFile());

		final ReportDocument document = new ReportDocument("A <report> & more", "Subtitle", List.of(
			new ReportSection.Heading(1, "People"),
			new ReportSection.Paragraph("See [Anna Rossi](@I1@), **bold**, *italic*, `code`; a ghost [Nobody](@I99@)."),
			new ReportSection.Heading(2, "Anna Rossi [I1]"),
			new ReportSection.BulletList(List.of("one", "tw\u0001o & three")),
			new ReportSection.Table(List.of("a", "b"), List.of(List.of("1"))),
			new ReportSection.Image(imageFile, "A \"photo\""),
			new ReportSection.Image(directory.resolve("missing.png"), "Lost"),
			new ReportSection.Paragraph("line one\n  line two"),
			new ReportSection.Spacer(),
			new ReportSection.HorizontalRule(),
			new ReportSection.PageBreak(),
			new ReportSection.Heading(1, "Sources")), "it", "Indice");
		final Path target = directory.resolve("book.epub");

		epub().render(document, target);

		//the first entry is the stored mimetype
		try(final ZipFile zip = new ZipFile(target.toFile())){
			final ZipEntry first = zip.entries().nextElement();
			Assertions.assertEquals("mimetype", first.getName());
			Assertions.assertEquals(ZipEntry.STORED, first.getMethod());
			Assertions.assertEquals("application/epub+zip", read(zip, "mimetype"));

			final List<String> names = new ArrayList<>();
			zip.stream().forEach(entry -> names.add(entry.getName()));
			//title page, People, Sources
			for(final String name : List.of("META-INF/container.xml", "OEBPS/content.opf", "OEBPS/nav.xhtml", "OEBPS/style.css",
					"OEBPS/chapter-001.xhtml", "OEBPS/chapter-002.xhtml", "OEBPS/chapter-003.xhtml", "OEBPS/images/image-1.png"))
				Assertions.assertTrue(names.contains(name), name + " in " + names);
			Assertions.assertFalse(names.contains("OEBPS/chapter-004.xhtml"), names.toString());
			Assertions.assertFalse(names.contains("OEBPS/images/image-2.png"), "the missing image is not in the book");

			for(final String name : names)
				if(name.endsWith(".xhtml") || name.endsWith(".opf") || name.endsWith(".xml"))
					assertWellFormed(read(zip, name), name);

			//the link to a record that has a heading goes to its chapter, the one to a record that has not is only text
			final String people = read(zip, "OEBPS/chapter-002.xhtml");
			Assertions.assertTrue(people.contains("<a href=\"chapter-002.xhtml#I1\">Anna Rossi</a>"), people);
			Assertions.assertTrue(people.contains("<a id=\"I1\"></a>"), people);
			Assertions.assertTrue(people.contains("Nobody") && !people.contains("#I99"), people);
			Assertions.assertTrue(people.contains("<b>bold</b>") && people.contains("<i>italic</i>") && people.contains("<code>code</code>"),
				people);
			Assertions.assertTrue(people.contains("tw" + "o &amp; three"), "the control character is dropped, the ampersand escaped: " + people);
			Assertions.assertTrue(people.contains("<img src=\"images/image-1.png\" alt=\"A &quot;photo&quot;\"/>"), people);
			Assertions.assertTrue(people.contains("<p>Lost</p>"), people);
			Assertions.assertTrue(people.contains("<pre>line one\n  line two</pre>"), people);

			final String titlePage = read(zip, "OEBPS/chapter-001.xhtml");
			Assertions.assertTrue(titlePage.contains("<h1>A &lt;report&gt; &amp; more</h1>"), titlePage);

			final String contents = read(zip, "OEBPS/nav.xhtml");
			Assertions.assertTrue(contents.contains("chapter-003.xhtml\">Sources</a>"), contents);
			Assertions.assertTrue(contents.contains("<h1>Indice</h1>"), "the title of the contents is in the language of the report: " + contents);
			Assertions.assertTrue(contents.contains("lang=\"it\"") && !contents.contains("dir=\"rtl\""), contents);

			final String opf = read(zip, "OEBPS/content.opf");
			Assertions.assertTrue(opf.contains("properties=\"nav\"") && opf.contains("dcterms:modified"), opf);
			Assertions.assertTrue(opf.contains("<itemref idref=\"chapter-3\"/>"), opf);
			Assertions.assertTrue(opf.contains("image/png"), opf);
			Assertions.assertTrue(opf.contains("<dc:language>it</dc:language>"), opf);
			Assertions.assertTrue(Files.size(target) > 0);
		}
	}

	@Test
	void aDocumentWithoutHeadingsIsASingleChapter(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("plain.epub");

		epub().render(new ReportDocument("Only", "", List.of(new ReportSection.Paragraph("text"))), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertNotNull(zip.getEntry("OEBPS/chapter-001.xhtml"));
			Assertions.assertNull(zip.getEntry("OEBPS/chapter-002.xhtml"));
			assertWellFormed(read(zip, "OEBPS/chapter-001.xhtml"), "chapter");
		}
	}

	@Test
	void aReportWithoutLanguageIsInEnglish(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("english.epub");

		epub().render(new ReportDocument("Only", "", List.of(new ReportSection.Heading(1, "One"))), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertTrue(read(zip, "OEBPS/content.opf").contains("<dc:language>en</dc:language>"));
			Assertions.assertTrue(read(zip, "OEBPS/nav.xhtml").contains("<h1>Contents</h1>"));
		}
	}

	private static String identifier(final String opf){
		final int start = opf.indexOf("<dc:identifier id=\"book-id\">") + "<dc:identifier id=\"book-id\">".length();
		return opf.substring(start, opf.indexOf("</dc:identifier>", start));
	}

	@Test
	void webpAndUnreadableImagesAreNotInTheBookButTheirCaptionStays(@TempDir final Path directory) throws Exception{
		final Path webp = directory.resolve("photo.webp");
		Files.write(webp, new byte[]{1, 2, 3});
		//a directory with the name of an image cannot be read
		final Path unreadable = Files.createDirectory(directory.resolve("broken.png"));
		final Path target = directory.resolve("book.epub");

		epub().render(new ReportDocument("T", "", List.of(
			new ReportSection.Image(webp, "Web picture"),
			new ReportSection.Image(unreadable, "Broken"),
			new ReportSection.Image(unreadable, "Broken again"))), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertTrue(zip.stream().noneMatch(entry -> entry.getName().startsWith("OEBPS/images/")));
			final String page = read(zip, "OEBPS/chapter-001.xhtml");
			Assertions.assertTrue(page.contains("<p>Web picture</p>") && page.contains("<p>Broken</p>")
				&& page.contains("<p>Broken again</p>") && !page.contains("<img"), page);
			Assertions.assertFalse(read(zip, "OEBPS/content.opf").contains("image/webp"));
		}
	}

	@Test
	void sameImageContentIsInTheBookOnceAndWithoutCaptionTheFileNameIsTheAlternativeText(@TempDir final Path directory) throws Exception{
		final BufferedImage picture = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
		final Path first = directory.resolve("first.png");
		final Path copy = directory.resolve("copy.png");
		ImageIO.write(picture, "png", first.toFile());
		Files.copy(first, copy);
		final Path target = directory.resolve("book.epub");

		epub().render(new ReportDocument("T", "", List.of(
			new ReportSection.Image(first, null),
			new ReportSection.Image(copy, "Same"),
			new ReportSection.Image(first, "Again"))), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertNotNull(zip.getEntry("OEBPS/images/image-1.png"));
			Assertions.assertNull(zip.getEntry("OEBPS/images/image-2.png"));
			final String page = read(zip, "OEBPS/chapter-001.xhtml");
			Assertions.assertTrue(page.contains("alt=\"first.png\""), page);
			Assertions.assertEquals(3, StringUtils.countMatches(page, "src=\"images/image-1.png\""), page);
			Assertions.assertEquals(1, StringUtils.countMatches(read(zip, "OEBPS/content.opf"), "<item id=\"image-"));
		}
	}

	@Test
	void theSubtitleIsOnTheTitlePageAndTheContentsNestTheSections(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("book.epub");

		epub().render(new ReportDocument("Title", "A *sub* title", List.of(
			new ReportSection.Heading(1, "People"),
			new ReportSection.Heading(2, "Anna [I1]"),
			new ReportSection.Heading(2, "Anna [I2]"),
			new ReportSection.Heading(3, "Too deep"),
			new ReportSection.Heading(1, "Sources")), "en", "Contents"), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			final String titlePage = read(zip, "OEBPS/chapter-001.xhtml");
			Assertions.assertTrue(titlePage.contains("<p class=\"subtitle\">A sub title</p>"), titlePage);

			final String people = read(zip, "OEBPS/chapter-002.xhtml");
			Assertions.assertTrue(people.contains("<h3 id=\"section-2-1\">") && people.contains("<h3 id=\"section-2-2\">"), people);
			Assertions.assertFalse(people.contains("id=\"section-2-3\""), "only the second level is listed: " + people);

			final String nav = read(zip, "OEBPS/nav.xhtml");
			assertWellFormed(nav, "nav");
			Assertions.assertTrue(nav.contains("<a href=\"chapter-002.xhtml\">People</a><ol>"
				+ "<li><a href=\"chapter-002.xhtml#section-2-1\">Anna</a></li>"
				+ "<li><a href=\"chapter-002.xhtml#section-2-2\">Anna</a></li></ol></li>"), nav);
			Assertions.assertTrue(nav.contains("<a href=\"chapter-003.xhtml\">Sources</a></li>"), nav);
			//landmarks: the contents and the first chapter after the title page
			Assertions.assertTrue(nav.contains("epub:type=\"landmarks\"") && nav.contains("href=\"nav.xhtml#toc\"")
				&& nav.contains("<a epub:type=\"bodymatter\" href=\"chapter-002.xhtml\">People</a>"), nav);
		}
	}

	@Test
	void theMetadataHasAStableIdentifierAPublisherAndADate(@TempDir final Path directory) throws Exception{
		final ReportDocument document = new ReportDocument("Same", "Sub", List.of(new ReportSection.Heading(1, "One")));
		final ReportDocument other = new ReportDocument("Other", "Sub", List.of(new ReportSection.Heading(1, "One")));
		final Path first = directory.resolve("a.epub");
		final Path second = directory.resolve("b.epub");
		final Path third = directory.resolve("c.epub");

		epub().render(document, first);
		epub().render(document, second);
		epub().render(other, third);

		try(final ZipFile a = new ZipFile(first.toFile()); final ZipFile b = new ZipFile(second.toFile());
				final ZipFile c = new ZipFile(third.toFile())){
			final String opf = read(a, "OEBPS/content.opf");
			Assertions.assertTrue(identifier(opf).startsWith("urn:uuid:"), opf);
			Assertions.assertEquals(identifier(opf), identifier(read(b, "OEBPS/content.opf")), "the same report is the same book");
			Assertions.assertNotEquals(identifier(opf), identifier(read(c, "OEBPS/content.opf")));
			Assertions.assertTrue(opf.contains("<dc:publisher>FamilyLegacy</dc:publisher>"), opf);
			Assertions.assertTrue(opf.contains("<dc:date>20"), opf);
			assertWellFormed(opf, "opf");
		}
	}

	@Test
	void anExistingBookIsReplacedAndNoTemporaryFileIsLeft(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("book.epub");
		Files.writeString(target, "old content");

		epub().render(new ReportDocument("T", "", List.of(new ReportSection.Paragraph("text"))), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertEquals("application/epub+zip", read(zip, "mimetype"));
		}
		try(final var files = Files.list(directory)){
			Assertions.assertEquals(List.of("book.epub"), files.map(file -> file.getFileName().toString()).toList());
		}
	}

	@Test
	void aRightToLeftLanguageSetsTheDirectionOfTheBook(@TempDir final Path directory) throws Exception{
		final Path target = directory.resolve("arabic.epub");

		epub().render(new ReportDocument("عنوان", "", List.of(new ReportSection.Heading(1, "فصل")), "ar", "المحتويات"), target);

		try(final ZipFile zip = new ZipFile(target.toFile())){
			Assertions.assertTrue(read(zip, "OEBPS/chapter-002.xhtml").contains("lang=\"ar\" xml:lang=\"ar\" dir=\"rtl\""));
			Assertions.assertTrue(read(zip, "OEBPS/content.opf").contains("page-progression-direction=\"rtl\""));
			Assertions.assertTrue(read(zip, "OEBPS/nav.xhtml").contains("<h1>المحتويات</h1>"));
			assertWellFormed(read(zip, "OEBPS/nav.xhtml"), "nav");
		}
	}

}
