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
import io.github.mtrevisan.familylegacy.ui.dialogs.GroupDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class GroupCitationDialog extends JDialog{

	@Serial
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

	private static final String KEY_GROUP_ID = "groupID";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable groupsTable = new JTable(new GroupTableModel());
	private final JScrollPane groupsScrollPane = new JScrollPane(groupsTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel groupLabel = new JLabel("Group:");
	private final JTextField groupField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton notesButton = new JButton("Notes");
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton helpButton = new JButton("Help");
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
		sorter.setComparator(TABLE_INDEX_GROUP_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_GROUP_NAME, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_GROUP_TYPE, Comparator.naturalOrder());
		groupsTable.setRowSorter(sorter);
		//clicking on a line links it to current group citation
		groupsTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = groupsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(groupsTable.convertRowIndexToModel(selectedRow));
		});
		groupsTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && groupsTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		groupsTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		groupsTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Serial
				private static final long serialVersionUID = -2147176729019326324L;

				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}


				@SuppressWarnings("unused")
				@Serial
				private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
					throw new NotSerializableException(getClass().getName());
				}

				@SuppressWarnings("unused")
				@Serial
				private void readObject(final ObjectInputStream is) throws NotSerializableException{
					throw new NotSerializableException(getClass().getName());
				}
			});
		groupsTable.setPreferredScrollableViewportSize(new Dimension(groupsTable.getPreferredSize().width,
			groupsTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> {
			final GedcomNode newGroup = store.create("GROUP");

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this group to the parent container
				final String newGroupID = store.addGroup(newGroup);
				container.addChildReference("GROUP", newGroupID);

				//refresh group list
				loadData();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP, newGroup, onCloseGracefully));
		});

		groupLabel.setLabelFor(groupField);
		GUIHelper.setEnabled(groupLabel, false);

		roleLabel.setLabelFor(roleField);
		GUIHelper.setEnabled(roleLabel, false);

		notesButton.setEnabled(false);
		notesButton.addActionListener(e -> EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, container)));

		credibilityLabel.setLabelFor(credibilityComboBox);
		GUIHelper.setEnabled(credibilityLabel, false);

		GUIHelper.setEnabled(restrictionCheckBox, false);

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.addActionListener(evt -> {
			final String id = (String)okButton.getClientProperty(KEY_GROUP_ID);
			final String role = roleField.getText();
			final int credibility = credibilityComboBox.getSelectedIndex() - 1;
			final String restriction = (restrictionCheckBox.isSelected()? "confidential": null);

			final GedcomNode group = store.traverse(container, "GROUP@" + id);
			group.replaceChildValue("ROLE", role);
			group.replaceChildValue("CREDIBILITY", (credibility >= 0? Integer.toString(credibility): null));
			group.replaceChildValue("RESTRICTION", restriction);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced groups!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,sizegroup label,split 2");
		add(filterField, "grow,wrap");
		add(groupsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2,wrap paragraph");
		add(groupLabel, "align label,sizegroup label,split 2");
		add(groupField, "grow,wrap");
		add(roleLabel, "align label,sizegroup label,split 2");
		add(roleField, "grow,wrap paragraph");
		add(notesButton, "sizegroup button,grow,wrap paragraph");
		add(credibilityLabel, "align label,sizegroup label,split 2");
		add(credibilityComboBox, "grow,wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	private void transferListToContainer(){
		//remove all reference to the group from the container
		container.removeChildrenWithTag("GROUP");
		//add all the remaining references to groups to the container
		for(int i = 0; i < groupsTable.getRowCount(); i ++){
			final String id = (String)groupsTable.getValueAt(i, TABLE_INDEX_GROUP_ID);
			container.addChildReference("GROUP", id);
		}
	}

	private void selectAction(final int selectedRow){
		final String selectedGroupID = (String)groupsTable.getValueAt(selectedRow, TABLE_INDEX_GROUP_ID);
		final GedcomNode selectedGroupCitation = store.traverse(container, "GROUP@" + selectedGroupID);
		final GedcomNode selectedGroup = store.getGroup(selectedGroupID);
		okButton.putClientProperty(KEY_GROUP_ID, selectedGroupID);
		GUIHelper.setEnabled(groupLabel, true);
		groupField.setText(store.traverse(selectedGroup, "NAME").getValue());

		GUIHelper.setEnabled(roleLabel, true);
		roleField.setText(store.traverse(selectedGroupCitation, "ROLE").getValue());
		notesButton.setEnabled(true);
		GUIHelper.setEnabled(credibilityLabel, true);
		final String credibility = store.traverse(selectedGroupCitation, "CREDIBILITY").getValue();
		credibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);
		GUIHelper.setEnabled(restrictionCheckBox, true);
		final String restriction = store.traverse(selectedGroupCitation, "RESTRICTION").getValue();
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		okButton.setEnabled(true);
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
		final int index = groupsTable.convertRowIndexToModel(groupsTable.getSelectedRow());
		model.removeRow(index);

		//remove from container
		transferListToContainer();
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


	private static class GroupTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -2985688516803729157L;


		GroupTableModel(){
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


		@SuppressWarnings("unused")
		@Serial
		private void writeObject(final ObjectOutputStream os) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
		}

		@SuppressWarnings("unused")
		@Serial
		private void readObject(final ObjectInputStream is) throws NotSerializableException{
			throw new NotSerializableException(getClass().getName());
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

		final JFrame parent = new JFrame();
		EventQueue.invokeLater(() -> {
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					switch(editCommand.getType()){
						case GROUP -> {
							final GroupDialog groupDialog = new GroupDialog(store, parent);
							groupDialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							groupDialog.setSize(300, 250);
							groupDialog.setLocationRelativeTo(parent);
							groupDialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog noteCitationDialog = new NoteCitationDialog(store, parent);
							noteCitationDialog.loadData(editCommand.getContainer());
							noteCitationDialog.setSize(450, 260);
							noteCitationDialog.setLocationRelativeTo(parent);
							noteCitationDialog.setVisible(true);
						}
						case NOTE -> {
							final NoteRecordDialog noteDialog = new NoteRecordDialog(store, parent);
							noteDialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							noteDialog.setSize(550, 350);
							noteDialog.setLocationRelativeTo(parent);
							noteDialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupCitationDialog dialog = new GroupCitationDialog(store, parent);
			dialog.loadData(container);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 440);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
