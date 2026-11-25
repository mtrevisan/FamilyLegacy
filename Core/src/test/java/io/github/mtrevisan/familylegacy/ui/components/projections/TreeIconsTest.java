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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeIcons;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.TreeLayout;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class TreeIconsTest{

	@Test
	void theArrowsHaveTheSizeOfTheImagesTheyReplace(){
		//as before: the vertical ones fill the area the panels reserve, the sideways ones are as wide and 8 pixels tall
		for(final Icon icon : new Icon[]{TreeIcons.ancestors(TreeLayout.VERTICAL), TreeIcons.descendants(TreeLayout.VERTICAL)}){
			assertEquals(TreeIcons.ARROW_SIZE.width, icon.getIconWidth());
			assertEquals(TreeIcons.ARROW_SIZE.height, icon.getIconHeight());
		}
		for(final Icon icon : new Icon[]{TreeIcons.ancestors(TreeLayout.HORIZONTAL), TreeIcons.descendants(TreeLayout.HORIZONTAL)}){
			assertEquals(TreeIcons.ARROW_SIZE.width, icon.getIconWidth());
			assertEquals(8, icon.getIconHeight());
		}
	}

	@Test
	void theArrowsFillTheirArea(){
		for(final TreeLayout layout : TreeLayout.values())
			for(final Icon icon : new Icon[]{TreeIcons.ancestors(layout), TreeIcons.descendants(layout)}){
				final BufferedImage image = paint(icon, new JLabel());
				int minX = Integer.MAX_VALUE;
				int maxX = -1;
				int minY = Integer.MAX_VALUE;
				int maxY = -1;
				for(int y = 0; y < image.getHeight(); y ++)
					for(int x = 0; x < image.getWidth(); x ++)
						if((image.getRGB(x, y) >>> 24) > 60){
							minX = Math.min(minX, x);
							maxX = Math.max(maxX, x);
							minY = Math.min(minY, y);
							maxY = Math.max(maxY, y);
						}
				assertEquals(0, minX);
				assertEquals(0, minY);
				assertEquals(image.getWidth() - 1, maxX);
				assertEquals(image.getHeight() - 1, maxY);
			}
	}

	@Test
	void theArrowsPointWhereTheTreeGoes(){
		//the head of the arrow is the heavy side: ancestors up (vertical) or right (horizontal), descendants down or left
		assertTrue(weight(TreeIcons.ancestors(TreeLayout.VERTICAL), true) > 0, "ancestors point up");
		assertTrue(weight(TreeIcons.descendants(TreeLayout.VERTICAL), true) < 0, "descendants point down");
		assertTrue(weight(TreeIcons.ancestors(TreeLayout.HORIZONTAL), false) > 0, "ancestors point right");
		assertTrue(weight(TreeIcons.descendants(TreeLayout.HORIZONTAL), false) < 0, "descendants point left");
	}

	@Test
	void theArrowIsInTheColourOfTheComponent(){
		final JLabel label = new JLabel();
		label.setForeground(new Color(0x1565C0));
		final Icon icon = TreeIcons.ancestors(TreeLayout.VERTICAL);

		final BufferedImage image = paint(icon, label);

		int painted = 0;
		for(int y = 0; y < image.getHeight(); y ++)
			for(int x = 0; x < image.getWidth(); x ++)
				if((image.getRGB(x, y) >>> 24) > 200){
					painted ++;
					assertTrue(Math.abs(0x15 - ((image.getRGB(x, y) >> 16) & 0xFF)) <= 3
						&& Math.abs(0x65 - ((image.getRGB(x, y) >> 8) & 0xFF)) <= 3
						&& Math.abs(0xC0 - (image.getRGB(x, y) & 0xFF)) <= 3);
				}
		assertTrue(painted > 10);
	}


	/** @return	The painted pixels of the first half minus the ones of the second (rows if vertical, else columns): positive if the head is on the first. */
	private static int weight(final Icon icon, final boolean vertical){
		final BufferedImage image = paint(icon, new JLabel());
		final int length = (vertical? image.getHeight(): image.getWidth());
		int first = 0;
		int second = 0;
		for(int y = 0; y < image.getHeight(); y ++)
			for(int x = 0; x < image.getWidth(); x ++)
				if((image.getRGB(x, y) >>> 24) > 100){
					final int position = (vertical? y: x);
					if(position < length / 2)
						first ++;
					else
						second ++;
				}
		//a head on the right has more weight on the second half: the sign is the one of "first is the head"
		return (vertical? first - second: second - first);
	}

	private static BufferedImage paint(final Icon icon, final JLabel owner){
		final BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = image.createGraphics();
		icon.paintIcon(owner, g, 0, 0);
		g.dispose();
		return image;
	}

}
