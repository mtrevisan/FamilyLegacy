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
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class EventTypeDialog extends CommonRecordDialog{

	@Serial
	private static final long serialVersionUID = 8998243615466495079L;

	private static final String TABLE_NAME = "event_type";
	private static final String TABLE_NAME_EVENT_SUPER_TYPE = "event_super_type";


	private JLabel superTypeLabel;
	private JComboBox<String> superTypeComboBox;
	private JLabel typeLabel;
	private JTextField typeField;
	private JLabel categoryLabel;
	private JComboBox<String> categoryComboBox;


	public static EventTypeDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new EventTypeDialog(store, parent);
	}


	private EventTypeDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public EventTypeDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected void initRecordComponents(){
		superTypeLabel = new JLabel("Super type:");
		superTypeComboBox = new JComboBox<>(new String[]{null, "Historical events", "Personal origins", "Physical description",
			"Citizenship and migration", "Real estate assets", "Education", "Work and Career", "Legal Events and Documents",
			"Health problems and habits", "Marriage and family life", "Military", "Confinement", "Transfers and travel", "Accolades",
			"Death and burial", "Others", "Religious events"});
		typeLabel = new JLabel("Type:");
		typeField = new JTextField();
		categoryLabel = new JLabel("Category:");
		//"birth" and "adoption" only if `superTypeComboBox` is "Personal origins"
		//"death" only if `superTypeComboBox` is "Death and burial"
		//"union" only if `superTypeComboBox` is "Marriage and family life"
		categoryComboBox = new JComboBox<>();


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

	public void loadData(final Map<String, Object> record){
		final Integer recordID = extractRecordID(record);
		final String capitalizedTableName = StringUtils.capitalize(getTableName());
		setTitle((recordID != null? capitalizedTableName + " ID " + recordID: StringHelper.pluralize(capitalizedTableName)));

		selectedRecord = record;

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
		final NavigableMap<Integer, Map<String, Object>> storeTables = getRecords(getTableName());
		final int nextRecordID = extractNextRecordID(storeTables);
		final Map<String, Object> newRecord = new HashMap<>();
		newRecord.put("id", nextRecordID);
		newRecord.put("type", type);
		storeTables.put(nextRecordID, newRecord);

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
		final Map<Integer, Map<String, Object>> storeEventSuperTypes = getRecords(TABLE_NAME_EVENT_SUPER_TYPE);
		final Integer superTypeID = extractRecordSuperTypeID(selectedRecord);
		final String superType = (superTypeID != null? extractRecordSuperType(storeEventSuperTypes.get(superTypeID)): null);
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
		final Integer superTypeID = getRecords(TABLE_NAME_EVENT_SUPER_TYPE)
			.values().stream()
			.filter(entry -> Objects.equals(superType, extractRecordSuperType(entry)))
			.findFirst()
			.map(CommonRecordDialog::extractRecordID)
			.orElse(null);
		final String type = GUIHelper.getTextTrimmed(typeField);
		final String category = GUIHelper.getTextTrimmed(categoryComboBox);

		selectedRecord.put("super_type_id", superTypeID);
		selectedRecord.put("type", type);
		selectedRecord.put("category", category);

		return true;
	}


	private static Integer extractRecordSuperTypeID(final Map<String, Object> record){
		return (Integer)record.get("super_type_id");
	}

	private static String extractRecordSuperType(final Map<String, Object> record){
		return (String)record.get("super_type");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordCategory(final Map<String, Object> record){
		return (String)record.get("category");
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> eventTypes = new TreeMap<>();
		store.put("event_type", eventTypes);
		final Map<String, Object> eventType1 = new HashMap<>();
		eventType1.put("id", 1);
		eventType1.put("super_type_id", 15);
		eventType1.put("type", "death");
		eventType1.put("category", "death");
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

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final EventTypeDialog dialog = create(store, parent);
			dialog.setTitle("Event Type");
			dialog.loadData(eventType1);

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
					System.out.println(store);
					System.exit(0);
				}
			});
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
