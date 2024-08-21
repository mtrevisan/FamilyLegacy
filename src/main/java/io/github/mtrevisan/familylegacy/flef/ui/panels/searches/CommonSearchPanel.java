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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.SearchParser;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;


public abstract class CommonSearchPanel extends JPanel implements FilteredTablePanelInterface{

	@Serial
	private static final long serialVersionUID = 2064570563728935886L;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	protected static final int TABLE_PREFERRED_WIDTH_ID = 25;

	protected static final int TABLE_INDEX_ID = 0;
	protected static final int TABLE_INDEX_FILTER = 1;
	protected static final int TABLE_INDEX_TABLE_NAME = 2;
	private static final int TABLE_ROWS_SHOWN = 15;

	protected static final String NO_DATA = StringUtils.EMPTY;


	protected JTable recordTable;
	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;

	private RecordListenerInterface linkListener;

	protected final List<SearchAllRecord> tableData = new ArrayList<>();


	protected CommonSearchPanel(final Map<String, TreeMap<Integer, Map<String, Object>>> store){
		this.store = store;


		initComponents();
	}


	protected final void setLinkListener(final RecordListenerInterface linkListener){
		this.linkListener = linkListener;
	}

	private void initComponents(){
		//store components:
		recordTable = new JTable(new DefaultTableModel(getTableColumnNames(), 0){
			@Serial
			private static final long serialVersionUID = -8796353396676510000L;

			@Override
			public Class<?> getColumnClass(final int column){
				return String.class;
			}

			@Override
			public boolean isCellEditable(final int row, final int column){
				return false;
			}
		});


		recordTable.setFocusable(true);
		recordTable.setGridColor(GRID_COLOR);
		TableHelper.setColumnFixedWidth(recordTable, TABLE_INDEX_ID, TABLE_PREFERRED_WIDTH_ID);

		//define selection
		recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && recordTable.getSelectedRow() >= 0)
					selectAction();
			}
		});

		//add sorter
		recordTable.setAutoCreateRowSorter(true);
		final Comparator<?>[] comparators = getTableColumnComparators();
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(getRecordTableModel());
		final int columnCount = recordTable.getColumnCount();
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final Comparator<?> comparator = comparators[columnIndex];

			if(comparator != null)
				sorter.setComparator(columnIndex, comparator);
		}
		recordTable.setRowSorter(sorter);

		final TableColumnModel columnModel = recordTable.getColumnModel();
		final EmptyBorder cellBorder = new EmptyBorder(new Insets(2, 5, 2, 5));
		final int[] alignments = getTableColumnAlignments();
		final JTableHeader tableHeader = recordTable.getTableHeader();
		tableHeader.setReorderingAllowed(false);
		final Font tableFont = recordTable.getFont();
		final Font headerFont = tableFont.deriveFont(Font.BOLD);
		tableHeader.setDefaultRenderer(new DefaultTableCellRenderer(){
			@Override
			public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
				final Component headerCell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				String cellText = null;
				if(value != null){
					cellText = value.toString();
					final FontMetrics fm = headerCell.getFontMetrics(tableFont);
					final int textWidth = fm.stringWidth(cellText);
					final Insets insets = ((JComponent)headerCell).getInsets();
					final int cellWidth = columnModel.getColumn(column).getWidth()
						- insets.left - insets.right;

					if(textWidth <= cellWidth)
						cellText = null;
				}
				setToolTipText(cellText);

				setBorder(cellBorder);

				final int alignment = alignments[table.convertColumnIndexToModel(column)];
				setHorizontalAlignment(alignment);
				setFont(headerFont);

				return headerCell;
			}
		});

		//add tooltip
		for(int columnIndex = 0; columnIndex < columnCount; columnIndex ++){
			final TableColumn column = columnModel.getColumn(columnIndex);
			final int alignment = alignments[columnIndex];

			column.setCellRenderer(new DefaultTableCellRenderer(){
				@Override
				public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
					final boolean hasFocus, final int row, final int column){
					final Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

					String cellText = null;
					if(value != null){
						cellText = value.toString();
						final FontMetrics fm = cell.getFontMetrics(tableFont);
						final int textWidth = fm.stringWidth(cellText);
						final Insets insets = ((JComponent)cell).getInsets();
						final int cellWidth = columnModel.getColumn(column).getWidth()
							- insets.left - insets.right;

						if(textWidth <= cellWidth)
							cellText = null;
					}
					setToolTipText(cellText);

					setBorder(cellBorder);

					setHorizontalAlignment(alignment);

					return cell;
				}
			});
		}

		//hide filter column
		final TableColumn hiddenColumn = columnModel.getColumn(TABLE_INDEX_FILTER);
		columnModel.removeColumn(hiddenColumn);

		final Dimension viewSize = new Dimension();
		viewSize.width = columnModel
			.getTotalColumnWidth();
		viewSize.height = TABLE_ROWS_SHOWN * recordTable.getRowHeight();
		recordTable.setPreferredScrollableViewportSize(viewSize);


		initLayout();
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(new JScrollPane(recordTable), "grow");
	}

	protected abstract String getTableName();

	protected abstract String[] getTableColumnNames();

	protected abstract int[] getTableColumnAlignments();

	protected abstract Comparator<?>[] getTableColumnComparators();

	protected DefaultTableModel getRecordTableModel(){
		return (DefaultTableModel)recordTable.getModel();
	}

	private void selectAction(){
		if(linkListener != null){
			String tableName = getTableName();
			final int viewRowIndex = recordTable.getSelectedRow();
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
			final TableModel model = getRecordTableModel();
			final Integer recordIdentifier = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_ID);
			if(tableName == null){
				tableName = (String)model.getValueAt(modelRowIndex, TABLE_INDEX_TABLE_NAME);
				tableName = SearchDialog.getTableName(tableName);
			}

			linkListener.onRecordSelected(tableName, recordIdentifier);
		}
	}


	public abstract void loadData();

	public List<SearchAllRecord> exportTableData(){
		return tableData;
	}

	@Override
	public void setFilterAndSorting(final String filterText, final List<Map.Entry<String, String>> additionalFields){
		//extract filter
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(filterText, TABLE_INDEX_FILTER);
		//extract ordering
		final List<RowSorter.SortKey> sortKeys = SearchParser.getSortKeys(additionalFields, getTableColumnNames());

		//apply filter and ordering
		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
		sorter.setSortKeys(!sortKeys.isEmpty()? sortKeys: null);
	}


	protected NavigableMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

}
