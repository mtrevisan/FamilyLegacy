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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;


/**
 * Writes the sections of a report as XHTML (that is also valid HTML5), for the renderers of a book and of a website: the markup of the
 * body of a page, not the page.
 */
public final class ReportXhtml{

	private ReportXhtml(){}


	/**
	 * Writes a section.
	 *
	 * @param sb	Where the markup goes.
	 * @param section	The section.
	 * @param linkTarget	Gives the address of the page that has a record (see {@link ReportMarkup#inline(String, UnaryOperator)}).
	 * @param imageHref	Gives the address of an image inside the output, after having put it there; {@code null} if it cannot be used
	 * 	(then only its caption is written).
	 */
	public static void writeSection(final StringBuilder sb, final ReportSection section, final UnaryOperator<String> linkTarget,
			final Function<Path, String> imageHref){
		writeSection(sb, section, linkTarget, imageHref, new HashSet<>());
	}

	/**
	 * Writes a section, with the anchors of the records written only once on a page.
	 *
	 * @param sb	Where the markup goes.
	 * @param section	The section.
	 * @param linkTarget	Gives the address of the page that has a record (see {@link ReportMarkup#inline(String, UnaryOperator)}).
	 * @param imageHref	Gives the address of an image inside the output, after having put it there; {@code null} if it cannot be used
	 * 	(then only its caption is written).
	 * @param writtenAnchors	The identifiers of the records whose anchor is already in the page: the same set for all the sections of
	 * 	a page, since an identifier cannot be twice in a page.
	 */
	public static void writeSection(final StringBuilder sb, final ReportSection section, final UnaryOperator<String> linkTarget,
			final Function<Path, String> imageHref, final Set<String> writtenAnchors){
		writeSection(sb, section, linkTarget, imageHref, writtenAnchors, Map.of());
	}

	/**
	 * Writes a section, giving an {@code id} to the headings that have one.
	 *
	 * @param sb	Where the markup goes.
	 * @param section	The section.
	 * @param linkTarget	Gives the address of the page that has a record (see {@link ReportMarkup#inline(String, UnaryOperator)}).
	 * @param imageHref	Gives the address of an image inside the output, after having put it there; {@code null} if it cannot be used.
	 * @param writtenAnchors	The identifiers of the records whose anchor is already in the page.
	 * @param headingIds	The {@code id} attribute of a heading (compared by identity, since two headings can have the same text), for
	 * 	the headings that must be the target of a link (a table of contents); the others have none.
	 */
	public static void writeSection(final StringBuilder sb, final ReportSection section, final UnaryOperator<String> linkTarget,
			final Function<Path, String> imageHref, final Set<String> writtenAnchors, final Map<ReportSection, String> headingIds){
		switch(section){
			case ReportSection.Heading heading -> {
				for(final String id : ReportMarkup.recordIds(heading.text()))
					if(writtenAnchors.add(id))
						sb.append("<a id=\"").append(ReportMarkup.escape(id)).append("\"></a>");
				final int level = Math.clamp(heading.level() + 1, 1, 6);
				final String headingId = headingIds.get(heading);
				sb.append("<h").append(level);
				if(headingId != null)
					sb.append(" id=\"").append(ReportMarkup.escape(headingId)).append('"');
				sb.append('>').append(ReportMarkup.inline(heading.text(), linkTarget))
					.append("</h").append(level).append(">\n");
			}
			case ReportSection.Paragraph paragraph -> {
				//the trees of the descendants carry the alignment with spaces and new lines: a preformatted block keeps it
				if(paragraph.text().contains("\n"))
					sb.append("<pre>").append(ReportMarkup.escape(paragraph.text())).append("</pre>\n");
				else
					sb.append("<p>").append(ReportMarkup.inline(paragraph.text(), linkTarget)).append("</p>\n");
			}
			case ReportSection.Spacer ignored -> sb.append("<br/>\n");
			case ReportSection.BulletList list -> {
				sb.append("<ul>");
				for(final String item : list.items())
					sb.append("<li>").append(ReportMarkup.inline(item, linkTarget)).append("</li>");
				sb.append("</ul>\n");
			}
			case ReportSection.Table table -> writeTable(sb, table, linkTarget);
			case ReportSection.Image image -> writeImage(sb, image, imageHref);
			case ReportSection.HorizontalRule ignored -> sb.append("<hr/>\n");
			case ReportSection.PageBreak ignored -> {
				//a book and a website have no pages
			}
		}
	}

	private static void writeTable(final StringBuilder sb, final ReportSection.Table table, final UnaryOperator<String> linkTarget){
		final int columns = table.columns();
		if(columns == 0)
			return;

		sb.append("<table>");
		//a table without headers has no header row; the columns without a header are empty
		if(!table.headers().isEmpty()){
			sb.append("<thead><tr>");
			for(int i = 0; i < columns; i ++)
				sb.append("<th>").append(ReportMarkup.inline(i < table.headers().size()? table.headers().get(i): "", linkTarget))
					.append("</th>");
			sb.append("</tr></thead>");
		}
		sb.append("<tbody>");
		for(final List<String> row : table.rows()){
			sb.append("<tr>");
			for(int i = 0; i < columns; i ++)
				sb.append("<td>").append(ReportMarkup.inline(i < row.size()? row.get(i): "", linkTarget)).append("</td>");
			sb.append("</tr>");
		}
		sb.append("</tbody></table>\n");
	}

	private static void writeImage(final StringBuilder sb, final ReportSection.Image section, final Function<Path, String> imageHref){
		final String caption = (section.caption() == null? "": section.caption());
		final String href = imageHref.apply(section.file());
		if(href == null){
			//an image that cannot be read or is not of a format of the output: its caption stays
			if(!caption.isBlank())
				sb.append("<p>").append(ReportMarkup.escape(caption)).append("</p>\n");
			return;
		}

		//without a caption the name of the file is the alternative text
		final String alt = (!caption.isBlank() || section.file() == null || section.file().getFileName() == null? caption:
			section.file().getFileName().toString());
		sb.append("<figure><img src=\"").append(ReportMarkup.escape(href)).append("\" alt=\"").append(ReportMarkup.escape(alt))
			.append("\"/>");
		if(!caption.isBlank())
			sb.append("<figcaption>").append(ReportMarkup.escape(caption)).append("</figcaption>");
		sb.append("</figure>\n");
	}

}
