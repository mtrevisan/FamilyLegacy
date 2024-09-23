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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class CalendarDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 9026792737072096011L;

	private static final int TABLE_INDEX_TYPE = 2;


	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);


	public static CalendarDialog create(final Frame parent){
		final CalendarDialog dialog = new CalendarDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static CalendarDialog createSelectOnly(final Frame parent){
		final CalendarDialog dialog = new CalendarDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.noteButton, dialog.assertionButton, dialog.eventButton);
		dialog.initialize();
		return dialog;
	}

	public static CalendarDialog createRecordOnly(final Frame parent){
		final CalendarDialog dialog = new CalendarDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private CalendarDialog(final Frame parent){
		super(parent);
	}


	public CalendarDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_CALENDAR;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Type"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelTextChangeUndo(typeLabel, typeField, this::saveData);
		addMandatoryField(typeField);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_CALENDAR, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_CALENDAR, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_CALENDAR, selectedRecord)));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,split 2");
		recordPanelBase.add(typeField, "grow");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(assertionButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(eventButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_CALENDAR);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String type = extractRecordType(record);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(type);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);

			row ++;
		}

		if(selectRecordOnly)
			selectFirstData();
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		typeField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer calendarID = extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_CALENDAR, calendarID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_CALENDAR, calendarID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_CALENDAR, calendarID);

		typeField.setText(type);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(eventButton, hasEvents);
	}

	@Override
	protected void clearData(){
		typeField.setText(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(eventButton);

		deleteRecordButton.setEnabled(false);
	}

	@Override
	protected boolean validateData(){
		final String type = GUIHelper.getTextTrimmed(typeField);
		if(!validData(type)){
			JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			typeField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeField);

		//update table:
		if(!Objects.equals(type, extractRecordType(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					model.setValueAt(type, modelRowIndex, TABLE_INDEX_TYPE);

					break;
				}
			}
		}

		insertRecordType(selectedRecord, type);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		Repository.save(EntityManager.NODE_CALENDAR, calendar1);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("id", 2);
		calendar2.put("type", "julian");
		Repository.save(EntityManager.NODE_CALENDAR, calendar2);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("id", 3);
		calendar3.put("type", "venetan");
		Repository.save(EntityManager.NODE_CALENDAR, calendar3);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", "calendar");
		note2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NOTE, note2);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final CalendarDialog dialog = create(parent);
//			final CalendarDialog dialog = createRecordOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(calendar3)))
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
					final int calendarID = extractRecordID(container);
					switch(editCommand.getType()){
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_CALENDAR, calendarID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_CALENDAR, calendarID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_CALENDAR, calendarID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_CALENDAR, calendarID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + calendarID);
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
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + calendarID);
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
