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

import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.autocomplete.AutoCompleteDecorator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;


public class GroupDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -2953401801022572404L;

	private static final int TABLE_PREFERRED_WIDTH_RECORD_CATEGORY = 65;

	private static final int TABLE_INDEX_RECORD_CATEGORY = 1;
	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 2;

	private static final String TABLE_NAME = "group";
	private static final String TABLE_NAME_GROUP = "group";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";


	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JButton photoButton;
	private JButton photoCropButton;

	private JButton noteButton;
	private JButton culturalNormButton;
	private JCheckBox restrictionCheckBox;


	public GroupDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Consumer<Object> onCloseGracefully,
			final Frame parent){
		super(store, onCloseGracefully, parent);

		setTitle("Groups");
	}


	@Override
	protected final String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected final DefaultTableModel getDefaultTableModel(){
		return new RecordTableModel();
	}

	@Override
	protected final void initStoreComponents(){
		super.initStoreComponents();


		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_RECORD_CATEGORY, 0, TABLE_PREFERRED_WIDTH_RECORD_CATEGORY);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_CATEGORY, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected final void initRecordComponents(){
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"family", "neighborhood", "fraternity", "ladies club",
			"literary society"});
		photoButton = new JButton("Photo", ICON_PHOTO);
		photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

		noteButton = new JButton("Notes", ICON_NOTE);
		culturalNormButton = new JButton("Cultural norm", ICON_CULTURAL_NORM);
		restrictionCheckBox = new JCheckBox("Confidential");


		typeLabel.setLabelFor(typeComboBox);
		typeComboBox.setEditable(true);
		GUIHelper.addUndoCapability(typeComboBox);
		AutoCompleteDecorator.decorate(typeComboBox);

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PHOTO, getSelectedRecord())));

		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PHOTO_CROP, getSelectedRecord())));
		photoCropButton.setEnabled(false);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected final void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(photoCropButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected final void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

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
	protected final void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_CATEGORY, TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected final void fillData(){
		final String type = extractRecordType(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final String photoCrop = extractRecordPhotoCrop(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		typeComboBox.setSelectedItem(type);
		GUIHelper.addBorder(photoButton, photoID != null, DATA_BUTTON_BORDER_COLOR);
		photoCropButton.setEnabled(photoCrop != null && !photoCrop.isEmpty());

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected final void clearData(){
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(photoButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected final boolean validateData(){
		return true;
	}

	@Override
	protected final void saveData(){
		//read record panel:
		final String type = (String)typeComboBox.getSelectedItem();

		selectedRecord.put("type", type);
	}


	private String extractIdentifier(final int selectedRecordID){
		final TreeMap<Integer, Map<String, Object>> storeGroupJunction = getRecords(TABLE_NAME_GROUP_JUNCTION);
		final TreeMap<Integer, Map<String, Object>> storePersonNames = getRecords(TABLE_NAME_PERSON_NAME);
		final TreeMap<Integer, Map<String, Object>> storeGroups = getRecords(TABLE_NAME_GROUP);
		final TreeMap<Integer, Map<String, Object>> storeLocalizedTexts = getRecords(TABLE_NAME_LOCALIZED_TEXT);
		String identifierCategory = "people";
		final StringJoiner identifier = new StringJoiner(" + ");
		for(final Map.Entry<Integer, Map<String, Object>> entry : storeGroupJunction.entrySet()){
			final Map<String, Object> groupElement = entry.getValue();
			final StringJoiner subIdentifier = new StringJoiner(", ");
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
		group1.put("photo_crop", "0 0 1 2");
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
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case NAME -> {
							//TODO
						}
						case PHOTO -> {
							//TODO
						}
						case PHOTO_CROP -> {
							//TODO
//							final Point cropStartPoint = ((CropDialog)cropDialog).getCropStartPoint();
//							final Point cropEndPoint = ((CropDialog)cropDialog).getCropEndPoint();
//							final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
//							sj.add(Integer.toString(cropStartPoint.x));
//							sj.add(Integer.toString(cropStartPoint.y));
//							sj.add(Integer.toString(cropEndPoint.x));
//							sj.add(Integer.toString(cropEndPoint.y));
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode place = editCommand.getContainer();
//							dialog.setTitle(place.getID() != null
//								? "Note " + place.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(place, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupDialog dialog = new GroupDialog(store, null, parent);
			if(!dialog.loadData(extractRecordID(group1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(355, 433);
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