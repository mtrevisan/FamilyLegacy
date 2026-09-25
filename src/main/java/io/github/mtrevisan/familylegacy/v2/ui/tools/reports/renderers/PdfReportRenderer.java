package io.github.mtrevisan.familylegacy.v2.ui.tools.reports.renderers;

import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportDocument;
import io.github.mtrevisan.familylegacy.v2.ui.tools.reports.ReportSection;
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
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * Renders a {@link ReportDocument} as a PDF using OpenPDF.
 *
 * <p>Layout and styling:</p>
 * <ul>
 *   <li>Title is centered, large and navy; subtitle is centered italic gray.</li>
 *   <li>Headings are color-coded by level; the H1 is followed by a thin gray rule.</li>
 *   <li>Tables have a shaded, bold header row, alternating body rows, and
 *       thin gray borders.</li>
 *   <li>Inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) is
 *       translated into per-span font styling.</li>
 *   <li>Images are centered, scaled to fit a maximum box while preserving the
 *       aspect ratio, and followed by an italic gray caption.</li>
 *   <li>Every page but the first carries a small title in the top-right
 *       corner, and every page carries a centered page number in the footer.</li>
 *   <li>Paragraphs containing newlines (the descendants tree) are rendered in
 *       a monospaced font, preserving the original layout.</li>
 * </ul>
 *
 * <p><b>Important:</b> the {@link Document} must be closed while the underlying
 * {@link OutputStream} is still open, otherwise OpenPDF tries to flush its
 * buffered bytes to a closed channel and throws
 * {@link java.nio.channels.ClosedChannelException}. This is why {@code close()}
 * is called inside the try-with-resources block.</p>
 */
public final class PdfReportRenderer implements ReportRenderer{

	/* ======================================================================
	 *                          Fonts
	 * ====================================================================== */

	private static final Color TITLE_COLOR = new Color(0x1F, 0x38, 0x64);
	private static final Color H1H2_COLOR = new Color(0x1F, 0x38, 0x64);
	private static final Color H3H4_COLOR = new Color(0x40, 0x40, 0x40);
	private static final Color MUTED_COLOR = new Color(0x59, 0x59, 0x59);
	private static final Color RULE_COLOR = new Color(0xBF, 0xBF, 0xBF);

