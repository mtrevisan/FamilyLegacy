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
package io.github.mtrevisan.familylegacy.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportDocument;
import io.github.mtrevisan.familylegacy.ui.tools.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Renders a {@link ReportDocument} as GitHub-Flavored Markdown.
 *
 * <p>Anchor generation: when a heading of level 2 or higher contains a
 * record reference such as {@code [I12]} or {@code [E3]}, an HTML anchor is
 * emitted on the line before the heading, so that internal links of the
 * form {@code [Giovanni Rossi](@I12@)} resolve correctly once converted to
 * {@code [Giovanni Rossi](#I12)}.</p>
 *
 * <p>Internal links: the protocol convention {@code [text](@id@)} is
 * converted to the standard Markdown anchor form {@code [text](#id)}.</p>
 *
 * <p>Image paths are rewritten relative to the report's output directory,
 * so the resulting document is portable as long as the referenced media
 * files are moved alongside it.</p>
 */
public final class MarkdownReportRenderer implements ReportRenderer{

	/** Matches a record reference such as {@code [I12]} inside heading text. */
	private static final Pattern RECORD_ID_PATTERN = Pattern.compile("\\[([A-Za-z]+\\d+)\\]");

	/** Matches the protocol's internal-link syntax {@code (@id@)}. */
	private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile("\\(@([A-Za-z]+\\d+)@\\)");


	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		try(BufferedWriter w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)){
			writeDocument(w, doc, target.getParent());
		}
	}


	/* ======================================================================
	 *                          Document
	 * ====================================================================== */

	private static void writeDocument(final BufferedWriter w, final ReportDocument doc,
		final Path baseDir) throws IOException{
		w.write("# ");
		w.write(linkify(doc.title()));
		w.newLine();
		w.newLine();

		if(doc.subtitle() != null && !doc.subtitle().isBlank()){
			w.write('*');
			w.write(doc.subtitle());
			w.write('*');
			w.newLine();
			w.newLine();
		}

		for(final ReportSection s : doc.sections())
			writeSection(w, s, baseDir);
	}


	private static void writeSection(final BufferedWriter w, final ReportSection s,
		final Path baseDir) throws IOException{
		if(s instanceof ReportSection.Heading h)
			writeHeading(w, h);
		else if(s instanceof ReportSection.Paragraph p)
			writeParagraph(w, p);
		else if(s instanceof ReportSection.Spacer)
			w.newLine();
		else if(s instanceof ReportSection.BulletList b)
			writeBulletList(w, b);
		else if(s instanceof ReportSection.Table t)
			writeTable(w, t);
		else if(s instanceof ReportSection.Image img)
			writeImage(w, img, baseDir);
		else if(s instanceof ReportSection.HorizontalRule)
			writeHorizontalRule(w);
		else if(s instanceof ReportSection.PageBreak)
			writePageBreak(w);
	}


	/* ======================================================================
	 *                          Headings
	 * ====================================================================== */

	private static void writeHeading(final BufferedWriter w, final ReportSection.Heading h)
		throws IOException{
		final int level = Math.max(1, h.level());

		// Anchor emission: for level >= 2 headings, extract any [Xxx] record
		// references from the heading text and emit one HTML anchor per ID on
		// a separate line before the heading itself.
		if(level >= 2){
			final List<String> ids = extractRecordIds(h.text());
			for(final String id : ids){
				w.write("<a id=\"");
				w.write(id);
				w.write("\"></a>");
			}
			if(!ids.isEmpty())
				w.newLine();
		}

		w.write("#".repeat(level));
		w.write(' ');
		w.write(linkify(h.text()));
		w.newLine();
		w.newLine();
	}


	/* ======================================================================
	 *                          Paragraphs / bullets
	 * ====================================================================== */

	private static void writeParagraph(final BufferedWriter w, final ReportSection.Paragraph p)
		throws IOException{
		w.write(linkify(p.text()));
		w.newLine();
		w.newLine();
	}


	private static void writeBulletList(final BufferedWriter w, final ReportSection.BulletList b)
		throws IOException{
		for(final String item : b.items()){
			w.write("- ");
			w.write(linkify(item));
			w.newLine();
		}
		w.newLine();
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private static void writeTable(final BufferedWriter w, final ReportSection.Table t)
		throws IOException{
		final int cols = t.headers().size();
		if(cols == 0)
			return;

		// Header row
		w.write('|');
		for(final String h : t.headers()){
			w.write(' ');
			w.write(escapeCell(h));
			w.write(" |");
		}
		w.newLine();

		// Separator row
		w.write('|');
		for(int i = 0; i < cols; i++)
			w.write(" --- |");
		w.newLine();

		// Body rows
		for(final List<String> row : t.rows()){
			w.write('|');
			for(int i = 0; i < cols; i++){
				final String cell = (i < row.size()? row.get(i): StringUtils.EMPTY);
				w.write(' ');
				w.write(escapeCell(cell));
				w.write(" |");
			}
			w.newLine();
		}
		w.newLine();
	}

	/**
	 * Escapes a table cell so it does not break the pipe-table syntax.
	 * Pipes are backslash-escaped; embedded line breaks become spaces; the
	 * protocol's internal-link markers are converted to Markdown anchors.
	 */
	private static String escapeCell(final String s){
		if(s == null)
			return StringUtils.EMPTY;
		return linkify(s)
			.replace("|", "\\|")
			.replace("\r", StringUtils.EMPTY)
			.replace(StringUtils.LF, StringUtils.SPACE);
	}


	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	private static void writeImage(final BufferedWriter w, final ReportSection.Image img,
		final Path baseDir) throws IOException{
		final Path file = img.file();
		String href = file.toString().replace('\\', '/');

		// Prefer a path relative to the output directory so the Markdown
		// stays portable.
		if(baseDir != null){
			try{
				href = baseDir.relativize(file.toAbsolutePath()).toString().replace('\\', '/');
			}
			catch(final IllegalArgumentException ignored){
				// Different filesystem roots: keep the absolute path.
			}
		}

		w.write("![");
		w.write(img.caption() == null? StringUtils.EMPTY: img.caption());
		w.write("](");
		w.write(href);
		w.write(")");
		w.newLine();
		w.newLine();
	}


	/* ======================================================================
	 *                          Rules / breaks
	 * ====================================================================== */

	private static void writeHorizontalRule(final BufferedWriter w) throws IOException{
		w.write("---");
		w.newLine();
		w.newLine();
	}

	private static void writePageBreak(final BufferedWriter w) throws IOException{
		w.newLine();
		w.write("---");
		w.newLine();
		w.newLine();
	}


	/* ======================================================================
	 *                          Link / anchor helpers
	 * ====================================================================== */

	/**
	 * Converts the protocol's internal-link syntax {@code [text](@id@)} into
	 * the standard Markdown form {@code [text](#id)}. Everything else is
	 * left untouched, so any legitimate {@code @} in the text is preserved.
	 */
	private static String linkify(final String text){
		if(text == null || !text.contains("@"))
			return text == null? StringUtils.EMPTY: text;
		final Matcher m = INTERNAL_LINK_PATTERN.matcher(text);
		final StringBuilder sb = new StringBuilder();
		while(m.find())
			m.appendReplacement(sb, Matcher.quoteReplacement("(#" + m.group(1) + ")"));
		m.appendTail(sb);
		return sb.toString();
	}

	/**
	 * Extracts every record reference of the form {@code [XxxN]} from the
	 * given text, preserving order and removing duplicates. The result is
	 * used to emit HTML anchors on section headings.
	 */
	private static List<String> extractRecordIds(final String text){
		final List<String> out = new ArrayList<>();
		if(text == null)
			return out;
		final Matcher m = RECORD_ID_PATTERN.matcher(text);
		while(m.find()){
			final String id = m.group(1);
			if(!out.contains(id))
				out.add(id);
		}
		return out;
	}

}
