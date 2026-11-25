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
package io.github.mtrevisan.familylegacy.ui.assistant;

import io.github.mtrevisan.familylegacy.assistant.FindingCounts;
import io.github.mtrevisan.familylegacy.ui.helpers.SymbolIcon;
import io.github.mtrevisan.familylegacy.ui.helpers.SymbolIcon.Symbol;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * What the indicator of the assistant shows after its name: for each kind of finding that is there its symbol and its number
 * (or a tick, if there is none), then, if given, a closing text (the ellipsis of a computation that is running).
 * <p>It is painted, with the symbols as images, because a font may not have them; it uses the font and the colour of the
 * component it is on.</p>
 */
final class CountsIcon implements Icon{

	private static final int SYMBOL_GAP = 3;
	private static final int ENTRY_GAP = 8;

	private final JComponent owner;
	private final List<Entry> entries = new ArrayList<>(3);
	private final String trailing;


	/**
	 * @param owner	The component the icon is on (its font tells the size of the numbers).
	 * @param counts	The findings.
	 * @param trailing	The text after the counts, or {@code null}.
	 */
	CountsIcon(final JComponent owner, final FindingCounts counts, final String trailing){
		this.owner = owner;
		this.trailing = trailing;

		if(counts.isClean())
			entries.add(new Entry(Symbol.CHECK, null));
		else{
			add(Symbol.CROSS, counts.errors());
			add(Symbol.WARNING, counts.warnings());
			add(Symbol.INFORMATION, counts.notes());
		}
	}

	private void add(final Symbol symbol, final int count){
		if(count > 0)
			entries.add(new Entry(symbol, Integer.toString(count)));
	}


	@Override
	public int getIconWidth(){
		final FontMetrics metrics = owner.getFontMetrics(owner.getFont());
		final int side = SymbolIcon.defaultSize();
		int width = 0;
		for(final Entry entry : entries){
			if(width > 0)
				width += ENTRY_GAP;
			width += side;
			if(entry.number != null)
				width += SYMBOL_GAP + metrics.stringWidth(entry.number);
		}
		if(trailing != null)
			width += ENTRY_GAP + metrics.stringWidth(trailing);
		return width;
	}

	@Override
	public int getIconHeight(){
		return Math.max(SymbolIcon.defaultSize(), owner.getFontMetrics(owner.getFont()).getHeight());
	}

	@Override
	public void paintIcon(final Component c, final Graphics graphics, final int x, final int y){
		//the numbers have to look as the text of the button: Swing paints that with the smoothing of the desktop (ClearType),
		//a plain Graphics has none, and the same font would look thinner and different
		final Graphics2D g = (Graphics2D)graphics.create();
		try{
			final Map<?, ?> hints = (Map<?, ?>)Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
			if(hints != null)
				g.addRenderingHints(hints);
			paintContent(c, g, x, y);
		}
		finally{
			g.dispose();
		}
	}

	private void paintContent(final Component c, final Graphics2D g, final int x, final int y){
		final FontMetrics metrics = c.getFontMetrics(c.getFont());
		final int side = SymbolIcon.defaultSize();
		final int height = getIconHeight();
		final int baseline = y + (height + metrics.getAscent() - metrics.getDescent()) / 2;
		g.setFont(c.getFont());
		g.setColor(c.getForeground());

		int left = x;
		for(final Entry entry : entries){
			if(left > x)
				left += ENTRY_GAP;
			entry.symbol.paint(g, left, y + (height - side) / 2, side, c.getForeground());
			left += side;
			if(entry.number != null){
				left += SYMBOL_GAP;
				g.drawString(entry.number, left, baseline);
				left += metrics.stringWidth(entry.number);
			}
		}
		if(trailing != null)
			g.drawString(trailing, left + ENTRY_GAP, baseline);
	}


	private static final class Entry{

		private final SymbolIcon symbol;
		private final String number;


		private Entry(final Symbol symbol, final String number){
			this.symbol = SymbolIcon.of(symbol);
			this.number = number;
		}

	}

}
