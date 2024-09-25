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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordGroupID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonNameID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPhotoID;
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
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.LEFT};
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

		GUIHelper.bindLabelUndoAutoComplete(linkCertaintyLabel, linkCertaintyComboBox);

		GUIHelper.bindLabelUndoAutoComplete(linkCredibilityLabel, linkCredibilityComboBox);
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
			final String categoryIdentifier = extractIdentifier(extractRecordID(record));
			final String category = categoryIdentifier.substring(0, categoryIdentifier.indexOf(':'));
			final String identifier = categoryIdentifier.substring(categoryIdentifier.indexOf(':') + 1);
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
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_GROUP, groupID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_GROUP, groupID);
		final boolean hasCulturalNorms = Repository.hasCulturalNorms(EntityManager.NODE_GROUP, groupID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_GROUP, groupID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_GROUP, groupID);
		final boolean hasGroups = Repository.hasGroups(EntityManager.NODE_GROUP, groupID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_GROUP, groupID);

		typeComboBox.setSelectedItem(type);
		setButtonEnableAndBorder(photoButton, photoID != null);

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
			final List<Map<String, Object>> recordGroupJunction = Repository.findRelationships(EntityManager.NODE_GROUP, groupID,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_OF);
			if(recordGroupJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			if(!recordGroupJunction.isEmpty()){
				selectedRecordLink = recordGroupJunction.getFirst();

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


	private String extractIdentifier(final int selectedRecordID){
		final List<Map<String, Object>> storeGroupJunction = Repository.findReferencingNodes(EntityManager.NODE_GROUP);
		final List<Map<String, Object>> storePersonNames = Repository.findAll(EntityManager.NODE_PERSON_NAME);
		final List<Map<String, Object>> storeGroups = Repository.findAll(EntityManager.NODE_GROUP);
		String identifierCategory = "people";
		final StringJoiner identifier = new StringJoiner(" + ");
		for(final Map<String, Object> groupElement : storeGroupJunction){
			final StringJoiner subIdentifier = new StringJoiner(" / ");
			if(extractRecordGroupID(groupElement).equals(selectedRecordID)){
				final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(EntityManager.NODE_ASSERTION,
					selectedRecordID, EntityManager.RELATIONSHIP_SUPPORTED_BY);
				final String referenceTable = (referencedNode != null? referencedNode.getKey(): null);
				final Integer referenceID = (referencedNode != null? extractRecordID(referencedNode.getValue()): null);
				final List<Map<String, Object>> personNamesInGroup;
				if(EntityManager.NODE_PERSON.equals(referenceTable))
					personNamesInGroup = extractPersonNamesInGroup(storePersonNames, referenceID);
				else if(EntityManager.NODE_GROUP.equals(referenceTable)){
					identifierCategory = "groups";

					//extract the names of all the persons of all the groups
					personNamesInGroup = new ArrayList<>();
					for(final Map<String, Object> storeGroup : storeGroups)
						if(referenceID.equals(extractRecordID(storeGroup)))
							personNamesInGroup.addAll(extractPersonNamesInGroup(storePersonNames, referenceID));
				}
				else
					throw new IllegalArgumentException("Cannot exist a group of " + referenceTable);

				final Collection<Integer> processedPersonIDs = new HashSet<>(0);
				for(int i = 0, length = personNamesInGroup.size(); i < length; i ++){
					final Map<String, Object> storePersonName = personNamesInGroup.get(i);

					final Integer personID = extractRecordPersonID(storePersonName);
					if(!processedPersonIDs.contains(personID)){
						final List<String> personAllNames = extractAllNames(personID);
						for(int j = 0, count = personAllNames.size(); j < count; j ++)
							subIdentifier.add(personAllNames.get(j));
					}
					processedPersonIDs.add(personID);
				}

				if(subIdentifier.length() > 0)
					identifier.add(subIdentifier.toString());
			}
		}
		return identifierCategory + ":" + (identifier.length() > 0? identifier: NO_DATA);
	}

	private List<String> extractAllNames(final Integer personID){
		final List<Map<String, Object>> localizedPersonNames = Repository.findAll(EntityManager.NODE_LOCALIZED_PERSON_NAME);
		final List<String> names = new ArrayList<>(0);
		Repository.findAll(EntityManager.NODE_PERSON_NAME)
			.stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.forEach(record -> {
				names.add(extractName(record));

				//extract transliterations
				final Integer personNameID = extractRecordID(record);
				localizedPersonNames
					.stream()
					.filter(record2 -> Objects.equals(personNameID, extractRecordPersonNameID(record2)))
					.map(GroupDialog::extractName)
					.filter(name -> !name.isEmpty())
					.forEach(names::add);
			});
		return names;
	}

	private static String extractName(final Map<String, Object> record){
		final String personalName = extractRecordPersonalName(record);
		final String familyName = extractRecordFamilyName(record);
		final StringJoiner name = new StringJoiner(", ");
		if(personalName != null)
			name.add(personalName);
		if(familyName != null)
			name.add(familyName);
		return name.toString();
	}

	/** Extract the names of all the persons in this group. */
	private static List<Map<String, Object>> extractPersonNamesInGroup(final List<Map<String, Object>> storePersonNames,
			final Integer personID){
		final List<Map<String, Object>> personNamesInGroup = new ArrayList<>();
		for(final Map<String, Object> storePersonName : storePersonNames)
			if(personID.equals(extractRecordPersonID(storePersonName)))
				personNamesInGroup.add(storePersonName);
		return personNamesInGroup;
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
group1.put("photo_id", 1);
		group1.put("photo_crop", "0 0 10 20");
		Repository.upsert(group1, EntityManager.NODE_GROUP);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("type", "neighborhood");
		Repository.upsert(group2, EntityManager.NODE_GROUP);

		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> groupJunction1 = new HashMap<>();
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group1),
			EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_OF, groupJunction1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.NODE_PERSON, 2,
			EntityManager.RELATIONSHIP_OF, groupJunction2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("role", "partner");
		groupJunction3.put("certainty", "certain");
		groupJunction3.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		Repository.upsertRelationship(EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.NODE_GROUP, extractRecordID(group2),
			EntityManager.RELATIONSHIP_OF, groupJunction3,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> personName1 = new HashMap<>();
personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name 1");
		personName1.put("family_name", "family name 1");
		personName1.put("type", "birth name");
		Repository.upsert(personName1, EntityManager.NODE_PERSON_NAME);
		final Map<String, Object> personName2 = new HashMap<>();
personName2.put("person_id", 1);
		personName2.put("personal_name", "personal name 2");
		personName2.put("family_name", "family name 2");
		personName2.put("type", "death name");
		Repository.upsert(personName2, EntityManager.NODE_PERSON_NAME);
		final Map<String, Object> personName3 = new HashMap<>();
personName3.put("person_id", 2);
		personName3.put("personal_name", "personal name 3");
		personName3.put("family_name", "family name 3");
		personName3.put("type", "other name");
		Repository.upsert(personName3, EntityManager.NODE_PERSON_NAME);

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("locale", "en");
		Repository.upsert(localizedPersonName1, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("locale", "en");
		Repository.upsert(localizedPersonName2, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("locale", "en");
		Repository.upsert(localizedPersonName3, EntityManager.NODE_LOCALIZED_PERSON_NAME);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
note1.put("reference_table", "person");
note1.put("reference_id", 1);
		Repository.upsert(note1, EntityManager.NODE_NOTE);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 1");
note2.put("reference_table", "group");
note2.put("reference_id", 1);
		Repository.upsert(note2, EntityManager.NODE_NOTE);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "/images/addPhoto.boy.jpg");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		Repository.upsert(media1, EntityManager.NODE_MEDIA);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
restriction1.put("reference_table", "group");
restriction1.put("reference_id", 1);
		Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);

		final Map<String, Object> modification1 = new HashMap<>();
modification1.put("reference_table", "group");
modification1.put("reference_id", 2);
		modification1.put("creation_date", EntityManager.now());
		Repository.upsert(modification1, EntityManager.NODE_MODIFICATION);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final GroupDialog dialog = create(parent);
//				.withReference(EntityManager.TABLE_NAME_GROUP, 2);
//			dialog.loadData();

			final GroupDialog dialog = createShowOnly(parent)
				.withReference(EntityManager.NODE_GROUP, 2);
			dialog.loadData(2);

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
					final Integer photoID = extractRecordPhotoID(container);
					switch(editCommand.getType()){
						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(dialog.photoButton)
									? MediaDialog.createEditOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_GROUP, groupID)
								.withOnCloseGracefully((record, recordID) -> {
									final Integer newPhotoID = extractRecordID(record);
									insertRecordPhotoID(container, newPhotoID);
								});
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
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + groupID);
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
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + groupID);
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
