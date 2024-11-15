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

import io.github.mtrevisan.familylegacy.flef.helpers.parsers.DateParser;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateOriginal;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDateOriginal;


public final class HistoricDateDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 3434407293578383806L;


	private static final int TABLE_INDEX_DATE = 2;

	private static final String RECORD_PANEL_NAME_BASE = "base";
	private static final String RECORD_PANEL_NAME_OTHER = "other";


	private final JLabel dateLabel = new JLabel("Date:");
	private final JTextField dateField = new JTextField();
	private final JLabel dateOriginalLabel = new JLabel("Date original:");
	private final JTextField dateOriginalField = new JTextField();
	private final JButton calendarOriginalButton = new JButton("Calendar original", ICON_CALENDAR);
	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");


	public static HistoricDateDialog create(final Frame parent){
		final HistoricDateDialog dialog = new HistoricDateDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static HistoricDateDialog createSelectOnly(final Frame parent){
		final HistoricDateDialog dialog = new HistoricDateDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.calendarOriginalButton, dialog.noteButton, dialog.assertionButton);
		dialog.initialize();
		return dialog;
	}

	public static HistoricDateDialog createRecordOnly(final Frame parent){
		final HistoricDateDialog dialog = new HistoricDateDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private HistoricDateDialog(final Frame parent){
		super(parent);

		addButtonComponent(COMPONENT_ID_CALENDAR_ORIGINAL_BUTTON, calendarOriginalButton);
		addButtonComponent(COMPONENT_ID_NOTE_BUTTON, noteButton);
		addButtonComponent(COMPONENT_ID_ASSERTION_BUTTON, assertionButton);
	}


	public HistoricDateDialog withOnCloseGracefully(final Consumer<ModifiedRecords> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_HISTORIC_DATE;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Date"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> dateComparator = (date1, date2) -> {
			final LocalDate localDate1 = DateParser.parse(date1);
			final LocalDate localDate2 = DateParser.parse(date2);
			return localDate1.compareTo(localDate2);
		};
		return new Comparator<?>[]{numericComparator, null, dateComparator};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelUndo(dateLabel, dateField);
		GUIHelper.bindOnTextChange(dateField, this::saveData);
		addMandatoryField(dateField);

		GUIHelper.bindLabelUndo(dateOriginalLabel, dateOriginalField);
		GUIHelper.bindOnTextChange(dateOriginalField, this::saveData);
		GUIHelper.addUndoCapability(dateOriginalField);

		calendarOriginalButton.setToolTipText("Calendar original");
		calendarOriginalButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CALENDAR_ORIGINAL, this, selectedRecord)));

		GUIHelper.bindLabelUndoAutoComplete(certaintyLabel, certaintyComboBox);
		GUIHelper.bindOnSelectionChange(certaintyComboBox, this::saveData);

		GUIHelper.bindLabelUndoAutoComplete(credibilityLabel, credibilityComboBox);
		GUIHelper.bindOnSelectionChange(credibilityComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, this, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, this, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(dateLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateField, "growx,sizegroup dtf");
		recordPanelBase.add(new JPanel(), "sizegroup btn,gapleft 30,wrap related");
		recordPanelBase.add(dateOriginalLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateOriginalField, "growx,sizegroup dtf");
		recordPanelBase.add(calendarOriginalButton, "sizegroup btn,gapleft 30,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(credibilityComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add(RECORD_PANEL_NAME_BASE, recordPanelBase);
		recordTabbedPane.add(RECORD_PANEL_NAME_OTHER, recordPanelOther);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_HISTORIC_DATE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String date = extractRecordDate(record);
			final String dateOriginal = extractRecordDateOriginal(record);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(date)
				.add(dateOriginal);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(date + (dateOriginal != null? " [" + dateOriginal + "]": StringUtils.EMPTY), row,
				TABLE_INDEX_DATE);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		dateField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer historicDateID = extractRecordID(selectedRecord);
		final String date = extractRecordDate(selectedRecord);
		final String dateOriginal = extractRecordDateOriginal(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final String restriction = Repository.getRestriction(EntityManager.NODE_HISTORIC_DATE, historicDateID);

		dateField.setText(date);
		dateOriginalField.setText(dateOriginal);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));


		refreshButtonStates(historicDateID);
	}

	@Override
	public void refreshButtonStates(final int recordID){
		final String tableName = getTableName();
		final boolean hasCalendarOriginal = (extractRecordCalendarID(recordID) != null);
		setButtonSelectEnableAndBorder(calendarOriginalButton, hasCalendarOriginal);

		final boolean hasNotes = Repository.hasNotes(tableName, recordID);
		final boolean hasAssertions = Repository.hasAssertions(tableName, recordID);
		setButtonSelectEnableAndBorder(noteButton, hasNotes);
		setButtonSelectEnableAndBorder(assertionButton, hasAssertions);
	}

	private Integer extractRecordCalendarID(final Integer historicDateID){
		final Map.Entry<String, Map<String, Object>> calendarNode = Repository.findReferencedNode(
			EntityManager.NODE_HISTORIC_DATE, historicDateID,
			EntityManager.RELATIONSHIP_EXPRESSED_IN);
		if(calendarNode == null || !EntityManager.NODE_CALENDAR.equals(calendarNode.getKey()))
			return null;

		return extractRecordID(calendarNode.getValue());
	}

	@Override
	protected void clearData(){
		dateField.setText(null);
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
		if(ignoreEvents || selectedRecord == null || selectRecordOnly)
			return false;

		//read record panel:
		final String date = GUIHelper.getTextTrimmed(dateField);
		final String dateOriginal = GUIHelper.getTextTrimmed(dateOriginalField);
		final String certainty = GUIHelper.getTextTrimmed(certaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(credibilityComboBox);

		//update table:
		if(!Objects.equals(date, extractRecordDate(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					model.setValueAt(date, modelRowIndex, TABLE_INDEX_DATE);

					break;
				}
			}
		}

		insertRecordDate(selectedRecord, date);
		insertRecordDateOriginal(selectedRecord, dateOriginal);
		insertRecordCertainty(selectedRecord, certainty);
		insertRecordCredibility(selectedRecord, credibility);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int historicDate1ID = Repository.upsert(historicDate1, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("type", "gregorian");
		Repository.upsert(calendar1, EntityManager.NODE_CALENDAR);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("type", "julian");
		int calendar2ID = Repository.upsert(calendar2, EntityManager.NODE_CALENDAR);
		Repository.upsertRelationship(EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.NODE_CALENDAR, calendar2ID,
			EntityManager.RELATIONSHIP_EXPRESSED_IN, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("type", "venetan");
		Repository.upsert(calendar3, EntityManager.NODE_CALENDAR);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 2");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_FOR, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final HistoricDateDialog dialog = create(parent);
//			final HistoricDateDialog dialog = createRecordOnly(parent);
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
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_CALENDAR_ORIGINAL_BUTTON)
									? CalendarDialog.createSelectOnly(parent)
									: CalendarDialog.create(parent))
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_CALENDAR);
										Repository.upsertRelationship(EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.NODE_CALENDAR, upsertedRecordID,
											EntityManager.RELATIONSHIP_EXPRESSED_IN, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.NODE_CALENDAR, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_EXPRESSED_IN);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(historicDateID);
								});
							calendarDialog.loadData();
							final Map.Entry<String, Map<String, Object>> calendarNode = Repository.findReferencedNode(
								EntityManager.NODE_HISTORIC_DATE, historicDateID,
								EntityManager.RELATIONSHIP_EXPRESSED_IN);
							if(calendarNode != null)
								calendarDialog.selectData(extractRecordID(calendarNode.getValue()));
							else
								calendarDialog.showNewRecord();

							calendarDialog.showDialog();
						}

						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_NOTE_BUTTON)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_HISTORIC_DATE, historicDateID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_NOTE);
										Repository.upsertRelationship(EntityManager.NODE_NOTE, upsertedRecordID,
											EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_NOTE, deletedIDs.get(i),
											EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(historicDateID);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}

						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_ASSERTION_BUTTON)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_HISTORIC_DATE, historicDateID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_ASSERTION);
										Repository.upsertRelationship(EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.NODE_ASSERTION, upsertedRecordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_HISTORIC_DATE, historicDateID,
											EntityManager.NODE_ASSERTION, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_SUPPORTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(historicDateID);
								});
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}

						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + historicDateID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + historicDateID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}

						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + historicDateID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + historicDateID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_NEW -> {
							final int parentRecordID = extractRecordID(dialog.getSelectedRecord());
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = extractRecordID(container);
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_RESEARCH_STATUS);
										Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, upsertedRecordID,
											EntityManager.NODE_HISTORIC_DATE, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, deletedIDs.get(i),
											EntityManager.NODE_HISTORIC_DATE, parentRecordID,
											EntityManager.RELATIONSHIP_FOR);

									//refresh research status table
									dialog.reloadResearchStatusTable();
								});
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("New research status for " + title + " " + parentRecordID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
