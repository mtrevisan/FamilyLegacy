/**
 * Copyright (c) 2021 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;


/**
 * Parent class need to implement {@link java.beans.PropertyChangeListener} as follows
 * <pre>{@code
 *    @Override
 * 	public void propertyChange(final PropertyChangeEvent evt){
 * 		if("text".equals(evt.getPropertyName())){
 * 			label.manageTooltip();
 * 		}
 * 	}
 * }</pre>
 * and the field must set
 * <pre>{@code
 * 	label.addPropertyChangeListener("text", this);
 * }</pre>
 */
public class TooltipLabel extends JLabel{

	public String getToolTipText(final MouseEvent e){
		final FontMetrics fm = getFontMetrics(getFont());
		final String text = getText();
		final int textWidth = fm.stringWidth(text);
		return (textWidth > getSize().width? text: null);
	}

	public JToolTip createToolTip(){
		final JToolTip tip = new JToolTip();
		tip.setBackground(java.awt.SystemColor.info);
		tip.setComponent(this);
		return tip;
	}

	public void manageToolTip(){
		final boolean showToolTip = (getUI().getPreferredSize(this).width > getWidth());
		setToolTipText(showToolTip? getText(): null);
	}

}
