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
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class NoteCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4428884121525685915L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;
	private static final int LANGUAGE_PREFERRED_WIDTH = 65;

	private static final int TABLE_INDEX_NOTE_ID = 0;
	private static final int TABLE_INDEX_NOTE_LANGUAGE = 1;
	private static final int TABLE_INDEX_NOTE_TEXT = 2;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable notesTable = new JTable(new NoteTableModel());
	private final JScrollPane notesScrollPane = new JScrollPane(notesTable);
	private final JButton addButton = new JButton("Add");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<NoteCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private String childTag;
	private Runnable addAction;
	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public static NoteCitationDialog createNoteCitation(final Flef store, final Frame parent){
		final NoteCitationDialog dialog = new NoteCitationDialog(store, parent);
		dialog.initComponents();
		dialog.childTag = "NOTE";
		hideColumn(dialog.notesTable, TABLE_INDEX_NOTE_LANGUAGE);
		dialog.addAction = () -> {
			final GedcomNode newNote = store.create(dialog.childTag);

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this note to the parent container
				final String newNoteID = store.addNote(newNote);
				dialog.container.addChildReference(dialog.childTag, newNoteID);

				//refresh note list
				dialog.loadData();
			};

			//fire add event
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, newNote, onCloseGracefully));
		};
		return dialog;
	}

	public static NoteCitationDialog createNoteTranslationCitation(final Flef store, final Frame parent){
		final NoteCitationDialog dialog = new NoteCitationDialog(store, parent);
		dialog.initComponents();
		dialog.childTag = "TRANSLATION";
		hideColumn(dialog.notesTable, TABLE_INDEX_NOTE_ID);
		dialog.addAction = () -> {
			final GedcomNode newNoteTranslation = store.create(dialog.childTag);

			final Consumer<Object> onCloseGracefully = ignored -> {
				dialog.container.addChild(newNoteTranslation);

				//refresh note list
				dialog.loadData();
			};

			//fire add event
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_TRANSLATION, newNoteTranslation, onCloseGracefully));
		};
		return dialog;
	}

	private static void hideColumn(final JTable table, final int columnIndex){
		final TableColumn hiddenColumn = table.getColumnModel().getColumn(columnIndex);
		hiddenColumn.setWidth(0);
		hiddenColumn.setMinWidth(0);
		hiddenColumn.setMaxWidth(0);
	}

	private NoteCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;
	}

	private void initComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(NoteCitationDialog.this);
			}
		});

		notesTable.setAutoCreateRowSorter(true);
		notesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		notesTable.setGridColor(GRID_COLOR);
		notesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		notesTable.setDragEnabled(true);
		notesTable.setDropMode(DropMode.INSERT_ROWS);
		notesTable.setTransferHandler(new TableTransferHandle(notesTable));
		notesTable.getTableHeader().setFont(notesTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(notesTable, TABLE_INDEX_NOTE_ID, 0, ID_PREFERRED_WIDTH);
		TableHelper.setColumnWidth(notesTable, TABLE_INDEX_NOTE_LANGUAGE, 0, LANGUAGE_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(notesTable.getModel());
		sorter.setComparator(TABLE_INDEX_NOTE_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_NOTE_LANGUAGE, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_NOTE_TEXT, Comparator.naturalOrder());
		notesTable.setRowSorter(sorter);
		notesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && notesTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		final InputMap notesTableInputMap = notesTable.getInputMap(JComponent.WHEN_FOCUSED);
		notesTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert");
		notesTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		final ActionMap notesTableActionMap = notesTable.getActionMap();
		notesTableActionMap.put("insert", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				addAction();
			}
		});
		notesTableActionMap.put("delete", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});
		notesTable.setPreferredScrollableViewportSize(new Dimension(notesTable.getPreferredSize().width,
			notesTable.getRowHeight() * 5));

		final ActionListener addAction = evt -> addAction();
		final ActionListener okAction = evt -> {
			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			setVisible(false);
		};
		final ActionListener cancelAction = evt -> setVisible(false);
		addButton.addActionListener(addAction);
		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.addActionListener(okAction);
		cancelButton.addActionListener(cancelAction);
		getRootPane().registerKeyboardAction(cancelAction, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		getRootPane().registerKeyboardAction(addAction, INSERT_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[][grow,fill][]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(notesScrollPane, "grow,wrap related");
		add(addButton, "sizegroup button,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	public void addAction(){
		addAction.run();
	}

	private void editAction(){
		//retrieve selected note
		final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
		final int index = notesTable.convertRowIndexToModel(notesTable.getSelectedRow());
		final String noteXRef = (String)model.getValueAt(index, TABLE_INDEX_NOTE_ID);
		final GedcomNode selectedNote;
		if(StringUtils.isBlank(noteXRef))
			selectedNote = store.traverseAsList(container, "TRANSLATION[]")
				.get(index);
		else
			selectedNote = store.getNote(noteXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(("NOTE".equals(childTag)? EditEvent.EditType.NOTE: EditEvent.EditType.NOTE_TRANSLATION), selectedNote));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
		final int index = notesTable.convertRowIndexToModel(notesTable.getSelectedRow());
		final String noteXRef = (String)model.getValueAt(index, TABLE_INDEX_NOTE_ID);
		final GedcomNode selectedNote;
		if(StringUtils.isBlank(noteXRef))
			selectedNote = store.traverseAsList(container, "TRANSLATION[]")
				.get(index);
		else
			selectedNote = store.getNote(noteXRef);

		container.removeChild(selectedNote);

		loadData();
	}

	public boolean loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		return loadData();
	}

	private boolean loadData(){
		final List<GedcomNode> notes = store.traverseAsList(container, childTag + "[]");
		final int size = notes.size();
		if("NOTE".equals(childTag))
			for(int i = 0; i < size; i ++){
				final String noteXRef = notes.get(i).getXRef();
				final GedcomNode note = store.getNote(noteXRef);
				notes.set(i, note);
			}

		final DefaultTableModel notesModel = (DefaultTableModel)notesTable.getModel();
		notesModel.setRowCount(size);
		for(int row = 0; row < size; row ++){
			final GedcomNode note = notes.get(row);

			notesModel.setValueAt(note.getID(), row, TABLE_INDEX_NOTE_ID);
			notesModel.setValueAt(store.traverse(note, "LOCALE").getValue(), row, TABLE_INDEX_NOTE_LANGUAGE);
			notesModel.setValueAt(NoteRecordDialog.toVisualText(note), row, TABLE_INDEX_NOTE_TEXT);
		}
		return (size > 0);
	}

	private void filterTableBy(final NoteCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(text, TABLE_INDEX_NOTE_ID, TABLE_INDEX_NOTE_TEXT);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)notesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
			sorter = new TableRowSorter<>(model);
			notesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class NoteTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 981117893723288957L;


		NoteTableModel(){
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
		store.load("/gedg/flef_0.0.7.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					if(editCommand.getType() == EditEvent.EditType.NOTE){
						final NoteRecordDialog noteDialog = NoteRecordDialog.createNote(store, parent);
						final GedcomNode note = editCommand.getContainer();
						noteDialog.setTitle(note.getID() != null
							? "Note for " + note.getID()
							: "New note for " + container.getID());
						noteDialog.loadData(note, editCommand.getOnCloseGracefully());

						noteDialog.setSize(550, 350);
						noteDialog.setLocationRelativeTo(parent);
						noteDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.NOTE_TRANSLATION){
						final NoteRecordDialog noteDialog = NoteRecordDialog.createNoteTranslation(store, parent);
						final GedcomNode note = editCommand.getContainer();
						noteDialog.setTitle(note.getID() != null
							? "Translation for " + note.getID()
							: "New translation for note for " + container.getID());
						noteDialog.loadData(note, editCommand.getOnCloseGracefully());

						noteDialog.setSize(550, 350);
						noteDialog.setLocationRelativeTo(parent);
						noteDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.NOTE_TRANSLATION_CITATION){
						final NoteCitationDialog noteCitationDialog = createNoteTranslationCitation(store, parent);
						final GedcomNode note = editCommand.getContainer();
						noteCitationDialog.setTitle(note.getID() != null
							? "Translation citation for " + note.getID()
							: "New translation citation for " + container.getID());
						if(!noteCitationDialog.loadData(note, editCommand.getOnCloseGracefully()))
							noteCitationDialog.addAction();

						noteCitationDialog.setSize(550, 350);
						noteCitationDialog.setLocationRelativeTo(parent);
						noteCitationDialog.setVisible(true);
					}
					else if(editCommand.getType() == EditEvent.EditType.SOURCE_CITATION){
						final GedcomNode sourceCitation = editCommand.getContainer();
						final SourceCitationDialog sourceCitationDialog = new SourceCitationDialog(store, parent);
						sourceCitationDialog.setTitle(sourceCitation.getID() != null
							? "Source citation for " + sourceCitation.getID()
							: "New source citation for note for " + container.getID());
						if(!sourceCitationDialog.loadData(sourceCitation, editCommand.getOnCloseGracefully()))
							sourceCitationDialog.addAction();

						sourceCitationDialog.setSize(550, 450);
						sourceCitationDialog.setLocationRelativeTo(parent);
						sourceCitationDialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final NoteCitationDialog dialog = createNoteCitation(store, parent);
//			final NoteCitationDialog dialog = createNoteTranslationCitation(store, parent);
			dialog.setTitle(container == null? "Note citations": "Note citations for " + container.getID());
			if(!dialog.loadData(container, null))
				//show a note input dialog
				dialog.addAction();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 260);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
