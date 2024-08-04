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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
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
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class CitationDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -7601387139021862486L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "citation";
	private static final String TABLE_NAME_SOURCE = "source";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_LOCALIZED_TEXT = "localized_text";


	private JLabel locationLabel;
	private JTextField locationField;
	private JLabel extractLabel;
	private TextPreviewPane extractTextPreview;
	private JLabel extractLocaleLabel;
	private JTextField extractLocaleField;
	private JButton transcribedExtractButton;
	private JLabel extractTypeLabel;
	private JComboBox<String> extractTypeComboBox;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private JButton assertionButton;

	private Integer filterSourceID;


	public static CitationDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new CitationDialog(store, parent);
	}


	private CitationDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public CitationDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public CitationDialog withFilterOnSourceID(final int filterSourceID){
		this.filterSourceID = filterSourceID;

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
		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterSourceID != null? " for source ID " + filterSourceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		locationLabel = new JLabel("Location:");
		locationField = new JTextField();
		extractLabel = new JLabel("Extract:");
		extractTextPreview = TextPreviewPane.createWithPreview(this);
		extractTextPreview.setTextViewFont(extractLabel.getFont());
		extractLocaleLabel = new JLabel("Locale:");
		extractLocaleField = new JTextField();
		transcribedExtractButton = new JButton("Transcribed extracts", ICON_TRANSLATION);
		extractTypeLabel = new JLabel("Type:");
		extractTypeComboBox = new JComboBox<>(new String[]{null, "transcript", "extract", "abstract"});

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Medias", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");

		assertionButton = new JButton("Assertions", ICON_ASSERTION);


		GUIHelper.bindLabelTextChangeUndo(locationLabel, locationField, this::saveData);

		GUIHelper.bindLabelTextChange(extractLabel, extractTextPreview, this::saveData);
		extractTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(extractLocaleLabel, extractLocaleField, this::saveData);

		transcribedExtractButton.setToolTipText("Transcribed extract");
		transcribedExtractButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_EXTRACT, TABLE_NAME, getSelectedRecord())));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(extractTypeLabel, extractTypeComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, getSelectedRecord())));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(locationLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(locationField, "grow,wrap paragraph");
		recordPanelBase.add(extractLabel, "align label,top,sizegroup lbl,split 2");
		recordPanelBase.add(extractTextPreview, "grow,wrap related");
		recordPanelBase.add(extractLocaleLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(extractLocaleField, "grow,wrap related");
		recordPanelBase.add(transcribedExtractButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(extractTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(extractTypeComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelChildren = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelChildren.add(assertionButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("children", recordPanelChildren);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = new HashMap<>(getRecords(TABLE_NAME));
		if(filterSourceID != null)
			records.values()
				.removeIf(entry -> !filterSourceID.equals(extractRecordSourceID(entry)));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String sourceIdentifier = extractRecordSourceIdentifier(container);
			final String location = extractRecordLocation(container);
			final StringJoiner identifier = new StringJoiner(StringUtils.SPACE);
			identifier.add((sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
				+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
				+ (location != null? location: StringUtils.EMPTY));
			final String extract = extractRecordExtract(container);
			if(extract != null && !extract.isEmpty())
				identifier.add("[" + extract + "]");

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first mandatory field
		extractTextPreview.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer citationID = extractRecordID(selectedRecord);
		final String location = extractRecordLocation(selectedRecord);
		final String extract = extractRecordExtract(selectedRecord);
		final String extractLocale = extractRecordExtractLocale(selectedRecord);
		final String extractType = extractRecordExtractType(selectedRecord);
		final Map<Integer, Map<String, Object>> recordTranscribedExtracts = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
			CommonRecordDialog::extractRecordReferenceType, "extract");
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		final Map<Integer, Map<String, Object>> recordAssertions = getRecords(TABLE_NAME_ASSERTION)
			.entrySet().stream()
			.filter(entry -> Objects.equals(citationID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

		locationField.setText(location);
		extractTextPreview.setText("Note " + extractRecordID(selectedRecord), extract, extractLocale);
		extractLocaleField.setText(extractLocale);
		extractTypeComboBox.setSelectedItem(extractType);
		GUIHelper.addBorder(transcribedExtractButton, !recordTranscribedExtracts.isEmpty(), DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		GUIHelper.addBorder(assertionButton, !recordAssertions.isEmpty(), DATA_BUTTON_BORDER_COLOR);
	}

	@Override
	protected void clearData(){
		locationField.setText(null);
		extractTypeComboBox.setSelectedItem(null);
		extractTextPreview.clear();
		extractLocaleField.setText(null);
		GUIHelper.setDefaultBorder(transcribedExtractButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);

		deleteRecordButton.setEnabled(false);

		GUIHelper.setDefaultBorder(assertionButton);
	}

	@Override
	protected boolean validateData(){
		final String extract = extractTextPreview.getTextTrimmed();
		if(!validData(extract)){
			JOptionPane.showMessageDialog(getParent(), "Extract field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			extractTextPreview.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String location = GUIHelper.getTextTrimmed(locationField);
		final String extract = extractTextPreview.getTextTrimmed();
		final String extractLocale = GUIHelper.getTextTrimmed(extractLocaleField);
		final String extractType = GUIHelper.getTextTrimmed(extractTypeComboBox);

		//update table:
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final Integer recordID = extractRecordID(selectedRecord);
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				final Map<String, Object> updatedCitationRecord = getRecords(TABLE_NAME).get(recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(updatedCitationRecord);
				final StringJoiner identifier = new StringJoiner(StringUtils.SPACE);
				identifier.add((sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
					+ (location != null? location: StringUtils.EMPTY));;
				if(extract != null && !extract.isEmpty())
					identifier.add("[" + extract + "]");

				model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_RECORD_IDENTIFIER);

				break;
			}

		selectedRecord.put("location", location);
		selectedRecord.put("extract", extract);
		selectedRecord.put("extract_locale", extractLocale);
		if(extractType != null && !extractType.isEmpty())
			selectedRecord.put("extract_type", extractType);

		return true;
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

	private static String extractRecordExtract(final Map<String, Object> record){
		return (String)record.get("extract");
	}

	private static String extractRecordExtractLocale(final Map<String, Object> record){
		return (String)record.get("extract_locale");
	}

	private static String extractRecordExtractType(final Map<String, Object> record){
		return (String)record.get("extract_type");
	}

	private static Integer extractRecordCitationID(final Map<String, Object> record){
		return (Integer)record.get("citation_id");
	}


	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
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
		citation.put("extract", "text 1");
		citation.put("extract_locale", "en-US");
		citation.put("extract_type", "transcript");
		citations.put((Integer)citation.get("id"), citation);

		final TreeMap<Integer, Map<String, Object>> assertions = new TreeMap<>();
		store.put(TABLE_NAME_ASSERTION, assertions);
		final Map<String, Object> assertion = new HashMap<>();
		assertion.put("id", 1);
		assertion.put("citation_id", 1);
		assertion.put("reference_table", "table");
		assertion.put("reference_id", 1);
		assertion.put("role", "father");
		assertion.put("certainty", "certain");
		assertion.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		assertions.put((Integer)assertion.get("id"), assertion);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put(TABLE_NAME_SOURCE, sources);
		final Map<String, Object> source = new HashMap<>();
		source.put("id", 1);
		source.put("repository_id", 1);
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
		final Map<String, Object> localizedTextJunction = new HashMap<>();
		localizedTextJunction.put("id", 1);
		localizedTextJunction.put("localized_text_id", 2);
		localizedTextJunction.put("reference_type", "extract");
		localizedTextJunction.put("reference_table", "citation");
		localizedTextJunction.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction.get("id"), localizedTextJunction);

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
		note2.put("note", "note 2");
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
			final CitationDialog dialog = create(store, parent);
//			dialog.withFilterOnSourceID(filterSourceID);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(source)))
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
					final int citationID = extractRecordID(container);
					switch(editCommand.getType()){
						case LOCALIZED_EXTRACT -> {
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(store, parent)
								.withReference(TABLE_NAME, citationID, "extract")
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", citationID);
									}
								});
							localizedTextDialog.initComponents();
							localizedTextDialog.loadData();

							localizedTextDialog.setLocationRelativeTo(dialog);
							localizedTextDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, citationID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", citationID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, citationID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", citationID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(TABLE_NAME, citationID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
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
