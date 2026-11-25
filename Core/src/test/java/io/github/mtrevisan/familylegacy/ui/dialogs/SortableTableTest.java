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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.components.projections.actions.SortableTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Locale;


class SortableTableTest{

	private static SortableTable table(){
		final DefaultTableModel model = new DefaultTableModel(new Object[][]{{"b", 2}, {"a", 3}, {"à", 1}}, new Object[]{"text", "number"}){
			@Override
			public Class<?> getColumnClass(final int column){
				return (column == 0? String.class: Integer.class);
			}
		};
		final SortableTable table = new SortableTable(model);
		table.setAutoCreateRowSorter(true);
		return table;
	}


	@Test
	void headerClickCyclesThroughNotSortedAscendingDescending(){
		final SortableTable table = table();
		final RowSorter<?> sorter = table.getRowSorter();

		for(int column = 0; column < 2; column ++){
			Assertions.assertTrue(sorter.getSortKeys().isEmpty());
			sorter.toggleSortOrder(column);
			Assertions.assertEquals(SortOrder.ASCENDING, sorter.getSortKeys().get(0).getSortOrder());
			sorter.toggleSortOrder(column);
			Assertions.assertEquals(SortOrder.DESCENDING, sorter.getSortKeys().get(0).getSortOrder());
			sorter.toggleSortOrder(column);
			Assertions.assertTrue(sorter.getSortKeys().isEmpty());
		}
	}

	@Test
	void textIsCollatedWithTheInterfaceLanguage(){
		final Locale previous = Locale.getDefault();
		try{
			Locale.setDefault(Locale.FRENCH);
			final SortableTable table = table();
			table.getRowSorter().toggleSortOrder(0);

			//the accented a is next to the a, not after the b
			Assertions.assertEquals(List.of("a", "à", "b"),
				List.of(table.getValueAt(0, 0), table.getValueAt(1, 0), table.getValueAt(2, 0)));
		}
		finally{
			Locale.setDefault(previous);
		}
	}

	@Test
	void modelReplacementKeepsASorter(){
		final SortableTable table = table();
		table.setModel(new DefaultTableModel(new Object[][]{{"x"}}, new Object[]{"t"}));

		Assertions.assertEquals(1, table.getRowCount());
		table.getRowSorter().toggleSortOrder(0);
		Assertions.assertEquals(1, table.getRowCount());
	}

}
