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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocation;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtract;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordExtractType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocation;


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


	public static CitationDialog create(final Frame parent){
		final CitationDialog dialog = new CitationDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createSelectOnly(final Frame parent){
		final CitationDialog dialog = new CitationDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.transcribedExtractButton, dialog.noteButton, dialog.mediaButton, dialog.assertionButton);
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createShowOnly(final Frame parent){
		final CitationDialog dialog = new CitationDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static CitationDialog createEditOnly(final Frame parent){
		final CitationDialog dialog = new CitationDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private CitationDialog(final Frame parent){
		super(parent);
	}


	public CitationDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
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
		return EntityManager.NODE_CITATION;
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
		GUIHelper.bindLabelUndo(locationLabel, locationField);

		GUIHelper.bindLabel(extractLabel, extractTextPreview);
		extractTextPreview.setTextViewFont(extractLabel.getFont());
		extractTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);
		extractTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

		GUIHelper.bindLabelUndo(extractLocaleLabel, extractLocaleField);

		transcribedExtractButton.setToolTipText("Transcribed extract");
		transcribedExtractButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_EXTRACT, EntityManager.NODE_CITATION, selectedRecord)));

		GUIHelper.bindLabelUndoAutoComplete(extractTypeLabel, extractTypeComboBox);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_CITATION, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_CITATION, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_CITATION, selectedRecord)));
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

		final List<Map<String, Object>> records = (filterSourceID == null
			? Repository.findAll(EntityManager.NODE_CITATION)
			: Repository.findReferencingNodes(EntityManager.NODE_CITATION,
			EntityManager.NODE_SOURCE, filterSourceID,
			EntityManager.RELATIONSHIP_QUOTES));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String sourceIdentifier = extractRecordSourceIdentifier(recordID);
			final String location = extractRecordLocation(record);
			final StringJoiner identifier = new StringJoiner(StringUtils.SPACE);
			identifier.add((sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
				+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
				+ (location != null? location: StringUtils.EMPTY));
			final String extract = extractRecordExtract(record);
			if(extract != null && !extract.isEmpty())
				identifier.add("[" + extract + "]");
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(sourceIdentifier)
				.add(location);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
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
		final boolean hasTranscribedExtracts = Repository.hasTranscriptions(EntityManager.NODE_CITATION, citationID,
			EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_CITATION, citationID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_CITATION, citationID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_CITATION, citationID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_CITATION, citationID);

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
				final Map<String, Object> updatedCitationRecord = Repository.findByID(EntityManager.NODE_CITATION, recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(extractRecordID(updatedCitationRecord));
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

	private String extractRecordSourceIdentifier(final Integer citationID){
		final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(
			EntityManager.NODE_CITATION, citationID,
			EntityManager.RELATIONSHIP_QUOTES);
		if(referencedNode == null || !referencedNode.getKey().equals(EntityManager.NODE_SOURCE))
			return null;

		final Map<String, Object> source = referencedNode.getValue();
		return extractRecordIdentifier(source);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		int repository1ID = Repository.upsert(repository1, EntityManager.NODE_REPOSITORY);

		final Map<String, Object> source1 = new HashMap<>();
		source1.put("identifier", "source 1");
		int source1ID = Repository.upsert(source1, EntityManager.NODE_SOURCE);
		Repository.upsertRelationship(EntityManager.NODE_SOURCE, source1ID,
			EntityManager.NODE_REPOSITORY, repository1ID,
			EntityManager.RELATIONSHIP_STORED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> citation1 = new HashMap<>();
		citation1.put("location", "here");
		citation1.put("extract", "text 1");
		citation1.put("extract_locale", "en-US");
		citation1.put("extract_type", "transcript");
		int citation1ID = Repository.upsert(citation1, EntityManager.NODE_CITATION);
		Repository.upsertRelationship(EntityManager.NODE_CITATION, citation1ID,
			EntityManager.NODE_SOURCE, source1ID,
			EntityManager.RELATIONSHIP_QUOTES, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> assertion1 = new HashMap<>();
		assertion1.put("role", "father");
		assertion1.put("certainty", "certain");
		assertion1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int assertion1ID = Repository.upsert(assertion1, EntityManager.NODE_ASSERTION);
		Repository.upsertRelationship(EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_INFERRED_FROM, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "it");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		int localizedText1ID = Repository.upsert(localizedText1, EntityManager.NODE_LOCALIZED_TEXT);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("text", "text 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "kana");
		localizedText2.put("transcription_type", "romanized");
		int localizedText2ID = Repository.upsert(localizedText2, EntityManager.NODE_LOCALIZED_TEXT);

		final Map<String, Object> localizedTextRelationship1 = new HashMap<>();
		localizedTextRelationship1.put("type", "extract");
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, localizedText1ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, localizedText2ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, localizedTextRelationship1,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_CITATION, citation1ID,
			EntityManager.RELATIONSHIP_FOR, restriction1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final CitationDialog dialog = create(parent);
			final CitationDialog dialog = createSelectOnly(parent);
//			final CitationDialog dialog = createRecordOnly(parent);
//			dialog.withFilterOnSourceID(filterSourceID);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(source1)))
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
							final LocalizedTextDialog localizedTextDialog = LocalizedTextDialog.createSimpleText(parent)
								.withReference(EntityManager.NODE_CITATION, citationID, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_TEXT, recordID,
											EntityManager.NODE_CITATION, citationID,
											EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
								});
							localizedTextDialog.loadData();

							localizedTextDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_CITATION, citationID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_CITATION, citationID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_CITATION, citationID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_CITATION, citationID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_CITATION, citationID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
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
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
