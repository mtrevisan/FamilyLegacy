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
package io.github.mtrevisan.reports.docx;

import io.github.mtrevisan.reports.ReportDocument;
import io.github.mtrevisan.reports.ReportRenderer;
import io.github.mtrevisan.reports.ReportSection;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.PictureType;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.Borders;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFAbstractNum;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFNumbering;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTAbstractNum;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTDocDefaults;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLanguage;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTLvl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPrDefault;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STNumberFormat;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


/**
 * Renders a {@link ReportDocument} as a DOCX (Office Open XML) file via
 * Apache POI.
 *
 * <p>Layout and styling:</p>
 * <ul>
 *   <li>Title is centered, large and dark blue; subtitle is centered italic gray.</li>
 *   <li>Headings use the {@code Heading1}..{@code Heading4} styles, defined in the document (with their outline level), so
 *       they participate in the navigation pane and TOC, but their run-level
 *       formatting is overridden for a consistent look across Word and
 *       LibreOffice.</li>
 *   <li>Bullet lists use a bullet numbering defined in the document.</li>
 *   <li>Tables have a shaded, bold header row (repeated on each page) and thin gray borders.</li>
 *   <li>Inline Markdown ({@code **bold**}, {@code *italic*}, {@code `code`}) is
 *       converted into separate runs with the corresponding font styling; a marker that is never closed is kept as literal text.</li>
 *   <li>Images are centered, scaled to fit a maximum box while preserving the
 *       aspect ratio (never enlarged), and followed by an italic caption, that is also their alternative text.</li>
 *   <li>A table of contents (a Word field, filled with the headings) follows the title when there are at least two headings, and each page
 *       shows its number in the footer; the page is A4 with margins.</li>
 *   <li>Paragraphs containing newlines (the descendants tree) are rendered in a
 *       monospaced font with hard line breaks, preserving the original layout.</li>
 *   <li>The text of a right-to-left language is marked as such (paragraphs, runs and tables).</li>
 * </ul>
 */
public final class DocxReportRenderer implements ReportRenderer{

	private static final Set<String> EAST_ASIAN_LANGUAGES = Set.of("zh", "ja", "ko");

	/** Heading sizes, in points. */
	private static final int TITLE_SIZE = 24;
	private static final int SUBTITLE_SIZE = 12;
	private static final int CAPTION_SIZE = 10;
	private static final int H1_SIZE = 22;
	private static final int H2_SIZE = 16;
	private static final int H3_SIZE = 13;
	private static final int H4_SIZE = 12;
	private static final int BODY_SIZE = 11;

	/** Spacing values, in twips (1 pt = 20 twips). */
	private static final int TITLE_SPACING_AFTER = 80;  //  4 pt
	private static final int HEADING_SPACING_BEFORE = 240;  // 12 pt
	private static final int HEADING_SPACING_AFTER = 120;  //  6 pt
	private static final int PARAGRAPH_SPACING_AFTER = 100; //  5 pt
	private static final int BULLET_SPACING_AFTER = 40;  //  2 pt
	private static final int CELL_SPACING = 20;  //  1 pt
	private static final int IMAGE_SPACING_AFTER = 80;  //  4 pt
	private static final int RULE_SPACING = 120;  //  6 pt

	/** A4 page, in twips. */
	private static final int PAGE_WIDTH = 11_906;
	private static final int PAGE_HEIGHT = 16_838;
	/** Margins of the page (2.5 cm) and distance of the footer (1.25 cm), in twips. */
	private static final int PAGE_MARGIN = 1_418;
	private static final int FOOTER_DISTANCE = 709;
	/** Width of the text, in twips. */
	private static final int TEXT_WIDTH = PAGE_WIDTH - 2 * PAGE_MARGIN;
	/** Bounds of the weight (the length of the longest text) of a column of a table, so that none is too thin or too wide. */
	private static final int COLUMN_WEIGHT_MIN = 4;
	private static final int COLUMN_WEIGHT_MAX = 40;
	/** The report has a table of contents when it has at least this many headings. */
	private static final int TOC_MIN_HEADINGS = 2;
	private static final int TOC_INDENT = 220;
	private static final int TOC_SPACING_AFTER = 40;
	private static final String TOC_INSTRUCTION = "TOC \\o \"1-4\" \\u";
	/** Indentation of a bullet, in twips. */
	private static final int BULLET_INDENT = 720;
	private static final int BULLET_HANGING = 360;

