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

import io.github.mtrevisan.familylegacy.flef.db.DatabaseManager;
import io.github.mtrevisan.familylegacy.flef.db.DatabaseManagerInterface;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
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
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


//TODO manage historic events not linked to anything
public final class EventDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 1136825738944999745L;

	private static final int TABLE_INDEX_RECORD_TYPE = 1;

	private static final String TABLE_NAME = "event";


	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel descriptionLabel;
	private JTextField descriptionField;
	private JButton placeButton;
	private JButton dateButton;
	private JButton referenceButton;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;


	public EventDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public EventDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Events");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_TYPE, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"historic fact", "birth", "marriage", "death", "coroner report", "cremation", "burial",
			"occupation", "imprisonment", "deportation", "invention", "religious conversion", "wedding", "ran away from home", "residence",
			"autopsy", "divorce", "engagement", "annulment", "separation", "eye color", "hair color", "height", "weight", "build",
			"complexion", "gender", "race", "ethnic origin", "anecdote", "marks/scars", "disability", "condition", "religion", "education",
			"able to read", "able to write", "career", "number of children (total)", "number of children (living)", "marital status",
			"political affiliation", "special talent", "hobby", "nationality", "draft registration", "legal problem", "tobacco use",
			"alcohol use", "drug problem", "guardianship", "inquest", "relationship", "bar mitzvah", "bas mitzvah", "jury duty", "baptism",
			"excommunication", "betrothal", "resignation", "naturalization", "marriage license", "christening", "confirmation", "will",
			"deed", "escrow", "probate", "retirement", "ordination", "graduation", "emigration", "enrollment", "execution", "employment",
			"land grant", "name change", "land purchase", "land sale", "military induction", "military enlistment", "military rank",
			"military award", "military promotion", "military service", "military release", "military discharge", "military resignation",
			"military retirement", "prison", "pardon", "membership", "hospitalization", "illness", "honor", "marriage bann",
			"missing in action", "adoption", "reburial", "filing for divorce", "exhumation", "funeral", "celebration of life", "partnership",
			"natural disaster", "blessing", "anniversary celebration", "first communion", "fosterage", "posthumous offspring", "immigration",
			"marriage contract", "reunion", "scattering of ashes", "inurnment", "cohabitation", "living together", "wedding anniversary",
			"patent filing", "patent granted", "internment", "learning", "conversion", "travel", "caste", "description",
			"number of marriages", "property", "imaginary", "marriage settlement", "specialty", "award"});

		descriptionLabel = new JLabel("Description:");
		descriptionField = new JTextField();

		placeButton = new JButton("Place", ICON_PLACE);
		dateButton = new JButton("Date", ICON_CALENDAR);
		referenceButton = new JButton("Reference", ICON_REFERENCE);

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelTextChangeUndo(descriptionLabel, descriptionField, evt -> saveData());

		placeButton.setToolTipText("Event place");
		placeButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, getSelectedRecord())));

		dateButton.setToolTipText("Event date");
		dateButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.DATE, getSelectedRecord())));

		referenceButton.setToolTipText("Reference");
		referenceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REFERENCE, getSelectedRecord())));
		GUIHelper.addBorder(referenceButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.MEDIA, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(descriptionLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(descriptionField, "growx,wrap paragraph");
		recordPanelBase.add(placeButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(dateButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(referenceButton, "sizegroup btn,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String type = extractRecordType(record.getValue());

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(type, row, TABLE_INDEX_RECORD_TYPE);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_TYPE);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String type = extractRecordType(selectedRecord);
		final String description = extractRecordDescription(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Integer dateID = extractRecordDateID(selectedRecord);
		final Integer referenceID = extractRecordReferenceID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		typeComboBox.setSelectedItem(type);
		descriptionField.setText(description);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(dateButton, dateID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(referenceButton, (referenceID != null? DATA_BUTTON_BORDER_COLOR: MANDATORY_COMBOBOX_BACKGROUND_COLOR));

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		typeComboBox.setSelectedItem(null);
		descriptionField.setText(null);
		GUIHelper.setDefaultBorder(placeButton);
		GUIHelper.setDefaultBorder(dateButton);
		GUIHelper.setDefaultBorder(referenceButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String type = extractRecordType(selectedRecord);
			//enforce non-nullity on `type`
			if(type == null || type.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				typeComboBox.requestFocusInWindow();

				return false;
			}

			final String referenceTable = extractRecordReferenceTable(selectedRecord);
			final Integer referenceID = extractRecordReferenceID(selectedRecord);
			//enforce non-nullity on `reference`
			if(referenceTable == null || referenceID == null){
				JOptionPane.showMessageDialog(getParent(), "Reference is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				referenceButton.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String type = (String)typeComboBox.getSelectedItem();
		final String description = descriptionField.getText();

		//update table
		if(! Objects.equals(type, extractRecordType(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
					model.setValueAt(type, modelRowIndex, TABLE_INDEX_RECORD_TYPE);
					break;
				}
		}

		selectedRecord.put("type", type);
		selectedRecord.put("description", description);
	}


	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 8973917668709639778L;


		RecordTableModel(){
			super(new String[]{"ID", "Type"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put(TABLE_NAME, events);
		final Map<String, Object> event = new HashMap<>();
		event.put("id", 1);
		event.put("type", "birth");
		event.put("description", "a birth");
		event.put("place_id", 1);
		event.put("date_id", 1);
		event.put("reference_table", "person");
		event.put("reference_id", 1);
		events.put((Integer)event.get("id"), event);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name_id", 1);
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place 1 name");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

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

		final TreeMap<Integer, Map<String, Object>> media = new TreeMap<>();
		store.put("media", media);
		final Map<String, Object> m1 = new HashMap<>();
		m1.put("id", 1);
		m1.put("identifier", "custom media");
		media.put((Integer)m1.get("id"), m1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case PLACE -> {
							//TODO
						}
						case DATE -> {
							//TODO
						}
						case REFERENCE -> {
							//TODO
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode assertion = editCommand.getContainer();
//							dialog.setTitle(assertion.getID() != null
//								? "Note " + assertion.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(assertion, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setVisible(true);
						}
						case MEDIA -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNoteTranslation(store, parent);
//							final GedcomNode noteTranslation = editCommand.getContainer();
//							dialog.setTitle(StringUtils.isNotBlank(noteTranslation.getValue())
//								? "Translation for language " + store.traverse(noteTranslation, "LOCALE").getValue()
//								: "New translation"
//							);
//							dialog.loadData(noteTranslation, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(450, 209);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final DependencyInjector injector = new DependencyInjector();
			final DatabaseManager dbManager = new DatabaseManager("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
			try{
				final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
				dbManager.initialize(grammarFile);

				dbManager.insertDatabase(store);
			}
			catch(final SQLException | IOException e){
				throw new RuntimeException(e);
			}
			injector.register(DatabaseManagerInterface.class, dbManager);

			final EventDialog dialog = new EventDialog(store, parent);
			injector.injectDependencies(dialog);
			if(!dialog.loadData(extractRecordID(event)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(303, 470);
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
