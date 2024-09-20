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
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordRepositoryID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordIdentifier;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class RepositoryDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6136508398081805353L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


	private final JLabel identifierLabel = new JLabel("Identifier:");
	private final JTextField identifierField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "public library", "college library",
		"national library", "prison library", "national archives", "website", "personal collection", "cemetery/mausoleum", "museum",
		"state library", "religious library", "genealogy society collection", "government agency", "funeral home"});
	private final JButton referencePersonButton = new JButton("Reference person", ICON_PERSON);
	private final JButton placeButton = new JButton("Place", ICON_PLACE);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JButton sourcesButton = new JButton("Sources", ICON_SOURCE);


	public static RepositoryDialog create(final Frame parent){
		final RepositoryDialog dialog = new RepositoryDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static RepositoryDialog createSelectOnly(final Frame parent){
		final RepositoryDialog dialog = new RepositoryDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.referencePersonButton, dialog.placeButton, dialog.noteButton, dialog.mediaButton,
			dialog.sourcesButton);
		dialog.initialize();
		return dialog;
	}

	public static RepositoryDialog createShowOnly(final Frame parent){
		final RepositoryDialog dialog = new RepositoryDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static RepositoryDialog createEditOnly(final Frame parent){
		final RepositoryDialog dialog = new RepositoryDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private RepositoryDialog(final Frame parent){
		super(parent);
	}


	public RepositoryDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_NAME_REPOSITORY;
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
		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, this::saveData);
		addMandatoryField(identifierField);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		referencePersonButton.setToolTipText("Reference person");
		referencePersonButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PERSON, EntityManager.NODE_NAME_REPOSITORY, selectedRecord)));

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, EntityManager.NODE_NAME_REPOSITORY, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_NAME_REPOSITORY, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_NAME_REPOSITORY, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		sourcesButton.setToolTipText("Sources");
		sourcesButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.SOURCE, EntityManager.NODE_NAME_REPOSITORY, selectedRecord)));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "wrap paragraph");
		recordPanelBase.add(referencePersonButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(placeButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelChildren = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelChildren.add(sourcesButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("children", recordPanelChildren);
	}

	@Override
	public void loadData(){
		unselectAction();

		final Map<Integer, Map<String, Object>> records = getRecords(EntityManager.NODE_NAME_REPOSITORY);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier);
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
		identifierField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer repositoryID = extractRecordID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final Integer personID = extractRecordPersonID(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final boolean hasNotes = (getRecords(EntityManager.NODE_NAME_NOTE)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_REPOSITORY, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(repositoryID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasMedia = (getRecords(EntityManager.NODE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_REPOSITORY, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(repositoryID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(EntityManager.NODE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_REPOSITORY, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(repositoryID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);
		final boolean hasSources = (getRecords(EntityManager.NODE_NAME_SOURCE)
			.values().stream()
			.filter(record -> Objects.equals(repositoryID, extractRecordRepositoryID(record)))
			.findFirst()
			.orElse(null) != null);

		identifierField.setText(identifier);
		typeComboBox.setSelectedItem(type);
		setButtonEnableAndBorder(referencePersonButton, personID != null);
		setButtonEnableAndBorder(placeButton, placeID != null);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));

		setButtonEnableAndBorder(sourcesButton, hasSources);
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(referencePersonButton);
		GUIHelper.setDefaultBorder(placeButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);

		GUIHelper.setDefaultBorder(sourcesButton);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		if(!validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			identifierField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		final String identifier = GUIHelper.getTextTrimmed(identifierField);
		if(identifier != null && !validData(identifier))
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeComboBox);

		//update table:
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_IDENTIFIER);

					break;
				}
		}

		insertRecordIdentifier(selectedRecord, identifier);
		insertRecordType(selectedRecord, type);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repository1.put("person_id", 1);
		repository1.put("place_id", 2);
		Repository.save(EntityManager.NODE_NAME_REPOSITORY, repository1);
		final Map<String, Object> repository2 = new HashMap<>();
		repository2.put("id", 2);
		repository2.put("identifier", "repo 2");
		repository2.put("type", "college library");
		Repository.save(EntityManager.NODE_NAME_REPOSITORY, repository2);
		final Map<String, Object> repository3 = new HashMap<>();
		repository3.put("id", 3);
		repository3.put("identifier", "repo 3");
		repository3.put("type", "private library");
		Repository.save(EntityManager.NODE_NAME_REPOSITORY, repository3);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place ident");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		place1.put("type", "province");
		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		place1.put("photo_id", 1);
		place1.put("photo_crop", "0 0 10 20");
		Repository.save(EntityManager.NODE_NAME_PLACE, place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "another place ident");
		place2.put("name", "name of another place");
		place2.put("locale", "en-US");
		place2.put("type", "custom");
		Repository.save(EntityManager.NODE_NAME_PLACE, place2);

		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		person1.put("photo_crop", "0 0 5 10");
		Repository.save(EntityManager.NODE_NAME_PERSON, person1);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place name 1");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "place name 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "IPA");
		localizedText2.put("transcription_type", "romanized");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText2);
		final Map<String, Object> localizedText3 = new HashMap<>();
		localizedText3.put("id", 3);
		localizedText3.put("text", "true name");
		localizedText3.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText3);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName1);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 1");
		note2.put("reference_table", "repository");
		note2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note2);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		Repository.save(EntityManager.NODE_NAME_MEDIA, media1);

		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("photo_crop", "0 0 10 20");
		Repository.upsertRelationship(EntityManager.NODE_NAME_MEDIA, extractRecordID(media1), EntityManager.NODE_NAME_REPOSITORY, 1,
			EntityManager.RELATIONSHIP_NAME_FOR, mediaJunction1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "repository");
		restriction1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_RESTRICTION, restriction1);

		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1 APA-style");
		source1.put("place_id", 2);
		source1.put("date_id", 1);
		source1.put("repository_id", 1);
		source1.put("location", "location 1");
		Repository.save(EntityManager.NODE_NAME_SOURCE, source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2 APA-style");
		source2.put("place_id", 2);
		source2.put("date_id", 2);
		source2.put("repository_id", 2);
		source2.put("location", "location 2");
		Repository.save(EntityManager.NODE_NAME_SOURCE, source2);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final RepositoryDialog dialog = create(parent);
			final RepositoryDialog dialog = createShowOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(repository1)))
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
					final String tableName = editCommand.getIdentifier();
					final int repositoryID = extractRecordID(container);
					switch(editCommand.getType()){
						case PERSON -> {
							final PersonDialog personDialog = (dialog.showRecordOnly
									? PersonDialog.createShowOnly(parent)
									: PersonDialog.create(parent))
								.withOnCloseGracefully((record, recordID) -> insertRecordPersonID(container, extractRecordID(record)));
							personDialog.loadData();
							final Integer personID = extractRecordPersonID(container);
							if(personID != null)
								personDialog.selectData(personID);

							personDialog.showDialog();
						}
						case PLACE -> {
							final PlaceDialog placeDialog = (dialog.showRecordOnly
									? PlaceDialog.createShowOnly(parent)
									: PlaceDialog.create(parent))
								.withOnCloseGracefully((record, recordID) -> insertRecordPlaceID(container, extractRecordID(record)));
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(tableName, repositoryID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null){
										insertRecordReferenceTable(record, tableName);
										insertRecordReferenceID(record, repositoryID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(parent)
									: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(tableName, repositoryID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.NODE_NAME_REPOSITORY);
										insertRecordReferenceID(record, repositoryID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case SOURCE -> {
							final SourceDialog sourceDialog = SourceDialog.create(parent)
								.withFilterOnRepositoryID(repositoryID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										record.put("repository_id", repositoryID);
								});
							sourceDialog.loadData();

							sourceDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + repositoryID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + repositoryID);
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