	/** Maximum image box, in points. */
	private static final int IMAGE_MAX_PT = 400;
	/** An image pixel in points (96 dpi). */
	private static final double PIXEL_TO_POINT = 0.75;

	/** Header cell background color (light blue-gray). */
	private static final String HEADER_BG = "EEF2F7";

	/** Colors used for text. */
	private static final String TITLE_COLOR = "1F3864";
	private static final String H1H2_COLOR = "1F3864";
	private static final String H3H4_COLOR = "404040";
	private static final String MUTED_COLOR = "595959";

	/** A monospaced font available everywhere. */
	private static final String MONO_FONT = "Courier New";

	private static final String STYLE_TITLE = "Title";
	private static final String STYLE_HEADING = "Heading";
	private static final String STYLE_BULLET = "ListBullet";
	private static final String STYLE_TOC_HEADING = "TOCHeading";
	private static final String STYLE_TOC = "TOC";

	private static final Map<String, PictureType> PICTURE_TYPES = Map.ofEntries(
		Map.entry(".png", PictureType.PNG),
		Map.entry(".jpg", PictureType.JPEG),
		Map.entry(".jpeg", PictureType.JPEG),
		Map.entry(".jpe", PictureType.JPEG),
		Map.entry(".gif", PictureType.GIF),
		Map.entry(".bmp", PictureType.BMP),
		Map.entry(".tif", PictureType.TIFF),
		Map.entry(".tiff", PictureType.TIFF),
		Map.entry(".emf", PictureType.EMF),
		Map.entry(".wmf", PictureType.WMF)
	);


	@Override
	public String id(){
		return "docx";
	}

	@Override
	public String displayName(){
		return "Word";
	}

	@Override
	public String extension(){
		return ".docx";
	}


	@Override
	public void render(final ReportDocument doc, final Path target) throws IOException{
		//the document is built in memory, so that a failure does not leave a truncated file (or erase the previous one)
		final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try(final XWPFDocument d = new XWPFDocument()){
			new Builder(d, doc.isRightToLeft())
				.build(doc);
			d.write(buffer);
		}
		Files.write(target, buffer.toByteArray());
	}


	/** Picture type of a file, from its extension; {@code null} if it is not a format Word can hold. */
	static PictureType pictureTypeFor(final Path file){
		final String name = file.getFileName()
			.toString()
			.toLowerCase(Locale.ROOT);
		final int dot = name.lastIndexOf('.');
		return (dot >= 0? PICTURE_TYPES.get(name.substring(dot)): null);
	}


	/** The state of the document that is being written. */
	private static final class Builder{

		private final XWPFDocument d;
		private final boolean rightToLeft;
		private BigInteger bulletNumId;
		/** A page break asked and not yet applied: the next paragraph starts a new page (no empty paragraph is needed). */
		private boolean pendingPageBreak;


		private Builder(final XWPFDocument d, final boolean rightToLeft){
			this.d = d;
			this.rightToLeft = rightToLeft;
		}


		private void build(final ReportDocument doc){
			setPageSize();
			final XWPFStyles styles = d.createStyles();
			applyLanguage(styles, doc.language());
			defineStyles(styles);
			defineBulletNumbering();
			writePageNumbers();
			d.getProperties()
				.getCoreProperties()
				.setTitle(StringUtils.defaultString(doc.title()));

			writeTitle(doc);
			writeTableOfContents(doc);

			for(final ReportSection s : doc.sections())
				writeSection(s);
		}

