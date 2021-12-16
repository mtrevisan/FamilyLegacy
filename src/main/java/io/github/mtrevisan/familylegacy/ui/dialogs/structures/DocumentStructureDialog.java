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
package io.github.mtrevisan.familylegacy.ui.dialogs.structures;

import io.github.mtrevisan.familylegacy.gedcom.Flef;
import io.github.mtrevisan.familylegacy.gedcom.GedcomGrammarParseException;
import io.github.mtrevisan.familylegacy.gedcom.GedcomNode;
import io.github.mtrevisan.familylegacy.gedcom.GedcomParseException;
import io.github.mtrevisan.familylegacy.gedcom.events.EditEvent;
import io.github.mtrevisan.familylegacy.services.ResourceHelper;
import io.github.mtrevisan.familylegacy.ui.dialogs.citations.NoteCitationDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.NoteRecordDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.utilities.ImagePreview;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class DocumentStructureDialog extends JDialog implements ActionListener, TextPreviewListenerInterface{

	@Serial
	private static final long serialVersionUID = 1919219115430275506L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final double OPEN_DOCUMENT_HEIGHT = 24.;
	private static final double OPEN_DOCUMENT_ASPECT_RATIO = 176. / 134.;
	private static final Dimension OPEN_DOCUMENT_SIZE = new Dimension((int)(OPEN_DOCUMENT_HEIGHT / OPEN_DOCUMENT_ASPECT_RATIO), (int)OPEN_DOCUMENT_HEIGHT);
	private static final ImageIcon OPEN_DOCUMENT = ResourceHelper.getImage("/images/openDocument.png", OPEN_DOCUMENT_SIZE);

	private static final int TABLE_INDEX_DOCUMENT_FILE = 0;

	private static final DefaultComboBoxModel<String> MAPPING_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"spherical_UV", "cylindrical_equirectangular_horizontal", "cylindrical_equirectangular_vertical"});
	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable filesTable = new JTable(new DocumentTableModel());
	private final JScrollPane filesScrollPane = new JScrollPane(filesTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel fileLabel = new JLabel("File:");
	private final JTextField fileField = new JTextField();
	private final JButton fileButton = new JButton(OPEN_DOCUMENT);
	private final JFileChooser fileChooser = new JFileChooser();
	private final JCheckBox sphericalCheckBox = new JCheckBox("Spherical");
	private final JLabel mappingLabel = new JLabel("Mapping:");
	private final JComboBox<String> mappingComboBox = new JComboBox<>(MAPPING_TYPE_MODEL);
	private final JLabel descriptionLabel = new JLabel("Description:");
	private final JTextField descriptionField = new JTextField();
	private TextPreviewPane extractPreviewView;
	private final JLabel extractTypeLabel = new JLabel("Type:");
	private final JComboBox<String> extractTypeComboBox = new JComboBox<>(EXTRACT_TYPE_MODEL);
	private final JLabel extractLocaleLabel = new JLabel("Locale:");
	private final LocaleFilteredComboBox extractLocaleComboBox = new LocaleFilteredComboBox();
	private final JCheckBox restrictionCheckBox = new JCheckBox("Confidential");
	private final JButton notesButton = new JButton("Notes");
	private final JButton helpButton = new JButton("Help");
	private final JButton okButton = new JButton("Ok");
	private final JButton cancelButton = new JButton("Cancel");

	private final Debouncer<DocumentStructureDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private volatile boolean updating;
	private int dataHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public DocumentStructureDialog(final Flef store, final Frame parent){
		super(parent, true);

		this.store = store;

		initComponents();

		loadData();
	}

	private void initComponents(){
		setTitle("Documents");

		filterLabel.setLabelFor(filterField);
		filterField.addKeyListener(new KeyAdapter(){
			public void keyReleased(final KeyEvent evt){
				filterDebouncer.call(DocumentStructureDialog.this);
			}
		});

		filesTable.setAutoCreateRowSorter(true);
		filesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		filesTable.setGridColor(GRID_COLOR);
		filesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		filesTable.setDragEnabled(true);
		filesTable.setDropMode(DropMode.INSERT_ROWS);
		filesTable.setTransferHandler(new TableTransferHandle(filesTable));
		filesTable.getTableHeader().setFont(filesTable.getFont().deriveFont(Font.BOLD));
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(filesTable.getModel());
		sorter.setComparator(TABLE_INDEX_DOCUMENT_FILE, Comparator.naturalOrder());
		filesTable.setRowSorter(sorter);
		//clicking on a line links it to current document citation
		filesTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = filesTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(filesTable.convertRowIndexToModel(selectedRow));
		});
		filesTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		filesTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Serial
				private static final long serialVersionUID = 2654469836713369932L;

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
		filesTable.setPreferredScrollableViewportSize(new Dimension(filesTable.getPreferredSize().width,
			filesTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> addAction());

		//TODO add the possibility to open file
		fileLabel.setLabelFor(fileField);
		fileField.setEnabled(false);
		fileField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void changedUpdate(final DocumentEvent evt){
				textChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent evt){
				textChanged();
			}

			@Override
			public void insertUpdate(final DocumentEvent evt){
				textChanged();
			}
		});

		fileButton.setEnabled(false);
		fileButton.addActionListener(evt -> {
			final int returnValue = fileChooser.showDialog(this, "Choose");
			if(returnValue == JFileChooser.APPROVE_OPTION){
				final String path = fileChooser.getSelectedFile().getPath();
				fileField.setText(store.stripBasePath(path));

				sphericalCheckBox.setEnabled(true);
			}
		});
		fileChooser.setAccessory(new ImagePreview(fileChooser, 150, 100));

		sphericalCheckBox.setEnabled(false);
		mappingComboBox.setEnabled(false);

		descriptionField.setEnabled(false);
		GUIHelper.bindLabelTextChangeUndo(descriptionLabel, descriptionField, evt -> textChanged());

		extractPreviewView = new TextPreviewPane(this);
		GUIHelper.setEnabled(extractPreviewView, false);

		extractTypeLabel.setLabelFor(extractTypeComboBox);
		extractTypeComboBox.setEnabled(false);
		extractTypeComboBox.addActionListener(evt -> textChanged());

		extractLocaleComboBox.setEnabled(false);
		extractLocaleComboBox.addActionListener(evt -> textChanged());

		final JPanel extractPanel = new JPanel();
		extractPanel.setBorder(BorderFactory.createTitledBorder("Extract"));
		extractPanel.setLayout(new MigLayout("", "[grow]"));
		extractPanel.add(extractPreviewView, "span 2,grow,wrap");
		extractPanel.add(extractTypeLabel, "align label,sizegroup label,split 2");
		extractPanel.add(extractTypeComboBox, "wrap");
		extractPanel.add(extractLocaleLabel, "align label,split 2,sizegroup label");
		extractPanel.add(extractLocaleComboBox);

		notesButton.setEnabled(false);
		notesButton.addActionListener(evt -> {
			final int selectedRow = filesTable.convertRowIndexToModel(filesTable.getSelectedRow());
			final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
			final GedcomNode selectedDocument = documents.get(selectedRow);

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedDocument));
		});

		restrictionCheckBox.setEnabled(false);
		restrictionCheckBox.addActionListener(evt -> textChanged());

		//TODO link to help
