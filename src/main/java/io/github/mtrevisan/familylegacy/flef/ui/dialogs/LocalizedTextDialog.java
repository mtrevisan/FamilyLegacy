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

import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JComboBox;
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


public final class LocalizedTextDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -8409918543709413945L;

	private static final int TABLE_INDEX_RECORD_TEXT = 1;

	private static final String TABLE_NAME = "localized_text";


	private JLabel textLabel;
	private JTextField textField;
	private JLabel localeLabel;
	private JTextField localeField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel transcriptionLabel;
	private JComboBox<String> transcriptionComboBox;
	private JLabel transcriptionTypeLabel;
	private JComboBox<String> transcriptionTypeComboBox;


	public LocalizedTextDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public LocalizedTextDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Localized texts");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_TEXT, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		textLabel = new JLabel("Text:");
		textField = new JTextField();
		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"original", "transliteration", "translation"});
		transcriptionLabel = new JLabel("Transcription:");
		transcriptionComboBox = new JComboBox<>(new String[]{"IPA", "Wade-Giles", "hanyu pinyin",
			"wāpuro rōmaji", "kana", "hangul"});
		transcriptionTypeLabel = new JLabel("Transcription type:");
		transcriptionTypeComboBox = new JComboBox<>(new String[]{"romanized", "anglicized", "cyrillized",
			"francized", "gairaigized", "latinized"});


		GUIHelper.bindLabelTextChangeUndo(textLabel, textField, evt -> saveData());
		GUIHelper.setBackgroundColor(textField, MANDATORY_FIELD_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(transcriptionLabel, transcriptionComboBox, evt -> saveData(),
			evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(transcriptionTypeLabel, transcriptionTypeComboBox, evt -> saveData(),
			evt -> saveData());
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(textLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(textField, "growx,wrap paragraph");
		recordPanelBase.add(localeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(localeField, "growx,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(transcriptionComboBox, "growx,wrap paragraph");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "growx");

		recordTabbedPane.add("base", recordPanelBase);
	}

	@Override
	protected void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final String identifier = extractRecordText(record.getValue());

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_TEXT);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_TEXT);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String text = extractRecordText(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		textField.setText(text);
		localeField.setText(locale);
		typeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);
	}

	@Override
	protected void clearData(){
		textField.setText(null);
		GUIHelper.setBackgroundColor(textField, Color.WHITE);

		localeField.setText(null);

		typeComboBox.setSelectedItem(null);

		transcriptionComboBox.setSelectedItem(null);

		transcriptionTypeComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String text = GUIHelper.readTextTrimmed(textField);
			//enforce non-nullity on `identifier`
			if(text == null || text.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Text field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				textField.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String text = GUIHelper.readTextTrimmed(textField);
		final String locale = GUIHelper.readTextTrimmed(localeField);
		final String type = (String)typeComboBox.getSelectedItem();
		final String transcription = (String)transcriptionComboBox.getSelectedItem();
		final String transcriptionType = (String)transcriptionTypeComboBox.getSelectedItem();

		//update table
		if(!Objects.equals(text, extractRecordText(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
					model.setValueAt(text, modelRowIndex, TABLE_INDEX_RECORD_TEXT);
					break;
				}
		}

		selectedRecord.put("text", text);
		selectedRecord.put("locale", locale);
		selectedRecord.put("type", type);
		selectedRecord.put("transcription", transcription);
		selectedRecord.put("transcription_type", transcriptionType);
	}


	private static String extractRecordText(final Map<String, Object> record){
		return (String)record.get("text");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordTranscription(final Map<String, Object> record){
		return (String)record.get("transcription");
	}

	private static String extractRecordTranscriptionType(final Map<String, Object> record){
		return (String)record.get("transcription_type");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -2557082779637153562L;


		RecordTableModel(){
			super(new String[]{"ID", "Date"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> texts = new TreeMap<>();
		store.put(TABLE_NAME, texts);
		final Map<String, Object> text1 = new HashMap<>();
		text1.put("id", 1);
		text1.put("text", "text 1");
		text1.put("locale", "en");
		text1.put("type", "original");
		text1.put("transcription", "IPA");
		text1.put("transcription_type", "romanized");
		texts.put((Integer)text1.get("id"), text1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			};
			EventBusService.subscribe(listener);

			final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
			if(!dialog.loadData(extractRecordID(text1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(420, 492);
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
