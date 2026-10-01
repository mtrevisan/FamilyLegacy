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
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


/**
 * Renders a {@link ReportDocument} as a DOCX (Office Open XML) file via
 * Apache POI.
 *
 * <p>Layout and styling:</p>
 * <ul>
 *   <li>Title is centered, large and dark blue; subtitle is centered italic gray.</li>
 *   <li>Headings use the built-in {@code Heading1}..{@code Heading4} styles so
 *       they participate in the navigation pane and TOC, but their run-level
 *       formatting is overridden for a consistent look across Word and
 *       LibreOffice.</li>
 *   <li>Tables have a shaded, bold header row and thin gray borders.</li>
 *   <li>Inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) is
 *       converted into separate runs with the corresponding font styling.</li>
 *   <li>Images are centered, scaled to fit a maximum box while preserving the
 *       aspect ratio, and followed by an italic caption.</li>
 *   <li>Paragraphs containing newlines (the descendants tree) are rendered in a
 *       monospaced font with hard line breaks, preserving the original layout.</li>
 * </ul>
 */
public final class DocxReportRenderer implements ReportRenderer{

	/** Heading sizes, in points. */
	private static final int H1_SIZE = 22;
	private static final int H2_SIZE = 16;
	private static final int H3_SIZE = 13;
	private static final int H4_SIZE = 12;
	private static final int BODY_SIZE = 11;

	/** Spacing values, in twips (1 pt = 20 twips). */
	private static final int HEADING_SPACING_BEFORE = 240;  // 12 pt
	private static final int HEADING_SPACING_AFTER = 120;  //  6 pt
	private static final int PARAGRAPH_SPACING_AFTER = 100; //  5 pt
	private static final int BULLET_SPACING_AFTER = 40;  //  2 pt
	private static final int CELL_SPACING = 20;  //  1 pt
	private static final int IMAGE_SPACING_AFTER = 80;  //  4 pt

	/** Maximum image box, in points. */
	private static final int IMAGE_MAX_PT = 400;

	/** Header cell background color (light blue-gray). */
	private static final String HEADER_BG = "EEF2F7";

