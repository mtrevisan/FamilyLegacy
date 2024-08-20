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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.MandatoryComboBoxEditor;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ResourceHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ValidDataListenerInterface;
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


public abstract class CommonRecordDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecordDialog.class);

	protected static final Color MANDATORY_BACKGROUND_COLOR = Color.PINK;
	protected static final Color DEFAULT_BACKGROUND_COLOR = Color.WHITE;
	protected static final Color DATA_BUTTON_BORDER_COLOR = Color.BLUE;

	private static final int ICON_WIDTH_DEFAULT = 20;
	private static final int ICON_HEIGHT_DEFAULT = 20;

	//https://thenounproject.com/search/?q=cut&i=3132059
	protected static final ImageIcon ICON_CHOOSE_DOCUMENT = ResourceHelper.getImage("/images/choose_document.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_OPEN_FOLDER = ResourceHelper.getImage("/images/open_folder.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_OPEN_LINK = ResourceHelper.getImage("/images/open_link.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_ASSERTION = ResourceHelper.getImage("/images/assertion.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CALENDAR = ResourceHelper.getImage("/images/calendar.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CITATION = ResourceHelper.getImage("/images/citation.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_CULTURAL_NORM = ResourceHelper.getImage("/images/cultural_norm.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_EVENT = ResourceHelper.getImage("/images/event.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_GROUP = ResourceHelper.getImage("/images/group.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_MEDIA = ResourceHelper.getImage("/images/media.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PERSON = ResourceHelper.getImage("/images/person.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PHOTO = ResourceHelper.getImage("/images/photo.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PHOTO_CROP = ResourceHelper.getImage("/images/photo_crop.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_PLACE = ResourceHelper.getImage("/images/place.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_REFERENCE = ResourceHelper.getImage("/images/reference.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_REPOSITORY = ResourceHelper.getImage("/images/repository.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_RESTRICTION = ResourceHelper.getImage("/images/restriction.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_TEXT = ResourceHelper.getImage("/images/text.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);
	protected static final ImageIcon ICON_TRANSLATION = ResourceHelper.getImage("/images/translation.png",
		ICON_WIDTH_DEFAULT, ICON_HEIGHT_DEFAULT);

	protected static final String TABLE_NAME_NOTE = "note";
	protected static final String TABLE_NAME_MEDIA_JUNCTION = "media_junction";
	protected static final String TABLE_NAME_RESTRICTION = "restriction";
	protected static final String TABLE_NAME_LOCALIZED_TEXT_JUNCTION = "localized_text_junction";
	private static final String TABLE_NAME_MODIFICATION = "modification";


	//record components:
	private final JPanel recordPanel = new JPanel();

	protected final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	private Consumer<Map<String, Object>> onCloseGracefully;

	protected Map<String, Object> selectedRecord;
	protected long selectedRecordHash;

	protected Consumer<Map<String, Object>> newRecordDefault;
	private final Collection<JTextComponent[]> mandatoryFields = new HashSet<>(0);

	protected volatile boolean ignoreEvents;


	protected CommonRecordDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(parent, true);

		this.store = store;


		initComponents();
	}


	protected void setNewRecordDefault(final Consumer<Map<String, Object>> newRecordDefault){
		this.newRecordDefault = newRecordDefault;
	}

	protected void setOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;
	}

	protected abstract String getTableName();

	private void initComponents(){
		initRecordComponents();

		initLayout();

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	protected void addValidDataListenerToMandatoryFields(final ValidDataListenerInterface validDataListener){
		for(final JTextComponent[] mandatoryFields : mandatoryFields)
			GUIHelper.addValidDataListener(validDataListener, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR, mandatoryFields);
	}

	protected abstract void initRecordComponents();

	protected final void manageRestrictionCheckBox(final ItemEvent evt){
		final Map<String, Object> recordRestriction = getSingleElementOrNull(extractReferences(TABLE_NAME_RESTRICTION));

		if(evt.getStateChange() == ItemEvent.SELECTED){
			if(recordRestriction != null)
				recordRestriction.put("restriction", "confidential");
			else{
				final NavigableMap<Integer, Map<String, Object>> storeRestrictions = getRecords(TABLE_NAME_RESTRICTION);
				//create a new record
				final Map<String, Object> newRestriction = new HashMap<>();
				newRestriction.put("id", extractNextRecordID(storeRestrictions));
				newRestriction.put("restriction", "confidential");
				newRestriction.put("reference_table", getTableName());
				newRestriction.put("reference_id", extractRecordID(selectedRecord));
				storeRestrictions.put(extractRecordID(newRestriction), newRestriction);
			}
		}
		else if(recordRestriction != null)
			recordRestriction.put("restriction", "public");
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
		if(closeAction())
			setVisible(false);
	}

	private boolean closeAction(){
		if(validateData()){
			okAction();

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

	protected static Integer extractRecordID(final Map<String, Object> record){
		return (record != null? (Integer)record.get("id"): null);
	}


	protected void selectAction(){
		if(validateData()){
			okAction();

			selectedRecord = getSelectedRecord();
			if(selectedRecord == null)
				return;

			selectedRecordHash = selectedRecord.hashCode();

			GUIHelper.setEnabled(recordPanel, true);

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
		final TreeMap<Integer, Map<String, Object>> matchedRecords = new TreeMap<>();
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

	protected static String extractRecordCertainty(final Map<String, Object> record){
		return (String)record.get("certainty");
	}

	protected static String extractRecordCredibility(final Map<String, Object> record){
		return (String)record.get("credibility");
	}

	protected static String extractRecordReferenceTable(final Map<String, Object> record){
		return (String)record.get("reference_table");
	}

	protected static Integer extractRecordReferenceID(final Map<String, Object> record){
		return (Integer)record.get("reference_id");
	}

	protected static String extractRecordReferenceType(final Map<String, Object> record){
		return (String)record.get("reference_type");
	}

	protected static Map<String, Object> getSingleElementOrNull(final NavigableMap<Integer, Map<String, Object>> store){
		return (store.isEmpty()? null: store.firstEntry().getValue());
	}

	protected Map<String, Object> getSelectedRecord(){
		final NavigableMap<Integer, Map<String, Object>> records = getRecords(getTableName());
		return records.sequencedValues().getFirst();
	}

	protected abstract void clearData();

	protected abstract boolean validateData();

	protected boolean validData(final Object field){
		return (selectedRecord == null || field instanceof Integer || field instanceof final String s && !s.isEmpty());
	}

	protected void okAction(){
		if(selectedRecord == null || !dataHasChanged())
			return;

		if(!ignoreEvents && saveData()){
			selectedRecordHash = selectedRecord.hashCode();

			LOGGER.debug("Saved data {}", selectedRecord);
		}

		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		final SortedMap<Integer, Map<String, Object>> recordModification = extractReferences(TABLE_NAME_MODIFICATION);
		if(recordModification.isEmpty()){
			//create a new record
			final NavigableMap<Integer, Map<String, Object>> storeModifications = getRecords(TABLE_NAME_MODIFICATION);
			final Map<String, Object> newModification = new HashMap<>();
			newModification.put("id", extractNextRecordID(storeModifications));
			newModification.put("reference_table", getTableName());
			newModification.put("reference_id", extractRecordID(selectedRecord));
			newModification.put("creation_date", now);
			storeModifications.put(extractRecordID(newModification), newModification);
		}
		else{
			//TODO ask for a modification note
//			//show note record dialog
//			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
//			changeNoteDialog.setTitle("Change note for " + getTableName() + " " + extractRecordID(selectedRecord));
//			changeNoteDialog.loadData(selectedRecord, dialog -> {
//				selectedRecord = selectedRecord;
//				selectedRecordHash = selectedRecord.hashCode();
//			});
//
//			changeNoteDialog.setVisible(true);


			//update the record with `update_date`
			recordModification.get(recordModification.firstKey())
				.put("update_date", now);
		}
	}

	protected boolean dataHasChanged(){
		return (selectedRecord != null && selectedRecord.hashCode() != selectedRecordHash);
	}

	protected abstract boolean saveData();

}
