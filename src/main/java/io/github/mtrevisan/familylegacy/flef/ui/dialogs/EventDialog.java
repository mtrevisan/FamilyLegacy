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
import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
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
import io.github.mtrevisan.familylegacy.flef.ui.panels.HistoryPanel;
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
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDateID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordPlaceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordSuperTypeID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordTypeID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordTypeID;


public final class EventDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 1136825738944999745L;

	private static final int TABLE_INDEX_TYPE = 2;

	private static final String TABLE_NAME = "event";
	private static final String TABLE_NAME_EVENT_TYPE = "event_type";
	private static final String TABLE_NAME_EVENT_SUPER_TYPE = "event_super_type";

	//NOTE: em dash `—`
	private static final String MENU_SEPARATOR = "\u2014";
	private static final String MENU_SEPARATOR_START = MENU_SEPARATOR + StringUtils.SPACE;
	private static final String MENU_SEPARATOR_END = StringUtils.SPACE + MENU_SEPARATOR;


	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JButton addTypeButton;
	private JButton removeTypeButton;
	private JLabel descriptionLabel;
	private JTextField descriptionField;
	private JButton placeButton;
	private JButton dateButton;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private HistoryPanel historyPanel;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static EventDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final EventDialog dialog = new EventDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static EventDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final EventDialog dialog = new EventDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static EventDialog createRecordOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final EventDialog dialog = new EventDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
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

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

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
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null,
			MENU_SEPARATOR_START + "Historical events" + MENU_SEPARATOR_END,
			"historic fact", "natural disaster", "invention", "patent filing", "patent granted",
			MENU_SEPARATOR_START + "Personal origins" + MENU_SEPARATOR_END,
			"birth", "sex", "fosterage", "adoption", "guardianship",
			MENU_SEPARATOR_START + "Physical description" + MENU_SEPARATOR_END,
			"physical description", "eye color", "hair color", "height", "weight", "build", "complexion", "gender", "race", "ethnic origin",
			"marks/scars", "special talent", "disability",
			MENU_SEPARATOR_START + "Citizenship and migration" + MENU_SEPARATOR_END,
			"nationality", "emigration", "immigration", "naturalization", "caste",
			MENU_SEPARATOR_START + "Real estate assets" + MENU_SEPARATOR_END,
			"residence", "land grant", "land purchase", "land sale", "property", "deed", "escrow",
			MENU_SEPARATOR_START + "Education" + MENU_SEPARATOR_END,
			"education", "graduation", "able to read", "able to write", "learning", "enrollment",
			MENU_SEPARATOR_START + "Work and Career" + MENU_SEPARATOR_END,
			"employment", "occupation", "career", "retirement", "resignation",
			MENU_SEPARATOR_START + "Legal Events and Documents" + MENU_SEPARATOR_END,
			"coroner report", "will", "probate", "legal problem", "name change", "inquest", "jury duty", "draft registration", "pardon",
			MENU_SEPARATOR_START + "Health problems and habits" + MENU_SEPARATOR_END,
			"hospitalization", "illness", "tobacco use", "alcohol use", "drug problem",
			MENU_SEPARATOR_START + "Marriage and family life" + MENU_SEPARATOR_END,
			"engagement", "betrothal", "cohabitation", "union", "wedding", "marriage", "number of marriages", "marriage bann",
			"marriage license", "marriage contract", "marriage settlement", "filing for divorce", "divorce", "annulment", "separation",
			"number of children (total)", "number of children (living)", "marital status", "wedding anniversary", "anniversary celebration",
			MENU_SEPARATOR_START + "Military" + MENU_SEPARATOR_END,
			"military induction", "military enlistment", "military rank", "military award", "military promotion", "military service",
			"military release", "military discharge", "military resignation", "military retirement", "missing in action",
			MENU_SEPARATOR_START + "Confinement" + MENU_SEPARATOR_END,
			"imprisonment", "deportation", "internment",
			MENU_SEPARATOR_START + "Transfers and travel" + MENU_SEPARATOR_END,
			"travel",
			MENU_SEPARATOR_START + "Accolades" + MENU_SEPARATOR_END,
			"honor", "award", "membership",
			MENU_SEPARATOR_START + "Death and burial" + MENU_SEPARATOR_END,
			"death", "execution", "autopsy", "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation", "reburial",
			MENU_SEPARATOR_START + "Others" + MENU_SEPARATOR_END,
			"anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home",
			MENU_SEPARATOR_START + "Religious events" + MENU_SEPARATOR_END,
			"religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening", "confirmation",
			"ordination", "blessing", "first communion"
		});
		addTypeButton = new JButton("Add");
		removeTypeButton = new JButton("Remove");

		descriptionLabel = new JLabel("Description:");
		descriptionField = new JTextField();

		placeButton = new JButton("Place", ICON_PLACE);
		dateButton = new JButton("Date", ICON_CALENDAR);

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");

		historyPanel = HistoryPanel.create(store)
			.withLinkListener((table, id) -> EventBusService.publish(EditEvent.create(EditEvent.EditType.MODIFICATION_HISTORY, getTableName(),
				Map.of("id", extractRecordID(selectedRecord), "note_id", id))));


		typeComboBox.setRenderer(new SeparatorComboBoxRenderer(MENU_SEPARATOR_START, MENU_SEPARATOR_END));
		GUIHelper.bindLabelSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);
		addMandatoryField(typeComboBox);
		addTypeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT_TYPE, TABLE_NAME, selectedRecord)));
		removeTypeButton.addActionListener(evt -> {
			//remove selected item from `typeComboBox`
			final String type = GUIHelper.getTextTrimmed(typeComboBox);
			if(type != null && (!type.startsWith(MENU_SEPARATOR_START) || !type.endsWith(MENU_SEPARATOR_END))){
				//remove data from store
				getRecords(TABLE_NAME_EVENT_TYPE)
					.values()
					.removeIf(entry -> Objects.equals(type, extractRecordType(entry)));

				typeComboBox.removeItem(type);
				typeComboBox.setSelectedItem(null);
			}
		});

		GUIHelper.bindLabelTextChangeUndo(descriptionLabel, descriptionField, this::saveData);

		placeButton.setToolTipText("Event place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, TABLE_NAME, selectedRecord)));

		dateButton.setToolTipText("Event date");
		dateButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, TABLE_NAME, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap related");
		recordPanelBase.add(addTypeButton, "sizegroup btn,tag add,split 2,align right");
		recordPanelBase.add(removeTypeButton, "sizegroup btn,tag delete,gapleft 20,wrap paragraph");
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
		recordTabbedPane.add("history", historyPanel);
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
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(type, row, TABLE_INDEX_TYPE);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		typeComboBox.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Map<Integer, Map<String, Object>> storeEventTypes = getRecords(TABLE_NAME_EVENT_TYPE);

		final Integer eventID = extractRecordID(selectedRecord);
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

		historyPanel.withReference(TABLE_NAME, eventID);
		historyPanel.loadData();
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

		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final Integer typeID = getRecords(TABLE_NAME_EVENT_TYPE)
			.values().stream()
			.filter(entry -> Objects.equals(type, extractRecordType(entry)))
			.findFirst()
			.map(EntityManager::extractRecordID)
			.orElse(null);
		final String description = GUIHelper.getTextTrimmed(descriptionField);

		insertRecordTypeID(selectedRecord, typeID);
		insertRecordDescription(selectedRecord, description);

		return true;
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
		eventType1.put("super_type_id", 2);
		eventType1.put("type", "birth");
		eventType1.put("category", "birth");
		eventTypes.put((Integer)eventType1.get("id"), eventType1);

		final TreeMap<Integer, Map<String, Object>> eventSuperTypes = new TreeMap<>();
		store.put("event_super_type", eventSuperTypes);
		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events");
		eventSuperTypes.put((Integer)eventSuperType1.get("id"), eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Personal origins");
		eventSuperTypes.put((Integer)eventSuperType2.get("id"), eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical description");
		eventSuperTypes.put((Integer)eventSuperType3.get("id"), eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Citizenship and migration");
		eventSuperTypes.put((Integer)eventSuperType4.get("id"), eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Real estate assets");
		eventSuperTypes.put((Integer)eventSuperType5.get("id"), eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education");
		eventSuperTypes.put((Integer)eventSuperType6.get("id"), eventSuperType6);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("id", 7);
		eventSuperType7.put("super_type", "Work and Career");
		eventSuperTypes.put((Integer)eventSuperType7.get("id"), eventSuperType7);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("id", 8);
		eventSuperType8.put("super_type", "Legal Events and Documents");
		eventSuperTypes.put((Integer)eventSuperType8.get("id"), eventSuperType8);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("id", 9);
		eventSuperType9.put("super_type", "Health problems and habits");
		eventSuperTypes.put((Integer)eventSuperType9.get("id"), eventSuperType9);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("id", 10);
		eventSuperType10.put("super_type", "Marriage and family life");
		eventSuperTypes.put((Integer)eventSuperType10.get("id"), eventSuperType10);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("id", 11);
		eventSuperType11.put("super_type", "Military");
		eventSuperTypes.put((Integer)eventSuperType11.get("id"), eventSuperType11);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("id", 12);
		eventSuperType12.put("super_type", "Confinement");
		eventSuperTypes.put((Integer)eventSuperType12.get("id"), eventSuperType12);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("id", 13);
		eventSuperType13.put("super_type", "Transfers and travel");
		eventSuperTypes.put((Integer)eventSuperType13.get("id"), eventSuperType13);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("id", 14);
		eventSuperType14.put("super_type", "Accolades");
		eventSuperTypes.put((Integer)eventSuperType14.get("id"), eventSuperType14);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("id", 15);
		eventSuperType15.put("super_type", "Death and burial");
		eventSuperTypes.put((Integer)eventSuperType15.get("id"), eventSuperType15);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("id", 16);
		eventSuperType16.put("super_type", "Others");
		eventSuperTypes.put((Integer)eventSuperType16.get("id"), eventSuperType16);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("id", 17);
		eventSuperType17.put("super_type", "Religious events");
		eventSuperTypes.put((Integer)eventSuperType17.get("id"), eventSuperType17);

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
//			final EventDialog dialog = createRecordOnly(store, parent);
			injector.injectDependencies(dialog);
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
					final int eventID = extractRecordID(container);
					switch(editCommand.getType()){
						case PLACE -> {
							final PlaceDialog placeDialog = PlaceDialog.create(store, parent);
							placeDialog.loadData();
							final Integer placeID = extractRecordPlaceID(container);
							if(placeID != null)
								placeDialog.selectData(placeID);

							placeDialog.showDialog();
						}
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.showDialog();
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, eventID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, eventID);
									}
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, eventID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, eventID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case EVENT_TYPE -> {
							//if type is not present in the list, show a dialog to insert it within its appropriate super-type
							final EventTypeDialog eventSuperTypeDialog = EventTypeDialog.create(store, parent)
								.withOnCloseGracefully(record -> {
									final String newType = extractRecordType(record);
									final Integer superTypeID = extractRecordSuperTypeID(record);
									final Map<Integer, Map<String, Object>> storeEventSuperTypes = getRecords(TABLE_NAME_EVENT_SUPER_TYPE, store);
									final String superTypeMenuItemText = MENU_SEPARATOR_START
										+ extractRecordSuperType(storeEventSuperTypes.get(superTypeID))
										+ MENU_SEPARATOR_END;

									//add `newType` at the end of the `superTypeMenuItemText` section
									for(int i = 0, length = dialog.typeComboBox.getItemCount(); i < length; i ++)
										if(superTypeMenuItemText.equals(dialog.typeComboBox.getItemAt(i))){
											//skip to end of section
											while(++ i < length){
												final String text = dialog.typeComboBox.getItemAt(i);
												if(text.startsWith(MENU_SEPARATOR_START) && text.endsWith(MENU_SEPARATOR_END))
													break;
											}

											//add new menu item
											dialog.typeComboBox.insertItemAt(newType, i);
											break;
										}

									dialog.typeComboBox.setSelectedItem(newType != null? newType: StringUtils.SPACE);
								});
							eventSuperTypeDialog.showNewRecord();

							eventSuperTypeDialog.showNewRecord();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("note_id");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationRecordOnly(store, parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Change modification note for " + title + " " + eventID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
					}
				}

				private static Map<Integer, Map<String, Object>> getRecords(final String tableName,
						final Map<String, TreeMap<Integer, Map<String, Object>>> store){
					return store.computeIfAbsent(tableName, k -> new TreeMap<>());
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
