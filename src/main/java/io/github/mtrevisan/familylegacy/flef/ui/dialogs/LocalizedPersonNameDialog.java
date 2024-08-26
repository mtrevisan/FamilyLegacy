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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.flef.ui.panels.HistoryPanel;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

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
import java.io.Serial;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class LocalizedPersonNameDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 6171448434725755800L;

	private static final int TABLE_INDEX_TEXT = 2;

	private static final String TABLE_NAME = "localized_person_name";


	private JLabel personalTextLabel;
	private JTextField personalTextField;
	private JLabel familyTextLabel;
	private JTextField familyTextField;
	private JLabel localeLabel;
	private JTextField localeField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel transcriptionLabel;
	private JComboBox<String> transcriptionComboBox;
	private JLabel transcriptionTypeLabel;
	private JComboBox<String> transcriptionTypeComboBox;

	private HistoryPanel historyPanel;

	private int filterReferenceID;


	public static LocalizedPersonNameDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static LocalizedPersonNameDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedPersonNameDialog createRecordOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private LocalizedPersonNameDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public LocalizedPersonNameDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public LocalizedPersonNameDialog withReference(final int referenceID){
		filterReferenceID = referenceID;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceID > 0? " for person name ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Text"};
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
		personalTextLabel = new JLabel("(Primary) Name:");
		personalTextField = new JTextField();
		familyTextLabel = new JLabel("(Secondary) Name:");
		familyTextField = new JTextField();
		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "original", "transliteration", "translation"});
		transcriptionLabel = new JLabel("Transcription:");
		transcriptionComboBox = new JComboBox<>(new String[]{null, "IPA", "Wade-Giles", "hanyu pinyin", "wāpuro rōmaji", "kana",
			"hangul"});
		transcriptionTypeLabel = new JLabel("Transcription type:");
		transcriptionTypeComboBox = new JComboBox<>(new String[]{null, "romanized", "anglicized", "cyrillized", "francized",
			"gairaigized", "latinized"});

		historyPanel = HistoryPanel.create(store)
			.withLinkListener((table, id) -> EventBusService.publish(EditEvent.create(EditEvent.EditType.MODIFICATION_HISTORY, getTableName(),
				Map.of("id", extractRecordID(selectedRecord), "note_id", id))));


		GUIHelper.bindLabelTextChangeUndo(personalTextLabel, personalTextField, this::saveData);
		GUIHelper.bindLabelTextChangeUndo(familyTextLabel, familyTextField, this::saveData);
		addMandatoryField(personalTextField, familyTextField);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionLabel, transcriptionComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionTypeLabel, transcriptionTypeComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personalTextLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(personalTextField, "grow,wrap related");
		recordPanelBase.add(familyTextLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(familyTextField, "grow,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "grow,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionComboBox, "grow,wrap related");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "grow");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("history", historyPanel);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = (filterReferenceID <= 0
			? getRecords(TABLE_NAME)
			: getFilteredRecords(filterReferenceID));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String primaryName = extractRecordPersonalName(container);
			final String secondaryName = extractRecordFamilyName(container);
			final StringJoiner identifier = new StringJoiner(", ");
			if(primaryName != null)
				identifier.add(primaryName);
			if(secondaryName != null)
				identifier.add(secondaryName);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier.toString(), row, TABLE_INDEX_TEXT);

			row ++;
		}
	}

	private Map<Integer, Map<String, Object>> getFilteredRecords(final int filterReferenceID){
		return getRecords(TABLE_NAME)
			.entrySet().stream()
			.filter(entry -> Objects.equals(filterReferenceID, extractRecordPersonNameID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		personalTextField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer localizedPersonNameID = extractRecordID(selectedRecord);
		final String primaryName = extractRecordPersonalName(selectedRecord);
		final String secondaryName = extractRecordFamilyName(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		personalTextField.setText(primaryName);
		familyTextField.setText(secondaryName);
		localeField.setText(locale);
		typeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);

		if(filterReferenceID <= 0){
			final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				LocalizedPersonNameDialog::extractRecordPersonNameID, localizedPersonNameID);
			if(recordMediaJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");
		}

		historyPanel.withReference(TABLE_NAME, localizedPersonNameID);
		historyPanel.loadData();
	}

	@Override
	protected void clearData(){
		personalTextField.setText(null);
		familyTextField.setText(null);

		localeField.setText(null);

		typeComboBox.setSelectedItem(null);
		transcriptionComboBox.setSelectedItem(null);
		transcriptionTypeComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String primaryText = GUIHelper.getTextTrimmed(personalTextField);
		final String secondaryText = GUIHelper.getTextTrimmed(familyTextField);
		if(!validData(primaryText) && !validData(secondaryText)){
			JOptionPane.showMessageDialog(getParent(), "(Primary or secondary) Name field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			personalTextField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String primaryText = GUIHelper.getTextTrimmed(personalTextField);
		final String secondaryText = GUIHelper.getTextTrimmed(familyTextField);
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String transcription = GUIHelper.getTextTrimmed(transcriptionComboBox);
		final String transcriptionType = GUIHelper.getTextTrimmed(transcriptionTypeComboBox);

		//update table:
		final boolean shouldUpdate = (!Objects.equals(primaryText, extractRecordPersonalName(selectedRecord))
			|| !Objects.equals(secondaryText, extractRecordFamilyName(selectedRecord)));
		if(shouldUpdate){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					final StringJoiner text = new StringJoiner(", ");
					if(primaryText != null)
						text.add(primaryText);
					if(secondaryText != null)
						text.add(secondaryText);

					model.setValueAt(text.toString(), modelRowIndex, TABLE_INDEX_TEXT);

					break;
				}
			}
		}

		selectedRecord.put("personal_name", primaryText);
		selectedRecord.put("family_name", secondaryText);
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

	private static Integer extractRecordPersonNameID(final Map<String, Object> record){
		return (Integer)record.get("person_name_id");
	}

	private static String extractRecordPersonalName(final Map<String, Object> record){
		return (String)record.get("personal_name");
	}

	private static String extractRecordFamilyName(final Map<String, Object> record){
		return (String)record.get("family_name");
	}


	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> localizedPersonNames = new TreeMap<>();
		store.put("localized_person_name", localizedPersonNames);
		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("id", 1);
		localizedPersonName1.put("personal_name", "personal name");
		localizedPersonName1.put("family_name", "family name");
		localizedPersonName1.put("locale", "en");
		localizedPersonName1.put("type", "original");
		localizedPersonName1.put("transcription", "IPA");
		localizedPersonName1.put("transcription_type", "romanized");
		localizedPersonNames.put((Integer)localizedPersonName1.get("id"), localizedPersonName1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final LocalizedPersonNameDialog dialog = create(store, parent);
//			final LocalizedPersonNameDialog dialog = createRecordOnly(store, parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(localizedPersonName1)))
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
					final int localizedPersonNameID = extractRecordID(container);
					switch(editCommand.getType()){
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("note_id");
							final NoteDialog changeNoteDialog = NoteDialog.createModificationRecordOnly(store, parent);
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle("Change modification note for " + title + " " + localizedPersonNameID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
					}
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
			//with secondary
			dialog.showDialog();
		});
	}

}
