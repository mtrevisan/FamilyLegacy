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
import io.github.mtrevisan.familylegacy.ui.dialogs.NoteDialog;
import io.github.mtrevisan.familylegacy.ui.utilities.Debouncer;
import io.github.mtrevisan.familylegacy.ui.utilities.LocaleFilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.utilities.TableTransferHandle;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewListenerInterface;
import io.github.mtrevisan.familylegacy.ui.utilities.TextPreviewPane;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventBusService;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.EventHandler;
import io.github.mtrevisan.familylegacy.ui.utilities.eventbus.events.BusExceptionEvent;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


public class DocumentCitationDialog extends JDialog implements ActionListener, TextPreviewListenerInterface{

	private static final long serialVersionUID = 1919219115430275506L;

	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

	/** [ms] */
	private static final int DEBOUNCER_TIME = 400;

	private static final Color GRID_COLOR = new Color(230, 230, 230);

	private static final int TABLE_INDEX_DOCUMENT_FILE = 0;

	private static final DefaultComboBoxModel<String> EXTRACT_TYPE_MODEL = new DefaultComboBoxModel<>(new String[]{StringUtils.EMPTY,
		"transcript", "extract", "abstract"});

	private final JLabel filterLabel = new JLabel("Filter:");
	private final JTextField filterField = new JTextField();
	private final JTable documentsTable = new JTable(new DocumentTableModel());
	private final JScrollPane documentsScrollPane = new JScrollPane(documentsTable);
	private final JButton addButton = new JButton("Add");
	private final JLabel fileLabel = new JLabel("File:");
	private final JTextField fileField = new JTextField();
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

	private final Debouncer<DocumentCitationDialog> filterDebouncer = new Debouncer<>(this::filterTableBy, DEBOUNCER_TIME);

	private GedcomNode container;
	private int extractHash;

	private Consumer<Object> onCloseGracefully;
	private final Flef store;


