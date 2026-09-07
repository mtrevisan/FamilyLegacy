package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.List;
import java.util.function.Consumer;


/**
 * Dialog for advanced searching across any record type,
 * with text search modes (fuzzy, whole-word) and event-based filters for individuals.
 */
public class AdvancedSearchDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = 1L;

	private static final int DEBOUNCE_TIME = 300;

	private final FLEFModel model;
	private final Consumer<FLEFRecord> onSelect;

	// Record type selection
	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	// Text search components
	private final JTextField searchField = new JTextField();
	private final JCheckBox fuzzyCheckBox = new JCheckBox("Fuzzy", false);
	private final JCheckBox wholeWordCheckBox = new JCheckBox("Whole word", false);

	// Advanced filters (only for individuals)
	private final JComboBox<String> eventTypeCombo = new JComboBox<>(new String[]{"Any", "birth", "death", "marriage", "baptism", "burial", "residence"});
	private final JTextField dateFromField = new JTextField(10);
	private final JTextField dateToField = new JTextField(10);
	private final JTextField locationField = new JTextField(20);

	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> resultList = new JList<>(listModel);
	private final JLabel statusLabel = new JLabel(" ");

	private List<AdvancedSearchService.SearchResult> results;
	private final AdvancedSearchService searchService = new AdvancedSearchService();
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> performSearch(), DEBOUNCE_TIME);

	private boolean confirmed;

	/**
	 * Creates a new AdvancedSearchDialog.
	 *
	 * @param parent       the parent window
	 * @param model        the FLEF model
	 * @param handlerTypes the record types to allow searching (e.g., IndividualHandler, GroupHandler)
	 * @param onSelect     callback when a result is selected (double-click or 'Select' button)
	 */
	public AdvancedSearchDialog(final Window parent, final FLEFModel model,
		final Class<? extends RecordTypeHandler<?>>[] handlerTypes,
		final Consumer<FLEFRecord> onSelect){
		super(parent, "Advanced Search", ModalityType.APPLICATION_MODAL);
		this.model = model;
		this.onSelect = onSelect;

		// Build type combo
		final RecordTypeHandler<?>[] handlers = new RecordTypeHandler[handlerTypes.length];
		for(int i = 0; i < handlerTypes.length; i++){
			handlers[i] = io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry.getHandler(handlerTypes[i]);
		}
		typeCombo = new JComboBox<>(handlers);
		typeCombo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean isSelected,
				final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value instanceof RecordTypeHandler<?> h){
					setText(h.getLabel());
				}
				return this;
			}
		});

		initComponents();
		pack();
		setMinimumSize(new Dimension(650, 500));
		setLocationRelativeTo(parent);

		// Initial search
		performSearch();
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10, fill", "[grow,fill]", "[][][grow][]"));

		// ----- Top panel: type + text search + modes -----
		final JPanel topPanel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", "[]"));
		topPanel.setBorder(BorderFactory.createTitledBorder("Search"));

		topPanel.add(new JLabel("Record type:"));
		topPanel.add(typeCombo, "growx");

		topPanel.add(new JLabel("Search text:"));
		topPanel.add(searchField, "growx");
		topPanel.add(fuzzyCheckBox, "span 2,left");
		topPanel.add(wholeWordCheckBox, "span 2,left");

		// Disable fuzzy/wholeWord when both are on (mutually exclusive)
		fuzzyCheckBox.addActionListener(e -> {
			if(fuzzyCheckBox.isSelected()) wholeWordCheckBox.setSelected(false);
			scheduleSearch();
		});
		wholeWordCheckBox.addActionListener(e -> {
			if(wholeWordCheckBox.isSelected()) fuzzyCheckBox.setSelected(false);
			scheduleSearch();
		});

		add(topPanel, "growx,wrap");

		// ----- Advanced filters panel (only for Individuals) -----
		final JPanel advPanel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", "[]"));
		advPanel.setBorder(BorderFactory.createTitledBorder("Event Filters (only for Individuals)"));

		advPanel.add(new JLabel("Event type:"));
		advPanel.add(eventTypeCombo, "growx");
		advPanel.add(new JLabel("Date from:"));
		advPanel.add(dateFromField, "growx");
		advPanel.add(new JLabel("Date to:"));
		advPanel.add(dateToField, "growx");
		advPanel.add(new JLabel("Location contains:"));
		advPanel.add(locationField, "growx");

		add(advPanel, "growx,wrap");

		// ----- Results panel -----
		final JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));

		resultList.setFixedCellHeight(24);
		resultList.setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean isSelected,
				final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(results != null && index >= 0 && index < results.size()){
					final RecordTypeHandler<?> handler = (RecordTypeHandler<?>)typeCombo.getSelectedItem();
					final String display = results.get(index).getDisplayText(model, handler);
					setText(display);
				}
				return this;
			}
		});

		final JScrollPane scrollPane = GUIHelper.createScrollPane(resultList);
		resultsPanel.add(scrollPane, BorderLayout.CENTER);

		add(resultsPanel, "grow,push,wrap");

		// ----- Status bar -----
		add(statusLabel, "growx,wrap");

		// ----- Buttons -----
		final JPanel buttonPanel = new JPanel();
		final JButton searchButton = new JButton("Search");
		final JButton selectButton = new JButton("Select");
		final JButton cancelButton = new JButton("Cancel");

		searchButton.addActionListener(e -> performSearch());
		selectButton.addActionListener(e -> selectResult());
		cancelButton.addActionListener(e -> dispose());

		buttonPanel.add(searchButton);
		buttonPanel.add(selectButton);
		buttonPanel.add(cancelButton);
		add(buttonPanel, "growx");

		// ----- Event listeners -----
		typeCombo.addActionListener(e -> {
			updateAdvancedFiltersEnabled();
			performSearch();
		});

		final DocumentListener textListener = new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				scheduleSearch();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				scheduleSearch();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				scheduleSearch();
			}
		};
		searchField.getDocument().addDocumentListener(textListener);
		dateFromField.getDocument().addDocumentListener(textListener);
		dateToField.getDocument().addDocumentListener(textListener);
		locationField.getDocument().addDocumentListener(textListener);
		eventTypeCombo.addActionListener(e -> scheduleSearch());

		// Double-click on result -> select
		resultList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
					selectResult();
				}
			}
		});

		// Initial state
		updateAdvancedFiltersEnabled();
	}

	private void updateAdvancedFiltersEnabled(){
		final RecordTypeHandler<?> handler = (RecordTypeHandler<?>)typeCombo.getSelectedItem();
		final boolean isIndividual = handler != null && handler.getType().equals(IndividualHandler.TYPE);
		eventTypeCombo.setEnabled(isIndividual);
		dateFromField.setEnabled(isIndividual);
		dateToField.setEnabled(isIndividual);
		locationField.setEnabled(isIndividual);
	}

	private void scheduleSearch(){
		searchDebouncer.call("search");
	}

	private void performSearch(){
		final RecordTypeHandler<?> handler = (RecordTypeHandler<?>)typeCombo.getSelectedItem();
		if(handler == null) return;

		final AdvancedSearchCriteria criteria = new AdvancedSearchCriteria(
			handler,
			searchField.getText().trim(),
			fuzzyCheckBox.isSelected(),
			wholeWordCheckBox.isSelected(),
			eventTypeCombo.getSelectedIndex() == 0? null: (String)eventTypeCombo.getSelectedItem(),
			dateFromField.getText().trim(),
			dateToField.getText().trim(),
			locationField.getText().trim()
		);

		statusLabel.setText("Searching...");
		resultList.setEnabled(false);
		listModel.clear();

		final SwingWorker<List<AdvancedSearchService.SearchResult>, Void> worker =
			new SwingWorker<>(){
				@Override
				protected List<AdvancedSearchService.SearchResult> doInBackground(){
					return searchService.search(model, criteria);
				}

				@Override
				protected void done(){
					try{
						results = get();
						updateResults();
						statusLabel.setText("Found " + results.size() + " records");
						resultList.setEnabled(true);
					}
					catch(Exception e){
						statusLabel.setText("Error during search");
						results = null;
						listModel.clear();
						resultList.setEnabled(false);
					}
				}
			};
		worker.execute();
	}

	private void updateResults(){
		listModel.clear();
		if(results == null || results.isEmpty()){
			listModel.addElement("[No matching records]");
			resultList.setEnabled(false);
		}
		else{
			resultList.setEnabled(true);
			final RecordTypeHandler<?> handler = (RecordTypeHandler<?>)typeCombo.getSelectedItem();
			for(final AdvancedSearchService.SearchResult r : results){
				listModel.addElement(r.getDisplayText(model, handler));
			}
		}
	}

	private void selectResult(){
		final int idx = resultList.getSelectedIndex();
		if(idx >= 0 && results != null && idx < results.size()){
			final FLEFRecord record = results.get(idx).record();
			confirmed = true;
			if(onSelect != null){
				onSelect.accept(record);
			}
			dispose();
		}
		else{
			JOptionPane.showMessageDialog(this,
				"Please select a result first.",
				"No Selection", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	public boolean isConfirmed(){
		return confirmed;
	}
}
