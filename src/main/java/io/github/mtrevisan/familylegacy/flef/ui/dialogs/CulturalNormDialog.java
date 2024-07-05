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

import io.github.mtrevisan.familylegacy.flef.db.DatabaseManager;
import io.github.mtrevisan.familylegacy.flef.db.DatabaseManagerInterface;
import io.github.mtrevisan.familylegacy.flef.helpers.DependencyInjector;
import io.github.mtrevisan.familylegacy.flef.ui.events.EditEvent;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CertaintyComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TextPreviewPane;
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
import java.io.IOException;
import java.io.Serial;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;


public final class CulturalNormDialog extends CommonListDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = -3961030253095528462L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "cultural_norm";
	private static final String TABLE_NAME_CULTURAL_NORM_JUNCTION = "cultural_norm_junction";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel descriptionLabel;
	private TextPreviewPane descriptionTextArea;
	private JButton placeButton;
	private JButton dateStartButton;
	private JButton dateEndButton;
	private JLabel certaintyLabel;
	private JComboBox<String> certaintyComboBox;
	private JLabel credibilityLabel;
	private JComboBox<String> credibilityComboBox;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private JButton referenceButton;
	private JLabel linkCertaintyLabel;
	private JComboBox<String> linkCertaintyComboBox;
	private JLabel linkCredibilityLabel;
	private JComboBox<String> linkCredibilityComboBox;

	private String filterReferenceTable;
	private int filterReferenceID;


	public static CulturalNormDialog create(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		return new CulturalNormDialog(store, parent);
	}

	public static CulturalNormDialog createWithReferenceTable(final Map<String, TreeMap<Integer, Map<String, Object>>> store,
			final String referenceTable, final int filterReferenceID, final Frame parent){
		final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
		dialog.filterReferenceTable = referenceTable;
		dialog.filterReferenceID = filterReferenceID;
		return dialog;
	}


	private CulturalNormDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public CulturalNormDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Cultural norms");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();

		descriptionLabel = new JLabel("Description:");
		descriptionTextArea = TextPreviewPane.createWithPreview(this);
		descriptionTextArea.setTextViewFont(identifierField.getFont());

		placeButton = new JButton("Place", ICON_PLACE);
		dateStartButton = new JButton("Date start", ICON_CALENDAR);
		dateEndButton = new JButton("Date end", ICON_CALENDAR);

		certaintyLabel = new JLabel("Certainty:");
		certaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
		credibilityLabel = new JLabel("Credibility:");
		credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");

		referenceButton = new JButton("Reference", ICON_REFERENCE);
		linkCertaintyLabel = new JLabel("Certainty:");
		linkCertaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
		linkCredibilityLabel = new JLabel("Credibility:");
		linkCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, null);
		GUIHelper.setBackgroundColor(identifierField, MANDATORY_FIELD_BACKGROUND_COLOR);

		GUIHelper.bindLabelTextChange(descriptionLabel, descriptionTextArea, evt -> saveData());

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.PLACE, getSelectedRecord())));

		dateStartButton.setToolTipText("Start date");
		dateStartButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.DATE, getSelectedRecord())));

		dateEndButton.setToolTipText("End date");
		dateEndButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.DATE, getSelectedRecord())));

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(certaintyLabel, certaintyComboBox, evt -> saveData(), evt -> saveData());
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(credibilityLabel, credibilityComboBox, evt -> saveData(),
			evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.MEDIA, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);


		if(filterReferenceTable == null){
			referenceButton.setToolTipText("Reference");
			referenceButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.REFERENCE, getSelectedRecord())
				.withOnCloseGracefully(container -> {
					//TODO save data
				})));
			GUIHelper.addBorder(referenceButton, MANDATORY_COMBOBOX_BACKGROUND_COLOR);
		}

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCertaintyLabel, linkCertaintyComboBox, evt -> saveData(),
			evt -> saveData());
		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(linkCredibilityLabel, linkCredibilityComboBox, evt -> saveData(),
			evt -> saveData());
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		referenceButton.setVisible(filterReferenceTable == null);

		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(identifierField, "growx,wrap paragraph");
		recordPanelBase.add(descriptionLabel, "align label,top,sizegroup label,split 2");
		recordPanelBase.add(descriptionTextArea, "grow,wrap paragraph");
		recordPanelBase.add(placeButton, "sizegroup btn,center,wrap paragraph");
		recordPanelBase.add(dateStartButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(dateEndButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(certaintyLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(certaintyComboBox, "wrap related");
		recordPanelBase.add(credibilityLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(credibilityComboBox);

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 2");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelLink = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelLink.add(referenceButton, "sizegroup btn,center,wrap paragraph,hidemode 3");
		recordPanelLink.add(linkCertaintyLabel, "align label,sizegroup label,split 2");
		recordPanelLink.add(linkCertaintyComboBox, "wrap related");
		recordPanelLink.add(linkCredibilityLabel, "align label,sizegroup label,split 2");
		recordPanelLink.add(linkCredibilityComboBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("link", recordPanelLink);
	}

	@Override
	public void loadData(){
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
		final String description = extractRecordDescription(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Integer dateStartID = extractRecordDateStartID(selectedRecord);
		final Integer dateEndID = extractRecordDateEndID(selectedRecord);
		final String certainty = extractRecordCertainty(selectedRecord);
		final String credibility = extractRecordCredibility(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION,
			CulturalNormDialog::extractRecordMediaID, extractRecordID(selectedRecord));
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		identifierField.setText(identifier);
		descriptionTextArea.setText("Cultural norm " + extractRecordID(selectedRecord), description, null);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(dateStartButton, dateStartID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(dateEndButton, dateEndID != null, DATA_BUTTON_BORDER_COLOR);
		certaintyComboBox.setSelectedItem(certainty);
		credibilityComboBox.setSelectedItem(credibility);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		if(filterReferenceTable == null){
			final Map<Integer, Map<String, Object>> recordCulturalNormJunction = extractReferences(TABLE_NAME_CULTURAL_NORM_JUNCTION,
				CulturalNormDialog::extractRecordCulturalNormID, extractRecordID(selectedRecord));
			if(recordCulturalNormJunction.size() > 1)
				throw new IllegalArgumentException("Data integrity error");

			final Map<String, Object> record = recordCulturalNormJunction.values().stream()
				.findFirst()
				.orElse(Collections.emptyMap());

			GUIHelper.addBorder(referenceButton, (!recordCulturalNormJunction.isEmpty()? DATA_BUTTON_BORDER_COLOR:
				MANDATORY_COMBOBOX_BACKGROUND_COLOR));
		}
		linkCertaintyComboBox.setSelectedItem(certainty);
		linkCredibilityComboBox.setSelectedItem(credibility);
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		GUIHelper.setBackgroundColor(identifierField, Color.WHITE);
		descriptionTextArea.clear();
		GUIHelper.setDefaultBorder(placeButton);
		GUIHelper.setDefaultBorder(dateStartButton);
		GUIHelper.setDefaultBorder(dateEndButton);
		certaintyComboBox.setSelectedItem(null);
		credibilityComboBox.setSelectedItem(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);

		if(filterReferenceTable == null){
			GUIHelper.setDefaultBorder(referenceButton);
			certaintyComboBox.setSelectedItem(null);
			credibilityComboBox.setSelectedItem(null);
		}
	}

	@Override
	protected boolean validateData(){
		final String identifier = extractRecordIdentifier(selectedRecord);
		if(!validData(identifier)){
			JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
				JOptionPane.ERROR_MESSAGE);
			identifierField.requestFocusInWindow();

			return false;
		}
		return true;
	}

	@Override
	protected void saveData(){
		//read record panel:
		final String identifier = identifierField.getText();
		final String description = descriptionTextArea.getText();
		final String certainty = (String)certaintyComboBox.getSelectedItem();
		final String credibility = (String)credibilityComboBox.getSelectedItem();

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
		selectedRecord.put("description", description);
		selectedRecord.put("certainty", certainty);
		selectedRecord.put("credibility", credibility);
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordDescription(final Map<String, Object> record){
		return (String)record.get("description");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordDateStartID(final Map<String, Object> record){
		return (Integer)record.get("date_start_id");
	}

	private static Integer extractRecordDateEndID(final Map<String, Object> record){
		return (Integer)record.get("date_end_id");
	}

	private static Integer extractRecordMediaID(final Map<String, Object> record){
		return (Integer)record.get("media_id");
	}

	private static Integer extractRecordCulturalNormID(final Map<String, Object> record){
		return (Integer)record.get("cultural_norm_id");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 5902002320122325722L;


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

		final TreeMap<Integer, Map<String, Object>> culturalNorms = new TreeMap<>();
		store.put(TABLE_NAME, culturalNorms);
		final Map<String, Object> culturalNorm = new HashMap<>();
		culturalNorm.put("id", 1);
		culturalNorm.put("identifier", "rule 1 id");
		culturalNorm.put("description", "rule 1");
		culturalNorm.put("place_id", 1);
		culturalNorm.put("date_start_id", 1);
		culturalNorm.put("date_end_id", 1);
		culturalNorm.put("certainty", "certain");
		culturalNorm.put("credibility", "direct and primary evidence used, or by dominance of the evidence");
		culturalNorms.put((Integer)culturalNorm.get("id"), culturalNorm);

		final TreeMap<Integer, Map<String, Object>> culturalNormJunctions = new TreeMap<>();
		store.put(TABLE_NAME_CULTURAL_NORM_JUNCTION, culturalNormJunctions);
		final Map<String, Object> culturalNormJunction1 = new HashMap<>();
		culturalNormJunction1.put("id", 1);
		culturalNormJunction1.put("cultural_norm_id", 1);
		culturalNormJunction1.put("reference_table", "cultural_norm");
		culturalNormJunction1.put("reference_id", 1);
		culturalNormJunction1.put("certainty", "probable");
		culturalNormJunction1.put("credibility", "probable");
		culturalNormJunctions.put((Integer)culturalNormJunction1.get("id"), culturalNormJunction1);

		final TreeMap<Integer, Map<String, Object>> places = new TreeMap<>();
		store.put("place", places);
		final Map<String, Object> place1 = new HashMap<>();
		place1.put("id", 1);
		place1.put("identifier", "place 1");
		place1.put("name_id", 1);
		places.put((Integer)place1.get("id"), place1);

		final TreeMap<Integer, Map<String, Object>> localizedTexts = new TreeMap<>();
		store.put("localized_text", localizedTexts);
		final Map<String, Object> localizedText1 = new HashMap<>();
		localizedText1.put("id", 1);
		localizedText1.put("text", "place 1 name");
		localizedTexts.put((Integer)localizedText1.get("id"), localizedText1);

		final TreeMap<Integer, Map<String, Object>> dates = new TreeMap<>();
		store.put("historic_date", dates);
		final Map<String, Object> date1 = new HashMap<>();
		date1.put("id", 1);
		date1.put("date", "18 OCT 2000");
		dates.put((Integer)date1.get("id"), date1);

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

		final TreeMap<Integer, Map<String, Object>> media = new TreeMap<>();
		store.put("media", media);
		final Map<String, Object> m1 = new HashMap<>();
		m1.put("id", 1);
		m1.put("identifier", "custom media");
		media.put((Integer)m1.get("id"), m1);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final DependencyInjector injector = new DependencyInjector();
			final DatabaseManager dbManager = new DatabaseManager("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
			try{
				final String grammarFile = "src/main/resources/gedg/treebard/FLeF.sql";
				dbManager.initialize(grammarFile);

				dbManager.insertDatabase(store);
			}
			catch(final SQLException | IOException e){
				throw new RuntimeException(e);
			}
			injector.register(DatabaseManagerInterface.class, dbManager);

			final CulturalNormDialog dialog = create(store, parent);
			injector.injectDependencies(dialog);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(culturalNorm)))
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
						case PLACE -> {
							//TODO single cultural norm
						}
						case DATE -> {
							//TODO single cultural norm
						}
						case NOTE -> {
							final int culturalNormID = extractRecordID(editCommand.getContainer());
							final NoteDialog noteDialog = NoteDialog.createWithReferenceTable(store, TABLE_NAME, culturalNormID, parent);
							noteDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 487);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case MEDIA -> {
							final int culturalNormID = extractRecordID(editCommand.getContainer());
							final MediaDialog mediaDialog = MediaDialog.createWithReferenceTable(store, TABLE_NAME, culturalNormID, parent)
								.withBasePath("\\Documents\\");
							mediaDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setSize(351, 460);
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
					System.exit(0);
				}
			});
			dialog.setSize(474, 665);
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
