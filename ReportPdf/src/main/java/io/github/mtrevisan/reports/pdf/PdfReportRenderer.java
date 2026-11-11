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
package io.github.mtrevisan.reports.pdf;

import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportMarkup;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.List;
import org.openpdf.text.ListItem;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfAction;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfOutline;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfString;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Renders a {@link ReportDocument} as a PDF using OpenPDF.
 *
 * <p>Layout and styling:</p>
 * <ul>
 *   <li>Title is centered, large and navy; subtitle is centered italic gray.</li>
 *   <li>Headings are color-coded by level; the H1 is followed by a thin gray rule. They are also the bookmarks of the file, and, when
 *       there are at least two of the first two levels, a table of contents with page numbers (that links to them) follows the title.</li>
 *   <li>Tables have a shaded, bold header row that is repeated on every page, alternating body rows, thin gray borders, and columns as wide
 *       as their content; a row shorter than the widest one is completed with empty cells.</li>
 *   <li>Inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) follows the rules of the other formats (a lone {@code *1850}
 *       is not an italic), and the links to a record jump to its heading.</li>
 *   <li>Images are centered, scaled down to fit the page (a small image is not enlarged) and followed by an italic gray caption.</li>
 *   <li>Every page but the first carries a small title in the top-right corner, and every page carries a centered page number.</li>
 *   <li>Paragraphs containing newlines (the descendants tree) are rendered in a monospaced font, preserving the original layout, and
 *       shrunk if they would be wider than the page.</li>
 *   <li>The fonts are the ones of the system, embedded, chosen by the user; a character that the font lacks is taken from another one
 *       (see {@link PdfFonts}). A language written from right to left is laid out from right to left.</li>
 * </ul>
 *
 * <p><b>Important:</b> the {@link Document} must be closed while the underlying
 * {@link OutputStream} is still open, otherwise OpenPDF tries to flush its
 * buffered bytes to a closed channel and throws
 * {@link java.nio.channels.ClosedChannelException}. This is why {@code close()}
 * is called inside the try-with-resources block.</p>
 */
public final class PdfReportRenderer implements ReportRenderer{

	private static final System.Logger LOGGER = System.getLogger(PdfReportRenderer.class.getName());

	/* ======================================================================
	 *                          Fonts and colors
	 * ====================================================================== */

	private static final Color HEADING_COLOR = new Color(0x1F, 0x38, 0x64);
	private static final Color MINOR_HEADING_COLOR = new Color(0x40, 0x40, 0x40);
	private static final Color MUTED_COLOR = new Color(0x59, 0x59, 0x59);
	private static final Color RULE_COLOR = new Color(0xBF, 0xBF, 0xBF);

	private static final float TITLE_SIZE = 24;
	private static final float SUBTITLE_SIZE = 12;
	private static final float[] HEADING_SIZES = {20, 15, 12, 11};
	private static final float BODY_SIZE = 11;
	private static final float MONO_SIZE = 9;
	private static final float MONO_MIN_SIZE = 5;
	private static final float CAPTION_SIZE = 10;

	/* ======================================================================
	 *                          Colors (tables)
	 * ====================================================================== */

	private static final Color TABLE_HEADER_BG = new Color(0xE8, 0xEE, 0xF7);
	private static final Color TABLE_ROW_ALT_BG = new Color(0xF7, 0xF9, 0xFC);
	private static final Color TABLE_BORDER = new Color(0xD9, 0xD9, 0xD9);

	/* ======================================================================
	 *                          Spacing (points)
	 * ====================================================================== */

	private static final float[] HEADING_SPACE_BEFORE = {20, 16, 12, 12};
	private static final float[] HEADING_SPACE_AFTER = {12, 6, 4, 4};
	private static final float PAR_SPACE_AFTER = 6;
	private static final float IMAGE_SPACE = 8;
	private static final float LEADING_FACTOR = 1.4f;
	private static final float MONO_LEADING_FACTOR = 1.15f;
	private static final float PAGE_MARGIN_SIDE = 56;
	private static final float PAGE_MARGIN_VERTICAL = 64;

