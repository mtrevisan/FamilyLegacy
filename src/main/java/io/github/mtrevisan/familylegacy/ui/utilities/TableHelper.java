package io.github.mtrevisan.familylegacy.ui.utilities;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;


public final class TableHelper{

	private TableHelper(){}

	public static TableColumn setColumnWidth(final JTable table, final int columnIndex, final int min, final int preferred){
		return setColumnWidth(table, columnIndex, min, preferred, Short.MAX_VALUE);
	}

	public static TableColumn setColumnWidth(final JTable table, final int columnIndex, final int min, final int preferred, final int max){
		final TableColumnModel columnModel = table.getColumnModel();
		final TableColumn column = columnModel.getColumn(columnIndex);
		column.setMinWidth(min);
		column.setPreferredWidth(preferred);
		column.setMaxWidth(max);
		return column;
	}

	/**
	 * Moves all rows contained between the positions <code>start</code> and <code>end</code> to the position specified by <code>dest</code>.
	 *
	 * @param model	The model of the table.
	 * @param startIndex	Start index of the rows to be moved (inclusive).
	 * @param endIndex	End index of the rows to be moved (exclusive).
	 * @param destinationIndex	Destination index.
	 */
	public static void moveRow(final DefaultTableModel model, final int startIndex, int endIndex, int destinationIndex){
		final int count = endIndex - startIndex;
		if(count > 0){
			if(destinationIndex > startIndex)
				destinationIndex = Math.max(startIndex, destinationIndex - count);
			endIndex --;

			model.moveRow(startIndex, endIndex, destinationIndex);
		}
	}

}
