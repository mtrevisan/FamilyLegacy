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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class CalendarDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 9026792737072096011L;

	private static final int TABLE_INDEX_RECORD_TYPE = 1;

	private static final String TABLE_NAME = "calendar";


	private JLabel typeLabel;
	private JTextField typeField;

	private JButton noteButton;


	public CalendarDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public CalendarDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected DefaultTableModel getDefaultTableModel(){
		return new RecordTableModel();
	}

	@Override
	protected void initStoreComponents(){
		setTitle("Calendars");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_TYPE, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		typeLabel = new JLabel("Type:");
		typeField = new JTextField();

		noteButton = new JButton("Notes", ICON_NOTE);


		GUIHelper.bindLabelTextChangeUndo(typeLabel, typeField, evt -> saveData());
		GUIHelper.setBackgroundColor(typeField, MANDATORY_FIELD_BACKGROUND_COLOR);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeField, "growx");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String type = extractRecordType(record.getValue());

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(type, row, TABLE_INDEX_RECORD_TYPE);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_TYPE);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String type = extractRecordType(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);

		typeField.setText(type);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
	}

	@Override
	protected void clearData(){
		typeField.setText(null);
		GUIHelper.setBackgroundColor(typeField, Color.WHITE);

		GUIHelper.setDefaultBorder(noteButton);
		deleteRecordButton.setEnabled(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String type = GUIHelper.readTextTrimmed(typeField);
			//enforce non-nullity on `identifier`
			if(type == null || type.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Type field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				typeField.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String type = GUIHelper.readTextTrimmed(typeField);

		//update table
		if(!Objects.equals(type, extractRecordType(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
					model.setValueAt(type, modelRowIndex, TABLE_INDEX_RECORD_TYPE);
					break;
				}
		}

		selectedRecord.put("type", type);
	}


	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -8500664012392778343L;


		RecordTableModel(){
			super(new String[]{"ID", "Type"}, 0);
		}

		@Override
		public final Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public final boolean isCellEditable(final int row, final int column){
			return false;
		}
	}


	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> calendars = new TreeMap<>();
		store.put(TABLE_NAME, calendars);
		final Map<String, Object> calendar1 = new HashMap<>();
		calendar1.put("id", 1);
		calendar1.put("type", "gregorian");
		calendars.put((Integer)calendar1.get("id"), calendar1);
		final Map<String, Object> calendar2 = new HashMap<>();
		calendar2.put("id", 2);
		calendar2.put("type", "julian");
		calendars.put((Integer)calendar2.get("id"), calendar2);
		final Map<String, Object> calendar3 = new HashMap<>();
		calendar3.put("id", 3);
		calendar3.put("type", "venetan");
		calendars.put((Integer)calendar3.get("id"), calendar3);

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put(TABLE_NAME_NOTE, notes);
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

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode calendar = editCommand.getContainer();
//							dialog.setTitle(calendar.getID() != null
//								? "Note " + calendar.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(calendar, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final CalendarDialog dialog = new CalendarDialog(store, parent);
		if(!dialog.loadData(extractRecordID(calendar3)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(270, 341);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(final java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
