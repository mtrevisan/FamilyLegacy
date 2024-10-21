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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class GroupDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -2953401801022572404L;

	private static final int TABLE_PREFERRED_WIDTH_CATEGORY = 70;

	private static final int TABLE_INDEX_CATEGORY = 2;
	private static final int TABLE_INDEX_IDENTIFIER = 3;

	private static final String NO_DATA = "?";


	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "family", "neighborhood", "fraternity", "ladies club",
		"literary society"});
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JButton groupButton = new JButton("Groups", ICON_GROUP);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JLabel linkRoleLabel = new JLabel("Role:");
	private final JTextField linkRoleField = new JTextField();
	private final JLabel linkCertaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> linkCertaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel linkCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> linkCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private String filterReferenceTable;
	private int filterReferenceID;


	public static GroupDialog create(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createSelectOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.photoButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.culturalNormButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createShowOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static GroupDialog createEditOnly(final Frame parent){
		final GroupDialog dialog = new GroupDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private GroupDialog(final Frame parent){
		super(parent);
	}


	public GroupDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		BiConsumer<Map<String, Object>, Integer> innerOnCloseGracefully = (record, recordID) -> {
			if(selectedRecord != null)
				Repository.upsertRelationship(EntityManager.NODE_GROUP, recordID,
					filterReferenceTable, filterReferenceID,
					EntityManager.RELATIONSHIP_OF, new HashMap<>(selectedRecordLink),
					GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
			else if(selectedRecordID != null)
				Repository.deleteRelationship(EntityManager.NODE_GROUP, recordID,
					filterReferenceTable, filterReferenceID,
					EntityManager.RELATIONSHIP_OF);
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

	@Override
	protected String getTableName(){
		return EntityManager.NODE_GROUP;
	}

	@Override
	protected String getJunctionTableName(){
		return EntityManager.RELATIONSHIP_OF;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Category", "Identifier"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, textComparator};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();


		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_CATEGORY, 0, TABLE_PREFERRED_WIDTH_CATEGORY);
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelUndoAutoComplete(typeLabel, typeComboBox);
		GUIHelper.bindOnSelectionChange(typeComboBox, this::saveData);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, EntityManager.NODE_GROUP, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_GROUP, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_GROUP, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_GROUP, selectedRecord)));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, EntityManager.NODE_GROUP, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_GROUP, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, EntityManager.NODE_GROUP, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		GUIHelper.bindLabelUndo(linkRoleLabel, linkRoleField);
		GUIHelper.bindOnTextChange(linkRoleField, this::saveData);

		GUIHelper.bindLabelUndoAutoComplete(linkCertaintyLabel, linkCertaintyComboBox);
		GUIHelper.bindOnSelectionChange(linkCertaintyComboBox, this::saveData);

		GUIHelper.bindLabelUndoAutoComplete(linkCredibilityLabel, linkCredibilityComboBox);
		GUIHelper.bindOnSelectionChange(linkCredibilityComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(eventButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(groupButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelLink.add(linkRoleLabel, "align label,sizegroup lbl,split 2");
		recordPanelLink.add(linkRoleField, "grow,wrap paragraph");
		recordPanelLink.add(linkCertaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelLink.add(linkCertaintyComboBox, "wrap related");
		recordPanelLink.add(linkCredibilityLabel, "align label,sizegroup lbl,split 2");
		recordPanelLink.add(linkCredibilityComboBox);
		recordPanelLink.setEnabled(filterReferenceTable != null);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("link", recordPanelLink);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceTable == null
			? Repository.findAll(EntityManager.NODE_GROUP)
			: Repository.findReferencingNodes(EntityManager.NODE_GROUP,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_OF));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String categoryIdentifier = extractIdentifier(record, extractRecordID(record));
			final String category = categoryIdentifier.substring(0, categoryIdentifier.indexOf('|'));
			final String identifier = categoryIdentifier.substring(categoryIdentifier.indexOf('|') + 1);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(category)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(category, row, TABLE_INDEX_CATEGORY);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
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
		final boolean hasPhoto = (Repository.getDepiction(EntityManager.NODE_GROUP, groupID) != null);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_GROUP, groupID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_GROUP, groupID);
		final boolean hasCulturalNorms = Repository.hasCulturalNorms(EntityManager.NODE_GROUP, groupID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_GROUP, groupID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_GROUP, groupID);
		final boolean hasGroups = Repository.hasGroups(EntityManager.NODE_GROUP, groupID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_GROUP, groupID);

		typeComboBox.setSelectedItem(type);
		setButtonEnableAndBorder(photoButton, hasPhoto);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(culturalNormButton, hasCulturalNorms);
		setButtonEnableAndBorder(eventButton, hasEvents);
		setButtonEnableAndBorder(groupButton, hasGroups);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));

		linkRoleField.setText(null);
		linkCertaintyComboBox.setSelectedItem(null);
		linkCredibilityComboBox.setSelectedItem(null);
		if(filterReferenceTable != null){
			final List<Map<String, Object>> recordGroupRelationships = Repository.findRelationships(
				EntityManager.NODE_GROUP, groupID,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_OF);
			if(recordGroupRelationships.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			if(!recordGroupRelationships.isEmpty()){
				selectedRecordLink = recordGroupRelationships.getFirst();

				final String linkRole = extractRecordRole(selectedRecordLink);
				final String linkCertainty = extractRecordCertainty(selectedRecordLink);
				final String linkCredibility = extractRecordCredibility(selectedRecordLink);

				linkRoleField.setText(linkRole);
				linkCertaintyComboBox.setSelectedItem(linkCertainty);
				linkCredibilityComboBox.setSelectedItem(linkCredibility);
			}
		}

		GUIHelper.enableTabByTitle(recordTabbedPane, "link", (showRecordOnly || filterReferenceTable != null && selectedRecord != null));
	}

	@Override
	protected void clearData(){
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(photoButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		GUIHelper.setDefaultBorder(eventButton);
		GUIHelper.setDefaultBorder(groupButton);
		restrictionCheckBox.setSelected(false);

		linkRoleField.setText(null);
		linkCertaintyComboBox.setSelectedItem(null);
		linkCredibilityComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeComboBox);

		if(filterReferenceTable != null){
			//read link panel:
			final String linkRole = GUIHelper.getTextTrimmed(linkRoleField);
			final String linkCertainty = GUIHelper.getTextTrimmed(linkCertaintyComboBox);
			final String linkCredibility = GUIHelper.getTextTrimmed(linkCredibilityComboBox);

			if(selectedRecordLink == null)
				selectedRecordLink = new HashMap<>(3);
			insertRecordRole(selectedRecordLink, linkRole);
			insertRecordCertainty(selectedRecordLink, linkCertainty);
			insertRecordCredibility(selectedRecordLink, linkCredibility);
			Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(selectedRecord),
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_FOR, selectedRecordLink, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		}

		insertRecordType(selectedRecord, type);

		return true;
	}


	private String extractIdentifier(final Map<String, Object> groupRecord, final int groupID){
		final String mainGroupType = extractRecordType(groupRecord);
		final List<Map.Entry<String, Map<String, Object>>> storeGroupRelationships = Repository.findReferencedNodes(
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_OF);
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
		return identifierCategory + " (" + mainGroupType + ")|" + (identifier.length() > 0? identifier: NO_DATA);
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
		final List<Map.Entry<String, Map<String, Object>>> storeRecordsInGroup = Repository.findReferencedNodes(
			EntityManager.NODE_GROUP, groupID,
			EntityManager.RELATIONSHIP_OF);
		for(final Map.Entry<String, Map<String, Object>> storeRecordInGroup : storeRecordsInGroup){
			final Integer referenceIDInGroup = extractRecordID(storeRecordInGroup.getValue());

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

		int person11ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		int person12ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);

		Repository.upsertRelationship(EntityManager.NODE_GROUP, group1ID,
			EntityManager.NODE_PERSON, person11ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_GROUP, group1ID,
			EntityManager.NODE_PERSON, person12ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupRelationship2 = new HashMap<>();
		groupRelationship2.put("role", "partner");
		groupRelationship2.put("certainty", "certain");
		groupRelationship2.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, group2ID,
			EntityManager.NODE_GROUP, group1ID,
			EntityManager.RELATIONSHIP_OF, groupRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_GROUP, group3ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
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
		localizedPersonName3.put("personal_name", "other");
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
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("creation_date", EntityManager.now());
		int modification1ID = Repository.upsert(modification1, EntityManager.NODE_MODIFICATION);
		Repository.upsertRelationship(EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.NODE_GROUP, group2ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final GroupDialog dialog = create(parent);
			dialog.loadData();

//			final GroupDialog dialog = createShowOnly(parent)
//				.withReference(EntityManager.NODE_GROUP, 2);
//			dialog.loadData(2);

//			final GroupDialog dialog = createRecordOnly(parent)
//				.withReference(EntityManager.TABLE_NAME_GROUP, 2);
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
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(dialog.photoButton)
									? MediaDialog.createEditOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_MEDIA, recordID,
											EntityManager.RELATIONSHIP_DEPICTED_BY, record,
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							photoDialog.loadData();
							if(photoID != null){
								//add photo manually because is not retrievable through a relationship
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
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(parent)
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_GROUP, groupID,
											EntityManager.NODE_CULTURAL_NORM, recordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, record, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_GROUP, groupID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case GROUP -> {
							final GroupDialog groupDialog = (dialog.isViewOnlyComponent(dialog.groupButton)
									? GroupDialog.createSelectOnly(parent)
									: GroupDialog.create(parent))
								.withReference(EntityManager.NODE_GROUP, groupID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}
						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + groupID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + groupID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + groupID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getIdentifier();
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
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = extractRecordID(container);
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, recordID,
											EntityManager.NODE_GROUP, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									else
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, recordID,
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
