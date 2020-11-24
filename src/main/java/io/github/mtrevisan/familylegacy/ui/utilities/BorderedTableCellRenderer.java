package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;


public class BorderedTableCellRenderer extends JLabel implements TableCellRenderer{

	private static final int HORIZONTAL_BORDER = 5;


	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus,
			final int row, final int column){
		setBorder(hasFocus?
			UIManager.getBorder("Table.focusCellHighlightBorder"):
			new CompoundBorder(new EmptyBorder(new Insets(1, HORIZONTAL_BORDER, 1, HORIZONTAL_BORDER)), null));
		setBackground(!hasFocus && isSelected? table.getSelectionBackground(): table.getBackground());
		setOpaque(true);
		setText((String)value);
		return this;
	}

}
