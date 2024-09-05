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

import io.github.mtrevisan.familylegacy.flef.db.EntityManager;
import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
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

import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.util.TreeMap;
import java.util.function.Consumer;

import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordNote;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.extractRecordReferenceTable;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordLocale;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordNote;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceID;
import static io.github.mtrevisan.familylegacy.flef.db.EntityManager.insertRecordReferenceTable;


public final class NoteDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 3280504923967901715L;

	private static final int TABLE_INDEX_NOTE = 2;

	private static final String TABLE_NAME = "note";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";


	private final JLabel noteLabel = new JLabel("Note:");
	private final TextPreviewPane noteTextPreview = TextPreviewPane.createWithPreview(NoteDialog.this);
	private final JLabel localeLabel = new JLabel("Locale:");
	private final JTextField localeField = new JTextField();

	private final JButton mediaButton = new JButton("Media", ICON_MEDIA);
	private final JButton culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private String filterReferenceTable;
	private int filterReferenceID;


	public static NoteDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.initialize();
		return dialog;
	}

	public static NoteDialog createSelectOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.hideUnselectButton = true;
		dialog.addViewOnlyComponents(dialog.mediaButton, dialog.culturalNormButton);
		dialog.initialize();
		return dialog;
	}

	public static NoteDialog createShowOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static NoteDialog createEditOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.initialize();
		return dialog;
	}

	public static NoteDialog createModificationNoteShowOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.selectRecordOnly = true;
		dialog.showRecordOnly = true;
		dialog.showRecordHistory = false;
		dialog.initialize();
		return dialog;
	}

	public static NoteDialog createModificationNoteEditOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.showRecordOnly = true;
		dialog.showRecordHistory = false;
		dialog.initialize();
		return dialog;
	}


	private NoteDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public NoteDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public NoteDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Note"};
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
		GUIHelper.bindLabelTextChange(noteLabel, noteTextPreview, this::saveData);
		noteTextPreview.setTextViewFont(noteLabel.getFont());
		noteTextPreview.setMinimumSize(MINIMUM_NOTE_TEXT_PREVIEW_SIZE);
		noteTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, selectedRecord)));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, TABLE_NAME, selectedRecord)));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(noteLabel, "align label,top,sizegroup lbl,split 2");
		recordPanelBase.add(noteTextPreview, "grow,wrap related");
		recordPanelBase.add(localeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(localeField, "grow");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(mediaButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(culturalNormButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	public void loadData(){
		unselectAction();

		final Map<Integer, Map<String, Object>> records = (filterReferenceTable == null
			? getRecords(TABLE_NAME)
			: getFilteredRecords(TABLE_NAME, filterReferenceTable, filterReferenceID));

		final DefaultTableModel model = getRecordTableModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String note = extractRecordNote(container);
			final FilterString filter = FilterString.create()
				.add(key)
				.add(note);
			final String filterData = filter.toString();

			model.setValueAt(key, row, TABLE_INDEX_ID);
			model.setValueAt(filterData, row, TABLE_INDEX_FILTER);
			model.setValueAt(note, row, TABLE_INDEX_NOTE);

			row ++;
		}

		if(selectRecordOnly)
			selectFirstData();
	}

	@Override
	protected void requestFocusAfterSelect(){
		//set focus on first field
		noteTextPreview.requestFocusInWindow();
	}

	@Override
	protected void fillData(){
		final Integer noteID = extractRecordID(selectedRecord);
		final String note = extractRecordNote(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final boolean hasMedia = (getRecords(TABLE_NAME_MEDIA_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(noteID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final boolean hasCulturalNorms = (getRecords(TABLE_NAME_CULTURAL_NORM_JUNCTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(noteID, extractRecordReferenceID(record)))
			.findFirst()
			.orElse(null) != null);
		final String restriction = getRecords(TABLE_NAME_RESTRICTION)
			.values().stream()
			.filter(record -> Objects.equals(TABLE_NAME, extractRecordReferenceTable(record)))
			.filter(record -> Objects.equals(noteID, extractRecordReferenceID(record)))
			.findFirst()
			.map(EntityManager::extractRecordRestriction)
			.orElse(null);

		noteTextPreview.setText("Note " + noteID, note, locale);
		localeField.setText(locale);

		setButtonEnableAndBorder(mediaButton, hasMedia);
		setButtonEnableAndBorder(culturalNormButton, hasCulturalNorms);
		setCheckBoxEnableAndBorder(restrictionCheckBox, EntityManager.RESTRICTION_CONFIDENTIAL.equals(restriction));
	}

	@Override
	protected void clearData(){
		noteTextPreview.clear();
		localeField.setText(null);

		GUIHelper.setDefaultBorder(mediaButton);
		GUIHelper.setDefaultBorder(culturalNormButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(filterReferenceTable == null && !validData(noteTextPreview.getTextTrimmed())){
			JOptionPane.showMessageDialog(getParent(), "Note field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			noteTextPreview.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String note = noteTextPreview.getTextTrimmed();
		final String locale = GUIHelper.getTextTrimmed(localeField);

		//update table:
		if(!Objects.equals(note, extractRecordNote(selectedRecord))){
			final DefaultTableModel model = getRecordTableModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(note, modelRowIndex, TABLE_INDEX_NOTE);

					break;
				}
		}

		insertRecordNote(selectedRecord, note);
		insertRecordLocale(selectedRecord, locale);

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

		final TreeMap<Integer, Map<String, Object>> notes = new TreeMap<>();
		store.put("note", notes);
		final Map<String, Object> note1 = new HashMap<>();
		note1.put("id", 1);
		note1.put("note", "note 1");
		note1.put("reference_table", "person");
		note1.put("reference_id", 1);
		notes.put((Integer)note1.get("id"), note1);
		final Map<String, Object> note2 = new HashMap<>();
		note2.put("id", 2);
		note2.put("note", "note 2");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 2);
		notes.put((Integer)note2.get("id"), note2);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put("restriction", restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final NoteDialog dialog = create(store, parent);
//			final NoteDialog dialog = createRecordOnly(store, parent);
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(note1)))
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
					final int noteID = extractRecordID(container);
					switch(editCommand.getType()){
						case CULTURAL_NORM -> {
							final CulturalNormDialog culturalNormDialog = CulturalNormDialog.create(store, parent)
								.withReference(TABLE_NAME, noteID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, noteID);
									}
								});
							culturalNormDialog.loadData();

							culturalNormDialog.showDialog();
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = (dialog.isViewOnlyComponent(dialog.mediaButton)
									? MediaDialog.createSelectOnlyForMedia(store, parent)
									: MediaDialog.createForMedia(store, parent))
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, noteID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										insertRecordReferenceTable(record, TABLE_NAME);
										insertRecordReferenceID(record, noteID);
									}
								});
							mediaDialog.loadData();

							mediaDialog.showDialog();
						}
						case MODIFICATION_HISTORY -> {
							final String tableName = editCommand.getIdentifier();
							final Integer modificationNoteID = (Integer)container.get("noteID");
							final Boolean showOnly = (Boolean)container.get("showOnly");
							final NoteDialog changeNoteDialog = (showOnly
								? NoteDialog.createModificationNoteShowOnly(store, parent)
								: NoteDialog.createModificationNoteEditOnly(store, parent));
							final String title = StringUtils.capitalize(StringUtils.replace(tableName, "_", StringUtils.SPACE));
							changeNoteDialog.setTitle((showOnly? "Show": "Edit") + " modification note for " + title + " " + noteID);
							changeNoteDialog.loadData();
							changeNoteDialog.selectData(modificationNoteID);

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
							researchStatusDialog.setTitle((showOnly? "Show": "Edit") + " research status for " + title + " " + noteID);
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
			dialog.showDialog();
		});
	}

}
