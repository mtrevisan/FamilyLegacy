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

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordDescription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class EventDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 1136825738944999745L;

	private static final int TABLE_INDEX_TYPE = 2;

	//NOTE: em dash `—`
	private static final String MENU_SEPARATOR = "\u2014";
	private static final String MENU_SEPARATOR_START = MENU_SEPARATOR + StringUtils.SPACE;
	private static final String MENU_SEPARATOR_END = StringUtils.SPACE + MENU_SEPARATOR;


	private static final class TypeItem{
		private final String label;
		private final int id;

		private TypeItem(final String label, final int id){
			this.label = label;
			this.id = id;
		}

		private TypeItem(final String label){
			this.label = MENU_SEPARATOR_START + label + MENU_SEPARATOR_END;
			this.id = -1;
		}

		public int getID(){
			return id;
		}

		@Override
		public String toString(){
			return label;
		}
	}

	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<TypeItem> typeComboBox = new JComboBox<>(extractDefaultItems());
	private final JButton addTypeButton = new JButton("Add");
	private final JButton removeTypeButton = new JButton("Remove");
	private final JLabel descriptionLabel = new JLabel("Description:");
	private final JTextField descriptionField = new JTextField();
	private final JButton placeButton = new JButton("Place", ICON_PLACE);
	private final JButton dateButton = new JButton("Date", ICON_CALENDAR);

	private final JButton noteButton = new JButton("Notes", ICON_NOTE);
	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private String filterReferenceTable;
	private int filterReferenceID;


	public static EventDialog create(final Frame parent){
		final EventDialog dialog = new EventDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static EventDialog createSelectOnly(final Frame parent){
		final EventDialog dialog = new EventDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.placeButton, dialog.dateButton, dialog.noteButton, dialog.mediaButton);
		dialog.initialize();
		return dialog;
	}

	public static EventDialog createShowOnly(final Frame parent){
		final EventDialog dialog = new EventDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static EventDialog createEditOnly(final Frame parent){
		final EventDialog dialog = new EventDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private EventDialog(final Frame parent){
		super(parent);
	}


	public EventDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
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
		return EntityManager.NODE_EVENT;
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
		typeComboBox.setRenderer(new SeparatorComboBoxRenderer());
		GUIHelper.bindLabelAutoComplete(typeLabel, typeComboBox);
		GUIHelper.bindOnSelectionChange(typeComboBox, this::saveData);
		addMandatoryField(typeComboBox);
		addTypeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT_TYPE, EntityManager.NODE_EVENT, selectedRecord)));
		removeTypeButton.addActionListener(evt -> {
			//remove selected item from `typeComboBox`
			final TypeItem selectedItem = (TypeItem)typeComboBox.getSelectedItem();
			final Integer typeID = (selectedItem != null? selectedItem.getID(): null);
			if(typeID != null && typeID >= 0){
				//remove data from store
				final List<Integer> ids = Repository.findAll(EntityManager.NODE_EVENT_TYPE)
					.stream()
					.filter(entry -> Objects.equals(typeID, EntityManager.extractRecordID(entry)))
					.map(EntityManager::extractRecordID)
					.toList();
				Repository.deleteNodes(EntityManager.NODE_EVENT_TYPE, ids);

				typeComboBox.removeItem(selectedItem);
				typeComboBox.setSelectedItem(null);
			}
		});

		GUIHelper.bindLabelUndo(descriptionLabel, descriptionField);
		GUIHelper.bindOnTextChange(descriptionField, this::saveData);

		placeButton.setToolTipText("Event place");
		placeButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PLACE, EntityManager.NODE_EVENT, selectedRecord)));

		dateButton.setToolTipText("Event date");
		dateButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, EntityManager.NODE_EVENT, selectedRecord)));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, EntityManager.NODE_EVENT, selectedRecord)));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, EntityManager.NODE_EVENT, selectedRecord)));

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
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceTable == null
			? Repository.findAll(EntityManager.NODE_EVENT)
			: Repository.findReferencingNodes(EntityManager.NODE_EVENT,
				filterReferenceTable, filterReferenceID,
				EntityManager.RELATIONSHIP_FOR));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String type = extractRecordType(recordID);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(type);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
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
		final Integer eventID = extractRecordID(selectedRecord);
		final String type = extractRecordType(eventID);
		final String description = extractRecordDescription(selectedRecord);
		final boolean hasPlaces = Repository.hasPlace(EntityManager.NODE_EVENT, eventID);
		final boolean hasDate = Repository.hasDate(EntityManager.NODE_EVENT, eventID);
		final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_EVENT, eventID);
		final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_EVENT, eventID);
		final String restriction = Repository.getRestriction(EntityManager.NODE_EVENT, eventID);

		final ItemEvent itemEvent = new ItemEvent(typeComboBox, ItemEvent.ITEM_STATE_CHANGED, typeComboBox.getItemAt(0),
			ItemEvent.SELECTED);
		final ItemListener[] itemListeners = typeComboBox.getItemListeners();
		for(int i = 0, length = itemListeners.length; i < length; i ++)
			itemListeners[i].itemStateChanged(itemEvent);
		typeComboBox.setSelectedItem(type);

		descriptionField.setText(description);
		setButtonEnableAndBorder(placeButton, hasPlaces);
		setButtonEnableAndBorder(dateButton, hasDate);

		setButtonEnableAndBorder(noteButton, hasNotes);
		setButtonEnableAndBorder(mediaButton, hasMedia);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
	}

	private String extractRecordType(final Integer eventID){
		final Map.Entry<String, Map<String, Object>> eventTypeNode = Repository.findReferencedNode(
			EntityManager.NODE_EVENT, eventID,
			EntityManager.RELATIONSHIP_OF_TYPE);
		if(eventTypeNode == null || !EntityManager.NODE_EVENT_TYPE.equals(eventTypeNode.getKey()))
			return null;

		return EntityManager.extractRecordType(eventTypeNode.getValue());
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
		if(filterReferenceTable == null && selectedRecord != null){
			final TypeItem selectedItem = (TypeItem)typeComboBox.getSelectedItem();
			if(selectedItem == null || selectedItem.getID() < 0){
				JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				typeComboBox.requestFocusInWindow();

				return false;
			}
		}

		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final TypeItem selectedItem = (TypeItem)typeComboBox.getSelectedItem();
		final Integer typeID = (selectedItem != null? selectedItem.getID(): null);
		final String description = GUIHelper.getTextTrimmed(descriptionField);

		Repository.upsertRelationship(EntityManager.NODE_EVENT, extractRecordID(selectedRecord),
			EntityManager.NODE_EVENT_TYPE, typeID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		insertRecordDescription(selectedRecord, description);

		return true;
	}


	private static void addDefaultItemsToDatabase(){
		addSuperTypeAndTypes("Historical events",
			"historic fact", "natural disaster", "invention", "patent filling", "patent granted");
		addSuperTypeAndTypes("Personal origins",
			"birth", "sex", "fosterage", "adoption", "guardianship");
		addSuperTypeAndTypes("Physical description",
			"physical description", "eye color", "hair color", "height", "weight", "build", "complexion", "gender", "race",
			"ethnic origin");
		addSuperTypeAndTypes("Citizenship & Migration",
			"nationality", "emigration", "immigration", "naturalization", "caste");
		addSuperTypeAndTypes("Real estate assets",
			"residence", "land grant", "land purchase", "land sale", "property", "deed", "escrow");
		addSuperTypeAndTypes("Education",
			"education", "graduation", "able to read", "able to write", "learning", "enrollment");
		addSuperTypeAndTypes("Work & Career",
			"employment", "occupation", "career", "retirement", "resignation");
		addSuperTypeAndTypes("Legal events & Documents",
			"coroner report", "will", "probate", "legal problem", "name change", "inquest", "jury duty", "draft registration",
			"pardon");
		addSuperTypeAndTypes("Health problems & Habits",
			"hospitalization", "illness", "tobacco use", "alcohol use", "drug problem");
		addSuperTypeAndTypes("Marriage & Family life",
			"engagement", "betrothal", "cohabitation", "union", "wedding", "marriage", "number of marriages", "marriage bann",
			"marriage license", "marriage contract", "marriage settlement", "filing for divorce", "divorce", "annulment", "separation",
			"number of children (total)", "number of children (living)", "marital status", "wedding anniversary", "anniversary celebration");
		addSuperTypeAndTypes("Military",
			"military induction", "military enlistment", "military rank", "military award", "military promotion", "military service",
			"military release", "military discharge", "military resignation", "military retirement", "missing in action");
		addSuperTypeAndTypes("Confinement",
			"imprisonment", "deportation", "internment");
		addSuperTypeAndTypes("Transfer & Travel",
			"travel");
		addSuperTypeAndTypes("Accolades",
			"honor", "award", "membership");
		addSuperTypeAndTypes("Death & burial",
			"death", "execution", "autopsy", "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation",
			"reburial");
		addSuperTypeAndTypes("Others",
			"anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home");
		addSuperTypeAndTypes("Religious events",
			"religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening",
			"confirmation", "ordination", "blessing", "first communion");
	}

	private static TypeItem[] extractDefaultItems(){
		addDefaultItemsToDatabase();

		final List<Map<String, Object>> superTypeRecords = Repository.findAll(EntityManager.NODE_EVENT_SUPER_TYPE);
		final Comparator<Map<String, Object>> comparator = Comparator.comparing(EntityManager::extractRecordID);
		superTypeRecords.sort(comparator);
		final int length = superTypeRecords.size();
		final List<TypeItem> items = new ArrayList<>(length + 1);
		items.add(null);
		for(int i = 0; i < length; i ++){
			final Map<String, Object> superTypeRecord = superTypeRecords.get(i);

			final List<Map<String, Object>> typeRecords = Repository.findReferencingNodes(EntityManager.NODE_EVENT_TYPE,
				EntityManager.NODE_EVENT_SUPER_TYPE, extractRecordID(superTypeRecord),
				EntityManager.RELATIONSHIP_OF);
			typeRecords.sort(comparator);

			items.add(new TypeItem(extractRecordSuperType(superTypeRecord)));
			for(int j = 0, count = typeRecords.size(); j < count; j ++){
				final Map<String, Object> typeRecord = typeRecords.get(j);

				items.add(new TypeItem(EntityManager.extractRecordType(typeRecord), extractRecordID(typeRecord)));
			}
		}
		return items.toArray(TypeItem[]::new);
	}

	private static void addSuperTypeAndTypes(final String superType, final String... types){
		final Map<String, Object> eventSuperType = new HashMap<>();
		insertRecordSuperType(eventSuperType, superType);
		final int eventSuperTypeID = Repository.upsert(eventSuperType, EntityManager.NODE_EVENT_SUPER_TYPE);
		for(int i = 0, length = types.length; i < length; i ++)
			addType(types[i], eventSuperTypeID);
	}

	private static void addType(final String type, final int eventSuperTypeID){
		final Map<String, Object> eventType = new HashMap<>();
		insertRecordType(eventType, type);
		final int eventTypeID = Repository.upsert(eventType, EntityManager.NODE_EVENT_TYPE);

		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventTypeID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperTypeID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
	}


	private static class SeparatorComboBoxRenderer extends DefaultListCellRenderer{

		private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(1, 1, 1, 1);


		private SeparatorComboBoxRenderer(){}

		@Override
		public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected,
				final boolean cellHasFocus){
			final TypeItem item = (TypeItem)value;
			if(item != null && item.id < 0){
				final JLabel separatorLabel = new JLabel(value.toString());
				final Font currentFont = separatorLabel.getFont();
				if(!currentFont.isBold())
					separatorLabel.setFont(new Font(currentFont.getName(), Font.BOLD, currentFont.getSize()));
				separatorLabel.setEnabled(false);
				separatorLabel.setOpaque(true);
				separatorLabel.setBorder(EMPTY_BORDER);
				separatorLabel.setHorizontalAlignment(SwingConstants.CENTER);
				return separatorLabel;
			}
			else{
				final Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setText(value == null? StringUtils.SPACE: value.toString());
				return comp;
			}
		}

	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> event1 = new HashMap<>();
		event1.put("description", "a birth");
		int event1ID = Repository.upsert(event1, EntityManager.NODE_EVENT);
		final Map<String, Object> eventType = new HashMap<>();
		eventType.put("id", 0);
		insertRecordType(eventType, "birth");
		insertRecordCategory(eventType, "birth");
		final int eventTypeID = Repository.upsert(eventType, EntityManager.NODE_EVENT_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_EVENT_TYPE, eventTypeID,
			EntityManager.RELATIONSHIP_OF_TYPE, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);

		final Map<String, Object> place1 = new HashMap<>();
		place1.put("identifier", "place 1");
		place1.put("name", "name of the place");
		place1.put("locale", "en-US");
		int place1ID = Repository.upsert(place1, EntityManager.NODE_PLACE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_PLACE, place1ID,
			EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> date1 = new HashMap<>();
		date1.put("date", "18 OCT 2000");
		int date1ID = Repository.upsert(date1, EntityManager.NODE_HISTORIC_DATE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT, event1ID,
			EntityManager.NODE_HISTORIC_DATE, date1ID,
			EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> note1 = new HashMap<>();
		note1.put("note", "note 1");
		int note1ID = Repository.upsert(note1, EntityManager.NODE_NOTE);
		Repository.upsertRelationship(EntityManager.NODE_NOTE, note1ID,
			EntityManager.NODE_EVENT, event1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);

		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("restriction", "confidential");
		int restriction1ID = Repository.upsert(restriction1, EntityManager.NODE_RESTRICTION);
		Repository.upsertRelationship(EntityManager.NODE_RESTRICTION, restriction1ID,
			EntityManager.NODE_EVENT, event1ID,
			EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final EventDialog dialog = create(parent);
//			final EventDialog dialog = createEditOnly(parent);
			dialog.loadData();
			if(!dialog.selectData(event1ID))
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
							final PlaceDialog placeDialog = (dialog.isViewOnlyComponent(dialog.placeButton)
								? PlaceDialog.createSelectOnly(parent)
								: PlaceDialog.create(parent))
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_EVENT, eventID,
											EntityManager.NODE_PLACE, recordID,
											EntityManager.RELATIONSHIP_HAPPENED_IN, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									else
										Repository.deleteRelationship(EntityManager.NODE_EVENT, eventID,
											EntityManager.NODE_PLACE, recordID,
											EntityManager.RELATIONSHIP_HAPPENED_IN);

									//update UI
									final boolean hasPlace = Repository.hasPlace(EntityManager.NODE_EVENT, eventID);
									dialog.setButtonEnableAndBorder(dialog.placeButton, hasPlace);
								});
							final Map.Entry<String, Map<String, Object>> placeNode = Repository.findReferencedNode(
								EntityManager.NODE_EVENT, eventID,
								EntityManager.RELATIONSHIP_HAPPENED_IN);
							placeDialog.loadData();
							if(placeNode != null && EntityManager.NODE_PLACE.equals(placeNode.getKey()))
								placeDialog.selectData(extractRecordID(placeNode.getValue()));

							placeDialog.showDialog();
						}

						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = (dialog.isViewOnlyComponent(dialog.dateButton)
								? HistoricDateDialog.createSelectOnly(parent)
								: HistoricDateDialog.create(parent))
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_EVENT, eventID,
											EntityManager.NODE_HISTORIC_DATE, recordID,
											EntityManager.RELATIONSHIP_HAPPENED_ON, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									else
										Repository.deleteRelationship(EntityManager.NODE_EVENT, eventID,
											EntityManager.NODE_HISTORIC_DATE, recordID,
											EntityManager.RELATIONSHIP_HAPPENED_ON);

									//update UI
									final boolean hasDate = Repository.hasDate(EntityManager.NODE_EVENT, eventID);
									dialog.setButtonEnableAndBorder(dialog.dateButton, hasDate);
								});
							final Map.Entry<String, Map<String, Object>> dateEndNode = Repository.findReferencedNode(
								EntityManager.NODE_EVENT, eventID,
								EntityManager.RELATIONSHIP_HAPPENED_ON);
							historicDateDialog.loadData();
							if(dateEndNode != null && EntityManager.NODE_HISTORIC_DATE.equals(dateEndNode.getKey()))
								historicDateDialog.selectData(extractRecordID(dateEndNode.getValue()));

							historicDateDialog.showDialog();
						}

						case NOTE -> {
							final NoteDialog noteDialog = (dialog.isViewOnlyComponent(dialog.noteButton)
								? NoteDialog.createSelectOnly(parent)
								: NoteDialog.create(parent))
								.withReference(EntityManager.NODE_EVENT, eventID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_EVENT, eventID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									else
										Repository.deleteRelationship(EntityManager.NODE_NOTE, recordID,
											EntityManager.NODE_EVENT, eventID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									final boolean hasNotes = Repository.hasNotes(EntityManager.NODE_EVENT, eventID);
									dialog.setButtonEnableAndBorder(dialog.noteButton, hasNotes);
								});
							noteDialog.loadData();

							noteDialog.showDialog();
						}

						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
								? MediaDialog.createSelectOnlyForMedia(parent)
								: MediaDialog.createForMedia(parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(EntityManager.NODE_EVENT, eventID)
								.withOnCloseGracefully((record, recordID) -> {
									if(record != null)
										Repository.upsertRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_EVENT, eventID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY);
									else
										Repository.deleteRelationship(EntityManager.NODE_MEDIA, recordID,
											EntityManager.NODE_EVENT, eventID,
											EntityManager.RELATIONSHIP_FOR);

									//update UI
									final boolean hasMedia = Repository.hasMedia(EntityManager.NODE_EVENT, eventID);
									dialog.setButtonEnableAndBorder(dialog.mediaButton, hasMedia);
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}

						//TODO
						case EVENT_TYPE -> {
							//if type is not present in the list, show a dialog to insert it within its appropriate super-type
							final EventTypeDialog eventSuperTypeDialog = EventTypeDialog.create(parent)
								.withOnCloseGracefully((record, recordID) -> {
									final TypeItem newType = new TypeItem(EntityManager.extractRecordType(record), recordID);
									final Map<String, Object> eventSuperType = Repository.findReferencedNode(
											EntityManager.NODE_EVENT_TYPE, eventID,
											EntityManager.RELATIONSHIP_OF)
										.getValue();
									final String superTypeMenuItemText = MENU_SEPARATOR_START
										+ extractRecordSuperType(eventSuperType)
										+ MENU_SEPARATOR_END;

									//add `newType` at the end of the `superTypeMenuItemText` section
									for(int i = 0, length = dialog.typeComboBox.getItemCount(); i < length; i ++)
										if(superTypeMenuItemText.equals(dialog.typeComboBox.getItemAt(i).toString())){
											//skip to end of section
											while(++ i < length){
												final String text = dialog.typeComboBox.getItemAt(i)
													.toString();
												if(text.startsWith(MENU_SEPARATOR_START) && text.endsWith(MENU_SEPARATOR_END))
													break;
											}

											//add new menu item
											dialog.typeComboBox.insertItemAt(newType, i);
											break;
										}

									dialog.typeComboBox.setSelectedItem(newType);
								});
							eventSuperTypeDialog.showNewRecord();

							eventSuperTypeDialog.showDialog();
						}

						case MODIFICATION_HISTORY_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Show modification note for " + title + " " + eventID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case MODIFICATION_HISTORY_EDIT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationNoteEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Edit modification note for " + title + " " + eventID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}

						case RESEARCH_STATUS_SHOW -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createShowOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Show research status for " + title + " " + eventID);
							researchStatusDialog.loadData();
							researchStatusDialog.selectData(researchStatusID);

							researchStatusDialog.showDialog();
						}
						case RESEARCH_STATUS_EDIT -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final ResearchStatusDialog researchStatusDialog = ResearchStatusDialog.createEditOnly(parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle("Edit research status for " + title + " " + eventID);
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
											EntityManager.NODE_EVENT, parentRecordID,
											EntityManager.RELATIONSHIP_FOR, Collections.emptyMap(),
											GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
									else
										Repository.deleteRelationship(EntityManager.NODE_RESEARCH_STATUS, recordID,
											EntityManager.NODE_EVENT, parentRecordID,
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
