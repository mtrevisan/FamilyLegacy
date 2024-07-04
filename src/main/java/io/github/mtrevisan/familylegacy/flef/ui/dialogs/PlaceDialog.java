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
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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


public final class PlaceDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -8409918543709413945L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "place";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JButton nameButton;
	private JButton transcribedNameButton;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel coordinateLabel;
	private JTextField coordinateField;
	private JLabel coordinateSystemLabel;
	private JComboBox<String> coordinateSystemComboBox;
	private JLabel coordinateCredibilityLabel;
	private JComboBox<String> coordinateCredibilityComboBox;
	private JButton primaryPlaceButton;
	private JButton photoButton;
	private JButton photoCropButton;

	private JButton noteButton;
	private JButton photosButton;
	private JCheckBox restrictionCheckBox;


	public PlaceDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public PlaceDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Places");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();
		nameButton = new JButton("Name", ICON_TEXT);
		transcribedNameButton = new JButton("Transcribed names", ICON_TRANSLATION);
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"nation", "province", "state", "county", "city", "township",
			"parish", "island", "archipelago", "continent", "unincorporated town", "settlement", "village", "address"});
		coordinateLabel = new JLabel("Coordinate:");
		coordinateField = new JTextField();
		coordinateSystemLabel = new JLabel("Coordinate system:");
		coordinateSystemComboBox = new JComboBox<>(new String[]{"WGS84", "UTM"});
		coordinateCredibilityLabel = new JLabel("Coordinate credibility:");
		coordinateCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
		primaryPlaceButton = new JButton("Primary place", ICON_PLACE);
		photoButton = new JButton("Photo", ICON_PHOTO);
		photoCropButton = new JButton("Photo crop", ICON_PHOTO_CROP);

		noteButton = new JButton("Notes", ICON_NOTE);
		photosButton = new JButton("Photos", ICON_PHOTO);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, evt -> saveData());
		GUIHelper.setBackgroundColor(identifierField, MANDATORY_FIELD_BACKGROUND_COLOR);

		nameButton.setToolTipText("Name");
		nameButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NAME, getSelectedRecord())));

		transcribedNameButton.setToolTipText("Transcribed names");
		transcribedNameButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.LOCALIZED_PLACE_NAME,
			getSelectedRecord())));

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelTextChangeUndo(coordinateLabel, coordinateField, evt -> saveData());
		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(coordinateSystemLabel, coordinateSystemComboBox, evt -> saveData(),
			evt -> saveData());

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(coordinateCredibilityLabel, coordinateCredibilityComboBox,
			evt -> saveData(), evt -> saveData());

		primaryPlaceButton.setToolTipText("Primary place");
		primaryPlaceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, getSelectedRecord())));

		photoButton.setToolTipText("Photo");
		photoButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PHOTO, getSelectedRecord())));

		photoCropButton.setToolTipText("Define a crop");
		photoCropButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PHOTO_CROP, getSelectedRecord())));
		photoCropButton.setEnabled(false);


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(identifierField, "growx,wrap paragraph");
		recordPanelBase.add(nameButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(transcribedNameButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "growx,wrap paragraph");
		recordPanelBase.add(coordinateLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(coordinateField, "growx,wrap related");
		recordPanelBase.add(coordinateSystemLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(coordinateSystemComboBox, "growx,wrap paragraph");
		recordPanelBase.add(coordinateCredibilityLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(coordinateCredibilityComboBox, "wrap paragraph");
		recordPanelBase.add(primaryPlaceButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(photoButton, "sizegroup btn,center,wrap related");
		recordPanelBase.add(photoCropButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(photosButton, "sizegroup btn,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

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
		final Integer nameID = extractRecordNameID(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final String coordinate = extractRecordCoordinate(selectedRecord);
		final String coordinateSystem = extractRecordCoordinateSystem(selectedRecord);
		final String coordinateCredibility = extractRecordCoordinateCredibility(selectedRecord);
		final Integer primaryPlaceID = extractRecordPrimaryPlaceID(selectedRecord);
		final Integer photoID = extractRecordPhotoID(selectedRecord);
		final String photoCrop = extractRecordPhotoCrop(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordTranscribedNames = extractLocalizedTextJunction("name");
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		identifierField.setText(identifier);
		GUIHelper.addBorder(nameButton, nameID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(transcribedNameButton, !recordTranscribedNames.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		typeComboBox.setSelectedItem(type);
		coordinateField.setText(coordinate);
		coordinateSystemComboBox.setSelectedItem(coordinateSystem);
		coordinateCredibilityComboBox.setSelectedItem(coordinateCredibility);
		GUIHelper.addBorder(primaryPlaceButton, primaryPlaceID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(photoButton, photoID != null, DATA_BUTTON_BORDER_COLOR);
		photoCropButton.setEnabled(photoCrop != null && !photoCrop.isEmpty());

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(photosButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		GUIHelper.setBackgroundColor(identifierField, Color.WHITE);
		GUIHelper.setDefaultBorder(nameButton);
		GUIHelper.setDefaultBorder(transcribedNameButton);
		typeComboBox.setSelectedItem(null);
		coordinateField.setText(null);
		coordinateSystemComboBox.setSelectedItem(null);
		coordinateCredibilityComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(primaryPlaceButton);
		GUIHelper.setDefaultBorder(photoButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(photosButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String date = GUIHelper.readTextTrimmed(identifierField);
			//enforce non-nullity on `identifier`
			if(date == null || date.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Date field is required", "Error",
					JOptionPane.ERROR_MESSAGE);
				identifierField.requestFocusInWindow();

				return false;
			}
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String identifier = GUIHelper.readTextTrimmed(identifierField);
		final String type = (String)typeComboBox.getSelectedItem();
		final String coordinate = GUIHelper.readTextTrimmed(coordinateField);
		final String coordinateSystem = (String)coordinateSystemComboBox.getSelectedItem();
		final String coordinateCredibility = (String)coordinateCredibilityComboBox.getSelectedItem();

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

		selectedRecord.put("identifier", identifier);
		selectedRecord.put("type", type);
		selectedRecord.put("coordinate", coordinate);
		selectedRecord.put("coordinate_system", coordinateSystem);
		selectedRecord.put("coordinate_credibility", coordinateCredibility);
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static Integer extractRecordNameID(final Map<String, Object> record){
		return (Integer)record.get("name_id");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordCoordinate(final Map<String, Object> record){
		return (String)record.get("coordinate");
	}

	private static String extractRecordCoordinateSystem(final Map<String, Object> record){
		return (String)record.get("coordinate_system");
	}

	private static String extractRecordCoordinateCredibility(final Map<String, Object> record){
		return (String)record.get("coordinate_credibility");
	}

	private static Integer extractRecordPrimaryPlaceID(final Map<String, Object> record){
		return (Integer)record.get("primary_place_id");
	}

	private static Integer extractRecordPhotoID(final Map<String, Object> record){
		return (Integer)record.get("photo_id");
	}

	private static String extractRecordPhotoCrop(final Map<String, Object> record){
		return (String)record.get("photo_crop");
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

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put(TABLE_NAME, places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place");
		place1.put("name_id", 1);
		place1.put("type", "province");
		place1.put("coordinate", "45.65, 12.19");
		place1.put("coordinate_system", "WGS84");
		place1.put("coordinate_credibility", "certain");
		place1.put("primary_place_id", 1);
		place1.put("photo_id", 1);
		place1.put("photo_crop", "0 0 10 20");
		places.put((Integer)place1.get("id"), place1);

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
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public static void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case NAME -> {
							//TODO
						}
						case LOCALIZED_PLACE_NAME -> {
							//TODO
						}
						case PLACE -> {
							//TODO
						}
						case PHOTO -> {
							//TODO
						}
						case PHOTO_CROP -> {
							//TODO
//							final Point cropStartPoint = ((CropDialog)cropDialog).getCropStartPoint();
//							final Point cropEndPoint = ((CropDialog)cropDialog).getCropEndPoint();
//							final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
//							sj.add(Integer.toString(cropStartPoint.x));
//							sj.add(Integer.toString(cropStartPoint.y));
//							sj.add(Integer.toString(cropEndPoint.x));
//							sj.add(Integer.toString(cropEndPoint.y));
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode place = editCommand.getContainer();
//							dialog.setTitle(place.getID() != null
//								? "Note " + place.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(place, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setLocationRelativeTo(parent);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final PlaceDialog dialog = new PlaceDialog(store, parent);
			if(!dialog.loadData(extractRecordID(place1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(522, 647);
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
