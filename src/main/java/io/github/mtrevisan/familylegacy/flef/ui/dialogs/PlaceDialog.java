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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinateCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinateSystem;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCoordinate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCoordinateCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCoordinateSystem;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class PlaceDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -8409918543709413945L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel identifierLabel = new JLabel("Identifier:");
	private final JTextField identifierField = new JTextField();
	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();
	private final JButton transcribedNameButton = new JButton("Transcribed names", ICON_TRANSLATION);
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "nation", "province", "state", "county", "city",
		"township", "parish", "island", "archipelago", "continent", "unincorporated town", "settlement", "village", "address"});
	private final JLabel coordinateLabel = new JLabel("Coordinate:");
	private final JTextField coordinateField = new JTextField();
	private final JLabel coordinateSystemLabel = new JLabel("Coordinate system:");
	private final JComboBox<String> coordinateSystemComboBox = new JComboBox<>(new String[]{null, "WGS84", "UTM"});
	private final JLabel coordinateCredibilityLabel = new JLabel("Coordinate credibility:");
	private final JComboBox<String> coordinateCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JButton groupButton = new JButton("Groups", ICON_GROUP);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private Integer filterPlaceID;


	public static PlaceDialog create(final Frame parent){
		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static PlaceDialog createSelectOnly(final Frame parent){
		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.photoButton, dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.eventButton,
			dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static PlaceDialog createShowOnly(final Frame parent){
		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static PlaceDialog createEditOnly(final Frame parent){
		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static PlaceDialog createWithPlace(final Integer filterPlaceID, final Frame parent){
		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.filterPlaceID = filterPlaceID;
		dialog.initialize();
		return dialog;
	}


	private PlaceDialog(final Frame parent){
		super(parent);
	}


	public PlaceDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_PLACE;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator};
	}

	@Override
	protected void initStoreComponents(){
		final String capitalizedTableName = StringUtils.capitalize(getTableName());
		setTitle(filterPlaceID != null? capitalizedTableName + " ID " + filterPlaceID: StringHelper.pluralize(capitalizedTableName));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, this::saveData);
		addMandatoryField(identifierField);

		GUIHelper.bindLabelTextChangeUndo(nameLabel, nameField, this::saveData);
		addMandatoryField(nameField);
		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_PLACE_NAME, EntityManager.NODE_PLACE, selectedRecord)));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		GUIHelper.bindLabelTextChangeUndo(coordinateLabel, coordinateField, this::saveData);
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(coordinateSystemLabel, coordinateSystemComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(coordinateCredibilityLabel, coordinateCredibilityComboBox, this::saveData);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, EntityManager.NODE_PLACE, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_PLACE, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_PLACE, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_PLACE, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_PLACE, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, EntityManager.NODE_PLACE, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "grow,wrap paragraph");
		recordPanelBase.add(nameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(nameField, "grow,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "grow,wrap related");
		recordPanelBase.add(transcribedNameButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "wrap paragraph");
		recordPanelBase.add(coordinateLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(coordinateField, "sizegroup gnss,wrap related");
		recordPanelBase.add(coordinateSystemLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(coordinateSystemComboBox, "sizegroup gnss,wrap paragraph");
		recordPanelBase.add(coordinateCredibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(coordinateCredibilityComboBox, "wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(eventButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(groupButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected Map<String, Object> getSelectedRecord(){
		return (filterPlaceID != null
			? Repository.findByID(EntityManager.NODE_PLACE, filterPlaceID)
			: super.getSelectedRecord());
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_PLACE);
		if(filterPlaceID != null)
			selectAction();
		else{
			final DefaultTableModel model = getRecordTableModel();
			model.setRowCount(records.size());
			int row = 0;
			for(final Map<String, Object> record : records){
				final Integer recordID = extractRecordID(record);
				final String identifier = extractRecordIdentifier(record);
				final FilterString filter = FilterString.create()
					.add(recordID)
					.add(identifier);
				final String filterData = filter.toString();

				model.setValueAt(recordID, row, TABLE_INDEX_ID);
				model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
				model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

				row ++;
			}
		}

		if(selectRecordOnly)
			selectFirstData();
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		identifierField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer placeID = extractRecordID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String name = extractRecordName(selectedRecord);
		final String nameLocale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String coordinate = extractRecordCoordinate(selectedRecord);
		final String coordinateSystem = extractRecordCoordinateSystem(selectedRecord);
		final String coordinateCredibility = extractRecordCoordinateCredibility(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_PLACE, placeID);
		final boolean hasTranscribedNames = Repository.hasTranscriptions(EntityManager.NODE_PLACE, placeID,
			EntityManager.LOCALIZED_TEXT_TYPE_NAME);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_PLACE, placeID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_PLACE, placeID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_PLACE, placeID);
		final boolean hasGroups = Repository.hasGroups(EntityManager.NODE_PLACE, placeID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_PLACE, placeID);

		identifierField.setText(identifier);
		nameField.setText(name);
		localeField.setText(nameLocale);
		setButtonEnableAndBorder(transcribedNameButton, hasTranscribedNames);
		typeComboBox.setSelectedItem(type);
		coordinateField.setText(coordinate);
		coordinateSystemComboBox.setSelectedItem(coordinateSystem);
		coordinateCredibilityComboBox.setSelectedItem(coordinateCredibility);
		setButtonEnableAndBorder(photoButton, photoID != null);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(eventButton, hasEvents);
		setButtonEnableAndBorder(groupButton, hasGroups);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		nameField.setText(null);
		localeField.setText(null);
		GUIHelper.setDefaultBorder(transcribedNameButton);
		typeComboBox.setSelectedItem(null);
		coordinateField.setText(null);
		coordinateSystemComboBox.setSelectedItem(null);
		coordinateCredibilityComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(photoButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(eventButton);
		GUIHelper.setDefaultBorder(groupButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		if(!validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			identifierField.requestFocusInWindow();

			return false;
		}
		final String name = GUIHelper.getTextTrimmed(nameField);
		if(!validData(name)){
			JOptionPane.showMessageDialog(getParent(), "Name field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			nameField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		final String name = GUIHelper.getTextTrimmed(nameField);
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String coordinate = GUIHelper.getTextTrimmed(coordinateField);
		final String coordinateSystem = GUIHelper.getTextTrimmed(coordinateSystemComboBox);
		final String coordinateCredibility = GUIHelper.getTextTrimmed(coordinateCredibilityComboBox);

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_IDENTIFIER);

					break;
				}
		}

		insertRecordIdentifier(selectedRecord, identifier);
		insertRecordName(selectedRecord, name);
		insertRecordLocale(selectedRecord, locale);
		insertRecordType(selectedRecord, type);
		insertRecordCoordinate(selectedRecord, coordinate);
		insertRecordCoordinateSystem(selectedRecord, coordinateSystem);
		insertRecordCoordinateCredibility(selectedRecord, coordinateCredibility);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		place1.put("type", "province");
//		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		place1.put("photo_id", 1);
		place1.put("photo_crop", "0 0 10 20");
		Repository.save(EntityManager.NODE_PLACE, place1);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "/images/addPhoto.boy.jpg");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		Repository.save(EntityManager.NODE_MEDIA, media1);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", "place");
		note2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NOTE, note2);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "place");
		restriction1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_RESTRICTION, restriction1);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Integer filterPlaceID = null;
			final PlaceDialog dialog;
			if(filterPlaceID == null)
				dialog = create(parent);
			else
				dialog = createWithPlace(filterPlaceID, parent);
			dialog.loadData();
			if(filterPlaceID == null && !dialog.selectData(extractRecordID(place1)))
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
					final int placeID = extractRecordID(container);
					final Integer photoID = extractRecordPhotoID(container);
					switch(editCommand.getType()){
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case LOCALIZED_PLACE_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(parent)
								.withReference(EntityManager.NODE_PLACE, placeID, EntityManager.LOCALIZED_TEXT_TYPE_NAME)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, recordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, record, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}
						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(dialog.photoButton)
									? MediaDialog.createEditOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PLACE, placeID);
							photoDialog.loadData();
							if(photoID != null){
								//add photo manually because is not retrievable through a junction
								photoDialog.addData(container);
								photoDialog.selectData(photoID);
							}
							else
								photoDialog.showNewRecord();

							photoDialog.showDialog();
						}
