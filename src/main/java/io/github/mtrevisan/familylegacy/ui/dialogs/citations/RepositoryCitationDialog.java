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
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class RepositoryCitationDialog extends JDialog{

	private static final long serialVersionUID = 4839647166680455355L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_REPOSITORY_ID = 0;
	private static final int TABLE_INDEX_REPOSITORY_NAME = 1;

	private static final String KEY_REPOSITORY_ID = "repositoryID";
	private static final String KEY_REPOSITORY_FILE = "repositoryFile";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable repositoryTable = new JTable(new RepositoryTableModel());
	private final JScrollPane repositoriesScrollPane = new JScrollPane(repositoryTable);
	private final JButton addButton = new JButton("Add");
	private final JButton editButton = new JButton("Edit");
	private final JButton removeButton = new JButton("Remove");
	private final JLabel repositoryNameLabel = new JLabel("Name:");
	private final JTextField repositoryNameField = new JTextField();
	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JButton notesButton = new JButton("Notes");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<RepositoryCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private final Flef store;


	public RepositoryCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Repository citations");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(RepositoryCitationDialog.this);
			}
		});

		repositoryTable.setAutoCreateRowSorter(true);
		repositoryTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		repositoryTable.setGridColor(GRID_COLOR);
		repositoryTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		repositoryTable.setDragEnabled(true);
		repositoryTable.setDropMode(DropMode.INSERT_ROWS);
		repositoryTable.setTransferHandler(new TableTransferHandle(repositoryTable));
		repositoryTable.getTableHeader().setFont(repositoryTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(repositoryTable, TABLE_INDEX_REPOSITORY_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(repositoryTable.getModel());
		sorter.setComparator(TABLE_INDEX_REPOSITORY_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_REPOSITORY_NAME, Comparator.naturalOrder());
		repositoryTable.setRowSorter(sorter);
		//clicking on a line links it to current repository citation
		repositoryTable.getSelectionModel().addListSelectionListener(evt -> {
			removeButton.setEnabled(true);

			final int selectedRow = repositoryTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0){
				final String selectedRepositoryID = (String)repositoryTable.getValueAt(selectedRow, TABLE_INDEX_REPOSITORY_ID);
				final GedcomNode selectedRepositoryCitation = store.traverse(container, "REPOSITORY@" + selectedRepositoryID);
				final GedcomNode selectedRepository = store.getRepository(selectedRepositoryID);
				okButton.putClientProperty(KEY_REPOSITORY_ID, selectedRepositoryID);
				repositoryNameField.setText(store.traverse(selectedRepository, "NAME").getValue());

				locationField.setEnabled(true);
				locationField.setText(store.traverse(selectedRepositoryCitation, "LOCATION").getValue());
				notesButton.setEnabled(true);
				notesButton.setEnabled(!store.traverseAsList(selectedRepositoryCitation, "NOTE[]").isEmpty());

				okButton.setEnabled(true);
			}
		});
		repositoryTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && repositoryTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});

		addButton.addActionListener(evt -> {
			final GedcomNode newRepository = store.create("REPOSITORY");

			final Consumer<Object> onCloseGracefully = ignored -> {
				//if ok was pressed, add this repository to the parent container
				final String newRepositoryID = store.addRepository(newRepository);
				container.addChildReference("REPOSITORY", newRepositoryID);

				//refresh group list
				loadData();
			};

			//fire edit event
			EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, newRepository, onCloseGracefully));
		});
		editButton.addActionListener(evt -> editAction());
		removeButton.setEnabled(false);
		removeButton.addActionListener(evt -> deleteAction());

		repositoryNameLabel.setLabelFor(repositoryNameField);
		repositoryNameField.setEnabled(false);

		locationLabel.setLabelFor(locationField);
		locationField.setEnabled(false);

		notesButton.setEnabled(false);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			final String id = (String)okButton.getClientProperty(KEY_REPOSITORY_ID);
			final String location = locationField.getText();

			final GedcomNode group = store.traverse(container, "REPOSITORY@" + id);
			group.replaceChildValue("LOCATION", location);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced repositories!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(repositoriesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2");
		add(editButton, "tag edit,sizegroup button2");
		add(removeButton, "tag remove,sizegroup button2,wrap paragraph");
		add(repositoryNameLabel, "align label,sizegroup label,split 2");
		add(repositoryNameField, "grow,wrap");
		add(locationLabel, "align label,split 2");
		add(locationField, "grow,wrap paragraph");
		add(notesButton, "sizegroup button,grow,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void editAction(){
		//retrieve selected repository
		final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
		final int index = repositoryTable.convertRowIndexToModel(repositoryTable.getSelectedRow());
		final String repositoryXRef = (String)model.getValueAt(index, TABLE_INDEX_REPOSITORY_ID);
		final GedcomNode selectedRepository = store.getRepository(repositoryXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, selectedRepository));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
		model.removeRow(repositoryTable.convertRowIndexToModel(repositoryTable.getSelectedRow()));
		removeButton.setEnabled(false);
	}

	public void loadData(final GedcomNode container){
		this.container = container;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> repositories = store.traverseAsList(container, "REPOSITORY[]");
		final int size = repositories.size();
		for(int i = 0; i < size; i ++)
			repositories.set(i, store.getRepository(repositories.get(i).getXRef()));

		if(size > 0){
			final DefaultTableModel repositoriesModel = (DefaultTableModel)repositoryTable.getModel();
			repositoriesModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode repository = repositories.get(row);

				repositoriesModel.setValueAt(repository.getID(), row, TABLE_INDEX_REPOSITORY_ID);
				repositoriesModel.setValueAt(store.traverse(repository, "NAME").getValue(), row, TABLE_INDEX_REPOSITORY_NAME);
			}
		}
	}

	private void filterTableBy(final RepositoryCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_REPOSITORY_ID, TABLE_INDEX_REPOSITORY_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)repositoryTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
			sorter = new TableRowSorter<>(model);
			repositoryTable.setRowSorter(sorter);
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


	private static class RepositoryTableModel extends DefaultTableModel{

		private static final long serialVersionUID = 4104009208535269036L;


		RepositoryTableModel(){
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


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.5.gedg", "src/main/resources/ged/small.flef.ged")
			.transform();
		final GedcomNode sourceCitation = store.traverseAsList(store.getIndividuals().get(0), "SOURCE[]").get(0);
		final GedcomNode container = store.getSource(sourceCitation.getXRef());

		EventQueue.invokeLater(() -> {
			final RepositoryCitationDialog dialog = new RepositoryCitationDialog(store, new JFrame());
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