		private void setPageSize(){
			final CTSectPr section = (d.getDocument().getBody().isSetSectPr()? d.getDocument().getBody().getSectPr(): d.getDocument().getBody().addNewSectPr());
			final CTPageSz size = (section.isSetPgSz()? section.getPgSz(): section.addNewPgSz());
			size.setW(BigInteger.valueOf(PAGE_WIDTH));
			size.setH(BigInteger.valueOf(PAGE_HEIGHT));

			final CTPageMar margins = (section.isSetPgMar()? section.getPgMar(): section.addNewPgMar());
			final BigInteger margin = BigInteger.valueOf(PAGE_MARGIN);
			margins.setTop(margin);
			margins.setBottom(margin);
			margins.setLeft(margin);
			margins.setRight(margin);
			margins.setHeader(BigInteger.valueOf(FOOTER_DISTANCE));
			margins.setFooter(BigInteger.valueOf(FOOTER_DISTANCE));
			margins.setGutter(BigInteger.ZERO);
		}

		/** The number of the page, centered at the bottom of each page. */
		private void writePageNumbers(){
			final XWPFFooter footer = d.createFooter(HeaderFooterType.DEFAULT);
			final XWPFParagraph p = footer.createParagraph();
			p.setAlignment(ParagraphAlignment.CENTER);
			markRightToLeft(p);

			final CTSimpleField field = p.getCTP().addNewFldSimple();
			field.setInstr("PAGE");
			//what is shown until the field is calculated
			field.addNewR()
				.addNewT()
				.setStringValue("1");
		}


		/* ==================================================================
		 *                          Language
		 * ================================================================== */

		/**
		 * Sets the language of the text of the document, as the default of the styles, so that Word spells it and reads it (screen readers)
		 * in that language: the language of the text, the one of the right-to-left text (bidi) and the one of the East Asian text.
		 */
		private void applyLanguage(final XWPFStyles xwpfStyles, final String language){
			final CTStyles styles = xwpfStyles.getCtStyles();
			final CTDocDefaults defaults = (styles.isSetDocDefaults()? styles.getDocDefaults(): styles.addNewDocDefaults());
			final CTRPrDefault runDefaults = (defaults.isSetRPrDefault()? defaults.getRPrDefault(): defaults.addNewRPrDefault());
			final CTRPr run = (runDefaults.isSetRPr()? runDefaults.getRPr(): runDefaults.addNewRPr());
			final CTLanguage lang = (run.sizeOfLangArray() > 0? run.getLangArray(0): run.addNewLang());

			lang.setVal(language);
			if(rightToLeft)
				lang.setBidi(language);
			if(EAST_ASIAN_LANGUAGES.contains(Locale.forLanguageTag(language).getLanguage()))
				lang.setEastAsia(language);
		}


		/* ==================================================================
		 *                          Styles and numbering
		 * ================================================================== */

		/**
		 * A new document holds no style: the ones that are used (title, headings, bullet) are defined here, or Word would ignore them (no
		 * navigation pane, no bullets).
		 */
		private static void defineStyles(final XWPFStyles styles){
			addStyle(styles, STYLE_TITLE, "Title", -1, TITLE_SIZE, true, TITLE_COLOR);
			for(int level = 1; level <= 4; level ++)
				addStyle(styles, STYLE_HEADING + level, "heading " + level, level - 1, headingSize(level), true, headingColor(level));
			addStyle(styles, STYLE_BULLET, "List Bullet", -1, BODY_SIZE, false, null);
			addStyle(styles, STYLE_TOC_HEADING, "TOC Heading", -1, H2_SIZE, true, H1H2_COLOR);
			for(int level = 1; level <= 4; level ++){
				addStyle(styles, STYLE_TOC + level, "toc " + level, -1, BODY_SIZE, false, null);
				final CTStyle style = styles.getStyle(STYLE_TOC + level).getCTStyle();
				final CTPPrGeneral paragraph = (style.isSetPPr()? style.getPPr(): style.addNewPPr());
				paragraph.addNewInd()
					.setLeft(BigInteger.valueOf((long)(level - 1) * TOC_INDENT));
			}
		}

