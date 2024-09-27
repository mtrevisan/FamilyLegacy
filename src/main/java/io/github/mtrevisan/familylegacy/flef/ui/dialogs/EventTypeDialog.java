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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class EventTypeDialog extends CommonRecordDialog{

	@Serial
	private static final long serialVersionUID = 8998243615466495079L;


	private final JLabel superTypeLabel = new JLabel("Super type:");
	private final JComboBox<String> superTypeComboBox = new JComboBox<>(new String[]{null, "Historical events", "Personal origins",
		"Physical description", "Citizenship and migration", "Real estate assets", "Education", "Work and Career",
		"Legal Events and Documents", "Health problems and habits", "Marriage and family life", "Military", "Confinement",
		"Transfers and travel", "Accolades", "Death and burial", "Others", "Religious events"});
	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	private final JLabel categoryLabel = new JLabel("Category:");
	//"birth" and "adoption" only if `superTypeComboBox` is "Personal origins"
	//"death" only if `superTypeComboBox` is "Death and burial"
	//"union" only if `superTypeComboBox` is "Marriage and family life"
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
		addMandatoryField(superTypeComboBox);
		final ActionListener updateCategoryComboBox = evt -> {
			final String selectedSuperType = (String)superTypeComboBox.getSelectedItem();
			categoryComboBox.removeAllItems();
			categoryComboBox.setEnabled(true);
			categoryComboBox.addItem(StringUtils.EMPTY);
			switch(selectedSuperType){
				case "Personal origins" -> {
					categoryComboBox.addItem("birth");
					categoryComboBox.addItem("adoption");
				}
				case "Death and burial" -> categoryComboBox.addItem("death");
				case "Marriage and family life" -> categoryComboBox.addItem("union");
				case null, default -> categoryComboBox.setEnabled(false);
			}
		};
		superTypeComboBox.addActionListener(updateCategoryComboBox);
		updateCategoryComboBox.actionPerformed(null);

		GUIHelper.bindLabelUndo(typeLabel, typeField);
		addMandatoryField(typeField);

		GUIHelper.bindLabelAutoComplete(categoryLabel, categoryComboBox);
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
		final String superType = (type != null? extractRecordSuperType(eventTypeID): null);
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

	private String extractRecordSuperType(final Integer eventTypeID){
		final Map.Entry<String, Map<String, Object>> eventSuperTypeNode = Repository.findReferencedNode(
			EntityManager.NODE_EVENT_TYPE, eventTypeID,
			EntityManager.RELATIONSHIP_OF);
		if(eventSuperTypeNode == null || !EntityManager.NODE_EVENT_SUPER_TYPE.equals(eventSuperTypeNode.getKey()))
			return null;

		return EntityManager.extractRecordSuperType(eventSuperTypeNode.getValue());
	}

	@Override
	protected void clearData(){
		typeField.setText(null);
		categoryComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String superType = GUIHelper.getTextTrimmed(superTypeComboBox);
		if(!validData(superType)){
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
		final String superType = GUIHelper.getTextTrimmed(superTypeComboBox);
		//TODO store super type id into typeComboBox, show description, get superTypeID
		final Integer superTypeID = Repository.findAll(EntityManager.NODE_EVENT_SUPER_TYPE)
			.stream()
			.filter(entry -> Objects.equals(superType, EntityManager.extractRecordSuperType(entry)))
			.findFirst()
			.map(EntityManager::extractRecordID)
			.orElse(null);
		final String type = GUIHelper.getTextTrimmed(typeField);
		final String category = GUIHelper.getTextTrimmed(categoryComboBox);

		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, extractRecordID(selectedRecord),
			EntityManager.NODE_EVENT_SUPER_TYPE, superTypeID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		insertRecordType(selectedRecord, type);
		insertRecordCategory(selectedRecord, category);

		return true;
	}

	public void showDialog(){
		setLocationRelativeTo(getParent());
		setVisible(true);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("type", "death");
		eventType1.put("category", "death");
		int eventType1ID = Repository.upsert(eventType1, EntityManager.NODE_EVENT_TYPE);

		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("super_type", "Historical events");
		Repository.upsert(eventSuperType1, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("super_type", "Personal origins");
		Repository.upsert(eventSuperType2, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("super_type", "Physical description");
		Repository.upsert(eventSuperType3, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("super_type", "Citizenship and migration");
		Repository.upsert(eventSuperType4, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("super_type", "Real estate assets");
		Repository.upsert(eventSuperType5, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("super_type", "Education");
		Repository.upsert(eventSuperType6, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("super_type", "Work and Career");
		Repository.upsert(eventSuperType7, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("super_type", "Legal Events and Documents");
		Repository.upsert(eventSuperType8, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("super_type", "Health problems and habits");
		Repository.upsert(eventSuperType9, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("super_type", "Marriage and family life");
		Repository.upsert(eventSuperType10, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("super_type", "Military");
		Repository.upsert(eventSuperType11, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("super_type", "Confinement");
		Repository.upsert(eventSuperType12, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("super_type", "Transfers and travel");
		Repository.upsert(eventSuperType13, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("super_type", "Accolades");
		Repository.upsert(eventSuperType14, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("super_type", "Death and burial");
		int eventSuperType15ID = Repository.upsert(eventSuperType15, EntityManager.NODE_EVENT_SUPER_TYPE);
		Repository.upsertRelationship(EntityManager.NODE_EVENT_TYPE, eventType1ID,
			EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType15ID,
			EntityManager.RELATIONSHIP_OF, Collections.emptyMap(), GraphDatabaseManager.OnDeleteType.RELATIONSHIP_ONLY,
			GraphDatabaseManager.OnDeleteType.CASCADE);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("super_type", "Others");
		Repository.upsert(eventSuperType16, EntityManager.NODE_EVENT_SUPER_TYPE);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("super_type", "Religious events");
		Repository.upsert(eventSuperType17, EntityManager.NODE_EVENT_SUPER_TYPE);


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
