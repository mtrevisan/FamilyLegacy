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
package io.github.mtrevisan.reports.pdf;

import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.SimpleBookmark;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


class PdfReportRendererTest{

	private static final Pattern SECOND_CHAPTER_ON_PAGE_2 = Pattern.compile("Second chapter\s*2\b");

	private final PdfReportRenderer renderer = new PdfReportRenderer();


	private Path render(final Path directory, final ReportDocument doc) throws IOException{
		final Path file = directory.resolve("report.pdf");
		renderer.render(doc, file);
		return file;
	}

	private static ReportDocument document(final ReportSection... sections){
		return new ReportDocument("Title", "Subtitle", List.of(sections));
	}

	private static String text(final Path file) throws IOException{
		final PdfReader reader = new PdfReader(Files.readAllBytes(file));
		try{
			final PdfTextExtractor extractor = new PdfTextExtractor(reader);
			final StringBuilder sb = new StringBuilder();
			for(int page = 1; page <= reader.getNumberOfPages(); page ++)
				sb.append(extractor.getTextFromPage(page)).append('\n');
			return sb.toString();
		}
		finally{
			reader.close();
		}
	}


	@Test
	void aRowWiderThanTheHeaderKeepsItsCells(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, document(new ReportSection.Table(List.of("A", "B"),
			List.of(List.of("one", "two", "extra")))));

