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

import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;


/**
 * A table whose automatically created sorter is a {@link CyclingRowSorter}: the header click cycles through "not sorted",
 * "ascending" and "descending", and the text is collated with the language of the interface.
 * <p>
 * Use {@code setAutoCreateRowSorter(true)} as usual; a sorter that is set explicitly should be a {@link CyclingRowSorter} too.
 */
public class SortableTable extends JTable{

	public SortableTable(){
		super();
	}

	public SortableTable(final TableModel model){
		super(model);
	}


	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setRowSorter(final RowSorter<? extends TableModel> sorter){
		if(sorter != null && sorter.getClass() == TableRowSorter.class)
			super.setRowSorter(new CyclingRowSorter(sorter.getModel()));
		else
			super.setRowSorter(sorter);
	}

}
