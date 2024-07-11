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
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class PersonNameDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -3816108402093925220L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "person_name";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_EVENT = "event";


	private JLabel primaryNameLabel;
	private JTextField primaryNameField;
	private JLabel secondaryNameLabel;
	private JTextField secondaryNameField;
	private JLabel nameLocaleLabel;
	private JTextField nameLocaleField;
	private JButton transcribedNameButton;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;

	private JButton noteButton;
	private JButton mediaButton;
	private JButton assertionButton;
	private JButton culturalNormButton;
	private JButton eventButton;
	private JCheckBox restrictionCheckBox;

	private int filterReferenceID;


	public static PersonNameDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new PersonNameDialog(store, parent);
	}


	private PersonNameDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public PersonNameDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public PersonNameDialog withReference(final int filterReferenceID){
		this.filterReferenceID = filterReferenceID;

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
		primaryNameLabel = new JLabel("(Primary) Name:");
		primaryNameField = new JTextField();
		secondaryNameLabel = new JLabel("(Secondary) Name:");
		secondaryNameField = new JTextField();
		nameLocaleLabel = new JLabel("Locale:");
		nameLocaleField = new JTextField();
		transcribedNameButton = new JButton("Transcribed names", ICON_TRANSLATION);
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "birth name", "also known as", "nickname", "family nickname", "pseudonym", "legal",
			"adoptive name", "stage name", "marriage name", "call name", "official name", "anglicized name", "religious order name",
			"pen name", "name at work", "immigrant"});

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Medias", ICON_MEDIA);
		assertionButton = new JButton("Assertions", ICON_ASSERTION);
		culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
		eventButton = new JButton("Events", ICON_EVENT);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChangeUndo(primaryNameLabel, primaryNameField, evt -> saveData());
		GUIHelper.bindLabelTextChangeUndo(secondaryNameLabel, secondaryNameField, evt -> saveData());
		addMandatoryField(primaryNameField, secondaryNameField);
		GUIHelper.bindLabelTextChangeUndo(nameLocaleLabel, nameLocaleField, evt -> saveData());

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_PERSON_NAME, TABLE_NAME, getSelectedRecord())));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, evt -> saveData());


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

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(primaryNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(primaryNameField, "growx,wrap related");
		recordPanelBase.add(secondaryNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(secondaryNameField, "growx,wrap related");
		recordPanelBase.add(nameLocaleLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(nameLocaleField, "grow,wrap related");
		recordPanelBase.add(transcribedNameButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "growx");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(eventButton, "sizegroup btn,center,wrap paragraph");
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
		final String title = GUIHelper.getTextTrimmed(filterField);
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
		final int personNameID = extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String primaryName = extractRecordPrimaryName(selectedRecord);
		final String secondaryName = extractRecordSecondaryName(selectedRecord);
		final String nameLocale = extractRecordNameLocale(selectedRecord);
		final Map<Integer, Map<String, Object>> recordTranscribedNames = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
			CommonRecordDialog::extractRecordReferenceType, "name");
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordAssertions = getRecords(TABLE_NAME_ASSERTION).entrySet().stream()
			.filter(entry -> personNameID == extractRecordReferenceID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordEvents = extractReferences(TABLE_NAME_EVENT);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		primaryNameField.setText(primaryName);
		secondaryNameField.setText(secondaryName);
		nameLocaleField.setText(nameLocale);
		GUIHelper.addBorder(transcribedNameButton, !recordTranscribedNames.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		typeComboBox.setSelectedItem(type);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(assertionButton, !recordAssertions.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(eventButton, !recordEvents.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		primaryNameField.setText(null);
		secondaryNameField.setText(null);
		nameLocaleField.setText(null);
		GUIHelper.setDefaultBorder(transcribedNameButton);
		typeComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		GUIHelper.setDefaultBorder(eventButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		final String primaryName = GUIHelper.getTextTrimmed(primaryNameField);
		final String secondaryName = GUIHelper.getTextTrimmed(secondaryNameField);
		if(!validData(primaryName) && !validData(secondaryName)){
			JOptionPane.showMessageDialog(getParent(), "(Primary or secondary) Name field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			primaryNameField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String primaryName = GUIHelper.getTextTrimmed(primaryNameField);
		final String secondaryName = GUIHelper.getTextTrimmed(secondaryNameField);
		final String nameLocale = GUIHelper.getTextTrimmed(nameLocaleField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);

		//update table:
		final Integer recordID = extractRecordID(selectedRecord);
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				final StringJoiner name = new StringJoiner(", ");
				if(primaryName != null)
					name.add(primaryName);
				if(secondaryName != null)
					name.add(secondaryName);

				model.setValueAt(name.toString(), modelRowIndex, TABLE_INDEX_RECORD_IDENTIFIER);

				break;
			}

		selectedRecord.put("primary_name", primaryName);
		selectedRecord.put("secondary_name", secondaryName);
		selectedRecord.put("name_locale", nameLocale);
		selectedRecord.put("type", type);

		return true;
	}


	private String extractIdentifier(final int selectedRecordID){
		final Map<String, Object> storePersonNames = getRecords(TABLE_NAME).get(selectedRecordID);
		final String primaryName = extractRecordPrimaryName(storePersonNames);
		final String secondaryName = extractRecordSecondaryName(storePersonNames);
		final StringJoiner name = new StringJoiner(", ");
		if(primaryName != null)
			name.add(primaryName);
		if(secondaryName != null)
			name.add(secondaryName);
		return name.toString();
	}

	private static String extractRecordPrimaryName(final Map<String, Object> record){
		return (String)record.get("primary_name");
	}

	private static String extractRecordSecondaryName(final Map<String, Object> record){
		return (String)record.get("secondary_name");
	}

	private static String extractRecordNameLocale(final Map<String, Object> record){
		return (String)record.get("name_locale");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
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
		personName1.put("primary_name", "toni");
		personName1.put("secondary_name", "bruxatin");
		personName1.put("name_locale", "vec-IT");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("primary_name", "antonio");
		personName2.put("secondary_name", "bruciatino");
		personName2.put("name_locale", "it-IT");
		personName2.put("type", "death name");
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
					final Map<String, Object> container = editCommand.getContainer();
					final int personNameID = extractRecordID(container);
					switch(editCommand.getType()){
						case LOCALIZED_PERSON_NAME -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleTextWithSecondary(store, parent)
								.withReference(TABLE_NAME, personNameID, "name")
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", personNameID);
									}
								});
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setSize(420, 480);
							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, personNameID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", personNameID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 474);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.create(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, personNameID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", personNameID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setSize(420, 497);
							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
						}
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(store, parent)
								.withReference(TABLE_NAME, personNameID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", personNameID);
									}
								});
							culturalNormDialog.initComponents();
							culturalNormDialog.loadData();

							culturalNormDialog.setSize(474, 652);
							culturalNormDialog.setLocationRelativeTo(dialog);
							culturalNormDialog.setVisible(true);
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(TABLE_NAME, personNameID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setSize(488, 386);
							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
						}
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.create(store, parent)
								.withReference(TABLE_NAME, personNameID);
							eventDialog.initComponents();
							eventDialog.loadData();

							eventDialog.setSize(309, 409);
							eventDialog.setLocationRelativeTo(null);
							eventDialog.setVisible(true);
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
			dialog.setSize(535, 469);
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
