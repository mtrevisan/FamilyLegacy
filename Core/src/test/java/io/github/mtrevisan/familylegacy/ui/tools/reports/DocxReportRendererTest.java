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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


/** The structure of the Word file: styles, bullets, tables, images, inline Markdown, page breaks, right-to-left text. */
class DocxReportRendererTest{

	private static ReportRenderer renderer(){
		return ReportRenderers.available().stream()
			.filter(renderer -> "docx".equals(renderer.id()))
			.findFirst()
			.orElseThrow();
	}

	private static Path render(final Path directory, final String language, final ReportSection... sections) throws IOException{
		final Path file = directory.resolve("report.docx");
		renderer().render(new ReportDocument("Title", "Subtitle", List.of(sections), language, "Contents"), file);
		return file;
	}

	private static String entry(final Path file, final String name) throws IOException{
		try(final ZipFile zip = new ZipFile(file.toFile())){
			final ZipEntry entry = zip.getEntry(name);
			if(entry == null)
				return null;
			try(final InputStream in = zip.getInputStream(entry)){
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		}
	}

	private static boolean hasEntryStartingWith(final Path file, final String prefix) throws IOException{
		try(final ZipFile zip = new ZipFile(file.toFile())){
			return Collections.list(zip.entries()).stream()
				.anyMatch(entry -> entry.getName().startsWith(prefix));
		}
	}

	private static int count(final String text, final String token){
		int count = 0;
		int index = -1;
		while((index = text.indexOf(token, index + 1)) >= 0)
			count ++;
		return count;
	}


	@Test
	void headingsAndBulletsUseStylesThatAreDefined(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Heading(2, "Chapter"),
			new ReportSection.BulletList(List.of("one", "two")));

		final String styles = entry(file, "word/styles.xml");
		for(final String id : List.of("Title", "Heading1", "Heading2", "Heading3", "Heading4", "ListBullet"))
			Assertions.assertTrue(styles.contains("w:styleId=\"" + id + "\""), id + " in " + styles);
		Assertions.assertTrue(styles.contains("w:outlineLvl w:val=\"1\""), "the outline level of heading 2: " + styles);

		final String numbering = entry(file, "word/numbering.xml");
		Assertions.assertNotNull(numbering, "the numbering of the bullets");
		Assertions.assertTrue(numbering.contains("w:val=\"bullet\""), numbering);

		final String document = entry(file, "word/document.xml");
		Assertions.assertTrue(document.contains("w:val=\"Heading2\""), document);
		Assertions.assertEquals(2, count(document, "<w:numId "), document);
	}

	@Test
	void anUnclosedMarkerIsLiteralText(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Paragraph("price 5 * 3 and **bold**"));

