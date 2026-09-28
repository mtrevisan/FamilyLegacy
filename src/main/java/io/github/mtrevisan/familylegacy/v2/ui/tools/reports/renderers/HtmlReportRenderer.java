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
package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportDocument;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportSection;
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
 * Renders a {@link ReportDocument} as a self-contained HTML5 file.
 *
 * <p>Anchor generation: when a heading of level 2 or higher contains a
 * record reference such as {@code [I12]} or {@code [E3]}, an HTML anchor
 * {@code <a id="I12"></a>} is emitted before the heading, so that internal
 * links of the form {@code [Giovanni Rossi](@I12@)} resolve once converted
 * to {@code <a href="#I12">Giovanni Rossi</a>}.</p>
 *
 * <p>Internal links: the protocol convention {@code [text](@id@)} is
 * converted to a standard HTML anchor. Inline Markdown
 * ({@code **bold**}, {@code *italic*}, {@code `code`}) is also supported.</p>
 *
 * <p>Image paths are made relative to the output directory, so the HTML can
 * be opened directly from disk without a web server.</p>
 */
public final class HtmlReportRenderer implements ReportRenderer{

	private static final String STYLE =
		"body{font-family:Georgia,serif;max-width:48em;margin:2em auto;line-height:1.5;}"
			+ "h1,h2,h3{border-bottom:1px solid #ccc;padding-bottom:.2em;}"
			+ "table{border-collapse:collapse;width:100%;margin:.5em 0;}"
			+ "td,th{border:1px solid #ccc;padding:2px 6px;text-align:left;}"
			+ "pre{white-space:pre-wrap;font-family:ui-monospace,monospace;}"
			+ "figure{margin:1em 0;}figcaption{font-size:.85em;color:#555;}"
			+ "a{color:#1f3864;text-decoration:none;}a:hover{text-decoration:underline;}";

	/** Matches a record reference such as {@code [I12]} inside heading text. */
	private static final Pattern RECORD_ID_PATTERN = Pattern.compile("\\[([A-Za-z]+\\d+)\\]");

	/** Matches the protocol's internal-link syntax {@code [text](@id@)}. */
	private static final Pattern INTERNAL_LINK_PATTERN = Pattern.compile(
		"\\[([^\\]]+?)\\]\\(@([A-Za-z]+\\d+)@\\)");


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
		w.write("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">");
		w.write("<title>");
		w.write(html(doc.title()));
		w.write("</title>");
		w.write("<style>");
		w.write(STYLE);
		w.write("</style></head><body>");

		w.write("<h1>");
		w.write(html(doc.title()));
		w.write("</h1>");
		if(doc.subtitle() != null && !doc.subtitle().isBlank()){
			w.write("<p><em>");
			w.write(html(doc.subtitle()));
			w.write("</em></p>");
		}

		for(final ReportSection s : doc.sections())
			writeSection(w, s, baseDir);

		w.write("</body></html>");
	}


	private static void writeSection(final BufferedWriter w, final ReportSection s,
		final Path baseDir) throws IOException{
		if(s instanceof ReportSection.Heading h)
			writeHeading(w, h);
		else if(s instanceof ReportSection.Paragraph p)
			writeParagraph(w, p);
		else if(s instanceof ReportSection.Spacer)
			w.write("<br>");
		else if(s instanceof ReportSection.BulletList b)
			writeBulletList(w, b);
		else if(s instanceof ReportSection.Table t)
			writeTable(w, t);
		else if(s instanceof ReportSection.Image img)
			writeImage(w, img, baseDir);
		else if(s instanceof ReportSection.HorizontalRule)
			w.write("<hr>");
		else if(s instanceof ReportSection.PageBreak)
			w.write("<div style=\"page-break-after:always\"></div>");
	}


	/* ======================================================================
	 *                          Headings
	 * ====================================================================== */

	private static void writeHeading(final BufferedWriter w, final ReportSection.Heading h)
		throws IOException{
		final int level = Math.clamp(h.level() + 1, 1, 6);

		// Anchor emission: for level >= 2 headings, extract any [Xxx] record
		// references from the heading text and emit one anchor per ID.
		if(h.level() >= 2){
			final List<String> ids = extractRecordIds(h.text());
			for(final String id : ids){
				w.write("<a id=\"");
				w.write(html(id));
				w.write("\"></a>");
			}
		}

		w.write("<h");
		w.write(String.valueOf(level));
		w.write('>');
		w.write(inline(h.text()));
		w.write("</h");
		w.write(String.valueOf(level));
		w.write('>');
	}


