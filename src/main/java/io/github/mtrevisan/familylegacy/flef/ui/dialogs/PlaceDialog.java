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
import io.github.mtrevisan.familylegacy.flef.ui.panels.BelongsToGroupPanel;
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
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinate;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinateCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCoordinateSystem;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
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
	public static final int TABLE_INDEX_DATA = 3;

	private static final String RECORD_PANEL_NAME_BASE = "base";
	private static final String RECORD_PANEL_NAME_OTHER = "other";


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

	public static PlaceDialog createCollection(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
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

	public static PlaceDialog createCollectionViewOnly(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final PlaceDialog dialog = new PlaceDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.showCollectionOnly = true;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
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

		addButtonComponent(COMPONENT_ID_TRANSCRIBED_NAME_BUTTON, transcribedNameButton);
		addButtonComponent(COMPONENT_ID_PHOTO_BUTTON, photoButton);
		addButtonComponent(COMPONENT_ID_NOTE_BUTTON, noteButton);
		addButtonComponent(COMPONENT_ID_MEDIA_BUTTON, mediaButton);
		addButtonComponent(COMPONENT_ID_ASSERTION_BUTTON, assertionButton);
		addButtonComponent(COMPONENT_ID_EVENT_BUTTON, eventButton);
		addButtonComponent(COMPONENT_ID_GROUP_BUTTON, groupButton);
	}


	public PlaceDialog withOnCloseGracefully(final Consumer<ModifiedRecords> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_PLACE;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier", "Data"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, null};
	}

	@Override
	protected void initStoreComponents(){
		final String capitalizedTableName = StringUtils.capitalize(getTableName());
		setTitle(filterPlaceID != null? capitalizedTableName + " ID " + filterPlaceID: StringHelper.pluralize(capitalizedTableName));

		super.initStoreComponents();

		//hide data column
		TableColumnModel columnModel = recordTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));

		//hide data column
		columnModel = collectionTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelUndo(identifierLabel, identifierField);
		GUIHelper.bindOnTextChange(identifierField, this::saveData);
		addMandatoryField(identifierField);

		GUIHelper.bindLabelUndo(nameLabel, nameField);
		GUIHelper.bindOnTextChange(nameField, this::saveData);
		addMandatoryField(nameField);
		GUIHelper.bindLabelUndo(localeLabel, localeField);
		GUIHelper.bindOnTextChange(localeField, this::saveData);

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_PLACE_NAME, this, selectedRecord)));

		GUIHelper.bindLabelUndoAutoComplete(typeLabel, typeComboBox);
		GUIHelper.bindOnSelectionChange(typeComboBox, this::saveData);

		GUIHelper.bindLabelUndo(coordinateLabel, coordinateField);
		GUIHelper.bindOnTextChange(coordinateField, this::saveData);
		GUIHelper.bindLabelUndoAutoComplete(coordinateSystemLabel, coordinateSystemComboBox);
		GUIHelper.bindOnSelectionChange(coordinateSystemComboBox, this::saveData);

		GUIHelper.bindLabelUndoAutoComplete(coordinateCredibilityLabel, coordinateCredibilityComboBox);
		GUIHelper.bindOnSelectionChange(coordinateCredibilityComboBox, this::saveData);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, this, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, this, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, this, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, this, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, this, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, this, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "growx,wrap paragraph");
		recordPanelBase.add(nameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(nameField, "growx,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "growx,wrap related");
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

		recordTabbedPane.add(RECORD_PANEL_NAME_BASE, recordPanelBase);
		recordTabbedPane.add(RECORD_PANEL_NAME_OTHER, recordPanelOther);
	}

	@Override
	public Map<String, Object> getSelectedRecord(){
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
			final DefaultTableModel collectionModel = (useCollection() && !collections.isEmpty()? getCollectionTableModel(): null);
			if(collectionModel != null)
				collectionModel.setRowCount(collections.size());
			int recordRow = 0;
			int collectionRow = 0;
			for(final Map<String, Object> record : records){
				final Integer recordID = extractRecordID(record);
				final String identifier = extractRecordIdentifier(record);
				final FilterString filter = FilterString.create()
					.add(recordID)
					.add(identifier);
				final String filterData = filter.toString();

				model.setValueAt(recordID, recordRow, TABLE_INDEX_ID);
				model.setValueAt(filterData, recordRow, TABLE_INDEX_FILTER);
				model.setValueAt(identifier, recordRow, TABLE_INDEX_IDENTIFIER);

				if(collectionModel != null && collections.containsKey(recordID)){
					final List<Map<String, Object>> relationships = Repository.findRelationships(EntityManager.NODE_PLACE, recordID,
						EntityManager.NODE_GROUP, filterCollectionTargetID,
						EntityManager.RELATIONSHIP_BELONGS_TO
					);
					final Map<String, Object> relationshipData = (!relationships.isEmpty()? relationships.getFirst(): new HashMap<>(0));

					collectionModel.setValueAt(recordID, collectionRow, TABLE_INDEX_ID);
//					collectionModel.setValueAt(filterData, collectionRow, TABLE_INDEX_FILTER);
					collectionModel.setValueAt(identifier, collectionRow, TABLE_INDEX_IDENTIFIER);
					collectionModel.setValueAt(relationshipData, collectionRow, TABLE_INDEX_DATA);

					collectionRow ++;
				}

				recordRow ++;
			}
		}
	}

	@Override
	public void loadDataWithCollection(final int recordID){
		loadCollections(recordID);
		loadData();
	}

	@Override
	protected void addToCollection(){
		final JDialog dialog = new JDialog(this, "Relationship data", true);
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final Integer recordID = extractRecordID(selectedRecord);
		final BelongsToGroupPanel panel = BelongsToGroupPanel.create(getTableName(), filterCollectionTargetID);
		panel.loadData(recordID);
		dialog.add(panel);

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		//close dialog
		final ActionListener onCloseAction = e -> {
			final Map<String, Object> relationshipData = panel.getRelationshipData();

			//transfer selected record to collection table:
			final DefaultTableModel model = getCollectionTableModel();
			int row = model.getRowCount();
			model.setRowCount(row + 1);
			final String identifier = extractRecordIdentifier(selectedRecord);
//			final FilterString filter = FilterString.create()
//				.add(recordID)
//				.add(identifier);
//			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
	//			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(relationshipData, row, TABLE_INDEX_DATA);


			finalizeAddToCollection(recordID, relationshipData);

			dialog.dispose();
		};
		dialog.getRootPane()
			.registerKeyboardAction(onCloseAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		dialog.setVisible(true);
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
		final String restriction = Repository.getRestriction(EntityManager.NODE_PLACE, placeID);

		identifierField.setText(identifier);
		nameField.setText(name);
		localeField.setText(nameLocale);
		typeComboBox.setSelectedItem(type);
		coordinateField.setText(coordinate);
		coordinateSystemComboBox.setSelectedItem(coordinateSystem);
		coordinateCredibilityComboBox.setSelectedItem(coordinateCredibility);

		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));


		refreshButtonStates(placeID);
	}

	@Override
	public void refreshButtonStates(final int recordID){
		final String tableName = getTableName();
		final boolean hasTranscribedNames = Repository.hasTranscriptions(tableName, recordID,
			EntityManager.LOCALIZED_TEXT_TYPE_NAME);
		final boolean hasPhoto = (Repository.getDepiction(tableName, recordID) != null);
		setButtonSelectEnableAndBorder(transcribedNameButton, hasTranscribedNames);
		setButtonSelectEnableAndBorder(photoButton, hasPhoto);

		final boolean hasNotes = Repository.hasNotes(tableName, recordID);
		final boolean hasMedia = Repository.hasMedia(tableName, recordID);
		final boolean hasAssertions = Repository.hasAssertions(tableName, recordID);
		final boolean hasEvents = Repository.hasEvents(tableName, recordID);
		final boolean hasGroups = Repository.hasGroups(tableName, recordID);
		setButtonSelectEnableAndBorder(noteButton, hasNotes);
		setButtonSelectEnableAndBorder(mediaButton, hasMedia);
		setButtonSelectEnableAndBorder(assertionButton, hasAssertions);
		setButtonSelectEnableAndBorder(eventButton, hasEvents);
		setButtonSelectEnableAndBorder(groupButton, hasGroups);
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
		if(ignoreEvents || selectedRecord == null || selectRecordOnly)
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
		place1.put("identifier", "place");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		place1.put("type", "province");
		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		place1.put("photo_crop", "0 0 10 20");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "/images/addPhoto.boy.jpg");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		int media1ID = Repository.upsert(media1, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_PLACE, place1ID,
			EntityManager.NODE_MEDIA, media1ID,
			EntityManager.RELATIONSHIP_DEPICTED_BY, media1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "18 OCT 2000");
		int date1ID = Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 1");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_FOR, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


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
					final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_PLACE, placeID);
					final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
					switch(editCommand.getType()){
						case LOCALIZED_PLACE_NAME -> {
							final LocalizedTextDialog localizedTextDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_TRANSCRIBED_NAME_BUTTON)
									? LocalizedTextDialog.createSimpleTextSelectOnly(parent)
									: LocalizedTextDialog.createSimpleText(parent))
								.withReference(EntityManager.NODE_PLACE, placeID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_LOCALIZED_TEXT);
										Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, upsertedRecordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_LOCALIZED_TEXT, deletedIDs.get(i),
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}

						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PHOTO_BUTTON)
									? MediaDialog.createSelectOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_PLACE, placeID,
											EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_PLACE, placeID,
											EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_DEPICTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							photoDialog.loadData();
							boolean selected = false;
							if(photoID != null)
								selected = photoDialog.selectData(photoID);
							if(!selected)
								photoDialog.showNewRecord();

							photoDialog.showDialog();
						}

						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_NOTE_BUTTON)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_NOTE);
										Repository.upsertRelationship(EntityManager.NODE_NOTE, upsertedRecordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_NOTE, deletedIDs.get(i),
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}

						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_MEDIA_BUTTON)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_ASSERTION_BUTTON)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_ASSERTION);
										Repository.upsertRelationship(EntityManager.NODE_PLACE, placeID,
											EntityManager.NODE_ASSERTION, upsertedRecordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_PLACE, placeID,
											EntityManager.NODE_ASSERTION, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_SUPPORTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}

						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_EVENT_BUTTON)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_PLACE, placeID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_EVENT);
										Repository.upsertRelationship(EntityManager.NODE_EVENT, upsertedRecordID,
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_EVENT, deletedIDs.get(i),
											EntityManager.NODE_PLACE, placeID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(placeID);
								});
							eventDialog.loadData();

							eventDialog.showDialog();
						}

						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.createShowOnly(parent)
								.withReference(EntityManager.NODE_PLACE, placeID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}

						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + placeID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + placeID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}

						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + placeID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + placeID);
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
											EntityManager.NODE_PLACE, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, deletedIDs.get(i),
											EntityManager.NODE_PLACE, parentRecordID,
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
