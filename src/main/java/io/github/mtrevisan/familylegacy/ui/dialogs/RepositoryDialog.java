/**
 * Copyright (c) 2020-2022 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.ui.dialogs;

import io.github.mtrevisan.familylegacy.flef.ui.helpers.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.TableHelper;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.flef.ui.helpers.eventbus.events.BusExceptionEvent;
import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class RepositoryDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5873775240948872171L;

	private static final String RECORD_TAG = "REPOSITORY";
	private static final String ARRAY = "[]";
	private static final String REFERENCE = "@";
	private static final String RECORD_TAG_REFERENCE = RECORD_TAG + REFERENCE;
	private static final String RECORD_TAG_ARRAY = RECORD_TAG + ARRAY;
	private static final String RECORD_NAME = "NAME";
	private static final String RECORD_AUTHOR = "AUTHOR";
	private static final String RECORD_CREDIBILITY = "CREDIBILITY";
	private static final String RECORD_NOTE = "NOTE";
	private static final String RECORD_NOTE_ARRAY = RECORD_NOTE + ARRAY;
	private static final String RECORD_DOCUMENT_ARRAY = "DOCUMENT" + ARRAY;
	private static final String RECORD_SOURCE = "SOURCE";
	private static final String RECORD_SOURCE_ARRAY = RECORD_SOURCE + ARRAY;
	private static final String RECORD_PLACE = "PLACE";
	private static final String RECORD_PUBLISHER = "PUBLISHER";
	private static final String RECORD_REPOSITORY_ARRAY = "REPOSITORY" + ARRAY;
	private static final String RECORD_MEDIA_TYPE = "MEDIA_TYPE";
	private static final String RECORD_CALENDAR = "CALENDAR";
	private static final String RECORD_ORIGINAL_TEXT = "ORIGINAL_TEXT";
	private static final String RECORD_LOCATION = "LOCATION";
	private static final String RECORD_CREATION = "CREATION";
	private static final String RECORD_DATE = "DATE";
	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
	private static final KeyStroke DELETE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int TABLE_PREFERRED_WIDTH_RECORD_ID = 25;

	private static final int TABLE_INDEX_RECORD_ID = 0;
	private static final int TABLE_INDEX_RECORD_NAME = 1;
	private static final int TABLE_ROWS_SHOWN = 4;

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);
	private static final ImageIcon ICON_PLACE = ResourceHelper.getImage("/images/place.png", 20, 20);
	private static final ImageIcon ICON_REPOSITORY = ResourceHelper.getImage("/images/repository.png", 20, 20);

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane recordScrollPane = new JScrollPane(recordTable);
	private final JButton newButton = new JButton("New");
	private final JButton deleteButton = new JButton("Delete");

	private final JPanel citationPanel = new JPanel();
	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JButton citationNoteButton = new JButton(ICON_NOTE);

	private final JTabbedPane tabbedPane = new JTabbedPane();
	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JLabel authorLabel = new JLabel("Author:");
	private final JTextField authorField = new JTextField();
	private final JButton publicationPlaceButton = new JButton(ICON_PLACE);
	private final DatePanel datePanel = new DatePanel();
	private final JLabel publisherLabel = new JLabel("Publisher:");
	private final JTextField publisherField = new JTextField();
	private final JButton repositoryButton = new JButton(ICON_REPOSITORY);
	private final JLabel mediaTypeLabel = new JLabel("Media type:");
	private final JTextField mediaTypeField = new JTextField();
	private final JButton documentButton = new JButton("Documents");
	private final JButton sourceButton = new JButton(ICON_SOURCE);
	private final JButton recordNoteButton = new JButton(ICON_NOTE);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<RepositoryDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode originalRecord;
	private GedcomNode record;
	private GedcomNode previouslySelectedRecord;
	private long previouslySelectedRecordHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public RepositoryDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}


	void initComponents(){
		initCitationsComponents();

		initRecordComponents();

		initLayout();
	}

	private void initCitationsComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(RepositoryDialog.this);
			}
		});

		recordTable.setAutoCreateRowSorter(true);
		recordTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		recordTable.setGridColor(GRID_COLOR);
		recordTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordTable.setDragEnabled(true);
		recordTable.setDropMode(DropMode.INSERT_ROWS);
		recordTable.setTransferHandler(new TableTransferHandle(recordTable, this::extractRecords,
			nodes -> {
				record.removeChildrenWithTag(RECORD_TAG);
				record.addChildren(nodes);
			}));
		recordTable.getTableHeader()
			.setFont(recordTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_RECORD_ID, 0, TABLE_PREFERRED_WIDTH_RECORD_ID);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_RECORD_NAME, Comparator.naturalOrder());
		recordTable.setRowSorter(sorter);
		//clicking on a line links it to current repository citation
		recordTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!evt.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0)
					selectAction();
			});
		final InputMap recordTableInputMap = recordTable.getInputMap(JComponent.WHEN_FOCUSED);
		recordTableInputMap.put(INSERT_STROKE, ACTION_MAP_KEY_INSERT);
		recordTableInputMap.put(DELETE_STROKE, ACTION_MAP_KEY_DELETE);
		final ActionMap recordTableActionMap = recordTable.getActionMap();
		recordTableActionMap.put(ACTION_MAP_KEY_INSERT, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				newAction();
			}
		});
		recordTableActionMap.put(ACTION_MAP_KEY_DELETE, new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});
		final Dimension viewSize = new Dimension();
		viewSize.width = recordTable.getColumnModel().getTotalColumnWidth();
		viewSize.height = TABLE_ROWS_SHOWN * recordTable.getRowHeight();
		recordTable.setPreferredScrollableViewportSize(viewSize);

		newButton.addActionListener(evt -> newAction());
		deleteButton.setEnabled(false);
		deleteButton.addActionListener(evt -> deleteAction());
	}

	private void initRecordComponents(){
		//citation part:
		locationLabel.setLabelFor(locationField);

		citationNoteButton.setToolTipText("Add citation note");
		final ActionListener addCitationNoteAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> notes = store.traverseAsList(record, RECORD_NOTE_ARRAY);
				GUIHelper.addBorder(citationNoteButton, !notes.isEmpty(), Color.BLUE);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, record, onAccept));
		};
		citationNoteButton.addActionListener(addCitationNoteAction);
		citationNoteButton.setEnabled(false);


		//record part:
		titleLabel.setLabelFor(titleField);

		authorLabel.setLabelFor(authorField);

		publisherLabel.setLabelFor(publisherField);

		publicationPlaceButton.setToolTipText("Place of publication");
		publicationPlaceButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, getSelectedRecord())));

		repositoryButton.setToolTipText("Repository");
		repositoryButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, getSelectedRecord())));

		mediaTypeLabel.setLabelFor(mediaTypeField);

		final ActionListener addRecordDocumentAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> documents = store.traverseAsList(record, RECORD_DOCUMENT_ARRAY);
				GUIHelper.addBorder(sourceButton, !documents.isEmpty(), Color.BLUE);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.DOCUMENT, record, onAccept));
		};
		documentButton.addActionListener(addRecordDocumentAction);

		sourceButton.setToolTipText("Add source");
		final ActionListener addRecordSourceAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> sources = store.traverseAsList(record, RECORD_SOURCE_ARRAY);
				GUIHelper.addBorder(sourceButton, !sources.isEmpty(), Color.BLUE);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, record, onAccept));
		};
		sourceButton.addActionListener(addRecordSourceAction);

		recordNoteButton.setToolTipText("Add record note");
		final ActionListener addRecordNoteAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> notes = store.traverseAsList(record, RECORD_NOTE_ARRAY);
				GUIHelper.addBorder(recordNoteButton, !notes.isEmpty(), Color.BLUE);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, record, onAccept));
		};
		recordNoteButton.addActionListener(addRecordNoteAction);

		credibilityLabel.setLabelFor(credibilityComboBox);
		credibilityComboBox.addActionListener(evt -> {
			final GedcomNode selectedRecord = getSelectedRecord();
			final GedcomNode selectedRecordPlace = store.traverse(selectedRecord, RECORD_PLACE);
			final int selectedIndex = credibilityComboBox.getSelectedIndex();
			if(selectedIndex > 0){
				final String credibility = Integer.toString(selectedIndex - 1);
				final GedcomNode credibilityNode = store.traverse(selectedRecordPlace, RECORD_CREDIBILITY);
				if(credibilityNode.isEmpty())
					selectedRecordPlace.addChildValue(RECORD_CREDIBILITY, credibility);
				else
					credibilityNode.withValue(credibility);
			}
			else
				selectedRecordPlace.removeChildrenWithTag(RECORD_CREDIBILITY);
		});
	}

	private void initLayout(){
		citationPanel.setBorder(BorderFactory.createTitledBorder("Citation"));
		citationPanel.setLayout(new MigLayout());
		citationPanel.add(locationLabel, "align label,sizegroup label,split 2");
		citationPanel.add(locationField, "grow,wrap");
		citationPanel.add(citationNoteButton, "sizegroup button,center");
		GUIHelper.setEnabled(citationPanel, false);

		final JPanel recordPanel1 = new JPanel(new MigLayout());
		recordPanel1.add(titleLabel, "align label,sizegroup label,split 2");
		recordPanel1.add(titleField, "grow,wrap");
		recordPanel1.add(authorLabel, "align label,sizegroup label,split 2");
		recordPanel1.add(authorField, "grow,wrap");
		recordPanel1.add(publicationPlaceButton, "split 2,center,wrap");
		recordPanel1.add(datePanel, "grow,wrap");
		recordPanel1.add(publisherLabel, "align label,sizegroup label,split 2");
		recordPanel1.add(publisherField, "grow");
		final JPanel recordPanel2 = new JPanel(new MigLayout());
		recordPanel2.add(repositoryButton, "center,wrap");
		recordPanel2.add(mediaTypeLabel, "align label,sizegroup label,split 2");
		recordPanel2.add(mediaTypeField, "grow,wrap");
		recordPanel2.add(documentButton, "split 3,center");
		recordPanel2.add(sourceButton, "center");
		recordPanel2.add(recordNoteButton, "center,wrap");
		recordPanel2.add(restrictionCheckBox, "wrap");
		recordPanel2.add(credibilityLabel, "align label,sizegroup label,split 2");
		recordPanel2.add(credibilityComboBox);
		tabbedPane.setBorder(BorderFactory.createTitledBorder("Record"));
		tabbedPane.add("reference", recordPanel1);
		tabbedPane.add("other", recordPanel2);
		GUIHelper.setEnabled(tabbedPane, false);

//		final ActionListener helpAction = evt -> helpAction();
		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> cancelAction();
		//TODO link to help
//		helpButton.addActionListener(helpAction);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[50%][50%]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow");
		add(tabbedPane, "span 1 4,grow,wrap paragraph");
		add(recordScrollPane, "grow,wrap related");
		add(newButton, "tag add,split 2,sizegroup button");
		add(deleteButton, "tag delete,sizegroup button,wrap paragraph");

		add(citationPanel, "grow,wrap paragraph");

		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	public final boolean loadData(final GedcomNode record, final Consumer<Object> onCloseGracefully){
		this.record = record;
		originalRecord = record.clone();
		this.onCloseGracefully = onCloseGracefully;

		final List<GedcomNode> records = extractRecords();

		final int size = records.size();
		if(size > 0){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			model.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode node = records.get(row);

				model.setValueAt(node.getID(), row, TABLE_INDEX_RECORD_ID);
				model.setValueAt(store.traverse(node, RECORD_NAME).getValue(), row, TABLE_INDEX_RECORD_NAME);
			}
		}

		return (size > 0);
	}

	private List<GedcomNode> extractRecords(){
		final List<GedcomNode> records = store.traverseAsList(record, RECORD_TAG_ARRAY);
		final int size = records.size();
		for(int i = 0; i < size; i ++){
			final String recordXRef = records.get(i).getXRef();
			final GedcomNode record = store.getRepository(recordXRef);
			records.set(i, record);
		}
		return records;
	}

	private void filterTableBy(final RepositoryDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			sorter = new TableRowSorter<>(model);
			recordTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	private void selectAction(){
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());
		final GedcomNode selectedRecord = getSelectedRecord();
		final GedcomNode selectedCitation = getSelectedCitation(selectedRecord.getID());
		if(store.traverse(selectedRecord, RECORD_CREATION).isEmpty())
			selectedRecord.addChild(
				store.create(RECORD_CREATION)
					.addChildValue(RECORD_DATE, now)
			);
		if(previouslySelectedRecord != null && previouslySelectedRecord.hashCode() != previouslySelectedRecordHash){
			//show note record dialog
			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
			changeNoteDialog.setTitle("Change note for repository " + previouslySelectedRecord.getID());
			changeNoteDialog.loadData(previouslySelectedRecord, dialog -> {
				previouslySelectedRecord = selectedRecord;
				previouslySelectedRecordHash = selectedRecord.hashCode() ^ selectedCitation.hashCode();
			});

			changeNoteDialog.setSize(450, 209);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
		else{
			previouslySelectedRecord = selectedRecord;
			previouslySelectedRecordHash = selectedRecord.hashCode() ^ selectedCitation.hashCode();
		}


		//fill citation panel:
		GUIHelper.setEnabled(citationPanel, true);
		final String location = store.traverse(selectedCitation, RECORD_LOCATION)
			.getValue();
		final List<GedcomNode> citationNotes = store.traverseAsList(selectedCitation, RECORD_NOTE_ARRAY);
		final String credibility = store.traverse(selectedCitation, RECORD_CREDIBILITY)
			.getValue();
		locationField.setText(location);
		GUIHelper.addBorder(citationNoteButton, !citationNotes.isEmpty(), Color.BLUE);
		credibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);


		//fill record panel:
		final String title = store.traverse(selectedRecord, RECORD_NAME)
			.getValue();
		final String author = store.traverse(selectedRecord, RECORD_AUTHOR)
			.getValue();
		final String publicationFacts = store.traverse(selectedRecord, RECORD_PUBLISHER)
			.getValue();
		final GedcomNode dateNode = store.traverse(selectedRecord, RECORD_DATE);
		final String date = dateNode.getValue();
		final String calendarXRef = store.traverse(dateNode, RECORD_CALENDAR)
			.getXRef();
		final String dateOriginalText = store.traverse(dateNode, RECORD_ORIGINAL_TEXT)
			.getValue();
		final String dateCredibility = store.traverse(dateNode, RECORD_CREDIBILITY)
			.getValue();
		final int dateCredibilityIndex = (dateCredibility != null? Integer.parseInt(dateCredibility): 0);
		final GedcomNode publicationPlace = store.traverse(selectedRecord, RECORD_PLACE);
		final List<GedcomNode> repositories = store.traverseAsList(selectedRecord, RECORD_REPOSITORY_ARRAY);
		final String mediaType = store.traverse(selectedRecord, RECORD_MEDIA_TYPE)
			.getValue();
		final List<GedcomNode> recordDocuments = store.traverseAsList(selectedRecord, RECORD_DOCUMENT_ARRAY);
		final List<GedcomNode> recordSources = store.traverseAsList(selectedRecord, RECORD_SOURCE_ARRAY);
		final List<GedcomNode> recordNotes = store.traverseAsList(selectedRecord, RECORD_NOTE_ARRAY);
		GUIHelper.setEnabled(tabbedPane, true);
		titleField.setText(title);
		authorField.setText(author);
		publisherField.setText(publicationFacts);
		datePanel.loadData(date, calendarXRef, dateOriginalText, dateCredibilityIndex);
		GUIHelper.addBorder(publicationPlaceButton, !publicationPlace.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(repositoryButton, !repositories.isEmpty(), Color.BLUE);
		mediaTypeField.setText(mediaType);
		GUIHelper.addBorder(documentButton, !recordDocuments.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(sourceButton, !recordSources.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(recordNoteButton, !recordNotes.isEmpty(), Color.BLUE);


		deleteButton.setEnabled(true);
	}

	private GedcomNode getSelectedRecord(){
		final int selectedRow = recordTable.getSelectedRow();
		final String recordID = (String)recordTable.getValueAt(selectedRow, TABLE_INDEX_RECORD_ID);
		return store.getRepository(recordID);
	}

	private GedcomNode getSelectedCitation(final String recordID){
		return store.traverse(record, RECORD_TAG_REFERENCE + recordID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final GedcomNode newRecord = store.create(RECORD_TAG);

		//add to store
		final String recordID = store.addRepository(newRecord);
		record.addChildReference(RECORD_TAG, recordID);

		//reset filter
		filterField.setText(null);

		//add to table
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int oldSize = model.getRowCount();
		model.setRowCount(oldSize + 1);
		model.setValueAt(newRecord.getID(), oldSize, TABLE_INDEX_RECORD_ID);
		//resort rows
		final RowSorter<? extends TableModel> recordTableSorter = recordTable.getRowSorter();
		recordTableSorter.setSortKeys(recordTableSorter.getSortKeys());

		//select the newly created record
		recordTable.setRowSelectionInterval(oldSize, oldSize);
		//make selected row visible
		recordTable.scrollRectToVisible(recordTable.getCellRect(oldSize, 0, true));
	}

	private void deleteAction(){
		GUIHelper.setEnabled(citationPanel, false);
		GUIHelper.setEnabled(tabbedPane, false);
		deleteButton.setEnabled(false);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int index = recordTable.getSelectedRow();
		final String recordID = (String)model.getValueAt(index, TABLE_INDEX_RECORD_ID);
		final GedcomNode selectedRecord;
		if(StringUtils.isBlank(recordID))
			selectedRecord = store.traverseAsList(record, RECORD_TAG_ARRAY)
				.get(index);
		else
			selectedRecord = store.getRepository(recordID);

		record.removeChild(selectedRecord);

		model.removeRow(index);


		//clear previously selected row
		previouslySelectedRecord = null;
	}

	private void okAction(){
		final String now = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now());

		if(store.traverse(record, RECORD_CREATION).isEmpty()){
			record.addChild(
				store.create(RECORD_CREATION)
					.addChildValue(RECORD_DATE, now)
			);

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			setVisible(false);
		}
		else{
			//show note record dialog
			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
			final GedcomNode selectedRecord = getSelectedRecord();
			changeNoteDialog.setTitle("Change note for repository " + selectedRecord.getID());
			changeNoteDialog.loadData(record, dialog -> {});

			changeNoteDialog.setSize(450, 209);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
	}

	private void cancelAction(){
		record.replaceWith(originalRecord);

		setVisible(false);
	}


	private static class RecordTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 3717450687790596773L;


		RecordTableModel(){
			super(new String[]{"ID", "Title"}, 0);
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


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.8.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode sourceCitation = store.traverseAsList(store.getIndividuals().get(0), "SOURCE[]").get(0);
		final GedcomNode container = store.getSource(sourceCitation.getXRef());

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
						case NOTE -> {
							final NoteDialog dialog = NoteDialog.createNote(store, parent);
							final GedcomNode repository = editCommand.getContainer();
							dialog.setTitle(repository.getID() != null
								? "Note " + repository.getID()
								: "New note for " + container.getID());
							dialog.loadData(repository, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 513);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_TRANSLATION -> {
							final NoteDialog dialog = NoteDialog.createNoteTranslation(store, parent);
							final GedcomNode noteTranslation = editCommand.getContainer();
							dialog.setTitle(StringUtils.isNotBlank(noteTranslation.getValue())
								? "Translation for language " + store.traverse(noteTranslation, "LOCALE").getValue()
								: "New translation"
							);
							dialog.loadData(noteTranslation, editCommand.getOnCloseGracefully());

							dialog.setSize(450, 209);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final RepositoryDialog dialog = new RepositoryDialog(store, parent);
							final GedcomNode repository = editCommand.getContainer();
							dialog.setTitle(repository.getID() != null
								? "Source for repository " + repository.getID()
								: "Source for new repository");
							if(!dialog.loadData(repository, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(946, 396);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case PLACE -> {
							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
							final GedcomNode repository = editCommand.getContainer();
							dialog.setTitle(repository.getID() != null
								? "Place for repository " + repository.getID()
								: "Place for new repository");
							if(!dialog.loadData(repository, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final RepositoryDialog dialog = new RepositoryDialog(store, parent);
			dialog.setTitle(container.getID() != null? "Repository for " + container.getID(): "Repository");
			if(!dialog.loadData(container, null))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(946, 369);
			dialog.setLocationRelativeTo(null);
			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
				@Override
				public void componentResized(java.awt.event.ComponentEvent e) {
					System.out.println("Resized to " + e.getComponent().getSize());
				}
			});
			dialog.setVisible(true);
		});
	}

}
