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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDateOriginal;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCalendarOriginalID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDateOriginal;


public final class HistoricDateDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 3434407293578383806L;

	private static final int TABLE_INDEX_DATE = 2;


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
	}


	public HistoricDateDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_NAME_HISTORIC_DATE;
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
		GUIHelper.bindLabelTextChangeUndo(dateLabel, dateField, this::saveData);
		addMandatoryField(dateField);

		GUIHelper.bindLabelTextChangeUndo(dateOriginalLabel, dateOriginalField, this::saveData);
		GUIHelper.addUndoCapability(dateOriginalField);

		calendarOriginalButton.setToolTipText("Calendar original");
		calendarOriginalButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CALENDAR_ORIGINAL, EntityManager.NODE_NAME_HISTORIC_DATE, selectedRecord)));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(certaintyLabel, certaintyComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(credibilityLabel, credibilityComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_NAME_HISTORIC_DATE, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_NAME_HISTORIC_DATE, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(dateLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateField, "grow,sizegroup dtf");
		recordPanelBase.add(new JPanel(), "sizegroup btn,gapleft 30,wrap related");
		recordPanelBase.add(dateOriginalLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(dateOriginalField, "grow,sizegroup dtf");
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
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_NAME_HISTORIC_DATE);

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

		if(selectRecordOnly)
			selectFirstData();
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
		final Integer calendarOriginalID = extractRecordCalendarOriginalID(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final boolean hasNotes = (Repository.findAll(EntityManager.NODE_NAME_NOTE)
			.stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_HISTORIC_DATE, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(historicDateID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasAssertions = (Repository.findAll(EntityManager.NODE_NAME_ASSERTION)
			.stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_HISTORIC_DATE, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(historicDateID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = Repository.findAll(EntityManager.NODE_NAME_RESTRICTION)
			.stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_HISTORIC_DATE, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(historicDateID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);

		dateField.setText(date);
		dateOriginalField.setText(dateOriginal);
		setButtonEnableAndBorder(calendarOriginalButton, calendarOriginalID != null);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
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
		if(ignoreEvents || selectedRecord == null)
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
		historicDate1.put("id", 1);
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("calendar_original_id", 2);
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.save(EntityManager.NODE_NAME_HISTORIC_DATE, historicDate1);

		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar1);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("id", 2);
		calendar2.put("type", "julian");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar2);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("id", 3);
		calendar3.put("type", "venetan");
		Repository.save(EntityManager.NODE_NAME_CALENDAR, calendar3);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", "historic_date");
		note2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note2);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "historic_date");
		restriction1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_RESTRICTION, restriction1);


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
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_HISTORIC_DATE, historicDateID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case CALENDAR_ORIGINAL -> {
							final CalendarDialog calendarDialog = CalendarDialog.create(parent)
								.withOnCloseGracefully((record, recordID) -> insertRecordCalendarOriginalID(container, extractRecordID(record)));
							calendarDialog.loadData();
							calendarDialog.selectData(extractRecordCalendarOriginalID(container));

							calendarDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_HISTORIC_DATE, historicDateID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.NODE_NAME_HISTORIC_DATE);
										insertRecordReferenceID(record, historicDateID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + historicDateID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + historicDateID);
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
