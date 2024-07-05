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
import java.util.stream.Collectors;


public final class RepositoryDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = 6136508398081805353L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "repository";
	private static final String TABLE_NAME_SOURCE = "source";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JButton personButton;
	private JButton placeButton;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private JButton sourceButton;


	public RepositoryDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Frame parent){
		super(store, parent);
	}


	public RepositoryDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Repositories");

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"public library", "college library", "national library",
			"prison library", "national archives", "website", "personal collection", "cemetery/mausoleum", "museum", "state library",
			"religious library", "genealogy society collection", "government agency", "funeral home"});
		personButton = new JButton("Reference person", ICON_PERSON);
		placeButton = new JButton("Place", ICON_PLACE);

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");

		sourceButton = new JButton("Sources", ICON_SOURCE);


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, null);
		GUIHelper.setBackgroundColor(identifierField, MANDATORY_FIELD_BACKGROUND_COLOR);

		GUIHelper.bindLabelUndoSelectionAutoCompleteChange(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		personButton.setToolTipText("Reference person");
		personButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.PERSON, getSelectedRecord())));

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.PLACE, getSelectedRecord())));


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.MEDIA, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);

		sourceButton.setToolTipText("Sources");
		sourceButton.addActionListener(e -> EventBusService.publish(EditEvent.create(EditEvent.EditType.SOURCE, getSelectedRecord())));
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(identifierField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "wrap paragraph");
		recordPanelBase.add(personButton, "sizegroup btn,center,split 2");
		recordPanelBase.add(placeButton, "sizegroup btn,gapleft 30,center");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		final JPanel recordPanelChildren = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelChildren.add(sourceButton, "sizegroup btn,center");

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
		recordTabbedPane.add("children", recordPanelChildren);
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
		final int repositoryID = extractRecordID(selectedRecord);
		final String identifier = extractRecordIdentifier(selectedRecord);
		final String type = extractRecordType(selectedRecord);
		final Integer personID = extractRecordPersonID(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);
		final Map<Integer, Map<String, Object>> recordSources = getRecords(TABLE_NAME_SOURCE).entrySet().stream()
			.filter(entry -> repositoryID == extractRecordRepositoryID(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

		identifierField.setText(identifier);
		typeComboBox.setSelectedItem(type);
		GUIHelper.addBorder(personButton, personID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());

		GUIHelper.addBorder(sourceButton, !recordSources.isEmpty(), DATA_BUTTON_BORDER_COLOR);
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		GUIHelper.setBackgroundColor(identifierField, Color.WHITE);
		typeComboBox.setSelectedItem(null);
		GUIHelper.setDefaultBorder(personButton);
		GUIHelper.setDefaultBorder(placeButton);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);

		GUIHelper.setDefaultBorder(sourceButton);
	}

	@Override
	protected boolean validateData(){
		final String identifier = GUIHelper.readTextTrimmed(identifierField);
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
		final String identifier = GUIHelper.readTextTrimmed(identifierField);
		if(!validData(identifier))
			return;

		//read record panel:
		final String type = (String)typeComboBox.getSelectedItem();

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
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static Integer extractRecordPersonID(final Map<String, Object> record){
		return (Integer)record.get("person_id");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordRepositoryID(final Map<String, Object> record){
		return (Integer)record.get("repository_id");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -4111367121357066276L;


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

		final TreeMap<Integer, Map<String, Object>> repositories = new TreeMap<>();
		store.put(TABLE_NAME, repositories);
		final Map<String, Object> repository1 = new HashMap<>();
		repository1.put("id", 1);
		repository1.put("identifier", "repo 1");
		repository1.put("type", "public library");
		repository1.put("person_id", 2);
		repository1.put("place_id", 3);
		repositories.put((Integer)repository1.get("id"), repository1);
		final Map<String, Object> repository2 = new HashMap<>();
		repository2.put("id", 2);
		repository2.put("identifier", "repo 2");
		repository2.put("type", "college library");
		repositories.put((Integer)repository2.get("id"), repository2);
		final Map<String, Object> repository3 = new HashMap<>();
		repository3.put("id", 3);
		repository3.put("identifier", "repo 3");
		repository3.put("type", "private library");
		repositories.put((Integer)repository3.get("id"), repository3);

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
		final TreeMap<Integer, Map<String, Object>> mediaJunctions = new TreeMap<>();
		store.put(TABLE_NAME_MEDIA_JUNCTION, mediaJunctions);
		final Map<String, Object> mediaJunction1 = new HashMap<>();
		mediaJunction1.put("id", 1);
		mediaJunction1.put("media_id", 1);
		mediaJunction1.put("photo_crop", "0 0 10 20");
		mediaJunction1.put("reference_table", TABLE_NAME);
		mediaJunction1.put("reference_id", 1);
		mediaJunctions.put((Integer)mediaJunction1.get("id"), mediaJunction1);

		final TreeMap<Integer, Map<String, Object>> restrictions = new TreeMap<>();
		store.put(TABLE_NAME_RESTRICTION, restrictions);
		final Map<String, Object> restriction1 = new HashMap<>();
		restriction1.put("id", 1);
		restriction1.put("restriction", "confidential");
		restriction1.put("reference_table", TABLE_NAME);
		restriction1.put("reference_id", 1);
		restrictions.put((Integer)restriction1.get("id"), restriction1);

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put("source", sources);
		final Map<String, Object> source1 = new HashMap<>();
		source1.put("id", 1);
		source1.put("identifier", "source 1");
		source1.put("type", "marriage certificate");
		source1.put("author", "author 1 APA-style");
		source1.put("place_id", 3);
		source1.put("date_id", 1);
		source1.put("repository_id", 1);
		source1.put("location", "location 1");
		sources.put((Integer)source1.get("id"), source1);
		final Map<String, Object> source2 = new HashMap<>();
		source2.put("id", 2);
		source2.put("identifier", "source 2");
		source2.put("type", "newspaper");
		source2.put("author", "author 2 APA-style");
		source2.put("place_id", 3);
		source2.put("date_id", 2);
		source2.put("repository_id", 2);
		source2.put("location", "location 2");
		sources.put((Integer)source2.get("id"), source2);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final RepositoryDialog dialog = new RepositoryDialog(store, parent);
			dialog.initComponents();
			dialog.loadData();
			if(!dialog.selectData(extractRecordID(repository1)))
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
						case PERSON -> {
							//TODO single person
						}
						case PLACE -> {
							//TODO single place
//							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
//							final GedcomNode repository = editCommand.getContainer();
//							dialog.setTitle(repository.getID() != null
//								? "Place for repository " + repository.getID()
//								: "Place for new repository");
//							if(!dialog.loadData(repository, editCommand.getOnCloseGracefully()))
//								dialog.showNewRecord();
//
//							dialog.setSize(550, 450);
//							dialog.setVisible(true);
						}
						case NOTE -> {
							final int repositoryID = extractRecordID(editCommand.getContainer());
							final NoteDialog noteDialog = NoteDialog.createWithReferenceTable(store, TABLE_NAME, repositoryID, parent);
							noteDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							noteDialog.initComponents();
							noteDialog.loadData();

							noteDialog.setSize(420, 487);
							noteDialog.setLocationRelativeTo(dialog);
							noteDialog.setVisible(true);
						}
						case MEDIA -> {
							final int repositoryID = extractRecordID(editCommand.getContainer());
							final MediaDialog mediaDialog = MediaDialog.createWithReferenceTable(store, TABLE_NAME, repositoryID, parent)
								.withBasePath("\\Documents\\");
							mediaDialog.withOnCloseGracefully(editCommand.getOnCloseGracefully());
							mediaDialog.initComponents();
							mediaDialog.loadData();

							mediaDialog.setSize(351, 460);
							mediaDialog.setLocationRelativeTo(dialog);
							mediaDialog.setVisible(true);
						}
						case SOURCE -> {
							final int repositoryID = extractRecordID(editCommand.getContainer());
							final SourceDialog dialog = SourceDialog.createWithRepository(store, repositoryID, parent);
							dialog.initComponents();
							dialog.loadData();

							dialog.setSize(440, 436);
							dialog.setLocationRelativeTo(null);
							dialog.setVisible(true);
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
			dialog.setSize(400, 408);
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
