package io.github.mtrevisan.familylegacy.flef.ui.helpers;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Component;


public class SeparatorComboBoxRenderer extends DefaultListCellRenderer{

	private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);


	@Override
	public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
			final boolean cellHasFocus){
		if(value instanceof String && ((String)value).startsWith("--- ") && ((String)value).endsWith(" ---")){
			final JLabel separatorLabel = new JLabel((String)value);
			separatorLabel.setEnabled(false);
			separatorLabel.setOpaque(true);
			separatorLabel.setBackground(Color.LIGHT_GRAY);
			separatorLabel.setBorder(EMPTY_BORDER);
			return separatorLabel;
		}
		else
			return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
	}

}
