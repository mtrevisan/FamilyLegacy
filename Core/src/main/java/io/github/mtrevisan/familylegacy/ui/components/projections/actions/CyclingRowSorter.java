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

import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.text.Collator;
import java.util.List;
import java.util.Locale;


/**
 * A row sorter whose header click cycles through "not sorted", "ascending" and "descending" (the Swing default only toggles
 * between the last two), and that compares the text of a column with the collation of the language of the interface.
 *
 * @param <M>	The type of the model.
 */
public class CyclingRowSorter<M extends TableModel> extends TableRowSorter<M>{

	public CyclingRowSorter(final M model){
		super(model);

		installCollation();
	}


	@Override
	public void toggleSortOrder(final int column){
		final List<? extends SortKey> keys = getSortKeys();
		if(!keys.isEmpty() && keys.getFirst().getColumn() == column && keys.getFirst().getSortOrder() == SortOrder.DESCENDING)
			setSortKeys(null);
		else
			super.toggleSortOrder(column);
	}

	@Override
	public void modelStructureChanged(){
		super.modelStructureChanged();

		installCollation();
	}

	private void installCollation(){
		final M model = getModel();
		if(model == null)
			return;

		final Collator collator = Collator.getInstance(Locale.getDefault());
		for(int column = 0, count = model.getColumnCount(); column < count; column ++)
			if(model.getColumnClass(column) == String.class && getComparator(column) == null)
				setComparator(column, collator);
	}

}