	private static final Font TITLE_FONT = new Font(Font.HELVETICA, 24, Font.BOLD, TITLE_COLOR);
	private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.ITALIC, MUTED_COLOR);
	private static final Font H1_FONT = new Font(Font.HELVETICA, 20, Font.BOLD, H1H2_COLOR);
	private static final Font H2_FONT = new Font(Font.HELVETICA, 15, Font.BOLD, H1H2_COLOR);
	private static final Font H3_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, H3H4_COLOR);
	private static final Font H4_FONT = new Font(Font.HELVETICA, 11, Font.BOLD, H3H4_COLOR);
	private static final Font BODY_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
	private static final Font MONO_FONT = new Font(Font.COURIER, 9, Font.NORMAL);
	private static final Font CAPTION_FONT = new Font(Font.HELVETICA, 10, Font.ITALIC, MUTED_COLOR);

	/* ======================================================================
	 *                          Colors (tables)
	 * ====================================================================== */

	private static final Color TABLE_HEADER_BG = new Color(0xE8, 0xEE, 0xF7);
	private static final Color TABLE_ROW_ALT_BG = new Color(0xF7, 0xF9, 0xFC);
	private static final Color TABLE_BORDER = new Color(0xD9, 0xD9, 0xD9);

	/* ======================================================================
	 *                          Spacing (points)
	 * ====================================================================== */

	private static final float H1_SPACE_BEFORE = 20;
	private static final float H1_SPACE_AFTER = 8;
	private static final float H2_SPACE_BEFORE = 16;
	private static final float H2_SPACE_AFTER = 6;
	private static final float H3_SPACE_BEFORE = 12;
	private static final float H3_SPACE_AFTER = 4;
	private static final float PAR_SPACE_AFTER = 6;
	private static final float RULE_SPACE = 10;
	private static final float IMAGE_SPACE = 8;
	private static final float LEADING_FACTOR = 1.4f;

	/** Maximum image box, in points. */
	private static final int IMAGE_MAX_PT = 400;


	/* ======================================================================
	 *                          Entry point
	 * ====================================================================== */

	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		try(OutputStream os = Files.newOutputStream(target)){
			final Document pdf = new Document(PageSize.A4, 56, 56, 64, 64);
			try{
				final PdfWriter writer = PdfWriter.getInstance(pdf, os);
				writer.setPageEvent(new PageDecorations(doc.title()));
				pdf.open();

				writeTitle(pdf, doc);
				for(final ReportSection s : doc.sections())
					writeSection(pdf, s);
			}
			catch(final DocumentException e){
				throw new IOException(e);
			}
			finally{
				// Close while `os` is still open: OpenPDF must flush its
				// buffered bytes before the channel is closed by try-with-resources.
				pdf.close();
			}
		}
	}


	/* ======================================================================
	 *                          Title
	 * ====================================================================== */

	private static void writeTitle(final Document pdf, final ReportDocument doc)
		throws DocumentException{
		final Paragraph title = new Paragraph(doc.title(), TITLE_FONT);
		title.setAlignment(Element.ALIGN_CENTER);
		title.setSpacingAfter(4);
		pdf.add(title);

		if(doc.subtitle() != null && !doc.subtitle().isBlank()){
			final Paragraph sub = new Paragraph(doc.subtitle(), SUBTITLE_FONT);
			sub.setAlignment(Element.ALIGN_CENTER);
			sub.setSpacingAfter(H1_SPACE_BEFORE);
			pdf.add(sub);
		}
	}


	/* ======================================================================
	 *                          Section dispatch
	 * ====================================================================== */

	private static void writeSection(final Document pdf, final ReportSection s)
		throws DocumentException{
		if(s instanceof ReportSection.Heading h)
			writeHeading(pdf, h);
		else if(s instanceof ReportSection.Paragraph p)
			writeParagraph(pdf, p);
		else if(s instanceof ReportSection.Spacer)
			pdf.add(Chunk.NEWLINE);
		else if(s instanceof ReportSection.BulletList b)
			writeBulletList(pdf, b);
		else if(s instanceof ReportSection.Table t)
			writeTable(pdf, t);
		else if(s instanceof ReportSection.HorizontalRule)
			writeHorizontalRule(pdf);
		else if(s instanceof ReportSection.PageBreak)
			pdf.newPage();
		else if(s instanceof ReportSection.Image img)
			writeImage(pdf, img);
	}


	/* ======================================================================
	 *                          Headings
	 * ====================================================================== */

	private static void writeHeading(final Document pdf, final ReportSection.Heading h)
		throws DocumentException{
		final int level = Math.max(1, Math.min(4, h.level()));
		final Font font = fontForHeading(level);

		final Paragraph p = new Paragraph(inline(h.text(), font));
		p.setSpacingBefore(spaceBeforeForHeading(level));
		p.setSpacingAfter(spaceAfterForHeading(level));
		p.setKeepTogether(true);
		pdf.add(p);

		// Thin rule under H1 only, drawn as a single-cell table with a
		// bottom border. OpenPDF's Paragraph has no border support.
		if(level == 1)
			pdf.add(thinRule());
	}

	/**
	 * A 100%-width table with no visible content and a thin bottom border,
	 * used to draw the rule under H1.
	 */
	private static PdfPTable thinRule(){
		final PdfPTable rule = new PdfPTable(1);
		rule.setWidthPercentage(100);
		rule.setSpacingBefore(2);
		rule.setSpacingAfter(RULE_SPACE);

		final PdfPCell cell = new PdfPCell();
		cell.setBorder(Rectangle.BOTTOM);
		cell.setBorderWidthBottom(0.7f);
		cell.setBorderColorBottom(RULE_COLOR);
		cell.setMinimumHeight(0);
		cell.setPadding(0);
		cell.setPhrase(new Phrase(" ", BODY_FONT));
		rule.addCell(cell);

		return rule;
	}

	private static Font fontForHeading(final int level){
		return switch(level){
			case 1 -> H1_FONT;
			case 2 -> H2_FONT;
			case 3 -> H3_FONT;
			default -> H4_FONT;
		};
	}

	private static float spaceBeforeForHeading(final int level){
		return switch(level){
			case 1 -> H1_SPACE_BEFORE;
			case 2 -> H2_SPACE_BEFORE;
			default -> H3_SPACE_BEFORE;
		};
	}

	private static float spaceAfterForHeading(final int level){
		return switch(level){
			case 1 -> H1_SPACE_AFTER;
			case 2 -> H2_SPACE_AFTER;
			default -> H3_SPACE_AFTER;
		};
	}


	/* ======================================================================
	 *                          Paragraphs
	 * ====================================================================== */

	private static void writeParagraph(final Document pdf, final ReportSection.Paragraph p)
		throws DocumentException{
		if(p.text().contains("\n")){
			final Paragraph para = new Paragraph(p.text(), MONO_FONT);
			para.setLeading(MONO_FONT.getSize() * 1.15f);
			para.setSpacingAfter(PAR_SPACE_AFTER);
			pdf.add(para);
			return;
		}

		final Paragraph para = new Paragraph(inline(p.text(), BODY_FONT));
		para.setLeading(BODY_FONT.getSize() * LEADING_FACTOR);
		para.setSpacingAfter(PAR_SPACE_AFTER);
		pdf.add(para);
	}


	/* ======================================================================
	 *                          Bullet lists
	 * ====================================================================== */

	private static void writeBulletList(final Document pdf, final ReportSection.BulletList b)
		throws DocumentException{
		final List list = new List(List.UNORDERED);
		list.setListSymbol("\u2022");
		list.setIndentationLeft(14);
		list.setSymbolIndent(10);

		for(final String item : b.items()){
			final ListItem li = new ListItem(inline(item, BODY_FONT));
			li.setLeading(BODY_FONT.getSize() * LEADING_FACTOR);
			list.add(li);
		}
		pdf.add(list);
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private static void writeTable(final Document pdf, final ReportSection.Table t)
		throws DocumentException{
		if(t.headers().isEmpty())
			return;

		final int cols = t.headers().size();
		final PdfPTable table = new PdfPTable(cols);
		table.setWidthPercentage(100);
		table.setSpacingBefore(2);
		table.setSpacingAfter(PAR_SPACE_AFTER);

		// Header row
		for(final String header : t.headers()){
			final PdfPCell cell = new PdfPCell(inline(header, H4_FONT));
			cell.setBackgroundColor(TABLE_HEADER_BG);
			cell.setBorderColor(TABLE_BORDER);
			cell.setBorderWidth(0.5f);
			cell.setPadding(5);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}

		// Body rows — rows shorter than the header are padded with empty cells.
		for(int r = 0; r < t.rows().size(); r++){
			final java.util.List<String> row = t.rows().get(r);
			final boolean shaded = (r % 2 == 1);

			for(int c = 0; c < cols; c++){
				final String v = (c < row.size()? row.get(c): "");
				final PdfPCell cell = new PdfPCell(inline(v, BODY_FONT));
				cell.setBorderColor(TABLE_BORDER);
				cell.setBorderWidth(0.5f);
				cell.setPadding(5);
				cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
				if(shaded)
					cell.setBackgroundColor(TABLE_ROW_ALT_BG);
				table.addCell(cell);
			}
		}

		pdf.add(table);
	}


	/* ======================================================================
	 *                          Rules / spacers
	 * ====================================================================== */

	private static void writeHorizontalRule(final Document pdf) throws DocumentException{
		pdf.add(thinRule());
	}


	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	private static void writeImage(final Document pdf, final ReportSection.Image img)
		throws DocumentException{
		try{
			final Image im = Image.getInstance(img.file().toAbsolutePath().toString());
			im.scaleToFit(IMAGE_MAX_PT, IMAGE_MAX_PT);
			im.setAlignment(Element.ALIGN_CENTER);
			pdf.add(im);

			if(img.caption() != null && !img.caption().isBlank()){
				final Paragraph cap = new Paragraph(img.caption(), CAPTION_FONT);
				cap.setAlignment(Element.ALIGN_CENTER);
				cap.setSpacingBefore(4);
				cap.setSpacingAfter(IMAGE_SPACE);
				pdf.add(cap);
			}
		}
		catch(final Exception ex){
			final Paragraph err = new Paragraph("[image not available: " + img.file() + "]",
				CAPTION_FONT);
			err.setAlignment(Element.ALIGN_CENTER);
			pdf.add(err);
		}
	}


	/* ======================================================================
	 *                          Inline Markdown
	 * ====================================================================== */

	/**
	 * Parses a tiny subset of inline Markdown ({@code **bold**}, {@code *italic*}
	 * and {@code `code`}) and produces a {@link Phrase} with per-span styling.
	 * Unbalanced markers are treated as literal characters.
	 */
	private static Phrase inline(final String text, final Font base){
		final Phrase phrase = new Phrase();
		if(text == null || text.isEmpty())
			return phrase;

		final Font bold = new Font(base.getFamily(), base.getSize(), Font.BOLD, base.getColor());
		final Font italic = new Font(base.getFamily(), base.getSize(), Font.ITALIC, base.getColor());
		final Font boldItalic = new Font(base.getFamily(), base.getSize(), Font.BOLDITALIC, base.getColor());
		final Font code = new Font(Font.COURIER, base.getSize() - 1, Font.NORMAL, base.getColor());

		final StringBuilder buf = new StringBuilder();
		boolean isBold = false, isItalic = false, isCode = false;

		for(int i = 0; i < text.length(); i++){
			final char c = text.charAt(i);

			if(c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*' && !isCode){
				flush(phrase, buf, isBold, isItalic, isCode, base, bold, italic, boldItalic, code);
				isBold = !isBold;
				i++;
			}
			else if(c == '*' && !isCode){
				flush(phrase, buf, isBold, isItalic, isCode, base, bold, italic, boldItalic, code);
				isItalic = !isItalic;
			}
			else if(c == '`' && !isBold && !isItalic){
				flush(phrase, buf, isBold, isItalic, isCode, base, bold, italic, boldItalic, code);
				isCode = !isCode;
			}
			else
				buf.append(c);
		}
		flush(phrase, buf, isBold, isItalic, isCode, base, bold, italic, boldItalic, code);

		return phrase;
	}


	private static void flush(final Phrase phrase, final StringBuilder buf,
		final boolean isBold, final boolean isItalic, final boolean isCode,
		final Font base, final Font bold, final Font italic,
		final Font boldItalic, final Font code){
		if(buf.length() == 0)
			return;

		final Font f;
		if(isCode) f = code;
		else if(isBold && isItalic) f = boldItalic;
		else if(isBold) f = bold;
		else if(isItalic) f = italic;
		else f = base;

		phrase.add(new Chunk(buf.toString(), f));
		buf.setLength(0);
	}


	/* ======================================================================
	 *                          Page decorations
	 * ====================================================================== */

	/**
	 * Draws the report title (top-right) on every page except the first, and a
	 * centered page number at the bottom of every page.
	 */
	private static final class PageDecorations extends PdfPageEventHelper{

		private final String title;
		private final Font headerFont = new Font(Font.HELVETICA, 8, Font.ITALIC,
			new Color(0x80, 0x80, 0x80));
		private final Font footerFont = new Font(Font.HELVETICA, 9, Font.NORMAL,
			new Color(0x80, 0x80, 0x80));

		PageDecorations(final String title){
			this.title = title;
		}

		@Override
		public void onEndPage(final PdfWriter writer, final Document document){
			final PdfContentByte cb = writer.getDirectContent();

			if(writer.getPageNumber() > 1 && title != null && !title.isBlank()){
				ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
					new Phrase(title, headerFont),
					document.right(), document.top() + 24, 0);
			}

			ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
				new Phrase(String.valueOf(writer.getPageNumber()), footerFont),
				(document.right() + document.left()) / 2,
				document.bottom() - 24, 0);
		}

	}

}
