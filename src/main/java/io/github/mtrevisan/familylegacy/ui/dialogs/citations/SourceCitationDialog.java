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
package io.github.mtrevisan.familylegacy.ui.dialogs.citations;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.CropDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.SourceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.CredibilityComboBoxModel;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;


public class SourceCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 5873775240948872171L;

	private static final String RECORD_TAG = "SOURCE";
	private static final String ARRAY = "[]";
	private static final String REFERENCE = "@";
	private static final String RECORD_TAG_ARRAY = RECORD_TAG + ARRAY;
	private static final String RECORD_TITLE = "TITLE";
	private static final String RECORD_CREDIBILITY = "CREDIBILITY";
	private static final String RECORD_NOTE = "NOTE";
	private static final String RECORD_NOTE_ARRAY = RECORD_NOTE + ARRAY;
	private static final String RECORD_FILE = "FILE";
	private static final String RECORD_FILE_ARRAY = RECORD_FILE + ARRAY;
	private static final String RECORD_SOURCE = "SOURCE";
	private static final String RECORD_SOURCE_REFERENCE = RECORD_SOURCE + REFERENCE;
	private static final String RECORD_CROP = "CROP";
	private static final String RECORD_EVENT = "EVENT";
	private static final String RECORD_EVENT_ARRAY = RECORD_EVENT + ARRAY;
	private static final String RECORD_LOCATION = "LOCATION";
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
	private static final int TABLE_INDEX_RECORD_TYPE = 1;
	private static final int TABLE_INDEX_RECORD_TITLE = 2;
	private static final int TABLE_ROWS_SHOWN = 4;

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon ICON_CROP = ResourceHelper.getImage("/images/crop.png", 20, 20);
	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);
	private static final ImageIcon ICON_PLACE = ResourceHelper.getImage("/images/place.png", 20, 20);
	private static final ImageIcon ICON_REPOSITORY = ResourceHelper.getImage("/images/repository.png", 20, 20);

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane recordScrollPane = new JScrollPane(recordTable);
	private final JButton newButton = new JButton("New");
	private final JButton deleteButton = new JButton("Delete");
	private final JButton editButton = new JButton("Edit");

	private final JPanel citationPanel = new JPanel();
	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton cropButton = new JButton(ICON_CROP);
	private final JButton noteButton = new JButton(ICON_NOTE);
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());

	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<SourceCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode originalRecord;
	private GedcomNode record;
	private GedcomNode previouslySelectedRecord;
	private long previouslySelectedRecordHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public SourceCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		initCitationsComponents();

		initRecordComponents();

		initLayout();
	}

	private void initCitationsComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(SourceCitationDialog.this);
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
		sorter.setComparator(TABLE_INDEX_RECORD_TYPE, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_RECORD_TITLE, Comparator.naturalOrder());
		recordTable.setRowSorter(sorter);
		//clicking on a line links it to current source citation
		recordTable.getSelectionModel()
			.addListSelectionListener(evt -> {
				if(!evt.getValueIsAdjusting() && recordTable.getSelectedRow() >= 0)
					selectAction();
			});
		recordTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && recordTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
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
		editButton.setEnabled(false);
		editButton.addActionListener(evt -> editAction());
	}

	private void initRecordComponents(){
		//citation part:
		locationLabel.setLabelFor(locationField);

		roleLabel.setLabelFor(roleField);

		cropButton.setToolTipText("Define a crop");
		cropButton.addActionListener(evt -> cropAction());
		cropButton.setEnabled(false);

		noteButton.setToolTipText("Add note");
		final ActionListener addNoteAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> notes = store.traverseAsList(record, RECORD_NOTE_ARRAY);
				GUIHelper.addBorderIfDataPresent(noteButton, !notes.isEmpty());

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, record, onAccept));
		};
		noteButton.addActionListener(addNoteAction);
		noteButton.setEnabled(false);

		credibilityLabel.setLabelFor(credibilityComboBox);
	}

	private void initLayout(){
		citationPanel.setBorder(BorderFactory.createTitledBorder("Citation"));
		citationPanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		citationPanel.add(locationLabel, "align label,sizegroup label,split 2");
		citationPanel.add(locationField, "grow,wrap");
		citationPanel.add(roleLabel, "align label,sizegroup label,split 2");
		citationPanel.add(roleField, "grow,wrap");
		citationPanel.add(cropButton, "sizegroup button,split 2,center");
		citationPanel.add(noteButton, "sizegroup button,center,wrap");
		citationPanel.add(credibilityLabel, "align label,sizegroup label,split 2");
		citationPanel.add(credibilityComboBox);
		GUIHelper.setEnabled(citationPanel, false);

//		final ActionListener helpAction = evt -> helpAction();
		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> cancelAction();
		//TODO link to help
//		helpButton.addActionListener(helpAction);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(recordScrollPane, "grow,wrap related");
		add(newButton, "tag add,split 3,sizegroup button");
		add(deleteButton, "tag delete,sizegroup button");
		add(editButton, "tag edit,sizegroup button,gap rel:push,wrap paragraph");

		add(citationPanel, "grow,wrap paragraph");

		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	//TODO
	private void cropAction(){
		final GedcomNode selectedRecord = getSelectedRecord();
		final GedcomNode selectedCitation = getSelectedCitation(selectedRecord.getID());
		final List<GedcomNode> documents = store.traverseAsList(selectedRecord, RECORD_FILE_ARRAY);
		final String imagePath = documents.get(0)
			.getValue();
		final String cropCoordinates = store.traverse(selectedCitation, RECORD_CROP)
			.getValue();
		final GedcomNode imageData = store.create(RECORD_FILE)
			.addChildValue(RECORD_SOURCE, imagePath)
			.addChildValue(RECORD_CROP, cropCoordinates);

		final Consumer<Object> onCloseGracefully = cropDialog -> {
			final Point cropStartPoint = ((CropDialog)cropDialog).getCropStartPoint();
			final Point cropEndPoint = ((CropDialog)cropDialog).getCropEndPoint();
			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			sj.add(Integer.toString(cropStartPoint.x));
			sj.add(Integer.toString(cropStartPoint.y));
			sj.add(Integer.toString(cropEndPoint.x));
			sj.add(Integer.toString(cropEndPoint.y));

			selectedRecord.removeChildrenWithTag(RECORD_CROP);
			selectedRecord.addChildValue(RECORD_CROP, sj.toString());

			//refresh group list
//			loadData();
		};

		//fire image crop event
		EventBusService.publish(new EditEvent(EditEvent.EditType.CROP, imageData, onCloseGracefully));
	}

	private boolean sourceContainsEvent(final String event){
		final GedcomNode selectedRecord = getSelectedRecord();
		final List<GedcomNode> events = store.traverseAsList(selectedRecord, RECORD_EVENT_ARRAY);

		boolean containsEvent = false;
		for(int i = 0; !containsEvent && i < events.size(); i ++)
			if(events.get(i).getValue().equalsIgnoreCase(event))
				containsEvent = true;
		return containsEvent;
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
				final List<GedcomNode> events = store.traverseAsList(node, RECORD_EVENT_ARRAY);
				final String[] eventTags = new String[events.size()];
				for(int i = 0; i < events.size(); i ++)
					eventTags[i] = events.get(i)
						.getValue();
				model.setValueAt(String.join(", ", eventTags), row, TABLE_INDEX_RECORD_TYPE);
				model.setValueAt(store.traverse(node, RECORD_TITLE).getValue(), row, TABLE_INDEX_RECORD_TITLE);
			}
		}

		return (size > 0);
	}

	private List<GedcomNode> extractRecords(){
		final List<GedcomNode> records = store.traverseAsList(record, RECORD_TAG_ARRAY);
		final int size = records.size();
		for(int i = 0; i < size; i ++)
			records.set(i, store.getSource(records.get(i).getXRef()));
		return records;
	}

	private void filterTableBy(final SourceCitationDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_TYPE, TABLE_INDEX_RECORD_TITLE);

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
			changeNoteDialog.setTitle("Change note for source " + previouslySelectedRecord.getID());
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
		final String role = store.traverse(selectedCitation, RECORD_ROLE)
			.getValue();
		final List<GedcomNode> notes = store.traverseAsList(selectedCitation, RECORD_NOTE_ARRAY);
		final String credibility = store.traverse(selectedCitation, RECORD_CREDIBILITY)
			.getValue();
		locationField.setText(location);
		roleField.setText(role);
		final List<GedcomNode> documents = store.traverseAsList(selectedRecord, RECORD_FILE_ARRAY);
		//only if there is one image
		cropButton.setEnabled(documents.size() == 1);
		GUIHelper.addBorderIfDataPresent(noteButton, !notes.isEmpty());
		credibilityComboBox.setSelectedIndex(credibility != null && !credibility.isEmpty()? Integer.parseInt(credibility) + 1: 0);


		deleteButton.setEnabled(true);
		editButton.setEnabled(true);
	}

	private GedcomNode getSelectedRecord(){
		final int selectedRow = recordTable.getSelectedRow();
		final String recordID = (String)recordTable.getValueAt(selectedRow, TABLE_INDEX_RECORD_ID);
		return store.getSource(recordID);
	}

	private GedcomNode getSelectedCitation(final String recordID){
		return store.traverse(record, RECORD_SOURCE_REFERENCE + recordID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final GedcomNode newRecord = store.create(RECORD_TAG);

		//add to store
		final String recordID = store.addSource(newRecord);
		record.addChildReference(RECORD_TAG, recordID);

		//reset filter
		filterField.setText(null);

		//add to table
		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int oldSize = model.getRowCount();
		model.setRowCount(oldSize + 1);
		model.setValueAt(newRecord.getID(), oldSize, TABLE_INDEX_RECORD_ID);

		//select the newly created record
		recordTable.setRowSelectionInterval(oldSize, oldSize);
		//make selected row visible
		recordTable.scrollRectToVisible(recordTable.getCellRect(oldSize, 0, true));
	}

	private void deleteAction(){
		GUIHelper.setEnabled(citationPanel, false);
		deleteButton.setEnabled(false);
		editButton.setEnabled(false);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int index = recordTable.getSelectedRow();
		final String recordID = (String)model.getValueAt(index, TABLE_INDEX_RECORD_ID);
		final GedcomNode selectedRecord;
		if(StringUtils.isBlank(recordID))
			selectedRecord = store.traverseAsList(record, RECORD_TAG_ARRAY)
				.get(index);
		else
			selectedRecord = store.getSource(recordID);

		record.removeChild(selectedRecord);

		model.removeRow(index);


		//clear previously selected row
		previouslySelectedRecord = null;
	}

	private void editAction(){
		final GedcomNode selectedRecord = getSelectedRecord();
		final Consumer<Object> onCloseGracefully = ignored -> {
			//TODO
System.out.println("edit " + selectedRecord.getID());
			final List<GedcomNode> notes = store.traverseAsList(record, RECORD_NOTE_ARRAY);
			GUIHelper.addBorderIfDataPresent(noteButton, !notes.isEmpty());

			//put focus on the ok button
			okButton.grabFocus();
		};

		//fire image crop event
		EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, selectedRecord, onCloseGracefully));
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
			changeNoteDialog.setTitle("Change note for source " + selectedRecord.getID());
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
			super(new String[]{"ID", "Type", "Title"}, 0);
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
						case CROP -> {
							try{
								final CropDialog dialog = new CropDialog(parent);
								final GedcomNode imageData = editCommand.getContainer();
								final String imagePath = store.traverse(imageData, RECORD_SOURCE)
									.getValue();
								final String cropCoordinates = store.traverse(imageData, RECORD_CROP)
									.getValue();
								final String[] coordinates = (!cropCoordinates.isEmpty()
									? StringUtils.split(cropCoordinates, ' ')
									: null);
								dialog.loadData(new File(store.getBasePath(), imagePath), editCommand.getOnCloseGracefully());
								if(coordinates != null){
									dialog.setCropStartPoint(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
									dialog.setCropEndPoint(Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3]));
								}

								dialog.setSize(500, 480);
								dialog.setLocationRelativeTo(parent);
								dialog.setVisible(true);
							}
							catch(final IOException ioe){
								ioe.printStackTrace();
							}
						}
						case NOTE -> {
							final NoteDialog dialog = NoteDialog.createNote(store, parent);
							final GedcomNode source = editCommand.getContainer();
							dialog.setTitle(source.getID() != null
								? "Note " + source.getID()
								: "New note for " + container.getID());
							dialog.loadData(source, editCommand.getOnCloseGracefully());

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
							final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Source for source " + note.getID()
								: "Source for new source");
							//TODO
//							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
//								dialog.showNewRecord();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case PLACE -> {
							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Place for source " + note.getID()
								: "Place for new source");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.showNewRecord();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case REPOSITORY -> {
							final RepositoryCitationDialog dialog = new RepositoryCitationDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Repository for source " + note.getID()
								: "Repository for new source");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.addAction();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
			dialog.setTitle(container.getID() != null? "Source for " + container.getID(): "Source");
			if(!dialog.loadData(container, null))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(464, 410);
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