		private static void addStyle(final XWPFStyles styles, final String id, final String name, final int outlineLevel, final int size,
				final boolean bold, final String color){
			final CTStyle style = CTStyle.Factory.newInstance();
			style.setType(STStyleType.PARAGRAPH);
			style.setStyleId(id);
			style.addNewName()
				.setVal(name);
			style.addNewQFormat();
			if(outlineLevel >= 0){
				final CTPPrGeneral paragraph = style.addNewPPr();
				paragraph.addNewKeepNext();
				paragraph.addNewOutlineLvl()
					.setVal(BigInteger.valueOf(outlineLevel));
			}
			final CTRPr run = style.addNewRPr();
			if(bold)
				run.addNewB();
			run.addNewSz()
				.setVal(BigInteger.valueOf(size * 2L));
			if(color != null)
				run.addNewColor()
					.setVal(color);
			styles.addStyle(new XWPFStyle(style, styles));
		}

		private void defineBulletNumbering(){
			final CTAbstractNum abstractNum = CTAbstractNum.Factory.newInstance();
			abstractNum.setAbstractNumId(BigInteger.ZERO);
			final CTLvl level = abstractNum.addNewLvl();
			level.setIlvl(BigInteger.ZERO);
			level.addNewStart()
				.setVal(BigInteger.ONE);
			level.addNewNumFmt()
				.setVal(STNumberFormat.BULLET);
			level.addNewLvlText()
				.setVal("•");
			final var indent = level.addNewPPr()
				.addNewInd();
			indent.setLeft(BigInteger.valueOf(BULLET_INDENT));
			indent.setHanging(BigInteger.valueOf(BULLET_HANGING));

			final XWPFNumbering numbering = d.createNumbering();
			final BigInteger abstractId = numbering.addAbstractNum(new XWPFAbstractNum(abstractNum));
			bulletNumId = numbering.addNum(abstractId);
		}


		/* ==================================================================
		 *                          Paragraph helpers
		 * ================================================================== */

		/** Creates a paragraph, marked as right-to-left if needed and starting a new page if a page break is pending. */
		private XWPFParagraph paragraph(){
			final XWPFParagraph p = d.createParagraph();
			if(pendingPageBreak){
				p.setPageBreak(true);
				pendingPageBreak = false;
			}
			markRightToLeft(p);
			return p;
		}

		private void markRightToLeft(final XWPFParagraph p){
			if(!rightToLeft)
				return;

			final CTPPr properties = (p.getCTP().isSetPPr()? p.getCTP().getPPr(): p.getCTP().addNewPPr());
			if(!properties.isSetBidi())
				properties.addNewBidi();
		}

		/** Appends a run with the given look; for a right-to-left text the complex-script look is set too, since Word uses that one. */
		private XWPFRun run(final XWPFParagraph p, final String text, final int size, final boolean bold, final boolean italic,
				final String color, final boolean mono){
			final XWPFRun run = p.createRun();
			run.setText(text);
			run.setBold(bold);
			run.setItalic(italic);
			run.setFontSize(size);
			if(mono)
				run.setFontFamily(MONO_FONT);
			if(color != null)
				run.setColor(color);

			if(rightToLeft){
				final CTRPr properties = (run.getCTR().isSetRPr()? run.getCTR().getRPr(): run.getCTR().addNewRPr());
				properties.addNewRtl();
				properties.addNewSzCs()
					.setVal(BigInteger.valueOf(size * 2L));
				if(bold)
					properties.addNewBCs();
				if(italic)
					properties.addNewICs();
			}
			return run;
		}


		/* ==================================================================
		 *                          Title
		 * ================================================================== */

