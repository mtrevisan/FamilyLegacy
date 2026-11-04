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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions;

import org.apache.commons.lang3.StringUtils;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;


/**
 * A read-only table model made of fixed columns and rows of plain values, shared by the report dialogs of the tools.
 * <p>
 * A column whose values are all {@link Integer} is sorted as numbers; any other is sorted as text.
 */
public final class RowTableModel extends AbstractTableModel{

	private final String[] columns;
	private final List<Object[]> rows;
	private final Class<?>[] columnClasses;


	/**
	 * @param columns	The titles of the columns.
	 * @param rows	The rows: each one has one value for each column ({@code null} is shown as an empty cell).
	 */
	public RowTableModel(final String[] columns, final List<Object[]> rows){
		this.columns = columns;
		this.rows = new ArrayList<>(rows);

		columnClasses = new Class<?>[columns.length];
		for(int column = 0; column < columns.length; column ++){
			boolean numeric = !this.rows.isEmpty();
			for(final Object[] row : this.rows)
				if(!(row[column] instanceof Integer)){
					numeric = false;
					break;
				}
			columnClasses[column] = (numeric? Integer.class: String.class);
		}
	}


	/** @return	The values of the row. */
	public Object[] rowAt(final int row){
		return rows.get(row);
	}

	/** @return	The rows, as tab-separated lines (the titles first), for the clipboard. */
	public String toText(){
		final StringBuilder sb = new StringBuilder(String.join("\t", columns))
			.append(StringUtils.LF);
		for(final Object[] row : rows){
			for(int column = 0; column < row.length; column ++){
				if(column > 0)
					sb.append('\t');
				sb.append(row[column] != null? row[column]: StringUtils.EMPTY);
			}
			sb.append(StringUtils.LF);
		}
		return sb.toString();
	}

	@Override
	public int getRowCount(){
		return rows.size();
	}

	@Override
	public int getColumnCount(){
		return columns.length;
	}

	@Override
	public String getColumnName(final int column){
		return columns[column];
	}

	@Override
	public Class<?> getColumnClass(final int column){
		return columnClasses[column];
	}

	@Override
	public Object getValueAt(final int row, final int column){
		final Object value = rows.get(row)[column];
		return (value != null? value: (columnClasses[column] == Integer.class? 0: StringUtils.EMPTY));
	}

}
