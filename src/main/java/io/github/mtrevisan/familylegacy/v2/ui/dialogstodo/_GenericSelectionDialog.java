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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * Generic dialog for selecting a record of a specific type from a list with search filter.
 * Double-click on an item selects it and closes the dialog.
 *
 * @param <T> the specific dialog type (used for edit dialogs)
 */
public class _GenericSelectionDialog<T extends BaseRecordDialog> extends JDialog{

	@Serial
	private static final long serialVersionUID = 5675310282486257639L;


	private final FLEFModel model;
	private final RecordTypeHandler<T> handler;
	private final Consumer<FLEFRecord> onSelection;

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> list = new JList<>(listModel);
	private final JTextField searchField = new JTextField(null);
	private final JButton searchButton = new JButton("Search");
	private final JButton clearButton = new JButton("Clear");
	private final JButton selectButton = new JButton("Select");
	private final JButton cancelButton = new JButton("Cancel");

	private List<FLEFRecord> allRecords = new ArrayList<>();
	private final List<FLEFRecord> filteredRecords = new ArrayList<>();


	/**
	 * Creates a selection dialog for the given record type.
	 *
	 * @param parent      the parent frame
	 * @param model       the FLEF model
	 * @param handler     the handler for the record type
	 * @param onSelection callback invoked with the selected record ID, or null if canceled
	 */
	public _GenericSelectionDialog(final Dialog parent, final FLEFModel model, final RecordTypeHandler<T> handler,
			final Consumer<FLEFRecord> onSelection){
		this(parent, model, handler, onSelection, null);
	}

	public _GenericSelectionDialog(final Dialog parent, final FLEFModel model, final RecordTypeHandler<T> handler,
			final Consumer<FLEFRecord> onSelection, final Supplier<List<FLEFRecord>> loadRecords){
		super(parent, "Select " + handler.getLabel(), ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.handler = handler;
		this.onSelection = onSelection;


		initComponents();

		this.loadRecords(loadRecords);

		filterRecords(StringUtils.EMPTY);

		pack();

		setLocationRelativeTo(parent);
	}


	private void initComponents(){
		setLayout(new BorderLayout(10, 10));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// Search panel
		final JPanel searchPanel = new JPanel(new MigLayout(StringUtils.EMPTY, "[grow][][][]"));
		searchPanel.add(new JLabel("Search:"), "align label");
		searchPanel.add(searchField, "growx");
		searchPanel.add(searchButton);
		searchPanel.add(clearButton);
		add(searchPanel, BorderLayout.NORTH);

		// List panel
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		scrollPane.setBorder(BorderFactory.createTitledBorder(handler.getLabel() + " List"));
		add(scrollPane, BorderLayout.CENTER);

		// Button panel
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.add(selectButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		// Event listeners
		searchButton.addActionListener(e -> filterRecords(searchField.getText().trim()));
		clearButton.addActionListener(e -> {
			searchField.setText(StringUtils.EMPTY);
			filterRecords(StringUtils.EMPTY);
		});
		cancelButton.addActionListener(e -> {
			onSelection.accept(null);

			dispose();
		});
		selectButton.addActionListener(e -> selectAndClose());

		// Double-click on list selects and closes
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2)
					selectAndClose();
			}
		});

		// Real-time search as user types (with debounce)
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				filterRecords(searchField.getText().trim());
			}
		});
	}


	private void loadRecords(final Supplier<List<FLEFRecord>> loadRecords){
		if(loadRecords == null)
			allRecords = model.getRecordsByType(handler.getType());
		else
			allRecords = loadRecords.get();
	}


	private void filterRecords(final String searchText){
		filteredRecords.clear();

		final String lowerSearch = searchText.toLowerCase();
		for(final FLEFRecord record : allRecords){
			final String display = handler.getDisplayText(record, model);
			if(display.toLowerCase().contains(lowerSearch))
				filteredRecords.add(record);
		}

		// Order by display name
		filteredRecords.sort((a, b) -> {
			final String nameA = handler.getDisplayText(a, model);
			final String nameB = handler.getDisplayText(b, model);
			return nameA.compareToIgnoreCase(nameB);
		});

		updateList();
	}


	private void updateList(){
		listModel.clear();

		if(filteredRecords.isEmpty()){
			listModel.addElement("[No matching records]");
			list.setEnabled(false);
			selectButton.setEnabled(false);
		}
		else{
			list.setEnabled(true);
			selectButton.setEnabled(true);
			for(final FLEFRecord record : filteredRecords){
				final String displayText = handler.getDisplayText(record, model);
				listModel.addElement(displayText);
			}
		}
	}


	private void selectAndClose(){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < filteredRecords.size()){
			final FLEFRecord selectedItem = filteredRecords.get(idx);
			onSelection.accept(selectedItem);

			dispose();
		}
		else
			// If nothing selected, show a message
			JOptionPane.showMessageDialog(this, "Please select a record first.",
				"No Selection", JOptionPane.INFORMATION_MESSAGE);
	}

}
