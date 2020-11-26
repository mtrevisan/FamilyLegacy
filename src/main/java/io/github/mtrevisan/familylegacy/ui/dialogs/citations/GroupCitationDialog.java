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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
		"",
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private static final String KEY_GROUP_ID = "groupID";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable groupsTable = new JTable(new GroupsTableModel());
	private final JScrollPane groupsScrollPane = new JScrollPane(groupsTable);
	private final JButton addButton = new JButton("Add");
	private final JButton removeButton = new JButton("Remove");
	private final JLabel groupLabel = new JLabel("Group:");
	private final JTextField groupField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JLabel groupNameLabel = new JLabel();
	private final JButton notesButton = new JButton("Notes");
	private final JButton sourcesButton = new JButton("Sources");
	private final JLabel credibilityLabel = new JLabel("Restriction:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
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
		groupsTable.setFocusable(false);
		groupsTable.setGridColor(GRID_COLOR);
		groupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
			final int selectedRow = groupsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				final String selectedGroupName = (String)groupsTable.getValueAt(selectedRow, TABLE_INDEX_GROUP_NAME);
				groupField.putClientProperty(KEY_GROUP_ID, groupsTable.getValueAt(selectedRow, TABLE_INDEX_GROUP_ID));
				groupField.setText(selectedGroupName);
			}
		});

		addButton.addActionListener(evt -> {
			//TODO
		});
		removeButton.addActionListener(evt -> {
			//TODO
		});

		groupLabel.setLabelFor(groupField);
		groupField.setEnabled(false);

		roleLabel.setLabelFor(roleField);

		notesButton.addActionListener(e -> {
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, container));
		});

		sourcesButton.addActionListener(e -> {
			EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE_CITATION, container));
		});

		credibilityLabel.setLabelFor(credibilityComboBox);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			//TODO
//			if(listener != null){
//				final GedcomNode selectedFamily = getSelectedFamily();
//				listener.onNodeSelected(selectedFamily, SelectedNodeType.FAMILY, panelReference);
//			}
			final String id = (String)groupField.getClientProperty(KEY_GROUP_ID);
			final String role = roleField.getText();
			//TODO notes
			//TODO source citations
			final int credibility = credibilityComboBox.getSelectedIndex() - 1;

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout());
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(groupsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 2,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(groupNameLabel, "grow,wrap");
		add(groupLabel, "align label,split 2");
		add(groupField, "grow,wrap");
		add(roleLabel, "align label,split 2");
		add(roleField, "grow,wrap");
		add(notesButton, "sizegroup button,grow,wrap");
		add(sourcesButton, "sizegroup button,grow,wrap");
		add(credibilityLabel, "align label,split 2");
		add(credibilityComboBox, "grow,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
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

		removeButton.setEnabled(size > 0);

		if(size > 0){
			final DefaultTableModel groupsModel = (DefaultTableModel)groupsTable.getModel();
			groupsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode group = groups.get(row);

				groupsModel.setValueAt(group.getID(), row, TABLE_INDEX_GROUP_ID);
				groupsModel.setValueAt(store.traverse(group, "NAME").getValue(), row, TABLE_INDEX_GROUP_NAME);
				groupsModel.setValueAt(store.traverse(group, "TYPE").getValue(), row, TABLE_INDEX_GROUP_TYPE);
			}

			final String groupID = store.traverse(container, "GROUP").getXRef();
			if(groupID != null)
				for(int index = 0; index < size; index ++)
					if(groupsModel.getValueAt(index, TABLE_INDEX_GROUP_ID).equals(groupID)){
						//select row whose id match container.id
						groupsTable.setRowSelectionInterval(index, index);

						final GedcomNode currentGroup = store.getGroup(groupID);
						groupField.putClientProperty(KEY_GROUP_ID, groupID);
						groupField.setText(store.traverse(currentGroup, "NAME").getValue());
						roleField.setText(store.traverse(container, "GROUP.ROLE").getValue());
						credibilityComboBox.setSelectedItem(store.traverse(container, "GROUP.CREDIBILITY").getValue());
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

}