	/* ======================================================================
	 *                          Paragraphs / bullets
	 * ====================================================================== */

	private static void writeParagraph(final BufferedWriter w, final ReportSection.Paragraph p)
		throws IOException{
		// Descendants trees carry alignment with spaces and newlines: emit
		// them in a preformatted block so the layout is preserved.
		if(p.text().contains(StringUtils.LF)){
			w.write("<pre>");
			w.write(html(p.text()));
			w.write("</pre>");
		}
		else{
			w.write("<p>");
			w.write(inline(p.text()));
			w.write("</p>");
		}
	}


	private static void writeBulletList(final BufferedWriter w, final ReportSection.BulletList b)
		throws IOException{
		w.write("<ul>");
		for(final String item : b.items()){
			w.write("<li>");
			w.write(inline(item));
			w.write("</li>");
		}
		w.write("</ul>");
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private static void writeTable(final BufferedWriter w, final ReportSection.Table t)
		throws IOException{
		if(t.headers().isEmpty())
			return;

		final int cols = t.headers().size();

		w.write("<table><thead><tr>");
		for(final String h : t.headers()){
			w.write("<th>");
			w.write(inline(h));
			w.write("</th>");
		}
		w.write("</tr></thead><tbody>");

		for(final var row : t.rows()){
			w.write("<tr>");
			for(int i = 0; i < cols; i++){
				final String cell = (i < row.size()? row.get(i): StringUtils.EMPTY);
				w.write("<td>");
				w.write(inline(cell));
				w.write("</td>");
			}
			w.write("</tr>");
		}
		w.write("</tbody></table>");
	}


	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	private static void writeImage(final BufferedWriter w, final ReportSection.Image img,
		final Path baseDir) throws IOException{
		final Path file = img.file();
		String href = file.toString().replace('\\', '/');
		if(baseDir != null){
			try{
				href = baseDir.relativize(file.toAbsolutePath()).toString().replace('\\', '/');
			}
			catch(final IllegalArgumentException ignored){
				// Different filesystem roots: keep the absolute path.
			}
		}

		w.write("<figure><img src=\"");
		w.write(html(href));
		w.write("\" alt=\"");
		w.write(html(img.caption() == null? StringUtils.EMPTY: img.caption()));
		w.write("\" style=\"max-width:100%\">");
		if(img.caption() != null && !img.caption().isBlank()){
			w.write("<figcaption>");
			w.write(html(img.caption()));
			w.write("</figcaption>");
		}
		w.write("</figure>");
	}


	/* ======================================================================
	 *                          Inline processing
	 * ====================================================================== */

	/** HTML-escapes the input. */
	private static String html(final String s){
		if(s == null)
			return StringUtils.EMPTY;
		return s.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;");
	}

	/**
	 * Converts a small subset of inline syntax to HTML:
	 * <ol>
	 *   <li>the protocol's internal link {@code [text](@id@)} is turned into
	 *       {@code <a href="#id">text</a>};</li>
	 *   <li>{@code **bold**} becomes {@code <b>}</li>
	 *   <li>{@code *italic*} becomes {@code <i>}</li>
	 *   <li>{@code `code`} becomes {@code <code>}</li>
	 * </ol>
	 *
	 * <p>Link conversion runs first so that inline markers inside the link
	 * text are still processed afterwards.</p>
	 */
	private static String inline(final String s){
		String t = html(s);

		// Internal links: [text](@id@) -> <a href="#id">text</a>
		final Matcher m = INTERNAL_LINK_PATTERN.matcher(t);
		final StringBuilder sb = new StringBuilder();
		while(m.find()){
			final String text = m.group(1);
			final String id = m.group(2);
			m.appendReplacement(sb, Matcher.quoteReplacement(
				"<a href=\"#" + id + "\">" + text + "</a>"));
		}
		m.appendTail(sb);
		t = sb.toString();

		// **bold**
		t = t.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
		// *italic* — single asterisk not adjacent to another asterisk
		t = t.replaceAll("(?<!\\*)\\*(?!\\*)([^*]+?)(?<!\\*)\\*(?!\\*)", "<i>$1</i>");
		// `code`
		t = t.replaceAll("`([^`]+?)`", "<code>$1</code>");

		return t;
	}


	/* ======================================================================
	 *                          Anchor helpers
	 * ====================================================================== */

	/**
	 * Extracts every record reference of the form {@code [XxxN]} from the
	 * given text, preserving order and removing duplicates.
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
