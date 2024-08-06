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

import io.github.mtrevisan.familylegacy.flef.helpers.FileHelper;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
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
import java.util.TreeMap;
import java.util.function.Consumer;


public final class NoteDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 3280504923967901715L;

	private static final int TABLE_INDEX_RECORD_NOTE = 2;

	private static final String TABLE_NAME = "note";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";


	private JLabel noteLabel;
	private TextPreviewPane noteTextPreview;
	private JLabel localeLabel;
	private JTextField localeField;

	private JButton mediaButton;
	private JButton culturalNormButton;
	private JCheckBox restrictionCheckBox;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static NoteDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new NoteDialog(store, parent);
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

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Date"};
	}

	@Override
	protected void initStoreComponents(){
		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_NOTE, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		noteLabel = new JLabel("Note:");
		noteTextPreview = TextPreviewPane.createWithPreview(this);
		noteTextPreview.setTextViewFont(noteLabel.getFont());
		localeLabel = new JLabel("Locale:");
		localeField = new JTextField();

		mediaButton = new JButton("Medias", ICON_MEDIA);
		culturalNormButton = new JButton("Cultural norms", ICON_CULTURAL_NORM);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChange(noteLabel, noteTextPreview, this::saveData);
		noteTextPreview.addValidDataListener(this, MANDATORY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChangeUndo(localeLabel, localeField, this::saveData);

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.MEDIA, TABLE_NAME, getSelectedRecord())));

		culturalNormButton.setToolTipText("Cultural norm");
		culturalNormButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.CULTURAL_NORM, TABLE_NAME, getSelectedRecord())));

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
		final Map<Integer, Map<String, Object>> records = (filterReferenceTable == null
			? getRecords(TABLE_NAME)
			: getFilteredRecords(TABLE_NAME, filterReferenceTable, filterReferenceID));

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordNote(container);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_NOTE);

			row ++;
		}
	}

	//FIXME filter table
//	@Override
//	protected void filterTableBy(final JDialog panel){
//		final String title = GUIHelper.getTextTrimmed(filterField);
//		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
//			TABLE_INDEX_RECORD_NOTE);
//
//		@SuppressWarnings("unchecked")
//		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
//		sorter.setRowFilter(filter);
//	}

	@Override
	protected void fillData(){
		final String note = extractRecordNote(selectedRecord);
		final String locale = extractRecordLocale(selectedRecord);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		noteTextPreview.setText("Note " + extractRecordID(selectedRecord), note, locale);
		localeField.setText(locale);

		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(culturalNormButton, !recordCulturalNormJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
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
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);

					model.setValueAt(note, modelRowIndex, TABLE_INDEX_RECORD_NOTE);

					break;
				}
		}

		selectedRecord.put("note", note);
		selectedRecord.put("locale", locale);

		return true;
	}


	private static String extractRecordNote(final Map<String, Object> record){
		return (String)record.get("note");
	}

	private static String extractRecordLocale(final Map<String, Object> record){
		return (String)record.get("locale");
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
			dialog.initComponents();
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
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", noteID);
									}
								});
							culturalNormDialog.initComponents();
							culturalNormDialog.loadData();

							culturalNormDialog.setLocationRelativeTo(dialog);
							culturalNormDialog.setVisible(true);
						}
						case MEDIA -> {
							final MediaDialog mediaDialog = MediaDialog.createForMedia(store, parent)
								.withBasePath(FileHelper.documentsDirectory())
								.withReference(TABLE_NAME, noteID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", noteID);
									}
								});
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
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
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
