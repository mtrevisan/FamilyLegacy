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
package io.github.mtrevisan.familylegacy.ui.components.searches.service;

import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFParser;
import io.github.mtrevisan.familylegacy.io.grammar.typedefinitions.FLEFGrammar;
import io.github.mtrevisan.familylegacy.io.merger.RecordDeleter;
import io.github.mtrevisan.familylegacy.quality.RecordReferences;
import io.github.mtrevisan.familylegacy.io.merger.manual.MergeGrammar;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordTags;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.ResearchSummary;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.sources.SourceUsage;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.bindings.BindingsHelper;
import io.github.mtrevisan.familylegacy.ui.bindings.ComboBoxSearch;
import io.github.mtrevisan.familylegacy.ui.bindings.Debouncer;
import io.github.mtrevisan.familylegacy.ui.bindings.FilteredComboBox;
import io.github.mtrevisan.familylegacy.ui.components.searches.RecordFilterPanel;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.components.searches.strategies.IndividualFilterPanel;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.MediaHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchTaskHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.MessageDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
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
 * {@link PlaceHierarchyView}: a place relationship is meaningful only
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


	/** The records that use the one to delete that are named in the question (the rest is counted). */
	private static final int MAX_USES_LISTED = 8;
	private static final String NO_MATCHING_RECORDS = I18N.t("search.no_matching_records");

	private static final int DEBOUNCE_TIME = 400;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";

	private static final Dimension SCROLL_PANE_PREFERRED_SIZE = new Dimension(450, 200);
	public static final String ACTION_CANCEL_SEARCH_OR_DIALOG = "cancelSearchOrDialog";

	private static final String CARD_LIST = "card-list";
	private static final String CARD_HIERARCHY = "card-hierarchy";


	/** A result: the record, the text to show, the tooltip, and its handler when the search covers several types (otherwise {@code null}). */
	private record DisplayItem(FLEFRecord record, String displayText, String explanation, RecordTypeHandler<?> handler){}

	/** The pseudo-type "Any": the search covers all the types offered by the dialog. It has no records of its own. */
	private static final class AnyTypeHandler implements RecordTypeHandler<RecordDialog>{
		@Override
		public String getLabel(){
			return I18N.t("search.combo.any");
		}

		@Override
		public String getType(){
			return "any";
		}

		@Override
		public String getDisplayText(final FLEFRecord record, final FLEFModel model){
			return record.getId();
		}

		@Override
		public RecordDialog createNewDialog(final Window parent, final FLEFModel model){
			throw new UnsupportedOperationException("No record can be created for any type");
		}

		@Override
		public RecordDialog createEditDialog(final Window parent, final FLEFModel model, final FLEFRecord record){
			throw new UnsupportedOperationException("No record can be edited for any type");
		}
	}


	private final FLEFModel model;
	private final RecordTypeHandler<?> defaultType;
	private final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect;
	private Consumer<RecordDialog> setupDialog;

	private final Map<String, String> initialFilters = new HashMap<>();
	private String lockedSex;

	// Cache display text for each record to avoid recomputing on repeated
	// filtering operations. Filled on the background thread during a
	// search, and reused across searches of the same session.
	private final Map<FLEFRecord, String> displayTextCache = new ConcurrentHashMap<>();

	// UI Components
	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	//the pseudo-type that searches all the types, and the real types it stands for (only when the dialog offers more than one type)
	private final RecordTypeHandler<?> anyHandler = new AnyTypeHandler();
	private final List<RecordTypeHandler<?>> typeHandlers = new ArrayList<>();
	private JButton createButton;
	// The text and the search modifiers (whole word, fuzzy, notes and citations): independent options, either, both, or neither can be
	// on. The text is debounced by the dialog itself, so the box is listened to through its document and its option changes.
	private final SearchBox searchBox = new SearchBox(20, true);
	private String fixedTitle;

	private final JPanel filterPanelHolder = new JPanel(new BorderLayout());
	/** The free tags the records must have (only for the kinds of record that can be tagged), separated by commas. */
	private final JLabel tagsLabel = new JLabel(I18N.l("search.filter.tags"));
	private final JTextField tagsField = new JTextField(20);

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
	private PlaceHierarchyView placeHierarchyPanel;

	// Services & Async tasks
	private final SearchService searchService;
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> performSearch(), DEBOUNCE_TIME);
	private SwingWorker<List<DisplayItem>, Integer> currentWorker;

	// State
	private boolean confirmed;
	private String selectedType;
	private FLEFRecord selectedRecord;
	private final boolean allowCreation;
	//the dialog manages the records instead of picking one
	private final boolean manageMode;


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
		return new RecordSelectionDialog(parent, model, onSelect, true, false, handlerTypes);
	}

	/**
	 * Creates the dialog used to manage the records of a type (Manage Sources, Manage Places, ...): nothing is picked, so there is no
	 * Select button, the dialog stays open after a record has been created, a double click edits the record, and the dialog is
	 * closed with Close.
	 */
	public static RecordSelectionDialog createForManagement(final Window parent, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>> handlerType){
		return new RecordSelectionDialog(parent, model, (record, handler) -> {}, true, true, handlerType);
	}


	@SafeVarargs
	private RecordSelectionDialog(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		this(parent, model, onSelect, false, false, handlerTypes);
	}

	@SafeVarargs
	private RecordSelectionDialog(final Window parent, final FLEFModel model,
			final BiConsumer<FLEFRecord, RecordTypeHandler<?>> onSelect,
			final boolean allowCreation, final boolean manageMode, final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		super(parent, I18N.t("search.title.select_record"), ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.model = model;
		this.onSelect = onSelect;
		this.allowCreation = allowCreation;
		this.manageMode = manageMode;
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


	public RecordSelectionDialog withSetupDialog(final Consumer<RecordDialog> setupDialog){
		this.setupDialog = setupDialog;

		return this;
	}

	/**
	 * Restricts the search to the individuals of a sex, that the user cannot change.
	 *
	 * @param sex	the sex, as it is stored (for example {@code male}).
	 */
	public RecordSelectionDialog withLockedSex(final String sex){
		lockedSex = sex;
		if(sex != null && filterPanelHolder.getComponentCount() > 0 && filterPanelHolder.getComponent(0) instanceof IndividualFilterPanel individualPanel)
			individualPanel.lockSex(sex);

		return this;
	}

	public RecordSelectionDialog withFilter(final String key, final String value){
		if(key != null && value != null)
			initialFilters.put(key, value);

		return this;
	}


	private JComboBox<RecordTypeHandler<?>> createTypeCombo(final Class<? extends RecordTypeHandler<?>>[] handlerTypes){
		final RecordTypeHandler<?>[] handlers = new RecordTypeHandler[handlerTypes.length + 1];
		handlers[0] = anyHandler;
		for(int i = 0, length = handlerTypes.length; i < length; i ++){
			handlers[i + 1] = HandlerRegistry.getHandler(handlerTypes[i]);
			typeHandlers.add(handlers[i + 1]);
		}

		final JComboBox<RecordTypeHandler<?>> plainCombo = new JComboBox<>(handlers);
		//"Any" is offered first, but the first real type is the one selected at the start
		plainCombo.setSelectedIndex(1);
		final JComboBox<RecordTypeHandler<?>> combo = ComboBoxSearch.searchable(plainCombo);
		combo.setRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value,
					final int index, final boolean isSelected, final boolean cellHasFocus){
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				if(value instanceof RecordTypeHandler<?> handler){
					setText(handler.getLabel());
					setToolTipText(index >= 0 && handler != anyHandler? GUIHelper.usageTooltip(handler.getType()): null);
				}
				return this;
			}
		});
		combo.addActionListener(e -> {
			searchBox.field().setText(StringUtils.EMPTY);

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
			topPanel.add(new JLabel(I18N.l("search.type")));
			topPanel.add(typeCombo, "growx");
		}
		topPanel.add(new JLabel(I18N.l("search.search_text")));
		topPanel.add(searchBox, "growx");
		tagsField.setToolTipText(I18N.t("search.filter.tags.tooltip"));
		topPanel.add(tagsLabel, "hidemode 3");
		topPanel.add(tagsField, "growx,hidemode 3");

		// Search modifiers: all start off, which means "plain substring match". Switching one on narrows the search according to the
		// semantics documented on the class (the notes are not a way to match a token: they widen the text the query is looked for in).
		// Every change to an option reschedules the search.
		searchBox.onOptionChange(this::scheduleSearch);

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
		//the tooltip of a result tells where the text was found and how relevant the record is
		resultList.addMouseMotionListener(new MouseMotionAdapter(){
			@Override
			public void mouseMoved(final MouseEvent e){
				final int index = resultList.locationToIndex(e.getPoint());
				final boolean over = (index >= 0 && resultList.getCellBounds(index, index).contains(e.getPoint()));
				resultList.setToolTipText(over? listModel.get(index).explanation(): null);
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

		selectButton.addActionListener(e -> selectResult());
		editButton.addActionListener(e -> editRecord());
		unlinkButton.addActionListener(e -> unlinkRecord());
		deleteButton.addActionListener(e -> deleteRecord());

		final JPanel bottomPanel = new JPanel(new MigLayout("ins 0,fillx", "[grow 1,sg 1][grow 1,sg 1]", "[]"));

		// Left column (New button if allowed, otherwise empty panel)
		final JPanel leftPanel = new JPanel(new MigLayout("ins 0", "[]10[]"));
		if(allowCreation){
			createButton = new JButton(I18N.t("button.new"));
			createButton.addActionListener(e -> createNewRecord());
			leftPanel.add(createButton);
		}
		leftPanel.add(editButton);
		leftPanel.add(unlinkButton);
		leftPanel.add(deleteButton);
		bottomPanel.add(leftPanel, "left");

		// Right column (the Select button pushed to the right edge)
		final JPanel rightPanel = new JPanel(new MigLayout("ins 0", "[]10[]"));
		if(!manageMode)
			rightPanel.add(selectButton);
		bottomPanel.add(rightPanel, "right");

		add(bottomPanel, "growx");

		// Listeners
		final Document document = searchBox.field().getDocument();
		document.addDocumentListener(new DocumentListener(){
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

		tagsField.getDocument()
			.addDocumentListener(new DocumentListener(){
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
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
					if(manageMode)
						editRecord();
					else
						selectResult();
				}
			}
		});

		updateWindowTitle();
	}

	private void setupEscapeKey(){
		final JRootPane rootPane1 = getRootPane();
		final InputMap inputMap = rootPane1.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(GUIHelper.ESCAPE_STROKE, ACTION_CANCEL_SEARCH_OR_DIALOG);
		final ActionMap actionMap = rootPane1.getActionMap();
		actionMap.put(ACTION_CANCEL_SEARCH_OR_DIALOG, new AbstractAction(){
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

	/** Gives the dialog the title of the action that opened it (it replaces the one of the selected type). */
	public RecordSelectionDialog withTitle(final String title){
		fixedTitle = title;
		setTitle(title);

		return this;
	}

	private void updateWindowTitle(){
		if(fixedTitle != null){
			setTitle(fixedTitle);

			return;
		}

		final RecordTypeHandler<?> desc = getSelectedHandler();
		final String title;
		if(desc == anyHandler || desc instanceof IndividualHandler)
			title = I18N.t("search.title.select_record");
		else if(desc instanceof EventHandler)
			title = I18N.t("search.title.manage.events");
		else if(desc instanceof EventParticipationHandler)
			title = I18N.t("search.title.manage.participants");
		else if(desc instanceof ResearchQuestionHandler)
			title = I18N.t("search.title.manage.research_questions");
		else if(desc instanceof ResearchActivityHandler)
			title = I18N.t("search.title.manage.research_activities");
		else if(desc instanceof ResearchTaskHandler)
			title = I18N.t("search.title.manage.research_tasks");
		else if(desc instanceof ConclusionHandler)
			title = I18N.t("search.title.manage.conclusions");
		else if(desc instanceof IdentityHypothesisHandler)
			title = I18N.t("search.title.manage.identity_hypotheses");
		else if(desc instanceof PlaceRelationshipHandler)
			title = I18N.t("search.title.manage.places");
		else if(desc instanceof SourceHandler)
			title = I18N.t("search.title.manage.sources");
		else if(desc instanceof RepositoryHandler)
			title = I18N.t("search.title.manage.repositories");
		else if(desc instanceof MediaHandler)
			title = I18N.t("search.title.manage.media");
		else
			title = selectionTitle(desc);
		setTitle(title);
	}

	/**
	 * @param handler	The handler of the type the user is choosing from (none if the type is not chosen yet).
	 * @return	"Select Place", "Select Group"..., for the types that have no title of their own: never a placeholder.
	 */
	static String selectionTitle(final RecordTypeHandler<?> handler){
		return (handler != null? I18N.tf("search.title.select_type", handler.getLabel()): I18N.t("search.title.select_record"));
	}

	/** @return	Whether the search covers all the types offered by the dialog. */
	private boolean isAnyHandler(){
		return (anyHandler == getSelectedHandler());
	}

	/** Selects the type "Any" (the search covers all the types), if the dialog offers more than one. */
	public RecordSelectionDialog withAnyTypeSelected(){
		if(typeCombo != null)
			typeCombo.setSelectedItem(anyHandler);

		return this;
	}

	/** @return	The handler of the type of a result: its own if the search covers several types, otherwise the selected one. */
	private RecordTypeHandler<?> handlerOf(final DisplayItem item){
		return (item.handler() != null? item.handler(): getSelectedHandler());
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
	 * Returns the search mode implied by the current combination of the independent options.
	 *
	 * @return the effective search mode
	 */
	private Set<SearchMode> currentMode(){
		return searchBox.modes();
	}

	private Set<SearchMode> searchModes(final boolean fuzzyRetry){
		if(!fuzzyRetry)
			return currentMode();

		final Set<SearchMode> modes = EnumSet.noneOf(SearchMode.class);
		modes.addAll(currentMode());
		modes.add(SearchMode.FUZZY);
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
		//no record can be created without knowing its type
		if(createButton != null)
			createButton.setEnabled(!isAnyHandler());

		if(isPlaceRelationship)
			applyFiltersToHierarchy();

		pack();
	}

	private void showHierarchyCard(){
		// The hierarchy panel is created lazily on the first switch to
		// the place_relationship type. Once created, it stays in the
		// card layout and is merely reloaded on subsequent switches.
		if(placeHierarchyPanel == null){
			placeHierarchyPanel = SelectionViewProvider.find().createPlaceHierarchyView(model);
			placeHierarchyPanel.setEditable(true);

			final JPanel wrapper = new JPanel(new BorderLayout());
			wrapper.setBorder(BorderFactory.createTitledBorder(I18N.t("search.results_panel.title")));
			wrapper.add(placeHierarchyPanel.component(), BorderLayout.CENTER);

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
		searchBox.setVisible(true);
		filterPanelHolder.setVisible(true);
	}

	private void rebuildDynamicFilterPanel(final RecordTypeHandler<?> handler){
		//the tags can be asked for only on the records that can have them
		final boolean taggable = (handler != null && RecordTags.isTaggable(handler.getType()));
		tagsLabel.setVisible(taggable);
		tagsField.setVisible(taggable);
		if(!taggable)
			tagsField.setText(StringUtils.EMPTY);

		filterPanelHolder.removeAll();
		if(handler != null){
			final JPanel panel = FilterPanelFactory.createPanel(handler, criteria -> scheduleSearch());
			if(panel instanceof IndividualFilterPanel individualPanel && lockedSex != null)
				individualPanel.lockSex(lockedSex);
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
		search(false);
	}

	/**
	 * @param fuzzyRetry	{@code true} if this is the second try of a search that found nothing: the typos are tolerated, whatever the
	 * 	option says.
	 */
	private void search(final boolean fuzzyRetry){
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

		//the model may have changed since the last search: the shared indexes of the event description are read again
		EventSummary.invalidate();
		ResearchSummary.invalidate();
		SourceUsage.invalidate();

		final RecordTypeHandler<?> handler = getSelectedHandler();
		final boolean any = isAnyHandler();
		if(any && StringUtils.isBlank(searchBox.query())){
			//all the records of all the types are too many to be of any use: a text is needed
			listModel.clear();
			resultList.setEnabled(false);
			progressBar.setVisible(false);
			statusLabel.setText(I18N.t("search.any.needs_text"));

			return;
		}

		//the types to search, each with its criteria (the filters of the panel are the ones of a single type)
		final List<RecordTypeHandler<?>> searchedHandlers = (any? typeHandlers: List.of(handler));
		final List<SearchCriteria> searchedCriteria = new ArrayList<>(searchedHandlers.size());
		for(final RecordTypeHandler<?> searchedHandler : searchedHandlers){
			final SearchCriteria searchCriteria = new SearchCriteria(searchedHandler.getType(), searchBox.query(), searchModes(fuzzyRetry));

			initialFilters.forEach(searchCriteria::withFilter);
			if(tagsField.isVisible() && StringUtils.isNotBlank(tagsField.getText()))
				searchCriteria.withFilter(SearchCriteria.FILTER_TAGS, tagsField.getText());

			if(!any && filterPanelHolder.getComponentCount() > 0
					&& filterPanelHolder.getComponent(0) instanceof RecordFilterPanel filterPanel)
				filterPanel.getFilters()
					.forEach(searchCriteria::withFilter);
			searchedCriteria.add(searchCriteria);
		}

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
				final int types = searchedCriteria.size();
				final List<SearchService.SearchHit> records = new ArrayList<>();
				final Map<FLEFRecord, RecordTypeHandler<?>> handlersByRecord = new HashMap<>();
				for(int t = 0; t < types; t ++){
					final int base = t * 50 / types;
					final int span = Math.max(1, 50 / types);
					final List<SearchService.SearchHit> typeHits = searchService.searchExplained(searchedCriteria.get(t),
						progress -> publish(base + progress * span / 100));
					if(isCancelled())
						return null;

					records.addAll(typeHits);
					for(final SearchService.SearchHit hit : typeHits)
						handlersByRecord.put(hit.record(), searchedHandlers.get(t));
				}
				if(types > 1)
					//a record that matches more words of the query always comes first (as inside a single type); the relevances, that come
					//from different types, only break the ties, then the text
					records.sort(Comparator.comparingInt(SearchService.SearchHit::matchedTerms).reversed()
						.thenComparing(Comparator.comparingDouble(SearchService.SearchHit::relevance).reversed())
						.thenComparing(hit -> displayTextCache.computeIfAbsent(hit.record(), searchService::getDisplayText),
							String.CASE_INSENSITIVE_ORDER));

				// Phase 2: Compute or fetch cached display texts in background
				// (50% - 100% of progress). When several types are searched, each result
				// says what it is: "[Group] Trevisan and Masutti family ...".
				final List<DisplayItem> items = new ArrayList<>(records.size());
				final int total = records.size();
				for(int i = 0; i < total; i ++){
					if(isCancelled())
						return null;

					final SearchService.SearchHit hit = records.get(i);
					final FLEFRecord record = hit.record();
					final String text = displayTextCache.computeIfAbsent(record, searchService::getDisplayText);
					if(types > 1){
						final RecordTypeHandler<?> recordHandler = handlersByRecord.get(record);
						items.add(new DisplayItem(record, "[" + recordHandler.getLabel() + "] " + text, hit.explanation(), recordHandler));
					}
					else
						items.add(new DisplayItem(record, text, hit.explanation(), null));

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

				boolean retrying = false;
				try{
					final List<DisplayItem> items = get();
					if(items != null){
						//a typo is not a dead end: with nothing found and the Fuzzy option off, the search is repeated tolerating the typos
						if(items.isEmpty() && !fuzzyRetry && StringUtils.isNotBlank(searchBox.query())
								&& !currentMode().contains(SearchMode.FUZZY)){
							retrying = true;
							search(true);

							return;
						}

						updateResults(items);
						if(fuzzyRetry && !items.isEmpty())
							statusLabel.setText(I18N.tf("search.fuzzy_fallback", items.size()));
					}
				}
				catch(final Exception e){
					statusLabel.setText(I18N.t("search.failed"));

					LOGGER.error("Error during search", e);

					listModel.clear();
					resultList.setEnabled(false);
				}
				finally{
					if(!retrying)
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

		if(placeHierarchyPanel.component().getPropertyChangeListeners(
			PlaceHierarchyView.PROPERTY_FILTERING_PROGRESS).length == 0){
			placeHierarchyPanel.component().addPropertyChangeListener(
				PlaceHierarchyView.PROPERTY_FILTERING_PROGRESS, evt -> {
					final Integer progress = (Integer)evt.getNewValue();
					if(progress != null){
						progressBar.setValue(progress);
						progressBar.repaint();
					}
				});
		}
		if(placeHierarchyPanel.component().getPropertyChangeListeners(PlaceHierarchyView.PROPERTY_FILTERING_DONE).length == 0)
			placeHierarchyPanel.component().addPropertyChangeListener(
				PlaceHierarchyView.PROPERTY_FILTERING_DONE, evt -> {
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
			currentMode(), searchBox.query(), relationType);

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
			listModel.addElement(new DisplayItem(null, NO_MATCHING_RECORDS, null, null));

			resultList.setEnabled(false);
		}
		else{
			listModel.addAll(items);

			resultList.setEnabled(true);
		}

		statusLabel.setText(I18N.tf("search.record_found", items.size()));
	}

	private void createNewRecord(){
		if(isAnyHandler())
			return;

		final RecordTypeHandler<?> handler = getSelectedHandler();
		final FLEFRecord created;
		if(handler instanceof PlaceHandler){
			//a place is typed as a line, with the places that contain it (the whole editor is behind More...)
			final PlaceChainDialog.Result result = PlaceChainDialog.show(this, model, null);
			if(result == null)
				return;

			created = model.getRecordById(result.placeId());
			if(created != null && result.openEditor())
				handler.createEditDialog(this, model, created)
					.setVisible(true);
		}
		else{
			final RecordDialog dialog = handler.createNewDialog(this, model);
			if(setupDialog != null)
				setupDialog.accept(dialog);
			dialog.setVisible(true);
			created = (dialog.isSaved()? dialog.getRecord(): null);
		}

		if(created == null)
			return;

		if(manageMode){
			//nothing to pick: the new record is in the list, the dialog stays open
			if(isPlaceRelationshipHandler())
				placeHierarchyPanel.reload();
			else
				performSearch();

			return;
		}

		selectedType = handler.getType();
		selectedRecord = created;
		confirmed = true;

		if(onSelect != null)
			onSelect.accept(created, handler);

		firePropertyChange(PROPERTY_TYPE_SELECTED, null, null);

		dispose();
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
				MessageDialogs.show(this,
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
			final RecordTypeHandler<?> handler = handlerOf(selectedItem);
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
			MessageDialogs.show(this,
				I18N.t("search.no_selection.message"),
				I18N.t("search.no_selection.title"), JOptionPane.INFORMATION_MESSAGE);
	}

	private void editRecord(){
		if(isPlaceRelationshipHandler()){
			placeHierarchyPanel.openEditorForSelection();

			return;
		}

		final DisplayItem selectedItem = resultList.getSelectedValue();
		final RecordTypeHandler<?> handler = (selectedItem != null? handlerOf(selectedItem): getSelectedHandler());
		if(handler != null){
			if(selectedItem == null || selectedItem.record() == null)
				return;

			final FLEFRecord record = selectedItem.record();
			final RecordDialog dialog = handler.createEditDialog(this, model, record);
			dialog.setVisible(true);

			if(dialog.isSaved()){
				// Invalidate cache for this record and refresh results
				displayTextCache.remove(record);

				performSearch();
			}
		}
	}

	private void deleteRecord(){
		final FLEFRecord record;
		final String displayText;

		// 1. Retrieve the selected record and its display text based on the current mode
		if(isPlaceRelationshipHandler()){
			record = placeHierarchyPanel.getSelectedPlaceRecord();
			displayText = searchService.getDisplayText(record);
			if(record == null)
				return;
		}
		else {
			final DisplayItem selectedItem = resultList.getSelectedValue();
			if(selectedItem == null || selectedItem.record() == null)
				return;

			record = selectedItem.record();
			displayText = selectedItem.displayText();
		}

		// 2. Ask for confirmation, saying who uses the record when somebody does
		if(!confirmDeletion(record, displayText))
			return;

		// 3. Remove the target record from the model (a source takes its extracts with it, and the references to them)
		deleteRecord(record);

		// 4. Clean cache and refresh the appropriate view
		displayTextCache.remove(record);
		if(isPlaceRelationshipHandler())
			placeHierarchyPanel.reload();
		else
			performSearch();
	}

	/**
	 * Asks whether to delete the record. A record that nobody uses is asked as a plain question; for one that is used, the question names
	 * the records that point to it and says that the references go with it, and the user can open the list of where it is used first.
	 *
	 * @return	{@code true} if the record is to be deleted.
	 */
	private boolean confirmDeletion(final FLEFRecord record, final String displayText){
		final String type = I18N.t("confirmation.exist_record." + record.getTag());
		final RecordUsageViewer views = RecordUsageViewer.find();
		while(true){
			final List<RecordReferences.Reference> uses = RecordReferences.to(model, record.getId());
			if(uses.isEmpty())
				return MessageDialogs.confirm(this, I18N.tf("confirmation.delete.record.message", false, type, displayText),
					I18N.t("confirmation.delete.record.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
					== JOptionPane.YES_OPTION;

			final StringBuilder owners = new StringBuilder();
			for(int i = 0; i < Math.min(uses.size(), MAX_USES_LISTED); i ++)
				owners.append("- ").append(views.describe(model, uses.get(i).owner())).append(StringUtils.LF);
			if(uses.size() > MAX_USES_LISTED)
				owners.append(I18N.tf("confirmation.delete.record.used.more", uses.size() - MAX_USES_LISTED)).append(StringUtils.LF);
			final String deleteAnyway = I18N.t("confirmation.delete.record.used.delete");
			final String whereUsed = I18N.t("confirmation.delete.record.used.where");
			final int choice = MessageDialogs.option(this,
				I18N.tf("confirmation.delete.record.used.message", uses.size(), type, displayText, owners.toString()),
				I18N.t("confirmation.delete.record.title"), JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
				new Object[]{deleteAnyway, whereUsed}, whereUsed);
			if(choice == 0)
				return true;
			if(choice != 1)
				return false;

			//the list is shown, the records may be edited from it: the question is asked again with what is left
			views.show(this, model, record);
		}
	}

	/**
	 * Removes a record from the model following the deletion rule of the protocol: the references to the deleted record are removed
	 * (the entries of the lists, the optional fields), and the records that cannot exist without it are deleted with it (the extracts
	 * of a source, with the references to them).
	 */
	private void deleteRecord(final FLEFRecord record){
		final FLEFGrammar grammar = MergeGrammar.instance();
		if(grammar != null)
			new RecordDeleter(grammar).delete(model, record.getId());
		else
			model.removeRecord(record.getId());
	}

	private void unlinkRecord(){
		final FLEFRecord record;
		final String displayText;

		// 1. Retrieve the selected record and its display text based on the current mode
		if(isPlaceRelationshipHandler()){
			record = placeHierarchyPanel.getSelectedPlaceRecord();
			displayText = searchService.getDisplayText(record);
			if(record == null)
				return;
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
		final UnlinkPrompt dialog = SelectionViewProvider.find().createUnlinkPrompt(this, model, recordId);
		dialog.setVisible(true);

		final List<String> linkIdsToRemove = dialog.getSelectedLinkIds();
		if(linkIdsToRemove.isEmpty())
			return;

		// 3. Prompt confirmation before deleting the record itself
		final int confirm = MessageDialogs.confirm(this,
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

	@Override
	public void dispose(){
		//no search is left running, or scheduled, for a dialog that is gone
		searchDebouncer.terminate();
		if(currentWorker != null)
			currentWorker.cancel(true);

		super.dispose();
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
