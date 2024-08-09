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
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class RepositoryDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6136508398081805353L;

	private static final int TABLE_INDEX_IDENTIFIER = 2;

	private static final String TABLE_NAME = "repository";
	private static final String TABLE_NAME_SOURCE = "source";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JButton personButton;
	private JButton placeButton;

	private JButton notesButton;
	private JButton mediasButton;
	private JCheckBox restrictionCheckBox;

	private JButton sourcesButton;


	public static RepositoryDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new RepositoryDialog(store, parent);
	}


	private RepositoryDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public RepositoryDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
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
		return new Comparator<?>[]{Comparator.comparingInt(key -> Integer.parseInt(key.toString())), null, Comparator.naturalOrder()};
	}

	@Override
	protected void initStoreComponents(){
		setTitle(StringUtils.capitalize(StringHelper.pluralize(getTableName())));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "public library", "college library", "national library",
			"prison library", "national archives", "website", "personal collection", "cemetery/mausoleum", "museum", "state library",
			"religious library", "genealogy society collection", "government agency", "funeral home"});
		personButton = new JButton("Reference person", ICON_PERSON);
		placeButton = new JButton("Place", ICON_PLACE);

		notesButton = new JButton("Notes", ICON_NOTE);
		mediasButton = new JButton("Medias", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");

		sourcesButton = new JButton("Sources", ICON_SOURCE);


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, this::saveData);
		addMandatoryField(identifierField);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		personButton.setToolTipText("Reference person");
		personButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PERSON, TABLE_NAME, getSelectedRecord())));

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, TABLE_NAME, getSelectedRecord())));


		notesButton.setToolTipText("Notes");
		notesButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		mediasButton.setToolTipText("Media");
		mediasButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		sourcesButton.setToolTipText("Sources");
		sourcesButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.SOURCE, TABLE_NAME, getSelectedRecord())));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(identifierField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "wrap paragraph");
		recordPanelBase.add(personButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(placeButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(notesButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediasButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelChildren = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelChildren.add(sourcesButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("children", recordPanelChildren);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

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

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filter.toString(), row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier, row, TABLE_INDEX_IDENTIFIER);

			row ++;
		}
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
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		final Map<Integer, Map<String, Object>> recordSources = getRecords(TABLE_NAME_SOURCE)
			.entrySet().stream()
			.filter(entry -> Objects.equals(repositoryID, extractRecordRepositoryID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

		identifierField.setText(identifier);
		typeComboBox.setSelectedItem(type);
		GUIHelper.addBorder(personButton, personID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(notesButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediasButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		GUIHelper.addBorder(sourcesButton, !recordSources.isEmpty(), DATA_BUTTON_BORDER_COLOR);
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(personButton);
		GUIHelper.setDefaultBorder(placeButton);

		GUIHelper.setDefaultBorder(notesButton);
		GUIHelper.setDefaultBorder(mediasButton);
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

		selectedRecord.put("identifier", identifier);
		selectedRecord.put("type", type);

		return true;
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordRepositoryID(final Map<String, Object> record){
		return (Integer)record.get("repository_id");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> repositories = new TreeMap<>();
		store.put("repository", repositories);
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repository1.put("person_id", 1);
		repository1.put("place_id", 2);
		repositories.put((Integer)repository1.get("id"), repository1);
		final Map<String, Object> repository2 = new HashMap<>();
		repository2.put("id", 2);
		repository2.put("identifier", "repo 2");
		repository2.put("type", "college library");
		repositories.put((Integer)repository2.get("id"), repository2);
		final Map<String, Object> repository3 = new HashMap<>();
		repository3.put("id", 3);
		repository3.put("identifier", "repo 3");
		repository3.put("type", "private library");
		repositories.put((Integer)repository3.get("id"), repository3);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
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
		places.put((Integer)place1.get("id"), place1);
		final Map<String, Object> place2 = new HashMap<>();
		place2.put("id", 2);
		place2.put("identifier", "another place ident");
		place2.put("name", "name of another place");
		place2.put("locale", "en-US");
		place2.put("type", "custom");
		places.put((Integer)place2.get("id"), place2);

		final TreeMap<Integer, Map<String, Object>> persons = new TreeMap<>();
		store.put("person", persons);
		final Map<String, Object> person1 = new HashMap<>();
		person1.put("id", 1);
		person1.put("photo_crop", "0 0 5 10");
		persons.put((Integer)person1.get("id"), person1);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place name 1");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);
		final Map<String, Object> localizedText2 = new HashMap<>();
		localizedText2.put("id", 2);
		localizedText2.put("text", "place name 2");
		localizedText2.put("locale", "en");
		localizedText2.put("type", "original");
		localizedText2.put("transcription", "IPA");
		localizedText2.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText2.get("id"), localizedText2);
		final Map<String, Object> localizedText3 = new HashMap<>();
		localizedText3.put("id", 3);
		localizedText3.put("text", "true name");
		localizedText3.put("locale", "en");
		localizedTexts.put((Integer)localizedText3.get("id"), localizedText3);

		final TreeMap<Integer, Map<String, Object>> personNames = new TreeMap<>();
		store.put("person_name", personNames);
		final Map<String, Object> personName1 = new HashMap<>();
		personName1.put("id", 1);
		personName1.put("person_id", 1);
		personName1.put("personal_name", "personal name");
		personName1.put("family_name", "family name");
		personName1.put("type", "birth name");
		personNames.put((Integer)personName1.get("id"), personName1);

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
		note2.put("note", "note 1");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put("media", medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		medias.put((Integer)media1.get("id"), media1);
		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();
		store.put("media_junction", mediaJunctions);
		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("id", 1);
		mediaJunction1.put("media_id", 1);
		mediaJunction1.put("photo_crop", "0 0 10 20");
		mediaJunction1.put("reference_table", TABLE_NAME);
		mediaJunction1.put("reference_id", 1);
		mediaJunctions.put((Integer)mediaJunction1.get("id"), mediaJunction1);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1 APA-style");
		source1.put("place_id", 2);
		source1.put("date_id", 1);
		source1.put("repository_id", 1);
		source1.put("location", "location 1");
		sources.put((Integer)source1.get("id"), source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2 APA-style");
		source2.put("place_id", 2);
		source2.put("date_id", 2);
		source2.put("repository_id", 2);
		source2.put("location", "location 2");
		sources.put((Integer)source2.get("id"), source2);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final RepositoryDialog dialog = create(store, parent);
			dialog.initComponents();
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
					final Integer recordID = extractRecordID(container);
					switch(editCommand.getType()){
						case PERSON -> {
							final PersonDialog personDialog = PersonDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("person_id", extractRecordID(record)));
							personDialog.initComponents();
							personDialog.loadData();
							final Integer personID = extractRecordPersonID(container);
							if(personID != null)
								personDialog.selectData(personID);

							personDialog.setLocationRelativeTo(null);
							personDialog.setVisible(true);
						}
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent)
								.withOnCloseGracefully(record -> container.put("place_id", extractRecordID(record)));
							placeDialog.initComponents();
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.setLocationRelativeTo(null);
							placeDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(tableName, recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", tableName);
										record.put("reference_id", recordID);
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
								.withReference(tableName, recordID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", recordID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceDialog sourceDialog = SourceDialog.create(store, parent)
								.withFilterOnRepositoryID(recordID)
								.withOnCloseGracefully(record -> {
									if(record != null)
										record.put("repository_id", recordID);
								});
							sourceDialog.initComponents();
							sourceDialog.loadData();

							sourceDialog.setLocationRelativeTo(null);
							sourceDialog.setVisible(true);
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
			dialog.setVisible(true);
		});
	}

}
