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
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
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
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordReferenceType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordText;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscriptionType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocalizedTextID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordReferenceType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordText;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordTranscription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordTranscriptionType;


public final class LocalizedTextDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 6171448434725755800L;

	private static final int TABLE_INDEX_TEXT = 2;


	private final JLabel textLabel = new JLabel("Text:");
	private final TextPreviewPane textTextPreview = TextPreviewPane.createWithPreview(LocalizedTextDialog.this);
	private final JTextField simpleTextField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> referenceTypeComboBox = new JComboBox<>(new String[]{null, "original", "transliteration",
		"translation"});
	private final JLabel transcriptionLabel = new JLabel("Transcription:");
	private final JComboBox<String> transcriptionComboBox = new JComboBox<>(new String[]{null, "IPA", "Wade-Giles", "hanyu pinyin",
		"wāpuro rōmaji", "kana", "hangul"});
	private final JLabel transcriptionTypeLabel = new JLabel("Transcription type:");
	private final JComboBox<String> transcriptionTypeComboBox = new JComboBox<>(new String[]{null, "romanized", "anglicized", "cyrillized",
		"francized", "gairaigized", "latinized"});

	private String filterReferenceTable;
	private int filterReferenceID;
	private String filterReferenceType;

	private boolean simplePrimaryText;


	public static LocalizedTextDialog createRecordOnlyComplexText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.selectRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedTextDialog createComplexText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedTextDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.simplePrimaryText = true;
		dialog.hideUnselectButton = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedTextDialog createRecordOnlySimpleText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.selectRecordOnly = true;
		dialog.simplePrimaryText = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedTextDialog createSimpleText(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final LocalizedTextDialog dialog = new LocalizedTextDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.simplePrimaryText = true;
		dialog.initialize();
		return dialog;
	}


	private LocalizedTextDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public LocalizedTextDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		Consumer<Map<String, Object>> innerOnCloseGracefully = record -> {
			final NavigableMap<Integer, Map<String, Object>> mediaJunctions = getRecords(EntityManager.TABLE_NAME_LOCALIZED_TEXT_JUNCTION);
			final int mediaJunctionID = extractNextRecordID(mediaJunctions);
			if(selectedRecord != null){
				final Integer localizedTextID = extractRecordID(selectedRecord);
				final Map<String, Object> mediaJunction = new HashMap<>();
				insertRecordID(mediaJunction, mediaJunctionID);
				insertRecordLocalizedTextID(mediaJunction, localizedTextID);
				insertRecordReferenceTable(mediaJunction, filterReferenceTable);
				insertRecordReferenceID(mediaJunction, filterReferenceID);
				insertRecordReferenceType(mediaJunction, filterReferenceType);
				mediaJunctions.put(mediaJunctionID, mediaJunction);
			}
			else
				mediaJunctions.remove(mediaJunctionID);
		};
		if(onCloseGracefully != null)
			innerOnCloseGracefully = innerOnCloseGracefully.andThen(onCloseGracefully);

		setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public LocalizedTextDialog withReference(final String referenceTable, final int referenceID, final String referenceType){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;
		filterReferenceType = referenceType;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	@Override
	protected String getTableName(){
		return EntityManager.TABLE_NAME_LOCALIZED_TEXT;
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
		if(simplePrimaryText){
			GUIHelper.bindLabelTextChangeUndo(textLabel, simpleTextField, this::saveData);
			addMandatoryField(simpleTextField);
		}
		else{
			GUIHelper.bindLabelTextChange(textLabel, textTextPreview, this::saveData);
			textTextPreview.setTextViewFont(textLabel.getFont());
			textTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);
			textTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);
		}

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, referenceTypeComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionLabel, transcriptionComboBox, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(transcriptionTypeLabel, transcriptionTypeComboBox, this::saveData);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(textLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add((simplePrimaryText? simpleTextField: textTextPreview), "grow,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "grow,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(referenceTypeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionComboBox, "grow,wrap related");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "grow");

		recordTabbedPane.add("base", recordPanelBase);
	}

	@Override
	public void loadData(){
		unselectAction();

		final Map<Integer, Map<String, Object>> records = new HashMap<>(getRecords(EntityManager.TABLE_NAME_LOCALIZED_TEXT));
		if(filterReferenceTable != null){
			final Set<Integer> filteredMedia = getFilteredRecords(EntityManager.TABLE_NAME_LOCALIZED_TEXT_JUNCTION, filterReferenceTable,
					filterReferenceID)
				.values().stream()
				.filter(record -> filterReferenceType.equals(extractRecordReferenceType(record)))
				.map(EntityManager::extractRecordReferenceID)
				.collect(Collectors.toSet());
			records.keySet()
				.removeIf(mediaID -> !filteredMedia.contains(mediaID));
		}

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String primaryName = extractRecordText(container);
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

		if(selectRecordOnly)
			selectFirstData();
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		textTextPreview.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer localizedTextID = extractRecordID(selectedRecord);
		final String text = extractRecordText(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordReferenceType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		if(simplePrimaryText)
			simpleTextField.setText(text);
		else
			textTextPreview.setText("Extract " + localizedTextID, text, locale);
		localeField.setText(locale);
		referenceTypeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);

		if(filterReferenceTable == null){
			final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(EntityManager.TABLE_NAME_LOCALIZED_TEXT_JUNCTION,
				EntityManager::extractRecordLocalizedTextID, localizedTextID);
			if(recordMediaJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");
		}
	}

	@Override
	protected void clearData(){
		if(simplePrimaryText)
			simpleTextField.setText(null);
		else
			textTextPreview.clear();

		localeField.setText(null);

		referenceTypeComboBox.setSelectedItem(null);
		transcriptionComboBox.setSelectedItem(null);
		transcriptionTypeComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String primaryText = (simplePrimaryText? GUIHelper.getTextTrimmed(simpleTextField): textTextPreview.getTextTrimmed());
		if(!validData(primaryText)){
			JOptionPane.showMessageDialog(getParent(), "Text field is required", "Error", JOptionPane.ERROR_MESSAGE);
			if(simplePrimaryText)
				simpleTextField.requestFocusInWindow();
			else
				textTextPreview.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String text = (simplePrimaryText? GUIHelper.getTextTrimmed(simpleTextField): textTextPreview.getTextTrimmed());
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final String referenceType = GUIHelper.getTextTrimmed(referenceTypeComboBox);
		final String transcription = GUIHelper.getTextTrimmed(transcriptionComboBox);
		final String transcriptionType = GUIHelper.getTextTrimmed(transcriptionTypeComboBox);

		//update table:
		if(!Objects.equals(text, extractRecordText(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					model.setValueAt(text, modelRowIndex, TABLE_INDEX_TEXT);

					break;
				}
			}
		}

		insertRecordText(selectedRecord, text);
		insertRecordLocale(selectedRecord, locale);
		insertRecordReferenceType(selectedRecord, referenceType);
		insertRecordTranscription(selectedRecord, transcription);
		insertRecordTranscriptionType(selectedRecord, transcriptionType);

		return true;
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

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "text 1");
		localizedText1.put("locale", "en");
		localizedText1.put("type", "original");
		localizedText1.put("transcription", "IPA");
		localizedText1.put("transcription_type", "romanized");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);

		final TreeMap<Integer, Map<String, Object>> localizedTextJunctions = new TreeMap<>();
		store.put("localized_text_junction", localizedTextJunctions);
		final Map<String, Object> localizedTextJunction1 = new HashMap<>();
		localizedTextJunction1.put("id", 1);
		localizedTextJunction1.put("localized_text_id", 1);
		localizedTextJunction1.put("reference_table", "citation");
		localizedTextJunction1.put("reference_id", 1);
		localizedTextJunction1.put("reference_type", "extract");
		localizedTextJunctions.put((Integer)localizedTextJunction1.get("id"), localizedTextJunction1);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final LocalizedTextDialog dialog = createComplexText(store, parent)
				.withReference(EntityManager.TABLE_NAME_CITATION, 1, EntityManager.LOCALIZED_TEXT_TYPE_EXTRACT);
//			final LocalizedTextDialog dialog = createSimpleText(store, parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(localizedText1)))
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
					final int localizedTextID = extractRecordID(container);
					switch(editCommand.getType()){
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(store, parent)
								: NoteDialog.createModificationNoteEditOnly(store, parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + localizedTextID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(store, parent)
								: ResearchStatusDialog.createEditOnly(store, parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + localizedTextID);
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
					System.out.println(store);
					System.exit(0);
				}
			});
			//with secondary
			dialog.showDialog();
		});
	}

}
