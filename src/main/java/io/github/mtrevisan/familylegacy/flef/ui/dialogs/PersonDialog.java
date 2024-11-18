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
import io.github.mtrevisan.familylegacy.flef.ui.panels.BelongsToGroupPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;


public final class PersonDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6043866696384851757L;


	private static final int TABLE_INDEX_IDENTIFIER = 2;
	public static final int TABLE_INDEX_DATA = 3;

	private static final String RECORD_PANEL_NAME_BASE = "base";
	private static final String RECORD_PANEL_NAME_OTHER = "other";


	private final JButton personNameButton = new JButton("Names", ICON_TEXT);
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JButton groupButton = new JButton("Groups", ICON_GROUP);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");


	public static PersonDialog create(final Frame parent){
		final PersonDialog dialog = new PersonDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createCollection(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final PersonDialog dialog = new PersonDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
		dialog.addViewOnlyComponents(dialog.personNameButton, dialog.photoButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createSelectOnly(final Frame parent){
		final PersonDialog dialog = new PersonDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.personNameButton, dialog.photoButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createCollectionViewOnly(final int filterGroupID,
			final BiFunction<String, Integer, BelongsToGroupPanel> panelCreator, final Frame parent){
		Objects.requireNonNull(panelCreator, "Relationship data panel creator cannot be null");

		final PersonDialog dialog = new PersonDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.filterCollectionTargetID = filterGroupID;
		dialog.showCollectionOnly = true;
		dialog.relationshipDataPanel = panelCreator.apply(dialog.getTableName(), filterGroupID);
		dialog.addViewOnlyComponents(dialog.personNameButton, dialog.photoButton,
			dialog.noteButton, dialog.mediaButton, dialog.assertionButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createShowOnly(final Frame parent){
		final PersonDialog dialog = new PersonDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createEditOnly(final Frame parent){
		final PersonDialog dialog = new PersonDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private PersonDialog(final Frame parent){
		super(parent);

		addButtonComponent(COMPONENT_ID_PERSON_NAME_BUTTON, personNameButton);
		addButtonComponent(COMPONENT_ID_PHOTO_BUTTON, photoButton);
		addButtonComponent(COMPONENT_ID_NOTE_BUTTON, noteButton);
		addButtonComponent(COMPONENT_ID_MEDIA_BUTTON, mediaButton);
		addButtonComponent(COMPONENT_ID_ASSERTION_BUTTON, assertionButton);
		addButtonComponent(COMPONENT_ID_EVENT_BUTTON, eventButton);
		addButtonComponent(COMPONENT_ID_GROUP_BUTTON, groupButton);
	}


	public PersonDialog withOnCloseGracefully(final Consumer<ModifiedRecords> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	public String getTableName(){
		return EntityManager.NODE_PERSON;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Name", "Data"};
	}

	@Override
	protected int[] getTableColumnAlignments(){
		return new int[]{SwingConstants.RIGHT, SwingConstants.LEFT, SwingConstants.LEFT, SwingConstants.LEFT};
	}

	@Override
	protected Comparator<?>[] getTableColumnComparators(){
		final Comparator<Integer> numericComparator = GUIHelper.getNumericComparator();
		final Comparator<String> textComparator = Comparator.naturalOrder();
		return new Comparator<?>[]{numericComparator, null, textComparator, null};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();

		//hide data column
		TableColumnModel columnModel = recordTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));

		//hide data column
		columnModel = collectionTable.getColumnModel();
		//NOTE: the filter column was already removed, therefore the `- 1`
		columnModel.removeColumn(columnModel.getColumn(TABLE_INDEX_DATA - 1));
	}

	@Override
	protected void initRecordComponents(){
		personNameButton.setToolTipText("Names");
		personNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PERSON_NAME, this, selectedRecord)));

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, this, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, this, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, this, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, this, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, this, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, this, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personNameButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(assertionButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(eventButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(groupButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add(RECORD_PANEL_NAME_BASE, recordPanelBase);
		recordTabbedPane.add(RECORD_PANEL_NAME_OTHER, recordPanelOther);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = Repository.findAll(EntityManager.NODE_PERSON);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		final DefaultTableModel collectionModel = (useCollection() && !collections.isEmpty()? getCollectionTableModel(): null);
		if(collectionModel != null)
			collectionModel.setRowCount(collections.size());
		int recordRow = 0;
		int collectionRow = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String identifier = extractIdentifier(recordID);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, recordRow, TABLE_INDEX_ID);
			model.setValueAt(filterData, recordRow, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, recordRow, TABLE_INDEX_IDENTIFIER);

			if(collectionModel != null && collections.containsKey(recordID)){
				final List<Map<String, Object>> relationships = Repository.findRelationships(EntityManager.NODE_PERSON, recordID,
					EntityManager.NODE_GROUP, filterCollectionTargetID,
					EntityManager.RELATIONSHIP_BELONGS_TO
				);
				final Map<String, Object> relationshipData = (!relationships.isEmpty()? relationships.getFirst(): new HashMap<>(0));

				collectionModel.setValueAt(recordID, collectionRow, TABLE_INDEX_ID);
//				collectionModel.setValueAt(filterData, collectionRow, TABLE_INDEX_FILTER);
				collectionModel.setValueAt(identifier, collectionRow, TABLE_INDEX_IDENTIFIER);
				collectionModel.setValueAt(relationshipData, collectionRow, TABLE_INDEX_DATA);

				collectionRow ++;
			}

			recordRow ++;
		}
	}

	@Override
	public void loadDataWithCollection(final int recordID){
		loadCollections(recordID);
		loadData();
	}

	@Override
	protected void addToCollection(){
		final JDialog dialog = new JDialog(this, "Relationship data", true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		final Integer recordID = extractRecordID(selectedRecord);
		final BelongsToGroupPanel panel = BelongsToGroupPanel.create(getTableName(), filterCollectionTargetID);
		panel.loadData(recordID);
		dialog.add(panel);

		dialog.pack();
		dialog.setLocationRelativeTo(this);
		//close dialog
		final ActionListener onCloseAction = e -> {
			final Map<String, Object> relationshipData = panel.getRelationshipData();

			//transfer selected record to collection table:
			final DefaultTableModel model = getCollectionTableModel();
			int row = model.getRowCount();
			model.setRowCount(row + 1);
			final String identifier = extractRecordIdentifier(selectedRecord);
//			final FilterString filter = FilterString.create()
//				.add(recordID)
//				.add(identifier);
//			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
//			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);
			model.setValueAt(relationshipData, row, TABLE_INDEX_DATA);


			finalizeAddToCollection(recordID, relationshipData);

			dialog.dispose();
		};
		dialog.getRootPane()
			.registerKeyboardAction(onCloseAction, GUIHelper.ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		dialog.setVisible(true);
	}

	@Override
	protected void fillData(){
		final Integer personID = extractRecordID(selectedRecord);
		final String restriction = Repository.getRestriction(EntityManager.NODE_PERSON, personID);

		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));


		refreshButtonStates(personID);
	}

	@Override
	public void refreshButtonStates(final int recordID){
		final String tableName = getTableName();
		final boolean hasPersonNames = Repository.hasPersonNames(tableName, recordID);
		final boolean hasPhoto = (Repository.getDepiction(tableName, recordID) != null);
		setButtonSelectEnableAndBorder(personNameButton, hasPersonNames);
		setButtonSelectEnableAndBorder(photoButton, hasPhoto);

		final boolean hasNotes = Repository.hasNotes(tableName, recordID);
		final boolean hasMedia = Repository.hasMedia(tableName, recordID);
		final boolean hasAssertions = Repository.hasAssertions(tableName, recordID);
		final boolean hasEvents = Repository.hasEvents(tableName, recordID);
		final boolean hasGroups = Repository.hasGroups(tableName, recordID);
		setButtonSelectEnableAndBorder(noteButton, hasNotes);
		setButtonSelectEnableAndBorder(mediaButton, hasMedia);
		setButtonSelectEnableAndBorder(assertionButton, hasAssertions);
		setButtonSelectEnableAndBorder(eventButton, hasEvents);
		setButtonSelectEnableAndBorder(groupButton, hasGroups);
	}

	@Override
	protected void clearData(){
		GUIHelper.setDefaultBorder(personNameButton);
		GUIHelper.setDefaultBorder(photoButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(eventButton);
		GUIHelper.setDefaultBorder(groupButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		return true;
	}

	@Override
	protected boolean saveData(){
		return false;
	}

	private String extractIdentifier(final Integer personID){
		final StringJoiner identifier = new StringJoiner(" / ");
		extractAllPersonNames(personID, identifier);
		return identifier.toString();
	}

	private void extractAllPersonNames(final Integer personID, final StringJoiner identifier){
		final List<Map<String, Object>> storePersonNames = Repository.findReferencingNodes(
			EntityManager.NODE_PERSON_NAME,
			EntityManager.NODE_PERSON, personID,
			EntityManager.RELATIONSHIP_FOR);
		for(final Map<String, Object> storePersonName : storePersonNames){
			final StringJoiner subIdentifier = new StringJoiner(" / ");
			final List<String> personAllNames = extractPersonNames(storePersonName);
			personAllNames.forEach(subIdentifier::add);

			if(subIdentifier.length() > 0)
				identifier.add(subIdentifier.toString());
		}
	}

	private List<String> extractPersonNames(final Map<String, Object> personNameRecord){
		final int personNameID = extractRecordID(personNameRecord);
		final List<Map<String, Object>> localizedPersonNames = Repository.findReferencingNodes(EntityManager.NODE_LOCALIZED_PERSON_NAME,
			EntityManager.NODE_PERSON_NAME, personNameID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);
		final Set<String> names = new TreeSet<>();
		names.add(extractSinglePersonName(personNameRecord));
		for(int i = 0, length = localizedPersonNames.size(); i < length; i ++){
			final Map<String, Object> localizedPersonName = localizedPersonNames.get(i);

			//extract transliterations
			names.add(extractSinglePersonName(localizedPersonName));
		}
		return names.stream()
			.filter(name -> !name.isEmpty())
			.toList();
	}

	private static String extractSinglePersonName(final Map<String, Object> personNameRecord){
		final String personalName = extractRecordPersonalName(personNameRecord);
		final String familyName = extractRecordFamilyName(personNameRecord);
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

		final Map<String, Object> person1 = new HashMap<>();
		person1.put("photo_crop", "0 0 5 10");
		int person1ID = Repository.upsert(person1, EntityManager.NODE_PERSON);

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

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("locale", "en");
		int localizedPersonName1ID = Repository.upsert(localizedPersonName1, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName1ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("locale", "en");
		int localizedPersonName2ID = Repository.upsert(localizedPersonName2, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName2ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("personal_name", RECORD_PANEL_NAME_OTHER);
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("locale", "en");
		int localizedPersonName3ID = Repository.upsert(localizedPersonName3, EntityManager.NODE_LOCALIZED_PERSON_NAME);
		Repository.upsertRelationship(EntityManager.NODE_LOCALIZED_PERSON_NAME, localizedPersonName3ID,
			EntityManager.NODE_PERSON_NAME, personName1ID,
			EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> historicDate1 = new HashMap<>();
		historicDate1.put("date", "27 FEB 1976");
		historicDate1.put("date_original", "FEB 27, 1976");
		historicDate1.put("certainty", "certain");
		historicDate1.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		int historicDate1ID = Repository.upsert(historicDate1, EntityManager.NODE_HISTORIC_DATE);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		int media1ID = Repository.upsert(media1, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media1ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_CREATED_ON, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		int media2ID = Repository.upsert(media2, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media2ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_CREATED_ON, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		int media3ID = Repository.upsert(media3, EntityManager.NODE_MEDIA);
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media3ID,
			EntityManager.NODE_HISTORIC_DATE, historicDate1ID,
			EntityManager.RELATIONSHIP_CREATED_ON, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_PERSON, person1ID,
			EntityManager.NODE_MEDIA, media3ID,
			EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media1ID,
			EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> mediaRelationship2 = new HashMap<>();
		mediaRelationship2.put("photo_crop", "0 0 10 50");
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media2ID,
			EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_FOR, mediaRelationship2,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		Repository.upsertRelationship(EntityManager.NODE_MEDIA, media3ID,
			EntityManager.NODE_PERSON, 1,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("note", "note 2");
		int note2ID = Repository.upsert(note2, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note2ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_PERSON, person1ID,
			EntityManager.RELATIONSHIP_FOR, EntityManager.DATA_RELATIONSHIP_TYPE_ONE_TO_ONE,
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final PersonDialog dialog = create(parent);
//			final PersonDialog dialog = createRecordOnly(parent);
//			final PersonDialog dialog = createSelectOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(person1)))
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
					final int personID = extractRecordID(container);
					final Map<String, Object> photoRecord = Repository.getDepiction(EntityManager.NODE_PERSON, personID);
					final Integer photoID = (photoRecord != null? extractRecordID(photoRecord): null);
					switch(editCommand.getType()){
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PERSON_NAME_BUTTON)
									? PersonNameDialog.createSelectOnly(parent)
									: PersonNameDialog.create(parent))
								.withReference(personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_PERSON_NAME);
										Repository.upsertRelationship(EntityManager.NODE_PERSON_NAME, upsertedRecordID,
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									//update table identifier
									dialog.loadData();

									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_PERSON_NAME, deletedIDs.get(i),
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}

						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_PHOTO_BUTTON)
									? MediaDialog.createSelectOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PERSON, personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_PERSON, personID,
											EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.RELATIONSHIP_DEPICTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_PERSON, personID,
											EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_DEPICTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							photoDialog.loadData();
							boolean selected = false;
							if(photoID != null)
								selected = photoDialog.selectData(photoID);
							if(!selected)
								photoDialog.showNewRecord();

							photoDialog.showDialog();
						}

						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_NOTE_BUTTON)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON, personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_NOTE);
										Repository.upsertRelationship(EntityManager.NODE_NOTE, upsertedRecordID,
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_NOTE, deletedIDs.get(i),
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}

						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_MEDIA_BUTTON)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_PERSON, personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_MEDIA);
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, upsertedRecordID,
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_MEDIA, deletedIDs.get(i),
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_ASSERTION_BUTTON)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON, personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_ASSERTION);
										Repository.upsertRelationship(EntityManager.NODE_PERSON, personID,
											EntityManager.NODE_ASSERTION, upsertedRecordID,
											EntityManager.RELATIONSHIP_SUPPORTED_BY, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_PERSON, personID,
											EntityManager.NODE_ASSERTION, deletedIDs.get(i),
											EntityManager.RELATIONSHIP_SUPPORTED_BY);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}

						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(COMPONENT_ID_EVENT_BUTTON)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_PERSON, personID)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_EVENT);
										Repository.upsertRelationship(EntityManager.NODE_EVENT, upsertedRecordID,
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_EVENT, deletedIDs.get(i),
											EntityManager.NODE_PERSON, personID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									if(!deletedIDs.isEmpty())
										dialog.refreshButtonStates(personID);
								});
							eventDialog.loadData();

							eventDialog.showDialog();
						}

						case GROUP -> {
							final GroupDialog groupDialog = GroupDialog.createShowOnly(parent)
								.withReference(EntityManager.NODE_PERSON, personID);
							groupDialog.loadData();

							groupDialog.showDialog();
						}

						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + personID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + personID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_NEW -> {
							final int parentRecordID = extractRecordID(dialog.getSelectedRecord());
							final String tableName = editCommand.getDialog().getTableName();
							final Integer researchStatusID = extractRecordID(container);
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent)
								.withOnCloseGracefully(modifiedRecords -> {
									for(final Map<String, Object> upsertedRecord : modifiedRecords.getUpsertedRecords()){
										final int upsertedRecordID = Repository.upsert(upsertedRecord, EntityManager.NODE_RESEARCH_STATUS);
										Repository.upsertRelationship(EntityManager.NODE_RESEARCH_STATUS, upsertedRecordID,
											EntityManager.NODE_ASSERTION, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									}
									final List<Integer> deletedIDs = modifiedRecords.getRemovedIDs();
									for(int i = 0, length = deletedIDs.size(); i < length; i ++)
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, deletedIDs.get(i),
											EntityManager.NODE_ASSERTION, parentRecordID,
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
