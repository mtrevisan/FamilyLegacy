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

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;


/**
 * A block of a report. The texts are Markdown inline text; a {@code null} text (or cell, or item) is taken as an empty one.
 */
public sealed interface ReportSection{

	/**
	 * @param level	The level of the heading, {@code 1} the highest (a smaller value is taken as {@code 1}).
	 * @param text	The text.
	 */
	record Heading(int level, String text) implements ReportSection{
		public Heading{
			level = Math.max(1, level);
			text = nullToEmpty(text);
		}
	}

	/** @param text	The text; if it has new lines it is preformatted (its alignment is kept, and it has no Markdown). */
	record Paragraph(String text) implements ReportSection{
		public Paragraph{
			text = nullToEmpty(text);
		}
	}

	record Spacer() implements ReportSection{
		/** The only one needed, since it carries nothing. */
		public static final Spacer INSTANCE = new Spacer();
	}

	record BulletList(List<String> items) implements ReportSection{
		public BulletList{
			items = allOrEmpty(items);
		}
	}

	/**
	 * @param headers	The headers; can be empty.
	 * @param rows	The rows; a row shorter than the widest one is completed with empty cells.
	 */
	record Table(List<String> headers, List<List<String>> rows) implements ReportSection{
		public Table{
			headers = allOrEmpty(headers);
			rows = rows.stream()
				.map(ReportSection::allOrEmpty)
				.toList();
		}

		/** @return	The number of columns: the one of the widest between the headers and the rows. */
		public int columns(){
			int columns = headers.size();
			for(final List<String> row : rows)
				columns = Math.max(columns, row.size());
			return columns;
		}
	}

	/**
	 * @param file	The image file.
	 * @param caption	The caption; empty if {@code null}.
	 */
	record Image(Path file, String caption) implements ReportSection{
		public Image{
			Objects.requireNonNull(file, "The image file is required");
			caption = nullToEmpty(caption);
		}
	}

	record PageBreak() implements ReportSection{
		/** The only one needed, since it carries nothing. */
		public static final PageBreak INSTANCE = new PageBreak();
	}

	record HorizontalRule() implements ReportSection{
		/** The only one needed, since it carries nothing. */
		public static final HorizontalRule INSTANCE = new HorizontalRule();
	}


	private static String nullToEmpty(final String text){
		return (text == null? "": text);
	}

	private static List<String> allOrEmpty(final List<String> texts){
		return texts.stream()
			.map(ReportSection::nullToEmpty)
			.toList();
	}

}
