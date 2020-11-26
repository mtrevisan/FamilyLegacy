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
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
	private static final int TABLE_INDEX_NOTE_NAME = 1;

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable notesTable = new JTable(new GroupsTableModel());
	private final JScrollPane notesScrollPane = new JScrollPane(notesTable);
	private final JButton addButton = new JButton("Add");
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
		notesTable.setFocusable(false);
		notesTable.setGridColor(GRID_COLOR);
		notesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
		sorter.setComparator(TABLE_INDEX_NOTE_NAME, Comparator.naturalOrder());
		notesTable.setRowSorter(sorter);

		addButton.addActionListener(evt -> {
			//TODO
		});
		removeButton.addActionListener(evt -> {
			//TODO
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//TODO
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(notesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 2,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
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

		removeButton.setEnabled(size > 0);

		if(size > 0){
			final DefaultTableModel notesModel = (DefaultTableModel)notesTable.getModel();
			notesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode note = notes.get(row);

				notesModel.setValueAt(note.getID(), row, TABLE_INDEX_NOTE_ID);
				notesModel.setValueAt(note.getValue(), row, TABLE_INDEX_NOTE_NAME);
			}
		}
	}

	private void filterTableBy(final NoteCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_NOTE_ID, TABLE_INDEX_NOTE_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) notesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel) notesTable.getModel();
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


	private static class GroupsTableModel extends DefaultTableModel{

		private static final long serialVersionUID = 981117893723288957L;


		GroupsTableModel(){
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

}
