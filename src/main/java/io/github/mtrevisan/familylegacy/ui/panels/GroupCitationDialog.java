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
package io.github.mtrevisan.familylegacy.ui.panels;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.Gedcom;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.Store;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.FamilyTableCellRenderer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
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

	private static final int ID_PREFERRED_WIDTH = 43;

	private static final int TABLE_INDEX_GROUP_ID = 0;
	private static final int TABLE_INDEX_GROUP_NAME = 1;

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		"",
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable groupsTable = new JTable(new GroupsTableModel());
	private final JScrollPane groupsScrollPane = new JScrollPane(groupsTable);
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

	//TODO show the list of groups, from which one can select one group, and then edit
	private void initComponents(){
		setTitle("Groups");

		final FamilyTableCellRenderer rightAlignedRenderer = new FamilyTableCellRenderer();
		rightAlignedRenderer.setHorizontalAlignment(JLabel.RIGHT);

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(GroupCitationDialog.this);
			}
		});

		//TODO clicking on a line links to current citation
		groupsTable.setAutoCreateRowSorter(true);
		groupsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		groupsTable.setFocusable(false);
		groupsTable.setGridColor(GRID_COLOR);
		groupsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupsTable.getTableHeader().setFont(groupsTable.getFont().deriveFont(Font.BOLD));
		final TableCellRenderer nameRenderer = new FamilyTableCellRenderer();
		groupsTable.setDefaultRenderer(String.class, nameRenderer);
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
		groupsTable.setRowSorter(sorter);

		groupLabel.setLabelFor(groupField);
		groupField.setEnabled(false);

		roleLabel.setLabelFor(roleField);

		notesButton.addActionListener(e -> {
			//TODO
		});

		sourcesButton.addActionListener(e -> {
			//TODO
		});

		credibilityLabel.setLabelFor(credibilityComboBox);

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
		add(groupsScrollPane, "grow,wrap paragraph");
		add(groupNameLabel, "grow,wrap paragraph");
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
		final List<GedcomNode> groups = store.getGroups();

		final int size = (groups != null? groups.size(): 0);
		if(size > 0){
			final DefaultTableModel groupsModel = (DefaultTableModel)groupsTable.getModel();
			groupsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode group = groups.get(row);

				groupsModel.setValueAt(group.getID(), row, TABLE_INDEX_GROUP_ID);
				groupsModel.setValueAt(store.traverse(group, "NAME"), row, TABLE_INDEX_GROUP_NAME);
			}

			//TODO select row whose id match container.id

			roleField.setText(store.traverse(container, "ROLE").getValue());
			credibilityComboBox.setSelectedItem(store.traverse(container, "CREDIBILITY").getValue());
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

		final Store storeGedcom = new Gedcom();
		final Flef storeFlef = (Flef)storeGedcom.load("/gedg/flef_0.0.3.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode container = storeFlef.getFamilies().get(0);

		EventQueue.invokeLater(() -> {
			final GroupCitationDialog dialog = new GroupCitationDialog(storeFlef, new JFrame());
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
			super(new String[]{"ID", "Name"}, 0);
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
