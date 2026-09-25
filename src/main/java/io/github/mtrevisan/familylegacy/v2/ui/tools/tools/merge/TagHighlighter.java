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
package io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge;

import io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge.TagSuggester.Context;
import io.github.mtrevisan.familylegacy.v2.ui.tools.tools.merge.TagSuggester.ValueKind;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Set;
import java.util.function.Function;


/**
 * Highlights problems in a FLEF editor made of a stack of text areas,
 * one per row:
 * <ul>
 *   <li><b>unknown tag</b>: a red wavy underline marks an identifier
 *       that does not appear in any type of the grammar;</li>
 *   <li><b>missing value</b>: a light amber background wash on the
 *       whole line marks a leaf tag that is not followed by a value;</li>
 *   <li><b>invalid value</b>: a red wavy underline on the value marks a
 *       value whose shape does not match the type declared in the
 *       grammar.</li>
 * </ul>
 * The highlighter works on the <b>concatenated text</b> of the whole
 * column, not on a single area, because the field's structure is split
 * across the areas: the enclosing block at a given line can only be
 * determined by looking at everything above it. The two functions
 * {@code textFor} and {@code offsetFor}, provided by the caller, give
 * the concatenation and the offset of each area inside it.
 */
public final class TagHighlighter{

	private static final Color COLOR_UNKNOWN = new Color(0xC03030);
	private static final Color COLOR_INVALID_VALUE = new Color(0xC03030);
	private static final Color COLOR_MISSING_BG = new Color(0xFF, 0xF3, 0xCD, 0x80);

	private static final Highlighter.HighlightPainter UNKNOWN_TAG_PAINTER =
		new SquigglyUnderlinePainter(COLOR_UNKNOWN);
	private static final Highlighter.HighlightPainter INVALID_VALUE_PAINTER =
		new SquigglyUnderlinePainter(COLOR_INVALID_VALUE);
	private static final Highlighter.HighlightPainter MISSING_VALUE_PAINTER =
		new LineBackgroundPainter(COLOR_MISSING_BG);

	private static final String TOOLTIP_UNKNOWN =
		"Unknown tag: it is not part of the FLEF grammar and will be ignored.";
	private static final String TOOLTIP_MISSING =
		"Missing value: the tag has no value.";
	private static final String TOOLTIP_INVALID =
		"Invalid value: it does not match the type declared in the grammar.";


	private TagHighlighter(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	public static void install(final List<JTextArea> areas, final TagSuggester suggester,
		final Function<JTextArea, String> textFor,
		final Function<JTextArea, Integer> offsetFor){
		final DocumentListener shared = new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ refreshAll(areas, suggester, textFor, offsetFor); }
			@Override public void removeUpdate(final DocumentEvent e){ refreshAll(areas, suggester, textFor, offsetFor); }
			@Override public void changedUpdate(final DocumentEvent e){ refreshAll(areas, suggester, textFor, offsetFor); }
		};

		for(final JTextArea area : areas){
			area.getDocument().addDocumentListener(shared);
			area.setToolTipText(null);
			area.addMouseMotionListener(new MouseMotionAdapter(){
				@Override
				public void mouseMoved(final MouseEvent e){
					area.setToolTipText(tooltipAt(area, e, suggester, textFor, offsetFor));
				}
			});
		}

