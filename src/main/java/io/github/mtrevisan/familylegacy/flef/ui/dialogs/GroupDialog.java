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
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class GroupDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -2953401801022572404L;

	private static final int TABLE_PREFERRED_WIDTH_RECORD_CATEGORY = 65;

	private static final int TABLE_INDEX_RECORD_CATEGORY = 1;
	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 2;

	private static final String TABLE_NAME = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_EVENT = "event";


	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JButton photoButton;
	private JButton photoCropButton;

	private JButton noteButton;
	private JButton mediaButton;
	private JButton assertionButton;
	private JButton culturalNormButton;
	private JButton eventButton;
	private JButton groupButton;
	private JCheckBox restrictionCheckBox;

	private JLabel linkRoleLabel;
	private JTextField linkRoleField;
	private JLabel linkCertaintyLabel;
	private JComboBox<String> linkCertaintyComboBox;
	private JLabel linkCredibilityLabel;
	private JComboBox<String> linkCredibilityComboBox;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static GroupDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new GroupDialog(store, parent);
	}


	private GroupDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public GroupDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		Consumer<Map<String, Object>> innerOnCloseGracefully = record -> {
			final TreeMap<Integer, Map<String, Object>> mediaJunctions = getRecords(TABLE_NAME_GROUP_JUNCTION);
			final int mediaJunctionID = extractNextRecordID(mediaJunctions);
			if(selectedRecord != null){
				final Integer groupID = extractRecordID(selectedRecord);
				final Map<String, Object> mediaJunction = new HashMap<>();
				mediaJunction.put("id", mediaJunctionID);
				mediaJunction.put("group_id", groupID);
				mediaJunction.put("reference_table", filterReferenceTable);
				mediaJunction.put("reference_id", filterReferenceID);
				mediaJunction.put("role", GUIHelper.getTextTrimmed(linkRoleField));
				mediaJunction.put("certainty", GUIHelper.getTextTrimmed(linkCertaintyComboBox));
				mediaJunction.put("credibility", GUIHelper.getTextTrimmed(linkCredibilityComboBox));
				mediaJunctions.put(mediaJunctionID, mediaJunction);
			}
			else
				mediaJunctions.remove(mediaJunctionID);
		};
		if(onCloseGracefully != null)
			innerOnCloseGracefully = innerOnCloseGracefully.andThen(onCloseGracefully);

		setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public GroupDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected DefaultTableModel getDefaultTableModel(){
		return new RecordTableModel();
	}

	@Override
	protected void initStoreComponents(){
		setTitle("Groups"
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_RECORD_CATEGORY, 0, TABLE_PREFERRED_WIDTH_RECORD_CATEGORY);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_CATEGORY, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "family", "neighborhood", "fraternity", "ladies club", "literary society"});
		photoButton = new JButton("Photo", ICON_PHOTO);
		photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Medias", ICON_MEDIA);
		assertionButton = new JButton("Assertions", ICON_ASSERTION);
		culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
		eventButton = new JButton("Events", ICON_EVENT);
		groupButton = new JButton("Groups", ICON_GROUP);
		restrictionCheckBox = new JCheckBox("Confidential");

		linkRoleLabel = new JLabel("Role:");
		linkRoleField = new JTextField();
		linkCertaintyLabel = new JLabel("Certainty:");
		linkCertaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
		linkCredibilityLabel = new JLabel("Credibility:");
		linkCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());


		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, TABLE_NAME, getSelectedRecord())));

		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO_CROP, TABLE_NAME, getSelectedRecord())));
		photoCropButton.setEnabled(false);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, getSelectedRecord())));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, TABLE_NAME, getSelectedRecord())));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, TABLE_NAME, getSelectedRecord())));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		GUIHelper.bindLabelTextChangeUndo(linkRoleLabel, linkRoleField, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCertaintyLabel, linkCertaintyComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCredibilityLabel, linkCredibilityComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(photoCropButton, "sizegroup btn,gapleft 30,center");

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
		final Map<Integer, Map<String, Object>> records = (filterReferenceTable == null
			? getRecords(TABLE_NAME)
			: getFilteredRecords(TABLE_NAME, filterReferenceTable, filterReferenceID));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String categoryIdentifier = extractIdentifier(extractRecordID(record.getValue()));
			final String category = categoryIdentifier.substring(0, categoryIdentifier.indexOf(':'));
			final String identifier = categoryIdentifier.substring(categoryIdentifier.indexOf(':') + 1);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(category, row, TABLE_INDEX_RECORD_CATEGORY);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_CATEGORY, TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final Integer groupID = extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final String photoCrop = extractRecordPhotoCrop(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordAssertions = getRecords(TABLE_NAME_ASSERTION).entrySet().stream()
			.filter(entry -> Objects.equals(groupID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordEvents = getRecords(TABLE_NAME_EVENT).entrySet().stream()
			.filter(entry -> Objects.equals(groupID, extractRecordGroupID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordGroups = extractReferences(TABLE_NAME);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		final String role = extractRecordRole(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);

		typeComboBox.setSelectedItem(type);
		GUIHelper.addBorder(photoButton, photoID != null, DATA_BUTTON_BORDER_COLOR);
		photoCropButton.setEnabled(photoID != null);
		GUIHelper.addBorder(photoCropButton, photoID != null && photoCrop != null && !photoCrop.isEmpty(), DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(assertionButton, !recordAssertions.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(eventButton, !recordEvents.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(groupButton, !recordGroups.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		linkRoleField.setText(role);
		linkCertaintyComboBox.setSelectedItem(certainty);
		linkCredibilityComboBox.setSelectedItem(credibility);


		final String referenceTable = extractRecordReferenceTable(selectedRecord);
		GUIHelper.enableTabByTitle(recordTabbedPane, "link", referenceTable != null);
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

		//read link panel:
		final String role = GUIHelper.getTextTrimmed(linkRoleField);
		final String certainty = GUIHelper.getTextTrimmed(linkCertaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(linkCredibilityComboBox);

		selectedRecord.put("type", type);

		return true;
	}


	private String extractIdentifier(final int selectedRecordID){
		final TreeMap<Integer, Map<String, Object>> storeGroupJunction = getRecords(TABLE_NAME_GROUP_JUNCTION);
		final TreeMap<Integer, Map<String, Object>> storePersonNames = getRecords(TABLE_NAME_PERSON_NAME);
		final TreeMap<Integer, Map<String, Object>> storeGroups = getRecords(TABLE_NAME);
		final TreeMap<Integer, Map<String, Object>> storeLocalizedTexts = getRecords(TABLE_NAME_LOCALIZED_TEXT);
		String identifierCategory = "people";
		final StringJoiner identifier = new StringJoiner(" + ");
		for(final Map<String, Object> groupElement : storeGroupJunction.values()){
			final StringJoiner subIdentifier = new StringJoiner(" / ");
			if(extractRecordGroupID(groupElement).equals(selectedRecordID)){
				final String referenceTable = extractRecordReferenceTable(groupElement);
				final Integer referenceID = extractRecordReferenceID(groupElement);
				final List<Map<String, Object>> personNamesInGroup;
				if("person".equals(referenceTable))
					personNamesInGroup = extractPersonNamesInGroup(storePersonNames, referenceID);
				else if("group".equals(referenceTable)){
					identifierCategory = "groups";

					//extract the names of all the persons of all the groups
					personNamesInGroup = new ArrayList<>();
					for(final Map<String, Object> storeGroup : storeGroups.values())
						if(referenceID.equals(extractRecordID(storeGroup)))
							personNamesInGroup.addAll(extractPersonNamesInGroup(storePersonNames, referenceID));
				}
				else
					throw new IllegalArgumentException("Cannot exist a group of " + referenceTable);

				for(int i = 0, length = personNamesInGroup.size(); i < length; i ++){
					final Map<String, Object> storePersonName = personNamesInGroup.get(i);

					final Integer extractRecordNameID = extractRecordNameID(storePersonName);
					final Map<String, Object> localizedText = storeLocalizedTexts.get(extractRecordNameID);
					final String name = extractRecordText(localizedText);
					subIdentifier.add(name != null? name: "?");
				}
				identifier.add(subIdentifier.toString());
			}
		}
		return identifierCategory + ":" + identifier;
	}

	/** Extract the names of all the persons in this group. */
	private static List<Map<String, Object>> extractPersonNamesInGroup(final TreeMap<Integer, Map<String, Object>> storePersonNames,
			final Integer personID){
		final List<Map<String, Object>> personNamesInGroup = new ArrayList<>();
		for(final Map<String, Object> storePersonName : storePersonNames.values())
			if(personID.equals(extractRecordPersonID(storePersonName)))
				personNamesInGroup.add(storePersonName);
		return personNamesInGroup;
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (String)record.get("photo_crop");
	}

	private static Integer extractRecordGroupID(final Map<String, Object> record){
		return (Integer)record.get("group_id");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static Integer extractRecordNameID(final Map<String, Object> record){
		return (Integer)record.get("name_id");
	}

	private static String extractRecordRole(final Map<String, Object> record){
		return (String)record.get("role");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 8927632880445915432L;


		RecordTableModel(){
			super(new String[]{"ID", "Category", "Identifier"}, 0);
		}

		@Override
		public final Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public final boolean isCellEditable(final int row, final int column){
			return false;
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> groups = new TreeMap<>();
		store.put(TABLE_NAME, groups);
		final Map<String, Object> group1 = new HashMap<>();
		group1.put("id", 1);
		group1.put("type", "family");
		group1.put("photo_id", 1);
		group1.put("photo_crop", "0 0 10 20");
		groups.put((Integer)group1.get("id"), group1);
		final Map<String, Object> group2 = new HashMap<>();
		group2.put("id", 2);
		group2.put("type", "neighborhood");
		groups.put((Integer)group2.get("id"), group2);

		final TreeMap<Integer, Map<String, Object>> groupJunctions = new TreeMap<>();
		store.put(TABLE_NAME_GROUP_JUNCTION, groupJunctions);
		final Map<String, Object> groupJunction1 = new HashMap<>();
		groupJunction1.put("id", 1);
		groupJunction1.put("group_id", 1);
		groupJunction1.put("reference_table", "person");
		groupJunction1.put("reference_id", 1);
		groupJunctions.put((Integer)groupJunction1.get("id"), groupJunction1);
		final Map<String, Object> groupJunction2 = new HashMap<>();
		groupJunction2.put("id", 2);
		groupJunction2.put("group_id", 1);
		groupJunction2.put("reference_table", "person");
		groupJunction2.put("reference_id", 2);
		groupJunctions.put((Integer)groupJunction2.get("id"), groupJunction2);
		final Map<String, Object> groupJunction3 = new HashMap<>();
		groupJunction3.put("id", 3);
		groupJunction3.put("group_id", 2);
		groupJunction3.put("reference_table", "group");
		groupJunction3.put("reference_id", 2);
		groupJunctions.put((Integer)groupJunction3.get("id"), groupJunction3);

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		persons.put((Integer)person1.get("id"), person1);
		final Map<String, Object> person2 = new HashMap<>();
		person2.put("id", 2);
		persons.put((Integer)person2.get("id"), person2);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("name_id", 1);
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("name_id", 2);
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);
		final Map<String, Object> personName3 = new HashMap<>();
		personName3.put("id", 3);
		personName3.put("person_id", 2);
		personName3.put("name_id", 3);
		personName3.put("type", "other name");
		personNames.put((Integer)personName3.get("id"), personName3);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);
		final Map<String, Object> localizedText3 = new HashMap<>();
		localizedText3.put("id", 3);
		localizedText3.put("text", "other name");
		localizedText3.put("locale", "en");
		localizedTexts.put((Integer)localizedText3.get("id"), localizedText3);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put(TABLE_NAME_NOTE, notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put("media", medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "/images/addPhoto.boy.jpg");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		medias.put((Integer)media1.get("id"), media1);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put(TABLE_NAME_RESTRICTION, restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final GroupDialog dialog = create(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(group1)))
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
					final int groupID = extractRecordID(container);
					final Integer photoID = extractRecordPhotoID(container);
					switch(editCommand.getType()){
						case PHOTO -> {
							final MediaDialog photoDialog = MediaDialog.createForPhoto(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, groupID)
								.withOnCloseGracefully(record -> {
									final Integer newPhotoID = extractRecordID(record);
									container.put("photo_id", newPhotoID);

									dialog.photoCropButton.setEnabled(newPhotoID != null);
								});
							photoDialog.initComponents();
							photoDialog.loadData();
							if(photoID != null){
								//add photo manually because is not retrievable through a junction
								photoDialog.addData(container);
								photoDialog.selectData(photoID);
							}
							else
								photoDialog.showNewRecord();

							photoDialog.setSize(420, 510);
							photoDialog.setLocationRelativeTo(dialog);
							photoDialog.setVisible(true);
						}
						case PHOTO_CROP -> {
							final PhotoCropDialog photoCropDialog = PhotoCropDialog.create(store, parent);
							photoCropDialog.withOnCloseGracefully(record -> {
								final Rectangle crop = photoCropDialog.getCrop();
								if(crop != null){
									final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
									sj.add(Integer.toString(crop.x))
										.add(Integer.toString(crop.y))
										.add(Integer.toString(crop.width))
										.add(Integer.toString(crop.height));
									container.put("photo_crop", sj);
								}
							});
							try{
								if(photoID != null){
									final String photoCrop = extractRecordPhotoCrop(container);
									photoCropDialog.loadData(photoID, photoCrop);
								}

								photoCropDialog.setSize(420, 295);
								photoCropDialog.setLocationRelativeTo(dialog);
								photoCropDialog.setVisible(true);
							}
							catch(final IOException ignored){}
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, groupID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", groupID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 474);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(store, parent)
								.withReference(TABLE_NAME, groupID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", groupID);
									}
								});
							culturalNormDialog.initComponents();
							culturalNormDialog.loadData();

							culturalNormDialog.setSize(474, 652);
							culturalNormDialog.setLocationRelativeTo(dialog);
							culturalNormDialog.setVisible(true);
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, groupID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", groupID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setSize(420, 497);
							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(TABLE_NAME, groupID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setSize(488, 386);
							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
						}
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.create(store, parent)
								.withReference(TABLE_NAME, groupID);
							eventDialog.initComponents();
							eventDialog.loadData();

							eventDialog.setSize(309, 409);
							eventDialog.setLocationRelativeTo(null);
							eventDialog.setVisible(true);
						}
						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.create(store, parent)
								.withReference(TABLE_NAME, groupID);
							groupDialog.initComponents();
							groupDialog.loadData();

							groupDialog.setSize(468, 469);
							groupDialog.setLocationRelativeTo(null);
							groupDialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setSize(468, 469);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
