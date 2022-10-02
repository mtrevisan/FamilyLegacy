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
	 * Moves all rows contained between the positions {@code start} and {@code end} to the position specified by <code>dest</code>.
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
