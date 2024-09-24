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
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCreationDate;
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

	private BiConsumer<Map<String, Object>, Integer> onCloseGracefully;

	protected Map<String, Object> selectedRecord;
	protected Integer selectedRecordID;
	protected Map<String, Object> selectedRecordLink;
	protected long selectedRecordHash;

	protected Consumer<Map<String, Object>> newRecordDefault;
	private final Collection<JTextComponent[]> mandatoryFields = new HashSet<>(0);

	protected volatile boolean ignoreEvents;


	protected CommonRecordDialog(final Frame parent){
		super(parent, true);
	}

	protected void initialize(){
		initComponents();

		initDialog();

		initLayout();
	}


	protected void setNewRecordDefault(final Consumer<Map<String, Object>> newRecordDefault){
		this.newRecordDefault = newRecordDefault;
	}

	protected void setOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		this.onCloseGracefully = onCloseGracefully;
	}

	protected abstract String getTableName();

	protected String getJunctionTableName(){
		return null;
	}

	protected abstract void initComponents();

	protected void initDialog(){
		//close dialog
		getRootPane().registerKeyboardAction(this::closeAction, GUIHelper.ESCAPE_STROKE,
			JComponent.WHEN_IN_FOCUSED_WINDOW);
		//close dialog
		getRootPane().registerKeyboardAction(this::closeActionNoModificationNote, GUIHelper.SHIFT_ESCAPE_STROKE,
			JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	protected void addValidDataListenerToMandatoryFields(final ValidDataListenerInterface validDataListener){
		for(final JTextComponent[] mandatoryFields : mandatoryFields)
			GUIHelper.addValidDataListener(validDataListener, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR, mandatoryFields);
	}

	protected final void manageRestrictionCheckBox(final ItemEvent evt){
		final List<Map<String, Object>> recordRestrictions = Repository.findReferencingNodes(EntityManager.NODE_RESTRICTION,
			getTableName(), selectedRecordID,
			EntityManager.RELATIONSHIP_FOR);
		final Map<String, Object> recordRestriction = (recordRestrictions.isEmpty()? null: recordRestrictions.getFirst());

		if(evt.getStateChange() == ItemEvent.SELECTED){
			if(recordRestriction != null)
				insertRecordRestriction(recordRestriction, EntityManager.RESTRICTION_CONFIDENTIAL);
			else{
				//create a new record
				final Map<String, Object> newRestriction = new HashMap<>();
				insertRecordRestriction(newRestriction, EntityManager.RESTRICTION_CONFIDENTIAL);
				final int newRestrictionID = Repository.save(EntityManager.NODE_RESTRICTION, newRestriction);
				Repository.upsertRelationship(getTableName(), extractRecordID(selectedRecord),
					EntityManager.NODE_RESTRICTION, newRestrictionID,
					EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
					GraphDatabaseManager.OnDeleteType.CASCADE, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
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
				onCloseGracefully.accept(selectedRecord, selectedRecordID);

			return true;
		}
		return false;
	}

	public abstract void loadData();


	protected void selectAction(){
		if(validateData()){
			okAction(true);

			final Map<String, Object> record = getSelectedRecord();
			if(record == null)
				return;

			selectedRecord = new HashMap<>(record);
			selectedRecordID = extractRecordID(record);
			selectedRecordLink = null;

			updateRecordHash();

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

	protected Map<String, Object> getSelectedRecord(){
		final List<Map<String, Object>> records = Repository.findAll(getTableName());
		return (!records.isEmpty()? records.getFirst(): null);
	}

	protected abstract void clearData();

	protected abstract boolean validateData();

	protected boolean validData(final Object field){
		return (selectedRecord == null || field instanceof Integer || field instanceof final String s && !s.isEmpty());
	}

	protected void okAction(final boolean askForModificationNote){
		if(selectedRecord == null)
			return;

		if(!ignoreEvents && (!dataHasChanged() || saveData())){
			updateRecordHash();

			//save `selectedRecord` into `store`
			final String tableName = getTableName();
			//TODO test upsert
			Repository.save(tableName, selectedRecord);
			//save `selectRecordLink` into `store`
			if(selectedRecordLink != null)
				Repository.upsertRelationship(getTableName(), extractRecordID(selectedRecord),
					//TODO node_restriction?
					EntityManager.NODE_RESTRICTION, selectedRecordID,
					getJunctionTableName(), new HashMap<>(selectedRecordLink),
					GraphDatabaseManager.OnDeleteType.CASCADE, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

			LOGGER.debug("Saved data {}", selectedRecord);
			if(selectedRecordLink != null)
				LOGGER.debug("Saved link {}", selectedRecordLink);

			//fire event only if something's changed
			EventBusService.publish(EditEvent.create(EditEvent.EditType.SEARCH, getTableName(), selectedRecord));
		}

		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());

		final String recordTableName = getTableName();
		final Integer selectedRecordID = extractRecordID(selectedRecord);
		final List<Map<String, Object>> recordModification = Repository.findReferencingNodes(EntityManager.NODE_MODIFICATION,
			recordTableName, selectedRecordID,
			EntityManager.RELATIONSHIP_FOR);
		if(recordModification.isEmpty()){
			//create a new record
			final Map<String, Object> newModification = new HashMap<>();
			insertRecordCreationDate(newModification, now);
			final int newModificationID = Repository.save(EntityManager.NODE_MODIFICATION, newModification);
			Repository.upsertRelationship(recordTableName, selectedRecordID,
				EntityManager.NODE_MODIFICATION, newModificationID,
				EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}
		else{
			if(askForModificationNote){
				//ask for a modification note
				//show note record dialog
				final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly((Frame)getParent())
					.withOnCloseGracefully((record, recordID) -> {
						if(record != null)
							Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
								EntityManager.NODE_MODIFICATION, recordID,
								EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(),
								GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
						else if(recordID != null)
							Repository.deleteNode(getTableName(), recordID);
					});
				final String title = StringUtils.capitalize(StringUtils.replace(recordTableName, "_", StringUtils.SPACE));
				changeNoteDialog.setTitle("Change note for " + title + " " + selectedRecordID);
				changeNoteDialog.showNewRecord();

				changeNoteDialog.showDialog();
			}


			//update the record with `update_date`
			final Map<String, Object> modification = recordModification.getFirst();
			insertRecordUpdateDate(modification, now);
		}
	}

	protected void updateRecordHash(){
		selectedRecordHash = Objects.hash(selectedRecord, selectedRecordLink);
	}

	private boolean dataHasChanged(){
		return (selectedRecord != null && Objects.hash(selectedRecord, selectedRecordLink) != selectedRecordHash);
	}

	protected abstract boolean saveData();

}
