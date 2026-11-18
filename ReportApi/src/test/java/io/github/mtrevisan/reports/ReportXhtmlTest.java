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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


class ReportXhtmlTest{

	private static String write(final ReportSection section){
		final StringBuilder sb = new StringBuilder();
		ReportXhtml.writeSection(sb, section, null, file -> null);
		return sb.toString();
	}


	@Test
	void tableWithoutHeadersKeepsItsRows(){
		final String xhtml = write(new ReportSection.Table(List.of(), List.of(List.of("a", "b"), List.of("c"))));

		Assertions.assertFalse(xhtml.contains("<thead>"));
		Assertions.assertEquals("<table><tbody><tr><td>a</td><td>b</td></tr><tr><td>c</td><td></td></tr></tbody></table>\n", xhtml);
	}

	@Test
	void tableWithRowsWiderThanHeadersKeepsAllCells(){
		final String xhtml = write(new ReportSection.Table(List.of("h"), List.of(List.of("a", "b"))));

		Assertions.assertEquals("<table><thead><tr><th>h</th><th></th></tr></thead><tbody><tr><td>a</td><td>b</td></tr></tbody></table>\n",
			xhtml);
	}

	@Test
	void emptyTableIsNothing(){
		Assertions.assertEquals("", write(new ReportSection.Table(List.of(), List.of())));
	}

	@Test
	void anchorIsWrittenOncePerPage(){
		final Set<String> written = new HashSet<>();
		final StringBuilder sb = new StringBuilder();
		ReportXhtml.writeSection(sb, new ReportSection.Heading(1, "A [I1]"), null, file -> null, written);
		ReportXhtml.writeSection(sb, new ReportSection.Heading(2, "B [I1]"), null, file -> null, written);

		final String xhtml = sb.toString();
		final int first = xhtml.indexOf("<a id=\"I1\">");
		Assertions.assertTrue(first >= 0);
		Assertions.assertEquals(-1, xhtml.indexOf("<a id=\"I1\">", first + 1));
	}

	@Test
	void linkWithoutTargetDoesNotFail(){
		Assertions.assertEquals("<p>Mario</p>\n", write(new ReportSection.Paragraph("[Mario](@I1@)")));
	}

	@Test
	void unreadableImageKeepsItsCaption(){
		Assertions.assertEquals("<p>Lost &amp; found</p>\n", write(new ReportSection.Image(Path.of("x.png"), "Lost & found")));
	}

}