		private void writeTitle(final ReportDocument doc){
			final XWPFParagraph title = paragraph();
			title.setStyle(STYLE_TITLE);
			title.setAlignment(ParagraphAlignment.CENTER);
			title.setSpacingAfter(TITLE_SPACING_AFTER);
			run(title, StringUtils.defaultString(doc.title()), TITLE_SIZE, true, false, TITLE_COLOR, false);

			if(doc.subtitle() != null && !doc.subtitle().isBlank()){
				final XWPFParagraph sub = paragraph();
				sub.setAlignment(ParagraphAlignment.CENTER);
				sub.setSpacingAfter(HEADING_SPACING_BEFORE);
				run(sub, doc.subtitle(), SUBTITLE_SIZE, false, true, MUTED_COLOR, false);
			}
		}


		/* ==================================================================
		 *                          Section dispatch
		 * ================================================================== */

		/**
		 * The table of contents, as a Word field over the headings (right-click, update: it gets the page numbers), already filled with
		 * the headings so that it reads even before the field is updated, and in any reader.
		 */
		private void writeTableOfContents(final ReportDocument doc){
			final List<ReportSection.Heading> headings = doc.sections().stream()
				.filter(ReportSection.Heading.class::isInstance)
				.map(ReportSection.Heading.class::cast)
				.toList();
			if(headings.size() < TOC_MIN_HEADINGS)
				return;

			final XWPFParagraph title = paragraph();
			title.setStyle(STYLE_TOC_HEADING);
			title.setSpacingBefore(HEADING_SPACING_BEFORE);
			title.setSpacingAfter(HEADING_SPACING_AFTER);
			run(title, doc.contentsTitle(), H2_SIZE, true, false, H1H2_COLOR, false);

			for(int i = 0; i < headings.size(); i ++){
				final ReportSection.Heading heading = headings.get(i);
				final XWPFParagraph entry = paragraph();
				entry.setStyle(STYLE_TOC + Math.clamp(heading.level(), 1, 4));
				entry.setSpacingAfter(TOC_SPACING_AFTER);
				if(i == 0){
					field(entry, STFldCharType.BEGIN);
					entry.createRun()
						.getCTR()
						.addNewInstrText()
						.setStringValue(TOC_INSTRUCTION);
					field(entry, STFldCharType.SEPARATE);
				}
				run(entry, plainText(heading.text()), BODY_SIZE, false, false, null, false);
				if(i == headings.size() - 1)
					field(entry, STFldCharType.END);
			}
		}

		private static void field(final XWPFParagraph p, final STFldCharType.Enum type){
			p.createRun()
				.getCTR()
				.addNewFldChar()
				.setFldCharType(type);
		}

		/** The text without the Markdown markers. */
		private static String plainText(final String text){
			final StringBuilder plain = new StringBuilder();
			for(final Span span : parse(text))
				plain.append(span.text());
			return plain.toString();
		}

		/** Splits the text in spans; a marker that is opened and never closed is kept as literal text (each pass makes one more literal, so it ends). */
		private static List<Span> parse(final String text){
			final Set<Integer> literals = new HashSet<>();
			final List<Span> spans = new ArrayList<>();
			int unmatched;
			while((unmatched = scan(text, literals, spans)) >= 0)
				literals.add(unmatched);
			return spans;
		}

		private void writeSection(final ReportSection s){
			//sealed: the compiler tells if a new kind of section is not handled
			switch(s){
				case ReportSection.Heading h -> writeHeading(h);
				case ReportSection.Paragraph p -> writeParagraph(p);
				case ReportSection.Spacer ignored -> writeSpacer();
				case ReportSection.BulletList b -> writeBulletList(b);
				case ReportSection.Table t -> writeTable(t);
				case ReportSection.HorizontalRule ignored -> writeHorizontalRule();
				case ReportSection.PageBreak ignored -> pendingPageBreak = true;
				case ReportSection.Image img -> writeImage(img);
			}
		}


