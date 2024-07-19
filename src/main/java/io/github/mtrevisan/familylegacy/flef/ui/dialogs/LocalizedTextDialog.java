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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
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
import java.awt.EventQueue;
import java.awt.Frame;
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class LocalizedTextDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -8409918543709413945L;

	private static final int TABLE_INDEX_RECORD_TEXT = 1;

	private static final String TABLE_NAME = "localized_text";


	private JLabel primaryTextLabel;
	private TextPreviewPane textTextPreview;
	private JTextField primaryTextField;
	private JLabel secondaryTextLabel;
	private JTextField secondaryTextField;
	private JLabel localeLabel;
	private JTextField localeField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel transcriptionLabel;
	private JComboBox<String> transcriptionComboBox;
	private JLabel transcriptionTypeLabel;
	private JComboBox<String> transcriptionTypeComboBox;

	private String filterReferenceTable;
	private int filterReferenceID;
	private String filterReferenceType;

	private boolean simplePrimaryText;
	private boolean withSecondaryInput;


	public static LocalizedTextDialog createComplexText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new LocalizedTextDialog(store, parent);
	}

	public static LocalizedTextDialog createSimpleText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.simplePrimaryText = true;
		return dialog;
	}

	public static LocalizedTextDialog createSimpleTextWithSecondary(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.simplePrimaryText = true;
		dialog.withSecondaryInput = true;
		return dialog;
	}


	private LocalizedTextDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public LocalizedTextDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		Consumer<Map<String, Object>> innerOnCloseGracefully = record -> {
			final TreeMap<Integer, Map<String, Object>> mediaJunctions = getRecords(TABLE_NAME_LOCALIZED_TEXT_JUNCTION);
			final int mediaJunctionID = extractNextRecordID(mediaJunctions);
			if(selectedRecord != null){
				final Integer localizedTextID = extractRecordID(selectedRecord);
				final Map<String, Object> mediaJunction = new HashMap<>();
				mediaJunction.put("id", mediaJunctionID);
				mediaJunction.put("localized_text_id", localizedTextID);
				mediaJunction.put("reference_table", filterReferenceTable);
				mediaJunction.put("reference_id", filterReferenceID);
				mediaJunction.put("reference_type", filterReferenceType);
				mediaJunctions.put(mediaJunctionID, mediaJunction);
			}
			else
				mediaJunctions.remove(mediaJunctionID);
		};
		if(onCloseGracefully != null)
			innerOnCloseGracefully = innerOnCloseGracefully.andThen(onCloseGracefully);

		super.setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public LocalizedTextDialog withReference(final String referenceTable, final int filterReferenceID, final String filterReferenceType){
		this.filterReferenceTable = referenceTable;
		this.filterReferenceID = filterReferenceID;
		this.filterReferenceType = filterReferenceType;

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
		setTitle("Localized texts"
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_TEXT, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		primaryTextLabel = new JLabel(withSecondaryInput? "(Primary) Name:": "Text:");
		textTextPreview = TextPreviewPane.createWithPreview(this);
		textTextPreview.setTextViewFont(primaryTextLabel.getFont());
		primaryTextField = new JTextField();
		secondaryTextLabel = new JLabel("(Secondary) Name:");
		secondaryTextField = new JTextField();
		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "original", "transliteration", "translation"});
		transcriptionLabel = new JLabel("Transcription:");
		transcriptionComboBox = new JComboBox<>(new String[]{null, "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji", "kana", "hangul"});
		transcriptionTypeLabel = new JLabel("Transcription type:");
		transcriptionTypeComboBox = new JComboBox<>(new String[]{null, "romanized", "anglicized", "cyrillized", "francized", "gairaigized",
			"latinized"});


		if(simplePrimaryText){
			GUIHelper.bindLabelTextChangeUndo(primaryTextLabel, primaryTextField, this::saveData);
			GUIHelper.bindLabelTextChangeUndo(secondaryTextLabel, secondaryTextField, this::saveData);
			addMandatoryField(primaryTextField, secondaryTextField);
		}
		else{
			GUIHelper.bindLabelTextChange(primaryTextLabel, textTextPreview, this::saveData);
			textTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
		}

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionLabel, transcriptionComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionTypeLabel, transcriptionTypeComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(primaryTextLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add((simplePrimaryText? primaryTextField: textTextPreview), "growx,wrap related");
		if(withSecondaryInput){
			recordPanelBase.add(secondaryTextLabel, "align label,sizegroup lbl,split 2");
			recordPanelBase.add(secondaryTextField, "growx,wrap related");
		}
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "growx,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionComboBox, "growx,wrap related");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "growx");

		recordTabbedPane.add("base", recordPanelBase);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = new HashMap<>(getRecords(TABLE_NAME));
		if(filterReferenceTable != null){
			final Set<Integer> filteredMedias = getFilteredRecords(TABLE_NAME_LOCALIZED_TEXT_JUNCTION, filterReferenceTable, filterReferenceID)
				.values().stream()
				.filter(record -> filterReferenceType.equals(extractRecordType(record)))
				.map(CommonRecordDialog::extractRecordID)
				.collect(Collectors.toSet());
			records.entrySet()
				.removeIf(entry -> !filteredMedias.contains(entry.getKey()));
		}

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String primaryName = (withSecondaryInput? extractRecordPrimaryName(container): extractRecordText(container));
			final String secondaryName = extractRecordSecondaryName(container);
			final StringJoiner identifier = new StringJoiner(", ");
			if(primaryName != null)
				identifier.add(primaryName);
			if(secondaryName != null)
				identifier.add(secondaryName);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier.toString(), row, TABLE_INDEX_RECORD_TEXT);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.getTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_TEXT);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String text = extractRecordText(selectedRecord);
		final String primaryName = extractRecordPrimaryName(selectedRecord);
		final String secondaryName = extractRecordSecondaryName(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		if(withSecondaryInput){
			primaryTextField.setText(primaryName);
			secondaryTextField.setText(secondaryName);
		}
		else if(simplePrimaryText)
			primaryTextField.setText(text);
		else
			textTextPreview.setText("Extract " + extractRecordID(selectedRecord), text, locale);
		localeField.setText(locale);
		typeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);

		if(filterReferenceTable == null){
			final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				LocalizedTextDialog::extractRecordLocalizedTextID, extractRecordID(selectedRecord));
			if(recordMediaJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");
		}
	}

	@Override
	protected void clearData(){
		if(simplePrimaryText){
			primaryTextField.setText(null);
			if(withSecondaryInput)
				secondaryTextField.setText(null);
		}
		else
			textTextPreview.clear();

		localeField.setText(null);

		typeComboBox.setSelectedItem(null);

		transcriptionComboBox.setSelectedItem(null);

		transcriptionTypeComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String primaryText = (simplePrimaryText? GUIHelper.getTextTrimmed(primaryTextField): textTextPreview.getTextTrimmed());
		final String secondaryText = GUIHelper.getTextTrimmed(secondaryTextField);
		if(!validData(primaryText) && !validData(secondaryText)){
			final String message = withSecondaryInput? "(Primary or secondary) Name field is required": "Text field is required";
			JOptionPane.showMessageDialog(getParent(), message, "Error", JOptionPane.ERROR_MESSAGE);
			primaryTextField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String primaryText = (simplePrimaryText? GUIHelper.getTextTrimmed(primaryTextField): textTextPreview.getTextTrimmed());
		final String secondaryText = GUIHelper.getTextTrimmed(secondaryTextField);
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String transcription = GUIHelper.getTextTrimmed(transcriptionComboBox);
		final String transcriptionType = GUIHelper.getTextTrimmed(transcriptionTypeComboBox);

		//update table:
		final boolean shouldUpdate = (!withSecondaryInput && !Objects.equals(primaryText, extractRecordText(selectedRecord))
			|| withSecondaryInput && (!Objects.equals(primaryText, extractRecordPrimaryName(selectedRecord))
				|| !Objects.equals(secondaryText, extractRecordSecondaryName(selectedRecord))));
		if(shouldUpdate){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					final StringJoiner text = new StringJoiner(", ");
					if(primaryText != null)
						text.add(primaryText);
					if(secondaryText != null)
						text.add(secondaryText);

					model.setValueAt(text.toString(), modelRowIndex, TABLE_INDEX_RECORD_TEXT);

					break;
				}
		}

		if(filterReferenceTable != null){
			//TODO upsert junction
		}

		if(withSecondaryInput){
			selectedRecord.put("primary_name", primaryText);
			selectedRecord.put("secondary_name", secondaryText);
		}
		else
			selectedRecord.put("text", primaryText);
		selectedRecord.put("locale", locale);
		selectedRecord.put("type", type);
		selectedRecord.put("transcription", transcription);
		selectedRecord.put("transcription_type", transcriptionType);

		return true;
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

	private static Integer extractRecordLocalizedTextID(final Map<String, Object> record){
		return (Integer)record.get("localized_text_id");
	}

	private static String extractRecordPrimaryName(final Map<String, Object> record){
		return (String)record.get("primary_name");
	}

	private static String extractRecordSecondaryName(final Map<String, Object> record){
		return (String)record.get("secondary_name");
	}


	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
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

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put(TABLE_NAME, localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "text 1");
		localizedText1.put("primary_name", "primary name");
		localizedText1.put("secondary_name", "secondary name");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put(TABLE_NAME_LOCALIZED_TEXT_JUNCTION, localizedTextJunctions);
		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("id", 1);
		localizedTextJunction1.put("localized_text_id", 1);
		localizedTextJunction1.put("reference_table", "localized_text");
		localizedTextJunction1.put("reference_id", 1);
		localizedTextJunction1.put("reference_type", "extract");
		localizedTextJunctions.put((Integer)localizedTextJunction1.get("id"), localizedTextJunction1);

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

//			final LocalizedTextDialog dialog = createComplexText(store, parent);
//			final LocalizedTextDialog dialog = createSimpleText(store, parent);
			final LocalizedTextDialog dialog = createSimpleTextWithSecondary(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(localizedText1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.out.println(store);
					System.exit(0);
				}
			});
			//complex
//			dialog.setSize(420, 581);
			//simple
//			dialog.setSize(420, 453);
			//with secondary
			dialog.setSize(420, 480);
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
