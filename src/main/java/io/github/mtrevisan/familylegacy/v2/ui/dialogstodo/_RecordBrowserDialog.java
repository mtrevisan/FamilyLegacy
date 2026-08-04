/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.v2.ui.dialogstodo;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


/**
 * Generic dialog for browsing and managing a record of a specific type.
 * It provides a searchable list of records, with the ability to add, edit,
 * and delete records via a right-click context menu.
 * <p>
 * Double-click on an item selects it and closes the dialog.
 * Right-click shows a popup menu with Add, Edit, Delete, and Select.
 * Keyboard shortcuts: Ins = Add, Delete = Delete, Enter = Select, Esc = Cancel.
 *
 * @param <T> the specific dialog type (used for edit dialogs)
 */
public class _RecordBrowserDialog<T extends JDialog> extends JDialog{

	@Serial
	private static final long serialVersionUID = -1546248199171435881L;


	private final FLEFModel model;
	private final RecordTypeHandler<T> handler;
	private final Consumer<String> onSelection;

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> list = new JList<>(listModel);
	private final JTextField searchField = new JTextField(15);
	private final JButton searchButton = new JButton("Search");
	private final JButton clearButton = new JButton("Clear");

	private List<FLEFRecord> allRecords = new ArrayList<>();
	private final List<FLEFRecord> filteredRecords = new ArrayList<>();


	/**
	 * Creates a new RecordBrowserDialog for the given record type.
	 *
	 * @param parent      the parent frame
	 * @param model       the FLEF model
	 * @param handler     the handler for the record type
	 * @param onSelection callback invoked with the selected record ID, or null if cancelled
	 */
	public _RecordBrowserDialog(Frame parent, FLEFModel model, RecordTypeHandler<T> handler,
			Consumer<String> onSelection){
		super(parent, "Browse " + handler.getLabel(), true);

		this.model = model;
		this.handler = handler;
		this.onSelection = onSelection;

		initComponents();
		loadAllRecords();
		filterRecords(StringUtils.EMPTY);
		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// ---- Search panel ----
		JPanel searchPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow][][][]"));
		searchPanel.add(new JLabel("Search:"), "align label");
		searchPanel.add(searchField, "growx");
		searchPanel.add(searchButton);
		searchPanel.add(clearButton);
		add(searchPanel, BorderLayout.NORTH);

		// ---- List panel ----
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		scrollPane.setBorder(BorderFactory.createTitledBorder(handler.getLabel() + " List"));
		add(scrollPane, BorderLayout.CENTER);

		// ---- Context menu for the list (right-click) ----
		JPopupMenu popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add...");
		JMenuItem editItem = new JMenuItem("Edit");
		JMenuItem deleteItem = new JMenuItem("Delete");
		popup.add(addItem);
		popup.addSeparator();
		popup.add(editItem);
		popup.add(deleteItem);

