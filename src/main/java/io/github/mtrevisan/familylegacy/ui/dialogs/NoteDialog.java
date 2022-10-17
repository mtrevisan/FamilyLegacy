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
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import java.util.Locale;
import java.util.function.Consumer;


public class NoteDialog extends JDialog implements TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = - 7902103855362510456L;

	private enum NoteType{DEFAULT, TRANSLATION, UPDATE}

	private static final String RECORD_TAG = "NOTE";
	private static final String ARRAY = "[]";
	private static final String RECORD_TAG_ARRAY = RECORD_TAG + ARRAY;
	private static final String RECORD_LOCALE = "LOCALE";
	private static final String RECORD_RESTRICTION = "RESTRICTION";
	private static final String RECORD_NOTE = "NOTE";
	private static final String RECORD_TRANSLATION_ARRAY = "TRANSLATION" + ARRAY;
	private static final String RECORD_SOURCE_ARRAY = "SOURCE" + ARRAY;
	private static final String RECORD_CREATION = "CREATION";
	private static final String RECORD_DATE = "DATE";
	private static final String RECORD_UPDATE = "UPDATE";
	private static final String ACTION_MAP_KEY_INSERT = "insert";
	private static final String ACTION_MAP_KEY_DELETE = "delete";

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);
	private static final KeyStroke DELETE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int TABLE_PREFERRED_WIDTH_RECORD_ID = 25;
	private static final int TABLE_PREFERRED_WIDTH_RECORD_LANGUAGE = 65;

	private static final int TABLE_INDEX_RECORD_ID = 0;
	private static final int TABLE_INDEX_RECORD_LANGUAGE = 1;
	private static final int TABLE_INDEX_RECORD_TEXT = 2;
	private static final int TABLE_ROWS_SHOWN = 4;

	private static final ImageIcon ICON_TRANSLATION = ResourceHelper.getImage("/images/translation.png", 20, 20);
	private static final ImageIcon ICON_SOURCE = ResourceHelper.getImage("/images/source.png", 20, 20);

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable recordTable = new JTable(new RecordTableModel());
	private final JScrollPane recordScrollPane = new JScrollPane(recordTable);
	private final JButton newButton = new JButton("New");
	private final JButton deleteButton = new JButton("Delete");

	private TextPreviewPane textPreviewView;
	private final JLabel localeLabel = new JLabel("Locale:");
	private final LocaleComboBox localeComboBox = new LocaleComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton translationButton = new JButton(ICON_TRANSLATION);
	private final JButton sourceButton = new JButton(ICON_SOURCE);

	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<NoteDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode originalRecord;
	private GedcomNode record;

	private NoteType noteType;
	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public static NoteDialog createNote(final Flef store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.noteType = NoteType.DEFAULT;
		dialog.initCitationsComponents();
		dialog.initRecordComponents();
		dialog.initLayout();
		return dialog;
	}

	static NoteDialog createNoteTranslation(final Flef store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.noteType = NoteType.TRANSLATION;
		dialog.initUpdateComponents();
		dialog.initUpdateLayout();
		return dialog;
	}

	static NoteDialog createUpdateNote(final Flef store, final Frame parent){
		final NoteDialog dialog = new NoteDialog(store, parent);
		dialog.noteType = NoteType.UPDATE;
		dialog.initUpdateComponents();
		dialog.initUpdateLayout();
		return dialog;
	}

	private NoteDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;
	}

	private void initCitationsComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(NoteDialog.this);
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
		TableHelper.setColumnWidth(recordTable, TABLE_INDEX_RECORD_LANGUAGE, 0, TABLE_PREFERRED_WIDTH_RECORD_LANGUAGE);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(recordTable.getModel());
		sorter.setComparator(TABLE_INDEX_RECORD_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_RECORD_LANGUAGE, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_RECORD_TEXT, Comparator.naturalOrder());
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
		textPreviewView = TextPreviewPane.createWithPreview(this);
		textPreviewView.setEnabled(false);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());
		GUIHelper.setEnabled(localeLabel, false);

		restrictionCheckBox.addActionListener(evt -> textChanged());
		restrictionCheckBox.setEnabled(false);

		translationButton.setToolTipText("Add translation");
		final ActionListener addTranslationAction = evt -> {
			final Consumer<Object> onAccept = ignored -> {
				final List<GedcomNode> translations = store.traverseAsList(record, RECORD_TRANSLATION_ARRAY);
				GUIHelper.addBorderIfDataPresent(translationButton, !translations.isEmpty());

				//put focus on the ok button
				okButton.grabFocus();
			};

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_TRANSLATION_CITATION, record, onAccept));
		};
		translationButton.addActionListener(addTranslationAction);
		translationButton.setEnabled(false);

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

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow][]"));
		add(filterLabel, "align label,spanx 3,split 2");
		add(filterField, "grow,wrap");
		add(recordScrollPane, "growx,spanx 3,wrap related");
		add(newButton, "tag add,spanx 3,split 2,sizegroup button");
		add(deleteButton, "tag delete,sizegroup button,wrap paragraph");

		//FIXME
