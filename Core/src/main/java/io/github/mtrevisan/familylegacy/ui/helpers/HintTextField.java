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

import org.apache.commons.lang3.StringUtils;

import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;


/**
 * A text field that says what to do while it is empty, as a grey text that is drawn (and never part of the text): <em>(right-click to
 * set)</em> for the references, that are picked with the right button of the mouse.
 */
public class HintTextField extends JTextField{

	private String hint = StringUtils.EMPTY;


	public HintTextField(final int columns){
		super(columns);
	}

	public HintTextField(final String text, final int columns){
		super(text, columns);
	}


	/** @param hint	What the field says while it is empty (nothing if blank). */
	public void setHint(final String hint){
		this.hint = StringUtils.defaultString(hint);

		repaint();
	}

	/** @return	What the field says while it is empty. */
	public String getHint(){
		return hint;
	}


	@Override
	protected void paintComponent(final Graphics g){
		super.paintComponent(g);

		if(hint.isEmpty() || !getText().isEmpty())
			return;

		final Color color = UIManager.getColor("TextField.inactiveForeground");
		g.setColor(color != null? color: Color.GRAY);
		g.setFont(getFont());
		final Insets insets = getInsets();
		final int baseline = insets.top + (getHeight() - insets.top - insets.bottom + g.getFontMetrics().getAscent()
			- g.getFontMetrics().getDescent()) / 2;
		g.drawString(hint, insets.left, baseline);
	}

}
