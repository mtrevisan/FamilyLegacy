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

import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
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
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordLocalizedTextID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceType;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPersonID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPhotoCrop;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordPhotoID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceTable;


public final class PersonDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6043866696384851757L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;

	private static final String TABLE_NAME = "person";
	private static final String TABLE_NAME_PERSON_NAME = "person_name";
	private static final String TABLE_NAME_LOCALIZED_PERSON_NAME = "localized_person_name";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_EVENT = "event";
	private static final String TABLE_NAME_GROUP_JUNCTION = "group_junction";


	private final JButton personNameButton = new JButton("Names", ICON_TEXT);
	private final JButton photoButton = new JButton("Photo", ICON_PHOTO);
	private final JButton photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton assertionButton = new JButton("Assertions", ICON_ASSERTION);
	private final JButton eventButton = new JButton("Events", ICON_EVENT);
	private final JButton groupButton = new JButton("Groups", ICON_GROUP);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");


	public static PersonDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final PersonDialog dialog = new PersonDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final PersonDialog dialog = new PersonDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.addViewOnlyComponents(dialog.personNameButton, dialog.photoButton, dialog.photoCropButton, dialog.noteButton,
			dialog.mediaButton, dialog.assertionButton, dialog.eventButton, dialog.groupButton);
		dialog.initialize();
		return dialog;
	}

	public static PersonDialog createRecordOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final PersonDialog dialog = new PersonDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private PersonDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public PersonDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
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
		final Comparator<String> numericComparator = GUIHelper.getNumericComparator();
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
			EditEvent.create(EditEvent.EditType.PERSON_NAME, TABLE_NAME, selectedRecord)));

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO, TABLE_NAME, selectedRecord)));

		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO_CROP, TABLE_NAME, selectedRecord)));
		photoCropButton.setEnabled(false);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, selectedRecord)));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, selectedRecord)));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, TABLE_NAME, selectedRecord)));

		groupButton.setToolTipText("Groups");
		groupButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.GROUP, TABLE_NAME, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personNameButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(photoCropButton, "sizegroup btn,gapleft 30,center");

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

		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

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
		final boolean hasPersonNames = (getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasNotes = (getRecords(TABLE_NAME_NOTE)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasMedia = (getRecords(TABLE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasAssertions = (getRecords(TABLE_NAME_ASSERTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasEvents = (getRecords(TABLE_NAME_EVENT)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasGroups = (getRecords(TABLE_NAME_GROUP_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(TABLE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(personID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);

		setButtonEnableAndBorder(personNameButton, hasPersonNames);
		setButtonEnableAndBorder(photoButton, photoID != null);
		photoCropButton.setEnabled(photoID != null && (!selectRecordOnly || photoCrop != null));
		GUIHelper.addBorder(photoCropButton, (photoID != null && photoCrop != null && !photoCrop.isEmpty()), DATA_BUTTON_BORDER_COLOR);

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
		GUIHelper.setDefaultBorder(photoCropButton);

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
		final NavigableMap<Integer, Map<String, Object>> localizedPersonNames = getRecords(TABLE_NAME_LOCALIZED_PERSON_NAME);
		getRecords(TABLE_NAME_PERSON_NAME)
			.values().stream()
			.filter(record -> Objects.equals(personID, extractRecordPersonID(record)))
			.forEach(record -> {
				//extract transliterations
				final StringJoiner subIdentifier = new StringJoiner(", ");
				final Integer personNameID = extractRecordID(record);
				getFilteredRecords(TABLE_NAME_LOCALIZED_TEXT_JUNCTION, TABLE_NAME_PERSON_NAME, personNameID)
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

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		person1.put("photo_id", 3);
		person1.put("photo_crop", "0 0 5 10");
		persons.put((Integer)person1.get("id"), person1);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "tòni");
		personName1.put("family_name", "bruxatin");
		personName1.put("locale", "vec-IT");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);
		final Map<String, Object> personName2 = new HashMap<>();
		personName2.put("id", 2);
		personName2.put("person_id", 1);
		personName2.put("personal_name", "antonio");
		personName2.put("family_name", "bruciatino");
		personName2.put("locale", "it-IT");
		personName2.put("type", "death name");
		personNames.put((Integer)personName2.get("id"), personName2);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "true name");
		localizedText1.put("locale", "en");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "fake name");
		localizedText2.put("locale", "en");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);

		final TreeMap<Integer, Map<String, Object>> localizedPersonNames = new TreeMap<>();
		store.put("localized_person_name", localizedPersonNames);
		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "true");
		localizedPersonName1.put("family_name", "name");
		localizedPersonName1.put("locale", "en");
		localizedPersonNames.put((Integer)localizedPersonName1.get("id"), localizedPersonName1);
		final Map<String, Object> localizedPersonName2 = new HashMap<>();
		localizedPersonName2.put("id", 2);
		localizedPersonName2.put("personal_name", "fake");
		localizedPersonName2.put("family_name", "name");
		localizedPersonName2.put("locale", "en");
		localizedPersonNames.put((Integer)localizedPersonName2.get("id"), localizedPersonName2);
		final Map<String, Object> localizedPersonName3 = new HashMap<>();
		localizedPersonName3.put("id", 3);
		localizedPersonName3.put("personal_name", "other");
		localizedPersonName3.put("family_name", "name");
		localizedPersonName3.put("locale", "en");
		localizedPersonNames.put((Integer)localizedPersonName3.get("id"), localizedPersonName3);

		final TreeMap<Integer, Map<String, Object>> media = new TreeMap<>();
		store.put("media", media);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		media.put((Integer)media1.get("id"), media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		media2.put("date_id", 1);
		media.put((Integer)media2.get("id"), media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		media3.put("date_id", 1);
		media.put((Integer)media3.get("id"), media3);

		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();
		store.put("media_junction", mediaJunctions);
		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("id", 1);
		mediaJunction1.put("media_id", 1);
		mediaJunction1.put("reference_table", "person");
		mediaJunction1.put("reference_id", 1);
		mediaJunctions.put((Integer)mediaJunction1.get("id"), mediaJunction1);
		final Map<String, Object> mediaJunction2 = new HashMap<>();
		mediaJunction2.put("id", 2);
		mediaJunction2.put("media_id", 2);
		mediaJunction2.put("reference_table", "person");
		mediaJunction2.put("reference_id", 1);
		mediaJunction2.put("photo_crop", "0 0 10 50");
		mediaJunctions.put((Integer)mediaJunction2.get("id"), mediaJunction2);
		final Map<String, Object> mediaJunction3 = new HashMap<>();
		mediaJunction3.put("id", 3);
		mediaJunction3.put("media_id", 3);
		mediaJunction3.put("reference_table", "person");
		mediaJunction3.put("reference_id", 1);
		mediaJunctions.put((Integer)mediaJunction3.get("id"), mediaJunction3);

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
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
//			final PersonDialog dialog = create(store, parent);
//			final PersonDialog dialog = createRecordOnly(store, parent);
			final PersonDialog dialog = createSelectOnly(store, parent);
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
									? AssertionDialog.createSelectOnly(store, parent)
									: AssertionDialog.create(store, parent))
								.withReference(TABLE_NAME, personID);
							assertionDialog.loadData();

							assertionDialog.showDialog();
						}
						case PERSON_NAME -> {
							final PersonNameDialog personNameDialog = (dialog.isViewOnlyComponent(dialog.personNameButton)
									? PersonNameDialog.createSelectOnly(store, parent)
									: PersonNameDialog.create(store, parent))
								.withReference(personID)
								.withOnCloseGracefully(record -> {
									insertRecordPersonID(record, personID);

									//update table identifier
									dialog.loadData();
								});
							personNameDialog.loadData();

							personNameDialog.showDialog();
						}
						case PHOTO -> {
							final MediaDialog photoDialog = (dialog.isViewOnlyComponent(dialog.photoButton)
									? MediaDialog.createRecordOnlyForPhoto(store, parent)
									: MediaDialog.createForPhoto(store, parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, personID)
								.withOnCloseGracefully(record -> {
									final Integer newPhotoID = extractRecordID(record);
									insertRecordPhotoID(container, newPhotoID);

									dialog.photoCropButton.setEnabled(newPhotoID != null);
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
						case PHOTO_CROP -> {
							final PhotoCropDialog photoCropDialog = (dialog.isViewOnlyComponent(dialog.photoCropButton)
								? PhotoCropDialog.createSelectOnly(store, parent)
								: PhotoCropDialog.create(store, parent));
							photoCropDialog.withOnCloseGracefully(record -> {
									final Rectangle crop = photoCropDialog.getCrop();
									if(crop != null){
										final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
										sj.add(Integer.toString(crop.x))
											.add(Integer.toString(crop.y))
											.add(Integer.toString(crop.width))
											.add(Integer.toString(crop.height));
										insertRecordPhotoCrop(container, sj.toString());
									}
								});
							try{
								if(photoID != null){
									final String photoCrop = extractRecordPhotoCrop(container);
									photoCropDialog.loadData(photoID, photoCrop);
								}

								photoCropDialog.setSize(420, 295);
								photoCropDialog.showDialog();
							}
							catch(final IOException ignored){}
						}
						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
									? NoteDialog.createSelectOnly(store, parent)
									: NoteDialog.create(store, parent))
								.withReference(TABLE_NAME, personID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, personID);
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
								.withReference(TABLE_NAME, personID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, personID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT -> {
							final EventDialog eventDialog = (dialog.isViewOnlyComponent(dialog.eventButton)
									? EventDialog.createSelectOnly(store, parent)
									: EventDialog.create(store, parent))
								.withReference(TABLE_NAME, personID);
							eventDialog.loadData();

							eventDialog.showDialog();
						}
						case GROUP -> {
							final GroupDialog groupDialog = (dialog.isViewOnlyComponent(dialog.groupButton)
									? GroupDialog.createSelectOnly(store, parent)
									: GroupDialog.create(store, parent))
								.withReference(TABLE_NAME, personID);
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
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.showDialog();
		});
	}

}
