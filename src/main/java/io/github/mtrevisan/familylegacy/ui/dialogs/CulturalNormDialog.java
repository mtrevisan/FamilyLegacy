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

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.SourceCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.CertaintyComboBoxModel;
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


public class CulturalNormDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 3322392561648823462L;

	private static final String RECORD_TAG = "CULTURAL_NORM";
	private static final String ARRAY = "[]";
	private static final String RECORD_TAG_ARRAY = RECORD_TAG + ARRAY;
	private static final String RECORD_TITLE = "TITLE";
	private static final String RECORD_CERTAINTY = "CERTAINTY";
	private static final String RECORD_CREDIBILITY = "CREDIBILITY";
	private static final String RECORD_NOTE = "NOTE";
	private static final String RECORD_NOTE_ARRAY = RECORD_NOTE + ARRAY;
	private static final String RECORD_SOURCE_ARRAY = "SOURCE" + ARRAY;
	private static final String RECORD_PLACE = "PLACE";
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
	private static final int TABLE_INDEX_RECORD_TITLE = 1;
	private static final int TABLE_ROWS_SHOWN = 4;

	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane recordScrollPane = new JScrollPane(recordTable);
	private final JButton newButton = new JButton("New");
	private final JButton deleteButton = new JButton("Delete");

	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JPanel placePanel = new JPanel();
	private final JButton placeButton = new JButton("Place");
	private final JLabel placeCertaintyLabel = new JLabel("Certainty:");
	private final JComboBox<String> placeCertaintyComboBox = new JComboBox<>(new CertaintyComboBoxModel());
	private final JLabel placeCredibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> placeCredibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
	private final JButton noteButton = new JButton(ICON_NOTE);
	private final JButton sourceButton = new JButton(ICON_SOURCE);

	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<CulturalNormDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode originalRecord;
	private GedcomNode record;
	private GedcomNode previouslySelectedRecord;
	private long previouslySelectedRecordHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public CulturalNormDialog(final Flef store, final Frame parent){
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
				filterDebouncer.call(CulturalNormDialog.this);
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
		sorter.setComparator(TABLE_INDEX_RECORD_TITLE, Comparator.naturalOrder());
		recordTable.setRowSorter(sorter);
		//clicking on a line links it to current source citation
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
		GUIHelper.bindLabelTextChangeUndo(titleLabel, titleField, evt -> {
			final String newTitle = titleField.getText();
			if(StringUtils.isNotBlank(newTitle)){
				final GedcomNode selectedRecord = getSelectedRecord();
				final GedcomNode titleNode = store.traverse(selectedRecord, RECORD_TITLE);
				if(titleNode.isEmpty())
					selectedRecord.addChildValue(RECORD_TITLE, newTitle);
				else
					titleNode.withValue(newTitle);

				//update table
				final int selectedRow = recordTable.getSelectedRow();
				recordTable.setValueAt(newTitle, selectedRow, TABLE_INDEX_RECORD_TITLE);
			}
		});
		GUIHelper.setEnabled(titleLabel, false);

		placeCertaintyLabel.setLabelFor(placeCertaintyComboBox);
		placeCertaintyComboBox.addActionListener(evt -> {
			final GedcomNode selectedRecord = getSelectedRecord();
			final int selectedIndex = placeCertaintyComboBox.getSelectedIndex();
			if(selectedIndex > 0){
				final String certainty = Integer.toString(selectedIndex - 1);
				final GedcomNode certaintyNode = store.traverse(selectedRecord, RECORD_CERTAINTY);
				if(certaintyNode.isEmpty())
					selectedRecord.addChildValue(RECORD_CERTAINTY, certainty);
				else
					certaintyNode.withValue(certainty);
			}
			else
				selectedRecord.removeChildrenWithTag(RECORD_CERTAINTY);
		});

		placeCredibilityLabel.setLabelFor(placeCredibilityComboBox);
		placeCredibilityComboBox.addActionListener(evt -> {
			final GedcomNode selectedRecord = getSelectedRecord();
			final GedcomNode selectedRecordPlace = store.traverse(selectedRecord, RECORD_PLACE);
			final int selectedIndex = placeCredibilityComboBox.getSelectedIndex();
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

		placeButton.addActionListener(e -> {
			final GedcomNode selectedRecord = getSelectedRecord();

			EventBusService.publish(new EditEvent(EditEvent.EditType.PLACE, selectedRecord));
		});
		placePanel.setBorder(BorderFactory.createTitledBorder("Place"));
		placePanel.setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		placePanel.add(placeButton, "sizegroup button,wrap");
		placePanel.add(placeCertaintyLabel, "align label,split 2");
		placePanel.add(placeCertaintyComboBox, "wrap");
		placePanel.add(placeCredibilityLabel, "align label,split 2");
		placePanel.add(placeCredibilityComboBox);
		GUIHelper.setEnabled(placePanel, false);

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

		sourceButton.setToolTipText("Add source");
		final ActionListener addSourceAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> sources = store.traverseAsList(record, RECORD_SOURCE_ARRAY);
				GUIHelper.addBorderIfDataPresent(sourceButton, !sources.isEmpty());

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, record, onAccept));
		};
		sourceButton.addActionListener(addSourceAction);
		sourceButton.setEnabled(false);
	}

	private void initLayout(){
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
		add(newButton, "tag add,split 2,sizegroup button");
		add(deleteButton, "tag delete,sizegroup button,wrap paragraph");

		add(titleLabel, "align label,sizegroup label,split 2");
		add(titleField, "grow,wrap");
		add(placePanel, "grow,wrap");
		add(noteButton, "sizegroup button,split 2,center");
		add(sourceButton, "sizegroup button,center,wrap paragraph");

		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	public final boolean loadData(final GedcomNode record, final Consumer<Object> onCloseGracefully){
		this.record = record;
		this.originalRecord = record.clone();
		this.onCloseGracefully = onCloseGracefully;

		final List<GedcomNode> records = extractRecords();

		final int size = records.size();
		if(size > 0){
			final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
			model.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode node = records.get(row);

				model.setValueAt(node.getID(), row, TABLE_INDEX_RECORD_ID);
				model.setValueAt(store.traverse(node, RECORD_TITLE).getValue(), row, TABLE_INDEX_RECORD_TITLE);
			}
		}

		return (size > 0);
	}

	private List<GedcomNode> extractRecords(){
		final List<GedcomNode> records = store.traverseAsList(record, RECORD_TAG_ARRAY);
		final int size = records.size();
		for(int i = 0; i < size; i ++)
			records.set(i, store.getCulturalNorm(records.get(i).getXRef()));
		return records;
	}

	private void filterTableBy(final CulturalNormDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID, TABLE_INDEX_RECORD_TITLE);

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
		if(store.traverse(selectedRecord, RECORD_CREATION).isEmpty())
			selectedRecord.addChild(
				store.create(RECORD_CREATION)
					.addChildValue(RECORD_DATE, now)
			);
		if(previouslySelectedRecord != null && previouslySelectedRecord.hashCode() != previouslySelectedRecordHash){
			//show note record dialog
			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
			changeNoteDialog.setTitle("Change note for cultural norm " + previouslySelectedRecord.getID());
			changeNoteDialog.loadData(previouslySelectedRecord, dialog -> {
				previouslySelectedRecord = selectedRecord;
				previouslySelectedRecordHash = selectedRecord.hashCode();
			});
//			changeNoteDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
//				@Override
//				public void componentResized(java.awt.event.ComponentEvent e) {
//					System.out.println("Resized to " + e.getComponent().getSize());
//				}
//			});

			changeNoteDialog.setSize(450, 209);
			changeNoteDialog.setLocationRelativeTo(this);
			changeNoteDialog.setVisible(true);
		}
		else{
			previouslySelectedRecord = selectedRecord;
			previouslySelectedRecordHash = selectedRecord.hashCode();
		}


		GUIHelper.setEnabled(titleLabel, true);
		titleField.setText(store.traverse(selectedRecord, RECORD_TITLE).getValue());

		GUIHelper.setEnabled(placePanel, true);
		final GedcomNode place = store.traverse(selectedRecord, RECORD_PLACE);
		GUIHelper.addBorderIfDataPresent(placeButton, !place.isEmpty());
		final String certainty = store.traverse(place, RECORD_CERTAINTY)
			.getValue();
		placeCertaintyComboBox.setSelectedIndex(certainty != null? Integer.parseInt(certainty) + 1: 0);
		final String credibility = store.traverse(place, RECORD_CREDIBILITY)
			.getValue();
		placeCredibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);

		final List<GedcomNode> notes = store.traverseAsList(selectedRecord, RECORD_NOTE_ARRAY);
		GUIHelper.addBorderIfDataPresent(noteButton, !notes.isEmpty());
		noteButton.setEnabled(true);

		final List<GedcomNode> sources = store.traverseAsList(selectedRecord, RECORD_SOURCE_ARRAY);
		GUIHelper.addBorderIfDataPresent(sourceButton, !sources.isEmpty());
		sourceButton.setEnabled(true);


		deleteButton.setEnabled(true);
	}

	private GedcomNode getSelectedRecord(){
		final int selectedRow = recordTable.getSelectedRow();
		final String recordID = (String)recordTable.getValueAt(selectedRow, TABLE_INDEX_RECORD_ID);
		return store.getCulturalNorm(recordID);
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final GedcomNode newRecord = store.create(RECORD_TAG);

		//add to store
		final String recordID = store.addCulturalNorm(newRecord);
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
		GUIHelper.setEnabled(titleLabel, false);
		GUIHelper.setEnabled(placePanel, false);
		noteButton.setEnabled(false);
		sourceButton.setEnabled(false);
		deleteButton.setEnabled(false);

		final DefaultTableModel model = (DefaultTableModel)recordTable.getModel();
		final int index = recordTable.getSelectedRow();
		final String recordID = (String)model.getValueAt(index, TABLE_INDEX_RECORD_ID);
		final GedcomNode selectedRecord;
		if(StringUtils.isBlank(recordID))
			selectedRecord = store.traverseAsList(record, RECORD_TAG_ARRAY)
				.get(index);
		else
			selectedRecord = store.getCulturalNorm(recordID);

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
			changeNoteDialog.setTitle("Change note for cultural norm " + selectedRecord.getID());
			changeNoteDialog.loadData(record, dialog -> {});

			changeNoteDialog.setSize(450, 500);
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
		private static final long serialVersionUID = -581310490684534579L;


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
						case PLACE -> {
							final PlaceRecordDialog dialog = new PlaceRecordDialog(store, parent);
							final GedcomNode place = editCommand.getContainer();
							dialog.setTitle(place.getID() != null
								? "Place " + place.getID()
								: "New place for " + container.getID());
							dialog.loadData(place, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 470);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteDialog dialog = NoteDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Note " + note.getID()
								: "New note for " + container.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

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

							dialog.setSize(500, 330);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_TRANSLATION_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteTranslationCitation(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Translation citations for note " + note.getID()
								: "Translation citations for new note");
							if(!dialog.loadData(note, editCommand.getOnCloseGracefully()))
								dialog.addAction();

							dialog.setSize(550, 450);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case SOURCE -> {
							final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Source citations for note " + note.getID()
								: "Source citations for new note");
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

			final CulturalNormDialog dialog = new CulturalNormDialog(store, parent);
			dialog.setTitle(container.getID() != null? "Cultural norm for " + container.getID(): "Cultural norm");
			if(!dialog.loadData(container, null))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(464, 446);
			dialog.setLocationRelativeTo(null);
//			dialog.addComponentListener(new java.awt.event.ComponentAdapter() {
//				@Override
//				public void componentResized(java.awt.event.ComponentEvent e) {
//					System.out.println("Resized to " + e.getComponent().getSize());
//				}
//			});
			dialog.setVisible(true);
		});
	}

}
