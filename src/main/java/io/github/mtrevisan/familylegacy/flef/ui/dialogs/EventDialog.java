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

import io.github.mtrevisan.familylegacy.flef.db.DatabaseManager;
import io.github.mtrevisan.familylegacy.flef.db.DatabaseManagerInterface;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.SeparatorComboBoxRenderer;
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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


//TODO manage historic events not linked to anything
public final class EventDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 1136825738944999745L;

	private static final int TABLE_INDEX_TYPE = 2;

	private static final String TABLE_NAME = "event";
	private static final String TABLE_NAME_EVENT_TYPE = "event_type";


	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel descriptionLabel;
	private JTextField descriptionField;
	private JButton placeButton;
	private JButton dateButton;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static EventDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new EventDialog(store, parent);
	}


	private EventDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public EventDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public EventDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Type"};
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
		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();
	}

	@Override
	protected void initRecordComponents(){
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null,
			"--- Historical events and relevant facts ---",
			"historic fact", "natural disaster", "invention", "patent filing", "patent granted",
			"--- Birth and early life ---",
			"birth", "sex", "fosterage", "adoption", "guardianship",
			"--- Physical condition and personal description ---",
			"physical description", "eye color", "hair color", "height", "weight", "build", "complexion", "gender", "race", "ethnic origin",
			"marks/scars", "special talent", "disability",
			"--- Nationality and immigration ---",
			"nationality", "emigration", "immigration", "naturalization", "caste",
			"--- Residence and property ---",
			"residence", "land grant", "land purchase", "land sale", "property", "deed", "escrow",
			"--- Education and learning ---",
			"education", "graduation", "able to read", "able to write", "learning", "enrollment",
			"--- Work and Career ---",
			"employment", "occupation", "career", "retirement", "resignation",
			"--- Legal Events and Documents ---",
			"coroner report", "will", "probate", "legal problem", "name change", "inquest", "jury duty", "draft registration", "pardon",
			"--- Health problems and habits ---",
			"hospitalization", "illness", "tobacco use", "alcohol use", "drug problem",
			"--- Marriage and family life ---",
			"engagement", "betrothal", "cohabitation", "union", "wedding", "marriage", "number of marriages", "marriage bann",
			"marriage license", "marriage contract", "marriage settlement", "filing for divorce", "divorce", "annulment", "separation",
			"number of children (total)", "number of children (living)", "marital status", "wedding anniversary", "anniversary celebration",
			"--- Military ---",
			"military induction", "military enlistment", "military rank", "military award", "military promotion", "military service",
			"military release", "military discharge", "military resignation", "military retirement", "missing in action",
			"--- Imprisonment and restrictions ---",
			"imprisonment", "deportation", "internment",
			"--- Transfers and travel ---",
			"travel",
			"--- Honors and Recognitions ---",
			"honor", "award", "membership",
			"--- Death and burial ---",
			"death", "execution", "autopsy",  "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation", "reburial",
			"--- Others ---",
			"anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home",
			"--- Religious events ---",
			"religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening", "confirmation",
			"ordination", "blessing", "first communion"
		});

		descriptionLabel = new JLabel("Description:");
		descriptionField = new JTextField();

		placeButton = new JButton("Place", ICON_PLACE);
		dateButton = new JButton("Date", ICON_CALENDAR);

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Medias", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");


		typeComboBox.setRenderer(new SeparatorComboBoxRenderer());
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);
		addMandatoryField(typeComboBox);

		GUIHelper.bindLabelTextChangeUndo(descriptionLabel, descriptionField, this::saveData);

		placeButton.setToolTipText("Event place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, TABLE_NAME, getSelectedRecord())));

		dateButton.setToolTipText("Event date");
		dateButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, TABLE_NAME, getSelectedRecord())));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(descriptionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(descriptionField, "grow,wrap paragraph");
		recordPanelBase.add(placeButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(dateButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = (filterReferenceTable == null
			? getRecords(TABLE_NAME)
			: getFilteredRecords(TABLE_NAME, filterReferenceTable, filterReferenceID));
		final Map<Integer, Map<String, Object>> storeEventTypes = getRecords(TABLE_NAME_EVENT_TYPE);

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final Integer typeID = extractRecordTypeID(container);
			final String type = extractRecordType(storeEventTypes.get(typeID));
			final FilterString filter = FilterString.create()
				.add(key)
				.add(type);

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filter.toString(), row, TABLE_INDEX_FILTER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);

			row ++;
		}
	}

	@Override
	protected void fillData(){
		final Map<Integer, Map<String, Object>> storeEventTypes = getRecords(TABLE_NAME_EVENT_TYPE);

		final Integer typeID = extractRecordTypeID(selectedRecord);
		final String type = extractRecordType(storeEventTypes.get(typeID));
		final String description = extractRecordDescription(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Integer dateID = extractRecordDateID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		final ItemEvent itemEvent = new ItemEvent(typeComboBox, ItemEvent.ITEM_STATE_CHANGED, typeComboBox.getItemAt(0),
			ItemEvent.SELECTED);
		final ItemListener[] itemListeners = typeComboBox.getItemListeners();
		for(int i = 0, length = itemListeners.length; i < length; i ++)
			itemListeners[i].itemStateChanged(itemEvent);
		typeComboBox.setSelectedItem(type);

		descriptionField.setText(description);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(dateButton, dateID != null, DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		typeComboBox.setSelectedItem(null);
		descriptionField.setText(null);
		GUIHelper.setDefaultBorder(placeButton);
		GUIHelper.setDefaultBorder(dateButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(filterReferenceTable == null && !validData(GUIHelper.getTextTrimmed(typeComboBox))){
			JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			typeComboBox.requestFocusInWindow();

			return false;
		}

//		if(selectedRecord != null){
//			final String referenceTable = extractRecordReferenceTable(selectedRecord);
//			final Integer referenceID = extractRecordReferenceID(selectedRecord);
//			if(!validData(referenceTable) || !validData(referenceID)){
//				JOptionPane.showMessageDialog(getParent(), "Reference is required", "Error",
//					JOptionPane.ERROR_MESSAGE);
//				referenceButton.requestFocusInWindow();
//
//				return false;
//			}
//		}

		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String description = GUIHelper.getTextTrimmed(descriptionField);

		//update table:
		//TODO test
		final NavigableMap<Integer, Map<String, Object>> storeEventTypes = getRecords(TABLE_NAME_EVENT_TYPE);
		final AtomicReference<Integer> recordTypeID = new AtomicReference<>(extractRecordTypeID(selectedRecord));
		final String recordType = extractRecordType(storeEventTypes.get(recordTypeID.get()));
		if(!Objects.equals(type, recordType)){
			final boolean typePresent = storeEventTypes.values().stream()
				.anyMatch(entry -> Objects.equals(type, extractRecordType(entry)));
			if(!typePresent){
				//if type is not present in the list, show a dialog to insert it within its appropriate super-type
				final EventTypeDialog eventSuperTypeDialog = EventTypeDialog.create(store, (Frame)getParent())
					.withOnCloseGracefully(record -> {
						final Integer superTypeID = extractRecordID(record);

						//add event type with the specified super-type
						final Map<String, Object> eventSuperType = new HashMap<>();
						recordTypeID.set(extractNextRecordID(storeEventTypes));
						eventSuperType.put("id", recordTypeID.get());
						eventSuperType.put("super_type_id", superTypeID);
						storeEventTypes.put(extractRecordID(eventSuperType), eventSuperType);
					});
				eventSuperTypeDialog.setTitle("New Event Type for `" + type + "`");
				eventSuperTypeDialog.initComponents();
				eventSuperTypeDialog.loadData();

				eventSuperTypeDialog.setLocationRelativeTo(null);
				eventSuperTypeDialog.setVisible(true);
			}

			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					model.setValueAt(type, modelRowIndex, TABLE_INDEX_TYPE);

					break;
				}
			}
		}

		selectedRecord.put("type_id", recordTypeID.get());
		selectedRecord.put("description", description);

		return true;
	}


	private static Integer extractRecordTypeID(final Map<String, Object> record){
		return (Integer)record.get("type_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
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

		final TreeMap<Integer, Map<String, Object>> events = new TreeMap<>();
		store.put("event", events);
		final Map<String, Object> event = new HashMap<>();
		event.put("id", 1);
		event.put("type_id", 1);
		event.put("description", "a birth");
		event.put("place_id", 1);
		event.put("date_id", 1);
		event.put("reference_table", "person");
		event.put("reference_id", 1);
		events.put((Integer)event.get("id"), event);

		final TreeMap<Integer, Map<String, Object>> eventTypes = new TreeMap<>();
		store.put("event_type", eventTypes);
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("super_type_id", 1);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		eventTypes.put((Integer)eventType1.get("id"), eventType1);

		final TreeMap<Integer, Map<String, Object>> eventSuperTypes = new TreeMap<>();
		store.put("event_super_type", eventSuperTypes);
		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Birth and early life");
		eventSuperTypes.put((Integer)eventSuperType1.get("id"), eventSuperType1);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

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

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		final TreeMap<Integer, Map<String, Object>> media = new TreeMap<>();
		store.put("media", media);
		final Map<String, Object> m1 = new HashMap<>();
		m1.put("id", 1);
		m1.put("identifier", "custom media");
		media.put((Integer)m1.get("id"), m1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final DependencyInjector injector = new DependencyInjector();
			final DatabaseManager dbManager = new DatabaseManager("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
			try{
				final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
				dbManager.initialize(grammarFile);

				dbManager.insertDatabase(store);
			}
			catch(final SQLException | IOException e){
				throw new RuntimeException(e);
			}
			injector.register(DatabaseManagerInterface.class, dbManager);

			final EventDialog dialog = create(store, parent);
			injector.injectDependencies(dialog);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(event)))
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
					switch(editCommand.getType()){
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent);
							placeDialog.initComponents();
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.setLocationRelativeTo(null);
							placeDialog.setVisible(true);
						}
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.initComponents();
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.setLocationRelativeTo(null);
							historicDateDialog.setVisible(true);
						}
						case NOTE -> {
							final int eventID = extractRecordID(container);
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, eventID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", eventID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case MEDIA -> {
							final int eventID = extractRecordID(container);
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, eventID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", eventID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
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
