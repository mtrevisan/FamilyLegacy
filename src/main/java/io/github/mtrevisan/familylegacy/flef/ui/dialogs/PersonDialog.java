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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocalizedTextID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceTable;


public final class PersonDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6043866696384851757L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;


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

	public static PersonDialog createSelectOnly(final Frame parent){
		final PersonDialog dialog = new PersonDialog(parent);
		dialog.selectRecordOnly = true;
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
	}


	public PersonDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_NAME_PERSON;
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
		personNameButton.setToolTipText("Names");
		personNameButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PERSON_NAME, EntityManager.NODE_NAME_PERSON, selectedRecord)));

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, EntityManager.NODE_NAME_PERSON, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_NAME_PERSON, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_NAME_PERSON, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, EntityManager.NODE_NAME_PERSON, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, EntityManager.NODE_NAME_PERSON, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, EntityManager.NODE_NAME_PERSON, selectedRecord)));

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

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		unselectAction();

		final Map<Integer, Map<String, Object>> records = getRecords(EntityManager.NODE_NAME_PERSON);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractIdentifier(extractRecordID(container));
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
	protected void fillData(){
		final Integer personID = extractRecordID(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final String photoCrop = extractRecordPhotoCrop(selectedRecord);
		final boolean hasPersonNames = (getRecords(EntityManager.NODE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasNotes = (getRecords(EntityManager.NODE_NAME_NOTE)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasMedia = (getRecords(EntityManager.NODE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasAssertions = (getRecords(EntityManager.NODE_NAME_ASSERTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasEvents = (getRecords(EntityManager.NODE_NAME_EVENT)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasGroups = (getRecords(EntityManager.NODE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(EntityManager.NODE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(EntityManager.NODE_NAME_PERSON, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);

		setButtonEnableAndBorder(personNameButton, hasPersonNames);
		setButtonEnableAndBorder(photoButton, photoID != null);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(assertionButton, hasAssertions);
		setButtonEnableAndBorder(eventButton, hasEvents);
		setButtonEnableAndBorder(groupButton, hasGroups);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
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
		final NavigableMap<Integer, Map<String, Object>> localizedPersonNames = getRecords(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME);
		getRecords(EntityManager.NODE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.forEach(record -> {
				//extract transliterations
				final StringJoiner subIdentifier = new StringJoiner(", ");
				final Integer personNameID = extractRecordID(record);
				getFilteredRecords(EntityManager.NODE_NAME_LOCALIZED_TEXT_JUNCTION, EntityManager.NODE_NAME_PERSON_NAME, personNameID)
					.values().stream()
					.filter(record2 -> Objects.equals(EntityManager.LOCALIZED_TEXT_TYPE_NAME, extractRecordReferenceType(record2)))
					.map(record2 -> localizedPersonNames.get(extractRecordLocalizedTextID(record2)))
					.forEach(record2 -> subIdentifier.add(extractName(record2)));

				identifier.add(extractName(record) + (subIdentifier.length() > 0? " (" + subIdentifier + ")": StringUtils.EMPTY));
			});
		return identifier.toString();
	}

	private static String extractName(final Map<String, Object> record){
		final String personalName = extractRecordPersonalName(record);
		final String familyName = extractRecordFamilyName(record);
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
		person1.put("id", 1);
		person1.put("photo_id", 3);
		person1.put("photo_crop", "0 0 5 10");
		Repository.save(EntityManager.NODE_NAME_PERSON, person1);

		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "tòni");
		personName1.put("family_name", "bruxatin");
		personName1.put("locale", "vec-IT");
		personName1.put("type", "birth name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "antonio");
		personName2.put("family_name", "bruciatino");
		personName2.put("locale", "it-IT");
		personName2.put("type", "death name");
		Repository.save(EntityManager.NODE_NAME_PERSON_NAME, personName2);

		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_TEXT, localizedText2);

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName1);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("id", 2);
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName2);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("id", 3);
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("locale", "en");
		Repository.save(EntityManager.NODE_NAME_LOCALIZED_PERSON_NAME, localizedPersonName3);

		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		Repository.save(EntityManager.NODE_NAME_MEDIA, media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		media2.put("date_id", 1);
		Repository.save(EntityManager.NODE_NAME_MEDIA, media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		media3.put("date_id", 1);
		Repository.save(EntityManager.NODE_NAME_MEDIA, media3);

		final Map<String, Object> mediaJunction1 = new HashMap<>();
		Repository.upsertRelationship(EntityManager.NODE_NAME_MEDIA, extractRecordID(media1), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_FOR, mediaJunction1, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> mediaJunction2 = new HashMap<>();
		mediaJunction2.put("photo_crop", "0 0 10 50");
		Repository.upsertRelationship(EntityManager.NODE_NAME_MEDIA, extractRecordID(media2), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_FOR, mediaJunction2, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
		final Map<String, Object> mediaJunction3 = new HashMap<>();
		Repository.upsertRelationship(EntityManager.NODE_NAME_MEDIA, extractRecordID(media3), EntityManager.NODE_NAME_PERSON, 1,
			EntityManager.RELATIONSHIP_NAME_FOR, mediaJunction3, GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 2");
		note2.put("reference_table", "person");
		note2.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_NOTE, note2);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", "person");
		restriction1.put("reference_id", 1);
		Repository.save(EntityManager.NODE_NAME_RESTRICTION, restriction1);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final PersonDialog dialog = create(parent);
//			final PersonDialog dialog = createRecordOnly(parent);
			final PersonDialog dialog = createSelectOnly(parent);
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
					final Integer photoID = extractRecordPhotoID(container);
					switch(editCommand.getType()){
						case ASSERTION -> {
							final AssertionDialog assertionDialog = (dialog.isViewOnlyComponent(dialog.assertionButton)
									? AssertionDialog.createSelectOnly(parent)
									: AssertionDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_PERSON, personID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = (dialog.isViewOnlyComponent(dialog.personNameButton)
									? PersonNameDialog.createSelectOnly(parent)
									: PersonNameDialog.create(parent))
								.withReference(personID)
								.withOnCloseGracefully((record, recordID) -> {
									insertRecordPersonID(record, personID);

									//update table identifier
									dialog.loadData();
								});
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}
						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(dialog.photoButton)
									? MediaDialog.createEditOnlyForPhoto(parent)
									: MediaDialog.createForPhoto(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_NAME_PERSON, personID)
								.withOnCloseGracefully((record, recordID) -> {
									final Integer newPhotoID = extractRecordID(record);
									insertRecordPhotoID(container, newPhotoID);
								});
							photoDialog.loadData();
							if(photoID != null){
								//add photo manually because is not retrievable through a junction
								photoDialog.addData(container);
								photoDialog.selectData(photoID);
							}
							else
								photoDialog.showNewRecord();

							photoDialog.showDialog();
						}
//						case PHOTO_CROP -> {
//							final PhotoCropDialog photoCropDialog = (dialog.isViewOnlyComponent(dialog.photoCropButton)
//								? PhotoCropDialog.createSelectOnly(parent)
//								: PhotoCropDialog.create(parent));
//							photoCropDialog.withOnCloseGracefully((record, recordID) -> {
//									final Rectangle crop = photoCropDialog.getCrop();
//									if(crop != null){
//										final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
//										sj.add(Integer.toString(crop.x))
//											.add(Integer.toString(crop.y))
//											.add(Integer.toString(crop.width))
//											.add(Integer.toString(crop.height));
//										insertRecordPhotoCrop(container, sj.toString());
//									}
//								});
//							try{
//								if(photoID != null){
//									final String photoCrop = extractRecordPhotoCrop(container);
//									photoCropDialog.loadData(photoID, photoCrop);
//								}
//
//								photoCropDialog.setSize(420, 295);
//								photoCropDialog.showDialog();
//							}
//							catch(final IOException ignored){}
//						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(parent)
									: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_PERSON, personID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.NODE_NAME_PERSON);
										insertRecordReferenceID(record, personID);
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
								.withReference(EntityManager.NODE_NAME_PERSON, personID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null){
										insertRecordReferenceTable(record, EntityManager.NODE_NAME_PERSON);
										insertRecordReferenceID(record, personID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(parent)
									: EventDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_PERSON, personID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case GROUP -> {
							final GroupDialog groupDialog = (dialog.isViewOnlyComponent(dialog.groupButton)
									? GroupDialog.createSelectOnly(parent)
									: GroupDialog.create(parent))
								.withReference(EntityManager.NODE_NAME_PERSON, personID);
							groupDialog.loadData();

							groupDialog.showDialog();
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
