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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.ImagePreview;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.StringHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public final class MediaDialog extends CommonListDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaDialog.class);


	@Serial
	private static final long serialVersionUID = -800755271311929604L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 2;

	private static final String TABLE_NAME = "media";
	private static final String TABLE_NAME_ASSERTION = "assertion";
	private static final String TABLE_NAME_EVENT = "event";

	private static final String MEDIA_TYPE_LINK = "link";
	private static final String MEDIA_TYPE_PHOTO = "photo";


	private JLabel fileLabel;
	private JTextField fileField;
	private JButton fileButton;
	private JFileChooser fileChooser;
	private JButton openFolderButton;
	private JButton openLinkButton;
	private JLabel titleLabel;
	private JTextField titleField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel photoProjectionLabel;
	private JComboBox<String> photoProjectionComboBox;
	private JButton dateButton;

	private JButton noteButton;
	private JButton assertionButton;
	private JButton eventButton;
	private JCheckBox restrictionCheckBox;

	private JButton photoCropButton;

	private String filterReferenceTable;
	private int filterReferenceID;

	private Path basePath;

	private boolean restrictToPhoto;
	private String mediaType = MEDIA_TYPE_LINK;


	public static MediaDialog createForMedia(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new MediaDialog(store, parent);
	}

	public static MediaDialog createForPhoto(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final MediaDialog dialog = new MediaDialog(store, parent);
		dialog.restrictToPhoto = true;
		dialog.mediaType = MEDIA_TYPE_PHOTO;
		dialog.setNewRecordDefault(newRecord -> {
			newRecord.put("type", "photo");

			dialog.typeComboBox.setEnabled(false);
		});
		return dialog;
	}

	public static MediaDialog createRecordOnly(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		final MediaDialog dialog = new MediaDialog(store, parent);
		dialog.showRecordOnly = true;
		return dialog;
	}


	private MediaDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public MediaDialog withOnCloseGracefully(final Consumer<Map<String, Object>> onCloseGracefully){
		final Consumer<Map<String, Object>> innerOnCloseGracefully = record -> {
			final NavigableMap<Integer, Map<String, Object>> mediaJunctions = getRecords(TABLE_NAME_MEDIA_JUNCTION);
			final int mediaJunctionID = extractNextRecordID(mediaJunctions);
			if(selectedRecord == null)
				mediaJunctions.remove(mediaJunctionID);
			else{
				final Integer mediaID = extractRecordID(selectedRecord);
				final Map<String, Object> mediaJunction = new HashMap<>();
				mediaJunction.put("id", mediaJunctionID);
				mediaJunction.put("media_id", mediaID);
				mediaJunction.put("reference_table", filterReferenceTable);
				mediaJunction.put("reference_id", filterReferenceID);
				mediaJunctions.put(mediaJunctionID, mediaJunction);

				if(onCloseGracefully != null)
					onCloseGracefully.accept(record);
			}
		};

		setOnCloseGracefully(innerOnCloseGracefully);

		return this;
	}

	public MediaDialog withReference(final String referenceTable, final int referenceID){
		filterReferenceTable = referenceTable;
		filterReferenceID = referenceID;

		return this;
	}

	public MediaDialog withBasePath(final Path basePath){
		this.basePath = basePath;

		return this;
	}

	@Override
	protected String getTableName(){
		return TABLE_NAME;
	}

	@Override
	protected String[] getTableColumnNames(){
		return new String[]{"ID", "Filter", "Identifier"};
	}

	@Override
	protected void initStoreComponents(){
		final String capitalizedPluralTableName = StringUtils.capitalize(StringHelper.pluralize(restrictToPhoto? "photo": getTableName()));
		setTitle(capitalizedPluralTableName
			+ (filterReferenceTable != null? " for " + filterReferenceTable + " ID " + filterReferenceID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		fileLabel = new JLabel("Identifier:");
		fileField = new JTextField();
		fileButton = new JButton(ICON_CHOOSE_DOCUMENT);
		fileChooser = new JFileChooser();
		openFolderButton = new JButton("Open folder", ICON_OPEN_FOLDER);
		openLinkButton = new JButton("Open " + mediaType, ICON_OPEN_LINK);
		titleLabel = new JLabel("Title:");
		titleField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{null, "photo", "audio", "video", "home movie", "newsreel", "microfilm", "microfiche",
			"cd-rom"});
		photoProjectionLabel = new JLabel("Photo projection:");
		photoProjectionComboBox = new JComboBox<>(new String[]{null, "rectangular", "spherical UV", "cylindrical equirectangular horizontal",
			"cylindrical equirectangular vertical"});
		dateButton = new JButton("Date", ICON_CALENDAR);

		noteButton = new JButton("Notes", ICON_NOTE);
		eventButton = new JButton("Events", ICON_EVENT);
		assertionButton = new JButton("Assertions", ICON_ASSERTION);
		restrictionCheckBox = new JCheckBox("Confidential");

		photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

		GUIHelper.bindLabelTextChangeUndo(fileLabel, fileField, () -> {
			String identifier = GUIHelper.getTextTrimmed(fileField);
			if(identifier != null && (identifier.charAt(0) == '/' || identifier.charAt(0) == '\\'))
				identifier = basePath + identifier;
			enablePhotoRelatedButtons(identifier);

			final Integer mediaID = extractRecordID(selectedRecord);
			photoCropButtonEnabledBorder(identifier, mediaID);

			saveData();
		});
		addMandatoryField(fileField);
		fileField.setEditable(false);

		fileButton.addActionListener(evt -> {
			FileNameExtensionFilter filter = null;
			if(restrictToPhoto){
				final String[] photoExtensions = supportedPhotoExtensions();
				filter = new FileNameExtensionFilter("Images", photoExtensions);
			}
			fileChooser.setFileFilter(filter);
			String identifier = extractRecordIdentifier(selectedRecord);
			if(identifier != null){
				if(identifier.startsWith("../") || identifier.startsWith("..\\") || identifier.charAt(0) == '/' || identifier.charAt(0) == '\\')
					identifier = FileHelper.getTargetPath(basePath, identifier);
				final File file = new File(identifier);
				fileChooser.setCurrentDirectory(file.getParentFile());
			}

			final int returnValue = fileChooser.showDialog(this, "Choose file");
			if(returnValue == JFileChooser.APPROVE_OPTION){
				final File selectedFile = fileChooser.getSelectedFile();
				identifier = FileHelper.getRelativePath(basePath, selectedFile.getPath());
				fileField.setText(identifier);


				final Integer recordID = extractRecordID(selectedRecord);
				final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
				for(int row = 0, length = model.getRowCount(); row < length; row ++)
					if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID)){
						model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);
						return;
					}
			}
		});
		fileChooser.setAccessory(new ImagePreview(fileChooser, 150, 100));

		openFolderButton.addActionListener(evt -> {
			File file = null;
			try{
				String identifier = GUIHelper.getTextTrimmed(fileField);
				if(identifier != null && (identifier.charAt(0) == '/' || identifier.charAt(0) == '\\'))
					identifier = basePath + identifier;
				file = FileHelper.loadFile(identifier);
				if(file != null)
					FileHelper.browse(file);
			}
			catch(final IOException | InterruptedException e){
				LOGGER.warn("Exception while opening folder {}", (file != null? file.getParent(): null), e);
			}
		});
		openLinkButton.addActionListener(evt -> {
			String identifier = GUIHelper.getTextTrimmed(fileField);
			File file = null;
			try{
				if(identifier != null && (identifier.charAt(0) == '/' || identifier.charAt(0) == '\\'))
					identifier = basePath + identifier;
				file = FileHelper.loadFile(identifier);
				if(file != null)
					FileHelper.openFileWithChosenEditor(file);
			}
			catch(final Exception e1){
				LOGGER.warn("Exception while opening file {}", file, e1);

				try{
					FileHelper.browseURL(identifier);
				}
				catch(final Exception e2){
					LOGGER.warn("Exception while browsing URL {}", identifier, e2);
				}
			}
		});

		GUIHelper.bindLabelTextChangeUndo(titleLabel, titleField, this::saveData);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, this::saveData);
		if(restrictToPhoto){
			typeComboBox.setSelectedItem("photo");
			typeComboBox.setEnabled(false);
		}

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(photoProjectionLabel, photoProjectionComboBox, this::saveData);

		dateButton.setToolTipText("Date");
		dateButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.HISTORIC_DATE, TABLE_NAME, getSelectedRecord())));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.NOTE, TABLE_NAME, getSelectedRecord())));

		assertionButton.setToolTipText("Assertions");
		assertionButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.ASSERTION, TABLE_NAME, getSelectedRecord())));

		eventButton.setToolTipText("Events");
		eventButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.EVENT, TABLE_NAME, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(
			EditEvent.create(EditEvent.EditType.PHOTO_CROP, TABLE_NAME, getSelectedRecord())));
		photoCropButton.setEnabled(false);
	}

	private static String[] supportedPhotoExtensions(){
		final Collection<String> supportedExtensions = new TreeSet<>();
		final String[] formatNames = ImageIO.getReaderFormatNames();
		for(int i = 0, formatNamesLength = formatNames.length; i < formatNamesLength; i ++){
			final Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(formatNames[i]);
			while(readers.hasNext()){
				final ImageReader reader = readers.next();

				final ImageReaderSpi spi = reader.getOriginatingProvider();
				final String[] suffixes = (spi != null? spi.getFileSuffixes(): null);
				for(int j = 0, suffixesLength = (suffixes != null? suffixes.length: 0); j < suffixesLength; j ++)
					supportedExtensions.add(suffixes[j].toLowerCase(Locale.ROOT));
			}
		}
		return supportedExtensions.toArray(String[]::new);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(fileLabel, "align label,sizegroup lbl,split 3");
		recordPanelBase.add(fileField, "grow");
		recordPanelBase.add(fileButton, "wrap related");
		recordPanelBase.add(openFolderButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(openLinkButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(titleLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(titleField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(typeComboBox, "wrap");
		recordPanelBase.add(photoProjectionLabel, "align label,sizegroup lbl,split 2");
		recordPanelBase.add(photoProjectionComboBox, "grow,wrap paragraph");
		recordPanelBase.add(dateButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(assertionButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(eventButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelLink.add(photoCropButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("link", recordPanelLink);
	}

	@Override
	public void loadData(){
		final Map<Integer, Map<String, Object>> records = new HashMap<>(getRecords(TABLE_NAME));
		if(filterReferenceTable != null){
			final Set<Integer> filteredMedias = getFilteredRecords(TABLE_NAME_MEDIA_JUNCTION, filterReferenceTable, filterReferenceID)
				.values().stream()
				.map(CommonRecordDialog::extractRecordID)
				.collect(Collectors.toSet());
			records.keySet()
				.removeIf(mediaID -> !filteredMedias.contains(mediaID));
		}
		if(restrictToPhoto)
			records.values()
				.removeIf(entry -> {
					String identifier = extractRecordIdentifier(entry);
					if(identifier != null && (identifier.startsWith("../") || identifier.startsWith("..\\") || identifier.charAt(0) == '/'
							|| identifier.charAt(0) == '\\'))
						identifier = FileHelper.getTargetPath(basePath, identifier);
					final File file = FileHelper.loadFile(identifier);
					return (file != null && (!file.exists() || !FileHelper.isPhoto(file)));
				});

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		model.setRowCount(records.size());
		int row = 0;
		for(final Map.Entry<Integer, Map<String, Object>> record : records.entrySet()){
			final Integer key = record.getKey();
			final Map<String, Object> container = record.getValue();

			final String identifier = extractRecordIdentifier(container);

			model.setValueAt(key, row, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, row, TABLE_INDEX_RECORD_IDENTIFIER);

			row ++;
		}
	}

	public void addData(final Map<String, Object> record){
		final Integer recordID = extractRecordID(record);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		for(int row = 0, length = model.getRowCount(); row < length; row ++)
			if(model.getValueAt(row, TABLE_INDEX_RECORD_ID).equals(recordID))
				return;

		final Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		if(records.containsKey(recordID)){
			final int oldSize = model.getRowCount();
			final String identifier = extractRecordIdentifier(record);
			model.setRowCount(oldSize + 1);
			model.setValueAt(recordID, oldSize, TABLE_INDEX_RECORD_ID);
			model.setValueAt(identifier, oldSize, TABLE_INDEX_RECORD_IDENTIFIER);
			//resort rows
			final RowSorter<? extends TableModel> recordTableSorter = recordTable.getRowSorter();
			recordTableSorter.setSortKeys(recordTableSorter.getSortKeys());
		}
	}

	//FIXME filter table
//	@Override
//	protected void filterTableBy(final JDialog panel){
//		final String title = GUIHelper.getTextTrimmed(filterField);
//		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
//			TABLE_INDEX_RECORD_IDENTIFIER);
//
//		@SuppressWarnings("unchecked")
//		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
//		sorter.setRowFilter(filter);
//	}

	@Override
	protected void fillData(){
		final Integer mediaID = extractRecordID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String title = extractRecordTitle(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String photoProjection = extractRecordPhotoProjection(selectedRecord);
		final Integer dateID = extractRecordDateID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordAssertions = getRecords(TABLE_NAME_ASSERTION)
			.entrySet().stream()
			.filter(entry -> Objects.equals(mediaID, extractRecordReferenceID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordEvents = getRecords(TABLE_NAME_EVENT)
			.entrySet().stream()
			.filter(entry -> Objects.equals(mediaID, extractRecordMediaID(entry.getValue())))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		fileField.setText(identifier);
		titleField.setText(title);
		typeComboBox.setSelectedItem(type);
		photoProjectionComboBox.setSelectedItem(photoProjection);
		GUIHelper.addBorder(dateButton, dateID != null, DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(assertionButton, !recordAssertions.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(eventButton, !recordEvents.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		enablePhotoRelatedButtons(identifier);


		photoCropButtonEnabledBorder(identifier, mediaID);
	}

	//NOTE working table-junction extraction
	private void photoCropButtonEnabledBorder(String identifier, final Integer mediaID){
		if(identifier != null && (identifier.charAt(0) == '/' || identifier.charAt(0) == '\\'))
			identifier = basePath + identifier;
		final File file = FileHelper.loadFile(identifier);
		final boolean isPhoto = (file != null && file.exists() && FileHelper.isPhoto(file));
		if(isPhoto){
			final Map<Integer, Map<String, Object>> recordMediaJunction = getFilteredRecords(TABLE_NAME_MEDIA_JUNCTION, filterReferenceTable,
				filterReferenceID)
				.entrySet().stream()
				.filter(entry -> Objects.equals(mediaID, extractRecordMediaID(entry.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));
			if(recordMediaJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			final Map<String, Object> mediaJunction = recordMediaJunction
				.values().stream()
				.findFirst().orElse(null);
			final String photoCrop = extractRecordPhotoCrop(mediaJunction);

			photoCropButton.setEnabled(true);
			GUIHelper.addBorder(photoCropButton, photoCrop != null && !photoCrop.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		}
		else{
			photoCropButton.setEnabled(false);
			GUIHelper.setDefaultBorder(photoCropButton);
		}
	}

	private void enablePhotoRelatedButtons(String identifier){
		openFolderButton.setEnabled(false);
		openLinkButton.setEnabled(false);
		photoProjectionComboBox.setEnabled(false);

		if(identifier == null)
			return;

		boolean enable = false;

		if(identifier.charAt(0) == '/' || identifier.charAt(0) == '\\')
			identifier = basePath + identifier;
		final File file = FileHelper.loadFile(identifier);
		if(file != null && file.exists()){
			openFolderButton.setEnabled(true);
			openLinkButton.setEnabled(true);

			enable = FileHelper.isPhoto(file);
		}

		photoProjectionComboBox.setEnabled(enable);
	}

	@Override
	protected void clearData(){
		fileField.setText(null);
		titleField.setText(null);
		typeComboBox.setSelectedItem(null);
		photoProjectionComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(dateButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(assertionButton);
		GUIHelper.setDefaultBorder(eventButton);
		restrictionCheckBox.setSelected(false);

		photoCropButton.setEnabled(false);
		GUIHelper.setDefaultBorder(photoCropButton);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.getTextTrimmed(fileField);
		if(!validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			fileField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected boolean saveData(){
		if(ignoreEvents || selectedRecord == null)
			return false;

		//read record panel:
		final String identifier = GUIHelper.getTextTrimmed(fileField);
		final String title = GUIHelper.getTextTrimmed(titleField);
		final String type = GUIHelper.getTextTrimmed(typeComboBox);
		final String photoProjection = GUIHelper.getTextTrimmed(photoProjectionComboBox);

		//update table:
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


		selectedRecord.put("identifier", FileHelper.getRelativePath(basePath, identifier));
		selectedRecord.put("title", title);
		selectedRecord.put("type", type);
		selectedRecord.put("photo_projection", photoProjection);

		return true;
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

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (record != null? (String)record.get("photo_crop"): null);
	}



	public static void main(final String[] args){
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Map<String, TreeMap<Integer, Map<String, Object>>> store = new HashMap<>();

		final TreeMap<Integer, Map<String, Object>> medias = new TreeMap<>();
		store.put("media", medias);
		final Map<String, Object> media1 = new HashMap<>();
		media1.put("id", 1);
		media1.put("identifier", "media 1");
		media1.put("title", "title 1");
		media1.put("type", "photo");
		media1.put("photo_projection", "rectangular");
		media1.put("date_id", 1);
		medias.put((Integer)media1.get("id"), media1);
		final Map<String, Object> media2 = new HashMap<>();
		media2.put("id", 2);
		media2.put("identifier", "https://www.google.com/");
		media2.put("title", "title 2");
		media2.put("type", "photo");
		media2.put("photo_projection", "rectangular");
		media2.put("date_id", 1);
		medias.put((Integer)media2.get("id"), media2);
		final Map<String, Object> media3 = new HashMap<>();
		media3.put("id", 3);
		media3.put("identifier", "/images/addPhoto.boy.jpg");
		media3.put("title", "title 3");
		media3.put("type", "photo");
		media3.put("photo_projection", "rectangular");
		media3.put("date_id", 1);
		medias.put((Integer)media3.get("id"), media3);

		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();
		store.put("media_junction", mediaJunctions);
		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("id", 1);
		mediaJunction1.put("media_id", 1);
		mediaJunction1.put("reference_table", "media");
		mediaJunction1.put("reference_id", 1);
		mediaJunction1.put("photo_crop", "0 0 10 50");
		mediaJunctions.put((Integer)mediaJunction1.get("id"), mediaJunction1);

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
		note2.put("note", "note 1");
		note2.put("reference_table", TABLE_NAME);
		note2.put("reference_id", 1);
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
			final MediaDialog dialog = createForMedia(store, parent)
//			final MediaDialog dialog = createRecordForPhoto(store, parent)
				.withBasePath(FileHelper.documentsDirectory());
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
					final Map<String, Object> container = editCommand.getContainer();
					final Integer mediaID = extractRecordID(container);
					switch(editCommand.getType()){
						case HISTORIC_DATE -> {
							final HistoricDateDialog historicDateDialog = HistoricDateDialog.create(store, parent);
							historicDateDialog.initComponents();
							historicDateDialog.loadData();
							final Integer dateID = extractRecordDateID(container);
							if(dateID != null)
								historicDateDialog.selectData(dateID);

							historicDateDialog.setLocationRelativeTo(null);
							historicDateDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog noteDialog = NoteDialog.create(store, parent)
								.withReference(TABLE_NAME, mediaID)
								.withOnCloseGracefully(record -> {
									if(record != null){
										record.put("reference_table", TABLE_NAME);
										record.put("reference_id", mediaID);
									}
								});
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case PHOTO_CROP -> {
							final PhotoCropDialog photoCropDialog = PhotoCropDialog.create(store, parent);
							photoCropDialog.withOnCloseGracefully(record -> {
								final Rectangle crop = photoCropDialog.getCrop();
								if(crop != null){
									final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
									sj.add(Integer.toString(crop.x))
										.add(Integer.toString(crop.y))
										.add(Integer.toString(crop.width))
										.add(Integer.toString(crop.height));
									container.put("photo_crop", sj);
								}
							});
							try{
								final Integer photoID = extractRecordPhotoID(container);
								if(photoID != null){
									final String photoCrop = extractRecordPhotoCrop(container);
									photoCropDialog.loadData(photoID, photoCrop);
								}

								photoCropDialog.setSize(420, 295);
								photoCropDialog.setLocationRelativeTo(dialog);
								photoCropDialog.setVisible(true);
							}
							catch(final IOException ignored){}
						}
						case ASSERTION -> {
							final AssertionDialog assertionDialog = AssertionDialog.create(store, parent)
								.withReference(TABLE_NAME, mediaID);
							assertionDialog.initComponents();
							assertionDialog.loadData();

							assertionDialog.setLocationRelativeTo(dialog);
							assertionDialog.setVisible(true);
						}
						case EVENT -> {
							final EventDialog eventDialog = EventDialog.create(store, parent)
								.withReference(TABLE_NAME, mediaID);
							eventDialog.initComponents();
							eventDialog.loadData();

							eventDialog.setLocationRelativeTo(null);
							eventDialog.setVisible(true);
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
