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
import javax.swing.JTabbedPane;
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
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class CitationDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -7601387139021862486L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "citation";
	private static final String TABLE_NAME_SOURCE = "source";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";


	private JTabbedPane recordTabbedPane;
	private JButton sourceButton;
	private JLabel locationLabel;
	private JTextField locationField;
	private JButton extractButton;
	private JButton transcribedExtractButton;
	private JLabel extractTypeLabel;
	private JComboBox<String> extractTypeComboBox;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private final Integer filterSourceID;


	public CitationDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Integer filterSourceID,
			final Frame parent){
		super(store, parent);

		this.filterSourceID = filterSourceID;
	}


	public CitationDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Citations" + (filterSourceID != null? " for source " + filterSourceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		recordTabbedPane = new JTabbedPane();
		sourceButton = new JButton("Source", ICON_SOURCE);
		locationLabel = new JLabel("Location:");
		locationField = new JTextField();
		extractButton = new JButton("Extract", ICON_NOTE);
		transcribedExtractButton = new JButton("Transcribed extracts", ICON_TRANSLATION);
		extractTypeLabel = new JLabel("Type:");
		extractTypeComboBox = new JComboBox<>(new String[]{"transcript", "extract", "abstract"});

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");


		sourceButton.setToolTipText("Source");
		sourceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, getSelectedRecord())));
		GUIHelper.addBorder(sourceButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(locationLabel, locationField, evt -> saveData());

		extractButton.setToolTipText("Extract");
		extractButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.EXTRACT, getSelectedRecord())));

		transcribedExtractButton.setToolTipText("Transcribed extract");
		transcribedExtractButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.LOCALIZED_EXTRACT,
			getSelectedRecord())));

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(extractTypeLabel, extractTypeComboBox, evt -> saveData(),
			evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.MEDIA, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(sourceButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(locationLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(locationField, "grow,wrap paragraph");
		recordPanelBase.add(extractButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(transcribedExtractButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(extractTypeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(extractTypeComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected void loadData(){
		Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		if(filterSourceID != null)
			records = records.entrySet().stream()
				.filter(entry -> filterSourceID.equals(extractRecordSourceID(entry.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String sourceIdentifier = extractRecordSourceIdentifier(record.getValue());
			final String location = extractRecordLocation(record.getValue());
			final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
				+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
				+ (location != null? location: StringUtils.EMPTY);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID, TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final Integer sourceID = extractRecordSourceID(selectedRecord);
		final String location = extractRecordLocation(selectedRecord);
		final Integer extractID = extractRecordExtractID(selectedRecord);
		final String extractType = extractRecordExtractType(selectedRecord);
		final Map<Integer, Map<String, Object>> recordTranscribedExtracts = extractLocalizedTextJunction("extract");
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		GUIHelper.addBorder(sourceButton, (sourceID != null? DATA_BUTTON_BORDER_COLOR: MANDATORY_COMBOBOX_BACKGROUND_COLOR));
		locationField.setText(location);
		GUIHelper.addBorder(extractButton, extractID != null, DATA_BUTTON_BORDER_COLOR);
		extractTypeComboBox.setSelectedItem(extractType);
		GUIHelper.addBorder(transcribedExtractButton, !recordTranscribedExtracts.isEmpty(), DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		GUIHelper.setDefaultBorder(sourceButton);
		locationField.setText(null);
		extractTypeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(extractButton);
		GUIHelper.setDefaultBorder(transcribedExtractButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);

		GUIHelper.setEnabled(recordTabbedPane, false);
		deleteRecordButton.setEnabled(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final Integer sourceID = extractRecordSourceID(selectedRecord);
			//enforce non-nullity on `sourceID`
			if(sourceID == null){
				JOptionPane.showMessageDialog(getParent(), "Source field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				sourceButton.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String location = GUIHelper.readTextTrimmed(locationField);
		final String extractType = (String)extractTypeComboBox.getSelectedItem();

		//update table
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final Integer recordID = extractRecordID(selectedRecord);
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				final Map<String, Object> updatedCitationRecord = getRecords(TABLE_NAME).get(recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(updatedCitationRecord);
				final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
					+ (location != null? location: StringUtils.EMPTY);

				model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_RECORD_IDENTIFIER);
				break;
			}

		selectedRecord.put("location", location);
		selectedRecord.put("extract_type", extractType);
	}


	private static Integer extractRecordSourceID(final Map<String, Object> record){
		return (Integer)record.get("source_id");
	}

	private String extractRecordSourceIdentifier(final Map<String, Object> citationRecord){
		final Integer sourceID = extractRecordSourceID(citationRecord);
		if(sourceID == null)
			return null;

		final Map<Integer, Map<String, Object>> sources = getRecords(TABLE_NAME_SOURCE);
		final Map<String, Object> source = sources.get(sourceID);
		if(source == null)
			return null;

		return (String)source.get("identifier");
	}

	private static String extractRecordLocation(final Map<String, Object> record){
		return (String)record.get("location");
	}

	private static Integer extractRecordExtractID(final Map<String, Object> record){
		return (Integer)record.get("extract_id");
	}

	private static String extractRecordExtractType(final Map<String, Object> record){
		return (String)record.get("extract_type");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 8100932853392677459L;


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

		final TreeMap<Integer, Map<String, Object>> citations = new TreeMap<>();
		store.put(TABLE_NAME, citations);
		final Map<String, Object> citation = new HashMap<>();
		citation.put("id", 1);
		citation.put("source_id", 1);
		citation.put("location", "here");
		citation.put("extract_id", 1);
		citation.put("extract_type", "transcript");
		citations.put((Integer)citation.get("id"), citation);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put(TABLE_NAME_SOURCE, sources);
		final Map<String, Object> source = new HashMap<>();
		source.put("id", 1);
		source.put("identifier", "source 1");
		sources.put((Integer)source.get("id"), source);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put(TABLE_NAME_LOCALIZED_TEXT, localizedTexts);
		final Map<String, Object> localized_text1 = new HashMap<>();
		localized_text1.put("id", 1);
		localized_text1.put("text", "text 1");
		localized_text1.put("locale", "it");
		localized_text1.put("type", "original");
		localized_text1.put("transcription", "IPA");
		localized_text1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localized_text1.get("id"), localized_text1);
		final Map<String, Object> localized_text2 = new HashMap<>();
		localized_text2.put("id", 2);
		localized_text2.put("text", "text 2");
		localized_text2.put("locale", "en");
		localized_text2.put("type", "original");
		localized_text2.put("transcription", "kana");
		localized_text2.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localized_text2.get("id"), localized_text2);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put(TABLE_NAME_LOCALIZED_TEXT_JUNCTION, localizedTextJunctions);
		final Map<String, Object> localized_text_junction = new HashMap<>();
		localized_text_junction.put("id", 1);
		localized_text_junction.put("localized_text_id", 2);
		localized_text_junction.put("reference_type", "extract");
		localized_text_junction.put("reference_table", "citation");
		localized_text_junction.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localized_text_junction.get("id"), localized_text_junction);

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
						case SOURCE -> {
							//TODO
						}
						case EXTRACT -> {
							//TODO
						}
						case LOCALIZED_EXTRACT -> {
							//TODO
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode citation = editCommand.getContainer();
//							dialog.setTitle(citation.getID() != null
//								? "Note " + citation.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(citation, editCommand.getOnCloseGracefully());
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
					}
				}
			};
			EventBusService.subscribe(listener);

			final Integer filterSourceID = null;
			final CitationDialog dialog = new CitationDialog(store, filterSourceID, parent);
			if(!dialog.loadData(extractRecordID(source)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(400, 462);
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
