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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRole;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCertainty;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCredibility;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordRole;


public final class AssertionDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -28220354680747790L;

	private static final int TABLE_INDEX_REFERENCE_TABLE = 2;


	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JLabel certaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private String filterReferenceTable;
	private int filterReferenceID;


	public static AssertionDialog create(final Frame parent){
		final AssertionDialog dialog = new AssertionDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static AssertionDialog createSelectOnly(final Frame parent){
		final AssertionDialog dialog = new AssertionDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.noteButton, dialog.mediaButton, dialog.culturalNormButton);
		dialog.initialize();
		return dialog;
	}

	public static AssertionDialog createRecordOnly(final Frame parent){
		final AssertionDialog dialog = new AssertionDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private AssertionDialog(final Frame parent){
		super(parent);
	}


	public AssertionDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public AssertionDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_ASSERTION;
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
		GUIHelper.bindLabelUndo(roleLabel, roleField);

		GUIHelper.bindLabelUndoAutoComplete(certaintyLabel, certaintyComboBox);

		GUIHelper.bindLabelUndoAutoComplete(credibilityLabel, credibilityComboBox);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_ASSERTION, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_ASSERTION, selectedRecord)));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, EntityManager.NODE_ASSERTION, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(roleLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(roleField, "grow,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup lbl,split 2");
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
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceTable == null
			? Repository.findAll(EntityManager.NODE_ASSERTION)
			: Repository.findReferencingNodes(EntityManager.NODE_ASSERTION,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_SUPPORTED_BY));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String sourceIdentifier = extractRecordSourceIdentifier(record);
			final String location = extractRecordLocation(recordID);
			final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(
				EntityManager.NODE_ASSERTION, recordID,
				EntityManager.RELATIONSHIP_SUPPORTED_BY);
			final String referenceTable = (referencedNode != null? referencedNode.getKey(): null);
			final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
				+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
				+ (location != null? location: StringUtils.EMPTY)
				+ (location != null && referenceTable != null? " for ": StringUtils.EMPTY)
				+ (referenceTable != null? referenceTable: StringUtils.EMPTY);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(sourceIdentifier)
				.add(location)
				.add(referenceTable);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_REFERENCE_TABLE);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		roleField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer assertionID = extractRecordID(selectedRecord);
		final String role = extractRecordRole(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_ASSERTION, assertionID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_ASSERTION, assertionID);
		final boolean hasCulturalNorms = Repository.hasCulturalNorms(EntityManager.NODE_ASSERTION, assertionID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_ASSERTION, assertionID);

		roleField.setText(role);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(culturalNormButton, hasCulturalNorms);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
	}

	@Override
	protected void clearData(){
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
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String role = GUIHelper.getTextTrimmed(roleField);
		final String certainty = GUIHelper.getTextTrimmed(certaintyComboBox);
		final String credibility = GUIHelper.getTextTrimmed(credibilityComboBox);

		//update table:
		final DefaultTableModel model = getRecordTableModel();
		final Integer recordID = extractRecordID(selectedRecord);
		for(int row = 0, length = model.getRowCount(); row < length; row ++){
			final int viewRowIndex = recordTable.convertRowIndexToView(row);
			final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

			if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
				final Map<String, Object> updatedAssertionRecord = Repository.findByID(EntityManager.NODE_ASSERTION, recordID);
				final String sourceIdentifier = extractRecordSourceIdentifier(updatedAssertionRecord);
				final String location = extractRecordLocation(extractRecordID(updatedAssertionRecord));
				final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(
					EntityManager.NODE_ASSERTION, recordID,
					EntityManager.RELATIONSHIP_SUPPORTED_BY);
				final String referenceTable = (referencedNode != null? referencedNode.getKey(): null);
				final String identifier = (sourceIdentifier != null? sourceIdentifier: StringUtils.EMPTY)
					+ (sourceIdentifier != null && location != null? " at ": StringUtils.EMPTY)
					+ (location != null? location: StringUtils.EMPTY)
					+ (location != null && referenceTable != null? " for ": StringUtils.EMPTY)
					+ (referenceTable != null? referenceTable: StringUtils.EMPTY);

				model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_REFERENCE_TABLE);

				break;
			}
		}

		insertRecordRole(selectedRecord, role);
		insertRecordCertainty(selectedRecord, certainty);
		insertRecordCredibility(selectedRecord, credibility);

		return true;
	}


	private String extractRecordLocation(final Integer assertionID){
		final Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(
			EntityManager.NODE_ASSERTION, assertionID,
			EntityManager.RELATIONSHIP_INFERRED_FROM);
		if(referencedNode == null || !referencedNode.getKey().equals(EntityManager.NODE_CITATION))
			return null;

		final Map<String, Object> citation = referencedNode.getValue();
		return EntityManager.extractRecordLocation(citation);
	}

	private String extractRecordSourceIdentifier(final Map<String, Object> assertionRecord){
		Map.Entry<String, Map<String, Object>> referencedNode = Repository.findReferencedNode(
			EntityManager.NODE_ASSERTION, extractRecordID(assertionRecord),
			EntityManager.RELATIONSHIP_INFERRED_FROM);

		if(referencedNode == null || !referencedNode.getKey().equals(EntityManager.NODE_CITATION))
			return null;

		final Map<String, Object> citation = referencedNode.getValue();


		referencedNode = Repository.findReferencedNode(EntityManager.NODE_CITATION, extractRecordID(citation),
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

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 2");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note3 = new HashMap<>();
		note3.put("note", "something to say");
		int note3ID = Repository.upsert(note3, EntityManager.NODE_NOTE);
		final Map<String, Object> note4 = new HashMap<>();
		note4.put("note", "something more to say");
		int note4ID = Repository.upsert(note4, EntityManager.NODE_NOTE);

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> modification1 = new HashMap<>();
		modification1.put("creation_date", EntityManager.now());
		modification1.put("update_date", EntityManager.now());
		int modification1ID = Repository.upsert(modification1, EntityManager.NODE_MODIFICATION, EntityManager.NODE_APPLICATION);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note3ID,
			EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_MODIFICATION, modification1ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> modification2 = new HashMap<>();
		modification2.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		modification2.put("update_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		int modification2ID = Repository.upsert(modification2, EntityManager.NODE_MODIFICATION, EntityManager.NODE_APPLICATION);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note4ID,
			EntityManager.NODE_MODIFICATION, modification2ID,
			EntityManager.RELATIONSHIP_CHANGELOG_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		Repository.upsertRelationship(EntityManager.NODE_MODIFICATION, modification2ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> culturalNorm1 = new HashMap<>();
		culturalNorm1.put("identifier", "rule 1 id");
		culturalNorm1.put("description", "rule 1");
		culturalNorm1.put("certainty", "certain");
		culturalNorm1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		final int culturalNorm1ID = Repository.upsert(culturalNorm1, EntityManager.NODE_CULTURAL_NORM);

		final Map<String, Object> culturalNormRelationship1 = new HashMap<>();
		culturalNormRelationship1.put("certainty", "probable");
		culturalNormRelationship1.put("credibility", "probable");
		Repository.upsertRelationship(EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.NODE_CULTURAL_NORM, culturalNorm1ID,
			EntityManager.RELATIONSHIP_SUPPORTED_BY, culturalNormRelationship1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> researchStatus1 = new HashMap<>();
		researchStatus1.put("identifier", "identifier 1");
		researchStatus1.put("description", "some description");
		researchStatus1.put("status", "open");
		researchStatus1.put("priority", 0);
		researchStatus1.put("creation_date", EntityManager.now());
		int researchStatus1ID = Repository.upsert(researchStatus1, EntityManager.NODE_RESEARCH_STATUS, EntityManager.NODE_APPLICATION);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus1ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> researchStatus2 = new HashMap<>();
		researchStatus2.put("identifier", "identifier 2");
		researchStatus2.put("description", "another description");
		researchStatus2.put("status", "active");
		researchStatus2.put("priority", 1);
		researchStatus2.put("creation_date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().minusDays(1)));
		int researchStatus2ID = Repository.upsert(researchStatus2, EntityManager.NODE_RESEARCH_STATUS, EntityManager.NODE_APPLICATION);
		Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, researchStatus2ID,
			EntityManager.NODE_ASSERTION, assertion1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final AssertionDialog dialog = create(parent);
//			final AssertionDialog dialog = createRecordOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(assertion1)))
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
					final int assertionID = extractRecordID(container);
					switch(editCommand.getType()){
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_ASSERTION, assertionID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_ASSERTION, assertionID,
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
								.withReference(EntityManager.NODE_ASSERTION, assertionID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_ASSERTION, assertionID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(parent)
								.withReference(EntityManager.NODE_ASSERTION, assertionID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_ASSERTION, assertionID,
											EntityManager.NODE_CULTURAL_NORM, recordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + assertionID);
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
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + assertionID);
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