		refreshAll(areas, suggester, textFor, offsetFor);
	}


	/* ======================================================================
	 *                          Refresh
	 * ====================================================================== */

	private static void refreshAll(final List<JTextArea> areas, final TagSuggester suggester,
		final Function<JTextArea, String> textFor,
		final Function<JTextArea, Integer> offsetFor){
		final String fullText = textFor.apply(areas.get(0));
		final Set<String> known = suggester.allKnownTags();
		if(known.isEmpty())
			return;

		for(final JTextArea area : areas){
			final int offset = offsetFor.apply(area);
			area.getHighlighter().removeAllHighlights();
			highlightArea(area, fullText, offset, suggester, known);
		}
	}

	private static void highlightArea(final JTextArea area, final String fullText,
		final int areaOffset, final TagSuggester suggester, final Set<String> known){
		final String text = area.getText();
		int lineStart = 0;
		while(lineStart <= text.length()){
			int lineEnd = text.indexOf('\n', lineStart);
			if(lineEnd < 0)
				lineEnd = text.length();

			highlightLine(area, text, fullText, areaOffset, lineStart, lineEnd,
				suggester, known);

			if(lineEnd >= text.length())
				break;
			lineStart = lineEnd + 1;
		}
	}

	private static void highlightLine(final JTextArea area, final String text,
		final String fullText, final int areaOffset, final int lineStart,
		final int lineEnd, final TagSuggester suggester, final Set<String> known){
		int start = lineStart;
		while(start < lineEnd
			&& (text.charAt(start) == ' ' || text.charAt(start) == '\t'))
			start ++;
		if(start >= lineEnd)
			return;
		final char first = text.charAt(start);
		if(!Character.isLetter(first) && first != '_')
			return;

		int end = start;
		while(end < lineEnd){
			final char c = text.charAt(end);
			if(Character.isLetterOrDigit(c) || c == '_' || c == '.')
				end ++;
			else
				break;
		}

		final Context ctx = suggester.resolveContext(fullText, areaOffset + lineStart);
		final String parentType = ctx.parentType();

		final String fullTag = text.substring(start, end);
		final int lastDot = fullTag.lastIndexOf('.');
		final String lastSegment = (lastDot >= 0? fullTag.substring(lastDot + 1): fullTag);

		int afterTag = end;
		while(afterTag < lineEnd
			&& (text.charAt(afterTag) == ' ' || text.charAt(afterTag) == '\t'))
			afterTag ++;
		final boolean hasBrace = (afterTag < lineEnd && text.charAt(afterTag) == '{');
		final boolean hasValue = (afterTag < lineEnd && !hasBrace);

		// Background first, then foreground.
		if(!hasBrace && !hasValue){
			final boolean knownBlockOnly = suggester.isBlockTag(lastSegment)
				&& !suggester.isLeafTag(lastSegment);
			if(!knownBlockOnly)
				addHighlight(area, start, lineEnd, MISSING_VALUE_PAINTER);
		}

		boolean unknown = false;
		int cursor = start;
		while(cursor < end){
			int dot = text.indexOf('.', cursor);
			if(dot < 0 || dot >= end)
				dot = end;
			final String segment = text.substring(cursor, dot);
			if(!segment.isEmpty() && !known.contains(segment)){
				addHighlight(area, cursor, dot, UNKNOWN_TAG_PAINTER);
				unknown = true;
			}
			cursor = dot + 1;
		}

		if(hasValue && !unknown && parentType != null){
			final ValueKind kind = suggester.valueKindOf(parentType, lastSegment);
			if(kind != ValueKind.ANY && kind != ValueKind.ENUM){
				final String value = text.substring(afterTag, lineEnd).trim();
				if(!value.isEmpty() && !isValidValue(value, kind))
					addHighlight(area, afterTag, lineEnd, INVALID_VALUE_PAINTER);
			}
		}
	}

	private static boolean isValidValue(final String value, final ValueKind kind){
		return switch(kind){
			case ANY, ENUM -> true;
			case INT -> value.matches("-?\\d+");
			case BOOL -> value.equals("true") || value.equals("false");
			case DATE -> value.matches("\\d{4}-\\d{2}-\\d{2}");
			case HISTORICAL_DATE -> value.matches("\\d{4}(-\\d{2}(-\\d{2})?)?");
			case URI -> value.matches("[a-zA-Z][a-zA-Z0-9+.-]*:.+");
			case SEMVER -> value.matches("\\d+\\.\\d+\\.\\d+(?:[-+].*)?");
			case LOCALE -> value.matches("[a-zA-Z]{2,3}(?:-[a-zA-Z0-9]+)*");
			case DURATION -> value.matches("P.*");
			case COORD -> value.matches("[+-]?\\d+(?:\\.\\d+)?[+-]\\d+(?:\\.\\d+)?.*");
		};
	}

	private static void addHighlight(final JTextArea area, final int start, final int end,
		final Highlighter.HighlightPainter painter){
		if(end <= start)
			return;
		try{
			area.getHighlighter().addHighlight(start, end, painter);
		}
		catch(final BadLocationException ignored){}
	}


	/* ======================================================================
	 *                          Tooltip
	 * ====================================================================== */

	private static String tooltipAt(final JTextArea area, final MouseEvent e,
		final TagSuggester suggester,
		final Function<JTextArea, String> textFor,
		final Function<JTextArea, Integer> offsetFor){
		final int offset = area.viewToModel2D(e.getPoint());
		if(offset < 0)
			return null;

		// An error or warning wins over any hint.
		for(final Highlighter.Highlight h : area.getHighlighter().getHighlights()){
			if(offset >= h.getStartOffset() && offset < h.getEndOffset()){
				final Highlighter.HighlightPainter painter = h.getPainter();
				if(painter instanceof SquigglyUnderlinePainter p){
					final Color c = p.color();
					return (c.equals(COLOR_UNKNOWN)? TOOLTIP_UNKNOWN: TOOLTIP_INVALID);
				}
				if(painter instanceof LineBackgroundPainter)
					return TOOLTIP_MISSING;
			}
		}

		// No highlight: show the format hint when the field expects a
		// value whose shape is not obvious.
		final String fullText = textFor.apply(area);
		final int fullOffset = offsetFor.apply(area) + offset;
		final String hint = suggester.valueHintAt(fullText, fullOffset);
		return (hint != null? "Expected value: " + hint: null);
	}


	/* ======================================================================
	 *                          Painters
	 * ====================================================================== */

	private static final class SquigglyUnderlinePainter implements Highlighter.HighlightPainter{

		private static final int WAVE_STEP = 2;
		private static final int WAVE_HEIGHT = 2;

		private final Color color;

		SquigglyUnderlinePainter(final Color color){
			this.color = color;
		}

		Color color(){
			return color;
		}

		@Override
		public void paint(final Graphics g, final int p0, final int p1,
			final Shape bounds, final JTextComponent c){
			try{
				final Rectangle2D r0 = c.modelToView2D(p0);
				final Rectangle2D r1 = c.modelToView2D(p1);
				if(r0 == null || r1 == null)
					return;

				final Graphics2D g2 = (Graphics2D)g.create();
				g2.setColor(color);
				g2.setStroke(new BasicStroke(1f));

				final int y = (int)r0.getMaxY() - 1;
				final int xStart = (int)r0.getMinX();
				final int xEnd = (int)r1.getMaxX();

				boolean up = true;
				int x = xStart;
				while(x < xEnd){
					final int nextX = Math.min(x + WAVE_STEP, xEnd);
					final int yLine = (up? y - WAVE_HEIGHT / 2: y + WAVE_HEIGHT / 2);
					g2.drawLine(x, yLine, nextX, yLine);
					x = nextX;
					up = !up;
				}
				g2.dispose();
			}
			catch(final BadLocationException ignored){}
		}
	}

	private static final class LineBackgroundPainter implements Highlighter.HighlightPainter{

		private final Color color;

		LineBackgroundPainter(final Color color){
			this.color = color;
		}

		@Override
		public void paint(final Graphics g, final int p0, final int p1,
			final Shape bounds, final JTextComponent c){
			try{
				final Rectangle2D r0 = c.modelToView2D(p0);
				if(r0 == null)
					return;

				final int yStart = (int)r0.getMinY();
				final int yEnd = (int)r0.getMaxY() - 1;
				if(yEnd <= yStart)
					return;

				final Graphics2D g2 = (Graphics2D)g.create();
				g2.setColor(color);
				g2.fillRect(0, yStart, c.getWidth(), yEnd - yStart);
				g2.dispose();
			}
			catch(final BadLocationException ignored){}
		}
	}

}