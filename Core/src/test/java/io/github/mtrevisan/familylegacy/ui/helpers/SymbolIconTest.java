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
package io.github.mtrevisan.familylegacy.ui.helpers;

import io.github.mtrevisan.familylegacy.ui.helpers.SymbolIcon.Symbol;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class SymbolIconTest{

	@Test
	void everySymbolIsPaintedInTheGivenColourOnly(){
		final Color red = new Color(0xC62828);
		for(final Symbol symbol : Symbol.values()){
			final BufferedImage image = paint(SymbolIcon.of(symbol, red), new JLabel());

			int painted = 0;
			for(int y = 0; y < image.getHeight(); y ++)
				for(int x = 0; x < image.getWidth(); x ++){
					final int argb = image.getRGB(x, y);
					//the faint pixels of the edges are not exact in a non-premultiplied image
					if((argb >>> 24) > 200){
						painted ++;
						assertClose(red, argb, symbol + " is not in the colour asked");
					}
				}
			assertTrue(painted > 10, symbol + " is not painted");
		}
	}

	@Test
	void theColourIsTheOneOfTheComponent(){
		final JLabel label = new JLabel();
		label.setForeground(new Color(0x1565C0));

		final BufferedImage image = paint(SymbolIcon.of(Symbol.CHECK), label);

		boolean found = false;
		for(int y = 0; y < image.getHeight() && !found; y ++)
			for(int x = 0; x < image.getWidth(); x ++)
				if((image.getRGB(x, y) >>> 24) > 200){
					assertClose(new Color(0x1565C0), image.getRGB(x, y), "the colour of the component");
					found = true;
					break;
				}
		assertTrue(found);
	}

	@Test
	void theSizeIsTheOneOfTheFontOfTheInterface(){
		final SymbolIcon icon = SymbolIcon.of(Symbol.WARNING);

		assertEquals(SymbolIcon.defaultSize(), icon.getIconWidth());
		assertEquals(SymbolIcon.defaultSize(), icon.getIconHeight());
		assertTrue(icon.getIconWidth() >= 10);
	}


	/** The channels may differ by the rounding of the painting. */
	private static void assertClose(final Color expected, final int argb, final String message){
		assertTrue(Math.abs(expected.getRed() - ((argb >> 16) & 0xFF)) <= 3
			&& Math.abs(expected.getGreen() - ((argb >> 8) & 0xFF)) <= 3
			&& Math.abs(expected.getBlue() - (argb & 0xFF)) <= 3, message);
	}

	private static BufferedImage paint(final SymbolIcon icon, final JLabel owner){
		final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		icon.paintIcon(owner, g, 0, 0);
		g.dispose();
		return image;
	}

}
