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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** The formats of the reports are found as providers, and each one writes a file. */
class ReportRenderersTest{

	@Test
	void theFormatsAreFoundInTheOrderOfTheDialog(){
		final List<String> ids = ReportRenderers.available().stream()
			.map(ReportRenderer::id)
			.toList();

		Assertions.assertEquals(List.of("text", "markdown", "html", "docx", "pdf", "epub", "site"), ids);
		Assertions.assertTrue(ids.contains(ReportRenderers.DEFAULT_ID));
	}

	@Test
	void theExtensionsAreDistinctAndAFileOneStartsWithADot(){
		final List<String> extensions = ReportRenderers.available().stream()
			.map(ReportRenderer::extension)
			.toList();

		Assertions.assertEquals(extensions.size(), extensions.stream().distinct().count());
		ReportRenderers.available().forEach(renderer -> Assertions.assertTrue("site".equals(renderer.id()) || renderer.extension().startsWith("."),
			renderer.id()));
	}

	@Test
	void everyFormatWritesANonEmptyFile(@TempDir final Path directory) throws IOException{
		final ReportDocument document = new ReportDocument("Title", "Subtitle", List.of(
			new ReportSection.Heading(1, "Heading"),
			new ReportSection.Paragraph("A paragraph."),
			new ReportSection.BulletList(List.of("one", "two")),
			new ReportSection.Table(List.of("a", "b"), List.of(List.of("1", "2")))));

		for(final ReportRenderer renderer : ReportRenderers.available()){
			final Path target = directory.resolve("report" + renderer.extension());

			renderer.render(document, target);

			//a format that is a directory has files in it
			final long size = (Files.isDirectory(target)? Files.list(target).count(): Files.size(target));
			Assertions.assertTrue(size > 0, renderer.id());
		}
	}

}
