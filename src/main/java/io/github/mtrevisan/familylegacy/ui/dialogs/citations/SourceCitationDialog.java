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
import io.github.mtrevisan.familylegacy.ui.dialogs.CropDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.SourceRecordDialog;
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
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;


//TODO
public class SourceCitationDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 8355033011385629078L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final KeyStroke INSERT_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_SOURCE_ID = 0;
	private static final int TABLE_INDEX_SOURCE_TYPE = 1;
	private static final int TABLE_INDEX_SOURCE_TITLE = 2;

	private static final double CROP_HEIGHT = 17.;
	private static final double CROP_ASPECT_RATIO = 270 / 248.;
	private static final Dimension CROP_SIZE = new Dimension((int)(CROP_HEIGHT / CROP_ASPECT_RATIO), (int)CROP_HEIGHT);

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon CROP = ResourceHelper.getImage("/images/crop.png", CROP_SIZE);

	private static final CredibilityComboBoxModel CREDIBILITY_MODEL = new CredibilityComboBoxModel();

	private static final String KEY_SOURCE_ID = "sourceID";
	private static final String KEY_SOURCE_FILE = "sourceFile";
	private static final String KEY_SOURCE_CROP = "sourceCrop";

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable sourcesTable = new JTable(new SourceTableModel());
	private final JScrollPane sourcesScrollPane = new JScrollPane(sourcesTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel titleLabel = new JLabel("Title:");
	private final JTextField titleField = new JTextField();
	private final JLabel locationLabel = new JLabel("Location:");
	private final JTextField locationField = new JTextField();
	private final JLabel roleLabel = new JLabel("Role:");
	private final JTextField roleField = new JTextField();
	private final JButton cropButton = new JButton(CROP);
	private final JButton noteButton = new JButton("Notes");
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<SourceCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public SourceCitationDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();
	}

	private void initComponents(){
		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(SourceCitationDialog.this);
			}
		});

		sourcesTable.setAutoCreateRowSorter(true);
		sourcesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		sourcesTable.setGridColor(GRID_COLOR);
		sourcesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sourcesTable.setDragEnabled(true);
		sourcesTable.setDropMode(DropMode.INSERT_ROWS);
		sourcesTable.setTransferHandler(new TableTransferHandle(sourcesTable));
		sourcesTable.getTableHeader().setFont(sourcesTable.getFont().deriveFont(Font.BOLD));
		TableHelper.setColumnWidth(sourcesTable, TABLE_INDEX_SOURCE_ID, 0, ID_PREFERRED_WIDTH);
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(sourcesTable.getModel());
		sorter.setComparator(TABLE_INDEX_SOURCE_ID, (Comparator<String>)GedcomNode::compareID);
		sorter.setComparator(TABLE_INDEX_SOURCE_TYPE, Comparator.naturalOrder());
		sorter.setComparator(TABLE_INDEX_SOURCE_TITLE, Comparator.naturalOrder());
		sourcesTable.setRowSorter(sorter);
		//clicking on a line links it to current source citation
		sourcesTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = sourcesTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(sourcesTable.convertRowIndexToModel(selectedRow));
		});
		sourcesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && sourcesTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		final InputMap sourcesTableInputMap = sourcesTable.getInputMap(JComponent.WHEN_FOCUSED);
		sourcesTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0), "insert");
		sourcesTableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		final ActionMap sourcesTableActionMap = sourcesTable.getActionMap();
		sourcesTableActionMap.put("insert", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				addAction();
			}
		});
		sourcesTableActionMap.put("delete", new AbstractAction(){
			@Override
			public void actionPerformed(final ActionEvent evt){
				deleteAction();
			}
		});
		sourcesTable.setPreferredScrollableViewportSize(new Dimension(sourcesTable.getPreferredSize().width,
			sourcesTable.getRowHeight() * 5));

		titleLabel.setLabelFor(titleField);
		GUIHelper.setEnabled(titleLabel, false);

		locationLabel.setLabelFor(locationField);
		GUIHelper.setEnabled(locationLabel, false);

		roleLabel.setLabelFor(roleField);
		GUIHelper.setEnabled(roleLabel, false);

		cropButton.setToolTipText("Define a crop");
		cropButton.setEnabled(false);
		cropButton.addActionListener(evt -> cropAction());

		noteButton.setEnabled(false);
		noteButton.addActionListener(evt -> {
			final String selectedSourceID = (String)okButton.getClientProperty(KEY_SOURCE_ID);
			final GedcomNode selectedSource = store.getSource(selectedSourceID);
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedSource));
		});

		credibilityLabel.setLabelFor(credibilityComboBox);
		GUIHelper.setEnabled(credibilityLabel, false);

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

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]", "[][grow,fill][][][][][][][]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(sourcesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(titleLabel, "align label,sizegroup label,split 2");
		add(titleField, "grow,wrap");
		add(locationLabel, "align label,sizegroup label,split 2");
		add(locationField, "grow,wrap");
		add(roleLabel, "align label,sizegroup label,split 2");
		add(roleField, "grow,wrap");
		add(cropButton, "wrap");
		add(noteButton, "sizegroup button,grow,wrap paragraph");
		add(credibilityLabel, "align label,sizegroup label,split 2");
		add(credibilityComboBox, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button2");
		add(okButton, "tag ok,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void cropAction(){
		final GedcomNode fileNode = store.create("FILE")
			.addChildValue("SOURCE", (String)cropButton.getClientProperty(KEY_SOURCE_FILE))
			.addChildValue("CROP", (String)cropButton.getClientProperty(KEY_SOURCE_CROP));

		final Consumer<Object> onCloseGracefully = cropDialog -> {
			final Point cropStartPoint = ((CropDialog)cropDialog).getCropStartPoint();
			final Point cropEndPoint = ((CropDialog)cropDialog).getCropEndPoint();
			final StringJoiner sj = new StringJoiner(StringUtils.SPACE);
			sj.add(Integer.toString(cropStartPoint.x));
			sj.add(Integer.toString(cropStartPoint.y));
			sj.add(Integer.toString(cropEndPoint.x));
			sj.add(Integer.toString(cropEndPoint.y));

			cropButton.putClientProperty(KEY_SOURCE_CROP, sj.toString());

			//refresh group list
			loadData();
		};

		//fire image crop event
		EventBusService.publish(new EditEvent(EditEvent.EditType.CROP, fileNode, onCloseGracefully));
	}

	private void okAction(){
		final String id = (String)okButton.getClientProperty(KEY_SOURCE_ID);
		final String location = locationField.getText();
		final String role = roleField.getText();
		final String file = (String)cropButton.getClientProperty(KEY_SOURCE_FILE);
		final String crop = (String)cropButton.getClientProperty(KEY_SOURCE_CROP);
		final int credibility = credibilityComboBox.getSelectedIndex() - 1;

		final GedcomNode group = store.traverse(container, "SOURCE@" + id);
		group.replaceChildValue("LOCATION", location);
		group.replaceChildValue("ROLE", role);
		group.replaceChildValue("CROP", crop);
		group.replaceChildValue("FILE", file);
		group.replaceChildValue("CREDIBILITY", (credibility >= 0? Integer.toString(credibility): null));
	}

	private void selectAction(final int selectedRow){
		final String selectedSourceID = (String)sourcesTable.getValueAt(selectedRow, TABLE_INDEX_SOURCE_ID);
		final GedcomNode selectedSourceCitation = store.traverse(container, "SOURCE@" + selectedSourceID);
		final GedcomNode selectedSource = store.getSource(selectedSourceID);
		okButton.putClientProperty(KEY_SOURCE_ID, selectedSourceID);
		GUIHelper.setEnabled(titleLabel, true);
		titleField.setText(store.traverse(selectedSource, "TITLE").getValue());

		GUIHelper.setEnabled(locationLabel, true);
		locationField.setText(store.traverse(selectedSourceCitation, "LOCATION").getValue());
		GUIHelper.setEnabled(roleLabel, true);
		roleField.setText(store.traverse(selectedSourceCitation, "ROLE").getValue());
		cropButton.setEnabled(true);
		cropButton.putClientProperty(KEY_SOURCE_FILE, store.traverse(selectedSource, "FILE").getValue());
		cropButton.putClientProperty(KEY_SOURCE_CROP, store.traverse(selectedSourceCitation, "CROP").getValue());
		noteButton.setEnabled(true);
		GUIHelper.setEnabled(credibilityLabel, true);
		final String credibility = store.traverse(selectedSourceCitation, "CREDIBILITY").getValue();
		credibilityComboBox.setSelectedIndex(!credibility.isEmpty()? Integer.parseInt(credibility) + 1: 0);

		okButton.setEnabled(true);
	}

	public final void addAction(){
		final GedcomNode newSource = store.create("SOURCE");

		final Consumer<Object> onCloseGracefully = dialog -> {
			//if ok was pressed, add this source to the parent container
			final String newSourceID = store.addSource(newSource);
			container.addChildReference("SOURCE", newSourceID);

			//refresh source list
			loadData();
		};

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, newSource, onCloseGracefully));
	}

	private void editAction(){
		//retrieve selected source
		final DefaultTableModel model = (DefaultTableModel)sourcesTable.getModel();
		final int index = sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow());
		final String sourceXRef = (String)model.getValueAt(index, TABLE_INDEX_SOURCE_ID);
		final GedcomNode selectedSource = store.getSource(sourceXRef);

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.SOURCE, selectedSource));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)sourcesTable.getModel();
		final int index = sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow());
		final String sourceXRef = (String)model.getValueAt(index, TABLE_INDEX_SOURCE_ID);
		final GedcomNode selectedSource = store.getSource(sourceXRef);

		container.removeChild(selectedSource);

		loadData();
	}

	public final boolean loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		return loadData();
	}

	private boolean loadData(){
		final List<GedcomNode> sources = store.traverseAsList(container, "SOURCE[]");
		final int size = sources.size();
		for(int i = 0; i < size; i ++){
			final String sourceXRef = sources.get(i).getXRef();
			final GedcomNode note = store.getSource(sourceXRef);
			sources.set(i, note);
		}

		final DefaultTableModel sourcesModel = (DefaultTableModel)sourcesTable.getModel();
		sourcesModel.setRowCount(size);
		for(int row = 0; row < size; row ++){
			final GedcomNode source = sources.get(row);

			sourcesModel.setValueAt(source.getID(), row, TABLE_INDEX_SOURCE_ID);
			final List<GedcomNode> events = store.traverseAsList(source, "EVENT[]");
			final StringJoiner sj = new StringJoiner(", ");
			for(final GedcomNode event : events)
				sj.add(event.getValue());
			sourcesModel.setValueAt(sj.toString(), row, TABLE_INDEX_SOURCE_TYPE);
			sourcesModel.setValueAt(store.traverse(source, "TITLE").getValue(), row, TABLE_INDEX_SOURCE_TITLE);
		}
		return (size > 0);
	}

	private void filterTableBy(final SourceCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = TableHelper.createTextFilter(text, TABLE_INDEX_SOURCE_ID, TABLE_INDEX_SOURCE_TYPE,
			TABLE_INDEX_SOURCE_TITLE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)sourcesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)sourcesTable.getModel();
			sorter = new TableRowSorter<>(model);
			sourcesTable.setRowSorter(sorter);
		}
		sorter.setRowFilter(filter);
	}


	private static class SourceTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -3229928471735627084L;


		SourceTableModel(){
			super(new String[]{"ID", "Type", "Title"}, 0);
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
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void error(final BusExceptionEvent exceptionEvent){
					final Throwable cause = exceptionEvent.getCause();
					JOptionPane.showMessageDialog(parent, cause.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}

				@EventHandler
				public void refresh(final EditEvent editCommand) throws IOException{
					switch(editCommand.getType()){
						case SOURCE -> {
							final SourceRecordDialog dialog = new SourceRecordDialog(store, parent);
							final GedcomNode note = editCommand.getContainer();
							dialog.setTitle(note.getID() != null
								? "Source " + note.getID()
								: "New source for " + container.getID());
							dialog.loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(500, 540);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case NOTE_CITATION -> {
							final NoteCitationDialog dialog = NoteCitationDialog.createNoteCitation(store, parent);
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
							dialog.setTitle("Note " + note.getID());
							dialog.loadData(note, editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
						case CROP -> {
							final CropDialog dialog = new CropDialog(parent);
							final GedcomNode container = editCommand.getContainer();
							//TODO add base path?
							final String imagePath = store.traverse(container, "SOURCE")
								.getValue();
							final String crop = store.traverse(container, "CROP")
								.getValue();
							final String[] coordinates = (!crop.isEmpty()? StringUtils.split(crop, ' '): null);
							dialog.loadData(imagePath, editCommand.getOnCloseGracefully());
							if(coordinates != null){
								dialog.setCropStartPoint(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
								dialog.setCropEndPoint(Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3]));
							}

							dialog.setSize(500, 480);
							dialog.setLocationRelativeTo(parent);
							dialog.setVisible(true);
						}
					}
				}
			};
			EventBusService.subscribe(listener);

			final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
			dialog.setTitle(container.isEmpty()? "Source citations": "Source citations for " + container.getID());
			if(!dialog.loadData(container, null))
				//show a note input dialog
				dialog.addAction();

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 460);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