		// ---- Mouse listener for context menu ----
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					boolean hasSelection = list.getSelectedIndex() != -1;
					editItem.setEnabled(hasSelection);
					deleteItem.setEnabled(hasSelection);
					popup.show(list, e.getX(), e.getY());
				}
			}

			@Override
			public void mouseReleased(MouseEvent e){
				if(e.isPopupTrigger()){
					int index = list.locationToIndex(e.getPoint());
					if(index != -1 && !list.isSelectedIndex(index)){
						list.setSelectedIndex(index);
					}
					boolean hasSelection = list.getSelectedIndex() != -1;
					editItem.setEnabled(hasSelection);
					deleteItem.setEnabled(hasSelection);
					popup.show(list, e.getX(), e.getY());
				}
			}
		});

		// ---- Context menu actions ----
		addItem.addActionListener(e -> createNewRecordAndSelect());
		editItem.addActionListener(e -> editSelectedRecord());
		deleteItem.addActionListener(e -> deleteSelectedRecord());

		// ---- Keyboard shortcuts ----
		list.addKeyListener(new KeyAdapter(){
			@Override
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_INSERT){
					createNewRecordAndSelect();
					e.consume();
				}
				else if(e.getKeyCode() == KeyEvent.VK_DELETE){
					deleteSelectedRecord();
					e.consume();
				}
			}
		});

		// ---- Enable/disable context menu items based on selection ----
		list.addListSelectionListener(e -> {
			boolean hasSelection = list.getSelectedIndex() != -1;
			editItem.setEnabled(hasSelection);
			deleteItem.setEnabled(hasSelection);
		});
		editItem.setEnabled(false);
		deleteItem.setEnabled(false);

		// ---- Search button and Clear button ----
		searchButton.addActionListener(e -> filterRecords(searchField.getText().trim()));
		clearButton.addActionListener(e -> {
			searchField.setText(StringUtils.EMPTY);
			filterRecords(StringUtils.EMPTY);
		});

		// ---- Real-time search as user types ----
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}

			@Override
			public void removeUpdate(DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}

			@Override
			public void changedUpdate(DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}
		});

		// ---- Escape key to close without selection ----
		getRootPane().registerKeyboardAction(
			e -> {
				onSelection.accept(null);
				dispose();
			},
			KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW
		);
	}


	/**
	 * Loads all records of the handled type from the model.
	 */
	private void loadAllRecords(){
		allRecords = model.getRecordsByType(handler.getType());
		if(allRecords == null){
			allRecords = new ArrayList<>();
		}
	}


	/**
	 * Filters the records based on the search text.
	 * The filter is case‑insensitive and applied to the display name of each record.
	 *
	 * @param searchText the text to search for (case‑insensitive)
	 */
	private void filterRecords(String searchText){
		filteredRecords.clear();
		String lowerSearch = searchText.toLowerCase();
		for(FLEFRecord record : allRecords){
			String display = handler.getDisplayText(record, model);
			if(display.toLowerCase().contains(lowerSearch)){
				filteredRecords.add(record);
			}
		}
		updateList();
	}


	/**
	 * Updates the list model with the currently filtered records.
	 */
	private void updateList(){
		listModel.clear();
		for(FLEFRecord record : filteredRecords){
			listModel.addElement(handler.getDisplayText(record, model));
		}
		if(filteredRecords.isEmpty()){
			listModel.addElement("[No matching records]");
		}
	}


	/**
	 * Creates a new record, automatically selects it, and closes the dialog.
	 * If the user cancels the creation, the dialog remains open.
	 */
	private void createNewRecordAndSelect(){
		// Remember existing IDs to detect the newly created one
		Set<String> before = new HashSet<>();
		for(FLEFRecord rec : allRecords){
			String id = rec.getId();
			if(id != null){
				before.add(id);
			}
		}

		// Open the creation dialog
		JDialog newDialog = handler.createNewDialog(this, model);
		newDialog.setVisible(true);

		// Reload all records and reapply the search filter
		loadAllRecords();
		filterRecords(searchField.getText().trim());

		// Find the newly created record
		String newId = null;
		for(FLEFRecord rec : allRecords){
			String id = rec.getId();
			if(id != null && !before.contains(id)){
				newId = id;
				break;
			}
		}

		if(newId != null){
			// Select the new record and close the dialog
			onSelection.accept(newId);
			dispose();
		}
	}


	/**
	 * Opens the edit dialog for the selected record.
	 * After editing, the list is refreshed.
	 */
	private void editSelectedRecord(){
		int idx = list.getSelectedIndex();
		if(idx < 0 || idx >= filteredRecords.size()){
			return;
		}
		FLEFRecord selected = filteredRecords.get(idx);
		JDialog editDialog = handler.createEditDialog(this, model, selected);
		editDialog.setVisible(true);

		// Refresh the list after editing
		loadAllRecords();
		filterRecords(searchField.getText().trim());
	}


	/**
	 * Deletes the selected record after user confirmation.
	 * The list is refreshed after deletion.
	 */
	private void deleteSelectedRecord(){
		int idx = list.getSelectedIndex();
		if(idx < 0 || idx >= filteredRecords.size()){
			return;
		}
		FLEFRecord selected = filteredRecords.get(idx);
		int confirm = JOptionPane.showConfirmDialog(this,
			"Delete the selected " + handler.getLabel() + "?",
			"Confirm Delete",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION){
			return;
		}

		String id = selected.getId();
		if(id != null){
			model.removeRecord(id);
			// Refresh the list after deletion
			loadAllRecords();
			filterRecords(searchField.getText().trim());
		}
	}

}