//						case PHOTO_CROP -> {
//							final PhotoCropDialog photoCropDialog = (dialog.isViewOnlyComponent(dialog.photoCropButton)
//								? PhotoCropDialog.createSelectOnly(parent)
//								: PhotoCropDialog.create(parent));
//							photoCropDialog.withOnCloseGracefully((record, recordID) -> {
//								final Rectangle crop = photoCropDialog.getCrop();
//								if(crop != null){
//									final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
//									sj.add(Integer.toString(crop.x))
//										.add(Integer.toString(crop.y))
//										.add(Integer.toString(crop.width))
//										.add(Integer.toString(crop.height));
//									insertRecordPhotoCrop(container, sj.toString());
//								}
//							});
//							try{
//								if(photoID != null){
//									final String photoCrop = extractRecordPhotoCrop(container);
//									photoCropDialog.loadData(photoID, photoCrop);
//								}
//
//								photoCropDialog.setSize(420, 295);
//								photoCropDialog.showDialog();
//							}
//							catch(final IOException ignored){}
//						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case GROUP -> {
							final GroupDialog groupDialog = (dialog.isViewOnlyComponent(dialog.groupButton)
									? GroupDialog.createSelectOnly(parent)
									: GroupDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + placeID);
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
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + placeID);
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
