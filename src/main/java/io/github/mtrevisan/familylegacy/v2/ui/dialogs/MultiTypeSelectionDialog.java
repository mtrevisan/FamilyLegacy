package io.github.mtrevisan.familylegacy.v2.ui.dialogs;

import io.github.mtrevisan.familylegacy.v2.io.FLEFParser;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.biologicaltree.BiologicalTreePanel;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.Debouncer;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.GUIHelper;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * Generic dialog for selecting a record from multiple types (e.g., Individual or Group).
 * Supports diacritic-insensitive search, fuzzy matching (trigram similarity),
 * and background filtering.
 */
public class MultiTypeSelectionDialog extends JDialog{

	@Serial
	private static final long serialVersionUID = -6835967045890180368L;


	private static final int DEBOUNCE_TIME = 400;
	private static final double TRIGRAM_SIMILARITY_THRESHOLD = 0.05;

	public static final String PROPERTY_TYPE_SELECTED = "type-selected";

	private static final Dimension SCROLL_PANE_PREFERRED_SIZE = new Dimension(400, 200);


	private final FLEFModel model;
	private final RecordTypeHandler<?> defaultType;
	private final Function<FLEFRecord, Boolean> fnFilter;
	private Consumer<BaseRecordDialog> setupDialog;

	private final JComboBox<RecordTypeHandler<?>> typeCombo;
	private final JTextField searchField;
	private final JCheckBox fuzzyCheckBox = new JCheckBox("Fuzzy", false);
	private final JCheckBox wholeWordCheckBox = new JCheckBox("Whole word", false);
	private final DefaultListModel<String> listModel = new DefaultListModel<>();
	private final JList<String> list = GUIHelper.createList(listModel);
	private final JLabel statusLabel = new JLabel(" ");

	private List<FLEFRecord> allRecords = new ArrayList<>();
	private List<FLEFRecord> filteredRecords = new ArrayList<>();
	private final Debouncer<String> searchDebouncer = new Debouncer<>(key -> startFiltering(), DEBOUNCE_TIME);

	private boolean confirmed;
	private String selectedType;
	private FLEFRecord selectedRecord;
	private FilterWorker currentWorker;


	@SafeVarargs
	public MultiTypeSelectionDialog(final Dialog parent, final FLEFModel model,
			final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		this(parent, model, null, handlerTypes);
	}

