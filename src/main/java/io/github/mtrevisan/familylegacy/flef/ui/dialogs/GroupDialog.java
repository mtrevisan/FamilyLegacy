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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
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
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class GroupDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -2953401801022572404L;


	private static final int TABLE_PREFERRED_WIDTH_CATEGORY = 70;

	private static final int TABLE_INDEX_CATEGORY = 2;
	private static final int TABLE_INDEX_IDENTIFIER = 3;
	public static final int TABLE_INDEX_DATA = 4;

	private static final String NO_DATA = "?";

	private static final String RECORD_PANEL_NAME_BASE = "base";
	private static final String RECORD_PANEL_NAME_OTHER = "other";
	private static final String RECORD_PANEL_NAME_LINK = "link";


	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "family", "neighborhood", "fraternity", "ladies club",
		"literary society"});
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);
	private final JButton peopleGroupButton = new JButton("People", ICON_PERSON);
	private final JButton groupsGroupButton = new JButton("Groups", ICON_GROUP);
	private final JButton placesGroupButton = new JButton("Places", ICON_PLACE);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JButton groupButton = new JButton("Groups", ICON_EVENT);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private String filterCategory;
	private String filterReferenceTable;
	private int filterReferenceID;


	public static GroupDialog create(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createCollection(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createSelectOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.photoButton, dialog.peopleGroupButton, dialog.groupsGroupButton, dialog.placesGroupButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.culturalNormButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createSelectOnly(final String filterCategory,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent)
			.withCategory(filterCategory);
		dialog.selectRecordOnly = true;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), 0);
		dialog.addViewOnlyComponents(dialog.photoButton, dialog.peopleGroupButton, dialog.groupsGroupButton, dialog.placesGroupButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.culturalNormButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createCollectionViewOnly(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.showCollectionOnly = true;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
		dialog.addViewOnlyComponents(dialog.photoButton, dialog.peopleGroupButton, dialog.groupsGroupButton, dialog.placesGroupButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.culturalNormButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createShowOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createEditOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createEditOnly(final String filterCategory,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent)
			.withCategory(filterCategory);
		dialog.showRecordOnly = true;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), 0);
		dialog.initialize();
		return dialog;
	}


	private GroupDialog(final Frame parent){
		super(parent);

		addButtonComponent(COMPONENT_ID_PHOTO_BUTTON, photoButton);
		addButtonComponent(COMPONENT_ID_PERSON_GROUP_BUTTON, peopleGroupButton);
		addButtonComponent(COMPONENT_ID_GROUP_GROUP_BUTTON, groupsGroupButton);
		addButtonComponent(COMPONENT_ID_PLACE_GROUP_BUTTON, placesGroupButton);
		addButtonComponent(COMPONENT_ID_NOTE_BUTTON, noteButton);
		addButtonComponent(COMPONENT_ID_MEDIA_BUTTON, mediaButton);
		addButtonComponent(COMPONENT_ID_ASSERTION_BUTTON, assertionButton);
		addButtonComponent(COMPONENT_ID_CULTURAL_NORM_BUTTON, culturalNormButton);
		addButtonComponent(COMPONENT_ID_EVENT_BUTTON, eventButton);
		addButtonComponent(COMPONENT_ID_GROUP_BUTTON, groupButton);
	}


	public GroupDialog withOnCloseGracefully(final Consumer<ModifiedRecords> onCloseGracefully){
		Consumer<ModifiedRecords> innerOnCloseGracefully = modifiedRecords -> {
			if(filterReferenceTable != null){
				if(selectedRecord != null)
					for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords())
						Repository.upsert(upsertedRecord, EntityManager.NODE_GROUP);
				final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
				for(int i = 0, length = deletedIDs.size(); i < length; i ++)
					Repository.deleteRelationship(filterReferenceTable, filterReferenceID,
						EntityManager.NODE_GROUP, deletedIDs.get(i),
						EntityManager.RELATIONSHIP_BELONGS_TO);
			}
		};
		if(onCloseGracefully != null)
			innerOnCloseGracefully = innerOnCloseGracefully.andThen(onCloseGracefully);

		setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public GroupDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	/**
	 * Sets the category filter (the children category of the group) for the dialog and updates the dialog title accordingly.
	 *
	 * @param category	The category to filter by. If {@code null}, no category filter is applied.
	 * @return	This instance.
	 */
	public GroupDialog withCategory(final String category){
		filterCategory = category;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterCategory != null? " filtered by " + filterCategory: StringUtils.EMPTY));

		return this;
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_GROUP;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Category", "Identifier", "Data"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator, null};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();


		//hide data column
		TableColumnModel columnModel = recordTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));

		//hide data column
		columnModel = collectionTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));

		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_CATEGORY, 0, TABLE_PREFERRED_WIDTH_CATEGORY);
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelUndoAutoComplete(typeLabel, typeComboBox);
		GUIHelper.bindOnSelectionChange(typeComboBox, this::saveData);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, this, selectedRecord)));

		peopleGroupButton.setToolTipText("People");
		peopleGroupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PERSON_GROUP, this, selectedRecord)));

		groupsGroupButton.setToolTipText("Groups");
		groupsGroupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP_GROUP, this, selectedRecord)));

		placesGroupButton.setToolTipText("Places");
		placesGroupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE_GROUP, this, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, this, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, this, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, this, selectedRecord)));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, this, selectedRecord)));

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
		recordPanelBase.add(typeLabel, "align label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(peopleGroupButton, "sizegroup btn,center,split 3");
		recordPanelBase.add(groupsGroupButton, "sizegroup btn,gapleft 30,center");
		recordPanelBase.add(placesGroupButton, "sizegroup btn,gapleft 30,center,wrap paragraph");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(eventButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(groupButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add(RECORD_PANEL_NAME_BASE, recordPanelBase);
		recordTabbedPane.add(RECORD_PANEL_NAME_OTHER, recordPanelOther);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "0[grow]0", "0[grow]0"));
		if(relationshipDataPanel != null){
			final JPanel linkPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "0[grow]0", "0[grow]0"));
			linkPanel.add((Component)relationshipDataPanel, "grow");
			recordPanelLink.add(linkPanel, "grow");
		}

		recordTabbedPane.add(RECORD_PANEL_NAME_LINK, recordPanelLink);

		if(relationshipDataPanel == null)
			GUIHelper.enableTabByTitle((JTabbedPane)recordTabbedPane, RECORD_PANEL_NAME_LINK, false);
		else if(filterCategory != null && (showRecordOnly || selectRecordOnly))
			GUIHelper.enableTabByTitle((JTabbedPane)recordTabbedPane, RECORD_PANEL_NAME_LINK, true);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceTable == null
				? Repository.findAll(EntityManager.NODE_GROUP)
				: Repository.findReferencingNodes(filterReferenceTable, filterReferenceID,
					EntityManager.RELATIONSHIP_BELONGS_TO).stream()
			.map(Map.Entry::getValue)
			.collect(Collectors.toList()));
		//filter by category
		if(filterCategory != null)
			records.removeIf(record -> {
				final String category = extractCategory(extractRecordID(record));
				return (!category.isEmpty() && !category.equals(filterCategory));
			});

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		final DefaultTableModel collectionModel = (useCollection() && !collections.isEmpty()? getCollectionTableModel(): null);
		if(collectionModel != null)
			collectionModel.setRowCount(collections.size());
		int recordRow = 0;
		int collectionRow = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String categoryIdentifier = extractIdentifier(record, recordID);
			final String category = categoryIdentifier.substring(0, categoryIdentifier.indexOf('|'));
			final String identifier = categoryIdentifier.substring(categoryIdentifier.indexOf('|') + 1);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(category)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, recordRow, TABLE_INDEX_ID);
			model.setValueAt(filterData, recordRow, TABLE_INDEX_FILTER);
			model.setValueAt(category, recordRow, TABLE_INDEX_CATEGORY);
			model.setValueAt(identifier, recordRow, TABLE_INDEX_IDENTIFIER);

			if(collectionModel != null && collections.containsKey(recordID)){
				final List<Map<String, Object>> relationships = Repository.findRelationships(EntityManager.NODE_GROUP, recordID,
					EntityManager.NODE_GROUP, filterCollectionTargetID,
					EntityManager.RELATIONSHIP_BELONGS_TO
				);
				final Map<String, Object> relationshipData = (!relationships.isEmpty()? relationships.getFirst(): new HashMap<>(0));

				collectionModel.setValueAt(recordID, collectionRow, TABLE_INDEX_ID);
//				collectionModel.setValueAt(filterData, collectionRow, TABLE_INDEX_FILTER);
				collectionModel.setValueAt(category, collectionRow, TABLE_INDEX_CATEGORY);
				collectionModel.setValueAt(identifier, collectionRow, TABLE_INDEX_IDENTIFIER);
				collectionModel.setValueAt(relationshipData, collectionRow, TABLE_INDEX_DATA);

				collectionRow ++;
			}

			recordRow ++;
		}
	}

	@Override
	protected void unselectAction(){
		peopleGroupButton.setEnabled(false);
		groupsGroupButton.setEnabled(false);
		placesGroupButton.setEnabled(false);

		super.unselectAction();
	}

	@Override
	public void loadDataWithCollection(final int recordID){
		loadCollections(recordID);
		loadData();
	}

	@Override
	protected void addToCollection(){
		final JDialog dialog = new JDialog(this, "Relationship data", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

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
			final String categoryIdentifier = extractIdentifier(selectedRecord, recordID);
			final String category = categoryIdentifier.substring(0, categoryIdentifier.indexOf('|'));
			final String identifier = categoryIdentifier.substring(categoryIdentifier.indexOf('|') + 1);
//			final FilterString filter = FilterString.create()
//				.add(recordID)
//				.add(identifier);
//			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
//			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(category, row, TABLE_INDEX_CATEGORY);
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
		typeComboBox.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer groupID = extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String restriction = Repository.getRestriction(EntityManager.NODE_GROUP, groupID);

		typeComboBox.setSelectedItem(type);

		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));


		refreshButtonStates(groupID);
	}

	@Override
	public void refreshButtonStates(final int recordID){
		final String tableName = getTableName();
		final boolean hasPhoto = (Repository.getDepiction(tableName, recordID) != null);
		final boolean hasPeople = Repository.hasPeople(tableName, recordID);
		final boolean hasGroups = Repository.hasGroups(tableName, recordID);
		final boolean hasPlaces = Repository.hasPlaces(tableName, recordID);
		final String category = (filterCategory != null? filterCategory: extractCategory(extractRecordID(selectedRecord)));
		setButtonSelectEnableAndBorder(photoButton, hasPhoto);
		peopleGroupButton.setEnabled(category.isEmpty() || EntityManager.NODE_PERSON.equals(category) || hasPeople);
		GUIHelper.addBorder(peopleGroupButton, EntityManager.NODE_PERSON.equals(category) && hasPeople, DATA_BUTTON_BORDER_COLOR);
		groupsGroupButton.setEnabled(category.isEmpty() || EntityManager.NODE_GROUP.equals(category) || hasGroups);
		GUIHelper.addBorder(groupsGroupButton, EntityManager.NODE_GROUP.equals(category) && hasGroups, DATA_BUTTON_BORDER_COLOR);
		placesGroupButton.setEnabled(category.isEmpty() || EntityManager.NODE_PLACE.equals(category) || hasPlaces);
		GUIHelper.addBorder(placesGroupButton, EntityManager.NODE_PLACE.equals(category) && hasPlaces, DATA_BUTTON_BORDER_COLOR);

		final boolean hasNotes = Repository.hasNotes(tableName, recordID);
		final boolean hasMedia = Repository.hasMedia(tableName, recordID);
		final boolean hasCulturalNorms = Repository.hasCulturalNorms(tableName, recordID);
		final boolean hasAssertions = Repository.hasAssertions(tableName, recordID);
		final boolean hasEvents = Repository.hasEvents(tableName, recordID);
		setButtonSelectEnableAndBorder(noteButton, hasNotes);
		setButtonSelectEnableAndBorder(mediaButton, hasMedia);
		setButtonSelectEnableAndBorder(assertionButton, hasAssertions);
		setButtonSelectEnableAndBorder(culturalNormButton, hasCulturalNorms);
		setButtonSelectEnableAndBorder(eventButton, hasEvents);
		setButtonSelectEnableAndBorder(groupButton, hasGroups);
	}

	@Override
	protected void clearData(){
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(photoButton);
		GUIHelper.setDefaultBorder(peopleGroupButton);
		GUIHelper.setDefaultBorder(groupsGroupButton);
		GUIHelper.setDefaultBorder(placesGroupButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		GUIHelper.setDefaultBorder(eventButton);
		GUIHelper.setDefaultBorder(groupButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null || selectRecordOnly)
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeComboBox);

		insertRecordType(selectedRecord, type);

		return true;
	}


	private static String extractCategory(final int groupID){
		final List<Map.Entry<String, Map<String, Object>>> storeGroupRelationships = Repository.findReferencingNodes(
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO);
		for(final Map.Entry<String, Map<String, Object>> storeGroupRelationship : storeGroupRelationships){
			final String referenceTable = storeGroupRelationship.getKey();
			if(!referenceTable.equals(EntityManager.NODE_PERSON)
					&& !referenceTable.equals(EntityManager.NODE_GROUP)
					&& !referenceTable.equals(EntityManager.NODE_PLACE))
				throw new IllegalArgumentException("Cannot exist a group of "
					+ StringHelper.pluralize(referenceTable.toUpperCase(Locale.ROOT)));

			return referenceTable;
		}
		return StringUtils.EMPTY;
	}

	private String extractIdentifier(final Map<String, Object> groupRecord, final int groupID){
		final String mainGroupType = extractRecordType(groupRecord);
		final List<Map.Entry<String, Map<String, Object>>> storeGroupRelationships = Repository.findReferencingNodes(
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO);
		String identifierCategory = StringUtils.EMPTY;
		final StringJoiner identifier = new StringJoiner(" + ");
		for(final Map.Entry<String, Map<String, Object>> storeGroupRelationship : storeGroupRelationships){
			final String referenceTable = storeGroupRelationship.getKey();
			final Integer referenceID = extractRecordID(storeGroupRelationship.getValue());

			switch(referenceTable){
				case EntityManager.NODE_PERSON -> {
					identifierCategory = "people";

					extractAllPersonNames(referenceID, identifier);
				}

				case EntityManager.NODE_GROUP -> {
					identifierCategory = "groups";

					extractAllPersonNamesInGroup(referenceID, identifier);
				}

				case EntityManager.NODE_PLACE -> {
					identifierCategory = "places";

					extractAllPlaceNames(referenceID, identifier);
				}

				default -> throw new IllegalArgumentException("Cannot exist a group of "
					+ StringHelper.pluralize(referenceTable.toUpperCase(Locale.ROOT)));
			}
		}
		return identifierCategory
			+ (mainGroupType != null? (!identifierCategory.isEmpty()? " (": "(") + mainGroupType + ")": StringUtils.EMPTY)
			+ "|" + (identifier.length() > 0? identifier: NO_DATA);
	}

	private void extractAllPersonNames(final Integer personID, final StringJoiner identifier){
		final List<Map<String, Object>> storePersonNames = Repository.findReferencingNodes(
			EntityManager.NODE_PERSON_NAME,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_FOR);
		for(final Map<String, Object> storePersonName : storePersonNames){
			final StringJoiner subIdentifier = new StringJoiner(" / ");
			final List<String> personAllNames = extractPersonNames(storePersonName);
			personAllNames.forEach(subIdentifier::add);

			if(subIdentifier.length() > 0)
				identifier.add(subIdentifier.toString());
		}
	}

	private List<String> extractPersonNames(final Map<String, Object> personNameRecord){
		final int personNameID = extractRecordID(personNameRecord);
		final List<Map<String, Object>> localizedPersonNames = Repository.findReferencingNodes(EntityManager.NODE_LOCALIZED_PERSON_NAME,
			EntityManager.NODE_PERSON_NAME, personNameID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);
		final Set<String> names = new TreeSet<>();
		names.add(extractSinglePersonName(personNameRecord));
		for(int i = 0, length = localizedPersonNames.size(); i < length; i ++){
			final Map<String, Object> localizedPersonName = localizedPersonNames.get(i);

			//extract transliterations
			names.add(extractSinglePersonName(localizedPersonName));
		}
		return names.stream()
			.filter(name -> !name.isEmpty())
			.toList();
	}

	private static String extractSinglePersonName(final Map<String, Object> personNameRecord){
		final String personalName = extractRecordPersonalName(personNameRecord);
		final String familyName = extractRecordFamilyName(personNameRecord);
		final StringJoiner name = new StringJoiner(", ");
		if(personalName != null)
			name.add(personalName);
		if(familyName != null)
			name.add(familyName);
		return name.toString();
	}

	private void extractAllPersonNamesInGroup(final Integer groupID, final StringJoiner identifier){
		//extract the names of all the persons of all the groups
		final List<Map<String, Object>> storeRecordsInGroup = Repository.findReferencedNodes(EntityManager.NODE_PERSON,
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_BELONGS_TO);
		for(final Map<String, Object> storeRecordInGroup : storeRecordsInGroup){
			final Integer referenceIDInGroup = extractRecordID(storeRecordInGroup);

			extractAllPersonNames(referenceIDInGroup, identifier);
		}
	}

	private static void extractAllPlaceNames(final Integer placeID, final StringJoiner identifier){
		//extract the name of the place
		final Map<String, Object> placeRecord = Repository.findByID(EntityManager.NODE_PLACE, placeID);
		final String name = extractRecordName(placeRecord);
		identifier.add(name);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> group1 = new HashMap<>();
		group1.put("type", "family");
		group1.put("photo_crop", "0 0 10 20");
		int group1ID = Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "neighborhood");
		int group2ID = Repository.upsert(group2, EntityManager.NODE_GROUP);
		final Map<String, Object> group3 = new HashMap<>();
		group3.put("type", "town");
		int group3ID = Repository.upsert(group3, EntityManager.NODE_GROUP);
		final Map<String, Object> group4 = new HashMap<>();
		group4.put("type", "group");
		int group4ID = Repository.upsert(group4, EntityManager.NODE_GROUP);

		int person11ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person12ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);

		final Map<String, Object> groupRelationship11 = new HashMap<>();
		groupRelationship11.put("role", "partner");
		groupRelationship11.put("certainty", "certain");
		groupRelationship11.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person11ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship11,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship12 = new HashMap<>();
		groupRelationship12.put("role", "partner");
		groupRelationship12.put("certainty", "certain");
		groupRelationship12.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person12ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship12,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		groupRelationship2.put("certainty", "certain");
		groupRelationship2.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, group4ID,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship3 = new HashMap<>();
		groupRelationship3.put("role", "partner");
		groupRelationship3.put("certainty", "certain");
		groupRelationship3.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_PLACE, place1ID,
			EntityManager.NODE_GROUP, group3ID,
			EntityManager.RELATIONSHIP_BELONGS_TO, groupRelationship3,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("personal_name", "personal name 1");
		personName1.put("family_name", "family name 1");
		personName1.put("type", "birth name");
		int personName1ID = Repository.upsert(personName1, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.NODE_PERSON, person11ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		int personName2ID = Repository.upsert(personName2, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName2ID,
			EntityManager.NODE_PERSON, person11ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		int personName3ID = Repository.upsert(personName3, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName3ID,
			EntityManager.NODE_PERSON, person12ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("locale", "en");
		int localizedPersonName1ID = Repository.upsert(localizedPersonName1, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName1ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("locale", "en");
		int localizedPersonName2ID = Repository.upsert(localizedPersonName2, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName2ID,
			EntityManager.NODE_PERSON_NAME, personName2ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("personal_name", RECORD_PANEL_NAME_OTHER);
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("locale", "en");
		int localizedPersonName3ID = Repository.upsert(localizedPersonName3, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName3ID,
			EntityManager.NODE_PERSON_NAME, personName3ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person11ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 1");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "/images/addPhoto.boy.jpg");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		int media1ID = Repository.upsert(media1, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_GROUP, group2ID,
			EntityManager.NODE_MEDIA, media1ID,
			EntityManager.RELATIONSHIP_DEPICTED_BY, media1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_FOR, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("creation_date", EntityManager.now());
		int modification1ID = Repository.upsert(modification1, EntityManager.NODE_MODIFICATION);
		Repository.upsertRelationship(EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final GroupDialog dialog = create(parent);
			dialog.loadData();
//			final GroupDialog dialog = create(parent)
//				.withCategory(EntityManager.NODE_PERSON);
//			dialog.loadData();
//			final GroupDialog dialog = createShowOnly(parent)
//				.withReference(EntityManager.NODE_GROUP, 2);
//			dialog.loadData(2);
//			final GroupDialog dialog = createRecordOnly(parent)
//				.withReference(EntityManager.NODE_GROUP, 2);
//			dialog.loadData();
//			if(!dialog.selectData(extractRecordID(group2)))
//				dialog.showNewRecord();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					final Map<String, Object> container = editCommand.getContainer();
					final int groupID = extractRecordID(container);
					final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_GROUP, groupID);
					final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
					switch(editCommand.getType()){
						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PHOTO_BUTTON)
									? MediaDialog.createSelectOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_DEPICTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
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
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_NOTE);
										Repository.upsertRelationship(EntityManager.NODE_NOTE, upsertedRecordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_NOTE, deletedIDs.get(i),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}

						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_CULTURAL_NORM_BUTTON)
									? CulturalNormDialog.createSelectOnly(parent)
									: CulturalNormDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_CULTURAL_NORM);
										Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, upsertedRecordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_CULTURAL_NORM, deletedIDs.get(i),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
								});
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}

						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_MEDIA_BUTTON)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_ASSERTION_BUTTON)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_ASSERTION);
										Repository.upsertRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_ASSERTION, upsertedRecordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_ASSERTION, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_SUPPORTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
								});
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}

						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_EVENT_BUTTON)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_EVENT);
										Repository.upsertRelationship(EntityManager.NODE_EVENT, upsertedRecordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_EVENT, deletedIDs.get(i),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(groupID);
								});
							eventDialog.loadData();

							eventDialog.showDialog();
						}

						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.createShowOnly(parent)
								.withReference(EntityManager.NODE_GROUP, groupID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}

						case PERSON_GROUP -> {
							final PersonDialog personDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PERSON_GROUP_BUTTON)
									? PersonDialog.createCollectionViewOnly(groupID, BelongsToGroupPanel::create, parent)
									: PersonDialog.createCollection(groupID, BelongsToGroupPanel::create, parent))
								.withOnCloseGracefully(modifiedRecords -> {
									final Set<Integer> currentPersonIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_PERSON,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO).stream()
										.map(EntityManager::extractRecordID)
										.collect(Collectors.toSet());
									final Map<Integer, Map<String, Object>> newPersonInGroup = modifiedRecords.getCollection();
									//extract the intersection between `currentPersonIDInGroup` and `newPersonIDInGroup`
									final Set<Integer> intersection = new HashSet<>(currentPersonIDInGroup);
									intersection.retainAll(newPersonInGroup.keySet());
									//retain only difference
									currentPersonIDInGroup.removeAll(intersection);
									for(final Integer newPersonID : intersection)
										newPersonInGroup.remove(newPersonID);
									//remove `currentPersonIDInGroup`
									for(final Integer oldPersonID : currentPersonIDInGroup)
										Repository.deleteRelationship(EntityManager.NODE_PERSON, oldPersonID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO);
									//add `newPersonIDInGroup`
									for(final Map.Entry<Integer, Map<String, Object>> newPerson : newPersonInGroup.entrySet())
										Repository.upsertRelationship(EntityManager.NODE_PERSON, newPerson.getKey(),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO, newPerson.getValue(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							personDialog.loadDataWithCollection(groupID);

							personDialog.showDialog();
						}

						case GROUP_GROUP -> {
							final GroupDialog groupDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_GROUP_GROUP_BUTTON)
									? createCollectionViewOnly(groupID, BelongsToGroupPanel::create, parent)
									: createCollection(groupID, BelongsToGroupPanel::create, parent))
								.withCategory(EntityManager.NODE_GROUP)
								.withOnCloseGracefully(modifiedRecords -> {
									final Set<Integer> currentGroupIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_GROUP,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO).stream()
										.map(EntityManager::extractRecordID)
										.collect(Collectors.toSet());
									final Map<Integer, Map<String, Object>> newGroupInGroup = modifiedRecords.getCollection();
									//extract the intersection between `currentGroupIDInGroup` and `newGroupIDInGroup`
									final Set<Integer> intersection = new HashSet<>(currentGroupIDInGroup);
									intersection.retainAll(newGroupInGroup.keySet());
									//retain only difference
									currentGroupIDInGroup.removeAll(intersection);
									for(final Integer newGroupID : intersection)
										newGroupInGroup.remove(newGroupID);
									//remove `currentGroupIDInGroup`
									for(final Integer oldGroupID : currentGroupIDInGroup)
										Repository.deleteRelationship(EntityManager.NODE_GROUP, oldGroupID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO);
									//add `newGroupIDInGroup`
									for(final Map.Entry<Integer, Map<String, Object>> newGroup : newGroupInGroup.entrySet())
										Repository.upsertRelationship(EntityManager.NODE_GROUP, newGroup.getKey(),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO, newGroup.getValue(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							groupDialog.loadDataWithCollection(groupID);

							groupDialog.showDialog();
						}

						case PLACE_GROUP -> {
							final PlaceDialog placeDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PLACE_GROUP_BUTTON)
									? PlaceDialog.createCollectionViewOnly(groupID, BelongsToGroupPanel::create, parent)
									: PlaceDialog.createCollection(groupID, BelongsToGroupPanel::create, parent))
								.withOnCloseGracefully(modifiedRecords -> {
									final Set<Integer> currentPlaceIDInGroup = Repository.findReferencingNodes(EntityManager.NODE_PLACE,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO).stream()
										.map(EntityManager::extractRecordID)
										.collect(Collectors.toSet());
									final Map<Integer, Map<String, Object>> newPlaceInGroup = modifiedRecords.getCollection();
									//extract the intersection between `currentPlaceIDInGroup` and `newPlaceIDInGroup`
									final Set<Integer> intersection = new HashSet<>(currentPlaceIDInGroup);
									intersection.retainAll(newPlaceInGroup.keySet());
									//retain only difference
									currentPlaceIDInGroup.removeAll(intersection);
									for(final Integer newPlaceID : intersection)
										newPlaceInGroup.remove(newPlaceID);
									//remove `currentPlaceIDInGroup`
									for(final Integer oldPlaceID : currentPlaceIDInGroup)
										Repository.deleteRelationship(EntityManager.NODE_PLACE, oldPlaceID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO);
									//add `newPlaceIDInGroup`
									for(final Map.Entry<Integer, Map<String, Object>> newPlace : newPlaceInGroup.entrySet())
										Repository.upsertRelationship(EntityManager.NODE_PLACE, newPlace.getKey(),
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_BELONGS_TO, newPlace.getValue(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							placeDialog.loadDataWithCollection(groupID);

							placeDialog.showDialog();
						}

						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + groupID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + groupID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}

						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + groupID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + groupID);
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
											EntityManager.NODE_GROUP, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, deletedIDs.get(i),
											EntityManager.NODE_GROUP, parentRecordID,
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
