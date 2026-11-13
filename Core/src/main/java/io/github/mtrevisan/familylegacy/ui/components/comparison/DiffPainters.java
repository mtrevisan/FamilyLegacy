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
package io.github.mtrevisan.familylegacy.ui.components.comparison;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.View;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;


/**
 * How a comparison of two records is painted, the same for every window that compares records: each line has the colour of what it
 * is (see {@link DiffKind}) over its whole width, and the lines that cite a record say, after their text, the name of the record.
 */
public final class DiffPainters{

	public static final Color COLOR_FROM_SOURCE = new Color(0xDCEBFA);
	public static final Color COLOR_FROM_TARGET = new Color(0xDFF2DF);
	public static final Color COLOR_CONFLICT = new Color(0xF9D6D5);
	/** A different record where the other side has another one: stronger than a different text. */
	public static final Color COLOR_REFERENCE = new Color(0xF0A4A2);
	public static final Color COLOR_EDITED = new Color(0xFFF3C4);
	public static final Color COLOR_ABSENT = new Color(0xF2F2F2);
	public static final Color COLOR_MUTED = new Color(0x757575);

	/** The longest name written after a reference. */
	private static final int MAX_NAME_LENGTH = 48;


	private DiffPainters(){}


	/** Paints the whole width of the line of a text, not only the characters, as the band of a line of a comparison. */
	private static final class BandPainter extends DefaultHighlighter.DefaultHighlightPainter{

		BandPainter(final Color color){
			super(color);
		}

		@Override
		public Shape paintLayer(final Graphics g, final int offset0, final int offset1, final Shape bounds, final JTextComponent component,
				final View view){
			try{
				final Rectangle line = component.modelToView2D(offset0).getBounds();
				g.setColor(getColor());
				g.fillRect(0, line.y, component.getWidth(), line.height);
				return new Rectangle(0, line.y, component.getWidth(), line.height);
			}
			catch(final BadLocationException ignored){
				return null;
			}
		}

	}

	/** Writes, after the text of a line, a label in a muted colour: the name of the record the line cites. */
	private static final class LabelPainter extends DefaultHighlighter.DefaultHighlightPainter{

		private final String label;


		LabelPainter(final String label){
			super(null);

			this.label = label;
		}

		@Override
		public Shape paintLayer(final Graphics g, final int offset0, final int offset1, final Shape bounds,
				final JTextComponent component, final View view){
			try{
				//the position of the line feed is where the text of the line ends
				final Rectangle2D end = component.modelToView2D(Math.max(offset0, offset1 - 1));
				final Font font = component.getFont().deriveFont(Font.ITALIC);
				g.setFont(font);
				g.setColor(COLOR_MUTED);
				final int x = (int)end.getMaxX() + 16;
				final int y = (int)end.getY() + g.getFontMetrics().getAscent();
				g.drawString(label, x, y);
				return new Rectangle(x, (int)end.getY(), g.getFontMetrics().stringWidth(label), (int)end.getHeight());
			}
			catch(final BadLocationException ignored){
				return null;
			}
		}

	}

	public static Color colorOf(final DiffKind kind){
		return switch(kind){
			case FROM_SOURCE -> COLOR_FROM_SOURCE;
			case FROM_TARGET -> COLOR_FROM_TARGET;
			case CONFLICT -> COLOR_CONFLICT;
			case REFERENCE -> COLOR_REFERENCE;
			case EDITED -> COLOR_EDITED;
			case FILLER -> COLOR_ABSENT;
			case SAME -> Color.WHITE;
		};
	}

	/**
	 * Paints a text: each line has the colour of its mark, the lines without one stay as they are, and the lines that cite a record say,
	 * after their text, the name of the record.
	 * <p>
	 * The names are added before the colours: a text area paints its highlights from the last added to the first, so the colour of a
	 * line would otherwise cover the name written on it.
	 *
	 * @param nameOfLine	The name of the record that a line cites, {@code null} if it cites none.
	 */
	public static void paint(final JTextArea area, final DiffKind[] marks, final Function<String, String> nameOfLine){
		area.getHighlighter().removeAllHighlights();
		addNames(area, nameOfLine);
		addColours(area, marks);
	}

	/** Colours each line of the text with its mark, and leaves the lines without one as they are. */
	public static void paintLines(final JTextArea area, final DiffKind[] marks){
		area.getHighlighter().removeAllHighlights();
		addColours(area, marks);
	}

	private static void addColours(final JTextArea area, final DiffKind[] marks){
		final Highlighter highlighter = area.getHighlighter();
		try{
			for(int line = 0; line < marks.length && line < area.getLineCount(); line ++)
				if(marks[line] != null){
					final int start = area.getLineStartOffset(line);
					final int end = area.getLineEndOffset(line);
					if(end > start)
						highlighter.addHighlight(start, end, new BandPainter(colorOf(marks[line])));
				}
		}
		catch(final BadLocationException ignored){
			//a line that is gone: the next change of the text paints them again
		}
	}

	/** The record of the file that a line cites ({@code place P3}), or {@code null}. */
	public static FLEFRecord referencedBy(final String line, final FLEFModel model){
		if(model == null)
			return null;

		final String stripped = line.strip();
		final int space = stripped.indexOf(' ');
		if(space <= 0 || RecordAlignment.isDelimiter(stripped))
			return null;

		final FLEFRecord referenced = model.getRecordById(stripped.substring(space + 1).strip());
		return (referenced != null && stripped.substring(0, space).equals(referenced.getTag())? referenced: null);
	}

	/**
	 * The name of the record that a line cites ({@code place P3}), or {@code null} if the line is not a reference. A reference is an id:
	 * the name is what tells which record it is.
	 *
	 * @param displayName	How a record is named, {@code null} if it has no name.
	 */
	public static String nameOfReference(final String line, final FLEFModel model, final Function<FLEFRecord, String> displayName){
		final FLEFRecord referenced = referencedBy(line, model);
		if(referenced == null)
			return null;

		final String name;
		try{
			name = displayName.apply(referenced);
		}
		catch(final RuntimeException ignored){
			return null;
		}
		return (StringUtils.isBlank(name)? null: "— " + StringUtils.abbreviate(name.strip(), MAX_NAME_LENGTH));
	}

	private static void addNames(final JTextArea area, final Function<String, String> nameOfLine){
		final Highlighter highlighter = area.getHighlighter();
		final String[] lines = RecordAlignment.linesOf(area.getText());
		try{
			for(int line = 0; line < lines.length && line < area.getLineCount(); line ++){
				final String name = nameOfLine.apply(lines[line]);
				if(name == null)
					continue;

				final int start = area.getLineStartOffset(line);
				final int end = area.getLineEndOffset(line);
				if(end > start)
					highlighter.addHighlight(start, end, new LabelPainter(name));
			}
		}
		catch(final BadLocationException ignored){
			//a line that is gone: the next change of the text paints them again
		}
	}

}
