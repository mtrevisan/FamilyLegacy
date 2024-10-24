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

import io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.persistence.db.GraphDatabaseManager;
import io.github.mtrevisan.familylegacy.flef.persistence.repositories.Repository;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class EventTypeDialog extends CommonRecordDialog{

	@Serial
	private static final long serialVersionUID = 8998243615466495079L;


	private static final class SuperTypeItem{
		private final String label;
		private final int id;

		private SuperTypeItem(final String label, final int id){
			this.label = label;
			this.id = id;
		}

		public int getID(){
			return id;
		}

		@Override
		public String toString(){
			return label;
		}

		@Override
		public boolean equals(final Object obj){
			if(this == obj)
				return true;
			if(obj == null || getClass() != obj.getClass())
				return false;

			final SuperTypeItem rhs = (SuperTypeItem)obj;
			return Objects.equals(id, rhs.id);
		}
	}
	private static final SuperTypeItem NULL_SUPER_TYPE = new SuperTypeItem(null, -1);


	private final JLabel superTypeLabel = new JLabel("Super type:");
	private final JComboBox<SuperTypeItem> superTypeComboBox = new JComboBox<>(extractDefaultItems());
	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	private final JLabel categoryLabel = new JLabel("Category:");
	//"birth" and "adoption" only if `superTypeComboBox` is "Personal origins"
	//"death" only if `superTypeComboBox` is "Death & Burial"
	//"union" only if `superTypeComboBox` is "Marriage & Family life"
	private final JComboBox<String> categoryComboBox = new JComboBox<>();


	public static EventTypeDialog create(final Frame parent){
		final EventTypeDialog dialog = new EventTypeDialog(parent);
		dialog.initialize();
		return dialog;
	}


	private EventTypeDialog(final Frame parent){
		super(parent);
	}


	public EventTypeDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.NODE_EVENT_TYPE;
	}

	@Override
	protected void initComponents(){
		GUIHelper.bindLabelAutoComplete(superTypeLabel, superTypeComboBox);
		GUIHelper.bindOnSelectionChange(superTypeComboBox, this::saveData);
		addMandatoryField(superTypeComboBox);
		final ActionListener updateCategoryComboBox = evt -> {
			SuperTypeItem selectedSuperType = (SuperTypeItem)superTypeComboBox.getSelectedItem();
			if(selectedSuperType == null)
				selectedSuperType = NULL_SUPER_TYPE;
			categoryComboBox.removeAllItems();
			categoryComboBox.setEnabled(true);
			categoryComboBox.addItem(StringUtils.EMPTY);
			switch(selectedSuperType.label){
				case "Personal origins" -> {
					categoryComboBox.addItem("birth");
					categoryComboBox.addItem("adoption");
				}
				case "Death & Burial" -> categoryComboBox.addItem("death");
				case "Marriage & Family life" -> categoryComboBox.addItem("union");
				case null, default -> categoryComboBox.setEnabled(false);
			}
		};
		superTypeComboBox.addActionListener(updateCategoryComboBox);
		updateCategoryComboBox.actionPerformed(null);

		GUIHelper.bindLabelUndo(typeLabel, typeField);
		GUIHelper.bindOnTextChange(typeField, this::saveData);
		addMandatoryField(typeField);

		GUIHelper.bindLabelAutoComplete(categoryLabel, categoryComboBox);
		GUIHelper.bindOnSelectionChange(categoryComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanel.add(superTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(superTypeComboBox, "grow,wrap related");
		recordPanel.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(typeField, "grow,wrap related");
		recordPanel.add(categoryLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(categoryComboBox, "grow");
	}

	public void loadData(final Integer eventID){
		final Map<String, Object> record = Repository.findByID(EntityManager.NODE_EVENT_TYPE, eventID);
		final String capitalizedTableName = StringUtils.capitalize(getTableName());
		setTitle((eventID != null? capitalizedTableName + " ID " + eventID: StringHelper.pluralize(capitalizedTableName)));

		selectedRecord = (record != null? new HashMap<>(record): Collections.emptyMap());

		selectActionInner();
	}

	@Override
	public void loadData(){
		selectedRecord = new HashMap<>(0);

		selectActionInner();
	}

	public void showNewRecord(){
		showNewRecord(null);
	}

	public void showNewRecord(final String type){
		setTitle(type != null? "New Event Type for `" + type + "`": "New Event Type");

		//create a new record
		final String tableName = getTableName();
		final Map<String, Object> newRecord = new HashMap<>();
		insertRecordType(newRecord, type);
		Repository.upsert(newRecord, tableName);

		selectedRecord = newRecord;

		selectActionInner();
	}

	private void selectActionInner(){
		selectedRecordHash = selectedRecord.hashCode();

		if(newRecordDefault != null)
			newRecordDefault.accept(selectedRecord);

		ignoreEvents = true;
		fillData();
		ignoreEvents = false;


		//set focus on first field
		superTypeComboBox.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer eventTypeID = EntityManager.extractRecordID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final SuperTypeItem superType = (type != null? extractRecordSuperType(eventTypeID): null);
		final String category = extractRecordCategory(selectedRecord);

		final ItemEvent itemEvent = new ItemEvent(categoryComboBox, ItemEvent.ITEM_STATE_CHANGED, categoryComboBox.getItemAt(0),
			ItemEvent.SELECTED);
		final ItemListener[] itemListeners = categoryComboBox.getItemListeners();
		for(int i = 0, length = itemListeners.length; i < length; i ++)
			itemListeners[i].itemStateChanged(itemEvent);

		superTypeComboBox.setSelectedItem(superType);
		typeField.setText(type);
		categoryComboBox.setSelectedItem(category);
	}

	private SuperTypeItem extractRecordSuperType(final Integer eventTypeID){
		final Map.Entry<String, Map<String, Object>> eventSuperTypeNode = Repository.findReferencedNode(
			EntityManager.NODE_EVENT_TYPE, eventTypeID,
			EntityManager.RELATIONSHIP_OF);
		if(eventSuperTypeNode == null || !EntityManager.NODE_EVENT_SUPER_TYPE.equals(eventSuperTypeNode.getKey()))
			return null;

		final Map<String, Object> superTypeRecord = eventSuperTypeNode.getValue();
		return new SuperTypeItem(EntityManager.extractRecordSuperType(superTypeRecord), extractRecordID(superTypeRecord));
	}

	@Override
	protected void clearData(){
		typeField.setText(null);
		categoryComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final SuperTypeItem selectedItem = (SuperTypeItem)superTypeComboBox.getSelectedItem();
		final Integer superTypeID = (selectedItem != null? selectedItem.getID(): null);
		if(!validData(superTypeID)){
			JOptionPane.showMessageDialog(getParent(), "Super type field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			superTypeComboBox.requestFocusInWindow();

			return false;
		}
		final String type = GUIHelper.getTextTrimmed(typeField);
		if(!validData(type)){
			JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			typeField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final SuperTypeItem selectedItem = (SuperTypeItem)superTypeComboBox.getSelectedItem();
		final Integer superTypeID = (selectedItem != null? selectedItem.getID(): null);
		final String type = GUIHelper.getTextTrimmed(typeField);
		final String category = GUIHelper.getTextTrimmed(categoryComboBox);

		final Integer selectedRecordID = extractRecordID(selectedRecord);
		Repository.deleteRelationship(EntityManager.NODE_EVENT_TYPE, selectedRecordID,
			EntityManager.NODE_EVENT_SUPER_TYPE,
			EntityManager.RELATIONSHIP_OF);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, selectedRecordID,
			EntityManager.NODE_EVENT_SUPER_TYPE, superTypeID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);
		insertRecordType(selectedRecord, type);
		insertRecordCategory(selectedRecord, category);

		return true;
	}

	public void showDialog(){
		setLocationRelativeTo(getParent());
		setVisible(true);
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
		addSuperTypeAndTypes("Death & Burial",
			"death", "execution", "autopsy", "funeral", "cremation", "scattering of ashes", "inurnment", "burial", "exhumation",
			"reburial");
		addSuperTypeAndTypes("Others",
			"anecdote", "political affiliation", "hobby", "partnership", "celebration of life", "ran away from home");
		addSuperTypeAndTypes("Religious events",
			"religion", "religious conversion", "bar mitzvah", "bas mitzvah", "baptism", "excommunication", "christening",
			"confirmation", "ordination", "blessing", "first communion");
	}

	private static SuperTypeItem[] extractDefaultItems(){
		addDefaultItemsToDatabase();

		final List<Map<String, Object>> superTypeRecords = Repository.findAll(EntityManager.NODE_EVENT_SUPER_TYPE);
		final Comparator<Map<String, Object>> comparator = Comparator.comparing(EntityManager::extractRecordID);
		superTypeRecords.sort(comparator);
		final int length = superTypeRecords.size();
		final List<SuperTypeItem> items = new ArrayList<>(length + 1);
		items.add(null);
		for(int i = 0; i < length; i ++){
			final Map<String, Object> superTypeRecord = superTypeRecords.get(i);

			items.add(new SuperTypeItem(EntityManager.extractRecordSuperType(superTypeRecord), extractRecordID(superTypeRecord)));
		}
		return items.toArray(SuperTypeItem[]::new);
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



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> eventType0 = new HashMap<>();
		eventType0.put("id", 0);
		insertRecordType(eventType0, "death");
		insertRecordCategory(eventType0, "death");
		final int eventType0ID = Repository.upsert(eventType0, EntityManager.NODE_EVENT_TYPE);

		final Map<String, Object> eventSuperType0 = new HashMap<>();
		eventSuperType0.put("id", 0);
		eventSuperType0.put("super_type", "Death");
		int eventSuperType0ID = Repository.upsert(eventSuperType0, EntityManager.NODE_EVENT_SUPER_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventType0ID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType0ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(),
			GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY, GraphDatabaseManager.OnDeleteType.CASCADE);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final EventTypeDialog dialog = create(parent);
			dialog.setTitle("Event Type");
			dialog.loadData();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new WindowAdapter(){
				@Override
				public void windowClosing(final WindowEvent e){
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
