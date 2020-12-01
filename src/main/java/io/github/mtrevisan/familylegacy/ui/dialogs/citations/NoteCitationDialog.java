/**
 * Copyright (c) 2020 Mauro Trevisan
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
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class NoteCitationDialog extends JDialog{

	private static final long serialVersionUID = 4428884121525685915L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_NOTE_ID = 0;
	private static final int TABLE_INDEX_NOTE_TEXT = 1;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable notesTable = new JTable(new NotesTableModel());
	private final JScrollPane notesScrollPane = new JScrollPane(notesTable);
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton removeButton = new JButton("Remove");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<NoteCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public NoteCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Note citations");

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
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(notesTable.getModel());
		final Comparator<String> idComparator = (value1, value2) -> {
			//NOTE: here it is assumed that all the IDs starts with a character followed by a number, and that years can begin with `~`
			final int v1 = Integer.parseInt(Character.isDigit(value1.charAt(0))? value1: value1.substring(1));
			final int v2 = Integer.parseInt(Character.isDigit(value2.charAt(0))? value2: value2.substring(1));
			return Integer.compare(v1, v2);
		};
		sorter.setComparator(TABLE_INDEX_NOTE_ID, idComparator);
		sorter.setComparator(TABLE_INDEX_NOTE_TEXT, Comparator.naturalOrder());
		notesTable.setRowSorter(sorter);
		notesTable.getSelectionModel().addListSelectionListener(event -> removeButton.setEnabled(true));
		notesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && notesTable.rowAtPoint(evt.getPoint()) >= 0)
					//fire edit event
					editAction();
			}
		});

		addButton.addActionListener(evt -> {
			final GedcomNode newNote = store.create("NOTE");

			final Runnable onCloseGracefully = () -> {
				//if ok was pressed, add this note to the parent container
				final String newNoteID = store.addNote(newNote);
				container.addChildReference("NOTE", newNoteID);
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, newNote, onCloseGracefully));
		});
		editButton.addActionListener(evt -> editAction());
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> deleteAction());

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//remove all reference to notes from the container
			container.removeChildrenWithTag("NOTE");
			//add all the remaining references to notes to the container
			for(int i = 0; i < notesTable.getRowCount(); i ++){
				final String id = (String)notesTable.getValueAt(i, TABLE_INDEX_NOTE_ID);
				container.addChildReference("NOTE", id);
			}
			//TODO remember, when saving the whole gedcom, to remove all non-referenced notes!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(notesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2");
		add(editButton, "tag edit,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void editAction(){
		//retrieve selected note
		final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
		final int index = notesTable.convertRowIndexToModel(notesTable.getSelectedRow());
		final String noteXRef = (String)model.getValueAt(index, TABLE_INDEX_NOTE_ID);
		final GedcomNode selectedNote = store.getNote(noteXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, selectedNote));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
		model.removeRow(notesTable.convertRowIndexToModel(notesTable.getSelectedRow()));
		removeButton.setEnabled(false);
	}

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> notes = store.traverseAsList(container, "NOTE[]");
		final int size = notes.size();
		for(int i = 0; i < size; i ++)
			notes.set(i, store.getNote(notes.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel notesModel = (DefaultTableModel)notesTable.getModel();
			notesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode note = notes.get(row);

				notesModel.setValueAt(note.getID(), row, TABLE_INDEX_NOTE_ID);
				notesModel.setValueAt(note.getValue(), row, TABLE_INDEX_NOTE_TEXT);
			}
		}
	}

	private void filterTableBy(final NoteCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_NOTE_ID, TABLE_INDEX_NOTE_TEXT);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)notesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)notesTable.getModel();
			sorter = new TableRowSorter<>(model);
			notesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}

	private RowFilter<DefaultTableModel, Object> createTextFilter(final String text, final int... columnIndexes){
		if(StringUtils.isNotBlank(text)){
			try{
				//split input text around spaces and commas
				final String[] components = StringUtils.split(text, " ,");
				final Collection<RowFilter<DefaultTableModel, Object>> andFilters = new ArrayList<>(components.length);
				for(final String component : components)
					andFilters.add(RowFilter.regexFilter("(?i)(?:" + Pattern.quote(component) + ")", columnIndexes));
				return RowFilter.andFilter(andFilters);
			}
			catch(final PatternSyntaxException ignored){
				//current expression doesn't parse, ignore it
			}
		}
		return null;
	}


	private static class NotesTableModel extends DefaultTableModel{

		private static final long serialVersionUID = 981117893723288957L;


		NotesTableModel(){
			super(new String[]{"ID", "Text"}, 0);
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return String.class;
		}

		@Override
		public boolean isCellEditable(final int row, final int column){
			return false;
		}
	}

	private static class TableTransferHandle extends TransferHandler{
		private static final long serialVersionUID = 7110295550176057986L;

		private final JTable table;

		TableTransferHandle(final JTable table){
			this.table = table;
		}

		@Override
		public int getSourceActions(final JComponent component){
			return TransferHandler.COPY_OR_MOVE;
		}

		@Override
		protected Transferable createTransferable(final JComponent component){
			return new StringSelection(Integer.toString(table.getSelectedRow()));
		}

		@Override
		public boolean canImport(final TransferHandler.TransferSupport support){
			return (support.isDrop() && support.isDataFlavorSupported(DataFlavor.stringFlavor));
		}

		@Override
		public boolean importData(final TransferHandler.TransferSupport support){
			if(!support.isDrop() || !canImport(support))
				return false;

			final DefaultTableModel model = (DefaultTableModel)table.getModel();
			final int size = model.getRowCount();
			//bound `rowTo` to be between 0 and `size`
			final int rowTo = Math.min(Math.max(((JTable.DropLocation)support.getDropLocation()).getRow(), 0), size);

			try{
				final int rowFrom = Integer.parseInt((String)support.getTransferable().getTransferData(DataFlavor.stringFlavor));
				if(rowFrom == rowTo - 1)
					return false;

				final List<Object[]> rows = new ArrayList<>(size);
				for(int i = 0; i < size; i ++){
					rows.add(new Object[]{table.getValueAt(0, TABLE_INDEX_NOTE_ID), table.getValueAt(0, TABLE_INDEX_NOTE_TEXT)});

					model.removeRow(0);
				}
				if(rowTo < size){
					final Object[] from = rows.get(rowFrom);
					final Object[] to = rows.get(rowTo);
					rows.set(rowTo, from);
					rows.set(rowFrom, to);
				}
				else{
					final Object[] from = rows.get(rowFrom);
					rows.remove(rowFrom);
					rows.add(from);
				}
				for(final Object[] row : rows)
					model.addRow(row);

				return true;
			}
			catch(final Exception ignored){}
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
		store.load("/gedg/flef_0.0.3.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final NoteCitationDialog dialog = new NoteCitationDialog(store, new JFrame());
			dialog.loadData(container);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 500);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
