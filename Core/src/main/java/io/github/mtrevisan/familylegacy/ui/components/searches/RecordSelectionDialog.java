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
package io.github.mtrevisan.familylegacy.ui.components.searches;

import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.Debouncer;
import io.github.mtrevisan.familylegacy.ui.bindings.FilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
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
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Unified dialog for selecting or creating a record across multiple record types.
 * Supports dynamic filtering strategies and three text search modes
 * (whole word, fuzzy).
 * <p>
 * The three search modes are mutually exclusive and are selected through
 * a group of checkboxes. Their semantics are documented on
 * {@link SearchMode}; in short:
 * <ul>
 *   <li><b>Whole word</b>: exact equality between query and target token;</li>
 *   <li><b>Fuzzy</b> (default): substring or small typographical
 *       difference.</li>
 * </ul>
 * Whichever the mode, the records are ranked by the number of matched
 * query tokens first, and by the sum of the per-token fuzzy scores
 * second, so a record matching both words of a two-word query always
 * ranks above a record matching only one.
 */
public class RecordSelectionDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(RecordSelectionDialog.class);


	private static final String NO_MATCHING_RECORDS = I18N.t("search.no.matching.records");

	private static final int DEBOUNCE_TIME = 400;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";

	private static final Dimension SCROLL_PANE_PREFERRED_SIZE = new Dimension(450, 200);
	public static final String ACTION_CANCEL_SEARCH_OR_DIALOG = "cancelSearchOrDialog";


	private record DisplayItem(FLEFRecord record, String displayText){}


	private final FLEFModel model;
	private final RecordTypeHandler<?> defaultType;
	private final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect;
	private Consumer<BaseRecordDialog> setupDialog;

	private final Map<String, String> initialFilters = new HashMap<>();

	// Cache display text for each record to avoid recomputing on repeated
	// filtering operations. Filled on the background thread during a
	// search, and reused across searches of the same session.
	private final Map<FLEFRecord, String> displayTextCache = new ConcurrentHashMap<>();

	// UI Components
	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	private final JTextField searchField = new JTextField();

	// Search mode: three mutually exclusive checkboxes. Fuzzy is the
	// default because it is the most permissive and it covers the most
	// common case (the user remembers part of the name and is not sure
	// of the exact spelling). The mutual exclusion is enforced manually:
	// selecting one box deselects the others, and deselecting the active
	// box without selecting another one falls back to Fuzzy.
	private final JCheckBox fuzzyCheckBox = new JCheckBox(SearchMode.FUZZY.label(), true);
	private final JCheckBox wholeWordCheckBox = new JCheckBox(SearchMode.WHOLE_WORD.label(), false);

	private final JPanel filterPanelHolder = new JPanel(new BorderLayout());

	private final DefaultListModel<DisplayItem> listModel = new DefaultListModel<>();
	private final JList<DisplayItem> resultList = BindingsHelper.createList(listModel);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
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
	public static RecordSelectionDialog create(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		return new RecordSelectionDialog(parent, model, onSelect, handlerTypes);
	}

	@SuppressWarnings("unchecked")
	public static RecordSelectionDialog createWithAllowRecordCreation(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		return new RecordSelectionDialog(parent, model, onSelect, true, handlerTypes);
	}


	@SafeVarargs
	private RecordSelectionDialog(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		this(parent, model, onSelect, false, handlerTypes);
	}

	@SafeVarargs
	private RecordSelectionDialog(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final boolean allowCreation, final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		super(parent, I18N.t("search.title"), ModalityType.APPLICATION_MODAL);

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
		setLayout(new MigLayout("ins 10,fill", "[grow,fill]", "[][][grow][]"));

		// Top Panel: Type + Text Search + Search-mode checkboxes
		final JPanel topPanel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", "[]"));
		topPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("search.search.panel.title")));

		if(typeCombo != null){
			topPanel.add(new JLabel(I18N.t("search.type") + ":"));
			topPanel.add(typeCombo, "growx");
		}
		topPanel.add(new JLabel(I18N.t("search.search.text") + ":"));
		topPanel.add(searchField, "growx");

		// Search mode checkboxes, mutually exclusive. The three boxes are
		// laid out in the second column so they line up with the search
		// field, and span both columns of the grid.
		topPanel.add(wholeWordCheckBox, "span 2,left");
		topPanel.add(fuzzyCheckBox, "span 2,left");

		// Mutual exclusion: selecting one mode deselects the others, and
		// each change reschedules the search. If the user deselects the
		// currently selected mode without selecting another one, the
		// selection falls back to Fuzzy so that exactly one mode is
		// always active.
		final List<JCheckBox> modeBoxes = List.of(fuzzyCheckBox, wholeWordCheckBox);
		for(final JCheckBox box : modeBoxes){
			box.addActionListener(e -> {
				if(box.isSelected()){
					for(final JCheckBox other : modeBoxes)
						if(other != box)
							other.setSelected(false);
				}
				else
					ensureOneModeSelected(modeBoxes);

				scheduleSearch();
			});
		}

		add(topPanel, "growx,wrap");

		// Dynamic Filters Panel
		add(filterPanelHolder, "growx,wrap");

		// Results Panel
		resultList.setFixedCellHeight(22);
		resultList.setCellRenderer(new DefaultListCellRenderer(){
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
		scrollPane.setBorder(BorderFactory.createTitledBorder(I18N.t("search.results.panel.title")));
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
		final JButton selectButton = new JButton(I18N.t("button.select"));
		final JButton cancelButton = new JButton(I18N.t("button.cancel"));

		selectButton.addActionListener(e -> selectResult());
		cancelButton.addActionListener(e -> dispose());

		final JPanel bottomPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow 1,sg 1][grow 1,sg 1]", "[]"));

		// Left column (New button if allowed, otherwise empty panel)
		if(allowCreation){
			final JButton createButton = new JButton(I18N.t("button.new"));
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

	/**
	 * Ensures that exactly one of the mode checkboxes is selected. Called
	 * when the user deselects the currently active checkbox: the
	 * selection falls back to Fuzzy, which is the most permissive mode
	 * and therefore the safest default.
	 */
	private static void ensureOneModeSelected(final List<JCheckBox> modeBoxes){
		for(final JCheckBox box : modeBoxes)
			if(box.isSelected())
				return;

		// Nothing selected: restore the default (Fuzzy).
		modeBoxes.getFirst()
			.setSelected(true);
	}

	private void setupEscapeKey(){
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(GUIHelper.ESCAPE_STROKE, ACTION_CANCEL_SEARCH_OR_DIALOG);
		getRootPane().getActionMap()
			.put(ACTION_CANCEL_SEARCH_OR_DIALOG, new AbstractAction(){
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
			statusLabel.setText(I18N.t("search.canceled"));
		}
	}

	private void updateWindowTitle(){
		final RecordTypeHandler<?> desc = getSelectedHandler();
		setTitle(I18N.tf("search.title", (desc != null), I18N.t("confirmation.exist.record.individual")));
	}

	private RecordTypeHandler<?> getSelectedHandler(){
		if(defaultType != null)
			return defaultType;

		return (typeCombo != null? (RecordTypeHandler<?>)typeCombo.getSelectedItem(): null);
	}

	/**
	 * Returns the currently active search mode. Precedence is
	 * deterministic and mirrors the checkbox layout: Whole word, then
	 * Fuzzy. In practice exactly one box is ever selected,
	 * so the precedence never actually matters.
	 */
	private SearchMode currentMode(){
		if(wholeWordCheckBox.isSelected())
			return SearchMode.WHOLE_WORD;

		return SearchMode.FUZZY;
	}

	private void updateFilterPanel(){
		displayTextCache.clear();

		updateWindowTitle();

		final RecordTypeHandler<?> handler = getSelectedHandler();
		filterPanelHolder.removeAll();
		if(handler != null){
			final JPanel panel = FilterPanelFactory.createPanel(handler, criteria -> scheduleSearch());
			if(panel != null)
				filterPanelHolder.add(panel, BorderLayout.CENTER);
		}

		filterPanelHolder.revalidate();
		filterPanelHolder.repaint();

		pack();
	}

	private void scheduleSearch(){
		searchDebouncer.call(FilteredComboBox.PROPERTY_DEBOUNCER);
	}

	private void performSearch(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
		if(handler == null)
			return;

		if(currentWorker != null && !currentWorker.isDone())
			currentWorker.cancel(true);

		criteria = new SearchCriteria(handler, searchField.getText()
			.trim(), currentMode());

		initialFilters.forEach(criteria::withFilter);

		if(filterPanelHolder.getComponentCount() > 0
			&& filterPanelHolder.getComponent(0) instanceof RecordFilterPanel filterPanel)
			filterPanel.getFilters()
				.forEach(criteria::withFilter);

		statusLabel.setText(I18N.t("search.searching"));
		progressBar.setValue(0);
		progressBar.setVisible(true);
		resultList.setEnabled(false);
		listModel.clear();

		currentWorker = new SwingWorker<>(){
			@Override
			protected List<DisplayItem> doInBackground(){
				// Phase 1: Record filtering and ranking (0% - 50% of the
				// progress bar). The service returns the records already
				// ordered by match quality: number of matched query tokens
				// first, sum of per-token fuzzy scores second, display text
				// third.
				final List<FLEFRecord> records = searchService.search(criteria,
					progress -> publish(progress / 2));
				if(isCancelled())
					return null;

				// Phase 2: Compute or fetch cached display texts in background
				// (50% - 100% of progress).
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
					statusLabel.setText(I18N.t("search.failed"));

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

		statusLabel.setText(I18N.tf("search.record.found", items.size()));
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
				I18N.t("search.no.selection.message"),
				I18N.t("search.no.selection.title"), JOptionPane.INFORMATION_MESSAGE);
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
		try(final InputStream is = RecordSelectionDialog.class.getResourceAsStream(modelUri)){
			content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
		}

		final FLEFParser parser = new FLEFParser();
		final FLEFModel model = parser.parse(content);

		SwingUtilities.invokeLater(() -> {
			@SuppressWarnings("unchecked")
			final RecordSelectionDialog dialog = create(null, model,
//			final RecordSelectionDialog dialog = createWithAllowRecordCreation(null, model,
				(record, handler) -> System.out.println(record),
//				io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.DocumentHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.EventHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler.class);
				io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler.class);
			dialog.setVisible(true);
		});
	}

}
