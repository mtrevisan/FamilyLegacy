package io.github.mtrevisan.familylegacy.v2.ui.components.searches;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.BiologicalTreePanel;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Unified dialog for selecting or creating a record across multiple record types.
 * Supports dynamic filtering strategies, text search modes (fuzzy, whole-word),
 * and optional creation of new records.
 */
public class RecordSelectionDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(RecordSelectionDialog.class);


	@Serial
	private static final long serialVersionUID = -9150455030962766194L;


	private static final String NO_MATCHING_RECORDS = "[No matching records]";

	private static final int DEBOUNCE_TIME = 400;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";

	private static final Dimension SCROLL_PANE_PREFERRED_SIZE = new Dimension(450, 200);


	private record DisplayItem(FLEFRecord record, String displayText){}


	private final FLEFModel model;
	private final RecordTypeHandler<?> defaultType;
	private final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect;
	private Consumer<BaseRecordDialog> setupDialog;

	private final Map<String, String> initialFilters = new HashMap<>();

	// Cache display text for each record to avoid recomputing on repeated filtering operations
	private final Map<FLEFRecord, String> displayTextCache = new ConcurrentHashMap<>();

	// UI Components
	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	private final JTextField searchField = new JTextField();
	private final JCheckBox fuzzyCheckBox = new JCheckBox("Fuzzy", false);
	private final JCheckBox wholeWordCheckBox = new JCheckBox("Whole word", false);

	private final JPanel filterPanelHolder = new JPanel(new BorderLayout());

	private final DefaultListModel<DisplayItem> listModel = new DefaultListModel<>();
	private final JList<DisplayItem> resultList = GUIHelper.createList(listModel);
	private final JLabel statusLabel = new JLabel(" ");
	private final JProgressBar progressBar = new JProgressBar(0, 100);

	// Services & Async tasks
	private final SearchService searchService;
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> performSearch(), DEBOUNCE_TIME);
	private SwingWorker<List<DisplayItem>, Integer> currentWorker;

	// State
	private boolean confirmed;
	private String selectedType;
	private FLEFRecord selectedRecord;
	private final boolean allowCreation;

	private SearchCriteria criteria;


	@SuppressWarnings("unchecked")
	public static RecordSelectionDialog create(final Dialog parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		return new RecordSelectionDialog(parent, model, onSelect, handlerTypes);
	}

	@SuppressWarnings("unchecked")
	public static RecordSelectionDialog createWithAllowRecordCreation(final Dialog parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		return new RecordSelectionDialog(parent, model, onSelect, true, handlerTypes);
	}


	@SafeVarargs
	private RecordSelectionDialog(final Dialog parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		this(parent, model, onSelect, false, handlerTypes);
	}

	@SafeVarargs
	private RecordSelectionDialog(final Dialog parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final boolean allowCreation, final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		super(parent, "Select Record", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.onSelect = onSelect;
		this.allowCreation = allowCreation;
		defaultType = (handlerTypes.length == 1? HandlerRegistry.getHandler(handlerTypes[0]): null);

		searchService = new SearchService(model);

		typeCombo = (handlerTypes.length > 1? createTypeCombo(handlerTypes): null);

		initComponents();
		setupEscapeKey();

		pack();

		setMinimumSize(new Dimension(600, 450));

		setLocationRelativeTo(parent);


		updateFilterPanel();

		performSearch();
	}


	public RecordSelectionDialog withSetupDialog(final Consumer<BaseRecordDialog> setupDialog){
		this.setupDialog = setupDialog;

		return this;
	}

	/**
	 * Pre-populates a filter parameter for the search criteria.
	 *
	 * @param key   the filter key (e.g., "sex")
	 * @param value the filter value
	 * @return this dialog instance
	 */
	public RecordSelectionDialog withFilter(final String key, final String value){
		if(key != null && value != null)
			initialFilters.put(key, value);

		return this;
	}


	private JComboBox<RecordTypeHandler<?>> createTypeCombo(final Class<? extends RecordTypeHandler<?>>[] handlerTypes){
		final RecordTypeHandler<?>[] handlers = new RecordTypeHandler[handlerTypes.length];
		for(int i = 0, length = handlerTypes.length; i < length; i ++)
			handlers[i] = HandlerRegistry.getHandler(handlerTypes[i]);

		final JComboBox<RecordTypeHandler<?>> combo = new JComboBox<>(handlers);
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
				final int index, final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if(value instanceof RecordTypeHandler<?> handler)
					setText(handler.getLabel());
				return this;
			}
		});
		combo.addActionListener(e -> {
			searchField.setText(StringUtils.EMPTY);

			updateFilterPanel();

			performSearch();
		});
		return combo;
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10, fill", "[grow,fill]", "[][][grow][]"));

		// Top Panel: Type + Text Search + Checkboxes
		final JPanel topPanel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", "[]"));
		topPanel.setBorder(BorderFactory.createTitledBorder("Search"));

		if(typeCombo != null){
			topPanel.add(new JLabel("Type:"));
			topPanel.add(typeCombo, "growx");
		}
		topPanel.add(new JLabel("Search text:"));
		topPanel.add(searchField, "growx");
		topPanel.add(fuzzyCheckBox, "span 2,left");
		topPanel.add(wholeWordCheckBox, "span 2,left");

		fuzzyCheckBox.addActionListener(e -> {
			if(fuzzyCheckBox.isSelected())
				wholeWordCheckBox.setSelected(false);
			scheduleSearch();
		});
		wholeWordCheckBox.addActionListener(e -> {
			if(wholeWordCheckBox.isSelected())
				fuzzyCheckBox.setSelected(false);
			scheduleSearch();
		});

		add(topPanel, "growx,wrap");

		// Dynamic Filters Panel
		add(filterPanelHolder, "growx,wrap");

		// Results Panel
		resultList.setFixedCellHeight(22);
		resultList.setCellRenderer(new DefaultListCellRenderer(){
			@Serial
			private static final long serialVersionUID = 695210076800270818L;

			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if(value instanceof DisplayItem item)
					setText(item.displayText());
				return this;
			}
		});
		final JScrollPane scrollPane = GUIHelper.createScrollPane(resultList);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Results"));
		scrollPane.setPreferredSize(SCROLL_PANE_PREFERRED_SIZE);
		add(scrollPane, "grow,push,wrap");

		// Status Bar with Progress Bar
		progressBar.setIndeterminate(false);
		progressBar.setStringPainted(true);
		progressBar.setVisible(false);

		final JPanel statusPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow,fill][]", "[]"));
		statusPanel.add(statusLabel, "left");
		statusPanel.add(progressBar, "w 140!,right,hidemode 3");
		add(statusPanel, "growx,wrap");

		// Action Buttons Panel
		final JButton selectButton = new JButton("Select");
		final JButton cancelButton = new JButton("Cancel");

		selectButton.addActionListener(e -> selectResult());
		cancelButton.addActionListener(e -> dispose());

		final JPanel bottomPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow 1,sg 1][grow 1,sg 1]", "[]"));

		// Left column (New button if allowed, otherwise empty panel)
		if(allowCreation){
			final JButton createButton = new JButton("New");
			createButton.addActionListener(e -> createNewRecord());
			bottomPanel.add(createButton, "left");
		}
		else
			bottomPanel.add(new JPanel(), "left");

		// Right column (Select and Cancel buttons pushed to the right edge)
		final JPanel rightPanel = new JPanel(new MigLayout("ins 0", "[]10[]"));
		rightPanel.add(selectButton);
		rightPanel.add(cancelButton);
		bottomPanel.add(rightPanel, "right");

		add(bottomPanel, "growx");

		// Listeners
		searchField.getDocument().addDocumentListener(new DocumentListener(){
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
		});

		resultList.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					selectResult();
			}
		});

		updateWindowTitle();
	}

	private void setupEscapeKey(){
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(GUIHelper.ESCAPE_STROKE, "cancelSearchOrDialog");
		getRootPane().getActionMap()
			.put("cancelSearchOrDialog", new AbstractAction(){
				@Override
				public void actionPerformed(final ActionEvent e){
					if(currentWorker != null && !currentWorker.isDone())
						cancelSearch();
					else
						dispose();
				}
			});
	}

	private void cancelSearch(){
		if(currentWorker != null && !currentWorker.isDone()){
			currentWorker.cancel(true);
			progressBar.setVisible(false);
			statusLabel.setText("Search cancelled");
		}
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

	private void updateFilterPanel(){
		displayTextCache.clear();

		updateWindowTitle();

		final RecordTypeHandler<?> handler = getSelectedHandler();
		filterPanelHolder.removeAll();
		if(handler != null){
			final JPanel panel = FilterPanelFactory.createPanel(handler, criteria -> scheduleSearch());
			filterPanelHolder.add(panel, BorderLayout.CENTER);
		}

		filterPanelHolder.revalidate();
		filterPanelHolder.repaint();

		pack();
	}

	private void scheduleSearch(){
		searchDebouncer.call("search");
	}

	private void performSearch(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
		if(handler == null)
			return;

		if(currentWorker != null && !currentWorker.isDone())
			currentWorker.cancel(true);

		criteria = new SearchCriteria(handler, searchField.getText()
			.trim(),
			fuzzyCheckBox.isSelected(), wholeWordCheckBox.isSelected());

		initialFilters.forEach(criteria::withFilter);

		if(filterPanelHolder.getComponentCount() > 0
				&& filterPanelHolder.getComponent(0) instanceof RecordFilterPanel filterPanel)
			filterPanel.getFilters()
				.forEach(criteria::withFilter);

		statusLabel.setText("Searching…");
		progressBar.setValue(0);
		progressBar.setVisible(true);
		resultList.setEnabled(false);
		listModel.clear();

		currentWorker = new SwingWorker<>(){
			@Override
			protected List<DisplayItem> doInBackground(){
				// Phase 1: Record filtering (0% - 50% of the progress bar)
				final List<FLEFRecord> records = searchService.search(criteria,
					progress -> publish(progress / 2));
				if(isCancelled())
					return null;

				// Phase 2: Compute or fetch cached display texts in background (50% - 100% of progress)
				final List<DisplayItem> items = new ArrayList<>(records.size());
				final int total = records.size();
				for(int i = 0; i < total; i ++){
					if(isCancelled())
						return null;

					final FLEFRecord record = records.get(i);
					final String text = displayTextCache.computeIfAbsent(record, searchService::getDisplayText);
					items.add(new DisplayItem(record, text));

					final int progress = 50 + (int)(((i + 1) / (double)total) * 50);
					publish(progress);
				}

				return items;
			}

			@Override
			protected void process(final List<Integer> chunks){
				final int latestProgress = chunks.getLast();
				progressBar.setValue(latestProgress);
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				try{
					final List<DisplayItem> items = get();
					if(items != null)
						updateResults(items);
				}
				catch(final Exception e){
					statusLabel.setText("Error during search");
					LOGGER.error("Error during search", e);

					listModel.clear();
					resultList.setEnabled(false);
				}
				finally{
					progressBar.setVisible(false);
				}
			}
		};
		currentWorker.execute();
	}

	private void updateResults(final List<DisplayItem> items){
		listModel.clear();

		if(items.isEmpty()){
			listModel.addElement(new DisplayItem(null, NO_MATCHING_RECORDS));

			resultList.setEnabled(false);
		}
		else{
			listModel.addAll(items);

			resultList.setEnabled(true);
		}

		statusLabel.setText("Found " + items.size() + " records");
	}

	private void createNewRecord(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
		if(handler == null)
			return;

		final BaseRecordDialog dialog = handler.createNewDialog(this, model);
		if(setupDialog != null)
			setupDialog.accept(dialog);
		dialog.setVisible(true);

		if(dialog.isSaved()){
			final FLEFRecord newRecord = dialog.getRecord();
			if(newRecord != null){
				selectedType = handler.getType();
				selectedRecord = newRecord;
				confirmed = true;

				if(onSelect != null)
					onSelect.accept(newRecord, handler);

				firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

				dispose();
			}
		}
	}

	private void selectResult(){
		final DisplayItem selectedItem = resultList.getSelectedValue();
		if(selectedItem != null && selectedItem.record() != null){
			final RecordTypeHandler<?> handler = getSelectedHandler();
			if(handler != null){
				selectedRecord = selectedItem.record();
				selectedType = handler.getType();
				confirmed = true;

				if(onSelect != null)
					onSelect.accept(selectedRecord, handler);

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


	public static void main(final String[] args) throws IOException{
		try{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(final Exception ignored){}

		final String modelUri = "/tests/TGMZ.flef";

		final String content;
		try(final InputStream is = BiologicalTreePanel.class.getResourceAsStream(modelUri)){
			content = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			@SuppressWarnings("unchecked")
			final RecordSelectionDialog dialog = create(null, model,
//			final RecordSelectionDialog dialog = createWithAllowRecordCreation(null, model,
				(record, handler) -> System.out.println(record),
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.ConclusionHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.CulturalNormHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.DocumentHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.HistoricEventHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.IdentityHypothesisHandler.class);
				io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchQuestionHandler.class);
//				io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler.class);
			dialog.setVisible(true);
		});
	}

}
