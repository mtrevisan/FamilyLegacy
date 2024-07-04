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
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class AssertionDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -28220354680747790L;

	private static final int TABLE_INDEX_RECORD_REFERENCE_TABLE = 1;

	private static final String TABLE_NAME = "assertion";
	private static final String TABLE_NAME_CITATION = "citation";
	private static final String TABLE_NAME_SOURCE = "source";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";


	private JButton citationButton;
	private JButton referenceButton;
	private JLabel roleLabel;
	private JTextField roleField;
	private JLabel certaintyLabel;
	private JComboBox<String> certaintyComboBox;
	private JLabel credibilityLabel;
	private JComboBox<String> credibilityComboBox;

	private JButton noteButton;
	private JButton mediaButton;
	private JButton culturalNormButton;
	private JCheckBox restrictionCheckBox;

	private final Integer filterCitationID;


	public AssertionDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Integer filterCitationID,
			final Frame parent){
		super(store, parent);

		this.filterCitationID = filterCitationID;
	}


	public AssertionDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Assertions" + (filterCitationID != null? " for citation " + filterCitationID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_REFERENCE_TABLE, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		citationButton = new JButton("Citation", ICON_CITATION);
		referenceButton = new JButton("Reference", ICON_REFERENCE);
		roleLabel = new JLabel("Role:");
		roleField = new JTextField();
		certaintyLabel = new JLabel("Certainty:");
		certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
		credibilityLabel = new JLabel("Credibility:");
		credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		culturalNormButton = new JButton("Cultural norm", ICON_CULTURAL_NORM);
		restrictionCheckBox = new JCheckBox("Confidential");


		citationButton.setToolTipText("Source");
		citationButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, getSelectedRecord())));
		GUIHelper.addBorder(citationButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);

		referenceButton.setToolTipText("Reference");
		referenceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REFERENCE, getSelectedRecord())));
		GUIHelper.addBorder(referenceButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(roleLabel, roleField, evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(certaintyLabel, certaintyComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(credibilityLabel, credibilityComboBox, evt -> saveData(),
			evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.MEDIA, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.CULTURAL_NORM,
			getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(citationButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(referenceButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(roleLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(roleField, "grow,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(credibilityComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected void loadData(){
		Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		if(filterCitationID != null)
			records = records.entrySet().stream()
				.filter(entry -> filterCitationID.equals(extractRecordCitationID(entry.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> assertion = record.getValue();

			final String sourceIdentifier = extractRecordSourceIdentifier(assertion);
			final String location = extractRecordLocation(assertion);
			final String referenceTable = extractRecordReferenceTable(assertion);
			final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (location != null? (sourceIdentifier != null? " at ": StringUtils.EMPTY) + location: StringUtils.EMPTY)
					+ (location != null || sourceIdentifier != null? " for ": StringUtils.EMPTY) + referenceTable;

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_REFERENCE_TABLE);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_REFERENCE_TABLE);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final Integer sourceID = extractRecordCitationID(selectedRecord);
		final Integer referenceID = extractRecordReferenceID(selectedRecord);
		final String location = extractRecordLocation(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		GUIHelper.addBorder(citationButton, (sourceID != null? DATA_BUTTON_BORDER_COLOR: MANDATORY_COMBOBOX_BACKGROUND_COLOR));
		GUIHelper.addBorder(referenceButton, (referenceID != null? DATA_BUTTON_BORDER_COLOR: MANDATORY_COMBOBOX_BACKGROUND_COLOR));
		roleField.setText(location);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		GUIHelper.setDefaultBorder(citationButton);
		GUIHelper.setDefaultBorder(referenceButton);
		roleField.setText(null);
		certaintyComboBox.setSelectedItem(null);
		credibilityComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final Integer citationID = extractRecordCitationID(selectedRecord);
			//enforce non-nullity on `citationID`
			if(citationID == null){
				JOptionPane.showMessageDialog(getParent(), "Citation field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				citationButton.requestFocusInWindow();

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
		final String role = GUIHelper.readTextTrimmed(roleField);
		final String certainty = (String)certaintyComboBox.getSelectedItem();
		final String credibility = (String)credibilityComboBox.getSelectedItem();

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final Integer recordID = extractRecordID(selectedRecord);
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				final Map<String, Object> updatedAssertionRecord = getRecords(TABLE_NAME).get(recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(updatedAssertionRecord);
				final String location = extractRecordLocation(updatedAssertionRecord);
				final String referenceTable = extractRecordReferenceTable(updatedAssertionRecord);
				final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (location != null? (sourceIdentifier != null? " at ": StringUtils.EMPTY) + location: StringUtils.EMPTY)
					+ (location != null || sourceIdentifier != null? " for ": StringUtils.EMPTY) + referenceTable;

				model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_RECORD_REFERENCE_TABLE);
				break;
			}

		selectedRecord.put("role", role);
		selectedRecord.put("certainty", certainty);
		selectedRecord.put("credibility", credibility);
	}


	private String extractRecordLocation(final Map<String, Object> assertionRecord){
		final Integer citationID = extractRecordCitationID(assertionRecord);
		if(citationID == null)
			return null;

		final Map<Integer, Map<String, Object>> citations = getRecords(TABLE_NAME_CITATION);
		final Map<String, Object> citation = citations.get(citationID);
		if(citation == null)
			return null;

		return (String)citation.get("location");
	}

	private static Integer extractRecordCitationID(final Map<String, Object> record){
		return (Integer)record.get("citation_id");
	}

	private String extractRecordSourceIdentifier(final Map<String, Object> assertionRecord){
		final Integer citationID = extractRecordCitationID(assertionRecord);
		if(citationID == null)
			return null;

		final Map<Integer, Map<String, Object>> citations = getRecords(TABLE_NAME_CITATION);
		final Map<String, Object> citation = citations.get(citationID);
		if(citation == null)
			return null;

		final Integer sourceID = extractRecordSourceID(citation);
		if(sourceID == null)
			return null;

		final Map<Integer, Map<String, Object>> sources = getRecords(TABLE_NAME_SOURCE);
		final Map<String, Object> source = sources.get(sourceID);
		if(source == null)
			return null;

		return (String)source.get("identifier");
	}

	private static Integer extractRecordSourceID(final Map<String, Object> record){
		return (Integer)record.get("source_id");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 6564164142990308313L;


		RecordTableModel(){
			super(new String[]{"ID", "Identifier"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> assertions = new TreeMap<>();
		store.put(TABLE_NAME, assertions);
		final Map<String, Object> assertion = new HashMap<>();
		assertion.put("id", 1);
		assertion.put("citation_id", 1);
		assertion.put("reference_table", "table");
		assertion.put("reference_id", 1);
		assertion.put("role", "father");
		assertion.put("certainty", "certain");
		assertion.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		assertions.put((Integer)assertion.get("id"), assertion);

		final TreeMap<Integer, Map<String, Object>> citations = new TreeMap<>();
		store.put("citation", citations);
		final Map<String, Object> citation = new HashMap<>();
		citation.put("id", 1);
		citation.put("source_id", 1);
		citation.put("location", "here");
		citation.put("extract_id", 1);
		citation.put("extract_type", "transcript");
		citations.put((Integer)citation.get("id"), citation);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source = new HashMap<>();
		source.put("id", 1);
		source.put("identifier", "source");
		sources.put((Integer)source.get("id"), source);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localized_text1 = new HashMap<>();
		localized_text1.put("id", 1);
		localized_text1.put("text", "text 1");
		localized_text1.put("locale", "it");
		localized_text1.put("type", "original");
		localized_text1.put("transcription", "IPA");
		localized_text1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localized_text1.get("id"), localized_text1);

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
				public static void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case SOURCE -> {
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
//							dialog.setLocationRelativeTo(parent);
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
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
						case CULTURAL_NORM -> {
							//TODO
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

			final Integer filterCitationID = null;
			final AssertionDialog dialog = new AssertionDialog(store, filterCitationID, parent);
			injector.injectDependencies(dialog);
			if(!dialog.loadData(extractRecordID(assertion)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(470, 457);
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