	@SafeVarargs
	public MultiTypeSelectionDialog(final Dialog parent, final FLEFModel model,
			final Function<FLEFRecord, Boolean> fnFilter, final Class<? extends RecordTypeHandler<?>>... handlerTypes){
		super(parent, "Select Record", ModalityType.APPLICATION_MODAL);

		this.model = model;
		this.defaultType = (handlerTypes.length == 1? HandlerRegistry.getHandler(handlerTypes[0]): null);
		this.fnFilter = fnFilter;

		typeCombo = (handlerTypes.length > 1? createTypeCombo(handlerTypes): null);
		searchField = new JTextField();
		list.setFixedCellHeight(20);

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
		final RecordTypeHandler<?>[] handlers = new RecordTypeHandler[handlerTypes.length];
		for(int i = 0; i < handlerTypes.length; i ++)
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
			loadRecordsForType(getSelectedHandler());

			searchField.setText(StringUtils.EMPTY);
		});
		return combo;
	}

	private void initComponents(){
		setLayout(new MigLayout("ins 10,fill", "[grow,fill]", "[][][grow][]"));

		// Top panel: type (if multiple) + search + fuzzy + whole word checkboxes
		final JPanel topPanel = new JPanel(new MigLayout("wrap 2", "[][grow,fill]", "[]"));
		if(typeCombo != null){
			topPanel.add(new JLabel("Type:"));
			topPanel.add(typeCombo, "growx");
		}
		topPanel.add(new JLabel("Search:"));
		topPanel.add(searchField, "growx");
		topPanel.add(fuzzyCheckBox, "span 2,left");
		topPanel.add(wholeWordCheckBox, "span 2,left");

		add(topPanel, "growx,wrap");

		// List with scroll
		final JScrollPane scrollPane = GUIHelper.createScrollPane(list);
		scrollPane.setBorder(BorderFactory.createTitledBorder("Records"));
		scrollPane.setPreferredSize(SCROLL_PANE_PREFERRED_SIZE);
		add(scrollPane, "grow,push,wrap");

		// Status label (shows loading message or record count)
		add(statusLabel, "growx,wrap");

		// Bottom buttons
		final JPanel bottomPanel = GUIHelper.createNewSelectCancelButtonPanel(
			getRootPane(),
			this::createNewRecord,
			this::selectAndClose,
			this::dispose
		);
		add(bottomPanel, "growx");

		updateWindowTitle();

		// Search debouncer
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				searchDebouncer.call("search");
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				searchDebouncer.call("search");
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				searchDebouncer.call("search");
			}
		});

		// Fuzzy checkbox triggers new search
		fuzzyCheckBox.addActionListener(e -> startFiltering());
		wholeWordCheckBox.addActionListener(e -> {
			// If whole word is selected, disable fuzzy (mutually exclusive)
			if(wholeWordCheckBox.isSelected())
				fuzzyCheckBox.setSelected(false);
			startFiltering();
		});
		// If fuzzy is selected, disable whole word
		fuzzyCheckBox.addActionListener(e -> {
			if(fuzzyCheckBox.isSelected())
				wholeWordCheckBox.setSelected(false);
		});

		// Double-click on list
		list.addMouseListener(new MouseAdapter(){
			@Override
			public void mousePressed(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
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

	// --- Data loading ---

	private void loadRecordsForType(final RecordTypeHandler<?> desc){
		if(desc == null)
			return;

		updateWindowTitle();

		allRecords = model.getRecordsByType(desc.getType());

		startFiltering();
	}

	// --- Filtering (background) ---

	private void startFiltering(){
		// Show loading state
		statusLabel.setText("Loading…");
		list.setEnabled(false);

		if(currentWorker != null && !currentWorker.isDone())
			currentWorker.cancel(true);
		currentWorker = new FilterWorker();
		currentWorker.execute();
	}

	private class FilterWorker extends SwingWorker<List<FLEFRecord>, Void>{
		private final String searchText;
		private final boolean fuzzy;
		private final boolean wholeWord;
		private final RecordTypeHandler<?> handler;

		FilterWorker(){
			this.searchText = searchField.getText().trim();
			this.fuzzy = fuzzyCheckBox.isSelected();
			this.wholeWord = wholeWordCheckBox.isSelected();
			this.handler = getSelectedHandler();
		}

		@Override
		protected List<FLEFRecord> doInBackground(){
			if(handler == null)
				return Collections.emptyList();

			final String normalizedSearch = normalize(searchText);
			final boolean searchEmpty = normalizedSearch.isEmpty();
			final Set<String> searchTrigrams = (searchEmpty? Collections.emptySet(): getTrigrams(normalizedSearch));
			// Pre-compile whole-word pattern if needed
			final Pattern wholeWordPattern = (!searchEmpty && wholeWord
				? Pattern.compile("\\b" + Pattern.quote(normalizedSearch) + "\\b")
				: null);
			final List<FLEFRecord> result = new ArrayList<>();

			for(final FLEFRecord record : allRecords){
				if(Thread.currentThread().isInterrupted())
					break;

				final String display = handler.getDisplayText(record, model);
				final String normalizedDisplay = normalize(display);

				boolean matches;
				if(searchEmpty)
					// Empty search → show all records (respect filter)
					matches = true;
				else if(wholeWord)
					// Whole word match using regex
					matches = wholeWordPattern.matcher(normalizedDisplay)
						.find();
				else if(fuzzy){
					final Set<String> displayTrigrams = getTrigrams(normalizedDisplay);
					final double similarity = jaccardSimilarity(searchTrigrams, displayTrigrams);
					matches = similarity >= TRIGRAM_SIMILARITY_THRESHOLD;
				}
				else
					// Simple contains (case-insensitive, diacritics removed)
					matches = normalizedDisplay.contains(normalizedSearch);

				if(matches && (fnFilter == null || fnFilter.apply(record)))
					result.add(record);
			}

			// Sort by display text
			result.sort((a, b) -> {
				final String na = handler.getDisplayText(a, model);
				final String nb = handler.getDisplayText(b, model);
				return na.compareToIgnoreCase(nb);
			});

			return result;
		}

		@Override
		protected void done(){
			try{
				filteredRecords = get();

				updateList();

				statusLabel.setText("Found " + filteredRecords.size() + " records");

				list.setEnabled(!filteredRecords.isEmpty());
			}
			catch(final Exception e){
				statusLabel.setText("Error during filtering");

				filteredRecords = Collections.emptyList();

				updateList();

				list.setEnabled(false);
			}
			currentWorker = null;
		}
	}

	// --- Normalization (diacritics + synonyms) ---

	private static String normalize(final String text){
		if(StringUtils.isEmpty(text))
			return text;

		// Remove diacritics
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
		normalized = normalized.replaceAll("\\p{M}", StringUtils.EMPTY);

		// Synonyms: expand abbreviations
		normalized = normalized.replaceAll("\\bSt\\b", "Street");
		normalized = normalized.replaceAll("\\bAve\\b", "Avenue");
		// Add more as needed.

		return normalized.toLowerCase();
	}

	// --- Trigram helpers ---

	private static Set<String> getTrigrams(final String text){
		final Set<String> trigrams = new HashSet<>();
		final String padded = "  " + text + "  ";
		for(int i = 0; i < padded.length() - 2; i ++)
			trigrams.add(padded.substring(i, i + 3));
		return trigrams;
	}

	private static double jaccardSimilarity(final Set<String> set1, final Set<String> set2){
		if(set1.isEmpty() && set2.isEmpty())
			return 1.;

		final Set<String> intersection = new HashSet<>(set1);
		intersection.retainAll(set2);
		final Set<String> union = new HashSet<>(set1);
		union.addAll(set2);
		return (double)intersection.size() / union.size();
	}

	// --- UI update ---

	private void updateList(){
		listModel.clear();

		if(filteredRecords.isEmpty()){
			listModel.addElement("[No matching records]");
			list.setEnabled(false);
		}
		else{
			list.setEnabled(true);
			final RecordTypeHandler<?> handler = getSelectedHandler();
			for(final FLEFRecord record : filteredRecords)
				listModel.addElement(handler.getDisplayText(record, model));
		}
	}

	// --- Actions ---

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
				// Reload and select the new record
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
	}

	private void selectAndClose(){
		final int idx = list.getSelectedIndex();
		if(idx >= 0 && idx < filteredRecords.size()){
			final RecordTypeHandler<?> handler = getSelectedHandler();
			if(handler != null){
				selectedType = handler.getType();
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

	// --- Getters ---

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
			final MultiTypeSelectionDialog dialog = new MultiTypeSelectionDialog(null, model,
				EventParticipationHandler.class);
			dialog.setVisible(true);
		});
	}

}