/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.utilities;

import io.github.mtrevisan.familylegacy.gedcom.parsers.Sex;
import io.github.mtrevisan.familylegacy.ui.dialogs.LinkIndividualDialog;
import io.github.mtrevisan.familylegacy.ui.panels.IndividualPanel;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Vector;


public class IndividualTableCellRenderer extends JLabel implements TableCellRenderer{

	private static final int HORIZONTAL_BORDER = 5;


	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus,
			final int row, final int column){
		Color backgroundColor = table.getBackground();
		if(column == LinkIndividualDialog.TABLE_INDEX_SEX){
			final Sex sex = Sex.fromCode((String)value);
			if(sex != Sex.UNKNOWN)
				backgroundColor = IndividualPanel.BACKGROUND_COLOR_FROM_SEX.get(sex);
		}
		else if(column == LinkIndividualDialog.TABLE_INDEX_NAME){
			final DefaultTableModel model = (DefaultTableModel)table.getModel();
			@SuppressWarnings({"UseOfObsoleteCollectionType", "rawtypes"})
			final Vector rowData = model.getDataVector().get(table.convertRowIndexToModel(row));
			//set alternative names as tooltip
			setToolTipText((String)rowData.get(LinkIndividualDialog.TABLE_INDEX_ADDITIONAL_NAMES));
		}

		setBorder(hasFocus?
			UIManager.getBorder("Table.focusCellHighlightBorder"):
			new CompoundBorder(new EmptyBorder(new Insets(1, HORIZONTAL_BORDER, 1, HORIZONTAL_BORDER)), null));
		setBackground(!hasFocus && isSelected? table.getSelectionBackground(): backgroundColor);
		setOpaque(true);
		setText((String)value);

		return this;
	}

}
