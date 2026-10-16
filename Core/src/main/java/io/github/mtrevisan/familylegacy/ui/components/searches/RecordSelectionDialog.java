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
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.Debouncer;
import io.github.mtrevisan.familylegacy.ui.bindings.FilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship.UnlinkDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.places.PlaceHierarchyPanel;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
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
import java.awt.CardLayout;
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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * Unified dialog for selecting or creating a record across multiple record types.
 * <p>
 * Two independent search modifiers are exposed as checkboxes:
 * <ul>
 *   <li><b>Whole word</b>: the query must match a token of the target
 *       exactly, instead of appearing anywhere inside it;</li>
 *   <li><b>Fuzzy</b>: small typographical differences are tolerated.</li>
 * </ul>
 * The two modifiers are independent and can be combined in any way.
 * <p>
 * <b>Place-relationship mode.</b> When the selected record type is
 * {@code place_relationship}, the dialog swaps the flat list for a
 * {@link PlaceHierarchyPanel}: a place relationship is meaningful only
 * in the context of the hierarchy it belongs to, so a flat list of
 * relations would be incomprehensible.
 * <p>
 * In that mode the search field and the dynamic filter panel remain
 * visible and functional: the text field drives the hierarchy's
 * substring filter, and the dynamic filter panel (a
 * {@code PlaceRelationshipFilterPanel}) drives the hierarchy's
 * relation-type filter. The fuzzy and whole-word checkboxes are hidden,
 * because the hierarchy uses a plain substring match and the checkboxes
 * would imply an effect that does not exist. Edit, unlink, and delete
 * are disabled, because the dialog is being used to pick a reference,
 * not to modify existing records.
 */
public class RecordSelectionDialog extends JDialog{

	private static final Logger LOGGER = LoggerFactory.getLogger(RecordSelectionDialog.class);


	private static final String NO_MATCHING_RECORDS = I18N.t("search.no_matching_records");

	private static final int DEBOUNCE_TIME = 400;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";

	private static final Dimension SCROLL_PANE_PREFERRED_SIZE = new Dimension(450, 200);
	public static final String ACTION_CANCEL_SEARCH_OR_DIALOG = "cancelSearchOrDialog";

	private static final String CARD_LIST = "card-list";
	private static final String CARD_HIERARCHY = "card-hierarchy";


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

	// Search modifiers: two independent checkboxes. Either, both, or
	// neither can be selected. The combination determines the effective
	// search mode, computed by currentMode().
	private final JCheckBox fuzzyCheckBox = new JCheckBox(I18N.t("search.mode.fuzzy"), false);
	private final JCheckBox wholeWordCheckBox = new JCheckBox(I18N.t("search.mode.whole_word"), false);

	private final JPanel filterPanelHolder = new JPanel(new BorderLayout());

	private final DefaultListModel<DisplayItem> listModel = new DefaultListModel<>();
	private final JList<DisplayItem> resultList = BindingsHelper.createList(listModel);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final JProgressBar progressBar = new JProgressBar(0, 100);

	/**
	 * Card container for the central area. Holds the flat list card and,
	 * lazily, the place-hierarchy card. Exactly one card is visible at a
	 * time; the switch is driven by the selected record type.
	 */
	private final JPanel centerPanel = new JPanel(new CardLayout());

