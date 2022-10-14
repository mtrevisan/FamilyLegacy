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
import io.github.mtrevisan.familylegacy.ui.dialogs.records.GroupRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
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
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;


public class GroupCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -4893058951719376351L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_GROUP_ID = 0;
	private static final int TABLE_INDEX_GROUP_NAME = 1;
	private static final int TABLE_INDEX_GROUP_TYPE = 2;

	private static final String KEY_GROUP_ID = "groupID";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable groupTable = new JTable(new GroupTableModel());
	private final JScrollPane groupsScrollPane = new JScrollPane(groupTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel groupLabel = new JLabel("Group:");
	private final JTextField groupField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton noteButton = new JButton(ICON_NOTE);
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(new CredibilityComboBoxModel());
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<GroupCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public GroupCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(GroupCitationDialog.this);
			}
		});

		groupTable.setAutoCreateRowSorter(true);
		groupTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		groupTable.setGridColor(GRID_COLOR);
		groupTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		groupTable.setDragEnabled(true);
		groupTable.setDropMode(DropMode.INSERT_ROWS);
		groupTable.setTransferHandler(new TableTransferHandle(groupTable));
		groupTable.getTableHeader().setFont(groupTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(groupTable, TABLE_INDEX_GROUP_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(groupTable.getModel());
		sorter.setComparator(TABLE_INDEX_GROUP_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_GROUP_NAME, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_GROUP_TYPE, Comparator.naturalOrder());
		groupTable.setRowSorter(sorter);
		//clicking on a line links it to current group citation
		groupTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = groupTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(groupTable.convertRowIndexToModel(selectedRow));
		});
		groupTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && groupTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		final InputMap groupTableInputMap = groupTable.getInputMap(JComponent.WHEN_FOCUSED);
		groupTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert");
		groupTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		final ActionMap groupTableActionMap = groupTable.getActionMap();
		groupTableActionMap.put("insert", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				addAction();
			}
		});
		groupTableActionMap.put("delete", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});

		groupLabel.setLabelFor(groupField);
		GUIHelper.setEnabled(groupLabel, false);

		roleLabel.setLabelFor(roleField);
		GUIHelper.setEnabled(roleLabel, false);

		noteButton.setToolTipText("Add note");
		noteButton.setEnabled(false);
		noteButton.addActionListener(evt -> {
			final String selectedGroupID = (String)okButton.getClientProperty(KEY_GROUP_ID);
			final GedcomNode selectedGroup = store.getSource(selectedGroupID);
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedGroup));
		});

		credibilityLabel.setLabelFor(credibilityComboBox);
		GUIHelper.setEnabled(credibilityLabel, false);

		GUIHelper.setEnabled(restrictionCheckBox, false);

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

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
		add(filterLabel, "align label,sizegroup label,split 2");
		add(filterField, "grow,wrap");
		add(groupsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(groupLabel, "align label,sizegroup label,split 2");
		add(groupField, "grow,wrap");
		add(roleLabel, "align label,sizegroup label,split 2");
		add(roleField, "grow,wrap paragraph");
		add(noteButton, "sizegroup button,wrap paragraph");
		add(credibilityLabel, "align label,sizegroup label,split 2");
		add(credibilityComboBox, "wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void selectAction(final int selectedRow){
		final String selectedGroupID = (String)groupTable.getValueAt(selectedRow, TABLE_INDEX_GROUP_ID);
		final GedcomNode selectedGroupCitation = store.traverse(container, "GROUP@" + selectedGroupID);
		okButton.putClientProperty(KEY_GROUP_ID, selectedGroupID);

		GUIHelper.setEnabled(groupLabel, true);
		final GedcomNode selectedGroup = store.getGroup(selectedGroupID);
		groupField.setText(store.traverse(selectedGroup, "NAME").getValue());

		GUIHelper.setEnabled(roleLabel, true);
		roleField.setText(store.traverse(selectedGroupCitation, "ROLE").getValue());
		noteButton.setEnabled(true);
		GUIHelper.setEnabled(credibilityLabel, true);
		final String credibility = store.traverse(selectedGroupCitation, "CREDIBILITY").getValue();
		credibilityComboBox.setSelectedIndex(credibility != null && !credibility.isEmpty()? Integer.parseInt(credibility) + 1: 0);
		GUIHelper.setEnabled(restrictionCheckBox, true);
		final String restriction = store.traverse(selectedGroupCitation, "RESTRICTION").getValue();
		restrictionCheckBox.setSelected("confidential".equals(restriction));

		okButton.setEnabled(true);
	}

	public final void addAction(){
		final GedcomNode newGroup = store.create("GROUP");

		final Consumer<Object> onCloseGracefully = dialog -> {
			//if ok was pressed, add this source to the parent container
			final String newBGroupID = store.addGroup(newGroup);
			container.addChildReference("GROUP", newBGroupID);

			//refresh source list
			loadData();
		};

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP, newGroup, onCloseGracefully));
	}

	private void editAction(){
		//retrieve selected note
		final DefaultTableModel model = (DefaultTableModel)groupTable.getModel();
		final int index = groupTable.convertRowIndexToModel(groupTable.getSelectedRow());
		final String groupXRef = (String)model.getValueAt(index, TABLE_INDEX_GROUP_ID);
		final GedcomNode selectedGroup;
		if(StringUtils.isBlank(groupXRef))
			selectedGroup = store.traverseAsList(container, "GROUP[]")
				.get(index);
		else
			selectedGroup = store.getGroup(groupXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.GROUP, selectedGroup));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)groupTable.getModel();
		final int index = groupTable.convertRowIndexToModel(groupTable.getSelectedRow());
		final String groupXRef = (String)model.getValueAt(index, TABLE_INDEX_GROUP_ID);
		final GedcomNode selectedGroup;
		if(StringUtils.isBlank(groupXRef))
			selectedGroup = store.traverseAsList(container, "GROUP[]")
				.get(index);
		else
			selectedGroup = store.getGroup(groupXRef);

		container.removeChild(selectedGroup);

		loadData();
	}

	public final boolean loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		return loadData();
	}

	private boolean loadData(){
		final List<GedcomNode> groups = store.traverseAsList(container, "GROUP[]");
		final int size = groups.size();
		for(int i = 0; i < size; i ++){
			final String groupXRef = groups.get(i).getXRef();
			final GedcomNode group = store.getGroup(groupXRef);
			groups.set(i, group);
		}

		final DefaultTableModel groupsModel = (DefaultTableModel)groupTable.getModel();
		groupsModel.setRowCount(size);
		for(int row = 0; row < size; row ++){
			final GedcomNode group = groups.get(row);

			groupsModel.setValueAt(group.getID(), row, TABLE_INDEX_GROUP_ID);
			groupsModel.setValueAt(store.traverse(group, "NAME").getValue(), row, TABLE_INDEX_GROUP_NAME);
			groupsModel.setValueAt(store.traverse(group, "TYPE").getValue(), row, TABLE_INDEX_GROUP_TYPE);
		}
		return (size > 0);
	}

	private void filterTableBy(final GroupCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(text, TABLE_INDEX_GROUP_ID, TABLE_INDEX_GROUP_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)groupTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)groupTable.getModel();
			sorter = new TableRowSorter<>(model);
			groupTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class GroupTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -2985688516803729157L;


		GroupTableModel(){
			super(new String[]{"ID", "Name", "Type"}, 0);
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
		final GedcomNode individual = store.getIndividuals().get(0);

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
						case GROUP -> {
							final GroupRecordDialog dialog = new GroupRecordDialog(store, parent);
							final GedcomNode group = editCommand.getContainer();
							dialog.setTitle(group.getID() != null
								? "Group " + group.getID()
								: "New group for " + individual.getID());
							dialog.loadData(group, editCommand.getOnCloseGracefully());

							dialog.setSize(300, 250);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
							final GedcomNode noteCitation = editCommand.getContainer();
							dialog.setTitle(noteCitation.getID() != null
								? "Note citation " + noteCitation.getID() + " for individual " + individual.getID()
								: "New note citation for individual " + individual.getID());
							if(!dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully()))
								//show a note input dialog
								dialog.addAction();

							dialog.setSize(450, 260);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE -> {
							final NoteRecordDialog dialog = NoteRecordDialog.createNote(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Note " + note.getID()
								: "New note for " + individual.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final GroupCitationDialog dialog = new GroupCitationDialog(store, parent);
			dialog.setTitle("Group citations");
			if(!dialog.loadData(individual, null))
				dialog.addAction();

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
