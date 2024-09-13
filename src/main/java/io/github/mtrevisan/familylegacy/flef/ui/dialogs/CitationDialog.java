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
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocation;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSourceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocation;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceTable;


public final class CitationDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -7601387139021862486L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JLabel extractLabel = new JLabel("Extract:");
	private final TextPreviewPane extractTextPreview = TextPreviewPane.createWithPreview(CitationDialog.this);
	private final JLabel extractLocaleLabel = new JLabel("Locale:");
	private final JTextField extractLocaleField = new JTextField();
	private final JButton transcribedExtractButton = new JButton("Transcribed extracts", ICON_TRANSLATION);
	private final JLabel extractTypeLabel = new JLabel("Type:");
	private final JComboBox<String> extractTypeComboBox = new JComboBox<>(new String[]{null, "transcript", "extract", "abstract"});

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);

	private Integer filterSourceID;


	public static CitationDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CitationDialog dialog = new CitationDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CitationDialog dialog = new CitationDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.transcribedExtractButton, dialog.noteButton, dialog.mediaButton, dialog.assertionButton);
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createShowOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CitationDialog dialog = new CitationDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createEditOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final CitationDialog dialog = new CitationDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
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

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName + " for Source ID " + filterSourceID);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.TABLE_NAME_CITATION;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		GUIHelper.bindLabelTextChangeUndo(locationLabel, locationField, this::saveData);

		GUIHelper.bindLabelTextChange(extractLabel, extractTextPreview, this::saveData);
		extractTextPreview.setTextViewFont(extractLabel.getFont());
		extractTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);
		extractTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(extractLocaleLabel, extractLocaleField, this::saveData);

		transcribedExtractButton.setToolTipText("Transcribed extract");
		transcribedExtractButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_EXTRACT, EntityManager.TABLE_NAME_CITATION, selectedRecord)));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(extractTypeLabel, extractTypeComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.TABLE_NAME_CITATION, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.TABLE_NAME_CITATION, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.TABLE_NAME_CITATION, selectedRecord)));
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
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
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
		unselectAction();

		final Map<Integer, Map<String, Object>> records = new HashMap<>(getRecords(EntityManager.TABLE_NAME_CITATION));
		if(filterSourceID != null)
			records.values()
				.removeIf(entry -> !filterSourceID.equals(extractRecordSourceID(entry)));

		final DefaultTableModel model = getRecordTableModel();
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
			final FilterString filter = FilterString.create()
				.add(key)
				.add(sourceIdentifier)
				.add(location);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}

		if(selectRecordOnly)
			selectFirstData();
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
		final boolean hasTranscribedExtracts = (getRecords(EntityManager.TABLE_NAME_LOCALIZED_TEXT_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.TABLE_NAME_CITATION, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(citationID, extractRecordReferenceID(record)))
			.filter(record -> Objects.equals(EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT, extractRecordReferenceType(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasNotes = (getRecords(EntityManager.TABLE_NAME_NOTE)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.TABLE_NAME_CITATION, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(citationID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasMedia = (getRecords(EntityManager.TABLE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.TABLE_NAME_CITATION, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(citationID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(EntityManager.TABLE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.TABLE_NAME_CITATION, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(citationID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);
		final boolean hasAssertions = (getRecords(EntityManager.TABLE_NAME_ASSERTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.TABLE_NAME_CITATION, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(citationID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);

		locationField.setText(location);
		extractTextPreview.setText("Note " + extractRecordID(selectedRecord), extract, extractLocale);
		extractLocaleField.setText(extractLocale);
		extractTypeComboBox.setSelectedItem(extractType);
		setButtonEnableAndBorder(transcribedExtractButton, hasTranscribedExtracts);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));

		setButtonEnableAndBorder(assertionButton, hasAssertions);
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
		final DefaultTableModel model = getRecordTableModel();
		final Integer recordID = extractRecordID(selectedRecord);
		for(int row = 0, length = model.getRowCount(); row < length; row ++){
			final int viewRowIndex = recordTable.convertRowIndexToView(row);
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

			if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
				final Map<String, Object> updatedCitationRecord = getRecords(EntityManager.TABLE_NAME_CITATION).get(recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(updatedCitationRecord);
				final StringJoiner identifier = new StringJoiner(StringUtils.SPACE);
				identifier.add((sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
					+ (location != null? location: StringUtils.EMPTY));
				if(extract != null && !extract.isEmpty())
					identifier.add("[" + extract + "]");

				model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_IDENTIFIER);

				break;
			}
		}

		insertRecordLocation(selectedRecord, location);
		insertRecordExtract(selectedRecord, extract);
		insertRecordExtractLocale(selectedRecord, extractLocale);
		if(extractType != null && !extractType.isEmpty())
			insertRecordExtractType(selectedRecord, extractType);

		return true;
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	private String extractRecordSourceIdentifier(final Map<String, Object> citationRecord){
		final Integer sourceID = extractRecordSourceID(citationRecord);
		if(sourceID == null)
			return null;

		final Map<Integer, Map<String, Object>> sources = getRecords(EntityManager.TABLE_NAME_SOURCE);
		final Map<String, Object> source = sources.get(sourceID);
		if(source == null)
			return null;

		return (String)source.get("identifier");
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> citations = new TreeMap<>();
		store.put("citation", citations);
		final Map<String, Object> citation = new HashMap<>();
		citation.put("id", 1);
		citation.put("source_id", 1);
		citation.put("location", "here");
		citation.put("extract", "text 1");
		citation.put("extract_locale", "en-US");
		citation.put("extract_type", "transcript");
		citations.put((Integer)citation.get("id"), citation);

		final TreeMap<Integer, Map<String, Object>> assertions = new TreeMap<>();
		store.put("assertion", assertions);
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
		store.put("source", sources);
		final Map<String, Object> source = new HashMap<>();
		source.put("id", 1);
		source.put("repository_id", 1);
		source.put("identifier", "source 1");
		sources.put((Integer)source.get("id"), source);

		final TreeMap<Integer, Map<String, Object>> repositories = new TreeMap<>();
		store.put("repository", repositories);
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repositories.put((Integer)repository1.get("id"), repository1);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
		localizedText2.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put("localized_text_junction", localizedTextJunctions);
		final Map<String, Object> localizedTextJunction = new HashMap<>();
		localizedTextJunction.put("id", 1);
		localizedTextJunction.put("localized_text_id", 2);
		localizedTextJunction.put("reference_type", "extract");
		localizedTextJunction.put("reference_table", "citation");
		localizedTextJunction.put("reference_id", 1);
		localizedTextJunctions.put((Integer)localizedTextJunction.get("id"), localizedTextJunction);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put("note", notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 2");
		note2.put("reference_table", "citation");
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "citation");
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final CitationDialog dialog = create(store, parent);
			final CitationDialog dialog = createSelectOnly(store, parent);
//			final CitationDialog dialog = createRecordOnly(store, parent);
//			dialog.withFilterOnSourceID(filterSourceID);
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
								.withReference(EntityManager.TABLE_NAME_CITATION, citationID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.TABLE_NAME_CITATION);
										insertRecordReferenceID(record, citationID);
									}
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(store, parent)
									: NoteDialog.create(store, parent))
								.withReference(EntityManager.TABLE_NAME_CITATION, citationID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.TABLE_NAME_CITATION);
										insertRecordReferenceID(record, citationID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(store, parent)
									: MediaDialog.createForMedia(store, parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.TABLE_NAME_CITATION, citationID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.TABLE_NAME_CITATION);
										insertRecordReferenceID(record, citationID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(store, parent)
									: AssertionDialog.create(store, parent))
								.withReference(EntityManager.TABLE_NAME_CITATION, citationID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(store, parent)
								: NoteDialog.createModificationNoteEditOnly(store, parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + citationID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(store, parent)
								: ResearchStatusDialog.createEditOnly(store, parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + citationID);
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
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
