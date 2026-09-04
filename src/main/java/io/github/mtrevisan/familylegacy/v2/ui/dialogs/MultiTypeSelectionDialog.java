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
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;


/**
 * Generic dialog for selecting a record from multiple types (e.g., Individual or Group).
 * If only one type is provided, the type combobox is automatically hidden.
 */
public class MultiTypeSelectionDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6835967045890180368L;


	/** [ms] */
	private static final int DEBOUNCE_TIME = 400;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";
	private static final String PROPERTY_DEBOUNCER = "search";


	private final FLEFModel model;
	// non-null if only one type
	private final RecordTypeHandler<?> defaultType;

	private Consumer<BaseRecordDialog> setupDialog;

	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	private final JTextField searchField;
	private final DefaultListModel<String> listModel;
	private final JList<String> list;

	private List<FLEFRecord> allRecords = new ArrayList<>();
	private final List<FLEFRecord> filteredRecords = new ArrayList<>();
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> filterRecords(), DEBOUNCE_TIME);

	private boolean confirmed;
	private String selectedType;
	private FLEFRecord selectedRecord;


	/**
	 * Creates a MultiTypeSelectionDialog.
	 *
	 * @param parent	The parent dialog.
	 * @param model	The FLEF model.
	 * @param handlerTypes	The list of supported participant types.
	 */
	@SafeVarargs
	public MultiTypeSelectionDialog(final Dialog parent, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		super(parent, "Select Participant", ModalityType.APPLICATION_MODAL);

		this.model = model;
		defaultType = (handlerTypes.length == 1? HandlerRegistry.getHandler(handlerTypes[0]): null);


		// UI components (typeCombo may remain null if only one type)
		typeCombo = (handlerTypes.length > 1? createTypeCombo(handlerTypes): null);
		searchField = new JTextField(null);
		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);


		initComponents();

		loadRecordsForType(getSelectedHandler());

		pack();

		setLocationRelativeTo(parent);
	}

	public MultiTypeSelectionDialog withSetupDialog(final Consumer<BaseRecordDialog> setupDialog){
		this.setupDialog = setupDialog;

		return this;
	}

	private JComboBox<RecordTypeHandler<?>> createTypeCombo(final Class<? extends RecordTypeHandler<?>>[] handlerTypes){
		final RecordTypeHandler<?>[] handlers = Arrays.stream(handlerTypes)
			.map(HandlerRegistry::getHandler)
			.toArray(RecordTypeHandler[]::new);
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
			loadRecordsForType(getSelectedHandler());

			searchField.setText(StringUtils.EMPTY);
		});
		return combo;
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10,fill", "[grow,fill]", "[][grow][]"));

		// Top panel: type combo (if more than one) + search field
		final JPanel topPanel = GUIHelper.createLabelFieldPanel(0, (typeCombo != null? "[]10[]": "[]"));
		if(typeCombo != null)
			GUIHelper.addLabeledComponent(topPanel, "Type:", typeCombo);
		GUIHelper.addLabeledComponent(topPanel, "Search:", searchField);
		add(topPanel, "growx,wrap");


		// Center panel
		final JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Records"));
		add(scrollPane, "grow,push,wrap");


		// Bottom panel
		final JPanel bottomPanel = GUIHelper.createNewSelectCancelButtonPanel(getRootPane(),
			this::createNewRecord,
			this::selectAndClose,
			this::dispose);
		add(bottomPanel, "growx");


		if(typeCombo != null)
			typeCombo.addActionListener(e -> updateWindowTitle());
		updateWindowTitle();

		// Search listener
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				searchDebouncer.call(PROPERTY_DEBOUNCER);
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				searchDebouncer.call(PROPERTY_DEBOUNCER);
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				searchDebouncer.call(PROPERTY_DEBOUNCER);
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
	}

	private void updateWindowTitle(){
		final RecordTypeHandler<?> desc = getSelectedHandler();
		setTitle("Select " + (desc != null? desc.getLabel(): "Record"));
	}

	private RecordTypeHandler<?> getSelectedHandler(){
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
		final RecordTypeHandler<?> desc = getSelectedHandler();
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
//			selectButton.setEnabled(false);
		}
		else{
			list.setEnabled(true);
//			selectButton.setEnabled(true);
			for(final FLEFRecord record : filteredRecords){
				final RecordTypeHandler<?> desc = getSelectedHandler();
				final String displayText = desc.getDisplayText(record, model);
				listModel.addElement(displayText);
			}
		}
	}

	private void createNewRecord(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
		final BaseRecordDialog dialog = handler.createNewDialog(this, model);
		if(setupDialog != null)
			setupDialog.accept(dialog);
		dialog.setVisible(true);

		FLEFRecord newRecord = null;
		if(dialog.isSaved())
			newRecord = dialog.getRecord();

		if(newRecord != null){
			// Reload the list for the current type and select the new record
			loadRecordsForType(handler);

			final int idx = filteredRecords.indexOf(newRecord);
			if(idx >= 0){
				list.setSelectedIndex(idx);
				list.ensureIndexIsVisible(idx);
			}

			selectedType = handler.getType();
			selectedRecord = newRecord;
			confirmed = true;

			firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

			dispose();
		}
	}

	private void selectAndClose(){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < filteredRecords.size()){
			final RecordTypeHandler<?> desc = getSelectedHandler();
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


	public static void main(final String[] args){
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final FLEFModel model = new FLEFModel();
		final List<Class<? extends RecordTypeHandler<?>>> handlerTypes = List.of(
			IndividualHandler.class, GroupHandler.class);

		SwingUtilities.invokeLater(() -> {
			@SuppressWarnings("unchecked")
			final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(null, model,
				handlerTypes.toArray(Class[]::new));
			dialog.setVisible(true);
		});
	}

}
