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


public final class SourceDialog extends CommonListDialog{

	@Serial
	private static final long serialVersionUID = -8850730067231141478L;

	private static final int TABLE_INDEX_RECORD_IDENTIFIER = 1;

	private static final String TABLE_NAME = "source";


	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel typeLabel;
	private JComboBox<String> typeComboBox;
	private JLabel authorLabel;
	private JTextField authorField;
	private JButton placeButton;
	private JButton dateButton;
	private JButton repositoryButton;
	private JLabel locationLabel;
	private JTextField locationField;

	private JButton noteButton;
	private JButton mediaButton;
	private JCheckBox restrictionCheckBox;

	private final Integer filterRepositoryID;


	public SourceDialog(final Map<String, TreeMap<Integer, Map<String, Object>>> store, final Integer filterRepositoryID,
			final Frame parent){
		super(store, parent);

		this.filterRepositoryID = filterRepositoryID;
	}


	public SourceDialog withOnCloseGracefully(final Consumer<Object> onCloseGracefully){
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
		setTitle("Sources" + (filterRepositoryID != null? " for repository " + filterRepositoryID: StringUtils.EMPTY));

		super.initStoreComponents();


		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_IDENTIFIER, Comparator.naturalOrder());
	}

	@Override
	protected void initRecordComponents(){
		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();
		typeLabel = new JLabel("Type:");
		typeComboBox = new JComboBox<>(new String[]{"newspaper", "technical journal", "magazine",
			"genealogy newsletter", "blog", "baptism record", "birth certificate", "birth register", "book", "grave marker", "census",
			"death certificate", "yearbook", "directory (organization)", "directory (telephone)", "deed", "land patent", "patent (invention)",
			"diary", "email message", "interview", "personal knowledge", "family story", "audio record", "video record", "letter/postcard",
			"probate record", "will", "legal proceedings record", "manuscript", "map", "marriage certificate", "marriage license",
			"marriage register", "marriage record", "naturalization", "obituary", "pension file", "photograph", "painting/drawing",
			"passenger list", "tax roll", "death index", "birth index", "town record", "web page", "military record", "draft registration",
			"enlistment record", "muster roll", "burial record", "cemetery record", "death notice", "marriage index", "alumni publication",
			"passport", "passport application", "identification card", "immigration record", "border crossing record", "funeral home record",
			"article", "newsletter", "brochure", "pamphlet", "poster", "jewelry", "advertisement", "cemetery", "prison record", "arrest record"});
		authorLabel = new JLabel("Author:");
		authorField = new JTextField();
		placeButton = new JButton("Place", ICON_PLACE);
		dateButton = new JButton("Date", ICON_CALENDAR);
		repositoryButton = new JButton("Repository", ICON_REPOSITORY);
		locationLabel = new JLabel("Location:");
		locationField = new JTextField();

		noteButton = new JButton("Notes", ICON_NOTE);
		mediaButton = new JButton("Media", ICON_MEDIA);
		restrictionCheckBox = new JCheckBox("Confidential");


		GUIHelper.bindLabelTextChangeUndo(identifierLabel, identifierField, evt -> saveData());
		GUIHelper.setBackgroundColor(identifierField, MANDATORY_FIELD_BACKGROUND_COLOR);

		GUIHelper.bindLabelEditableSelectionAutoCompleteChangeUndo(typeLabel, typeComboBox, evt -> saveData(), evt -> saveData());

		GUIHelper.bindLabelTextChangeUndo(authorLabel, authorField, evt -> saveData());

		placeButton.setToolTipText("Place");
		placeButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, getSelectedRecord())));

		dateButton.setToolTipText("Date");
		dateButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.DATE, getSelectedRecord())));

		repositoryButton.setToolTipText("Repository");
		repositoryButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, getSelectedRecord())));

		GUIHelper.bindLabelTextChangeUndo(locationLabel, locationField, evt -> saveData());


		noteButton.setToolTipText("Notes");
		noteButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, getSelectedRecord())));

		mediaButton.setToolTipText("Media");
		mediaButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.MEDIA, getSelectedRecord())));

		restrictionCheckBox.addItemListener(this::manageRestrictionCheckBox);
	}

	@Override
	protected void initRecordLayout(final JComponent recordTabbedPane){
		final JPanel recordPanelBase = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelBase.add(identifierLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(identifierField, "grow,wrap related");
		recordPanelBase.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(typeComboBox, "wrap");
		recordPanelBase.add(authorLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(authorField, "grow,wrap paragraph");
		recordPanelBase.add(placeButton, "sizegroup btn,center,split 3");
		recordPanelBase.add(dateButton, "sizegroup btn,gapleft 30,center");
		recordPanelBase.add(repositoryButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelBase.add(locationLabel, "align label,sizegroup label,split 2");
		recordPanelBase.add(locationField, "grow");

		final JPanel recordPanelOther = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow]"));
		recordPanelOther.add(noteButton, "sizegroup btn,center,split 3");
		recordPanelOther.add(mediaButton, "sizegroup btn,gapleft 30,center,wrap paragraph");
		recordPanelOther.add(restrictionCheckBox);

		recordTabbedPane.add("base", recordPanelBase);
		recordTabbedPane.add("other", recordPanelOther);
	}

	@Override
	protected void loadData(){
		Map<Integer, Map<String, Object>> records = getRecords(TABLE_NAME);
		if(filterRepositoryID != null)
			records = records.entrySet().stream()
				.filter(entry -> filterRepositoryID.equals(extractRecordRepositoryID(entry.getValue())))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, TreeMap::new));

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
		final String type = extractRecordType(selectedRecord);
		final String author = extractRecordAuthor(selectedRecord);
		final Integer placeID = extractRecordPlaceID(selectedRecord);
		final Integer dateID = extractRecordDateID(selectedRecord);
		final Integer repositoryID = extractRecordRepositoryID(selectedRecord);
		final String location = extractRecordLocation(selectedRecord);
		final Map<Integer, Map<String, Object>> recordNotes = extractReferences(TABLE_NAME_NOTE);
		final Map<Integer, Map<String, Object>> recordMediaJunction = extractReferences(TABLE_NAME_MEDIA_JUNCTION);
		final Map<Integer, Map<String, Object>> recordRestriction = extractReferences(TABLE_NAME_RESTRICTION);

		identifierField.setText(identifier);
		typeComboBox.setSelectedItem(type);
		authorField.setText(author);
		GUIHelper.addBorder(placeButton, placeID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(dateButton, dateID != null, DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(repositoryButton, repositoryID != null, DATA_BUTTON_BORDER_COLOR);
		locationField.setText(location);

		GUIHelper.addBorder(noteButton, !recordNotes.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		GUIHelper.addBorder(mediaButton, !recordMediaJunction.isEmpty(), DATA_BUTTON_BORDER_COLOR);
		restrictionCheckBox.setSelected(!recordRestriction.isEmpty());
	}

	@Override
	protected void clearData(){
		identifierField.setText(null);
		GUIHelper.setBackgroundColor(identifierField, Color.WHITE);
		typeComboBox.setSelectedItem(null);
		authorField.setText(null);
		GUIHelper.setDefaultBorder(placeButton);
		GUIHelper.setDefaultBorder(dateButton);
		GUIHelper.setDefaultBorder(repositoryButton);
		locationField.setText(null);

		GUIHelper.setDefaultBorder(noteButton);
		GUIHelper.setDefaultBorder(mediaButton);
		restrictionCheckBox.setSelected(false);
	}

	@Override
	protected boolean validateData(){
		if(selectedRecord != null){
			//read record panel:
			final String identifier = GUIHelper.readTextTrimmed(identifierField);
			//enforce non-nullity on `identifier`
			if(identifier == null || identifier.isEmpty()){
				JOptionPane.showMessageDialog(getParent(), "Identifier field is required", "Error",
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
		final String author = GUIHelper.readTextTrimmed(authorField);
		final String location = GUIHelper.readTextTrimmed(locationField);

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
		selectedRecord.put("author", author);
		selectedRecord.put("location", location);
	}


	private static String extractRecordIdentifier(final Map<String, Object> record){
		return (String)record.get("identifier");
	}

	private static String extractRecordType(final Map<String, Object> record){
		return (String)record.get("type");
	}

	private static String extractRecordAuthor(final Map<String, Object> record){
		return (String)record.get("author");
	}

	private static Integer extractRecordPlaceID(final Map<String, Object> record){
		return (Integer)record.get("place_id");
	}

	private static Integer extractRecordDateID(final Map<String, Object> record){
		return (Integer)record.get("date_id");
	}

	private static Integer extractRecordRepositoryID(final Map<String, Object> record){
		return (Integer)record.get("repository_id");
	}

	private static String extractRecordLocation(final Map<String, Object> record){
		return (String)record.get("location");
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -7690715719682561917L;


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

		final TreeMap<Integer, Map<String, Object>> sources = new TreeMap<>();
		store.put(TABLE_NAME, sources);
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
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case PLACE -> {
							//TODO
//							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
//							final GedcomNode source = editCommand.getContainer();
//							dialog.setTitle(source.getID() != null
//								? "Place for source " + source.getID()
//								: "Place for new source");
//							if(!dialog.loadData(source, editCommand.getOnCloseGracefully()))
//								dialog.showNewRecord();
//
//							dialog.setSize(550, 450);
//							dialog.setVisible(true);
						}
						case DATE -> {
							//TODO
						}
						case REPOSITORY -> {
							//TODO
						}
						case NOTE -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNote(store, parent);
//							final GedcomNode source = editCommand.getContainer();
//							dialog.setTitle(source.getID() != null
//								? "Note " + source.getID()
//								: "New note for " + container.getID());
//							dialog.loadData(source, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(500, 513);
//							dialog.setVisible(true);
						}
						case MEDIA -> {
							//TODO
//							final NoteDialog dialog = NoteDialog.createNoteTranslation(store, parent);
//							final GedcomNode noteTranslation = editCommand.getContainer();
//							dialog.setTitle(StringUtils.isNotBlank(noteTranslation.getValue())
//								? "Translation for language " + store.traverse(noteTranslation, "LOCALE").getValue()
//								: "New translation"
//							);
//							dialog.loadData(noteTranslation, editCommand.getOnCloseGracefully());
//
//							dialog.setSize(450, 209);
//							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final Integer filterRepositoryID = null;
			final SourceDialog dialog = new SourceDialog(store, filterRepositoryID, parent);
			if(!dialog.loadData(extractRecordID(source1)))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(440, 475);
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
