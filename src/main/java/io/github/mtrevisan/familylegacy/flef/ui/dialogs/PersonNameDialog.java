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
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class PersonNameDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -3816108402093925220L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "person_name";
	private static final String TABLE_NAME_PERSON = "person";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";


	private JButton personButton;
	private JButton nameButton;
	private JButton transcribedNameButton;
	private JButton nameAlternateSortButton;
	private JButton transcribedAlternateNameButton;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;

	private JButton noteButton;
	private JButton culturalNormButton;
	private JCheckBox restrictionCheckBox;

	private int filterReferenceID;


	public static PersonNameDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new PersonNameDialog(store, parent);
	}

	public static PersonNameDialog createWithReferenceTable(final Map<String, TreeMap<Integer, Map<String, Object>>> store,
			final int filterReferenceID, final Frame parent){
		final PersonNameDialog dialog = new PersonNameDialog(store, parent);
		dialog.filterReferenceID = filterReferenceID;
		return dialog;
	}


	private PersonNameDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public PersonNameDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

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
		setTitle("Person names"
			+ (filterReferenceID > 0? " for person ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		personButton = new JButton("Person", ICON_PERSON);
		nameButton = new JButton("Name", ICON_TEXT);
		transcribedNameButton = new JButton("Transcribed names", ICON_TRANSLATION);
		nameAlternateSortButton = new JButton("Alternate (sort) name", ICON_TEXT);
		transcribedAlternateNameButton = new JButton("Transcribed alternate (sort) names", ICON_TRANSLATION);
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"birth name", "also known as", "nickname", "family nickname",
			"pseudonym", "legal", "adoptive name", "stage name", "marriage name", "call name", "official name", "anglicized name",
			"religious order name", "pen name", "name at work", "immigrant"});

		noteButton = new JButton("Notes", ICON_NOTE);
		culturalNormButton = new JButton("Cultural norm", ICON_CULTURAL_NORM);
		restrictionCheckBox = new JCheckBox("Confidential");


		personButton.setToolTipText("Person");
		personButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.PERSON, getSelectedRecord())));

		nameButton.setToolTipText("Name");
		nameButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NAME, getSelectedRecord())));

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.LOCALIZED_PERSON_NAME,
			getSelectedRecord())));

		nameAlternateSortButton.setToolTipText("Alternate (sort) name");
		nameAlternateSortButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NAME,
			getSelectedRecord())));

		transcribedAlternateNameButton.setToolTipText("Transcribed alternate (sort) names");
		transcribedAlternateNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_PERSON_NAME_ALTERNATE, getSelectedRecord())));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NOTE, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.CULTURAL_NORM,
			getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(nameButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(transcribedNameButton, "sizegroup btn,gapleft 30,center,wrap related");
		recordPanelBase.add(nameAlternateSortButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(transcribedAlternateNameButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = (filterReferenceID <= 0
			? getRecords(TABLE_NAME)
			: getFilteredRecords(filterReferenceID));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String identifier = extractIdentifier(extractRecordID(record.getValue()));

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	private TreeMap<Integer, Map<String, Object>> getFilteredRecords(final int filterReferenceID){
		return getRecords(TABLE_NAME).entrySet().stream()
			.filter(entry -> filterReferenceID == extractRecordPersonID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			sorter = new TableRowSorter<>(model);
			recordTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String type = extractRecordType(selectedRecord);
		final Integer personID = extractRecordPersonID(selectedRecord);
		final Integer nameID = extractRecordNameID(selectedRecord);
		final Integer alternateSortNameID = extractRecordAlternateSortNameID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordTranscribedNames = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
			CommonRecordDialog::extractRecordReferenceType, "name");
		final Map<Integer, Map<String, Object>> recordAlternateTranscribedNames = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
			CommonRecordDialog::extractRecordReferenceType, "alternate name");
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		GUIHelper.addBorder(personButton, personID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(nameButton, nameID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(transcribedNameButton, !recordTranscribedNames.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(nameAlternateSortButton, alternateSortNameID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(transcribedAlternateNameButton, !recordAlternateTranscribedNames.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		typeComboBox.setSelectedItem(type);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		GUIHelper.setDefaultBorder(personButton);
		GUIHelper.setDefaultBorder(nameButton);
		GUIHelper.setDefaultBorder(transcribedNameButton);
		GUIHelper.setDefaultBorder(nameAlternateSortButton);
		GUIHelper.setDefaultBorder(transcribedAlternateNameButton);
		typeComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		final Integer personID = extractRecordPersonID(selectedRecord);
		if(!validData(personID)){
			JOptionPane.showMessageDialog(getParent(), "Person field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			personButton.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String type = (String)typeComboBox.getSelectedItem();

		selectedRecord.put("type", type);
	}


	private String extractIdentifier(final int selectedRecordID){
		final Map<String, Object> storePersonNames = getRecords(TABLE_NAME).get(selectedRecordID);
		final Integer mainNameID = extractRecordNameID(storePersonNames);
		final Integer alternateNameID = extractRecordAlternateSortNameID(storePersonNames);
		final Map<Integer, Map<String, Object>> storeRecords = getRecords(TABLE_NAME_LOCALIZED_TEXT);
		final String mainRecordText = extractRecordText(storeRecords.get(mainNameID));
		final String alternateRecordText = extractRecordText(storeRecords.get(alternateNameID));
		return mainRecordText + (alternateRecordText != null? " (" + alternateRecordText + ")": StringUtils.EMPTY);
	}

	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static Integer extractRecordNameID(final Map<String, Object> record){
		return (Integer)record.get("name_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordAlternateSortNameID(final Map<String, Object> record){
		return (Integer)record.get("alternate_sort_name_id");
	}

	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 3064355539476914117L;


		RecordTableModel(){
			super(new String[]{"ID", "Name"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put(TABLE_NAME, personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("name_id", 1);
		personName1.put("type", "birth name");
		personName1.put("alternate_sort_name_id", 11);
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("name_id", 2);
		personName2.put("type", "death name");
		personName2.put("alternate_sort_name_id", 12);
		personNames.put((Integer)personName2.get("id"), personName2);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put(TABLE_NAME_LOCALIZED_TEXT, localizedTexts);
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
		final Map<String, Object> localizedText11 = new HashMap<>();
		localizedText11.put("id", 11);
		localizedText11.put("text", "alternate true name");
		localizedText11.put("locale", "en");
		localizedTexts.put((Integer)localizedText11.get("id"), localizedText11);
		final Map<String, Object> localizedText12 = new HashMap<>();
		localizedText12.put("id", 12);
		localizedText12.put("text", "alternate fake name");
		localizedText12.put("locale", "en");
		localizedTexts.put((Integer)localizedText12.get("id"), localizedText12);

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
			final PersonNameDialog dialog = create(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(personName1)))
				dialog.showNewRecord();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case NAME -> {
							//TODO single name
						}
						case LOCALIZED_PERSON_NAME -> {
							final int personNameID = extractRecordID(editCommand.getContainer());
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createWithReferenceTable(store, TABLE_NAME,
								personNameID, "name", parent);
							localizedTextDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 492);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}
						case LOCALIZED_PERSON_NAME_ALTERNATE -> {
							final int personNameID = extractRecordID(editCommand.getContainer());
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createWithReferenceTable(store, TABLE_NAME,
								personNameID, "alternate name", parent);
							localizedTextDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 492);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}
						case NOTE -> {
							final int personNameID = extractRecordID(editCommand.getContainer());
							final NoteDialog noteDialog = NoteDialog.createWithReferenceTable(store, TABLE_NAME, personNameID, parent);
							noteDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 487);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(535, 466);
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
