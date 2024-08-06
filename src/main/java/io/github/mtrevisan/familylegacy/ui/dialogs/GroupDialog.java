/**
 * Copyright (c) 2022 Mauro Trevisan
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


public class GroupDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -3846833238043091625L;

	private static final String RECORD_TAG = "GROUP";
	private static final String ARRAY = "[]";
	private static final String REFERENCE = "@";
	private static final String RECORD_TAG_REFERENCE = RECORD_TAG + REFERENCE;
	private static final String RECORD_TAG_ARRAY = RECORD_TAG + ARRAY;
	private static final String RECORD_NAME = "NAME";
	private static final String RECORD_TYPE = "TYPE";
	private static final String RECORD_CREDIBILITY = "CREDIBILITY";
	private static final String RECORD_EVENT = "EVENT";
	private static final String RECORD_EVENT_ARRAY = RECORD_EVENT + ARRAY;
	private static final String RECORD_NOTE = "NOTE";
	private static final String RECORD_NOTE_ARRAY = RECORD_NOTE + ARRAY;
	private static final String RECORD_SOURCE = "SOURCE";
	private static final String RECORD_SOURCE_ARRAY = RECORD_SOURCE + ARRAY;
	private static final String RECORD_INDIVIDUALS_ARRAY = "INDIVIDUAL" + ARRAY;
	private static final String RECORD_FAMILY_ARRAY = "FAMILY" + ARRAY;
	private static final String RECORD_ROLE = "ROLE";
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
	private static final ImageIcon ICON_EVENT = ResourceHelper.getImage("/images/event.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane recordScrollPane = new JScrollPane(recordTable);
	private final JButton newButton = new JButton("New");
	private final JButton deleteButton = new JButton("Delete");

	private final JPanel citationPanel = new JPanel();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton citationNoteButton = new JButton(ICON_NOTE);
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JPanel recordPanel = new JPanel(new MigLayout());
	private final JLabel nameLabel = new JLabel("Name:");
	private final JTextField nameField = new JTextField();
	private final JLabel typeLabel = new JLabel("Type:");
	private final JTextField typeField = new JTextField();
	private final JButton individualButton = new JButton("Individuals");
	private final JButton familyButton = new JButton("Families");
	private final JButton eventButton = new JButton(ICON_EVENT);
	private final JButton recordNoteButton = new JButton(ICON_NOTE);
	private final JButton sourceButton = new JButton(ICON_SOURCE);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");

	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<GroupDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode originalRecord;
	private GedcomNode record;
	private GedcomNode previouslySelectedRecord;
	private long previouslySelectedRecordHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public GroupDialog(final Flef store, final Frame parent){
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
			@Override
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(GroupDialog.this);
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
		//clicking on a line links it to current group citation
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
		roleLabel.setLabelFor(roleField);

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

		credibilityLabel.setLabelFor(credibilityComboBox);
		credibilityComboBox.addActionListener(evt -> {
			final GedcomNode selectedRecord = getSelectedRecord();
			final int selectedIndex = credibilityComboBox.getSelectedIndex();
			if(selectedIndex > 0){
				final String credibility = Integer.toString(selectedIndex - 1);
				final GedcomNode credibilityNode = store.traverse(selectedRecord, RECORD_CREDIBILITY);
				if(credibilityNode.isEmpty())
					selectedRecord.addChildValue(RECORD_CREDIBILITY, credibility);
				else
					credibilityNode.withValue(credibility);
			}
			else
				selectedRecord.removeChildrenWithTag(RECORD_CREDIBILITY);
		});


		//record part:
		nameLabel.setLabelFor(nameField);

		typeLabel.setLabelFor(typeField);

		individualButton.setToolTipText("Place of publication");
		individualButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, getSelectedRecord())));

		familyButton.setToolTipText("Repository");
		familyButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, getSelectedRecord())));

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

		eventButton.setToolTipText("Add record event");
		final ActionListener addRecordEventAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> events = store.traverseAsList(record, RECORD_EVENT_ARRAY);
				GUIHelper.addBorder(eventButton, !events.isEmpty(), Color.BLUE);

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.EVENT, record, onAccept));
		};
		eventButton.addActionListener(addRecordEventAction);

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
	}

	private void initLayout(){
		citationPanel.setBorder(BorderFactory.createTitledBorder("Citation"));
		citationPanel.setLayout(new MigLayout());
		citationPanel.add(roleLabel, "align label,sizegroup label,split 2");
		citationPanel.add(roleField, "grow,wrap");
		citationPanel.add(citationNoteButton, "sizegroup button,center,wrap");
		citationPanel.add(credibilityLabel, "align label,sizegroup label,split 2");
		citationPanel.add(credibilityComboBox);
		GUIHelper.setEnabled(citationPanel, false);

		recordPanel.setBorder(BorderFactory.createTitledBorder("Record"));
		recordPanel.add(nameLabel, "align label,sizegroup label,split 2");
		recordPanel.add(nameField, "grow,wrap");
		recordPanel.add(typeLabel, "align label,sizegroup label,split 2");
		recordPanel.add(typeField, "grow,wrap");
		recordPanel.add(individualButton, "split 2,center");
		recordPanel.add(familyButton, "center,wrap");
		recordPanel.add(eventButton, "split 3,center");
		recordPanel.add(recordNoteButton, "center");
		recordPanel.add(sourceButton, "center,wrap");
		recordPanel.add(restrictionCheckBox, "wrap");
		GUIHelper.setEnabled(recordPanel, false);

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
		add(recordPanel, "span 1 4,grow,wrap paragraph");
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
			final GedcomNode record = store.getGroup(recordXRef);
			records.set(i, record);
		}
		return records;
	}

	private void filterTableBy(final GroupDialog panel){
		final String name = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(name, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_NAME);

		@SuppressWarnings("unchecked")
		final TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)recordTable.getRowSorter();
//		if(sorter == null){
//			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
//			sorter = new TableRowSorter<>(model);
//			recordTable.setRowSorter(sorter);
//		}
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
			changeNoteDialog.setTitle("Change note for group " + previouslySelectedRecord.getID());
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
		final String role = store.traverse(selectedCitation, RECORD_ROLE)
			.getValue();
		final List<GedcomNode> citationNotes = store.traverseAsList(selectedCitation, RECORD_NOTE_ARRAY);
		final String credibility = store.traverse(selectedCitation, RECORD_CREDIBILITY)
			.getValue();
		roleField.setText(role);
		GUIHelper.addBorder(citationNoteButton, !citationNotes.isEmpty(), Color.BLUE);
		credibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);


		//fill record panel:
		final String name = store.traverse(selectedRecord, RECORD_NAME)
			.getValue();
		final String type = store.traverse(selectedRecord, RECORD_TYPE)
			.getValue();
		final List<GedcomNode> individuals = store.traverseAsList(selectedRecord, RECORD_INDIVIDUALS_ARRAY);
		final List<GedcomNode> families = store.traverseAsList(selectedRecord, RECORD_FAMILY_ARRAY);
		final List<GedcomNode> recordEvents = store.traverseAsList(selectedRecord, RECORD_EVENT_ARRAY);
		final List<GedcomNode> recordNotes = store.traverseAsList(selectedRecord, RECORD_NOTE_ARRAY);
		final List<GedcomNode> recordSources = store.traverseAsList(selectedRecord, RECORD_SOURCE_ARRAY);
		GUIHelper.setEnabled(recordPanel, true);
		nameField.setText(name);
		typeField.setText(type);
		GUIHelper.addBorder(individualButton, !individuals.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(familyButton, !families.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(eventButton, !recordEvents.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(recordNoteButton, !recordNotes.isEmpty(), Color.BLUE);
		GUIHelper.addBorder(sourceButton, !recordSources.isEmpty(), Color.BLUE);


		deleteButton.setEnabled(true);
	}

	private GedcomNode getSelectedRecord(){
		final int selectedRow = recordTable.getSelectedRow();
		final String recordID = (String)recordTable.getValueAt(selectedRow, TABLE_INDEX_RECORD_ID);
		return store.getGroup(recordID);
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
		final String recordID = store.addGroup(newRecord);
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
		GUIHelper.setEnabled(recordPanel, false);
		deleteButton.setEnabled(false);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int index = recordTable.getSelectedRow();
		final String recordID = (String)model.getValueAt(index, TABLE_INDEX_RECORD_ID);
		final GedcomNode selectedRecord;
		if(StringUtils.isBlank(recordID))
			selectedRecord = store.traverseAsList(record, RECORD_TAG_ARRAY)
				.get(index);
		else
			selectedRecord = store.getGroup(recordID);

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
			changeNoteDialog.setTitle("Change note for group " + selectedRecord.getID());
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
		private static final long serialVersionUID = -7463243250214703789L;


		RecordTableModel(){
			super(new String[]{"ID", "Name"}, 0);
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
		final GedcomNode container = store.getIndividuals().get(0);

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
							final GedcomNode group = editCommand.getContainer();
							dialog.setTitle(group.getID() != null
								? "Note " + group.getID()
								: "New note for " + container.getID());
							dialog.loadData(group, editCommand.getOnCloseGracefully());

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
							final GroupDialog dialog = new GroupDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Source for group " + note.getID()
								: "Source for new group");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(946, 396);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case PLACE -> {
							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Place for group " + note.getID()
								: "Place for new group");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case REPOSITORY -> {
							final RepositoryDialog dialog = new RepositoryDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Repository for group " + note.getID()
								: "Repository for new group");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(946, 396);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupDialog dialog = new GroupDialog(store, parent);
			dialog.setTitle(container.getID() != null? "Group for " + container.getID(): "Group");
			if(!dialog.loadData(container, null))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(905, 396);
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
