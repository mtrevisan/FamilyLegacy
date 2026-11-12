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
package io.github.mtrevisan.familylegacy.reports;

import io.github.mtrevisan.familylegacy.reports.renderers.MarkdownReportRenderer;
import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportSection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** The internal links of the text ({@code (@I12@)}) become Markdown anchors wherever they are in the text, not only at its start. */
class MarkdownInternalLinkTest{

	@Test
	void linkInTheMiddleOfTheText(@TempDir final Path directory) throws IOException{
		final Path target = directory.resolve("report.md");
		new MarkdownReportRenderer().render(new ReportDocument("Title", null,
			List.of(new ReportSection.Paragraph("Son of [Giovanni Rossi](@I12@) and others")), "en", "Contents"), target);

		final String text = Files.readString(target, StandardCharsets.UTF_8);
		Assertions.assertTrue(text.contains("Son of [Giovanni Rossi](#I12) and others"), text);
	}

}
