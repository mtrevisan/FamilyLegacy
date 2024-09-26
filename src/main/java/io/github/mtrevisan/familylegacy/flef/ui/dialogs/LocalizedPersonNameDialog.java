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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.FilterString;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.BiConsumer;

import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordTranscriptionType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.extractRecordType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordFamilyName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordPersonalName;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordTranscription;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordTranscriptionType;
import static io.github.mtrevisan.familylegacy.flef.persistence.db.EntityManager.insertRecordType;


public final class LocalizedPersonNameDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 6171448434725755800L;

	private static final int TABLE_INDEX_TEXT = 2;


	private final JLabel personalNameLabel = new JLabel("(Primary) Name:");
	private final JTextField personalNameField = new JTextField();
	private final JLabel familyNameLabel = new JLabel("(Secondary) Name:");
	private final JTextField familyNameField = new JTextField();
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JComboBox<String> typeComboBox = new JComboBox<>(new String[]{null, "original", "transliteration", "translation"});
	private final JLabel transcriptionLabel = new JLabel("Transcription:");
	private final JComboBox<String> transcriptionComboBox = new JComboBox<>(new String[]{null, "IPA", "Wade-Giles", "hanyu pinyin",
		"wāpuro rōmaji", "kana", "hangul"});
	private final JLabel transcriptionTypeLabel = new JLabel("Transcription type:");
	private final JComboBox<String> transcriptionTypeComboBox = new JComboBox<>(new String[]{null, "romanized", "anglicized", "cyrillized",
		"francized", "gairaigized", "latinized"});

	private int filterReferenceID;


	public static LocalizedPersonNameDialog create(final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(parent);
		dialog.initialize();
		return dialog;
	}

	public static LocalizedPersonNameDialog createSelectOnly(final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.initialize();
		return dialog;
	}

	public static LocalizedPersonNameDialog createRecordOnly(final Frame parent){
		final LocalizedPersonNameDialog dialog = new LocalizedPersonNameDialog(parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}


	private LocalizedPersonNameDialog(final Frame parent){
		super(parent);
	}


	public LocalizedPersonNameDialog withOnCloseGracefully(final BiConsumer<Map<String, Object>, Integer> onCloseGracefully){
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
		return EntityManager.NODE_LOCALIZED_PERSON_NAME;
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
		GUIHelper.bindLabelUndo(personalNameLabel, personalNameField);
		GUIHelper.bindLabelUndo(familyNameLabel, familyNameField);
		addMandatoryField(personalNameField, familyNameField);

		GUIHelper.bindLabelUndo(localeLabel, localeField);

		GUIHelper.bindLabelUndoAutoComplete(typeLabel, typeComboBox);

		GUIHelper.bindLabelUndoAutoComplete(transcriptionLabel, transcriptionComboBox);

		GUIHelper.bindLabelUndoAutoComplete(transcriptionTypeLabel, transcriptionTypeComboBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(personalNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(personalNameField, "grow,wrap related");
		recordPanelBase.add(familyNameLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(familyNameField, "grow,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "grow,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "grow,wrap paragraph");
		recordPanelBase.add(transcriptionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionComboBox, "grow,wrap related");
		recordPanelBase.add(transcriptionTypeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(transcriptionTypeComboBox, "grow");

		recordTabbedPane.add("base", recordPanelBase);
	}

	@Override
	public void loadData(){
		unselectAction();

		final List<Map<String, Object>> records = (filterReferenceID <= 0
			? Repository.findAll(EntityManager.NODE_LOCALIZED_PERSON_NAME)
			: Repository.findReferencingNodes(EntityManager.NODE_NOTE,
				EntityManager.NODE_PERSON, filterReferenceID,
				EntityManager.RELATIONSHIP_FOR));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map<String, Object> record : records){
			final Integer recordID = extractRecordID(record);
			final String primaryName = extractRecordPersonalName(record);
			final String secondaryName = extractRecordFamilyName(record);
			final StringJoiner identifier = new StringJoiner(", ");
			if(primaryName != null)
				identifier.add(primaryName);
			if(secondaryName != null)
				identifier.add(secondaryName);
			final FilterString filter = FilterString.create()
				.add(recordID)
				.add(identifier);
			final String filterData = filter.toString();

			model.setValueAt(recordID, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(identifier.toString(), row, TABLE_INDEX_TEXT);

			row ++;
		}
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		personalNameField.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer localizedPersonNameID = extractRecordID(selectedRecord);
		final String personalName = extractRecordPersonalName(selectedRecord);
		final String familyName = extractRecordFamilyName(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String transcription = extractRecordTranscription(selectedRecord);
		final String transcriptionType = extractRecordTranscriptionType(selectedRecord);

		personalNameField.setText(personalName);
		familyNameField.setText(familyName);
		localeField.setText(locale);
		typeComboBox.setSelectedItem(type);
		transcriptionComboBox.setSelectedItem(transcription);
		transcriptionTypeComboBox.setSelectedItem(transcriptionType);

		if(filterReferenceID > 0){
			final List<Map<String, Object>> recordPersonNameRelationships = Repository.findRelationships(EntityManager.NODE_LOCALIZED_PERSON_NAME,
				localizedPersonNameID,
				EntityManager.NODE_PERSON_NAME, filterReferenceID,
				EntityManager.RELATIONSHIP_TRANSCRIPTION_FOR);
			if(recordPersonNameRelationships.size() > 1)
				throw new IllegalArgumentException("Data integrity error");
		}
	}

	@Override
	protected void clearData(){
		personalNameField.setText(null);
		familyNameField.setText(null);

		localeField.setText(null);

		typeComboBox.setSelectedItem(null);
		transcriptionComboBox.setSelectedItem(null);
		transcriptionTypeComboBox.setSelectedItem(null);
	}

	@Override
	protected boolean validateData(){
		final String personName = GUIHelper.getTextTrimmed(personalNameField);
		final String familyName = GUIHelper.getTextTrimmed(familyNameField);
		if(!validData(personName) && !validData(familyName)){
			JOptionPane.showMessageDialog(getParent(), "(Primary or secondary) Name field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			personalNameField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String personalName = GUIHelper.getTextTrimmed(personalNameField);
		final String familyName = GUIHelper.getTextTrimmed(familyNameField);
		final String locale = GUIHelper.getTextTrimmed(localeField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String transcription = GUIHelper.getTextTrimmed(transcriptionComboBox);
		final String transcriptionType = GUIHelper.getTextTrimmed(transcriptionTypeComboBox);

		//update table:
		final boolean shouldUpdate = (!Objects.equals(personalName, extractRecordPersonalName(selectedRecord))
			|| !Objects.equals(familyName, extractRecordFamilyName(selectedRecord)));
		if(shouldUpdate){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++){
				final int viewRowIndex = recordTable.convertRowIndexToView(row);
				final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

				if(model.getValueAt(modelRowIndex, TABLE_INDEX_ID).equals(recordID)){
					final StringJoiner text = new StringJoiner(", ");
					if(personalName != null)
						text.add(personalName);
					if(familyName != null)
						text.add(familyName);

					model.setValueAt(text.toString(), modelRowIndex, TABLE_INDEX_TEXT);

					break;
				}
			}
		}

		insertRecordPersonalName(selectedRecord, personalName);
		insertRecordFamilyName(selectedRecord, familyName);
		insertRecordLocale(selectedRecord, locale);
		insertRecordType(selectedRecord, type);
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


		GraphDatabaseManager.clearDatabase();

		final Map<String, Object> localizedPersonName1 = new HashMap<>();
		localizedPersonName1.put("personal_name", "personal name");
		localizedPersonName1.put("family_name", "family name");
		localizedPersonName1.put("locale", "en");
		localizedPersonName1.put("type", "original");
		localizedPersonName1.put("transcription", "IPA");
		localizedPersonName1.put("transcription_type", "romanized");
		Repository.upsert(localizedPersonName1, EntityManager.NODE_LOCALIZED_PERSON_NAME);


		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final LocalizedPersonNameDialog dialog = create(parent);
//			final LocalizedPersonNameDialog dialog = createRecordOnly(parent);
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
							final Integer noteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(parent)
								: NoteDialog.createModificationNoteEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + localizedPersonNameID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(noteID);

							changeNoteDialog.showDialog();
						}
						case RESEARCH_STATUS -> {
							final String tableName = editCommand.getIdentifier();
							final Integer researchStatusID = (Integer)container.get("researchStatusID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final ResearchStatusDialog researchStatusDialog = (showOnly
								? ResearchStatusDialog.createShowOnly(parent)
								: ResearchStatusDialog.createEditOnly(parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + localizedPersonNameID);
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
					System.out.println(Repository.logDatabase());

					System.exit(0);
				}
			});
			//with secondary
			dialog.showDialog();
		});
	}

}