//		helpButton.addActionListener(evt -> dispose());
		okButton.setEnabled(false);
		okButton.addActionListener(evt -> {
			okAction();

			if(onCloseGracefully != null)
				onCloseGracefully.accept(this);

			//TODO remember, when saving the whole gedcom, to remove all non-referenced documents!

			dispose();
		});
		getRootPane().registerKeyboardAction(this, ESCAPE_STROKE, JComponent.WHEN_IN_FOCUSED_WINDOW);
		cancelButton.addActionListener(this::actionPerformed);

		setLayout(new MigLayout("", "[grow]"));
		add(filterLabel, "align label,split 2");
		add(filterField, "grow,wrap");
		add(filesScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(fileLabel, "align label,sizegroup label,split 3");
		add(fileField, "grow");
		add(fileButton, "wrap");
		add(sphericalCheckBox, "wrap");
		add(mappingLabel, "align label,sizegroup label,split 2");
		add(mappingComboBox, "wrap paragraph");
		add(descriptionLabel, "align label,sizegroup label,split 2");
		add(descriptionField, "grow,wrap");
		add(extractPanel, "grow,wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(notesButton, "grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	@Override
	public void textChanged(){
		if(!updating)
			okButton.setEnabled(StringUtils.isNotBlank(fileField.getText()) && calculateDataHash() != dataHash);
	}

	private int calculateDataHash(){
		final int fileHash = Objects.requireNonNullElse(fileField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int sphericalHash = Boolean.valueOf(sphericalCheckBox.isSelected())
			.hashCode();
		final int mappingHash = Objects.requireNonNullElse(mappingComboBox.getSelectedItem(), StringUtils.EMPTY)
			.hashCode();
		final int descriptionHash = Objects.requireNonNullElse(descriptionField.getText(), StringUtils.EMPTY)
			.hashCode();
		final int extractHash = extractPreviewView.getText()
			.hashCode();
		final int extractTypeHash = Objects.requireNonNullElse(extractTypeComboBox.getSelectedItem(), StringUtils.EMPTY)
			.hashCode();
		final int extractLanguageTagHash = extractLocaleComboBox.getSelectedLanguageTag()
			.hashCode();
		final int restrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		return fileHash ^ sphericalHash ^ mappingHash ^ descriptionHash ^ extractHash ^ extractTypeHash ^ extractLanguageTagHash
			^ restrictionHash;
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	private void okAction(){
		final String file = fileField.getText();
		final boolean spherical = sphericalCheckBox.isSelected();
		final String mapping = (String)mappingComboBox.getSelectedItem();
		final String description = descriptionField.getText();
		final String extract = extractPreviewView.getText();
		final String extractType = (String)extractTypeComboBox.getSelectedItem();
		final String extractLanguageTag = extractLocaleComboBox.getSelectedLanguageTag();
		final String restriction = (restrictionCheckBox.isSelected()? "confidential": null);

		final int index = filesTable.convertRowIndexToModel(filesTable.getSelectedRow());
		final GedcomNode fileNode = container.getChildrenWithTag("FILE")
			.get(index);
		final GedcomNode extractNode = store.traverse(fileNode, "EXTRACT");
		extractNode.withTag("EXTRACT").withValue(extract)
			.replaceChildValue("TYPE", extractType)
			.replaceChildValue("LOCALE", extractLanguageTag);
		fileNode.withValue(file)
			.replaceChildValue("SPHERICAL", (spherical? "Y": "N"))
			.replaceChildValue("MAPPING", mapping)
			.replaceChildValue("DESCRIPTION", description)
			.removeChildrenWithTag("EXTRACT")
			.addChild(extractNode)
			.replaceChildValue("RESTRICTION", restriction);
	}

	private void selectAction(final int selectedRow){
		final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
		final GedcomNode selectedDocument = documents.get(selectedRow);

		final String file = selectedDocument.getValue();
		final boolean spherical = "Y".equals(store.traverse(selectedDocument, "SPHERICAL").getValue());
		final String mapping = store.traverse(selectedDocument, "MAPPING").getValue();
		final String description = store.traverse(selectedDocument, "DESCRIPTION").getValue();
		final GedcomNode extractNode = store.traverse(selectedDocument, "EXTRACT");
		final String extract = extractNode.getValue();
		final String extractType = store.traverse(extractNode, "TYPE").getValue();
		final String extractLanguageTag = store.traverse(extractNode, "LOCALE").getValue();
		final String restriction = store.traverse(selectedDocument, "RESTRICTION").getValue();

		updating = true;

		fileField.setText(file);
		fileButton.setEnabled(true);
		sphericalCheckBox.setEnabled(true);
		sphericalCheckBox.setSelected(spherical);
		mappingComboBox.setEnabled(true);
		mappingComboBox.setSelectedItem(mapping);
		descriptionField.setEnabled(true);
		descriptionField.setText(description);
		GUIHelper.setEnabled(extractPreviewView, true);
		extractPreviewView.setText("Document " + file, extract, extractLanguageTag);
		extractTypeComboBox.setEnabled(true);
		extractTypeComboBox.setSelectedItem(extractType);
		extractLocaleComboBox.setEnabled(true);
		extractLocaleComboBox.setSelectedByLanguageTag(extractLanguageTag);
		restrictionCheckBox.setEnabled(true);
		restrictionCheckBox.setSelected("confidential".equals(restriction));
		notesButton.setEnabled(true);

		updating = false;

		dataHash = calculateDataHash();
	}

	private void addAction(){
		final GedcomNode newDocument = store.create("FILE");
		container.forceAddChild(newDocument);

		//refresh group list
		loadData();

		final int lastRowIndex = filesTable.getModel().getRowCount() - 1;
		filesTable.setRowSelectionInterval(lastRowIndex, lastRowIndex);
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)filesTable.getModel();
		final int index = filesTable.convertRowIndexToModel(filesTable.getSelectedRow());
		model.removeRow(index);
	}

	public void loadData(final GedcomNode container, final Consumer<Object> onCloseGracefully){
		this.container = container;
		this.onCloseGracefully = onCloseGracefully;

		loadData();

		repaint();
	}

	private void loadData(){
		final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
		final int size = documents.size();

		if(size > 0){
			final DefaultTableModel documentsModel = (DefaultTableModel)filesTable.getModel();
			documentsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode document = documents.get(row);

				documentsModel.setValueAt(document.getValue(), row, TABLE_INDEX_DOCUMENT_FILE);
			}
		}
	}

	private void filterTableBy(final DocumentStructureDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_DOCUMENT_FILE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)filesTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)filesTable.getModel();
			sorter = new TableRowSorter<>(model);
			filesTable.setRowSorter(sorter);
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

	@Override
	public void actionPerformed(final ActionEvent evt){
		dispose();
	}


	private static class DocumentTableModel extends DefaultTableModel{

		@Serial
		private static final long serialVersionUID = 2839717535515895303L;


		DocumentTableModel(){
			super(new String[]{"Title"}, 0);
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
		final GedcomNode container = store.getSources().get(0);

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
					JDialog dialog = null;
					switch(editCommand.getType()){
						case NOTE_CITATION -> {
							dialog = new NoteCitationDialog(store, parent);
							((NoteCitationDialog)dialog).loadData(editCommand.getContainer());
							dialog.setSize(450, 260);
						}
						case NOTE -> {
							dialog = new NoteRecordDialog(store, parent);
							((NoteRecordDialog)dialog).loadData(editCommand.getContainer(), editCommand.getOnCloseGracefully());
							dialog.setSize(550, 350);
						}
					}
					if(dialog != null){
						dialog.setLocationRelativeTo(parent);
						dialog.setVisible(true);
					}
				}
			};
			EventBusService.subscribe(listener);

			final DocumentStructureDialog dialog = new DocumentStructureDialog(store, parent);
			dialog.loadData(container, null);

			dialog.addWindowListener(new java.awt.event.WindowAdapter(){
				@Override
				public void windowClosing(final java.awt.event.WindowEvent e){
					System.exit(0);
				}
			});
			dialog.setSize(450, 650);
			dialog.setLocationRelativeTo(null);
			dialog.setVisible(true);
		});
	}

}
