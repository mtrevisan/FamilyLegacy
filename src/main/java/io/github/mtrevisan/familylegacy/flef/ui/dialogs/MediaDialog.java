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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ImagePreview;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class MediaDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -800755271311929604L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "media";


	private JLabel fileLabel;
	private JTextField fileField;
	private JButton fileButton;
	private JFileChooser fileChooser;
	private JLabel titleLabel;
	private JTextField titleField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel photoProjectionLabel;
	private JComboBox<String> photoProjectionComboBox;
	private JButton dateButton;

	private JButton noteButton;
	private JCheckBox restrictionCheckBox;

	private JButton referenceButton;
	private JButton photoCropButton;

	private String filterReferenceTable;
	private int filterReferenceID;
	private String basePath;

	private final Tika tika = new Tika();


	public static MediaDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new MediaDialog(store, parent);
	}

	public static MediaDialog createWithReferenceTable(final Map<String, TreeMap<Integer, Map<String, Object>>> store,
			final String referenceTable, final int filterReferenceID, final Frame parent){
		final MediaDialog dialog = new MediaDialog(store, parent);
		dialog.filterReferenceTable = referenceTable;
		dialog.filterReferenceID = filterReferenceID;
		return dialog;
	}


	private MediaDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public MediaDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
		super.setOnCloseGracefully(onCloseGracefully);

		return this;
	}

	public MediaDialog withBasePath(final String basePath){
		this.basePath = basePath;

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
		setTitle("Medias"
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		fileLabel = new JLabel("Identifier:");
		fileField = new JTextField();
		fileButton = new JButton(ICON_OPEN_DOCUMENT);
		fileChooser = new JFileChooser();
		titleLabel = new JLabel("Title:");
		titleField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"photo", "audio", "video", "home movie", "newsreel",
			"microfilm", "microfiche", "cd-rom"});
		photoProjectionLabel = new JLabel("Photo projection:");
		photoProjectionComboBox = new JComboBox<>(new String[]{"rectangular", "spherical UV",
			"cylindrical equirectangular horizontal", "cylindrical equirectangular vertical"});
		dateButton = new JButton("Date", ICON_CALENDAR);

		noteButton = new JButton("Notes", ICON_NOTE);
		restrictionCheckBox = new JCheckBox("Confidential");

		referenceButton = new JButton("Reference", ICON_REFERENCE);
		photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

		GUIHelper.bindLabelTextChangeUndo(fileLabel, fileField, evt -> {
			final String identifier = GUIHelper.readTextTrimmed(fileField);
			final String photoCrop = extractRecordPhotoCrop(selectedRecord);
			enablePhotoCropButton(identifier, photoCrop);

			saveData();
		});
		GUIHelper.setBackgroundColor(fileField, MANDATORY_FIELD_BACKGROUND_COLOR);

		fileButton.addActionListener(evt -> {
			final int returnValue = fileChooser.showDialog(this, "Choose file");
			if(returnValue == JFileChooser.APPROVE_OPTION){
				final File selectedFile = fileChooser.getSelectedFile();
				final String path = selectedFile.getPath();
				fileField.setText(stripBasePath(path));

				try{
					final String mimeType = tika.detect(selectedFile);
					if(mimeType != null && mimeType.startsWith("image/")){
						//TODO infer projection type (see CylindricalProjectionTypeDetector and ImageProjectionTypeDetector)
//						final String projection = "...";
//						photoProjectionComboBox.setSelectedItem(projection);
					}
				}
				catch(final IOException ignored){}
			}
		});
		fileChooser.setAccessory(new ImagePreview(fileChooser, 150, 100));

		GUIHelper.bindLabelTextChangeUndo(titleLabel, titleField, evt -> saveData());

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(photoProjectionLabel, photoProjectionComboBox, evt -> saveData(),
			evt -> saveData());

		dateButton.setToolTipText("Date");
		dateButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.DATE, getSelectedRecord())));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NOTE, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		if(filterReferenceTable == null){
			referenceButton.setToolTipText("Reference");
			referenceButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.REFERENCE, getSelectedRecord())
				.withOnCloseGracefully(container -> {
					//TODO save data
				})));
			GUIHelper.addBorder(referenceButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);
		}

		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.PHOTO_CROP, getSelectedRecord())));
		photoCropButton.setEnabled(false);
	}

	private String stripBasePath(final String absolutePath){
		return StringUtils.removeStart(absolutePath, basePath);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		referenceButton.setVisible(filterReferenceTable == null);

		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(fileLabel, "align label,sizegroup label,split 3");
		recordPanelBase.add(fileField, "grow");
		recordPanelBase.add(fileButton, "wrap paragraph");
		recordPanelBase.add(titleLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(titleField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "wrap");
		recordPanelBase.add(photoProjectionLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(photoProjectionComboBox, "grow,wrap paragraph");
		recordPanelBase.add(dateButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelLink.add(referenceButton, "sizegroup btn,center,wrap paragraph,hidemode 3");
		recordPanelLink.add(photoCropButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("link", recordPanelLink);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		if(filterReferenceTable != null){
			final Set<Integer> filteredMedias = getFilteredRecords(TABLE_NAME_MEDIA_JUNCTION, filterReferenceTable, filterReferenceID)
				.values().stream()
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
			final String identifier = extractRecordIdentifier(record.getValue());

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	@Override
	protected void filterTableBy(final JDialog panel){
		final String title = GUIHelper.readTextTrimmed(filterField);
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_IDENTIFIER);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		sorter.setRowFilter(filter);
	}

	@Override
	protected void fillData(){
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String title = extractRecordTitle(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String photoProjection = extractRecordPhotoProjection(selectedRecord);
		final Integer dateID = extractRecordDateID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		fileField.setText(identifier);
		titleField.setText(title);
		typeComboBox.setSelectedItem(type);
		photoProjectionComboBox.setSelectedItem(photoProjection);
		GUIHelper.addBorder(dateButton, dateID != null, DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		if(filterReferenceTable == null){
			final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION,
				MediaDialog::extractRecordMediaID, extractRecordID(selectedRecord));
			if(recordMediaJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			final Map<String, Object> record = recordMediaJunction.values().stream()
				.findFirst()
				.orElse(Collections.emptyMap());
			final String photoCrop = extractRecordPhotoCrop(record);

			GUIHelper.addBorder(referenceButton, (!recordMediaJunction.isEmpty()? DATA_BUTTON_BORDER_COLOR: MANDATORY_COMBOBOX_BACKGROUND_COLOR));
			enablePhotoCropButton(identifier, photoCrop);
		}
	}

	private void enablePhotoCropButton(final String fileURI, final String photoCrop){
		photoCropButton.setEnabled(true);
		final String mimeType = tika.detect(fileURI);
		if(mimeType == null || !mimeType.startsWith("image/"))
			photoCropButton.setEnabled(false);
		else
			GUIHelper.addBorder(photoCropButton, photoCrop != null && !photoCrop.isEmpty(), DATA_BUTTON_BORDER_COLOR);
	}

	@Override
	protected void clearData(){
		fileField.setText(null);
		titleField.setText(null);
		GUIHelper.setBackgroundColor(fileField, Color.WHITE);
		typeComboBox.setSelectedItem(null);
		photoProjectionComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(dateButton);

		GUIHelper.setDefaultBorder(noteButton);
		restrictionCheckBox.setSelected(false);

		if(filterReferenceTable == null)
			GUIHelper.setDefaultBorder(referenceButton);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.readTextTrimmed(fileField);
		if(filterReferenceTable == null && !validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			fileField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String identifier = GUIHelper.readTextTrimmed(fileField);
		final String title = GUIHelper.readTextTrimmed(titleField);
		final String type = (String)typeComboBox.getSelectedItem();
		final String photoProjection = (String)photoProjectionComboBox.getSelectedItem();

		//update table
		if(!Objects.equals(identifier, extractRecordIdentifier(selectedRecord))){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			final Integer recordID = extractRecordID(selectedRecord);
			for(int row = 0, length = model.getRowCount(); row < length; row ++)
				if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
					final int viewRowIndex = recordTable.convertRowIndexToView(row);
					final int modelRowIndex = recordTable.convertRowIndexToModel(viewRowIndex);
					model.setValueAt(identifier, modelRowIndex, TABLE_INDEX_RECORD_IDENTIFIER);
					break;
				}
		}

		if(filterReferenceTable != null){
			//TODO upsert junction
		}

		selectedRecord.put("identifier", identifier);
		selectedRecord.put("title", title);
		selectedRecord.put("type", type);
		selectedRecord.put("photo_projection", photoProjection);
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordTitle(final Map<String, Object> record){
		return (String)record.get("title");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordPhotoProjection(final Map<String, Object> record){
		return (String)record.get("photo_projection");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordMediaID(final Map<String, Object> record){
		return (Integer)record.get("media_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (String)record.get("photo_crop");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -35701794732758533L;


		RecordTableModel(){
			super(new String[]{"ID", "Identifier"}, 0);
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

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put(TABLE_NAME, medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		medias.put((Integer)media1.get("id"), media1);

		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();
		store.put(TABLE_NAME_MEDIA_JUNCTION, mediaJunctions);
		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("id", 1);
		mediaJunction1.put("media_id", 1);
		mediaJunction1.put("reference_table", "media");
		mediaJunction1.put("reference_id", 1);
		mediaJunction1.put("photo_crop", "0 0 10 50");
		mediaJunctions.put((Integer)mediaJunction1.get("id"), mediaJunction1);

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

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put(TABLE_NAME_RESTRICTION, restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final MediaDialog dialog = create(store, parent).withBasePath("\\Documents\\");
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(mediaJunction1)))
				dialog.showNewRecord();

			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case DATE -> {
							//TODO single media
						}
						case NOTE -> {
							final int mediaID = extractRecordID(editCommand.getContainer());
							final NoteDialog noteDialog = NoteDialog.createWithReferenceTable(store, TABLE_NAME, mediaID, parent);
							noteDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 487);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case REFERENCE -> {
							//TODO single media
						}
						case PHOTO_CROP -> {
							//TODO single media
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(351, 460);
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
