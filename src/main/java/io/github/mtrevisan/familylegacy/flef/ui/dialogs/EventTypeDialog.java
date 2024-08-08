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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class EventTypeDialog extends CommonRecordDialog{

	@Serial
	private static final long serialVersionUID = 8998243615466495079L;

	private static final String TABLE_NAME = "event_super_type";
	private static final String TABLE_NAME_EVENT_TYPE = "event_type";


	//TODO next (plus test save a new type from EventDialog)
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
		typeLabel = new JLabel("Type:");
		typeField = new JTextField();
		categoryLabel = new JLabel("Category:");
		categoryComboBox = new JComboBox<>(new String[]{"birth", "death", "union", "adoption"});


		GUIHelper.bindLabelTextChangeUndo(typeLabel, typeField, null);
		//TODO
//		addMandatoryField(typeField);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(categoryLabel, categoryComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordPanel){
		recordPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanel.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(typeField, "grow,wrap paragraph");
		recordPanel.add(categoryLabel, "align label,sizegroup lbl,split 2");
		recordPanel.add(categoryComboBox, "grow");
	}

	@Override
	public void loadData(){
		selectedRecord = getRecords(TABLE_NAME)
			.computeIfAbsent(1, k -> new HashMap<>());

		ignoreEvents = true;
		fillData();
		ignoreEvents = false;
	}

	@Override
	protected void fillData(){
		final String type = extractRecordType(selectedRecord);
		final String category = extractRecordCategory(selectedRecord);

		final ItemEvent itemEvent = new ItemEvent(categoryComboBox, ItemEvent.ITEM_STATE_CHANGED, categoryComboBox.getItemAt(0),
			ItemEvent.SELECTED);
		final ItemListener[] itemListeners = categoryComboBox.getItemListeners();
		for(int i = 0, length = itemListeners.length; i < length; i ++)
			itemListeners[i].itemStateChanged(itemEvent);

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
		final String identifier = GUIHelper.getTextTrimmed(typeField);
		if(!validData(identifier)){
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
		final String type = GUIHelper.getTextTrimmed(typeField);
		final String category = GUIHelper.getTextTrimmed(categoryComboBox);

		selectedRecord.put("type", type);
		selectedRecord.put("category", category);

		return true;
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

		final TreeMap<Integer, Map<String, Object>> eventSuperTypes = new TreeMap<>();
		store.put("event_super_type", eventSuperTypes);
		final Map<String, Object> eventSuperType1 = new HashMap<>();
		eventSuperType1.put("id", 1);
		eventSuperType1.put("super_type", "Historical events and relevant facts");
		eventSuperTypes.put((Integer)eventSuperType1.get("id"), eventSuperType1);
		final Map<String, Object> eventSuperType2 = new HashMap<>();
		eventSuperType2.put("id", 2);
		eventSuperType2.put("super_type", "Birth and early life");
		eventSuperTypes.put((Integer)eventSuperType2.get("id"), eventSuperType2);
		final Map<String, Object> eventSuperType3 = new HashMap<>();
		eventSuperType3.put("id", 3);
		eventSuperType3.put("super_type", "Physical condition and personal description");
		eventSuperTypes.put((Integer)eventSuperType3.get("id"), eventSuperType3);
		final Map<String, Object> eventSuperType4 = new HashMap<>();
		eventSuperType4.put("id", 4);
		eventSuperType4.put("super_type", "Nationality and immigration");
		eventSuperTypes.put((Integer)eventSuperType4.get("id"), eventSuperType4);
		final Map<String, Object> eventSuperType5 = new HashMap<>();
		eventSuperType5.put("id", 5);
		eventSuperType5.put("super_type", "Residence and property");
		eventSuperTypes.put((Integer)eventSuperType5.get("id"), eventSuperType5);
		final Map<String, Object> eventSuperType6 = new HashMap<>();
		eventSuperType6.put("id", 6);
		eventSuperType6.put("super_type", "Education and learning");
		eventSuperTypes.put((Integer)eventSuperType6.get("id"), eventSuperType6);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final EventTypeDialog dialog = create(store, parent);
			dialog.setTitle("Event Type");
			dialog.initComponents();
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