		final String document = entry(file, "word/document.xml");
		Assertions.assertTrue(document.contains("price 5 * 3 and </w:t>"), document);
		Assertions.assertTrue(document.contains(">bold</w:t>"), document);
		//the asterisk did not turn the rest of the paragraph to italic
		final String body = document.substring(document.indexOf("Subtitle</w:t>"));
		Assertions.assertFalse(body.contains("<w:i w:val=\"on\"/>"), body);
	}

	@Test
	void aClosedMarkerStillMakesItalicAndCode(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Paragraph("a *b* `c`"));

		final String document = entry(file, "word/document.xml");
		Assertions.assertFalse(document.contains("*"), document);
		Assertions.assertFalse(document.contains("`"), document);
		Assertions.assertTrue(document.contains("Courier New"), document);
	}

	@Test
	void aTableWithoutHeadersKeepsItsRows(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Table(List.of(), List.of(List.of("alpha", "beta"), List.of("gamma"))));

		final String document = entry(file, "word/document.xml");
		for(final String text : List.of("alpha", "beta", "gamma"))
			Assertions.assertTrue(document.contains(">" + text + "</w:t>"), text + " in " + document);
		Assertions.assertFalse(document.contains("<w:tblHeader"), document);
	}

	@Test
	void theHeaderOfATableIsRepeatedAndRowsAreNotSplit(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Table(List.of("H1", "H2"), List.of(List.of("a", "b"), List.of("c"))));

		final String document = entry(file, "word/document.xml");
		Assertions.assertEquals(1, count(document, "<w:tblHeader"), document);
		Assertions.assertEquals(3, count(document, "<w:cantSplit"), document);
	}

	@Test
	void aPageBreakStartsTheNextParagraphWithoutAnEmptyOne(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Paragraph("before"), ReportSection.PageBreak.INSTANCE,
			new ReportSection.Paragraph("after"));

		final String document = entry(file, "word/document.xml");
		Assertions.assertEquals(1, count(document, "<w:pageBreakBefore"), document);
		//title, subtitle, before, after
		Assertions.assertEquals(4, count(document, "<w:p>") + count(document, "<w:p "), document);
	}

	@Test
	void anImageIsEmbeddedWithItsAlternativeTextAndNotEnlarged(@TempDir final Path directory) throws IOException{
		final Path image = directory.resolve("small.png");
		ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", image.toFile());

		final Path file = render(directory, "en", new ReportSection.Image(image, "A caption"));

		Assertions.assertTrue(hasEntryStartingWith(file, "word/media/"));
		final String document = entry(file, "word/document.xml");
		Assertions.assertTrue(document.contains("descr=\"A caption\""), document);
		//10 px = 7.5 pt, rounded to 8 pt = 101600 EMU, not stretched to the 400 pt box
		Assertions.assertTrue(document.contains("cx=\"101600\""), document);
	}

	@Test
	void anImageOfUnknownFormatIsNotWrittenAsJpeg(@TempDir final Path directory) throws IOException{
		final Path image = directory.resolve("picture.webp");
		Files.write(image, new byte[]{1, 2, 3});

		final Path file = render(directory, "en", new ReportSection.Image(image, ""));

		Assertions.assertFalse(hasEntryStartingWith(file, "word/media/"));
		final String document = entry(file, "word/document.xml");
		Assertions.assertTrue(document.contains("[image not available: picture.webp]"), document);
		Assertions.assertFalse(document.contains(directory.toString()), "the path is not shown");
	}

	@Test
	void aMissingImageIsAPlaceholder(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Image(directory.resolve("missing.png"), ""));

		Assertions.assertTrue(entry(file, "word/document.xml").contains("[image not available: missing.png]"));
	}

	@Test
	void rightToLeftTextIsMarkedInParagraphsRunsAndTables(@TempDir final Path directory) throws IOException{
		final ReportSection[] sections = {new ReportSection.Paragraph("text"), new ReportSection.Heading(1, "head"),
			new ReportSection.BulletList(List.of("item")), new ReportSection.Table(List.of("H"), List.of(List.of("cell")))};

		final String arabic = entry(render(directory, "ar", sections), "word/document.xml");
		Assertions.assertTrue(arabic.contains("<w:bidi/>"), arabic);
		Assertions.assertTrue(arabic.contains("<w:rtl/>"), arabic);
		Assertions.assertTrue(arabic.contains("<w:bidiVisual/>"), arabic);
		Assertions.assertTrue(arabic.contains("<w:szCs "), arabic);

		final String italian = entry(render(directory, "it", sections), "word/document.xml");
		Assertions.assertFalse(italian.contains("<w:bidi"), italian);
		Assertions.assertFalse(italian.contains("<w:rtl"), italian);
	}

	@Test
	void theTableOfContentsListsTheHeadingsWithItsTitle(@TempDir final Path directory) throws IOException{
		final Path file = directory.resolve("report.docx");
		renderer().render(new ReportDocument("Title", null, List.of(new ReportSection.Heading(1, "First *part*"),
			new ReportSection.Paragraph("text"), new ReportSection.Heading(2, "Second"), new ReportSection.Paragraph("more")), "it",
			"Indice"), file);

		final String document = entry(file, "word/document.xml");
		Assertions.assertTrue(document.contains(">Indice</w:t>"), document);
		Assertions.assertTrue(document.contains("w:fldCharType=\"begin\""), document);
		Assertions.assertTrue(document.contains("w:fldCharType=\"separate\""), document);
		Assertions.assertTrue(document.contains("w:fldCharType=\"end\""), document);
		Assertions.assertTrue(document.contains("TOC \\o \"1-4\" \\u"), document);
		Assertions.assertTrue(document.contains("w:val=\"TOC1\""), document);
		Assertions.assertTrue(document.contains("w:val=\"TOC2\""), document);
		//the entry is the heading without its Markdown: the heading itself has two runs "First " and "part", the entry one "First part"
		Assertions.assertTrue(document.contains(">First part</w:t>"), document);
		Assertions.assertTrue(entry(file, "word/styles.xml").contains("w:styleId=\"TOC2\""));
	}

	@Test
	void aReportWithOneHeadingHasNoTableOfContents(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Heading(1, "Only"), new ReportSection.Paragraph("text"));

		final String document = entry(file, "word/document.xml");
		Assertions.assertFalse(document.contains("fldCharType"), document);
		Assertions.assertFalse(document.contains("Contents"), document);
	}

	@Test
	void everyPageShowsItsNumber(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Paragraph("text"));

		final String footer = entry(file, "word/footer1.xml");
		Assertions.assertNotNull(footer, "the footer");
		Assertions.assertTrue(footer.contains("w:instr=\"PAGE\""), footer);
		Assertions.assertTrue(entry(file, "word/document.xml").contains("w:footerReference"));
	}

	@Test
	void thePageIsA4WithMargins(@TempDir final Path directory) throws IOException{
		final String document = entry(render(directory, "en", new ReportSection.Paragraph("text")), "word/document.xml");

		Assertions.assertTrue(document.contains("w:w=\"11906\""), document);
		Assertions.assertTrue(document.contains("w:h=\"16838\""), document);
		Assertions.assertTrue(document.contains("w:left=\"1418\""), document);
	}

	@Test
	void theColumnsOfATableAreProportionalToTheirText(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, "en", new ReportSection.Table(List.of("N", "Description"),
			List.of(List.of("1", "a rather long description of the row"))));

		final String document = entry(file, "word/document.xml");
		//weights 4 and 36 over 9070 twips
		Assertions.assertTrue(document.contains("w:w=\"907\""), document);
		Assertions.assertTrue(document.contains("w:w=\"8163\""), document);
	}

	@Test
	void theFileIsWrittenInOneGoOverAnExistingOne(@TempDir final Path directory) throws IOException{
		final Path target = directory.resolve("report.docx");
		Files.writeString(target, "old content that is longer than nothing");

		render(directory, "en", new ReportSection.Paragraph("new"), new ReportSection.Spacer(), new ReportSection.HorizontalRule());

		Assertions.assertTrue(entry(target, "word/document.xml").contains(">new</w:t>"));
		Assertions.assertTrue(entry(target, "docProps/core.xml").contains("Title"), "the title of the properties");
	}

}
