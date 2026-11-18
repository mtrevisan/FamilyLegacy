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
import java.util.Arrays;
import java.util.List;


class ReportSectionTest{

	@Test
	void nullTextsAreEmpty(){
		Assertions.assertEquals("", new ReportSection.Paragraph(null).text());
		Assertions.assertEquals("", new ReportSection.Heading(1, null).text());
		Assertions.assertEquals("", new ReportSection.Image(Path.of("a.png"), null).caption());
		Assertions.assertEquals(List.of("", "x"), new ReportSection.BulletList(Arrays.asList(null, "x")).items());
	}

	@Test
	void tableWithNullCells(){
		final ReportSection.Table table = new ReportSection.Table(Arrays.asList("a", null),
			List.of(Arrays.asList("1", null, "3")));

		Assertions.assertEquals(List.of("a", ""), table.headers());
		Assertions.assertEquals(List.of(List.of("1", "", "3")), table.rows());
		Assertions.assertEquals(3, table.columns());
	}

	@Test
	void headingLevelIsAtLeastOne(){
		Assertions.assertEquals(1, new ReportSection.Heading(0, "x").level());
		Assertions.assertEquals(3, new ReportSection.Heading(3, "x").level());
	}

	@Test
	void imageNeedsAFile(){
		Assertions.assertThrows(NullPointerException.class, () -> new ReportSection.Image(null, "x"));
	}

	@Test
	void singletonsAreEqualToNewInstances(){
		Assertions.assertEquals(new ReportSection.PageBreak(), ReportSection.PageBreak.INSTANCE);
		Assertions.assertEquals(new ReportSection.Spacer(), ReportSection.Spacer.INSTANCE);
		Assertions.assertEquals(new ReportSection.HorizontalRule(), ReportSection.HorizontalRule.INSTANCE);
	}

}
