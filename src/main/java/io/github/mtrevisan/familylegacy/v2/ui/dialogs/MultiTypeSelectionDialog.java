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
package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;


/**
 * Generic dialog for selecting a record from multiple types (e.g., Individual or Group).
 * If only one type is provided, the type combobox is automatically hidden.
 */
public class MultiTypeSelectionDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6835967045890180368L;


	public static final String PROPERTY_TYPE_SELECTED = "type-selected";


	private final FLEFModel model;
	// non-null if only one type
	private final RecordTypeHandler<?> defaultType;

	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	private final JTextField searchField;
	private final DefaultListModel<String> listModel;
	private final JList<String> list;
	private final JButton selectButton;

	private List<FLEFRecord> allRecords = new ArrayList<>();
	private final List<FLEFRecord> filteredRecords = new ArrayList<>();

	private boolean confirmed;
	private String selectedType;
	private FLEFRecord selectedRecord;


	/**
	 * Creates a MultiTypeSelectionDialog.
	 *
	 * @param parent	The parent dialog.
	 * @param model	The FLEF model.
	 * @param handlerType	The supported participant type.
	 */
	public MultiTypeSelectionDialog(final Dialog parent, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		this(parent, model, List.of(handlerType));
	}

	/**
	 * Creates a MultiTypeSelectionDialog.
	 *
	 * @param parent	The parent dialog.
	 * @param model	The FLEF model.
	 * @param handlerTypes	The list of supported participant types.
	 */
	public MultiTypeSelectionDialog(final Dialog parent, final FLEFModel model,
			final List<Class<? extends RecordTypeHandler<?>>> handlerTypes){
		super(parent, "Select Participant", ModalityType.APPLICATION_MODAL);

		this.model = model;
		final RecordTypeHandler<?>[] handlers = handlerTypes.stream()
			.map(HandlerRegistry::getHandler)
			.toArray(value -> new RecordTypeHandler<?>[handlerTypes.size()]);
		defaultType = (handlerTypes.size() == 1? handlers[0]: null);

		// UI components (typeCombo may remain null if only one type)
		typeCombo = (handlerTypes.size() > 1? createTypeCombo(handlers): null);
		searchField = new JTextField(null);
		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		selectButton = new JButton("Select");


		initComponents();

		loadRecordsForType(getSelectedDescriptor());

		pack();

		setLocationRelativeTo(parent);
	}

	private JComboBox<RecordTypeHandler<?>> createTypeCombo(final RecordTypeHandler<?>[] handlers){
		final JComboBox<RecordTypeHandler<?>> combo = new JComboBox<>(handlers);
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if(value instanceof RecordTypeHandler<?> desc)
					setText(desc.getLabel());

				return this;
			}
		});
		combo.addActionListener(e -> {
			loadRecordsForType(getSelectedDescriptor());

			searchField.setText(StringUtils.EMPTY);
		});
		return combo;
	}

	private void initComponents(){
		setLayout(new BorderLayout(10, 10));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// Top panel: type combo (if more than one) + search field
		final JPanel topPanel = GUIHelper.createLabelFieldPanel(0, (typeCombo != null? "[]10[]": "[]"));
		if(typeCombo != null)
			GUIHelper.addLabeledComponent(topPanel, "Type:", typeCombo);
		GUIHelper.addLabeledComponent(topPanel, "Search:", searchField);

		// List panel
		final JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Records"));
		add(topPanel, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		// Buttons
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton cancelButton = new JButton("Cancel");
		final JButton createButton = new JButton("Create New…");
		buttonPanel.add(createButton);
		buttonPanel.add(selectButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, BorderLayout.SOUTH);

		if(typeCombo != null)
			typeCombo.addActionListener(e -> updateWindowTitle());
		updateWindowTitle();

		// Search listener
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				filterRecords();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				filterRecords();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				filterRecords();
			}
		});

		// List interactions
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					selectAndClose();
			}
		});
		selectButton.addActionListener(e -> selectAndClose());

		// Cancel / close handlers
		cancelButton.addActionListener(e -> dispose());

		// Create new record
		createButton.addActionListener(e -> createNewRecord());
	}

	private void updateWindowTitle(){
		final RecordTypeHandler<?> desc = getSelectedDescriptor();
		setTitle("Select " + (desc != null? desc.getLabel(): "Record"));
	}

	private RecordTypeHandler<?> getSelectedDescriptor(){
		if(defaultType != null)
			return defaultType;

		return (typeCombo != null? (RecordTypeHandler<?>)typeCombo.getSelectedItem(): null);
	}

	private void loadRecordsForType(final RecordTypeHandler<?> desc){
		if(desc == null)
			return;

		updateWindowTitle();

		allRecords = model.getRecordsByType(desc.getType());

		filterRecords();
	}

	private void filterRecords(){
		filteredRecords.clear();

		final String text = searchField.getText()
			.trim()
			.toLowerCase();
		final RecordTypeHandler<?> desc = getSelectedDescriptor();
		if(desc == null)
			return;
		for(final FLEFRecord record : allRecords){
			final String display = desc.getDisplayText(record, model);
			if(display.toLowerCase().contains(text))
				filteredRecords.add(record);
		}
		filteredRecords.sort((a, b) -> {
			final String nameA = desc.getDisplayText(a, model);
			final String nameB = desc.getDisplayText(b, model);
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
				final RecordTypeHandler<?> desc = getSelectedDescriptor();
				final String displayText = desc.getDisplayText(record, model);
				listModel.addElement(displayText);
			}
		}
	}

	private void createNewRecord(){
		final RecordTypeHandler<?> desc = getSelectedDescriptor();
		if(desc == null)
			return;

		final BaseRecordDialog createDialog = desc.createNewDialog(this, model);
		createDialog.setVisible(true);

		FLEFRecord newRecord = null;
		if(createDialog.isSaved())
			newRecord = createDialog.getRecord();

		if(newRecord != null){
			// Reload the list for the current type and select the new record
			loadRecordsForType(desc);

			final int idx = filteredRecords.indexOf(newRecord);
			if(idx >= 0){
				list.setSelectedIndex(idx);
				list.ensureIndexIsVisible(idx);
			}

			selectedType = desc.getType();
			selectedRecord = newRecord;
			confirmed = true;

			firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

			dispose();
		}
	}

	private void selectAndClose(){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < filteredRecords.size()){
			final RecordTypeHandler<?> desc = getSelectedDescriptor();
			if(desc != null){
				selectedType = desc.getType();
				selectedRecord = filteredRecords.get(idx);
				confirmed = true;

				firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

				dispose();
			}
		}
		else
			JOptionPane.showMessageDialog(this,
				"Please select a record first.",
				"No Selection", JOptionPane.INFORMATION_MESSAGE);
	}

	public boolean isConfirmed(){
		return confirmed;
	}

	public String getSelectedType(){
		return selectedType;
	}

	public FLEFRecord getSelectedRecord(){
		return selectedRecord;
	}

}
