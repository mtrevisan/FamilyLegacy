/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.Border;
import java.awt.Component;


public class SeparatorComboBoxRenderer extends DefaultListCellRenderer{

	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);


	private final String menuSeparatorStartsWith;
	private final String menuSeparatorEndsWith;


	public SeparatorComboBoxRenderer(final String menuSeparatorStartsWith, final String menuSeparatorEndsWith){
		this.menuSeparatorStartsWith = menuSeparatorStartsWith;
		this.menuSeparatorEndsWith = menuSeparatorEndsWith;
	}

	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus){
		if(value instanceof String && ((String)value).startsWith(menuSeparatorStartsWith) && ((String)value).endsWith(menuSeparatorEndsWith)){
			final JLabel separatorLabel = new JLabel((String)value);
			separatorLabel.setEnabled(false);
			separatorLabel.setOpaque(true);
			separatorLabel.setBorder(EMPTY_BORDER);
			return separatorLabel;
		}
		else{
			final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			setText(value == null? StringUtils.SPACE: value.toString());
			return comp;
		}
	}

}