//		add(textPreviewView, "spany 2,grow");
		add(textPreviewView, "spany 2,height 188!,grow");
		add(translationButton, "tag add,sizegroup button,wrap");
		add(sourceButton, "tag add,top,sizegroup button,wrap");
		add(localeLabel, "align label,spanx 3,split 2,sizegroup label");
		add(localeComboBox, "wrap");
		add(restrictionCheckBox, "spanx 3,wrap paragraph");

		add(helpButton, "tag help2,spanx 3,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void initUpdateComponents(){
		textPreviewView = TextPreviewPane.createWithoutPreview(this);

		localeLabel.setLabelFor(localeComboBox);
		localeComboBox.addActionListener(evt -> textChanged());
	}

	private void initUpdateLayout(){
//		final ActionListener helpAction = evt -> helpAction();
		final ActionListener okAction = evt -> okAction();
		final ActionListener cancelAction = evt -> cancelAction();
		//TODO link to help
//		helpButton.addActionListener(helpAction);
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[fill,grow][][]"));
		add(textPreviewView, "height 100!,grow,wrap");
		add(localeLabel, "align label,split 2,sizegroup label");
		add(localeComboBox, "wrap");

		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	@Override
	public final void textChanged(){
		okButton.setEnabled(StringUtils.isNotBlank(textPreviewView.getText()));
	}

	@Override
	@SuppressWarnings("BooleanParameter")
	public final void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
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
				model.setValueAt(store.traverse(node, RECORD_LOCALE).getValue(), row, TABLE_INDEX_RECORD_LANGUAGE);
				model.setValueAt(NoteRecordDialog.toVisualText(node), row, TABLE_INDEX_RECORD_TEXT);
			}
		}

		return (size > 0);
	}

	private List<GedcomNode> extractRecords(){
		final List<GedcomNode> records = store.traverseAsList(record, RECORD_TAG_ARRAY);
		final int size = records.size();
		for(int i = 0; i < size; i ++)
			records.set(i, store.getNote(records.get(i).getXRef()));
		return records;
	}

	private void filterTableBy(final NoteDialog panel){
		final String title = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(title, TABLE_INDEX_RECORD_ID,
			TABLE_INDEX_RECORD_LANGUAGE, TABLE_INDEX_RECORD_TEXT);

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
		final GedcomNode selectedRecord = getSelectedRecord();

		final String text = toNoteText(selectedRecord);
		final String languageTag = store.traverse(selectedRecord, RECORD_LOCALE)
			.getValue();
		final String restriction = store.traverse(selectedRecord, RECORD_RESTRICTION)
			.getValue();

		textPreviewView.setText(getTitle(), text, languageTag);
		GUIHelper.setEnabled(textPreviewView, true);

		if(languageTag != null)
			localeComboBox.setSelectedItem(Locale.forLanguageTag(languageTag));
		localeComboBox.setEnabled(true);

		restrictionCheckBox.setSelected("confidential".equals(restriction));
		restrictionCheckBox.setEnabled(true);

		final List<GedcomNode> translations = store.traverseAsList(selectedRecord, RECORD_TRANSLATION_ARRAY);
		GUIHelper.addBorderIfDataPresent(translationButton, !translations.isEmpty());
		translationButton.setEnabled(true);

		final List<GedcomNode> sources = store.traverseAsList(selectedRecord, RECORD_SOURCE_ARRAY);
		GUIHelper.addBorderIfDataPresent(sourceButton, !sources.isEmpty());
		sourceButton.setEnabled(true);


		deleteButton.setEnabled(true);
	}

	private GedcomNode getSelectedRecord(){
		final int selectedRow = recordTable.getSelectedRow();
		final String recordID = (String)recordTable.getValueAt(selectedRow, TABLE_INDEX_RECORD_ID);
		return store.getNote(recordID);
	}

	private static String toNoteText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "\n");
	}

	public static String toVisualText(final GedcomNode note){
		return StringUtils.replace(note.getValue(), "\\n", "↵");
	}

	public static String fromNoteText(final String text){
		return StringUtils.replace(text, "\n", "\\n");
	}

	public final void showNewRecord(){
		newAction();
	}

	private void newAction(){
		//create a new record
		final GedcomNode newRecord = store.create(RECORD_TAG);

		//add to store
		final String recordID = store.addNote(newRecord);
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
		GUIHelper.setEnabled(textPreviewView, false);
		localeComboBox.setEnabled(false);
		restrictionCheckBox.setEnabled(false);
		translationButton.setEnabled(false);
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
			selectedRecord = store.getNote(recordID);

		record.removeChild(selectedRecord);

		model.removeRow(index);
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
		else if(noteType != NoteType.DEFAULT){
			record.addChild(
				store.create(RECORD_UPDATE)
					.addChildValue(RECORD_DATE, now)
					.addChildValue(RECORD_NOTE, NoteDialog.fromNoteText(textPreviewView.getText()))
			);

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			setVisible(false);
		}
		else{
			//show note record dialog
			final GedcomNode changeNote = store.create(RECORD_NOTE);
			final NoteDialog changeNoteDialog = NoteDialog.createUpdateNote(store, (Frame)getParent());
			changeNoteDialog.setTitle("Change note for calendar " + record.getID());
			changeNoteDialog.loadData(changeNote, ignored -> {});

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
			super(new String[]{"ID", "Language", "Text"}, 0);
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

			final NoteDialog dialog = NoteDialog.createNote(store, parent);
			dialog.setTitle(container.getID() != null? "Note for " + container.getID(): "Note");
			if(!dialog.loadData(container, null))
				dialog.showNewRecord();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(500, 513);
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
