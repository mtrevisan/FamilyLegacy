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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

import java.nio.file.Path;
import java.util.List;


public sealed interface ReportSection{

	record Heading(int level, String text) implements ReportSection{
	}

	record Paragraph(String text) implements ReportSection{
	}

	record Spacer() implements ReportSection{
	}

	record BulletList(List<String> items) implements ReportSection{
		public BulletList{
			items = List.copyOf(items);
		}
	}

	record Table(List<String> headers, List<List<String>> rows) implements ReportSection{
		public Table{
			headers = List.copyOf(headers);
			rows = rows.stream().map(List::copyOf).toList();
		}
	}

	record Image(Path file, String caption) implements ReportSection{
	}

	record PageBreak() implements ReportSection{
	}

	record HorizontalRule() implements ReportSection{
	}

}
