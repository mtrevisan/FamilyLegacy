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
package io.github.mtrevisan.familylegacy.flef.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;


public class GroupDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -2953401801022572404L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GroupDialog.class);

	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	/** [ms] */
	private static final int DEBOUNCE_TIME = 400;

	private static final Color DATA_BUTTON_BORDER_COLOR = Color.BLUE;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	private static final int TABLE_PREFERRED_WIDTH_RECORD_ID = 25;

	private static final int TABLE_INDEX_RECORD_ID = 0;
	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;
	private static final int TABLE_ROWS_SHOWN = 5;

	private static final int ICON_WIDTH_DEFAULT = 20;
	private static final int ICON_HEIGHT_DEFAULT = 20;

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon ICON_PHOTO = ResourceHelper.getImage("/images/photo.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	private static final ImageIcon ICON_CULTURAL_NORM = ResourceHelper.getImage("/images/culturalNorm.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);

	private static final String TABLE_NAME = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_NOTE = "note";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_RESTRICTION = "restriction";
	private static final String TABLE_NAME_MODIFICATION = "modification";


	//store components:
	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane tableScrollPane = new JScrollPane(recordTable);
	private final JButton newRecordButton = new JButton("New");
	private final JButton deleteRecordButton = new JButton("Delete");
	//record components:
	private final JTabbedPane recordTabbedPane = new JTabbedPane();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"family", "neighborhood", "fraternity", "ladies club",
		"literary society"});
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);
	private final JLabel photoCropLabel = new JLabel("Photo crop:");
	private final JTextField photoCropField = new JTextField();

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton culturalNormButton = new JButton("Cultural norm", ICON_CULTURAL_NORM);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final Debouncer<GroupDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCE_TIME);

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private Map<String, Object> selectedRecord;
	private long selectedRecordHash;

	private final Consumer<Object> onCloseGracefully;


	public GroupDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Consumer<Object> onCloseGracefully,
			final Frame parent){
		super(parent, true);

		this.store = store;
		this.onCloseGracefully = onCloseGracefully;

		initComponents();

		loadData();
	}


	private void initComponents(){
		initStoreComponents();

		initRecordComponents();

		initLayout();
	}

	private void initStoreComponents(){
		filterLabel.setLabelFor(filterField);
		GUIHelper.addUndoCapability(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(GroupDialog.this);
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
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
		recordTable.setRowSorter(sorter);
		recordTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!evt.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0)
					selectAction();
			});
		final InputMap recordTableInputMap = recordTable.getInputMap(JComponent.WHEN_FOCUSED);
		recordTableInputMap.put(GUIHelper.INSERT_STROKE, ACTION_MAP_KEY_INSERT);
		recordTableInputMap.put(GUIHelper.DELETE_STROKE, ACTION_MAP_KEY_DELETE);
		final ActionMap recordTableActionMap = recordTable.getActionMap();
		recordTableActionMap.put(ACTION_MAP_KEY_INSERT, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 1586429502313261736L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				newAction();
			}
		});
		recordTableActionMap.put(ACTION_MAP_KEY_DELETE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = -7179898605181310778L;

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

	private void initRecordComponents(){
		typeLabel.setLabelFor(typeComboBox);
		typeComboBox.setEditable(true);
		GUIHelper.addUndoCapability(typeComboBox);
		AutoCompleteDecorator.decorate(typeComboBox);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.IMAGE, getSelectedRecord())));

		photoCropLabel.setLabelFor(photoCropField);
		GUIHelper.addUndoCapability(photoCropField);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	private void manageRestrictionCheckBox(final ItemEvent evt){
		final Map<String, Object> recordRestriction = getSingleElementOrNull(extractReferences(TABLE_NAME_RESTRICTION));

		if(evt.getStateChange() == ItemEvent.SELECTED){
			if(recordRestriction != null)
				recordRestriction.put("restriction", "confidential");
			else{
				final TreeMap<Integer, Map<String, Object>> storeRestrictions = getRecords(TABLE_NAME_RESTRICTION);
				//create a new record
				final Map<String, Object> newRestriction = new HashMap<>();
				final int newRestrictionID = extractNextRecordID(storeRestrictions);
				newRestriction.put("id", newRestrictionID);
				newRestriction.put("restriction", "confidential");
				newRestriction.put("reference_table", TABLE_NAME);
				newRestriction.put("reference_id", extractRecordID(selectedRecord));
				storeRestrictions.put(newRestrictionID, newRestriction);
			}
		}
		else if(recordRestriction != null)
			recordRestriction.put("restriction", "public");
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(photoCropLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(photoCropField, "growx");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.setBorder(BorderFactory.createTitledBorder("Record"));
		GUIHelper.setEnabled(recordTabbedPane, false);
		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap paragraph");
		add(tableScrollPane, "grow,wrap related");
		add(newRecordButton, "sizegroup btn,tag add,split 2,align right");
		add(deleteRecordButton, "sizegroup btn,tag delete,gapleft 30,wrap paragraph");
		add(recordTabbedPane, "grow");
	}

	private void closeAction(final ActionEvent evt){
		if(closeAction())
			setVisible(false);
	}

	private boolean closeAction(){
		okAction();

		if(onCloseGracefully != null)
			onCloseGracefully.accept(this);

		return true;
	}

	private void loadData(){
		final SortedMap<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String identifier = extractIdentifier(extractRecordID(record.getValue()));

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	private boolean loadData(final int recordID){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
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

		LOGGER.info(TABLE_NAME + " ID {} does not exists", recordID);

		return false;
	}

	private TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	private static int extractNextRecordID(final TreeMap<Integer, Map<String, Object>> records){
		return (records.isEmpty()? 1: records.lastKey() + 1);
	}

	private static int extractRecordID(final Map<String, Object> record){
		return (int)record.get("id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (String)record.get("photo_crop");
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static String extractRecordName(final Map<String, Object> record){
		return (String)record.get("name");
	}

	private static Integer extractRecordNameID(final Map<String, Object> record){
		return (Integer)record.get("name_id");
	}

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			sorter = new TableRowSorter<>(model);
			recordTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private void selectAction(){
		okAction();

		selectedRecord = getSelectedRecord();
		if(selectedRecord == null)
			return;

		selectedRecordHash = selectedRecord.hashCode();


		fillData();


		deleteRecordButton.setEnabled(true);
	}

	//fill record panel
	private void fillData(){
		final String type = extractRecordType(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final String photoCrop = extractRecordPhotoCrop(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		typeComboBox.setSelectedItem(type);
		GUIHelper.addBorder(photoButton, photoID != null, DATA_BUTTON_BORDER_COLOR);
		photoCropField.setText(photoCrop);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		GUIHelper.setEnabled(recordTabbedPane, true);
	}

	/**
	 * Extracts the references from a given table based on the selectedRecord.
	 *
	 * @param fromTable	The table name to extract the references to this table from.
	 * @return	A {@link TreeMap} of matched records, with the record ID as the key and the record as the value.
	 */
	private TreeMap<Integer, Map<String, Object>> extractReferences(final String fromTable){
		final SortedMap<Integer, Map<String, Object>> storeRecords = getRecords(fromTable);
		final TreeMap<Integer, Map<String, Object>> matchedRecords = new TreeMap<>();
		final int selectedRecordID = extractRecordID(selectedRecord);
		for(final Map<String, Object> storeRecord : storeRecords.values())
			if(TABLE_NAME.equals(extractRecordReferenceTable(storeRecord)) && extractRecordReferenceID(storeRecord) == selectedRecordID)
				matchedRecords.put(extractRecordID(storeRecord), storeRecord);
		return matchedRecords;
	}

	private String extractIdentifier(final int selectedRecordID){
		//TODO
		final TreeMap<Integer, Map<String, Object>> storeGroupJunction = getRecords(TABLE_NAME_GROUP_JUNCTION);
		final TreeMap<Integer, Map<String, Object>> storePersonNames = getRecords(TABLE_NAME_PERSON_NAME);
		final TreeMap<Integer, Map<String, Object>> storeLocalizedTexts = getRecords(TABLE_NAME_LOCALIZED_TEXT);
		final StringJoiner identifier = new StringJoiner(" + ");
		for(final Map.Entry<Integer, Map<String, Object>> entry : storeGroupJunction.entrySet()){
			final Map<String, Object> groupElement = entry.getValue();
			final StringJoiner subIdentifier = new StringJoiner(", ");
			if(extractRecordGroupID(groupElement).equals(selectedRecordID)){
				final String referenceTable = extractRecordReferenceTable(groupElement);
				final Integer referenceID = extractRecordReferenceID(groupElement);
				if("person".equals(referenceTable)){
					for(final Map<String, Object> storePersonName : storePersonNames.values())
						if(extractRecordPersonID(storePersonName).equals(referenceID)){
							//TODO extract name
							final Integer extractRecordNameID = extractRecordNameID(storePersonName);
							final Map<String, Object> localizedText = storeLocalizedTexts.get(extractRecordNameID);
							final String name = extractRecordText(localizedText);
							subIdentifier.add(name != null? name: "?");
						}
				}
				else{
					//TODO
				}

				identifier.add(subIdentifier.toString());
			}
		}
		return identifier.toString();

		//		final Map<String, Object> storePersonNames = getRecords(TABLE_NAME).get(selectedRecordID);
//		final Integer mainRecordID = extractRecordNameID(storePersonNames);
//		final Integer alternateRecordID = extractRecordAlternateSortNameID(storePersonNames);
//		final SortedMap<Integer, Map<String, Object>> storeRecords = getRecords(TABLE_NAME_LOCALIZED_TEXT);
//		final Map<String, Object> mainRecord = storeRecords.get(mainRecordID);
//		final Map<String, Object> alternateRecord = storeRecords.get(alternateRecordID);
//		final String mainRecordText = extractRecordText(mainRecord);
//		final String alternateRecordText = extractRecordText(alternateRecord);
//		return mainRecordText + (alternateRecordText != null? " (" + alternateRecordText + ")": StringUtils.EMPTY);
	}

	private static Map<String, Object> getSingleElementOrNull(final NavigableMap<Integer, Map<String, Object>> store){
		return (store.isEmpty()? null: store.firstEntry().getValue());
	}

	private Map<String, Object> getSelectedRecord(){
		final int viewRowIndex = recordTable.getSelectedRow();
		if(viewRowIndex == -1)
			//no row selected
			return null;

		final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
		final TableModel model = recordTable.getModel();
		final Integer recordID = (Integer)model.getValueAt(modelRowIndex, TABLE_INDEX_RECORD_ID);

		return getRecords(TABLE_NAME).get(recordID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final Map<String, Object> newTable = new HashMap<>();
		final TreeMap<Integer, Map<String, Object>> storeTables = getRecords(TABLE_NAME);
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


		model.removeRow(modelRowIndex);
		getRecords(TABLE_NAME).remove(recordID);

		final Map<Integer, Map<String, Object>> storeNotes = getRecords(TABLE_NAME_NOTE);
		final SortedMap<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		for(final Integer noteID : recordNotes.keySet())
			storeNotes.remove(noteID);
		final Map<Integer, Map<String, Object>> storeRestriction = getRecords(TABLE_NAME_RESTRICTION);
		final SortedMap<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		for(final Integer restrictionID : recordRestriction.keySet())
			storeRestriction.remove(restrictionID);
		//TODO check referential integrity
		//FIXME use a database?

		//clear previously selected row
		selectedRecord = null;
	}

	private void clearData(){
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(photoButton);
		photoCropField.setText(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		restrictionCheckBox.setSelected(false);

		GUIHelper.setEnabled(recordTabbedPane, false);
		deleteRecordButton.setEnabled(false);
	}

	private void okAction(){
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
				newModification.put("reference_table", TABLE_NAME);
				newModification.put("reference_id", extractRecordID(selectedRecord));
				newModification.put("creation_date", now);
				storeModifications.put(newModificationID, newModification);
			}
			else{
				//TODO ask for a modification note
//				//show note record dialog
//				final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
//				changeNoteDialog.setTitle("Change note for " + TABLE_NAME + " " + extractRecordID(selectedRecord));
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

	private void saveData(){
		//read record panel:
		final String type = (String)typeComboBox.getSelectedItem();
		final String photoCrop = photoCropField.getText();

		selectedRecord.put("type", type);
		selectedRecord.put("photo_crop", photoCrop);
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 8927632880445915432L;


		RecordTableModel(){
			super(new String[]{"ID", "Name"}, 0);
		}

		@Override
		public final Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public final boolean isCellEditable(final int row, final int column){
			return false;
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put(TABLE_NAME, groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		group1.put("photo_id", 1);
		group1.put("photo_crop", "0 0 1 2");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "neighborhood");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put(TABLE_NAME_GROUP_JUNCTION, groupJunctions);
		final Map<String, Object> groupJunction1 = new HashMap<>();
		groupJunction1.put("id", 1);
		groupJunction1.put("group_id", 1);
		groupJunction1.put("reference_table", "person");
		groupJunction1.put("reference_id", 1);
		groupJunctions.put((Integer)groupJunction1.get("id"), groupJunction1);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 3);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "group");
		groupJunction3.put("reference_id", 1);
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put(TABLE_NAME_PERSON_NAME, personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("name_id", 1);
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("name_id", 2);
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("id", 3);
		personName3.put("person_id", 2);
		personName3.put("name_id", 3);
		personNames.put((Integer)personName3.get("id"), personName3);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);
		final Map<String, Object> localizedText3 = new HashMap<>();
		localizedText3.put("id", 3);
		localizedText3.put("text", "other name");
		localizedText3.put("locale", "en");
		localizedTexts.put((Integer)localizedText3.get("id"), localizedText3);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put(TABLE_NAME_NOTE, notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put(TABLE_NAME_RESTRICTION, restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case NAME -> {
							//TODO
						}
						case IMAGE -> {
							//TODO
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode place = editCommand.getContainer();
//							dialog.setTitle(place.getID() != null
//								? "Note " + place.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(place, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupDialog dialog = new GroupDialog(store, null, parent);
			dialog.setTitle("Persons");
			if(!dialog.loadData(GroupDialog.extractRecordID(group1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(355, 433);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}