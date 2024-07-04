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
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


public abstract class CommonSingletonDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonSingletonDialog.class);

	protected static final Color MANDATORY_FIELD_BACKGROUND_COLOR = Color.PINK;
	protected static final Color MANDATORY_COMBOBOX_BACKGROUND_COLOR = Color.RED;
	protected static final Color DATA_BUTTON_BORDER_COLOR = Color.BLUE;

	private static final int ICON_WIDTH_DEFAULT = 20;
	private static final int ICON_HEIGHT_DEFAULT = 20;

	//https://thenounproject.com/search/?q=cut&i=3132059
	protected static final ImageIcon ICON_OPEN_DOCUMENT = ResourceHelper.getImage("/images/openDocument.png",
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


	//record components:
	private final JPanel recordPanel = new JPanel();

	private final Map<String, TreeMap<Integer, Map<String, Object>>> store;
	protected Map<String, Object> selectedRecord;

	private Consumer<Object> onCloseGracefully;


	protected CommonSingletonDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}


	protected void setOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;
	}

	protected abstract String getTableName();

	protected void initComponents(){
		initRecordComponents();

		initLayout();
	}

	protected abstract void initRecordComponents();

	protected final void manageRestrictionCheckBox(final ItemEvent evt){
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
				newRestriction.put("reference_table", getTableName());
				newRestriction.put("reference_id", extractRecordID(selectedRecord));
				storeRestrictions.put(newRestrictionID, newRestriction);
			}
		}
		else if(recordRestriction != null)
			recordRestriction.put("restriction", "public");
	}

	//http://www.migcalendar.com/miglayout/cheatsheet.html
	protected void initLayout(){
		initRecordLayout(recordPanel);

		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(recordPanel, "grow");
	}

	protected abstract void initRecordLayout(final JComponent recordPanel);

	protected final void closeAction(final ActionEvent evt){
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

	protected abstract void loadData();

	protected final TreeMap<Integer, Map<String, Object>> getRecords(final String tableName){
		return store.computeIfAbsent(tableName, k -> new TreeMap<>());
	}

	protected static int extractNextRecordID(final TreeMap<Integer, Map<String, Object>> records){
		return (records.isEmpty()? 1: records.lastKey() + 1);
	}

	protected static int extractRecordID(final Map<String, Object> record){
		return (int)record.get("id");
	}


	protected void selectAction(){
		if(validateData()){
			okAction();

			selectedRecord = getSelectedRecord();
			if(selectedRecord == null)
				return;


			fillData();

			GUIHelper.setEnabled(recordPanel, true);
		}
	}

	//fill record panel
	protected abstract void fillData();

	/**
	 * Extracts the references from a given table based on the selectedRecord.
	 *
	 * @param fromTable	The table name to extract the references to this table from.
	 * @return	A {@link TreeMap} of matched records, with the record ID as the key and the record as the value.
	 */
	protected final TreeMap<Integer, Map<String, Object>> extractReferences(final String fromTable){
		final Map<Integer, Map<String, Object>> storeRecords = getRecords(fromTable);
		final TreeMap<Integer, Map<String, Object>> matchedRecords = new TreeMap<>();
		final int selectedRecordID = extractRecordID(selectedRecord);
		final String tableName = getTableName();
		for(final Map<String, Object> storeRecord : storeRecords.values())
			if(tableName.equals(extractRecordReferenceTable(storeRecord)) && extractRecordReferenceID(storeRecord) == selectedRecordID)
				matchedRecords.put(extractRecordID(storeRecord), storeRecord);
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

	protected final TreeMap<Integer, Map<String, Object>> extractLocalizedTextJunction(final String referenceType){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME_LOCALIZED_TEXT_JUNCTION);
		final TreeMap<Integer, Map<String, Object>> matchedRecords = new TreeMap<>();
		final int selectedRecordID = extractRecordID(selectedRecord);
		final String tableName = getTableName();
		for(final Map<String, Object> record : records.values())
			if(Objects.equals(referenceType, extractRecordReferenceType(record))
					&& tableName.equals(extractRecordReferenceTable(record))
					&& extractRecordReferenceID(record) == selectedRecordID)
				matchedRecords.put(extractRecordID(record), record);
		return matchedRecords;
	}

	private static String extractRecordReferenceType(final Map<String, Object> record){
		return (String)record.get("reference_type");
	}

	protected static Map<String, Object> getSingleElementOrNull(final NavigableMap<Integer, Map<String, Object>> store){
		return (store.isEmpty()? null: store.firstEntry().getValue());
	}

	protected Map<String, Object> getSelectedRecord(){
		return getRecords(getTableName()).get(1);
	}

	protected abstract void clearData();

	protected abstract boolean validateData();

	protected void okAction(){
		if(selectedRecord == null)
			return;

		saveData();
	}

	protected abstract void saveData();

}
