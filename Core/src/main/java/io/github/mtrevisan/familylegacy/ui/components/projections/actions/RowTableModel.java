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

import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;


/**
 * A read-only table model made of fixed columns and rows of plain values, shared by the report dialogs of the tools.
 * <p>
 * A column whose values are all {@link Integer} (or empty) is sorted as numbers; any other is sorted as text.
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
			boolean numeric = false;
			for(final Object[] row : this.rows){
				if(row[column] == null)
					continue;
				if(!(row[column] instanceof Integer)){
					numeric = false;
					break;
				}
				numeric = true;
			}
			columnClasses[column] = (numeric? Integer.class: String.class);
		}
	}


	/**
	 * Puts the model into the table, keeping what the user had set: the selected row (same position, or the last one if the table is
	 * now shorter) and the sort order. Replacing a model alone drops both, which is annoying after a dialog that edited a record.
	 *
	 * @param table	The table.
	 * @param model	The new model.
	 */
	public static void replaceModel(final JTable table, final TableModel model){
		final int selected = table.getSelectedRow();
		final RowSorter<?> oldSorter = table.getRowSorter();
		final List<? extends RowSorter.SortKey> sortKeys = (oldSorter != null? new ArrayList<>(oldSorter.getSortKeys()): List.of());

		table.setModel(model);

		final RowSorter<?> sorter = table.getRowSorter();
		if(sorter != null && !sortKeys.isEmpty()){
			try{
				sorter.setSortKeys(sortKeys);
			}
			catch(final IllegalArgumentException ignored){
				//the columns are not the same any more: leave the default order
			}
		}
		final int count = table.getRowCount();
		if(selected >= 0 && count > 0){
			final int row = Math.min(selected, count - 1);
			table.setRowSelectionInterval(row, row);
			table.scrollRectToVisible(table.getCellRect(row, 0, true));
		}
	}

	/**
	 * Notes the selected row of every table of every tab, for a dialog that builds its tabs again after a record has been edited.
	 *
	 * @param tabs	The tabs.
	 * @return	For each tab, the selected row of each of its tables (in the order they appear), {@code -1} if none.
	 */
	public static List<List<Integer>> selectionsOf(final JTabbedPane tabs){
		final List<List<Integer>> selections = new ArrayList<>();
		for(int i = 0; i < tabs.getTabCount(); i ++){
			final List<Integer> rows = new ArrayList<>();
			for(final JTable table : tablesOf(tabs.getComponentAt(i)))
				rows.add(table.getSelectedRow());
			selections.add(rows);
		}
		return selections;
	}

	/**
	 * Selects again the rows noted by {@link #selectionsOf(JTabbedPane)} (the last row if the table is shorter now); a table that
	 * already has a selection is left alone.
	 *
	 * @param tabs	The tabs, built again.
	 * @param selections	The rows noted before.
	 */
	public static void restoreSelections(final JTabbedPane tabs, final List<List<Integer>> selections){
		for(int i = 0; i < Math.min(selections.size(), tabs.getTabCount()); i ++){
			final List<JTable> tables = tablesOf(tabs.getComponentAt(i));
			final List<Integer> rows = selections.get(i);
			for(int t = 0; t < Math.min(tables.size(), rows.size()); t ++){
				final JTable table = tables.get(t);
				final int row = rows.get(t);
				if(row >= 0 && table.getSelectedRow() < 0 && table.getRowCount() > 0){
					final int target = Math.min(row, table.getRowCount() - 1);
					table.setRowSelectionInterval(target, target);
				}
			}
		}
	}

	private static List<JTable> tablesOf(final Component component){
		final List<JTable> tables = new ArrayList<>();
		if(component instanceof JTable table)
			tables.add(table);
		else if(component instanceof Container container)
			for(final Component child : container.getComponents())
				tables.addAll(tablesOf(child));
		return tables;
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
