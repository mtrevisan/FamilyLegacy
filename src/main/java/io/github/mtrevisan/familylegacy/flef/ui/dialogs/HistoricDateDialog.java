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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class HistoricDateDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 3434407293578383806L;

	private static final int TABLE_INDEX_RECORD_DATE = 1;

	private static final String TABLE_NAME = "historic_date";
	private static final String TABLE_NAME_ASSERTION = "assertion";


	private JLabel dateLabel;
	private JTextField dateField;
	private JButton calendarButton;
	private JLabel dateOriginalLabel;
	private JTextField dateOriginalField;
	private JButton calendarOriginalButton;
	private JLabel certaintyLabel;
	private JComboBox<String> certaintyComboBox;
	private JLabel credibilityLabel;
	private JComboBox<String> credibilityComboBox;

	private JButton noteButton;
	private JButton assertionButton;
	private JCheckBox restrictionCheckBox;


	public static HistoricDateDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new HistoricDateDialog(store, parent);
	}


	private HistoricDateDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public HistoricDateDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected DefaultTableModel getDefaultTableModel(){
		return new RecordTableModel();
	}

	@Override
	protected void initStoreComponents(){
		setTitle("Historic dates");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_DATE, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		dateLabel = new JLabel("Date:");
		dateField = new JTextField();
		calendarButton = new JButton("Calendar", ICON_CALENDAR);
		dateOriginalLabel = new JLabel("Date original:");
		dateOriginalField = new JTextField();
		calendarOriginalButton = new JButton("Calendar original", ICON_CALENDAR);
		certaintyLabel = new JLabel("Certainty:");
		certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
		credibilityLabel = new JLabel("Credibility:");
		credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

		noteButton = new JButton("Notes", ICON_NOTE);
		assertionButton = new JButton("Assertions", ICON_ASSERTION);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChangeUndo(dateLabel, dateField, evt -> saveData());
		addMandatoryField(dateField);

		calendarButton.setToolTipText("Calendar");
		calendarButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CALENDAR, TABLE_NAME, getSelectedRecord())));

		GUIHelper.bindLabelTextChangeUndo(dateOriginalLabel, dateOriginalField, evt -> saveData());
		GUIHelper.addUndoCapability(dateOriginalField);

		calendarOriginalButton.setToolTipText("Calendar original");
		calendarOriginalButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CALENDAR_ORIGINAL, TABLE_NAME, getSelectedRecord())));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(certaintyLabel, certaintyComboBox, evt -> saveData());

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(credibilityLabel, credibilityComboBox, evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(dateLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateField, "growx");
		recordPanelBase.add(calendarButton, "sizegroup btn,gapleft 30,wrap related");
		recordPanelBase.add(dateOriginalLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateOriginalField, "growx");
		recordPanelBase.add(calendarOriginalButton, "sizegroup btn,gapleft 30,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(credibilityComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String date = extractRecordDate(container);
			final String dateOriginal = extractRecordDateOriginal(container);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(date + (dateOriginal != null? " [" + dateOriginal + "]": StringUtils.EMPTY), row,
				TABLE_INDEX_RECORD_DATE);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_DATE);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final int dateID = extractRecordID(selectedRecord);
		final String date = extractRecordDate(selectedRecord);
		final Integer calendarID = extractRecordCalendarID(selectedRecord);
		final String dateOriginal = extractRecordDateOriginal(selectedRecord);
		final Integer calendarOriginalID = extractRecordCalendarOriginalID(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordAssertions = getRecords(TABLE_NAME_ASSERTION).entrySet().stream()
			.filter(entry -> dateID == extractRecordReferenceID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		dateField.setText(date);
		GUIHelper.addBorder(calendarButton, calendarID != null, DATA_BUTTON_BORDER_COLOR);
		dateOriginalField.setText(dateOriginal);
		GUIHelper.addBorder(calendarOriginalButton, calendarOriginalID != null, DATA_BUTTON_BORDER_COLOR);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(assertionButton, !recordAssertions.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		dateField.setText(null);
		GUIHelper.setDefaultBorder(calendarButton);
		dateOriginalField.setText(null);
		certaintyComboBox.setSelectedItem(null);
		credibilityComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(assertionButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		final String date = GUIHelper.getTextTrimmed(dateField);
		if(!validData(date)){
			JOptionPane.showMessageDialog(getParent(), "Date field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			dateField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String date = GUIHelper.getTextTrimmed(dateField);
		final String dateOriginal = GUIHelper.getTextTrimmed(dateOriginalField);
		final String certainty = GUIHelper.getTextTrimmed(certaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(credibilityComboBox);

		//update table:
		if(!Objects.equals(date, extractRecordDate(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(date, modelRowIndex, TABLE_INDEX_RECORD_DATE);

					break;
				}
		}

		selectedRecord.put("date", date);
		selectedRecord.put("date_original", dateOriginal);
		selectedRecord.put("certainty", certainty);
		selectedRecord.put("credibility", credibility);

		return true;
	}


	private static String extractRecordDate(final Map<String, Object> record){
		return (String)record.get("date");
	}

	private static Integer extractRecordCalendarID(final Map<String, Object> record){
		return (Integer)record.get("calendar_id");
	}

	private static String extractRecordDateOriginal(final Map<String, Object> record){
		return (String)record.get("date_original");
	}

	private static Integer extractRecordCalendarOriginalID(final Map<String, Object> record){
		return (Integer)record.get("calendar_original_id");
	}

	private static Integer extractRecordHistoricDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -1366589790847427762L;


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

		final TreeMap<Integer, Map<String, Object>> historicDates = new TreeMap<>();
		store.put(TABLE_NAME, historicDates);
		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("calendar_id", 1);
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("calendar_original_id", 2);
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		historicDates.put((Integer)historicDate1.get("id"), historicDate1);

		final TreeMap<Integer, Map<String, Object>> calendars = new TreeMap<>();
		store.put("calendar", calendars);
		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		calendars.put((Integer)calendar1.get("id"), calendar1);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("id", 2);
		calendar2.put("type", "julian");
		calendars.put((Integer)calendar2.get("id"), calendar2);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("id", 3);
		calendar3.put("type", "venetan");
		calendars.put((Integer)calendar3.get("id"), calendar3);

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
			final HistoricDateDialog dialog = create(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(historicDate1)))
				dialog.showNewRecord();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final Map<String, Object> container = editCommand.getContainer();
					final int historicDateID = extractRecordID(container);
					switch(editCommand.getType()){
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(TABLE_NAME, historicDateID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setSize(488, 386);
							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
						}
						case CALENDAR -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("calendar_id", extractRecordID(record)));
							calendarDialog.initComponents();
							calendarDialog.loadData();
							calendarDialog.selectData(extractRecordCalendarID(container));

							calendarDialog.setSize(309, 377);
							calendarDialog.setLocationRelativeTo(dialog);
							calendarDialog.setVisible(true);
						}
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("calendar_original_id", extractRecordID(record)));
							calendarDialog.initComponents();
							calendarDialog.loadData();
							calendarDialog.selectData(extractRecordCalendarOriginalID(container));

							calendarDialog.setSize(309, 377);
							calendarDialog.setLocationRelativeTo(dialog);
							calendarDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, historicDateID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", historicDateID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 474);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setSize(481, 427);
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