	/** Colors used for text. */
	private static final String TITLE_COLOR = "1F3864";
	private static final String H1H2_COLOR = "1F3864";
	private static final String H3H4_COLOR = "404040";
	private static final String MUTED_COLOR = "595959";


	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		try(XWPFDocument d = new XWPFDocument();
			 OutputStream os = Files.newOutputStream(target)){

			writeTitle(d, doc);

			for(final ReportSection s : doc.sections())
				writeSection(d, s);

			d.write(os);
		}
	}


	/* ======================================================================
	 *                          Title
	 * ====================================================================== */

	private static void writeTitle(final XWPFDocument d, final ReportDocument doc){
		final XWPFParagraph title = d.createParagraph();
		title.setAlignment(ParagraphAlignment.CENTER);
		title.setSpacingAfter(80);

		final XWPFRun titleRun = title.createRun();
		titleRun.setText(doc.title());
		titleRun.setBold(true);
		titleRun.setFontSize(24);
		titleRun.setColor(TITLE_COLOR);

		if(doc.subtitle() != null && !doc.subtitle().isBlank()){
			final XWPFParagraph sub = d.createParagraph();
			sub.setAlignment(ParagraphAlignment.CENTER);
			sub.setSpacingAfter(HEADING_SPACING_BEFORE);

			final XWPFRun subRun = sub.createRun();
			subRun.setText(doc.subtitle());
			subRun.setItalic(true);
			subRun.setFontSize(12);
			subRun.setColor(MUTED_COLOR);
		}
	}


	/* ======================================================================
	 *                          Section dispatch
	 * ====================================================================== */

	private static void writeSection(final XWPFDocument d, final ReportSection s){
		if(s instanceof ReportSection.Heading h)
			writeHeading(d, h);
		else if(s instanceof ReportSection.Paragraph p)
			writeParagraph(d, p);
		else if(s instanceof ReportSection.Spacer)
			writeSpacer(d);
		else if(s instanceof ReportSection.BulletList b)
			writeBulletList(d, b);
		else if(s instanceof ReportSection.Table t)
			writeTable(d, t);
		else if(s instanceof ReportSection.HorizontalRule)
			writeHorizontalRule(d);
		else if(s instanceof ReportSection.PageBreak)
			d.createParagraph().setPageBreak(true);
		else if(s instanceof ReportSection.Image img)
			writeImage(d, img);
	}


	/* ======================================================================
	 *                          Headings
	 * ====================================================================== */

	private static void writeHeading(final XWPFDocument d, final ReportSection.Heading h){
		final int level = Math.clamp(h.level(), 1, 4);

		final XWPFParagraph p = d.createParagraph();
		// Built-in styles give Word the document structure (navigation, TOC).
		p.setStyle("Heading" + level);
		p.setSpacingBefore(HEADING_SPACING_BEFORE);
		p.setSpacingAfter(HEADING_SPACING_AFTER);

		// Heading text is always bold, but inline markers such as
		// "*(subject)*" must still toggle italic on top of the base style.
		writeInlineMarkdown(p, h.text(), headingSize(level), true, false, headingColor(level));
	}

	private static int headingSize(final int level){
		return switch(level){
			case 1 -> H1_SIZE;
			case 2 -> H2_SIZE;
			case 3 -> H3_SIZE;
			default -> H4_SIZE;
		};
	}

	private static String headingColor(final int level){
		return (level <= 2? H1H2_COLOR: H3H4_COLOR);
	}


	/* ======================================================================
	 *                          Paragraphs
	 * ====================================================================== */

	private static void writeParagraph(final XWPFDocument d, final ReportSection.Paragraph p){
		final XWPFParagraph para = d.createParagraph();
		para.setSpacingAfter(PARAGRAPH_SPACING_AFTER);

		// Descendants trees carry alignment with spaces and newlines: keep the
		// original layout by rendering each line as a separate mono run.
		if(p.text().contains(StringUtils.LF))
			writeMonospaceBlock(para, p.text());
		else
			writeInlineMarkdown(para, p.text(), BODY_SIZE, false, false, null);
	}

	private static void writeMonospaceBlock(final XWPFParagraph p, final String text){
		final String[] lines = text.split(StringUtils.LF, -1);
		for(int i = 0; i < lines.length; i++){
			final XWPFRun run = p.createRun();
			run.setFontFamily("Consolas");
			run.setFontSize(BODY_SIZE - 1);
			run.setText(lines[i]);
			if(i < lines.length - 1)
				run.addBreak();
		}
	}


	/* ======================================================================
	 *                          Bullet lists
	 * ====================================================================== */

	private static void writeBulletList(final XWPFDocument d, final ReportSection.BulletList b){
		for(final String item : b.items()){
			final XWPFParagraph p = d.createParagraph();
			p.setStyle("ListBullet");
			p.setSpacingAfter(BULLET_SPACING_AFTER);
			writeInlineMarkdown(p, item, BODY_SIZE, false, false, null);
		}
	}


	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private static void writeTable(final XWPFDocument d, final ReportSection.Table t){
		if(t.headers().isEmpty())
			return;

		final int cols = t.headers().size();
		final XWPFTable table = d.createTable(t.rows().size() + 1, cols);
		table.setWidth("100%");
		applyTableBorders(table);

		// Header row: bold text on a light shaded background.
		final XWPFTableRow headerRow = table.getRow(0);
		for(int c = 0; c < cols; c++){
			final XWPFTableCell cell = headerRow.getCell(c);
			cell.setColor(HEADER_BG);
			final XWPFParagraph p = prepareCell(cell);
			writeInlineMarkdown(p, t.headers().get(c), BODY_SIZE, true, false, null);
		}

		// Body rows: rows shorter than the header are padded with empty cells.
		for(int r = 0; r < t.rows().size(); r++){
			final List<String> row = t.rows().get(r);
			final XWPFTableRow tableRow = table.getRow(r + 1);
			for(int c = 0; c < cols; c++){
				final String v = (c < row.size()? row.get(c): StringUtils.EMPTY);
				final XWPFTableCell cell = tableRow.getCell(c);
				final XWPFParagraph p = prepareCell(cell);
				writeInlineMarkdown(p, v, BODY_SIZE, false, false, null);
			}
		}

		// Small gap after the table.
		final XWPFParagraph spacer = d.createParagraph();
		spacer.setSpacingAfter(0);
		spacer.setSpacingBefore(0);
	}


	/**
	 * Prepares the default paragraph of a table cell: removes the built-in
	 * spacing and returns it so runs can be appended.
	 */
	private static XWPFParagraph prepareCell(final XWPFTableCell cell){
		final XWPFParagraph p = cell.getParagraphs().getFirst();
		p.setSpacingBefore(CELL_SPACING);
		p.setSpacingAfter(CELL_SPACING);
		return p;
	}

	private static void applyTableBorders(final XWPFTable table){
		table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
		table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
		table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
		table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "BFBFBF");
		table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "D9D9D9");
		table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "D9D9D9");
	}


	/* ======================================================================
	 *                          Rules / spacers
	 * ====================================================================== */

	private static void writeHorizontalRule(final XWPFDocument d){
		final XWPFParagraph p = d.createParagraph();
		p.setSpacingBefore(120);
		p.setSpacingAfter(120);
		p.setBorderBottom(Borders.SINGLE);
	}

	private static void writeSpacer(final XWPFDocument d){
		final XWPFParagraph p = d.createParagraph();
		p.setSpacingBefore(0);
		p.setSpacingAfter(0);
	}


	/* ======================================================================
	 *                          Images
	 * ====================================================================== */

	private static void writeImage(final XWPFDocument d, final ReportSection.Image img){
		final Path file = img.file();
		try(final InputStream is = Files.newInputStream(file)){
			final int[] size = scaledImageSize(file, IMAGE_MAX_PT, IMAGE_MAX_PT);
			final PictureType pictureType = pictureTypeFor(file);

			final XWPFParagraph p = d.createParagraph();
			p.setAlignment(ParagraphAlignment.CENTER);
			p.setSpacingAfter(IMAGE_SPACING_AFTER);

			p.createRun()
				.addPicture(is, pictureType, file.getFileName().toString(),
					(int)Units.toEMU(size[0]), (int)Units.toEMU(size[1]));

			if(img.caption() != null && !img.caption().isBlank()){
				final XWPFParagraph cap = d.createParagraph();
				cap.setAlignment(ParagraphAlignment.CENTER);
				cap.setSpacingAfter(HEADING_SPACING_AFTER);

				final XWPFRun capRun = cap.createRun();
				capRun.setText(img.caption());
				capRun.setItalic(true);
				capRun.setFontSize(10);
				capRun.setColor(MUTED_COLOR);
			}
		}
		catch(final Exception ex){
			final XWPFParagraph p = d.createParagraph();
			final XWPFRun run = p.createRun();
			run.setText("[image not available: " + file + "]");
			run.setItalic(true);
			run.setColor(MUTED_COLOR);
		}
	}


	/**
	 * Reads the image and returns a width/height pair (in points) that fits
	 * inside the given box while preserving the aspect ratio. Falls back to
	 * the box size when the image cannot be decoded.
	 */
	private static int[] scaledImageSize(final Path file, final int maxW, final int maxH){
		try(final InputStream is = Files.newInputStream(file)){
			final BufferedImage img = ImageIO.read(is);
			if(img == null)
				return new int[]{maxW, maxH};

			final double scale = Math.min(maxW / (double)img.getWidth(),
				maxH / (double)img.getHeight());
			return new int[]{
				(int)Math.round(img.getWidth() * scale),
				(int)Math.round(img.getHeight() * scale)
			};
		}
		catch(final IOException e){
			return new int[]{maxW, maxH};
		}
	}


	/** Maps a file extension to the POI picture-type constant. */
	private static PictureType pictureTypeFor(final Path file){
		final String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
		if(name.endsWith(".png"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_PNG);
		if(name.endsWith(".gif"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_GIF);
		if(name.endsWith(".bmp"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_BMP);
		if(name.endsWith(".tif") || name.endsWith(".tiff"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_TIFF);
		if(name.endsWith(".emf"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_EMF);
		if(name.endsWith(".wmf"))
			return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_WMF);
		return PictureType.findByOoxmlId(XWPFDocument.PICTURE_TYPE_JPEG);
	}


	/* ======================================================================
	 *                          Inline Markdown
	 * ====================================================================== */

	/**
	 * Parses a tiny subset of inline Markdown and appends styled runs to the
	 * given paragraph:
	 * <ul>
	 *   <li>{@code **bold**}</li>
	 *   <li>{@code *italic*}</li>
	 *   <li>{@code `code`} (rendered in a monospaced font one point smaller)</li>
	 * </ul>
	 *
	 * <p>The parser is state-machine based: markers toggle a state, and the
	 * accumulated text is flushed as a run whenever the state changes. Unbalanced
	 * markers are treated as literal characters.</p>
	 *
	 * <p>{@code baseBold} and {@code baseItalic} describe the styling that
	 * applies to every run in the paragraph (e.g. headings are bold). Inline
	 * markers are combined with the base style using OR, so {@code *(subject)*}
	 * inside a bold heading yields a bold+italic run, which matches Markdown
	 * semantics (Markdown has no "unbold" construct).</p>
	 *
	 * @param p          the target paragraph
	 * @param text       the text to parse
	 * @param baseSize   base font size in points
	 * @param baseBold   whether all runs are bold by default
	 * @param baseItalic whether all runs are italic by default
	 * @param baseColor  optional color (RRGGBB) applied to every run, or {@code null}
	 */
	private static void writeInlineMarkdown(final XWPFParagraph p, final String text,
		final int baseSize, final boolean baseBold, final boolean baseItalic,
		final String baseColor){
		if(text == null || text.isEmpty())
			return;

		final StringBuilder buf = new StringBuilder();
		boolean extraBold = false;
		boolean extraItalic = false;
		boolean isCode = false;

		for(int i = 0; i < text.length(); i++){
			final char c = text.charAt(i);

			if(c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*' && !isCode){
				flushRun(p, buf, baseBold || extraBold, baseItalic || extraItalic,
					isCode, baseSize, baseColor);
				extraBold = !extraBold;
				i++;
			}
			else if(c == '*' && !isCode){
				flushRun(p, buf, baseBold || extraBold, baseItalic || extraItalic,
					isCode, baseSize, baseColor);
				extraItalic = !extraItalic;
			}
			else if(c == '`' && !extraBold && !extraItalic){
				flushRun(p, buf, baseBold || extraBold, baseItalic || extraItalic,
					isCode, baseSize, baseColor);
				isCode = !isCode;
			}
			else
				buf.append(c);
		}
		flushRun(p, buf, baseBold || extraBold, baseItalic || extraItalic,
			isCode, baseSize, baseColor);
	}


	/** Emits the accumulated text as a single styled run. */
	private static void flushRun(final XWPFParagraph p, final StringBuilder buf,
		final boolean bold, final boolean italic, final boolean code,
		final int size, final String color){
		if(buf.length() == 0)
			return;

		final XWPFRun run = p.createRun();
		run.setText(buf.toString());
		run.setBold(bold);
		run.setItalic(italic);
		run.setFontSize(code? size - 1: size);
		if(code)
			run.setFontFamily("Consolas");
		if(color != null)
			run.setColor(color);

		buf.setLength(0);
	}

}
