/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.panels.searches;

import io.github.mtrevisan.familylegacy.flef.ui.dialogs.SearchDialog;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;

import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;


public class SearchAllPanel extends CommonSearchPanel{

	@Serial
	private static final long serialVersionUID = -352153790206338469L;

	private static final int TABLE_INDEX_IDENTIFIER = 3;

	private static final int TABLE_PREFERRED_WIDTH_TABLE_NAME = 90;


	public static SearchAllPanel create(){
		return new SearchAllPanel();
	}


	private SearchAllPanel(){
		super();


		initComponents();
	}


	private void initComponents(){
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_PANE_TITLE, 0, TABLE_PREFERRED_WIDTH_TABLE_NAME);
	}

	@Override
	public String getTableName(){
		return null;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Table", "Identifier"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, null, textComparator};
	}


	@Override
	public void loadData(){}

	public void loadData(final List<SearchAllRecord> data){
		final int length = data.size();
		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(length);
		int row = 0;
		for(int i = 0; i < length; i ++){
			final SearchAllRecord record = data.get(i);
			final Integer id = record.id();
			final String tableName = SearchDialog.getPaneTitle(record.tableName());
			final String filter = record.filter();
			final String identifier = record.identifier();

			model.setValueAt(id, row, TABLE_INDEX_ID);
			model.setValueAt(filter, row, TABLE_INDEX_FILTER);
			model.setValueAt(tableName, row, TABLE_INDEX_PANE_TITLE);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
	}

}
