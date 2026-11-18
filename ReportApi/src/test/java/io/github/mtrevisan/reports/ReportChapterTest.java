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
package io.github.mtrevisan.reports;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;


class ReportChapterTest{

	@Test
	void splitAtFirstLevelHeadings(){
		final ReportDocument doc = new ReportDocument("The *book*", null, List.of(
			new ReportSection.Paragraph("intro"),
			new ReportSection.Heading(1, "Mario Rossi [I12]"),
			new ReportSection.Heading(2, "Life"),
			new ReportSection.Heading(1, "Places [P1]")));

		final List<ReportChapter> chapters = ReportChapter.split(doc);

		Assertions.assertEquals(3, chapters.size());
		Assertions.assertTrue(chapters.getFirst().titlePage());
		Assertions.assertEquals("The book", chapters.getFirst().title());
		Assertions.assertEquals("Mario Rossi", chapters.get(1).title());
		Assertions.assertEquals(2, chapters.get(1).sections().size());
		Assertions.assertEquals("Places", chapters.get(2).title());
	}

	@Test
	void anchorsPointToTheFirstChapter(){
		final ReportDocument doc = new ReportDocument("T", null, List.of(
			new ReportSection.Heading(1, "A [I1]"),
			new ReportSection.Heading(1, "B [I1] [I2]")));

		final Map<String, Integer> anchors = ReportChapter.anchors(ReportChapter.split(doc));

		Assertions.assertEquals(1, anchors.get("I1"));
		Assertions.assertEquals(2, anchors.get("I2"));
	}

	@Test
	void documentWithoutHeadingsHasOneChapter(){
		Assertions.assertEquals(1, ReportChapter.split(new ReportDocument("T", null, List.of())).size());
	}

}
