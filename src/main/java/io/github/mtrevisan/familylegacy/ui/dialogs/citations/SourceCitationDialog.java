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
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.SourceDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.TableHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class SourceCitationDialog extends JDialog{

	private static final long serialVersionUID = 8355033011385629078L;

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int ID_PREFERRED_WIDTH = 25;

	private static final int TABLE_INDEX_SOURCE_ID = 0;
	private static final int TABLE_INDEX_SOURCE_TYPE = 1;
	private static final int TABLE_INDEX_SOURCE_TITLE = 2;

	private static final double CUTOUT_HEIGHT = 17.;
	private static final double CUTOUT_ASPECT_RATIO = 270 / 248.;
	private static final Dimension CUTOUT_SIZE = new Dimension((int)(CUTOUT_HEIGHT / CUTOUT_ASPECT_RATIO), (int)CUTOUT_HEIGHT);

	//https://thenounproject.com/search/?q=cut&i=3132059
	private static final ImageIcon CUTOUT = ResourceHelper.getImage("/images/cutout.png", CUTOUT_SIZE);

	private static final DefaultComboBoxModel<String> CREDIBILITY_MODEL = new DefaultComboBoxModel<>(new String[]{
		StringUtils.EMPTY,
		"Unreliable/estimated data",
		"Questionable reliability of evidence",
		"Secondary evidence, data officially recorded sometime after event",
		"Direct and primary evidence used, or by dominance of the evidence"});

	private static final String KEY_SOURCE_ID = "sourceID";
	private static final String KEY_SOURCE_FILE = "sourceFile";
	private static final String KEY_SOURCE_CUTOUT = "sourceCut";

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
	private final JButton cutoutButton = new JButton(CUTOUT);
	private final JButton notesButton = new JButton("Notes");
	private final JLabel credibilityLabel = new JLabel("Credibility:");
	private final JComboBox<String> credibilityComboBox = new JComboBox<>(CREDIBILITY_MODEL);
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<SourceCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private Runnable onCloseGracefully;
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
				selectAction(selectedRow);
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
				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}
			});
		sourcesTable.setPreferredScrollableViewportSize(new Dimension(sourcesTable.getPreferredSize().width,
			sourcesTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> addAction());

		titleLabel.setLabelFor(titleField);
		titleField.setEnabled(false);

		locationLabel.setLabelFor(locationField);
		locationField.setEnabled(false);

		roleLabel.setLabelFor(roleField);
		roleField.setEnabled(false);

		cutoutButton.setToolTipText("Define a cutout");
		cutoutButton.setEnabled(false);
		cutoutButton.addActionListener(evt -> cutoutAction());

		notesButton.setEnabled(false);
		notesButton.addActionListener(evt -> {
			final String selectedSourceID = (String)okButton.getClientProperty(KEY_SOURCE_ID);
			final GedcomNode selectedSource = store.getSource(selectedSourceID);
			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedSource));
		});

		credibilityLabel.setLabelFor(credibilityComboBox);
		credibilityComboBox.setEnabled(false);

		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.run();

			//TODO remember, when saving the whole gedcom, to remove all non-referenced source citations!

			dispose();
		});
		cancelButton.addActionListener(evt -> dispose());

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(sourcesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button2,wrap paragraph");
		add(titleLabel, "align label,sizegroup label,split 2");
		add(titleField, "grow,wrap");
		add(locationLabel, "align label,sizegroup label,split 2");
		add(locationField, "grow,wrap");
		add(roleLabel, "align label,sizegroup label,split 2");
		add(roleField, "grow,wrap");
		add(cutoutButton, "wrap");
		add(notesButton, "sizegroup button,grow,wrap paragraph");
		add(credibilityLabel, "align label,sizegroup label,split 2");
		add(credibilityComboBox, "grow,wrap paragraph");
		add(okButton, "tag ok,split 2,sizegroup button2");
		add(cancelButton, "tag cancel,sizegroup button2");
	}

	private void cutoutAction(){
		//TODO
		final GedcomNode fileNode = store.create("FILE")
			.addChildValue("SOURCE", (String)cutoutButton.getClientProperty(KEY_SOURCE_FILE));
//			.addChildValue("CUTOUT", (String)cutoutButton.getClientProperty(KEY_SOURCE_CUTOUT))
//			.addChildValue("PREFERRED", (String)cutoutButton.getClientProperty(KEY_SOURCE_PREFERRED));

		final Runnable onCloseGracefully = () -> {
//			cutoutButton.putClientProperty(KEY_SOURCE_CUTOUT, fileNode.getChildrenWithTag("CUTOUT").get(0).getValue());
//			cutoutButton.putClientProperty(KEY_SOURCE_PREFERRED, fileNode.getChildrenWithTag("PREFERRED").get(0).getValue());

			//refresh group list
			loadData();
		};

		//fire image cutout event
		EventBusService.publish(new EditEvent(EditEvent.EditType.IMAGE, fileNode, onCloseGracefully));
	}

	private void okAction(){
		final String id = (String)okButton.getClientProperty(KEY_SOURCE_ID);
		final String location = locationField.getText();
		final String role = roleField.getText();
		final String file = (String)cutoutButton.getClientProperty(KEY_SOURCE_FILE);
		final String cutout = (String)cutoutButton.getClientProperty(KEY_SOURCE_CUTOUT);
		final int credibility = credibilityComboBox.getSelectedIndex() - 1;

		final GedcomNode group = store.traverse(container, "SOURCE@" + id);
		group.replaceChildValue("LOCATION", location);
		group.replaceChildValue("ROLE", role);
//		group.replaceChildValue("CUTOUT", cutout);
		group.replaceChildValue("FILE", file);
		group.replaceChildValue("CREDIBILITY", (credibility >= 0? Integer.toString(credibility): null));
	}

	private void selectAction(final int selectedRow){
		final String selectedSourceID = (String)sourcesTable.getValueAt(selectedRow, TABLE_INDEX_SOURCE_ID);
		final GedcomNode selectedSourceCitation = store.traverse(container, "SOURCE@" + selectedSourceID);
		final GedcomNode selectedSource = store.getSource(selectedSourceID);
		okButton.putClientProperty(KEY_SOURCE_ID, selectedSourceID);
		titleField.setText(store.traverse(selectedSource, "TITLE").getValue());

		locationField.setEnabled(true);
		locationField.setText(store.traverse(selectedSourceCitation, "LOCATION").getValue());
		roleField.setEnabled(true);
		roleField.setText(store.traverse(selectedSourceCitation, "ROLE").getValue());
		cutoutButton.setEnabled(true);
		cutoutButton.putClientProperty(KEY_SOURCE_FILE, store.traverse(selectedSource, "FILE").getValue());
		cutoutButton.putClientProperty(KEY_SOURCE_CUTOUT, store.traverse(selectedSourceCitation, "CUTOUT").getValue());
		notesButton.setEnabled(true);
		credibilityComboBox.setEnabled(true);
		credibilityComboBox.setEnabled(true);
		final String credibility = store.traverse(selectedSourceCitation, "CREDIBILITY").getValue();
		credibilityComboBox.setSelectedIndex(credibility != null? Integer.parseInt(credibility) + 1: 0);

		okButton.setEnabled(true);
	}

	private void addAction(){
		final GedcomNode newSource = store.create("SOURCE");

		final Runnable onCloseGracefully = () -> {
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
		model.removeRow(sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()));
	}

	public void loadData(final GedcomNode container, final Runnable onCloseGracefully){
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
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_SOURCE_ID, TABLE_INDEX_SOURCE_TYPE,
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


	private static class SourceTableModel extends DefaultTableModel{

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
		final GedcomNode container = store.getIndividuals().get(0);

		EventQueue.invokeLater(() -> {
			final JFrame parent = new JFrame();
			final Object listener = new Object(){
				@EventHandler
				public void refresh(final EditEvent editCommand){
					JDialog dialog = null;
					switch(editCommand.getType()){
						case SOURCE:
							dialog = new SourceDialog(store, parent);
							((SourceDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(550, 440);
							break;

						case NOTE_CITATION:
							dialog = new NoteCitationDialog(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer());

							dialog.setSize(450, 260);
							break;

						case NOTE:
							dialog = new NoteDialog(store, parent);
							((NoteDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());

							dialog.setSize(550, 350);
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
