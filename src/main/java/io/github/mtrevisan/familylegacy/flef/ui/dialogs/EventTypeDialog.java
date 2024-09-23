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
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSuperType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordSuperTypeID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordCategory;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordSuperTypeID;
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
		GUIHelper.bindLabelSelectionAutoCompleteChange(superTypeLabel, superTypeComboBox, this::saveData);
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

		GUIHelper.bindLabelTextChangeUndo(typeLabel, typeField, this::saveData);
		addMandatoryField(typeField);

		GUIHelper.bindLabelSelectionAutoCompleteChange(categoryLabel, categoryComboBox, this::saveData);
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
		Repository.save(tableName, newRecord);

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
		final Integer superTypeID = extractRecordSuperTypeID(selectedRecord);
		final Map<String, Object> storeEventSuperType = Repository.findByID(EntityManager.NODE_EVENT_SUPER_TYPE, superTypeID);
		final String superType = (superTypeID != null? extractRecordSuperType(storeEventSuperType): null);
		final String type = extractRecordType(selectedRecord);
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
		final Integer superTypeID = Repository.findAll(EntityManager.NODE_EVENT_SUPER_TYPE)
			.stream()
			.filter(entry -> Objects.equals(superType, extractRecordSuperType(entry)))
			.findFirst()
			.map(EntityManager::extractRecordID)
			.orElse(null);
		final String type = GUIHelper.getTextTrimmed(typeField);
		final String category = GUIHelper.getTextTrimmed(categoryComboBox);

		insertRecordSuperTypeID(selectedRecord, superTypeID);
		insertRecordType(selectedRecord, type);
		insertRecordCategory(selectedRecord, category);

		return true;
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}


		GraphDatabaseManager.clearDatabase();
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("super_type_id", 15);
		eventType1.put("type", "death");
		eventType1.put("category", "death");
		Repository.save(EntityManager.NODE_EVENT_TYPE, eventType1);

		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Personal origins");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical description");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Citizenship and migration");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Real estate assets");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType6);
		final Map<String, Object> eventSuperType7 = new HashMap<>();
		eventSuperType7.put("id", 7);
		eventSuperType7.put("super_type", "Work and Career");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType7);
		final Map<String, Object> eventSuperType8 = new HashMap<>();
		eventSuperType8.put("id", 8);
		eventSuperType8.put("super_type", "Legal Events and Documents");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType8);
		final Map<String, Object> eventSuperType9 = new HashMap<>();
		eventSuperType9.put("id", 9);
		eventSuperType9.put("super_type", "Health problems and habits");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType9);
		final Map<String, Object> eventSuperType10 = new HashMap<>();
		eventSuperType10.put("id", 10);
		eventSuperType10.put("super_type", "Marriage and family life");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType10);
		final Map<String, Object> eventSuperType11 = new HashMap<>();
		eventSuperType11.put("id", 11);
		eventSuperType11.put("super_type", "Military");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType11);
		final Map<String, Object> eventSuperType12 = new HashMap<>();
		eventSuperType12.put("id", 12);
		eventSuperType12.put("super_type", "Confinement");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType12);
		final Map<String, Object> eventSuperType13 = new HashMap<>();
		eventSuperType13.put("id", 13);
		eventSuperType13.put("super_type", "Transfers and travel");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType13);
		final Map<String, Object> eventSuperType14 = new HashMap<>();
		eventSuperType14.put("id", 14);
		eventSuperType14.put("super_type", "Accolades");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType14);
		final Map<String, Object> eventSuperType15 = new HashMap<>();
		eventSuperType15.put("id", 15);
		eventSuperType15.put("super_type", "Death and burial");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType15);
		final Map<String, Object> eventSuperType16 = new HashMap<>();
		eventSuperType16.put("id", 16);
		eventSuperType16.put("super_type", "Others");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType16);
		final Map<String, Object> eventSuperType17 = new HashMap<>();
		eventSuperType17.put("id", 17);
		eventSuperType17.put("super_type", "Religious events");
		Repository.save(EntityManager.NODE_EVENT_SUPER_TYPE, eventSuperType17);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final EventTypeDialog dialog = create(parent);
			dialog.setTitle("Event Type");
			dialog.loadData(1);

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