		/* ==================================================================
		 *                          Headings
		 * ================================================================== */

		private void writeHeading(final ReportSection.Heading h){
			final int level = Math.clamp(h.level(), 1, 4);

			final XWPFParagraph p = paragraph();
			// Defined styles give Word the document structure (navigation, TOC).
			p.setStyle(STYLE_HEADING + level);
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


		/* ==================================================================
		 *                          Paragraphs
		 * ================================================================== */

		private void writeParagraph(final ReportSection.Paragraph p){
			final XWPFParagraph para = paragraph();
			para.setSpacingAfter(PARAGRAPH_SPACING_AFTER);
			writeText(para, p.text(), false);
		}

		/**
		 * Descendants trees carry alignment with spaces and newlines: keep the original layout by rendering each line as a separate mono
		 * run; any other text is inline Markdown.
		 */
		private void writeText(final XWPFParagraph p, final String text, final boolean bold){
			if(text.contains(StringUtils.LF))
				writeMonospaceBlock(p, text);
			else
				writeInlineMarkdown(p, text, BODY_SIZE, bold, false, null);
		}

		private void writeMonospaceBlock(final XWPFParagraph p, final String text){
			final String[] lines = StringUtils.splitPreserveAllTokens(text, '\n');
			for(int i = 0; i < lines.length; i ++){
				final XWPFRun run = run(p, lines[i], BODY_SIZE - 1, false, false, null, true);
				if(i < lines.length - 1)
					run.addBreak();
			}
		}


		/* ==================================================================
		 *                          Bullet lists
		 * ================================================================== */

		private void writeBulletList(final ReportSection.BulletList b){
			for(final String item : b.items()){
				final XWPFParagraph p = paragraph();
				p.setStyle(STYLE_BULLET);
				p.setNumID(bulletNumId);
				p.setSpacingAfter(BULLET_SPACING_AFTER);
				writeInlineMarkdown(p, item, BODY_SIZE, false, false, null);
			}
		}


		/* ==================================================================
		 *                          Tables
		 * ================================================================== */

		private void writeTable(final ReportSection.Table t){
			final int cols = t.columns();
			if(cols == 0)
				return;

			//a table cannot carry a page break: an empty paragraph does
			if(pendingPageBreak)
				writeSpacer();

			final boolean hasHeader = !t.headers().isEmpty();
			final int offset = (hasHeader? 1: 0);
			final XWPFTable table = d.createTable(t.rows().size() + offset, cols);
			table.setWidth("100%");
			applyTableBorders(table);
			applyColumnWidths(table, t, cols);
			if(rightToLeft){
				final CTTblPr properties = (table.getCTTbl().getTblPr() != null? table.getCTTbl().getTblPr(): table.getCTTbl().addNewTblPr());
				properties.addNewBidiVisual();
			}

			// Header row: bold text on a light shaded background, repeated on each page.
			if(hasHeader){
				final XWPFTableRow headerRow = table.getRow(0);
				headerRow.setRepeatHeader(true);
				headerRow.setCantSplitRow(true);
				for(int c = 0; c < cols; c ++){
					final XWPFTableCell cell = headerRow.getCell(c);
					cell.setColor(HEADER_BG);
					writeText(prepareCell(cell), cellText(t.headers(), c), true);
				}
			}

			// Body rows: rows shorter than the widest one are padded with empty cells.
			for(int r = 0; r < t.rows().size(); r ++){
				final List<String> row = t.rows()
					.get(r);
				final XWPFTableRow tableRow = table.getRow(r + offset);
				tableRow.setCantSplitRow(true);
				for(int c = 0; c < cols; c ++)
					writeText(prepareCell(tableRow.getCell(c)), cellText(row, c), false);
			}

			// Small gap after the table.
			final XWPFParagraph spacer = paragraph();
			spacer.setSpacingAfter(0);
			spacer.setSpacingBefore(0);
		}

		/**
		 * The columns share the width of the text in proportion to the length of their longest text (within bounds), instead of being all
		 * of the same width.
		 */
		private static void applyColumnWidths(final XWPFTable table, final ReportSection.Table t, final int cols){
			final int[] weights = new int[cols];
			for(int c = 0; c < cols; c ++){
				int longest = longestLine(cellText(t.headers(), c));
				for(final List<String> row : t.rows())
					longest = Math.max(longest, longestLine(cellText(row, c)));
				weights[c] = Math.clamp(longest, COLUMN_WEIGHT_MIN, COLUMN_WEIGHT_MAX);
			}
			int total = 0;
			for(final int weight : weights)
				total += weight;

			final CTTblGrid grid = table.getCTTbl().getTblGrid();
			for(int c = 0; c < cols; c ++){
				final int width = (int)((long)TEXT_WIDTH * weights[c] / total);
				if(grid != null && grid.sizeOfGridColArray() == cols)
					grid.getGridColArray(c)
						.setW(BigInteger.valueOf(width));
				for(final XWPFTableRow row : table.getRows())
					row.getCell(c)
						.setWidth(Integer.toString(width));
			}
		}

		private static int longestLine(final String text){
			int longest = 0;
			for(final String line : StringUtils.splitPreserveAllTokens(text, '\n'))
				longest = Math.max(longest, line.length());
			return longest;
		}

		private static String cellText(final List<String> row, final int column){
			return (column < row.size()? row.get(column): StringUtils.EMPTY);
		}

		/**
		 * Prepares the default paragraph of a table cell: removes the built-in
		 * spacing and returns it so runs can be appended.
		 */
		private XWPFParagraph prepareCell(final XWPFTableCell cell){
			final XWPFParagraph p = cell.getParagraphs()
				.getFirst();
			p.setSpacingBefore(CELL_SPACING);
			p.setSpacingAfter(CELL_SPACING);
			markRightToLeft(p);
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


		/* ==================================================================
		 *                          Rules / spacers
		 * ================================================================== */

		private void writeHorizontalRule(){
			final XWPFParagraph p = paragraph();
			p.setSpacingBefore(RULE_SPACING);
			p.setSpacingAfter(RULE_SPACING);
			p.setBorderBottom(Borders.SINGLE);
		}

		private void writeSpacer(){
			final XWPFParagraph p = paragraph();
			p.setSpacingBefore(0);
			p.setSpacingAfter(0);
		}


		/* ==================================================================
		 *                          Images
		 * ================================================================== */

		private void writeImage(final ReportSection.Image img){
			final Path file = img.file();
			final XWPFParagraph p = paragraph();
			p.setAlignment(ParagraphAlignment.CENTER);
			p.setSpacingAfter(IMAGE_SPACING_AFTER);
			if(!img.caption().isBlank()){
				//the caption stays with its image
				final CTPPr properties = (p.getCTP().isSetPPr()? p.getCTP().getPPr(): p.getCTP().addNewPPr());
				properties.addNewKeepNext();
			}

			try{
				//an unknown format is not guessed: written as another one, it would make the file unreadable
				final PictureType pictureType = pictureTypeFor(file);
				if(pictureType == null)
					throw new IOException("Unsupported image format");

				final byte[] data = Files.readAllBytes(file);
				final int[] size = scaledImageSize(data, IMAGE_MAX_PT, IMAGE_MAX_PT);

				final XWPFRun run = p.createRun();
				run.addPicture(new ByteArrayInputStream(data), pictureType, file.getFileName().toString(),
					(int)Units.toEMU(size[0]), (int)Units.toEMU(size[1]));
				if(!img.caption().isBlank())
					run.getCTR().getDrawingArray(0).getInlineArray(0).getDocPr()
						.setDescr(img.caption());
			}
			catch(final IOException | InvalidFormatException | RuntimeException ex){
				//only the name of the file: the path of the user is not for the reader of the report
				run(p, "[image not available: " + file.getFileName() + "]", BODY_SIZE, false, true, MUTED_COLOR, false);
				return;
			}

			if(!img.caption().isBlank()){
				final XWPFParagraph cap = paragraph();
				cap.setAlignment(ParagraphAlignment.CENTER);
				cap.setSpacingAfter(HEADING_SPACING_AFTER);
				run(cap, img.caption(), CAPTION_SIZE, false, true, MUTED_COLOR, false);
			}
		}

		/**
		 * Reads the image and returns a width/height pair (in points) that fits inside the given box while preserving the aspect ratio;
		 * an image smaller than the box is not enlarged. Falls back to the box size when the image cannot be decoded.
		 */
		private static int[] scaledImageSize(final byte[] data, final int maxW, final int maxH){
			try{
				final BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
				if(img == null)
					return new int[]{maxW, maxH};

				final double width = img.getWidth() * PIXEL_TO_POINT;
				final double height = img.getHeight() * PIXEL_TO_POINT;
				final double scale = Math.min(1., Math.min(maxW / width, maxH / height));
				return new int[]{
					Math.max(1, (int)Math.round(width * scale)),
					Math.max(1, (int)Math.round(height * scale))
				};
			}
			catch(final IOException e){
				return new int[]{maxW, maxH};
			}
		}


		/* ==================================================================
		 *                          Inline Markdown
		 * ================================================================== */

		/** A piece of text with its inline look. */
		private record Span(String text, boolean bold, boolean italic, boolean code){}

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
		 * accumulated text is flushed as a run whenever the state changes. A marker that is opened and never closed is kept as literal
		 * text (the text is scanned again without it).</p>
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
		private void writeInlineMarkdown(final XWPFParagraph p, final String text, final int baseSize, final boolean baseBold,
				final boolean baseItalic, final String baseColor){
			if(StringUtils.isEmpty(text))
				return;

			for(final Span span : parse(text))
				run(p, span.text(), (span.code()? baseSize - 1: baseSize), baseBold || span.bold(), baseItalic || span.italic(), baseColor,
					span.code());
		}

		/**
		 * Splits the text in spans.
		 *
		 * @param text	The text.
		 * @param literals	The positions of the markers to be taken as text.
		 * @param spans	The output, cleared first.
		 * @return	The position of a marker that was opened and never closed, {@code -1} if there is none.
		 */
		private static int scan(final String text, final Set<Integer> literals, final List<Span> spans){
			spans.clear();
			final StringBuilder buf = new StringBuilder();
			boolean bold = false;
			boolean italic = false;
			boolean code = false;
			int boldAt = -1;
			int italicAt = -1;
			int codeAt = -1;

			for(int i = 0; i < text.length(); i ++){
				final char c = text.charAt(i);

				if(c == '*' && i + 1 < text.length() && text.charAt(i + 1) == '*' && !code){
					if(literals.contains(i))
						buf.append("**");
					else{
						flush(spans, buf, bold, italic, code);
						bold = !bold;
						boldAt = (bold? i: -1);
					}
					i ++;
				}
				else if(c == '*' && !code){
					if(literals.contains(i))
						buf.append(c);
					else{
						flush(spans, buf, bold, italic, code);
						italic = !italic;
						italicAt = (italic? i: -1);
					}
				}
				else if(c == '`' && !bold && !italic){
					if(literals.contains(i))
						buf.append(c);
					else{
						flush(spans, buf, bold, italic, code);
						code = !code;
						codeAt = (code? i: -1);
					}
				}
				else
					buf.append(c);
			}
			flush(spans, buf, bold, italic, code);

			return (boldAt >= 0? boldAt: (italicAt >= 0? italicAt: codeAt));
		}

		private static void flush(final List<Span> spans, final StringBuilder buf, final boolean bold, final boolean italic,
				final boolean code){
			if(buf.isEmpty())
				return;

			spans.add(new Span(buf.toString(), bold, italic, code));
			buf.setLength(0);
		}

	}

}
