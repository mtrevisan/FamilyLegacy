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

import org.openpdf.text.Chunk;
import org.openpdf.text.Image;
import org.openpdf.text.pdf.PdfTemplate;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Writes the scripts that the PDF writer cannot shape (Devanagari, Bengali): the letters of these scripts change shape and place by
 * what is around them (the vowel signs that stand before the consonant, the conjuncts), and OpenPDF writes one glyph for each character.
 * <p>
 * The shaping is done by the text layout of Java (HarfBuzz), with the Noto font of the {@code fonts} directory, and each word is drawn in the page as
 * an outline (a vector drawing, as sharp as text at any zoom), in an inline image the size of the word. The word cannot be selected or
 * searched in the PDF.
 */
final class IndicShaper{

	/** The scripts that are shaped, with the range of their characters and the files of the font. */
	enum Script{

		DEVANAGARI(0x0900, 0x097F, "NotoSansDevanagari"),
		BENGALI(0x0980, 0x09FF, "NotoSansBengali");


		private final int from;
		private final int to;
		private final String file;


		Script(final int from, final int to, final String file){
			this.from = from;
			this.to = to;
			this.file = file;
		}

	}


	/** @param text	The characters.
	 * @param script	The script to shape ({@code null} for the text that the fonts write by themselves). */
	record Run(String text, Script script){}


	private static final int ZERO_WIDTH_NON_JOINER = 0x200C;
	private static final int ZERO_WIDTH_JOINER = 0x200D;
	private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);
	private static final Map<String, Font> FONTS = new ConcurrentHashMap<>();


	private IndicShaper(){}


	/** @return	The script of the character, {@code null} if it is not one of the shaped scripts. */
	static Script scriptOf(final int codePoint){
		for(final Script script : Script.values())
			if(codePoint >= script.from && codePoint <= script.to)
				return script;
		return null;
	}

	static boolean containsShaped(final String text){
		return text.codePoints()
			.anyMatch(cp -> scriptOf(cp) != null);
	}

	/** @return	The text split in the words of a shaped script and the text between them. */
	static List<Run> split(final String text){
		final List<Run> runs = new ArrayList<>();
		final StringBuilder current = new StringBuilder();
		Script currentScript = null;
		final int[] codePoints = text.codePoints().toArray();
		for(int i = 0; i < codePoints.length; i ++){
			final int cp = codePoints[i];
			Script script = scriptOf(cp);
			//the joiners belong to the word they are in
			if(script == null && (cp == ZERO_WIDTH_JOINER || cp == ZERO_WIDTH_NON_JOINER) && currentScript != null && i + 1 < codePoints.length
					&& scriptOf(codePoints[i + 1]) == currentScript)
				script = currentScript;

			if(script != currentScript && !current.isEmpty()){
				runs.add(new Run(current.toString(), currentScript));
				current.setLength(0);
			}
			currentScript = script;
			current.appendCodePoint(cp);
		}
		if(!current.isEmpty())
			runs.add(new Run(current.toString(), currentScript));
		return runs;
	}

	/**
	 * @param run	The word.
	 * @param script	The script of the word.
	 * @param size	The size, in points.
	 * @param bold	Whether it is bold.
	 * @param color	The color ({@code null} for black).
	 * @param writer	The writer of the document.
	 * @return	The word as a chunk with its drawing, {@code null} if the font of the program cannot be read.
	 */
	static Chunk chunk(final String run, final Script script, final float size, final boolean bold, final Color color, final PdfWriter writer){
		final Font base = font(script, bold);
		if(base == null || writer == null)
			return null;

		final TextLayout layout = new TextLayout(run, base.deriveFont(size), CONTEXT);
		final Shape outline = layout.getOutline(null);
		final Rectangle2D bounds = outline.getBounds2D();
		//what sticks out of the advance of the word (the vowel signs written before the consonant) is inside the drawing
		final float left = (float)Math.min(0, bounds.getMinX());
		final float right = (float)Math.max(layout.getAdvance(), bounds.getMaxX());
		final float above = Math.max(layout.getAscent(), (float)-bounds.getMinY());
		final float below = Math.max(layout.getDescent(), (float)bounds.getMaxY());

		final PdfTemplate template = writer.getDirectContent()
			.createTemplate(right - left, above + below);
		template.setColorFill(color != null? color: Color.BLACK);
		//the y-axis of the page goes up, the one of the text layout goes down; the origin of the word is on its baseline
		final PathIterator path = outline.getPathIterator(new AffineTransform(1, 0, 0, -1, -left, below));
		final float[] c = new float[6];
		float x = 0;
		float y = 0;
		while(!path.isDone()){
			switch(path.currentSegment(c)){
				case PathIterator.SEG_MOVETO -> {
					template.moveTo(c[0], c[1]);
					x = c[0];
					y = c[1];
				}
				case PathIterator.SEG_LINETO -> {
					template.lineTo(c[0], c[1]);
					x = c[0];
					y = c[1];
				}
				case PathIterator.SEG_QUADTO -> {
					//a quadratic curve as a cubic one
					template.curveTo(x + 2f / 3 * (c[0] - x), y + 2f / 3 * (c[1] - y),
						c[2] + 2f / 3 * (c[0] - c[2]), c[3] + 2f / 3 * (c[1] - c[3]), c[2], c[3]);
					x = c[2];
					y = c[3];
				}
				case PathIterator.SEG_CUBICTO -> {
					template.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]);
					x = c[4];
					y = c[5];
				}
				default -> template.closePath();
			}
			path.next();
		}
		template.fill();

		try{
			return new Chunk(Image.getInstance(template), left, -below, false);
		}
		catch(final Exception e){
			return null;
		}
	}

	/** Forgets the fonts that were read (a test changes the directory). */
	static void forget(){
		FONTS.clear();
	}

	/** @return	The font of the {@code fonts} directory, {@code null} if it is not there (it is optional: the words are then left to the fonts). */
	private static Font font(final Script script, final boolean bold){
		final String name = script.file + (bold? "-Bold": "-Regular");
		final Font cached = FONTS.get(name);
		if(cached != null)
			return cached;

		final Path directory = PdfFonts.fontsDirectory();
		if(directory == null)
			return null;

		final Path file = directory.resolve(name + ".ttf");
		if(!Files.isRegularFile(file))
			return null;

		try{
			final Font font = Font.createFont(Font.TRUETYPE_FONT, file.toFile());
			FONTS.put(name, font);
			return font;
		}
		catch(final IOException | FontFormatException | RuntimeException e){
			return null;
		}
	}

}
