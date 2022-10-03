/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Deque;
import java.util.LinkedList;


public final class FontHelper{

	public static final String CLIENT_PROPERTY_KEY_FONTABLE = "fontable";

	private static final FontRenderContext FRC = new FontRenderContext(null, RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT,
		RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);



	private FontHelper(){}

	public static Rectangle2D getStringBounds(final Font font, final String text){
		return font.getStringBounds(text, FRC);
	}

	public static void setCurrentFont(final Font font, final Component... parentFrames){
		final int size = (parentFrames != null? parentFrames.length: 0);
		for(int i = 0; i < size; i ++)
			updateComponent(parentFrames[i], font);
	}

	private static void updateComponent(final Component component, final Font font){
		final Deque<Component> stack = new LinkedList<>();
		stack.push(component);
		while(!stack.isEmpty()){
			final Component comp = stack.pop();

			if(comp instanceof JEditorPane)
				((JComponent)comp).putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			if((comp instanceof JComponent) && ((JComponent)comp).getClientProperty(CLIENT_PROPERTY_KEY_FONTABLE) == Boolean.TRUE)
				comp.setFont(font);

			if(comp instanceof Container){
				final Component[] components = ((Container)comp).getComponents();
				final int size = (components != null? components.length: 0);
				for(int i = 0; i < size; i++)
					stack.push(components[i]);
			}
		}
	}

}