		final String text = text(file);
		Assertions.assertTrue(text.contains("extra"), text);
		Assertions.assertTrue(text.contains("one"), text);
	}

	@Test
	void aTableWithoutHeadersIsWritten(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, document(new ReportSection.Table(List.of(), List.of(List.of("alpha", "beta")))));

		final String text = text(file);
		Assertions.assertTrue(text.contains("alpha") && text.contains("beta"), text);
	}

	@Test
	void theLongTableRepeatsItsHeader(@TempDir final Path directory) throws IOException{
		final List<List<String>> rows = new ArrayList<>();
		for(int i = 0; i < 120; i ++)
			rows.add(List.of("row" + i, "value"));

		final Path file = render(directory, document(new ReportSection.Table(List.of("HEADERCELL", "Other"), rows)));

		final String text = text(file);
		Assertions.assertTrue(StringUtils.countMatches(text, "HEADERCELL") >= 2, "the header is repeated on the next pages");
	}

	@Test
	void greekAndCyrillicTextIsWritten(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, document(new ReportSection.Paragraph("Ελληνικά Кириллица čšž")));

		final String text = text(file);
		Assertions.assertTrue(text.contains("Ελληνικά"), text);
		Assertions.assertTrue(text.contains("Кириллица"), text);
		Assertions.assertTrue(text.contains("čšž"), text);
	}

	@Test
	void anImageThatIsNotThereLeavesItsCaptionAndNotThePath(@TempDir final Path directory) throws IOException{
		final Path missing = directory.resolve("secret-folder").resolve("missing.png");

		final Path file = render(directory, document(new ReportSection.Image(missing, "A caption")));

		final String text = text(file);
		Assertions.assertTrue(text.contains("A caption"), text);
		Assertions.assertFalse(text.contains("secret-folder"), text);
		Assertions.assertFalse(new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1).contains("secret-folder"));
	}

	@Test
	void aLoneAsteriskIsNotAnItalic(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, document(new ReportSection.Paragraph("*1850 Padova, *1852 Roma and **bold** and `code`")));

		final String text = text(file);
		Assertions.assertTrue(text.contains("*1850 Padova, *1852 Roma"), text);
		Assertions.assertTrue(text.contains("bold") && text.contains("code"), text);
		Assertions.assertFalse(text.contains("**"), text);
	}

	@Test
	void theHeadingsAreBookmarksAndTheContentsHasTheirPages(@TempDir final Path directory) throws IOException{
		final List<ReportSection> sections = new ArrayList<>();
		sections.add(new ReportSection.Heading(1, "First chapter"));
		sections.add(new ReportSection.Heading(2, "A section [I1]"));
		sections.add(new ReportSection.Paragraph("see [the section](@I1@)"));
		sections.add(PageBreak());
		sections.add(new ReportSection.Heading(1, "Second chapter"));
		sections.add(new ReportSection.Paragraph("text"));

		final Path file = render(directory, new ReportDocument("Title", null, sections, "en", "Index of the report"));

		final String text = text(file);
		Assertions.assertTrue(text.contains("Index of the report"), text);
		final PdfReader reader = new PdfReader(Files.readAllBytes(file));
		try{
			final List<Map<String, Object>> bookmarks = SimpleBookmark.getBookmarkList(reader);
			Assertions.assertEquals(2, bookmarks.size(), String.valueOf(bookmarks));
			Assertions.assertEquals("First chapter", bookmarks.get(0).get("Title"));
			Assertions.assertEquals("Second chapter", bookmarks.get(1).get("Title"));
			Assertions.assertNotNull(bookmarks.get(0).get("Kids"), "the section is under its chapter");
		}
		finally{
			reader.close();
		}

		//the entry of the second chapter tells the page it is on (the third one: the title and the contents, the break)
		final String firstPage = pageText(file, 1);
		Assertions.assertTrue(firstPage.contains("Second chapter"), firstPage);
		Assertions.assertTrue(firstPage.matches("(?s).*Second chapter\\s*2\\b.*"), firstPage);
	}

	@Test
	void aDocumentWithOneHeadingHasNoContents(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, new ReportDocument("Title", null,
			List.of(new ReportSection.Heading(1, "Only one"), new ReportSection.Paragraph("text")), "en", "Index of the report"));

		Assertions.assertFalse(text(file).contains("Index of the report"));
	}

	@Test
	void aWideTreeIsShrunkToFitThePage(@TempDir final Path directory) throws IOException{
		final String line = "+-- " + "x".repeat(150);
		final Path file = render(directory, document(new ReportSection.Paragraph("root\n" + line + "\n    child")));

		//a line that is wrapped would be split in two lines of the text
		Assertions.assertTrue(text(file).contains(line), "the longest line stays in one line");
	}

	@Test
	void aSpacerAfterAPageBreakDoesNotMoveTheText(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, document(new ReportSection.Paragraph("before"), PageBreak(),
			new ReportSection.Spacer(), new ReportSection.Paragraph("after")));

		Assertions.assertTrue(pageText(file, 2).contains("after"));
	}

	@Test
	void theLanguageAndTheMetadataAreTold(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, new ReportDocument("The title", "The subtitle", List.of(new ReportSection.Paragraph("x")), "it",
			null));

		final String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
		Assertions.assertTrue(content.contains("/Lang (it)") || content.contains("/Lang(it)"));
		Assertions.assertTrue(content.contains("/Title (The title)") || content.contains("/Title(The title)"));
		Assertions.assertTrue(content.contains("Family Legacy"));
	}

	@Test
	void aRightToLeftDocumentIsWritten(@TempDir final Path directory) throws IOException{
		final Path file = render(directory, new ReportDocument("عنوان", null, List.of(
			new ReportSection.Heading(1, "فصل"),
			new ReportSection.Heading(1, "فصل ثان"),
			new ReportSection.Paragraph("نص عربي"),
			new ReportSection.BulletList(List.of("بند")),
			new ReportSection.Table(List.of("أ"), List.of(List.of("ب")))), "ar", null));

		final PdfReader reader = new PdfReader(Files.readAllBytes(file));
		try{
			Assertions.assertTrue(reader.getNumberOfPages() >= 1);
		}
		finally{
			reader.close();
		}
	}

	@Test
	void theSystemFontsCanBeChosen(@TempDir final Path directory) throws IOException{
		final List<String> fonts = renderer.fonts();
		Assertions.assertFalse(fonts.contains("Helvetica"), "the standard fonts are not a choice");
		Assumptions.assumeFalse(fonts.isEmpty(), "the system has no font");

		final Path file = directory.resolve("chosen.pdf");
		renderer.render(document(new ReportSection.Paragraph("Some text")), file, fonts.getFirst());
		Assertions.assertTrue(text(file).contains("Some text"));

		//a font that does not exist is the default one
		final Path other = directory.resolve("other.pdf");
		renderer.render(document(new ReportSection.Paragraph("Some text")), other, "No such font");
		Assertions.assertTrue(text(other).contains("Some text"));
	}


	private static ReportSection PageBreak(){
		return ReportSection.PageBreak.INSTANCE;
	}

	private static String pageText(final Path file, final int page) throws IOException{
		final PdfReader reader = new PdfReader(Files.readAllBytes(file));
		try{
			return new PdfTextExtractor(reader).getTextFromPage(page);
		}
		finally{
			reader.close();
		}
	}

}
