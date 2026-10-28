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
package io.github.mtrevisan.familylegacy.ui.components.lists;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.Objects;


/**
 * Icons drawn side by side, vertically centered.
 */
public final class CompoundIcon implements Icon{

	private static final int GAP = 3;


	private final Icon[] icons;


	private CompoundIcon(final Icon[] icons){
		this.icons = icons;
	}


	/**
	 * Combines the icons, skipping the {@code null} ones.
	 *
	 * @param icons	The icons, from left to right.
	 * @return	{@code null} if there is no icon, the icon itself if there is only one, else the compound icon.
	 */
	public static Icon of(final Icon... icons){
		final Icon[] present = Arrays.stream(icons)
			.filter(Objects::nonNull)
			.toArray(Icon[]::new);
		return switch(present.length){
			case 0 -> null;
			case 1 -> present[0];
			default -> new CompoundIcon(present);
		};
	}


	@Override
	public void paintIcon(final Component c, final Graphics g, final int x, final int y){
		final int height = getIconHeight();
		int left = x;
		for(final Icon icon : icons){
			icon.paintIcon(c, g, left, y + (height - icon.getIconHeight()) / 2);
			left += icon.getIconWidth() + GAP;
		}
	}

	@Override
	public int getIconWidth(){
		int width = GAP * (icons.length - 1);
		for(final Icon icon : icons)
			width += icon.getIconWidth();
		return width;
	}

	@Override
	public int getIconHeight(){
		int height = 0;
		for(final Icon icon : icons)
			height = Math.max(height, icon.getIconHeight());
		return height;
	}

}
