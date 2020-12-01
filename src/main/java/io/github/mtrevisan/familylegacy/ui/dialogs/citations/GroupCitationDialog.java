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


public class GroupCitationDialog extends JDialog{

	private static final long serialVersionUID = -4893058951719376351L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_GROUP_ID = 0;
	private static final int TABLE_INDEX_GROUP_NAME = 1;
	private static final int TABLE_INDEX_GROUP_TYPE = 2;

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private static final DefaultComboBoxModel<String> RESTRICTION_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"confidential", "locked", "private"});

	private static final String KEY_GROUP_ID = "groupID";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable groupsTable = new JTable(new GroupsTableModel());
	private final JScrollPane groupsScrollPane = new JScrollPane(groupsTable);
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton removeButton = new JButton("Remove");
	private final JLabel groupLabel = new JLabel("Group:");
	private final JTextField groupField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JLabel groupNameLabel = new JLabel();
	private final JButton notesButton = new JButton("Notes");
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JLabel restrictionLabel = new JLabel("Restriction:");
	private final JComboBox<String> restrictionComboBox = new JComboBox<>(RESTRICTION_MODEL);
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<GroupCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public GroupCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Group citations");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(GroupCitationDialog.this);
			}
		});

		groupsTable.setAutoCreateRowSorter(true);
		groupsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		groupsTable.setGridColor(GRID_COLOR);
		groupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupsTable.setDragEnabled(true);
		groupsTable.setDropMode(DropMode.INSERT_ROWS);
		groupsTable.setTransferHandler(new TableTransferHandle(groupsTable));
		groupsTable.getTableHeader().setFont(groupsTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(groupsTable, TABLE_INDEX_GROUP_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(groupsTable.getModel());
		final Comparator<String> idComparator = (value1, value2) -> {
			//NOTE: here it is assumed that all the IDs starts with a character followed by a number, and that years can begin with `~`
			final int v1 = Integer.parseInt(Character.isDigit(value1.charAt(0))? value1: value1.substring(1));
			final int v2 = Integer.parseInt(Character.isDigit(value2.charAt(0))? value2: value2.substring(1));
			return Integer.compare(v1, v2);
		};
		sorter.setComparator(TABLE_INDEX_GROUP_ID, idComparator);
		sorter.setComparator(TABLE_INDEX_GROUP_NAME, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_GROUP_TYPE, Comparator.naturalOrder());
		groupsTable.setRowSorter(sorter);
		//clicking on a line links it to current group
		groupsTable.getSelectionModel().addListSelectionListener(evt -> {
			removeButton.setEnabled(true);

			final int selectedRow = groupsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				final String selectedGroupID = (String)groupsTable.getValueAt(selectedRow, TABLE_INDEX_GROUP_ID);
				final GedcomNode selectedGroupCitation = store.traverse(container, "GROUP@" + selectedGroupID);
				final GedcomNode selectedGroup = store.getGroup(selectedGroupID);
				groupField.putClientProperty(KEY_GROUP_ID, selectedGroupID);
				groupField.setText(store.traverse(selectedGroup, "NAME").getValue());

				roleField.setText(store.traverse(selectedGroupCitation, "ROLE").getValue());
				credibilityComboBox.setEnabled(true);
				final String credibility = store.traverse(selectedGroupCitation, "CREDIBILITY").getValue();
				credibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);
				restrictionComboBox.setEnabled(true);
				final String restriction = store.traverse(selectedGroupCitation, "RESTRICTION").getValue();
				restrictionComboBox.setSelectedIndex(restriction != null? Integer.parseInt(restriction) + 1: 0);

				roleField.setEnabled(true);
				notesButton.setEnabled(!store.traverseAsList(selectedGroupCitation, "NOTE[]").isEmpty());
			}
		});
		groupsTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && groupsTable.rowAtPoint(evt.getPoint()) >= 0)
					//fire edit event
					editAction();
			}
		});

		addButton.addActionListener(evt -> {
			final GedcomNode newGroup = store.create("GROUP");

			final Runnable onCloseGracefully = () -> {
				//if ok was pressed, add this note to the parent container
				final String newGroupID = store.addNote(newGroup);
				container.addChildReference("GROUP", newGroupID);
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE, newGroup, onCloseGracefully));
		});
		editButton.addActionListener(evt -> editAction());
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> deleteAction());

		groupLabel.setLabelFor(groupField);
		groupField.setEnabled(false);

		roleField.setEnabled(false);
		roleLabel.setLabelFor(roleField);

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, container)));

		credibilityComboBox.setEnabled(false);
		credibilityLabel.setLabelFor(credibilityComboBox);

		restrictionLabel.setLabelFor(restrictionComboBox);
		restrictionComboBox.setEnabled(false);
		restrictionComboBox.setEditable(true);
		restrictionComboBox.addActionListener(e -> {
			if("comboBoxEdited".equals(e.getActionCommand())){
				final String newValue = (String)RESTRICTION_MODEL.getSelectedItem();
				RESTRICTION_MODEL.addElement(newValue);

				restrictionComboBox.setSelectedItem(newValue);
			}
		});

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//TODO
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}
			final String id = (String)groupField.getClientProperty(KEY_GROUP_ID);
			final String role = roleField.getText();
			final int credibility = credibilityComboBox.getSelectedIndex() - 1;
			final int restriction = restrictionComboBox.getSelectedIndex() - 1;

			//remove all reference to groups from the container
			container.removeChildrenWithTag("GROUP");
			//add all the remaining groups to notes to the container
			//TODO ummm... cannot work, there are other data apart from the xref
			for(int i = 0; i < groupsTable.getRowCount(); i ++){
				final String id2 = (String)groupsTable.getValueAt(i, TABLE_INDEX_GROUP_ID);
				container.addChildReference("GROUP", id2);
			}
			//TODO remember, when saving the whole gedcom, to remove all non-referenced groups!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(groupsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2");
		add(editButton, "tag edit,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(groupNameLabel, "grow,wrap");
		add(groupLabel, "align label,split 2");
		add(groupField, "grow,wrap");
		add(roleLabel, "align label,split 2");
		add(roleField, "grow,wrap");
		add(notesButton, "sizegroup button,grow,wrap");
		add(credibilityLabel, "align label,split 2");
		add(credibilityComboBox, "grow,wrap paragraph");
		add(restrictionLabel, "align label,split 2");
		add(restrictionComboBox, "grow,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void editAction(){
		//retrieve selected note
		final DefaultTableModel model = (DefaultTableModel)groupsTable.getModel();
		final int index = groupsTable.convertRowIndexToModel(groupsTable.getSelectedRow());
		final String groupXRef = (String)model.getValueAt(index, TABLE_INDEX_GROUP_ID);
		final GedcomNode selectedNote = store.getGroup(groupXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP, selectedNote));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)groupsTable.getModel();
		model.removeRow(groupsTable.convertRowIndexToModel(groupsTable.getSelectedRow()));
		removeButton.setEnabled(false);
	}

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> groups = store.traverseAsList(container, "GROUP[]");
		final int size = groups.size();
		for(int i = 0; i < size; i ++)
			groups.set(i, store.getGroup(groups.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel groupsModel = (DefaultTableModel)groupsTable.getModel();
			groupsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode group = groups.get(row);

				groupsModel.setValueAt(group.getID(), row, TABLE_INDEX_GROUP_ID);
				groupsModel.setValueAt(store.traverse(group, "NAME").getValue(), row, TABLE_INDEX_GROUP_NAME);
				groupsModel.setValueAt(store.traverse(group, "TYPE").getValue(), row, TABLE_INDEX_GROUP_TYPE);
			}
		}
	}

	private void filterTableBy(final GroupCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_GROUP_ID, TABLE_INDEX_GROUP_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)groupsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)groupsTable.getModel();
			sorter = new TableRowSorter<>(model);
			groupsTable.setRowSorter(sorter);
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


	private static class GroupsTableModel extends DefaultTableModel{

		private static final long serialVersionUID = -2985688516803729157L;


		GroupsTableModel(){
			super(new String[]{"ID", "Name", "Type"}, 0);
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
		private static final long serialVersionUID = 6100956630293306315L;

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
					rows.add(new Object[]{table.getValueAt(0, TABLE_INDEX_GROUP_ID), table.getValueAt(0, TABLE_INDEX_GROUP_NAME),
						table.getValueAt(0, TABLE_INDEX_GROUP_TYPE)});

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
			final GroupCitationDialog dialog = new GroupCitationDialog(store, new JFrame());
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