	/** Maximum height of an image, in points; its width is the one of the page. */
	private static final float IMAGE_MAX_HEIGHT = 480;

	/** The columns of a table are as wide as their longest text (in characters), between these values. */
	private static final int COLUMN_MIN_WEIGHT = 6;
	private static final int COLUMN_MAX_WEIGHT = 40;
	/** The rows that are measured to size the columns of a table. */
	private static final int COLUMN_SAMPLE_ROWS = 200;

	private static final int TOC_MIN_HEADINGS = 2;
	private static final int TOC_MAX_LEVEL = 2;
	/** What the page number of an entry of the table of contents is until the pages are known: as wide as a real one. */
	private static final String TOC_PLACEHOLDER = "000";

	private static final String CREATOR = "Family Legacy";
	private static final String BULLET = "•";
	private static final String HEADING_PREFIX = "h";


	@Override
	public String id(){
		return "pdf";
	}

	@Override
	public String displayName(){
		return "PDF";
	}

	@Override
	public String extension(){
		return ".pdf";
	}

	@Override
	public boolean choosesFont(){
		return true;
	}

	@Override
	public java.util.List<String> fonts(){
		return PdfFonts.families();
	}


	/* ======================================================================
	 *                          Entry point
	 * ====================================================================== */

	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		render(doc, target, null);
	}

	@Override
	public void render(final ReportDocument doc, final Path target, final String font) throws IOException{
		final PdfFonts fonts = new PdfFonts(font, allText(doc), needsMonospaced(doc));

		//the table of contents has the page of each heading, that is known only after the layout: the first one finds them (the entries
		//have a placeholder as wide as a number), the second one writes them; a third one is needed only if the numbers moved something
		final boolean contents = (contentsHeadings(doc).size() >= TOC_MIN_HEADINGS);
		Map<Integer, Integer> pages = Map.of();
		if(contents)
			pages = new Layout(doc, fonts, pages).write(OutputStream.nullOutputStream());

		for(int pass = 0; ; pass ++){
			final Map<Integer, Integer> observed;
			try(final OutputStream os = Files.newOutputStream(target)){
				observed = new Layout(doc, fonts, pages).write(os);
				// Close while `os` is still open: OpenPDF must flush its
				// buffered bytes before the channel is closed by try-with-resources.
			}
			if(!contents || observed.equals(pages) || pass > 0)
				return;

			pages = observed;
		}
	}

	/** @return	The indices of the sections that are headings of the table of contents (the first levels, that have a text). */
	private static java.util.List<Integer> contentsHeadings(final ReportDocument doc){
		final java.util.List<Integer> headings = new ArrayList<>();
		for(int i = 0; i < doc.sections().size(); i ++)
			if(doc.sections().get(i) instanceof ReportSection.Heading h && h.level() <= TOC_MAX_LEVEL && !contentsTitle(h).isBlank())
				headings.add(i);
		return headings;
	}

	private static String contentsTitle(final ReportSection.Heading heading){
		return ReportMarkup.withoutRecordIds(ReportMarkup.plain(heading.text()));
	}

	/** @return	All the text of the document, to know which characters the fonts must have. */
	private static String allText(final ReportDocument doc){
		final StringBuilder sb = new StringBuilder();
		sb.append(doc.title()).append(' ').append(doc.subtitle()).append(' ').append(doc.contentsTitle()).append('\n');
		for(final ReportSection section : doc.sections())
			switch(section){
				case ReportSection.Heading h -> sb.append(h.text()).append('\n');
				case ReportSection.Paragraph p -> sb.append(p.text()).append('\n');
				case ReportSection.BulletList b -> b.items().forEach(item -> sb.append(item).append('\n'));
				case ReportSection.Table t -> {
					t.headers().forEach(cell -> sb.append(cell).append(' '));
					t.rows().forEach(row -> row.forEach(cell -> sb.append(cell).append(' ')));
					sb.append('\n');
				}
				case ReportSection.Image i -> sb.append(i.caption()).append('\n');
				default -> {}
			}
		return sb.toString();
	}

	/** @return	The chunks of the phrase (the very objects it holds: their attributes can be changed). */
	private static java.util.List<Chunk> chunks(final Phrase phrase){
		final java.util.List<Chunk> chunks = new ArrayList<>(phrase.size());
		for(final Element element : phrase)
			if(element instanceof Chunk chunk)
				chunks.add(chunk);
		return chunks;
	}

	private static boolean needsMonospaced(final ReportDocument doc){
		for(final ReportSection section : doc.sections())
			if(section instanceof ReportSection.Paragraph p && p.text().contains(StringUtils.LF))
				return true;
		return allText(doc).indexOf('`') >= 0;
	}


	/* ======================================================================
	 *                          One layout of the document
	 * ====================================================================== */

	/** One pass of the writing of the document. */
	private static final class Layout{

		private final ReportDocument doc;
		private final PdfFonts fonts;
		private final Map<Integer, Integer> pages;
		private final boolean rtl;
		/** The records that have a heading, that a link can jump to. */
		private final Set<String> anchors = new HashSet<>();
		/** The records whose anchor was already written (a destination cannot be defined twice). */
		private final Set<String> writtenAnchors = new HashSet<>();
		private final PdfOutline[] outlines = new PdfOutline[HEADING_SIZES.length + 1];

		private Document pdf;
		private PdfWriter writer;


		Layout(final ReportDocument doc, final PdfFonts fonts, final Map<Integer, Integer> pages){
			this.doc = doc;
			this.fonts = fonts;
			this.pages = pages;
			rtl = doc.isRightToLeft();

			for(final ReportSection section : doc.sections())
				if(section instanceof ReportSection.Heading h)
					anchors.addAll(ReportMarkup.recordIds(h.text()));
		}


		/**
		 * @param os	Where to write.
		 * @return	The page of each heading of the table of contents (by the index of its section).
		 */
		Map<Integer, Integer> write(final OutputStream os) throws IOException{
			final Map<Integer, Integer> observed = new HashMap<>();
			try(final Document document = new Document(PageSize.A4, PAGE_MARGIN_SIDE, PAGE_MARGIN_SIDE, PAGE_MARGIN_VERTICAL,
					PAGE_MARGIN_VERTICAL)){
				pdf = document;
				writer = PdfWriter.getInstance(document, os);
				if(rtl)
					writer.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
				writer.setPageEvent(new PageDecorations(fonts, doc.title(), rtl, observed));
				//the language of the text (screen readers, hyphenation of the viewers) and the properties of the document
				writer.getExtraCatalog()
					.put(PdfName.LANG, new PdfString(doc.language()));
				writer.setViewerPreferences(PdfWriter.DisplayDocTitle | PdfWriter.PageModeUseOutlines);
				document.addTitle(doc.title());
				document.addCreator(CREATOR);
				if(doc.subtitle() != null && !doc.subtitle().isBlank())
					document.addSubject(doc.subtitle());
				document.open();

				writeTitle();
				writeContents();
				for(int i = 0; i < doc.sections().size(); i ++)
					writeSection(i, doc.sections().get(i));
			}
			catch(final DocumentException e){
				throw new IOException(e);
			}
			return observed;
		}


		/* ==================================================================
		 *                          Title and contents
		 * ================================================================== */

		private void writeTitle() throws DocumentException{
			final Paragraph title = paragraph(inline(doc.title(), TITLE_SIZE, true, false, HEADING_COLOR), TITLE_SIZE * 1.2f);
			title.setAlignment(Element.ALIGN_CENTER);
			title.setSpacingAfter(4);
			pdf.add(title);

			if(doc.subtitle() != null && !doc.subtitle().isBlank()){
				final Paragraph sub = paragraph(inline(doc.subtitle(), SUBTITLE_SIZE, false, true, MUTED_COLOR), SUBTITLE_SIZE * LEADING_FACTOR);
				sub.setAlignment(Element.ALIGN_CENTER);
				sub.setSpacingAfter(HEADING_SPACE_BEFORE[0]);
				pdf.add(sub);
			}
		}

		/** The table of contents: the entries link to the headings, with their page. */
		private void writeContents() throws DocumentException{
			final java.util.List<Integer> headings = contentsHeadings(doc);
			if(headings.size() < TOC_MIN_HEADINGS)
				return;

			final Paragraph title = paragraph(inline(doc.contentsTitle(), HEADING_SIZES[1], true, false, HEADING_COLOR),
				HEADING_SIZES[1] * 1.25f);
			title.setSpacingBefore(HEADING_SPACE_BEFORE[1]);
			title.setSpacingAfter(HEADING_SPACE_AFTER[1]);
			title.setKeepTogether(true);
			pdf.add(title);

			final PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			table.setSpacingAfter(HEADING_SPACE_BEFORE[0]);
			table.setWidths(new float[]{92, 8});
			if(rtl)
				table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
			for(final int index : headings){
				final ReportSection.Heading heading = (ReportSection.Heading)doc.sections().get(index);

				final Phrase text = fonts.phrase(contentsTitle(heading), BODY_SIZE, heading.level() == 1, false, false, null);
				chunks(text)
					.forEach(chunk -> chunk.setLocalGoto(HEADING_PREFIX + index));
				final PdfPCell entry = tocCell(text);
				entry.setPaddingLeft(rtl? 0: (heading.level() - 1) * 14);
				entry.setPaddingRight(rtl? (heading.level() - 1) * 14: 0);
				table.addCell(entry);

				final Integer pageNumber = pages.get(index);
				final Phrase number = fonts.phrase((pageNumber != null? String.valueOf(pageNumber): TOC_PLACEHOLDER), BODY_SIZE, false, false, false,
					null);
				final PdfPCell page = tocCell(number);
				page.setHorizontalAlignment(rtl? Element.ALIGN_LEFT: Element.ALIGN_RIGHT);
				table.addCell(page);
			}
			pdf.add(table);
		}

		private PdfPCell tocCell(final Phrase phrase){
			final PdfPCell cell = new PdfPCell(phrase);
			cell.setBorder(Rectangle.NO_BORDER);
			cell.setPaddingTop(1);
			cell.setPaddingBottom(2);
			cell.setPaddingLeft(0);
			cell.setPaddingRight(0);
			if(rtl)
				cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
			return cell;
		}


		/* ==================================================================
		 *                          Section dispatch
		 * ================================================================== */

		private void writeSection(final int index, final ReportSection section) throws DocumentException{
			switch(section){
				case ReportSection.Heading h -> writeHeading(index, h);
				case ReportSection.Paragraph p -> writeParagraph(p);
				case ReportSection.Spacer ignored -> writeSpacer();
				case ReportSection.BulletList b -> writeBulletList(b);
				case ReportSection.Table t -> writeTable(t);
				case ReportSection.HorizontalRule ignored -> writeHorizontalRule();
				case ReportSection.PageBreak ignored -> pdf.newPage();
				case ReportSection.Image img -> writeImage(img);
			}
		}


		/* ==================================================================
		 *                          Headings
		 * ================================================================== */

		private void writeHeading(final int index, final ReportSection.Heading h) throws DocumentException{
			final int level = Math.clamp(h.level(), 1, HEADING_SIZES.length);
			final float size = HEADING_SIZES[level - 1];
			final Color color = (level <= 2? HEADING_COLOR: MINOR_HEADING_COLOR);

			final Phrase text = inline(h.text(), size, true, false, color);
			final java.util.List<Chunk> chunks = chunks(text);
			final String name = HEADING_PREFIX + index;
			if(!chunks.isEmpty()){
				//the destination of the bookmark and of the table of contents, and the page where the heading lands
				chunks.getFirst()
					.setLocalDestination(name);
				if(level <= TOC_MAX_LEVEL)
					chunks.getFirst()
						.setGenericTag(name);
			}

			final Paragraph p = new Paragraph();
			p.setLeading(size * 1.25f);
			//the destinations of the links to the records of the heading: a second one with the same name is ignored
			for(final String id : ReportMarkup.recordIds(h.text()))
				if(writtenAnchors.add(id))
					p.add(new Chunk(StringUtils.SPACE, new Font(Font.HELVETICA, 1)).setLocalDestination(id));
			p.add(text);
			p.setSpacingBefore(HEADING_SPACE_BEFORE[level - 1]);
			p.setSpacingAfter(HEADING_SPACE_AFTER[level - 1]);
			p.setKeepTogether(true);
			//a thin rule under H1 only, drawn on the baseline of its last line, under the text
			if(level == 1)
				p.add(new Chunk(new LineSeparator(0.7f, 100, RULE_COLOR, Element.ALIGN_CENTER, -(size * 0.25f))));
			pdf.add(p);

			final String title = contentsTitle(h);
			if(!chunks.isEmpty() && !title.isBlank())
				bookmark(level, name, title);
		}

		/** Adds a bookmark, under the nearest heading of a higher level. */
		private void bookmark(final int level, final String name, final String title){
			PdfOutline parent = writer.getRootOutline();
			for(int l = level - 1; l >= 1; l --)
				if(outlines[l] != null){
					parent = outlines[l];
					break;
				}

			outlines[level] = new PdfOutline(parent, PdfAction.gotoLocalPage(name, false), title);
			for(int l = level + 1; l < outlines.length; l ++)
				outlines[l] = null;
		}


		/* ==================================================================
		 *                          Paragraphs
		 * ================================================================== */

		private void writeParagraph(final ReportSection.Paragraph p) throws DocumentException{
			if(p.text().contains(StringUtils.LF)){
				//preformatted: the font is shrunk, if needed, so that the longest line fits the page and the alignment is not lost
				final float available = pdf.right() - pdf.left();
				float size = MONO_SIZE;
				final float width = fonts.width(p.text(), size, true);
				if(width > available)
					size = Math.max(MONO_MIN_SIZE, size * available / width);

				final Paragraph para = paragraph(fonts.phrase(p.text(), size, false, false, true, null), size * MONO_LEADING_FACTOR);
				para.setAlignment(Element.ALIGN_LEFT);
				para.setSpacingAfter(PAR_SPACE_AFTER);
				pdf.add(para);
				return;
			}

			final Paragraph para = paragraph(inline(p.text(), BODY_SIZE, false, false, null), BODY_SIZE * LEADING_FACTOR);
			para.setSpacingAfter(PAR_SPACE_AFTER);
			pdf.add(para);
		}

		private void writeSpacer() throws DocumentException{
			//a blank line at the top of a page (after a page break) is only a hole
			if(Math.abs(writer.getVerticalPosition(false) - pdf.top()) < 1)
				return;

			pdf.add(paragraph(fonts.phrase(StringUtils.SPACE, BODY_SIZE, false, false, false, null), BODY_SIZE * LEADING_FACTOR));
		}


		/* ==================================================================
		 *                          Bullet lists
		 * ================================================================== */

		private void writeBulletList(final ReportSection.BulletList b) throws DocumentException{
			if(rtl){
				//the list of OpenPDF puts the symbol on the left: here the bullet is the first character of the line
				for(final String item : b.items()){
					final Phrase line = fonts.phrase(BULLET + StringUtils.SPACE, BODY_SIZE, false, false, false, null);
					line.addAll(inline(item, BODY_SIZE, false, false, null));
					final Paragraph para = paragraph(line, BODY_SIZE * LEADING_FACTOR);
					para.setIndentationRight(14);
					pdf.add(para);
				}
				return;
			}

			final List list = new List(List.UNORDERED);
			//the symbol has the font of the text, so that it has its size and color
			list.setListSymbol(chunks(fonts.phrase(BULLET, BODY_SIZE, false, false, false, null))
				.getFirst());
			list.setIndentationLeft(14);
			list.setSymbolIndent(10);

			for(final String item : b.items()){
				final ListItem li = new ListItem();
				li.setLeading(BODY_SIZE * LEADING_FACTOR);
				li.add(inline(item, BODY_SIZE, false, false, null));
				list.add(li);
			}
			pdf.add(list);
		}


		/* ==================================================================
		 *                          Tables
		 * ================================================================== */

		private void writeTable(final ReportSection.Table t) throws DocumentException{
			final int cols = t.columns();
			if(cols == 0)
				return;

			final PdfPTable table = new PdfPTable(cols);
			table.setWidthPercentage(100);
			table.setSpacingBefore(2);
			table.setSpacingAfter(PAR_SPACE_AFTER);
			table.setWidths(columnWidths(t, cols));
			if(rtl)
				table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

			// Header row (repeated on every page); the columns without a header are empty
			if(!t.headers().isEmpty()){
				for(int c = 0; c < cols; c ++){
					final PdfPCell cell = cell(c < t.headers().size()? t.headers().get(c): StringUtils.EMPTY, true);
					cell.setBackgroundColor(TABLE_HEADER_BG);
					table.addCell(cell);
				}
				table.setHeaderRows(1);
			}

			// Body rows: rows shorter than the widest one are padded with empty cells.
			for(int r = 0; r < t.rows().size(); r ++){
				final java.util.List<String> row = t.rows()
					.get(r);

				for(int c = 0; c < cols; c ++){
					final PdfPCell cell = cell(c < row.size()? row.get(c): StringUtils.EMPTY, false);
					if(r % 2 == 1)
						cell.setBackgroundColor(TABLE_ROW_ALT_BG);
					table.addCell(cell);
				}
			}

			pdf.add(table);
		}

		private PdfPCell cell(final String text, final boolean header){
			final PdfPCell cell = new PdfPCell(header
				? inline(text, BODY_SIZE, true, false, MINOR_HEADING_COLOR)
				: inline(text, BODY_SIZE, false, false, null));
			cell.setBorderColor(TABLE_BORDER);
			cell.setBorderWidth(0.5f);
			cell.setPadding(5);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			if(rtl)
				cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
			return cell;
		}

		/** @return	The relative width of the columns: the length of their longest text, within a range, so that a name is not squeezed. */
		private static float[] columnWidths(final ReportSection.Table t, final int cols){
			final float[] widths = new float[cols];
			Arrays.fill(widths, COLUMN_MIN_WEIGHT);

			for(int c = 0; c < t.headers().size(); c ++)
				widths[c] = Math.max(widths[c], weight(t.headers().get(c)));
			final int rows = Math.min(t.rows().size(), COLUMN_SAMPLE_ROWS);
			for(int r = 0; r < rows; r ++){
				final java.util.List<String> row = t.rows().get(r);
				for(int c = 0; c < row.size(); c ++)
					widths[c] = Math.max(widths[c], weight(row.get(c)));
			}
			return widths;
		}

		private static float weight(final String text){
			return Math.min(COLUMN_MAX_WEIGHT, ReportMarkup.plain(text).length());
		}


		/* ==================================================================
		 *                          Rules
		 * ================================================================== */

		private void writeHorizontalRule() throws DocumentException{
			final Paragraph rule = new Paragraph(8);
			rule.add(new Chunk(new LineSeparator(0.7f, 100, RULE_COLOR, Element.ALIGN_CENTER, 0)));
			rule.setSpacingBefore(2);
			rule.setSpacingAfter(PAR_SPACE_AFTER);
			pdf.add(rule);
		}


		/* ==================================================================
		 *                          Images
		 * ================================================================== */

		private void writeImage(final ReportSection.Image img) throws DocumentException{
			final Image image = load(img.file());
			//an image that cannot be read leaves only its caption, as in the other formats
			if(image != null){
				final float maxWidth = pdf.right() - pdf.left();
				//an image that is smaller than the page keeps its size: it is not enlarged
				if(image.getWidth() > maxWidth || image.getHeight() > IMAGE_MAX_HEIGHT)
					image.scaleToFit(maxWidth, IMAGE_MAX_HEIGHT);
				image.setAlignment(Element.ALIGN_CENTER);
				pdf.add(image);
			}

			if(!img.caption().isBlank()){
				final Paragraph cap = paragraph(fonts.phrase(img.caption(), CAPTION_SIZE, false, true, false, MUTED_COLOR),
					CAPTION_SIZE * LEADING_FACTOR);
				cap.setAlignment(Element.ALIGN_CENTER);
				cap.setSpacingBefore(4);
				cap.setSpacingAfter(IMAGE_SPACE);
				pdf.add(cap);
			}
		}

		private static Image load(final Path file){
			try{
				return Image.getInstance(file.toAbsolutePath().toString());
			}
			catch(final IOException | RuntimeException e){
				//the path is not in the file: it is the path of the user
				LOGGER.log(System.Logger.Level.WARNING, "Cannot read the image {0}: {1}", file, e.toString());
				return null;
			}
		}


		/* ==================================================================
		 *                          Text
		 * ================================================================== */

		private static Paragraph paragraph(final Phrase phrase, final float leading){
			final Paragraph paragraph = new Paragraph();
			paragraph.setLeading(leading);
			paragraph.add(phrase);
			return paragraph;
		}

		/**
		 * The text with its inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}, the links to a record) as chunks.
		 *
		 * @param text	The text.
		 * @param size	The size of the font, in points.
		 * @param bold	Whether the text is bold as a whole.
		 * @param italic	Whether the text is italic as a whole.
		 * @param color	The color of the text ({@code null} for black).
		 */
		private Phrase inline(final String text, final float size, final boolean bold, final boolean italic, final Color color){
			final Phrase phrase = new Phrase();
			for(final ReportMarkup.Span span : ReportMarkup.spans(text)){
				final Phrase part = fonts.phrase(span.text(), (span.code()? size - 1: size), bold || span.bold(),
					italic || span.italic(), span.code(), color);
				final boolean linked = (span.link() != null && anchors.contains(span.link()));
				for(final Chunk chunk : chunks(part)){
					if(linked)
						chunk.setLocalGoto(span.link());
					phrase.add(chunk);
				}
			}
			return phrase;
		}

	}


	/* ======================================================================
	 *                          Page decorations
	 * ====================================================================== */

	/**
	 * Draws the report title (top-right) on every page except the first, and a
	 * centered page number at the bottom of every page; notes the page of each heading of the table of contents.
	 */
	private static final class PageDecorations extends PdfPageEventHelper{

		private static final Color DECORATION_COLOR = new Color(0x80, 0x80, 0x80);
		private static final float HEADER_SIZE = 8;
		private static final float FOOTER_SIZE = 9;
		private static final float DECORATION_DISTANCE = 24;

		private final PdfFonts fonts;
		private final String title;
		private final boolean rtl;
		private final Map<Integer, Integer> pages;


		PageDecorations(final PdfFonts fonts, final String title, final boolean rtl, final Map<Integer, Integer> pages){
			this.fonts = fonts;
			this.title = title;
			this.rtl = rtl;
			this.pages = pages;
		}


		@Override
		public void onGenericTag(final PdfWriter writer, final Document document, final Rectangle rect, final String text){
			if(text.startsWith(HEADING_PREFIX))
				pages.putIfAbsent(Integer.parseInt(text.substring(HEADING_PREFIX.length())), writer.getPageNumber());
		}

		@Override
		public void onEndPage(final PdfWriter writer, final Document document){
			final PdfContentByte cb = writer.getDirectContent();

			if(writer.getPageNumber() > 1 && title != null && !title.isBlank()){
				//a long title is cut: it must not run out of the page
				final String header = fonts.truncate(title, HEADER_SIZE, document.right() - document.left());
				final Phrase phrase = fonts.phrase(header, HEADER_SIZE, false, true, false, DECORATION_COLOR);
				if(rtl)
					ColumnText.showTextAligned(cb, Element.ALIGN_LEFT, phrase, document.left(), document.top() + DECORATION_DISTANCE, 0,
						PdfWriter.RUN_DIRECTION_RTL, 0);
				else
					ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT, phrase, document.right(), document.top() + DECORATION_DISTANCE, 0);
			}

			ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
				fonts.phrase(String.valueOf(writer.getPageNumber()), FOOTER_SIZE, false, false, false, DECORATION_COLOR),
				(document.right() + document.left()) / 2, document.bottom() - DECORATION_DISTANCE, 0);
		}

	}

}
