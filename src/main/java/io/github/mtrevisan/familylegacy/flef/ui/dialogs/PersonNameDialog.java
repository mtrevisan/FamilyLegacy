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

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class PersonNameDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -3816108402093925220L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel personalNameLabel = new JLabel("(Personal) Name:");
	private final JTextField personalNameField = new JTextField();
	private final JLabel familyNameLabel = new JLabel("(Family) Name:");
	private final JTextField familyNameField = new JTextField();
	private final JLabel nameLocaleLabel = new JLabel("Locale:");
	private final JTextField nameLocaleField = new JTextField();
	private final JButton transcribedNameButton = new JButton("Transcribed names", ICON_TRANSLATION);
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "birth name", "also known as", "nickname",
		"family nickname", "pseudonym", "legal", "adoptive name", "stage name", "marriage name", "call name", "official name",
		"anglicized name", "religious order name", "pen name", "name at work", "immigrant"});

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private int filterReferenceID;


	public static PersonNameDialog create(final Frame parent){
		final PersonNameDialog dialog = new PersonNameDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static PersonNameDialog createSelectOnly(final Frame parent){
		final PersonNameDialog dialog = new PersonNameDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.transcribedNameButton, dialog.noteButton, dialog.mediaButton, dialog.assertionButton,
			dialog.culturalNormButton, dialog.eventButton);
		dialog.initialize();
		return dialog;
	}

	public static PersonNameDialog createRecordOnly(final Frame parent){
		final PersonNameDialog dialog = new PersonNameDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private PersonNameDialog(final Frame parent){
		super(parent);
	}


	public PersonNameDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public PersonNameDialog withReference(final int filterReferenceID){
		this.filterReferenceID = filterReferenceID;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceID > 0? " for person ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_PERSON_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Name"};
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
		GUIHelper.bindLabelUndo(personalNameLabel, personalNameField);
		GUIHelper.bindOnTextChange(personalNameField, this::saveData);
		GUIHelper.bindLabelUndo(familyNameLabel, familyNameField);
		GUIHelper.bindOnTextChange(familyNameField, this::saveData);
		addMandatoryField(personalNameField, familyNameField);
		GUIHelper.bindLabelUndo(nameLocaleLabel, nameLocaleField);
		GUIHelper.bindOnTextChange(nameLocaleField, this::saveData);

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.LOCALIZED_PERSON_NAME, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		GUIHelper.bindLabelUndoAutoComplete(typeLabel, typeComboBox);
		GUIHelper.bindOnSelectionChange(typeComboBox, this::saveData);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_PERSON_NAME, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personalNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(personalNameField, "grow,wrap related");
		recordPanelBase.add(familyNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(familyNameField, "grow,wrap related");
		recordPanelBase.add(nameLocaleLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(nameLocaleField, "grow,wrap related");
		recordPanelBase.add(transcribedNameButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "grow");

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
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceID <= 0
			? Repository.findAll(EntityManager.NODE_PERSON_NAME)
			: Repository.findReferencingNodes(EntityManager.NODE_PERSON_NAME,
				EntityManager.NODE_PERSON_NAME, filterReferenceID,
				EntityManager.RELATIONSHIP_FOR));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String identifier = extractIdentifier(extractRecordID(record));
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		personalNameField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer personNameID = extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String personalName = extractRecordPersonalName(selectedRecord);
		final String familyName = extractRecordFamilyName(selectedRecord);
		final String nameLocale = extractRecordLocale(selectedRecord);
		final boolean hasTransliterations = Repository.hasPersonNameTransliterations(EntityManager.NODE_PERSON_NAME, personNameID);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_PERSON_NAME, personNameID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_PERSON_NAME, personNameID);
		final boolean hasAssertions = Repository.hasAssertions(EntityManager.NODE_PERSON_NAME, personNameID);
		final boolean hasCulturalNorms = Repository.hasCulturalNorms(EntityManager.NODE_PERSON_NAME, personNameID);
		final boolean hasEvents = Repository.hasEvents(EntityManager.NODE_PERSON_NAME, personNameID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_PERSON_NAME, personNameID);

		personalNameField.setText(personalName);
		familyNameField.setText(familyName);
		nameLocaleField.setText(nameLocale);
		GUIHelper.addBorder(transcribedNameButton, hasTransliterations, DATA_BUTTON_BORDER_COLOR);
		typeComboBox.setSelectedItem(type);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(culturalNormButton, hasCulturalNorms);
		setButtonEnableAndBorder(eventButton, hasEvents);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
	}

	@Override
	protected void clearData(){
		personalNameField.setText(null);
		familyNameField.setText(null);
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
		final String personalName = GUIHelper.getTextTrimmed(personalNameField);
		final String familyName = GUIHelper.getTextTrimmed(familyNameField);
		if(!validData(personalName) && !validData(familyName)){
			JOptionPane.showMessageDialog(getParent(), "(Personal or family) Name field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			personalNameField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String personalName = GUIHelper.getTextTrimmed(personalNameField);
		final String familyName = GUIHelper.getTextTrimmed(familyNameField);
		final String nameLocale = GUIHelper.getTextTrimmed(nameLocaleField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);

		//update table:
		final Integer recordID = extractRecordID(selectedRecord);
		final DefaultTableModel model = getRecordTableModel();
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_ID).equals(recordID)){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				final StringJoiner name = new StringJoiner(", ");
				if(personalName != null)
					name.add(personalName);
				if(familyName != null)
					name.add(familyName);

				model.setValueAt(name.toString(), modelRowIndex, TABLE_INDEX_IDENTIFIER);

				break;
			}

		insertRecordPersonalName(selectedRecord, personalName);
		insertRecordFamilyName(selectedRecord, familyName);
		insertRecordLocale(selectedRecord, nameLocale);
		insertRecordType(selectedRecord, type);

		return true;
	}

	private String extractIdentifier(final int selectedRecordID){
		final Map<String, Object> storePersonNames = Repository.findByID(EntityManager.NODE_PERSON_NAME, selectedRecordID);
		final String personalName = extractRecordPersonalName(storePersonNames);
		final String familyName = extractRecordFamilyName(storePersonNames);
		final StringJoiner name = new StringJoiner(", ");
		if(personalName != null)
			name.add(personalName);
		if(familyName != null)
			name.add(familyName);
		return name.toString();
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		int person1ID = Repository.upsert(new HashMap<>(), EntityManager.NODE_PERSON);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("personal_name", "tòni");
		personName1.put("family_name", "bruxatin");
		personName1.put("locale", "vec-IT");
		personName1.put("type", "birth name");
		int personName1ID = Repository.upsert(personName1, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("personal_name", "antonio");
		personName2.put("family_name", "bruciatino");
		personName2.put("locale", "it-IT");
		personName2.put("type", "death name");
		int personName2ID = Repository.upsert(personName2, EntityManager.NODE_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personName2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		int localizedText1ID = Repository.upsert(localizedText1, EntityManager.NODE_LOCALIZED_TEXT);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		int localizedText2ID = Repository.upsert(localizedText2, EntityManager.NODE_LOCALIZED_TEXT);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 1");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final PersonNameDialog dialog = create(parent);
//			final PersonNameDialog dialog = createRecordOnly(parent);
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
							final LocalizedPersonNameDialog localizedPersonNameDialog = LocalizedPersonNameDialog.create(parent)
								.withReference(personNameID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, personNameID,
											EntityManager.NODE_PERSON, recordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
								});
							localizedPersonNameDialog.loadData();

							localizedPersonNameDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON_NAME, personNameID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_PERSON_NAME, personNameID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PERSON_NAME, personNameID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_PERSON_NAME, personNameID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(parent)
								.withReference(EntityManager.NODE_PERSON_NAME, personNameID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_CULTURAL_NORM, recordID,
											EntityManager.NODE_PERSON_NAME, personNameID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
								});
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON_NAME, personNameID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON_NAME, personNameID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + personNameID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + personNameID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + personNameID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + personNameID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_NEW -> {
							final int parentRecordID = extractRecordID(dialog.getSelectedRecord());
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = extractRecordID(container);
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, recordID,
											EntityManager.NODE_PERSON_NAME, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									else
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, recordID,
											EntityManager.NODE_PERSON_NAME, parentRecordID,
											EntityManager.RELATIONSHIP_FOR);

									//refresh research status table
									dialog.reloadResearchStatusTable();
								});
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("New research status for " + title + " " + parentRecordID);
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