	/**
	 * The place-hierarchy card. Created lazily on the first switch to a
	 * {@code place_relationship} type, so that a dialog opened for a
	 * different type does not pay the cost of building the hierarchy.
	 */
	private PlaceHierarchyPanel placeHierarchyPanel;

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
		topPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("search.search_panel.title")));

		if(typeCombo != null){
			topPanel.add(new JLabel(I18N.t("search.type") + ":"));
			topPanel.add(typeCombo, "growx");
		}
		topPanel.add(new JLabel(I18N.t("search.search_text") + ":"));
		topPanel.add(searchField, "growx");

		// Search modifiers: two independent checkboxes. Both start
		// unselected, which means "plain substring match". Selecting one
		// or both narrows the search according to the semantics
		// documented on the class.
		topPanel.add(wholeWordCheckBox, "span 2,left");
		topPanel.add(fuzzyCheckBox, "span 2,left");

		// Every change to either checkbox reschedules the search.
		fuzzyCheckBox.addActionListener(e -> scheduleSearch());
		wholeWordCheckBox.addActionListener(e -> scheduleSearch());

		add(topPanel, "growx,wrap");

		// Dynamic Filters Panel
		add(filterPanelHolder, "growx,wrap");

		// Results Panel: a card container that switches between the flat
		// list and the place hierarchy, depending on the selected type.
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
		scrollPane.setBorder(BorderFactory.createTitledBorder(I18N.t("search.results_panel.title")));
		scrollPane.setPreferredSize(SCROLL_PANE_PREFERRED_SIZE);
		centerPanel.add(scrollPane, CARD_LIST);
		add(centerPanel, "grow,push,wrap");

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
		// Buttons kept as fields so their enabled state can be toggled when
		// the dialog switches to the hierarchy card.
		final JButton editButton = new JButton(I18N.t("button.edit"));
		final JButton unlinkButton = new JButton(I18N.t("button.unlink"));
		final JButton deleteButton = new JButton(I18N.t("button.delete"));
		final JButton cancelButton = new JButton(I18N.t("button.cancel"));

		selectButton.addActionListener(e -> selectResult());
		editButton.addActionListener(e -> editRecord());
		unlinkButton.addActionListener(e -> unlinkRecord());
		deleteButton.addActionListener(e -> deleteRecord());
		cancelButton.addActionListener(e -> dispose());

		final JPanel bottomPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow 1,sg 1][grow 1,sg 1]", "[]"));

		// Left column (New button if allowed, otherwise empty panel)
		final JPanel leftPanel = new JPanel(new MigLayout("ins 0", "[]10[]"));
		if(allowCreation){
			final JButton createButton = new JButton(I18N.t("button.new"));
			createButton.addActionListener(e -> createNewRecord());
			leftPanel.add(createButton);
		}
		leftPanel.add(editButton);
		leftPanel.add(unlinkButton);
		leftPanel.add(deleteButton);
		bottomPanel.add(leftPanel, "left");

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
		setTitle(I18N.tf("search.title_with_record", (desc != null), I18N.t("confirmation.exist_record."
			+ (desc != null? desc.getType().toLowerCase(): StringUtils.EMPTY))));
	}

	private RecordTypeHandler<?> getSelectedHandler(){
		if(defaultType != null)
			return defaultType;

		return (typeCombo != null? (RecordTypeHandler<?>)typeCombo.getSelectedItem(): null);
	}

	/**
	 * Returns {@code true} when the currently selected record type is
	 * {@code place_relationship}. In that case the dialog shows the
	 * place hierarchy instead of the flat list.
	 */
	private boolean isPlaceRelationshipHandler(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
		return (handler != null && Objects.equals(PlaceRelationshipHandler.TYPE, handler.getType()));
	}

	/**
	 * Returns the search mode implied by the current combination of the
	 * two independent checkboxes.
	 *
	 * @return the effective search mode
	 */
	private Set<SearchMode> currentMode(){
		final Set<SearchMode> modes = EnumSet.noneOf(SearchMode.class);
		if(fuzzyCheckBox.isSelected())
			modes.add(SearchMode.FUZZY);
		if(wholeWordCheckBox.isSelected())
			modes.add(SearchMode.WHOLE_WORD);

		return modes;
	}

	/**
	 * Refreshes the central content and the toolbar controls when the
	 * selected record type changes.
	 * <p>
	 * For {@code place_relationship}, the flat list is swapped for the
	 * place hierarchy. The search field and the dynamic filter panel
	 * stay visible and drive the hierarchy's filters through
	 * {@link #applyFiltersToHierarchy()}. The fuzzy and whole-word
	 * checkboxes are hidden because they would imply an effect that the
	 * hierarchy does not have. Edit, unlink, and delete are disabled
	 * because the dialog is picking a reference, not modifying records.
	 * <p>
	 * For every other type, the flat list is restored and the search
	 * controls are made visible again.
	 */
	private void updateFilterPanel(){
		displayTextCache.clear();

		updateWindowTitle();

		final RecordTypeHandler<?> handler = getSelectedHandler();
		final boolean isPlaceRelationship = isPlaceRelationshipHandler();

		if(isPlaceRelationship)
			showHierarchyCard();
		else
			showListCard();

		updateControlsVisibility();
		rebuildDynamicFilterPanel(handler);

		if(isPlaceRelationship)
			applyFiltersToHierarchy();

		pack();
	}

	private void showHierarchyCard(){
		// The hierarchy panel is created lazily on the first switch to
		// the place_relationship type. Once created, it stays in the
		// card layout and is merely reloaded on subsequent switches.
		if(placeHierarchyPanel == null){
			placeHierarchyPanel = new PlaceHierarchyPanel(model);
			placeHierarchyPanel.setEditable(true);

			final JPanel wrapper = new JPanel(new BorderLayout());
			wrapper.setBorder(BorderFactory.createTitledBorder(I18N.t("search.results_panel.title")));
			wrapper.add(placeHierarchyPanel, BorderLayout.CENTER);

			centerPanel.add(wrapper, CARD_HIERARCHY);
		}
		else
			placeHierarchyPanel.reload();

		final CardLayout layout = (CardLayout)centerPanel.getLayout();
		layout.show(centerPanel, CARD_HIERARCHY);
	}

	private void showListCard(){
		final CardLayout layout = (CardLayout)centerPanel.getLayout();
		layout.show(centerPanel, CARD_LIST);
	}

	private void updateControlsVisibility(){
		searchField.setVisible(true);
		filterPanelHolder.setVisible(true);
		wholeWordCheckBox.setVisible(true);
		fuzzyCheckBox.setVisible(true);
	}

	private void rebuildDynamicFilterPanel(final RecordTypeHandler<?> handler){
		filterPanelHolder.removeAll();
		if(handler != null){
			final JPanel panel = FilterPanelFactory.createPanel(handler, criteria -> scheduleSearch());
			if(panel != null)
				filterPanelHolder.add(panel, BorderLayout.CENTER);
		}

		filterPanelHolder.revalidate();
		filterPanelHolder.repaint();
	}

	private void scheduleSearch(){
		searchDebouncer.call(FilteredComboBox.PROPERTY_DEBOUNCER);
	}

	private void performSearch(){
		// In place-relationship mode the search is routed to the
		// hierarchy: the same text field and the same dynamic filter
		// panel drive the tree. The flat-list search pipeline is not
		// started at all.
		if(isPlaceRelationshipHandler()){
			applyFiltersToHierarchy();

			return;
		}

		if(currentWorker != null && !currentWorker.isDone())
			currentWorker.cancel(true);

		final RecordTypeHandler<?> handler = getSelectedHandler();
		criteria = new SearchCriteria(handler, searchField.getText().trim(), currentMode());

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

	/**
	 * Forwards the current search text and the current filter panel to
	 * the hierarchy. Called whenever the search input changes while the
	 * dialog is in {@code place_relationship} mode.
	 */
	private void applyFiltersToHierarchy(){
		if(placeHierarchyPanel == null)
			return;

		if(placeHierarchyPanel.getPropertyChangeListeners(
			PlaceHierarchyPanel.PROPERTY_FILTERING_PROGRESS).length == 0){
			placeHierarchyPanel.addPropertyChangeListener(
				PlaceHierarchyPanel.PROPERTY_FILTERING_PROGRESS, evt -> {
					final Integer progress = (Integer)evt.getNewValue();
					if(progress != null){
						progressBar.setValue(progress);
						progressBar.repaint();
					}
				});
		}
		if(placeHierarchyPanel.getPropertyChangeListeners(PlaceHierarchyPanel.PROPERTY_FILTERING_DONE).length == 0)
			placeHierarchyPanel.addPropertyChangeListener(
				PlaceHierarchyPanel.PROPERTY_FILTERING_DONE, evt -> {
					progressBar.setValue(100);
					progressBar.setVisible(false);
					statusLabel.setText(StringUtils.SPACE);
				});

		String relationType = null;
		if(filterPanelHolder.getComponentCount() > 0
				&& filterPanelHolder.getComponent(0) instanceof RecordFilterPanel filterPanel){
			final Map<String, String> filters = filterPanel.getFilters();
			relationType = filters.get(PlaceRelationshipReader.TAG_TYPE);
		}

		final boolean rebuildStarted = placeHierarchyPanel.setFilters(
			currentMode(), searchField.getText().trim(), relationType);

		if(rebuildStarted){
			progressBar.setIndeterminate(false);
			progressBar.setValue(0);
			progressBar.setVisible(true);
			progressBar.repaint();
			statusLabel.setText(I18N.t("search.searching"));
		}
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

		statusLabel.setText(I18N.tf("search.record_found", items.size()));
	}

	private void createNewRecord(){
		final RecordTypeHandler<?> handler = getSelectedHandler();
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
		// In place-relationship mode the selection comes from the
		// hierarchy tree, not from the flat list. A node with no incoming
		// relation (a root place) yields null, which tells the user that
		// no relationship is selected: a relationship is meaningful only
		// as an edge between a child place and a parent place.
		if(isPlaceRelationshipHandler()){
			if(placeHierarchyPanel == null)
				return;

			final FLEFRecord selectedRelationship = placeHierarchyPanel.getSelectedPlaceRecord();
			if(selectedRelationship == null){
				JOptionPane.showMessageDialog(this,
					I18N.t("search.no_selection.place_relationship"),
					I18N.t("search.no_selection.title"), JOptionPane.INFORMATION_MESSAGE);

				return;
			}

			selectedRecord = selectedRelationship;
			selectedType = PlaceRelationshipHandler.TYPE;
			confirmed = true;

			if(onSelect != null)
				onSelect.accept(selectedRecord, getSelectedHandler());

			firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

			dispose();

			return;
		}

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
				I18N.t("search.no_selection.message"),
				I18N.t("search.no_selection.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	private void editRecord(){
		if(isPlaceRelationshipHandler()){
			placeHierarchyPanel.openEditorForSelection();

			return;
		}

		final RecordTypeHandler<?> handler = getSelectedHandler();
		if(handler != null){
			final DisplayItem selectedItem = resultList.getSelectedValue();
			if(selectedItem == null || selectedItem.record() == null)
				return;

			final FLEFRecord record = selectedItem.record();
			final BaseRecordDialog dialog = handler.createEditDialog(this, model, record);
			dialog.setVisible(true);

			if(dialog.isSaved()){
				// Invalidate cache for this record and refresh results
				displayTextCache.remove(record);

				performSearch();
			}
		}
	}

	// TODO test
	private void deleteRecord(){
		final FLEFRecord record;
		final String displayText;

		// 1. Retrieve the selected record and its display text based on the current mode
		if(isPlaceRelationshipHandler()){
			record = placeHierarchyPanel.getSelectedPlaceRecord();
			displayText = searchService.getDisplayText(record);
		}
		else {
			final DisplayItem selectedItem = resultList.getSelectedValue();
			if(selectedItem == null || selectedItem.record() == null)
				return;

			record = selectedItem.record();
			displayText = selectedItem.displayText();
		}

		// 2. Ask for confirmation
		final int confirm = JOptionPane.showConfirmDialog(this,
			I18N.tf("confirmation.delete.record.message", false,
				I18N.t("confirmation.exist_record." + record.getTag()), displayText),
			I18N.t("confirmation.delete.record.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);

		if(confirm != JOptionPane.YES_OPTION)
			return;

		// 3. Remove the target record from the model
		model.removeRecord(record.getId());

		// 4. Clean cache and refresh the appropriate view
		displayTextCache.remove(record);
		if(isPlaceRelationshipHandler())
			placeHierarchyPanel.reload();
		else
			performSearch();
	}

	// TODO test
	private void unlinkRecord(){
		final FLEFRecord record;
		final String displayText;

		// 1. Retrieve the selected record and its display text based on the current mode
		if(isPlaceRelationshipHandler()){
			record = placeHierarchyPanel.getSelectedPlaceRecord();
			displayText = searchService.getDisplayText(record);
		}
		else {
			final DisplayItem selectedItem = resultList.getSelectedValue();
			if(selectedItem == null || selectedItem.record() == null)
				return;

			record = selectedItem.record();
			displayText = selectedItem.displayText();
		}

		final String recordId = record.getId();

		// 2. Check if the record has active relationships using UnlinkRelationshipsDialog
		// Since we are now selecting a PlaceRecord (both in hierarchy and flat list),
		// this dialog correctly lists all relationships pointing to/from this place.
		final UnlinkDialog dialog = new UnlinkDialog(this, model, recordId);
		dialog.setVisible(true);

		final List<String> linkIdsToRemove = dialog.getSelectedLinkIds();
		if(linkIdsToRemove.isEmpty())
			return;

		// 3. Prompt confirmation before deleting the record itself
		final int confirm = JOptionPane.showConfirmDialog(this,
			I18N.tf("confirmation.delete.record.message", dialog.removeParentRecord(),
				I18N.t("confirmation.exist_record." + record.getTag()), displayText),
			I18N.t("confirmation.delete.record.title"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		// 4. First, remove any selected relationship records to maintain referential integrity
		for(final String linkId : linkIdsToRemove)
			model.removeRecord(linkId);

		// 5. Then, remove the target record itself from the model (if requested)
		if(dialog.removeParentRecord())
			model.removeRecord(recordId);

		// 6. Clean cache and refresh the appropriate view
		displayTextCache.remove(record);
		if(isPlaceRelationshipHandler())
			placeHierarchyPanel.reload();
		else
			performSearch();
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
//				io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler.class);
				io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler.class);
//				io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler.class);
			dialog.setVisible(true);
		});
	}

}
