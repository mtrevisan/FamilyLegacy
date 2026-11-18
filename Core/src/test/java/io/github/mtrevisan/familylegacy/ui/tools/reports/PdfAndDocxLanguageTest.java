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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;


/** The language of the report is told to the reader of the PDF and of the Word file. */
class PdfAndDocxLanguageTest{

	private static ReportRenderer renderer(final String id){
		return ReportRenderers.available().stream()
			.filter(renderer -> id.equals(renderer.id()))
			.findFirst()
			.orElseThrow();
	}

	private static ReportDocument document(final String language){
		return new ReportDocument("Title", "Subtitle", List.of(new ReportSection.Paragraph("text")), language, "Contents");
	}

	private static String styles(final Path file) throws IOException{
		try(final ZipFile zip = new ZipFile(file.toFile());
				final InputStream in = zip.getInputStream(zip.getEntry("word/styles.xml"))){
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}


	@Test
	void thePdfTellsTheLanguageAndTheTitle(@TempDir final Path directory) throws IOException{
		final Path file = directory.resolve("report.pdf");

		renderer("pdf").render(document("it"), file);

		final String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
		Assertions.assertTrue(content.contains("/Lang (it)") || content.contains("/Lang(it)"), "the language of the catalog");
		Assertions.assertTrue(content.contains("/Title (Title)") || content.contains("/Title(Title)"), "the title of the properties");
	}

	@Test
	void theWordFileTellsTheLanguage(@TempDir final Path directory) throws IOException{
		final Path file = directory.resolve("report.docx");

		renderer("docx").render(document("it"), file);

		final String styles = styles(file);
		Assertions.assertTrue(styles.contains("w:val=\"it\""), styles);
		Assertions.assertFalse(styles.contains("w:bidi=\""), styles);
		Assertions.assertFalse(styles.contains("w:eastAsia=\"it\""), styles);
	}

	@Test
	void theWordFileTellsTheLanguageOfRightToLeftAndEastAsianText(@TempDir final Path directory) throws IOException{
		final Path arabic = directory.resolve("arabic.docx");
		final Path chinese = directory.resolve("chinese.docx");

		renderer("docx").render(document("ar"), arabic);
		renderer("docx").render(document("zh-CN"), chinese);

		Assertions.assertTrue(styles(arabic).contains("w:bidi=\"ar\""), styles(arabic));
		Assertions.assertTrue(styles(chinese).contains("w:eastAsia=\"zh-CN\""), styles(chinese));
	}

}
