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
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.CropDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.SourceRecordDialog;
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;


public class SourceCitationDialog extends JDialog implements ActionListener{

	@Serial
	private static final long serialVersionUID = 8355033011385629078L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

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

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

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
	private final JButton notesButton = new JButton("Notes");
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

		loadData();
	}

	private void initComponents(){
		setTitle("Source citations");

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
		sourcesTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		sourcesTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Serial
				private static final long serialVersionUID = -6131110796684649318L;

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
		sourcesTable.setPreferredScrollableViewportSize(new Dimension(sourcesTable.getPreferredSize().width,
			sourcesTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> addAction());

		titleLabel.setLabelFor(titleField);
		GUIHelper.setEnabled(titleLabel, false);

		locationLabel.setLabelFor(locationField);
		GUIHelper.setEnabled(locationLabel, false);

		roleLabel.setLabelFor(roleField);
		GUIHelper.setEnabled(roleLabel, false);

		cropButton.setToolTipText("Define a crop");
		cropButton.setEnabled(false);
		cropButton.addActionListener(evt -> cropAction());

		notesButton.setEnabled(false);
		notesButton.addActionListener(evt -> {
			final String selectedSourceID = (String)okButton.getClientProperty(KEY_SOURCE_ID);
			final GedcomNode selectedSource = store.getSource(selectedSourceID);
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedSource));
		});

		credibilityLabel.setLabelFor(credibilityComboBox);
		GUIHelper.setEnabled(credibilityLabel, false);

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced source citations!

			dispose();
		});
		getRootPane().registerKeyboardAction(this, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		cancelButton.addActionListener(this::actionPerformed);

		setLayout(new MigLayout(StringUtils.EMPTY, "[grow]"));
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
		add(notesButton, "sizegroup button,grow,wrap paragraph");
		add(credibilityLabel, "align label,sizegroup label,split 2");
		add(credibilityComboBox, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
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
		notesButton.setEnabled(true);
		GUIHelper.setEnabled(credibilityLabel, true);
		final String credibility = store.traverse(selectedSourceCitation, "CREDIBILITY").getValue();
		credibilityComboBox.setSelectedIndex(!credibility.isEmpty()? Integer.parseInt(credibility) + 1: 0);

		okButton.setEnabled(true);
	}

	private void addAction(){
		final GedcomNode newSource = store.create("SOURCE");

		final Consumer<Object> onCloseGracefully = dialog -> {
			//if ok was pressed, add this source to the parent container
			final String newSourceID = store.addSource(newSource);
			container.addChildReference("SOURCE", newSourceID);

			//refresh group list
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
		model.removeRow(index);
	}

	public void loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> sources = store.traverseAsList(container, "SOURCE[]");
		final int size = sources.size();
		for(int i = 0; i < size; i ++)
			sources.set(i, store.getSource(sources.get(i).getXRef()));

		if(size > 0){
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
		}
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

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
	}


	private static class SourceTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = -3229928471735627084L;


		SourceTableModel(){
			super(new String[]{"ID", "Type", "Title"}, 0);
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
					JDialog dialog = null;
					switch(editCommand.getType()){
						case SOURCE -> {
							dialog = new SourceRecordDialog(store, parent);
							((SourceRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(500, 540);
						}
						case NOTE_CITATION -> {
							dialog = new NoteCitationDialog(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer());
							dialog.setSize(450, 260);
						}
						case NOTE -> {
							dialog = NoteRecordDialog.createNote(store, parent);
							((NoteRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(550, 350);
						}
						case CROP -> {
							dialog = new CropDialog(parent);
							final GedcomNode container = editCommand.getContainer();
							//TODO add base path?
							final String imagePath = store.traverse(container, "SOURCE")
								.getValue();
							final String crop = store.traverse(container, "CROP")
								.getValue();
							final String[] coordinates = (!crop.isEmpty()? StringUtils.split(crop, ' '): null);
							((CropDialog)dialog).loadData(imagePath, editCommand.getOnCloseGracefully());
							if(coordinates != null){
								((CropDialog)dialog).setCropStartPoint(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
								((CropDialog)dialog).setCropEndPoint(Integer.parseInt(coordinates[2]), Integer.parseInt(coordinates[3]));
							}
							dialog.setSize(500, 480);
						}
					}
					if(dialog != null){
						dialog.setLocationRelativeTo(parent);
						dialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final SourceCitationDialog dialog = new SourceCitationDialog(store, parent);
			dialog.loadData(container, null);

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
