/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;


public abstract class CommonListDialog extends CommonSingletonDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonListDialog.class);

	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	/** [ms] */
	private static final int DEBOUNCE_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	private static final int TABLE_PREFERRED_WIDTH_RECORD_ID = 25;

	protected static final int TABLE_INDEX_RECORD_ID = 0;
	private static final int TABLE_ROWS_SHOWN = 5;

	private static final String TABLE_NAME_MODIFICATION = "modification";


	//store components:
	private JLabel filterLabel;
	protected JTextField filterField;
	protected JTable recordTable;
	private JScrollPane tableScrollPane;
	private JButton newRecordButton;
	protected JButton deleteRecordButton;
	//record components:
	private JTabbedPane recordTabbedPane;

	private final Debouncer<CommonListDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCE_TIME);

	private int previousIndex = -1;
	private volatile boolean ignoreSelectionEvents;

	private long selectedRecordHash;


	protected CommonListDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Consumer<Object> onCloseGracefully,
			final Frame parent){
		super(store, onCloseGracefully, parent);
	}


	protected abstract DefaultTableModel getDefaultTableModel();

	@Override
	protected final void initComponents(){
		initStoreComponents();

		super.initComponents();
	}

	protected void initStoreComponents(){
		//store components:
		filterLabel = new JLabel("Filter:");
		filterField = new JTextField();
		recordTable = new JTable(getDefaultTableModel());
		tableScrollPane = new JScrollPane(recordTable);
		newRecordButton = new JButton("New");
		deleteRecordButton = new JButton("Delete");
		//record components:
		recordTabbedPane = new JTabbedPane();


		filterLabel.setLabelFor(filterField);
		GUIHelper.addUndoCapability(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(CommonListDialog.this);
			}
		});

		recordTable.setAutoCreateRowSorter(true);
		recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		recordTable.setGridColor(GRID_COLOR);
		recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordTable.setDragEnabled(true);
		recordTable.setDropMode(DropMode.INSERT_ROWS);
		recordTable.getTableHeader()
			.setFont(recordTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_RECORD_ID, 0, TABLE_PREFERRED_WIDTH_RECORD_ID);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_ID, Comparator.naturalOrder());
		recordTable.setRowSorter(sorter);
		recordTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!ignoreSelectionEvents && !evt.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0)
					selectAction();
			});
		final InputMap recordTableInputMap = recordTable.getInputMap(JComponent.WHEN_FOCUSED);
		recordTableInputMap.put(GUIHelper.INSERT_STROKE, ACTION_MAP_KEY_INSERT);
		recordTableInputMap.put(GUIHelper.DELETE_STROKE, ACTION_MAP_KEY_DELETE);
		final ActionMap recordTableActionMap = recordTable.getActionMap();
		recordTableActionMap.put(ACTION_MAP_KEY_INSERT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -6235301416538799926L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				newAction();
			}
		});
		recordTableActionMap.put(ACTION_MAP_KEY_DELETE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -898408312774616906L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});
		final Dimension viewSize = new Dimension();
		viewSize.width = recordTable.getColumnModel()
			.getTotalColumnWidth();
		viewSize.height = TABLE_ROWS_SHOWN * recordTable.getRowHeight();
		recordTable.setPreferredScrollableViewportSize(viewSize);

		newRecordButton.addActionListener(evt -> newAction());
		deleteRecordButton.setEnabled(false);
		deleteRecordButton.addActionListener(evt -> deleteAction());
	}

	@Override
	protected final void initLayout(){
		initRecordLayout(recordTabbedPane);

		recordTabbedPane.setBorder(BorderFactory.createTitledBorder("Record"));
		GUIHelper.setEnabled(recordTabbedPane, false);

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap paragraph");
		add(tableScrollPane, "grow,wrap related");
		add(newRecordButton, "sizegroup btn,tag add,split 2,align right");
		add(deleteRecordButton, "sizegroup btn,tag delete,gapleft 30,wrap paragraph");
		add(recordTabbedPane, "grow");
	}

	protected final boolean loadData(final int recordID){
		final String tableName = getTableName();
		final Map<Integer, Map<String, Object>> records = getRecords(tableName);
		if(records.containsKey(recordID)){
			final TableModel model = recordTable.getModel();
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					recordTable.setRowSelectionInterval(viewRowIndex, viewRowIndex);
					return true;
				}
			}
		}

		LOGGER.info("{} id {} does not exists", tableName, recordID);

		return false;
	}

	protected abstract void filterTableBy(final JDialog panel);


	@Override
	protected final void selectAction(){
		if(!validateData()){
			final ListSelectionModel selectionModel = recordTable.getSelectionModel();
			ignoreSelectionEvents = true;
			if(previousIndex != -1)
				selectionModel.setSelectionInterval(previousIndex, previousIndex);
			else
				selectionModel.clearSelection();
			ignoreSelectionEvents = false;
		}
		else{
			previousIndex = recordTable.getSelectedRow();

			okAction();

			selectedRecord = getSelectedRecord();
			if(selectedRecord == null)
				return;

			selectedRecordHash = selectedRecord.hashCode();


			fillData();

			GUIHelper.setEnabled(recordTabbedPane, true);

			deleteRecordButton.setEnabled(true);
		}
	}

	@Override
	protected final Map<String, Object> getSelectedRecord(){
		final int viewRowIndex = recordTable.getSelectedRow();
		if(viewRowIndex == -1)
			//no row selected
			return null;

		final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
		final TableModel model = recordTable.getModel();
		final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_RECORD_ID);

		return getRecords(getTableName()).get(recordID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final Map<String, Object> newTable = new HashMap<>();
		final TreeMap<Integer, Map<String, Object>> storeTables = getRecords(getTableName());
		final int newTableID = extractNextRecordID(storeTables);
		newTable.put("id", newTableID);
		storeTables.put(newTableID, newTable);

		//reset filter
		filterField.setText(null);

		//add to table
		final RowSorter<? extends TableModel> recordTableSorter = recordTable.getRowSorter();
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int oldSize = model.getRowCount();
		model.setRowCount(oldSize + 1);
		model.setValueAt(newTableID, oldSize, TABLE_INDEX_RECORD_ID);
		//resort rows
		recordTableSorter.setSortKeys(recordTableSorter.getSortKeys());

		//select the newly created record
		final int newRowIndex = recordTable.convertRowIndexToView(oldSize);
		recordTable.setRowSelectionInterval(newRowIndex, newRowIndex);
		//make selected row visible
		recordTable.scrollRectToVisible(recordTable.getCellRect(newRowIndex, 0, true));
	}

	private void deleteAction(){
		final int viewRowIndex = recordTable.getSelectedRow();
		final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_RECORD_ID);
		if(viewRowIndex == -1)
			//no row selected
			return;


		clearData();

		GUIHelper.setEnabled(recordTabbedPane, false);
		deleteRecordButton.setEnabled(false);


		model.removeRow(modelRowIndex);
		getRecords(getTableName()).remove(recordID);

		final Map<Integer, Map<String, Object>> storeNotes = getRecords(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		for(final Integer noteID : recordNotes.keySet())
			storeNotes.remove(noteID);
		final Map<Integer, Map<String, Object>> storeMediaJunction = getRecords(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		for(final Integer mediaJunctionID : recordMediaJunction.keySet())
			storeMediaJunction.remove(mediaJunctionID);
		final Map<Integer, Map<String, Object>> storeRestriction = getRecords(TABLE_NAME_RESTRICTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		for(final Integer restrictionID : recordRestriction.keySet())
			storeRestriction.remove(restrictionID);
		//TODO check referential integrity
		//FIXME use a database?

		//clear previously selected row
		selectedRecord = null;
	}

	@Override
	protected final void okAction(){
		if(selectedRecord == null)
			return;

		saveData();

		if(selectedRecord.hashCode() != selectedRecordHash){
			final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
			final SortedMap<Integer, Map<String, Object>> recordModification = extractReferences(TABLE_NAME_MODIFICATION);
			if(recordModification.isEmpty()){
				//create a new record
				final TreeMap<Integer, Map<String, Object>> storeModifications = getRecords(TABLE_NAME_MODIFICATION);
				final Map<String, Object> newModification = new HashMap<>();
				final int newModificationID = extractNextRecordID(storeModifications);
				newModification.put("id", newModificationID);
				newModification.put("reference_table", getTableName());
				newModification.put("reference_id", extractRecordID(selectedRecord));
				newModification.put("creation_date", now);
				storeModifications.put(newModificationID, newModification);
			}
			else{
				//TODO ask for a modification note
//				//show note record dialog
//				final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
//				changeNoteDialog.setTitle("Change note for " + getTableName() + " " + extractRecordID(selectedRecord));
//				changeNoteDialog.loadData(selectedRecord, dialog -> {
//					selectedRecord = selectedRecord;
//					selectedRecordHash = selectedRecord.hashCode();
//				});
//
//				changeNoteDialog.setSize(450, 209);
//				changeNoteDialog.setLocationRelativeTo(this);
//				changeNoteDialog.setVisible(true);


				//update the record with `update_date`
				recordModification.get(recordModification.firstKey())
					.put("update_date", now);
			}
		}
	}

}