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
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RepositoryRecordDialog;
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


public class RepositoryCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 4839647166680455355L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

	private static final ImageIcon ICON_NOTE = ResourceHelper.getImage("/images/note.png", 20, 20);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_REPOSITORY_ID = 0;
	private static final int TABLE_INDEX_REPOSITORY_NAME = 1;

	private static final String KEY_REPOSITORY_ID = "repositoryID";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable repositoryTable = new JTable(new RepositoryTableModel());
	private final JScrollPane repositoriesScrollPane = new JScrollPane(repositoryTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JButton noteButton = new JButton(ICON_NOTE);
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<RepositoryCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public RepositoryCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
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
			final int selectedRow = repositoryTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(repositoryTable.convertRowIndexToModel(selectedRow));
		});
		repositoryTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && repositoryTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		final InputMap repositoryTableInputMap = repositoryTable.getInputMap(JComponent.WHEN_FOCUSED);
		repositoryTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert");
		repositoryTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		final ActionMap repositoryTableActionMap = repositoryTable.getActionMap();
		repositoryTableActionMap.put("insert", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				addAction();
			}
		});
		repositoryTableActionMap.put("delete", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});

		locationLabel.setLabelFor(locationField);
		GUIHelper.setEnabled(locationLabel, false);

		noteButton.setToolTipText("Add note");
		noteButton.setEnabled(false);
		noteButton.addActionListener(evt -> {
			final String selectedRepositoryID = (String)okButton.getClientProperty(KEY_REPOSITORY_ID);
			final GedcomNode selectedRepository = store.getSource(selectedRepositoryID);
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedRepository));
		});

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
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(repositoriesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(locationLabel, "align label,split 2");
		add(locationField, "grow,wrap");
		add(noteButton, "sizegroup button,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void selectAction(final int selectedRow){
		final String selectedRepositoryID = (String)repositoryTable.getValueAt(selectedRow, TABLE_INDEX_REPOSITORY_ID);
		final GedcomNode selectedRepositoryCitation = store.traverse(container, "REPOSITORY@" + selectedRepositoryID);
		okButton.putClientProperty(KEY_REPOSITORY_ID, selectedRepositoryID);

		GUIHelper.setEnabled(locationLabel, true);
		locationField.setText(store.traverse(selectedRepositoryCitation, "LOCATION").getValue());

		noteButton.setEnabled(true);

		okButton.setEnabled(true);
	}

	public final void addAction(){
		final GedcomNode newRepository = store.create("REPOSITORY");

		final Consumer<Object> onCloseGracefully = dialog -> {
			//if ok was pressed, add this source to the parent container
			final String newRepositoryID = store.addRepository(newRepository);
			container.addChildReference("REPOSITORY", newRepositoryID);

			//refresh source list
			loadData();
		};

		EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, newRepository, onCloseGracefully));
	}

	private void editAction(){
		//retrieve selected repository
		final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
		final int index = repositoryTable.convertRowIndexToModel(repositoryTable.getSelectedRow());
		final String repositoryXRef = (String)model.getValueAt(index, TABLE_INDEX_REPOSITORY_ID);
		final GedcomNode selectedRepository;
		if(StringUtils.isBlank(repositoryXRef))
			selectedRepository = store.traverseAsList(container, "REPOSITORY[]")
				.get(index);
		else
			selectedRepository = store.getRepository(repositoryXRef);

		EventBusService.publish(new EditEvent(EditEvent.EditType.REPOSITORY, selectedRepository));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
		final int index = repositoryTable.convertRowIndexToModel(repositoryTable.getSelectedRow());
		final String repositoryXRef = (String)model.getValueAt(index, TABLE_INDEX_REPOSITORY_ID);
		final GedcomNode selectedRepository;
		if(StringUtils.isBlank(repositoryXRef))
			selectedRepository = store.traverseAsList(container, "REPOSITORY[]")
				.get(index);
		else
			selectedRepository = store.getRepository(repositoryXRef);

		container.removeChild(selectedRepository);

		loadData();
	}

	public final boolean loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		return loadData();
	}

	private boolean loadData(){
		final List<GedcomNode> repositories = store.traverseAsList(container, "REPOSITORY[]");
		final int size = repositories.size();
		for(int i = 0; i < size; i ++){
			final String repositoryXRef = repositories.get(i).getXRef();
			final GedcomNode repository = store.getRepository(repositoryXRef);
			repositories.set(i, repository);
		}

		final DefaultTableModel repositoriesModel = (DefaultTableModel)repositoryTable.getModel();
		repositoriesModel.setRowCount(size);
		for(int row = 0; row < size; row ++){
			final GedcomNode repository = repositories.get(row);

			repositoriesModel.setValueAt(repository.getID(), row, TABLE_INDEX_REPOSITORY_ID);
			repositoriesModel.setValueAt(store.traverse(repository, "NAME").getValue(), row, TABLE_INDEX_REPOSITORY_NAME);
		}
		return (size > 0);
	}

	private void filterTableBy(final RepositoryCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(text, TABLE_INDEX_REPOSITORY_ID, TABLE_INDEX_REPOSITORY_NAME);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)repositoryTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)repositoryTable.getModel();
			sorter = new TableRowSorter<>(model);
			repositoryTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class RepositoryTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 4104009208535269036L;


		RepositoryTableModel(){
			super(new String[]{"ID", "Name"}, 0);
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
		final GedcomNode sourceCitation = store.traverseAsList(store.getIndividuals().get(0), "SOURCE[]").get(0);
		final GedcomNode source = store.getSource(sourceCitation.getXRef());

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
						case REPOSITORY -> {
							final RepositoryRecordDialog dialog = new RepositoryRecordDialog(store, parent);
							final GedcomNode repository = editCommand.getContainer();
							dialog.setTitle(repository.getID() != null
								? "Repository " + repository.getID()
								: "New repository for " + source.getID());
							dialog.loadData(repository, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
							final GedcomNode noteCitation = editCommand.getContainer();
							dialog.setTitle(noteCitation.getID() != null
								? "Note citation " + noteCitation.getID() + " for source " + source.getID()
								: "New note citation for source " + source.getID());
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
								: "New note for " + source.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(500, 330);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_TRANSLATION -> {
							final NoteRecordDialog dialog = NoteRecordDialog.createNoteTranslation(store, parent);
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
						case SOURCE_CITATION -> {
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

			final RepositoryCitationDialog dialog = new RepositoryCitationDialog(store, parent);
			dialog.setTitle(source.isEmpty()? "Repository citations": "Repository citations for " + source.getID());
			if(!dialog.loadData(source, null))
				dialog.addAction();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 320);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
