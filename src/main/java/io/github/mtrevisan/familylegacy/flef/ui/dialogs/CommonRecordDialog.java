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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.MandatoryComboBoxEditor;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ValidDataListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCreationDate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordRestriction;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordUpdateDate;


public abstract class CommonRecordDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecordDialog.class);

	protected static final Color MANDATORY_BACKGROUND_COLOR = Color.PINK;
	protected static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
	protected static final Color DATA_BUTTON_BORDER_COLOR = Color.BLUE;
	protected static final Color NON_EXISTENT_MEDIA_BORDER_COLOR = Color.RED;

	private static final int ICON_HEIGHT_DEFAULT = 20;

	//https://thenounproject.com/search/?q=cut&i=3132059
	protected static final ImageIcon ICON_CHOOSE_DOCUMENT = ResourceHelper.getImageFixedHeight("/images/choose_document.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_OPEN_FOLDER = ResourceHelper.getImageFixedHeight("/images/open_folder.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_OPEN_LINK = ResourceHelper.getImageFixedHeight("/images/open_link.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_ASSERTION = ResourceHelper.getImageFixedHeight("/images/assertion.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CALENDAR = ResourceHelper.getImageFixedHeight("/images/calendar.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CITATION = ResourceHelper.getImageFixedHeight("/images/citation.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CULTURAL_NORM = ResourceHelper.getImageFixedHeight("/images/cultural_norm.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_EVENT = ResourceHelper.getImageFixedHeight("/images/event.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_GROUP = ResourceHelper.getImageFixedHeight("/images/group.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_MEDIA = ResourceHelper.getImageFixedHeight("/images/media.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_NOTE = ResourceHelper.getImageFixedHeight("/images/note.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PERSON = ResourceHelper.getImageFixedHeight("/images/person.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PHOTO = ResourceHelper.getImageFixedHeight("/images/photo.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PHOTO_CROP = ResourceHelper.getImageFixedHeight("/images/photo_crop.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PLACE = ResourceHelper.getImageFixedHeight("/images/place.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_REFERENCE = ResourceHelper.getImageFixedHeight("/images/reference.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_REPOSITORY = ResourceHelper.getImageFixedHeight("/images/repository.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_RESTRICTION = ResourceHelper.getImageFixedHeight("/images/restriction.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_SOURCE = ResourceHelper.getImageFixedHeight("/images/source.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_TEXT = ResourceHelper.getImageFixedHeight("/images/text.png",
		ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_TRANSLATION = ResourceHelper.getImageFixedHeight("/images/translation.png",
		ICON_HEIGHT_DEFAULT);

	protected static final Dimension MINIMUM_NOTE_TEXT_PREVIEW_SIZE = new Dimension(300, 150);


	//record components:
	protected final JPanel recordPanel = new JPanel();

	protected final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private Consumer<Map<String, Object>> onCloseGracefully;

	protected Map<String, Object> selectedRecord;
	protected Map<String, Object> selectedRecordLink;
	protected long selectedRecordHash;

	protected Consumer<Map<String, Object>> newRecordDefault;
	private final Collection<JTextComponent[]> mandatoryFields = new HashSet<>(0);

	protected volatile boolean ignoreEvents;


	protected CommonRecordDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(parent, true);

		this.store = store;
	}

	protected void initialize(){
		initComponents();

		initDialog();

		initLayout();
	}


	protected void setNewRecordDefault(final Consumer<Map<String, Object>> newRecordDefault){
		this.newRecordDefault = newRecordDefault;
	}

	protected void setOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;
	}

	protected abstract String getTableName();

	protected String getJunctionTableName(){
		return null;
	}

	protected abstract void initComponents();

	protected void initDialog(){
		//close dialog
		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		//close dialog
		getRootPane().registerKeyboardAction(this::closeActionNoModificationNote, GUIHelper.SHIFT_ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	protected void addValidDataListenerToMandatoryFields(final ValidDataListenerInterface validDataListener){
		for(final JTextComponent[] mandatoryFields : mandatoryFields)
			GUIHelper.addValidDataListener(validDataListener, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR, mandatoryFields);
	}

	protected final void manageRestrictionCheckBox(final ItemEvent evt){
		final Map<String, Object> recordRestriction = getSingleElementOrNull(extractReferences(EntityManager.TABLE_NAME_RESTRICTION));

		if(evt.getStateChange() == ItemEvent.SELECTED){
			if(recordRestriction != null)
				insertRecordRestriction(recordRestriction, EntityManager.RESTRICTION_CONFIDENTIAL);
			else{
				final NavigableMap<Integer, Map<String, Object>> storeRestrictions = getRecords(EntityManager.TABLE_NAME_RESTRICTION);
				//create a new record
				final Map<String, Object> newRestriction = new HashMap<>();
				insertRecordID(newRestriction, extractNextRecordID(storeRestrictions));
				insertRecordRestriction(newRestriction, EntityManager.RESTRICTION_CONFIDENTIAL);
				insertRecordReferenceTable(newRestriction, getTableName());
				insertRecordReferenceID(newRestriction, extractRecordID(selectedRecord));
				storeRestrictions.put(extractRecordID(newRestriction), newRestriction);
			}
		}
		else if(recordRestriction != null)
			insertRecordRestriction(recordRestriction, EntityManager.RESTRICTION_PUBLIC);
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	protected void initLayout(){
		initRecordLayout(recordPanel);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(recordPanel, "grow");

		pack();
	}

	protected abstract void initRecordLayout(final JComponent recordPanel);

	protected void addMandatoryField(final JTextComponent... fields){
		mandatoryFields.add(fields);
	}

	protected static void addMandatoryField(final JComboBox<String> comboBox){
		comboBox.setEditor(new MandatoryComboBoxEditor(comboBox, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR));
	}

	protected void setMandatoryFieldsBackgroundColor(final Color color){
		for(final JTextComponent[] mandatoryFields : mandatoryFields)
			for(int j = 0, length = mandatoryFields.length; j < length; j ++)
				mandatoryFields[j].setBackground(color);
	}

	private void closeAction(final ActionEvent evt){
		if(closeAction(true))
			setVisible(false);
	}

	private void closeActionNoModificationNote(final ActionEvent evt){
		if(closeAction(false))
			setVisible(false);
	}

	private boolean closeAction(final boolean askForModificationNote){
		if(validateData()){
			okAction(askForModificationNote);

			if(onCloseGracefully != null)
				onCloseGracefully.accept(selectedRecord);

			return true;
		}
		return false;
	}

	public abstract void loadData();

	protected final NavigableMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected final NavigableMap<Integer, Map<String, Object>> getFilteredRecords(final String tableName, final String filterReferenceTable,
			final int filterReferenceID){
		return getRecords(tableName)
			.entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceTable, extractRecordReferenceTable(entry.getValue())))
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	protected static int extractNextRecordID(final NavigableMap<Integer, Map<String, Object>> records){
		return (records.isEmpty()? 1: records.lastKey() + 1);
	}


	protected void selectAction(){
		if(validateData()){
			okAction(true);

			final Map<String, Object> record = getSelectedRecord();
			if(record == null)
				return;

			selectedRecord = new HashMap<>(record);
			selectedRecordLink = null;

			selectedRecordHash = Objects.hash(selectedRecord, selectedRecordLink);

			GUIHelper.setEnabled(recordPanel);

			if(newRecordDefault != null)
				newRecordDefault.accept(selectedRecord);

			ignoreEvents = true;
			fillData();
			ignoreEvents = false;
		}
	}

	//fill record panel
	protected abstract void fillData();

	/**
	 * Extracts the references from a given table based on the selected record.
	 *
	 * @param fromTable	The table name to extract the references to this table from.
	 * @return	A {@link TreeMap} of matched records, with the record ID as the key and the record as the value.
	 */
	protected final NavigableMap<Integer, Map<String, Object>> extractReferences(final String fromTable){
		return extractReferences(fromTable, null, null);
	}

	protected final <T> NavigableMap<Integer, Map<String, Object>> extractReferences(final String fromTable,
			final Function<Map<String, Object>, T> filter, final T filterValue){
		final NavigableMap<Integer, Map<String, Object>> matchedRecords = new TreeMap<>();
		if(selectedRecord != null){
			final Integer selectedRecordID = extractRecordID(selectedRecord);
			final String tableName = getTableName();
			final NavigableMap<Integer, Map<String, Object>> records = getRecords(fromTable);
			records.forEach((key, value) -> {
				if(((filter == null || Objects.equals(filterValue, filter.apply(value)))
						&& tableName.equals(extractRecordReferenceTable(value))
						&& Objects.equals(selectedRecordID, extractRecordReferenceID(value))))
					matchedRecords.put(key, value);
			});
		}
		return matchedRecords;
	}

	private static Map<String, Object> getSingleElementOrNull(final NavigableMap<Integer, Map<String, Object>> store){
		return (store.isEmpty()? null: store.firstEntry().getValue());
	}

	protected Map<String, Object> getSelectedRecord(){
		final NavigableMap<Integer, Map<String, Object>> records = getRecords(getTableName());
		return (!records.isEmpty()? records.sequencedValues().getFirst(): null);
	}

	protected abstract void clearData();

	protected abstract boolean validateData();

	protected boolean validData(final Object field){
		return (selectedRecord == null || field instanceof Integer || field instanceof final String s && !s.isEmpty());
	}

	protected void okAction(final boolean askForModificationNote){
		if(selectedRecord == null || !dataHasChanged())
			return;

		final Integer recordID = extractRecordID(selectedRecord);
		if(!ignoreEvents && saveData()){
			selectedRecordHash = Objects.hash(selectedRecord, selectedRecordLink);

			//save `selectedRecord` into `store`
			final String tableName = getTableName();
			store.get(tableName)
				.put(recordID, selectedRecord);
			//save `selectRecordLink` into `store`
			if(selectedRecordLink != null){
				final String junctionTableName = getJunctionTableName();
				final TreeMap<Integer, Map<String, Object>> records = store.get(junctionTableName);
				if(records != null)
					records.put(extractRecordID(selectedRecordLink), selectedRecordLink);
			}

			LOGGER.debug("Saved data {}", selectedRecord);
			if(selectedRecordLink != null){
				LOGGER.debug("Saved link {}", selectedRecordLink);
				LOGGER.debug("Store {}", store);
			}

			//fire event only if something's changed
			EventBusService.publish(EditEvent.create(EditEvent.EditType.SEARCH, getTableName(), selectedRecord));
		}

		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		final String recordTableName = getTableName();
		final SortedMap<Integer, Map<String, Object>> recordModification = extractReferences(EntityManager.TABLE_NAME_MODIFICATION);
		if(recordModification.isEmpty()){
			//create a new record
			final NavigableMap<Integer, Map<String, Object>> storeModifications = getRecords(EntityManager.TABLE_NAME_MODIFICATION);
			final Map<String, Object> newModification = new HashMap<>();
			insertRecordID(newModification, extractNextRecordID(storeModifications));
			insertRecordReferenceTable(newModification, recordTableName);
			insertRecordReferenceID(newModification, recordID);
			insertRecordCreationDate(newModification, now);
			storeModifications.put(extractRecordID(newModification), newModification);
		}
		else{
			if(askForModificationNote){
				//ask for a modification note
				//show note record dialog
				final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(store, (Frame)getParent())
					.withOnCloseGracefully(record -> {
						if(record != null){
							insertRecordReferenceTable(record, recordTableName);
							insertRecordReferenceID(record, recordID);
						}
					});
				final String title = StringUtils.capitalize(StringUtils.replace(recordTableName, "_", StringUtils.SPACE));
				changeNoteDialog.setTitle("Change note for " + title + " " + recordID);
				changeNoteDialog.showNewRecord();

				changeNoteDialog.showDialog();
			}


			//update the record with `update_date`
			final Map<String, Object> modification = recordModification.firstEntry()
				.getValue();
			insertRecordUpdateDate(modification, now);
		}
	}

	private boolean dataHasChanged(){
		return (selectedRecord != null && Objects.hash(selectedRecord, selectedRecordLink) != selectedRecordHash);
	}

	protected abstract boolean saveData();

}