	public DocumentCitationDialog(final Flef store, final Frame parent){
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
				filterDebouncer.call(DocumentCitationDialog.this);
			}
		});

		documentsTable.setAutoCreateRowSorter(true);
		documentsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		documentsTable.setGridColor(GRID_COLOR);
		documentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		documentsTable.setDragEnabled(true);
		documentsTable.setDropMode(DropMode.INSERT_ROWS);
		documentsTable.setTransferHandler(new TableTransferHandle(documentsTable));
		documentsTable.getTableHeader().setFont(documentsTable.getFont().deriveFont(Font.BOLD));
		final TableRowSorter<TableModel> sorter = new TableRowSorter<>(documentsTable.getModel());
		sorter.setComparator(TABLE_INDEX_DOCUMENT_FILE, Comparator.naturalOrder());
		documentsTable.setRowSorter(sorter);
		//clicking on a line links it to current document citation
		documentsTable.getSelectionModel().addListSelectionListener(evt -> {
			final int selectedRow = documentsTable.getSelectedRow();
			if(!evt.getValueIsAdjusting() && selectedRow >= 0)
				selectAction(documentsTable.convertRowIndexToModel(selectedRow));
		});
		documentsTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent evt){
				if(evt.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(evt) && documentsTable.rowAtPoint(evt.getPoint()) >= 0)
					editAction();
			}
		});
		documentsTable.getInputMap(JComponent.WHEN_FOCUSED)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		documentsTable.getActionMap()
			.put("delete", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent evt){
					deleteAction();
				}
			});
		documentsTable.setPreferredScrollableViewportSize(new Dimension(documentsTable.getPreferredSize().width,
			documentsTable.getRowHeight() * 5));

		addButton.addActionListener(evt -> addAction());

		fileLabel.setLabelFor(fileField);
		fileField.setEnabled(false);

		descriptionLabel.setLabelFor(descriptionField);
		descriptionField.setEnabled(false);

		extractPreviewView = new TextPreviewPane(this);

		extractTypeLabel.setLabelFor(extractTypeComboBox);
		extractTypeComboBox.setEnabled(false);

		notesButton.setEnabled(false);
		notesButton.addActionListener(evt -> {
			final int selectedRow = documentsTable.convertRowIndexToModel(documentsTable.getSelectedRow());
			final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
			final GedcomNode selectedDocument = documents.get(selectedRow);

			EventBusService.publish(new EditEvent(EditEvent.EditType.NOTE_CITATION, selectedDocument));
		});

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
		add(documentsScrollPane, "grow,wrap related");
		add(addButton, "tag add,split 3,sizegroup button,wrap paragraph");
		add(fileLabel, "align label,sizegroup label,split 2");
		add(fileField, "grow,wrap");
		add(descriptionLabel, "align label,sizegroup label,split 2");
		add(descriptionField, "grow,wrap");
		add(extractPreviewView, "span 2,grow,wrap");
		add(extractTypeLabel, "align label,sizegroup label,split 2");
		add(extractTypeComboBox, "wrap");
		add(extractLocaleLabel, "align label,split 2,sizegroup label");
		add(extractLocaleComboBox, "wrap");
		add(restrictionCheckBox, "wrap paragraph");
		add(notesButton, "sizegroup button,grow,wrap paragraph");
		add(helpButton, "tag help2,split 3,sizegroup button");
		add(okButton, "tag ok,sizegroup button");
		add(cancelButton, "tag cancel,sizegroup button");
	}

	@Override
	public void textChanged(){
		final int newTextHash = extractPreviewView.getText()
			.hashCode();
		final int newLanguageTagHash = extractLocaleComboBox.getSelectedLanguageTag()
			.hashCode();
		final int newRestrictionHash = (restrictionCheckBox.isSelected()? "confidential": StringUtils.EMPTY)
			.hashCode();
		final int newExtractHash = newTextHash ^ newLanguageTagHash ^ newRestrictionHash;

		okButton.setEnabled(newExtractHash != extractHash);
	}

	@Override
	public void onPreviewStateChange(final boolean visible){
		TextPreviewListenerInterface.centerDivider(this, visible);
	}

	private void okAction(){
		//TODO
//		final String file = (String)okButton.getClientProperty(KEY_DOCUMENT_FILE);
	}

	private void selectAction(final int selectedRow){
		final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
		final GedcomNode selectedDocument = documents.get(selectedRow);

		final String file = selectedDocument.getValue();
		final String description = store.traverse(selectedDocument, "DESCRIPTION").getValue();
		final GedcomNode extractNode = store.traverse(selectedDocument, "EXTRACT");
		final String extract = extractNode.getValue();
		final String extractType = store.traverse(extractNode, "TYPE").getValue();
		final String extractLanguageTag = store.traverse(extractNode, "LOCALE").getValue();
		final String restriction = store.traverse(selectedDocument, "RESTRICTION").getValue();

		final int textHash = Objects.requireNonNullElse(extract, StringUtils.EMPTY).hashCode();
		final int languageTagHash = Objects.requireNonNullElse(extractLanguageTag, StringUtils.EMPTY).hashCode();
		final int restrictionHash = Objects.requireNonNullElse(restriction, StringUtils.EMPTY).hashCode();
		extractHash = textHash ^ languageTagHash ^ restrictionHash;

		fileField.setText(file);
		descriptionField.setEnabled(true);
		descriptionField.setText(description);
		extractPreviewView.setText("Document " + file, extract, extractLanguageTag);
		extractTypeComboBox.setEnabled(true);
		extractTypeComboBox.setSelectedItem(extractType);
		extractLocaleComboBox.setEnabled(true);
		if(extractLanguageTag != null)
			extractLocaleComboBox.setSelectedByLanguageTag(extractLanguageTag);
		restrictionCheckBox.setSelected("confidential".equals(restriction));
		notesButton.setEnabled(true);

		okButton.setEnabled(true);
	}

	private void addAction(){
		//TODO
		final GedcomNode newDocument = store.create("FILE");

		final Consumer<Object> onCloseGracefully = dialog -> {
			//if ok was pressed, add this document to the parent container

			final String file = fileField.getText();
			final String description = descriptionField.getText();
			final String extract = extractPreviewView.getText();
			final String extractType = (String)extractTypeComboBox.getSelectedItem();
			final String extractLanguageTag = extractLocaleComboBox.getSelectedLanguageTag();
			final String restriction = (restrictionCheckBox.isSelected()? "confidential": null);
			final GedcomNode extractNode = store.create("EXTRACT")
				.withValue(extract)
				.addChildValue("TYPE", extractType)
				.addChildValue("LOCALE", extractLanguageTag);
			newDocument
				.withValue(file)
				.addChildValue("DESCRIPTION", description)
				.addChild(extractNode)
				.addChildValue("RESTRICTION", restriction);

			container.addChild(newDocument);

			//refresh group list
			loadData();
		};

		//fire edit event
		EventBusService.publish(new EditEvent(EditEvent.EditType.DOCUMENT, newDocument, onCloseGracefully));
	}

	private void editAction(){
		//retrieve selected document
		final List<GedcomNode> documents = store.traverseAsList(container, "FILE[]");
		final int index = documentsTable.convertRowIndexToModel(documentsTable.getSelectedRow());
		final GedcomNode selectedDocument = documents.get(index);

//		final String file = selectedDocument.getValue();
//		final String description = store.traverse(selectedDocument, "DESCRIPTION").getValue();
//		final GedcomNode extractNode = store.traverse(selectedDocument, "EXTRACT");
//		final String extract = extractNode.getValue();
//		final String extractType = store.traverse(extractNode, "TYPE").getValue();
//		final String extractLocale = store.traverse(extractNode, "LOCALE").getValue();
//		final String restriction = store.traverse(selectedDocument, "RESTRICTION").getValue();

		//TODO

		//fire edit event
//		EventBusService.publish(new EditEvent(EditEvent.EditType.DOCUMENT, selectedDocument));
	}

	private void deleteAction(){
		final DefaultTableModel model = (DefaultTableModel)documentsTable.getModel();
		final int index = documentsTable.convertRowIndexToModel(documentsTable.getSelectedRow());
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
			final DefaultTableModel documentsModel = (DefaultTableModel)documentsTable.getModel();
			documentsModel.setRowCount(size);

			for(int row = 0; row < size; row ++){
				final GedcomNode document = documents.get(row);

				documentsModel.setValueAt(document.getValue(), row, TABLE_INDEX_DOCUMENT_FILE);
			}
		}
	}

	private void filterTableBy(final DocumentCitationDialog panel){
		final String text = filterField.getText();
		final RowFilter<DefaultTableModel, Object> filter = createTextFilter(text, TABLE_INDEX_DOCUMENT_FILE);

		@SuppressWarnings("unchecked")
		TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>)documentsTable.getRowSorter();
		if(sorter == null){
			final DefaultTableModel model = (DefaultTableModel)documentsTable.getModel();
			sorter = new TableRowSorter<>(model);
			documentsTable.setRowSorter(sorter);
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
	}


	public static void main(final String[] args) throws GedcomParseException, GedcomGrammarParseException{
		try{
			final String lookAndFeelName = UIManager.getSystemLookAndFeelClassName();
			UIManager.setLookAndFeel(lookAndFeelName);
		}
		catch(final Exception ignored){}

		final Flef store = new Flef();
		store.load("/gedg/flef_0.0.6.gedg", "src/main/resources/ged/small.flef.ged")
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
				public void refresh(final EditEvent editCommand) throws IOException{
					JDialog dialog = null;
					switch(editCommand.getType()){
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

			final DocumentCitationDialog dialog = new DocumentCitationDialog(store, parent);
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
