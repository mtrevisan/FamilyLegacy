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

import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;


public class LocalizedTextDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -8409918543709413945L;

	private static final Logger LOGGER = LoggerFactory.getLogger(LocalizedTextDialog.class);

	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	/** [ms] */
	private static final int DEBOUNCE_TIME = 400;

	private static final Color MANDATORY_FIELD_BACKGROUND_COLOR = Color.PINK;

	private static final Color GRID_COLOR = new Color(230, 230, 230);
	private static final int TABLE_PREFERRED_WIDTH_RECORD_ID = 25;

	private static final int TABLE_INDEX_RECORD_ID = 0;
	private static final int TABLE_INDEX_RECORD_TEXT = 1;
	private static final int TABLE_ROWS_SHOWN = 5;

	private static final String TABLE_NAME = "localized_text";
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
	private final JLabel textLabel = new JLabel("Text:");
	private final JTextField textField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{"original", "transliteration", "translation"});
	private final JLabel transcriptionLabel = new JLabel("Transcription:");
	private final JComboBox<String> transcriptionComboBox = new JComboBox<>(new String[]{"IPA", "Wade-Giles", "hanyu pinyin",
		"wāpuro rōmaji", "kana", "hangul"});
	private final JLabel transcriptionTypeLabel = new JLabel("Transcription type:");
	private final JComboBox<String> transcriptionTypeComboBox = new JComboBox<>(new String[]{"romanized", "anglicized", "cyrillized",
		"francized", "gairaigized", "latinized"});

	private final Debouncer<LocalizedTextDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCE_TIME);

	private int previousIndex = -1;
	private volatile boolean ignoreSelectionEvents;

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private Map<String, Object> selectedRecord;
	private long selectedRecordHash;

	private final Consumer<Object> onCloseGracefully;


	public LocalizedTextDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Consumer<Object> onCloseGracefully,
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
				filterDebouncer.call(LocalizedTextDialog.this);
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
		sorter.setComparator(TABLE_INDEX_RECORD_TEXT, Comparator.naturalOrder());
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
			private static final long serialVersionUID = 2681917388034333400L;

			@Override
			public void actionPerformed(final ActionEvent evt){
				newAction();
			}
		});
		recordTableActionMap.put(ACTION_MAP_KEY_DELETE, new AbstractAction(){
			@Serial
			private static final long serialVersionUID = 5777359370647888434L;

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
		textLabel.setLabelFor(textField);
		GUIHelper.addUndoCapability(textField);
		GUIHelper.addBackground(textField, MANDATORY_FIELD_BACKGROUND_COLOR);

		localeLabel.setLabelFor(localeField);
		GUIHelper.addUndoCapability(localeField);

		typeLabel.setLabelFor(typeComboBox);
		typeComboBox.setEditable(true);
		GUIHelper.addUndoCapability(typeComboBox);

		transcriptionLabel.setLabelFor(transcriptionComboBox);
		transcriptionComboBox.setEditable(true);
		GUIHelper.addUndoCapability(transcriptionComboBox);

		transcriptionTypeLabel.setLabelFor(transcriptionTypeComboBox);
		transcriptionTypeComboBox.setEditable(true);
		GUIHelper.addUndoCapability(transcriptionTypeComboBox);
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	private void initLayout(){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(textLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(textField, "growx,wrap paragraph");
		recordPanelBase.add(localeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(localeField, "growx,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(transcriptionComboBox, "growx,wrap paragraph");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "growx");

		recordTabbedPane.setBorder(BorderFactory.createTitledBorder("Record"));
		GUIHelper.setEnabled(recordTabbedPane, false);
		recordTabbedPane.add("base", recordPanelBase);

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
		if(validateData()){
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			return true;
		}
		return false;
	}

	private void loadData(){
		final SortedMap<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String identifier = extractRecordText(record.getValue());

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_TEXT);

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

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordTranscription(final Map<String, Object> record){
		return (String)record.get("transcription");
	}

	private static String extractRecordTranscriptionType(final Map<String, Object> record){
		return (String)record.get("transcription_type");
	}

	private static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	private static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	private void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID, TABLE_INDEX_RECORD_TEXT);

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
		if(!validateData()){
			final ListSelectionModel selectionModel = recordTable.getSelectionModel();
			ignoreSelectionEvents = true;
			if(previousIndex != -1)
				selectionModel.setSelectionInterval(previousIndex, previousIndex);
			else
				selectionModel.clearSelection();
			ignoreSelectionEvents = false;

			return;
		}
		else
			previousIndex = recordTable.getSelectedRow();

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
		final String text = extractRecordText(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		textField.setText(text);
		localeField.setText(locale);
		typeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);

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

		//TODO check referential integrity
		//FIXME use a database?

		//clear previously selected row
		selectedRecord = null;
	}

	private void clearData(){
		textField.setText(null);
		GUIHelper.addBackground(textField, Color.WHITE);

		localeField.setText(null);

		typeComboBox.setSelectedItem(null);

		transcriptionComboBox.setSelectedItem(null);

		transcriptionTypeComboBox.setSelectedItem(null);

		GUIHelper.setEnabled(recordTabbedPane, false);
		deleteRecordButton.setEnabled(false);
	}

	private boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String text = GUIHelper.readTextTrimmed(textField);
			//enforce non-nullity on `identifier`
			if(text == null || text.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Text field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				textField.requestFocusInWindow();

				return false;
			}
		}
		return true;
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
		final String text = GUIHelper.readTextTrimmed(textField);
		final String locale = GUIHelper.readTextTrimmed(localeField);
		final String type = (String)typeComboBox.getSelectedItem();
		final String transcription = (String)transcriptionComboBox.getSelectedItem();
		final String transcriptionType = (String)transcriptionTypeComboBox.getSelectedItem();

		//update table
		if(!Objects.equals(text, extractRecordText(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
					model.setValueAt(text, modelRowIndex, TABLE_INDEX_RECORD_TEXT);
					break;
				}
		}

		selectedRecord.put("text", text);
		selectedRecord.put("locale", locale);
		selectedRecord.put("type", type);
		selectedRecord.put("transcription", transcription);
		selectedRecord.put("transcription_type", transcriptionType);
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -2557082779637153562L;


		RecordTableModel(){
			super(new String[]{"ID", "Date"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> texts = new TreeMap<>();
		store.put(TABLE_NAME, texts);
		final Map<String, Object> text1 = new HashMap<>();
		text1.put("id", 1);
		text1.put("text", "text 1");
		text1.put("locale", "en");
		text1.put("type", "original");
		text1.put("transcription", "IPA");
		text1.put("transcription_type", "romanized");
		texts.put((Integer)text1.get("id"), text1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			};
			EventBusService.subscribe(listener);

			final LocalizedTextDialog dialog = new LocalizedTextDialog(store, null, parent);
			dialog.setTitle("Localized texts");
			if(!dialog.loadData(LocalizedTextDialog.extractRecordID(text1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(420, 492);
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