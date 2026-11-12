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
package io.github.mtrevisan.familylegacy.ui.tools.sources;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.AttachedTextExtractor;
import io.github.mtrevisan.familylegacy.search.SearchMatcher;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.helpers.SavedFileNotice;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Function;


/**
 * Shows how the sources and the documents are used in the file, one tab for each topic:
 * <ul>
 *   <li><b>Unused sources</b>: sources that no record cites;</li>
 *   <li><b>Unused documents</b>: documents that no source or extract references;</li>
 *   <li><b>Uncited assertions</b>: the records of the analysed types that carry no citation. They can be tens of thousands, so the
 *   tab opens on a summary by type (how many are uncited, out of how many); a double-click opens the list of one type (or of all of
 *   them), that can be filtered by text.</li>
 * </ul>
 * The rows can be opened (double-click, or "Open record") to fix the problem on the spot; the analysis is made again afterwards, so
 * the lists are what the file is now. Every tab can be exported to a CSV file.
 */
final class CitationAnalysisDialog extends JDialog{

	private static final int UNCITED_TAB = 2;
	private static final String CARD_SUMMARY = "summary";
	private static final String CARD_LIST = "list";
	/** The key of the list of all the types, in the summary. */
	private static final String ALL_TYPES = StringUtils.EMPTY;


	private final FLEFModel model;

	private final JTabbedPane tabs = new JTabbedPane();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JButton exportCsv = new JButton(I18N.t("button.export_csv"));
	private final JLabel summary = new JLabel(StringUtils.SPACE);
	private final List<JTable> tables = new ArrayList<>();
	/** The uncited list of the types chosen in the summary; the tab shows the summary until a type is chosen. */
	private final JTable uncitedTable = new JTable();
	//the same search as the record selection dialog: word by word, whole words, fuzzy, notes and citations
	private final SearchBox searchBox = new SearchBox(24, true);
	private final JLabel listTitle = new JLabel();

	private JPanel uncitedCards;
	/** What the list of the uncited shows before the search text is applied (the records of the chosen type). */
	private RecordTableModel listBase;
	private SwingWorker<List<FLEFRecord>, Void> searchWorker;
	/** All the uncited records, for the export of the summary. */
	private RecordTableModel allUncited;
	/** What the uncited tab shows: {@code null} the summary, {@link #ALL_TYPES} every type, else the type. */
	private String listedType;


	CitationAnalysisDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.citation_analysis.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;

		tabs.setPreferredSize(new Dimension(820, 520));
		tabs.addChangeListener(e -> updateButtons());

		searchBox.onChange(this::applyFilter);
		configureRecordTable(uncitedTable);

		openRecord.addActionListener(e -> openSelected());
		exportCsv.addActionListener(e -> exportToCsv());
		final JButton copy = new JButton(I18N.t("button.copy_all"));
		copy.addActionListener(e -> copyToClipboard());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(openRecord);
		buttons.add(exportCsv);
		buttons.add(copy);

		final JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 6));
		footer.add(summary, BorderLayout.WEST);
		footer.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		reload();

		pack();
		setLocationRelativeTo(owner);
	}


	/** Reads the model again and builds the tabs, keeping the one in view (and the list of the uncited, if it is open). */
	private void reload(){
		//a search still running would show the records of the previous analysis
		if(searchWorker != null && !searchWorker.isDone())
			searchWorker.cancel(true);

		final CitationAnalysisTool.Analysis analysis = CitationAnalysisTool.analyze(model);

		final int selectedTab = Math.max(tabs.getSelectedIndex(), 0);
		tabs.removeAll();
		tables.clear();

		final List<FLEFRecord> unusedSources = analysis.unusedSources();
		final List<FLEFRecord> unusedDocuments = analysis.unusedDocuments();
		final List<FLEFRecord> uncited = analysis.uncited();
		final JTable sourcesTable = createRecordTable(unusedSources, I18N.t("column.id"), I18N.t("column.title"), I18N.t("column.author"),
			SourceHelper::sourceTitle, SourceHelper::sourceAuthor);
		final JTable documentsTable = createRecordTable(unusedDocuments, I18N.t("column.id"), I18N.t("column.uri"),
			I18N.t("column.description"), SourceHelper::documentDisplayUri, SourceHelper::documentDescription);
		allUncited = newUncitedModel(uncited);
		tables.add(sourcesTable);
		tables.add(documentsTable);
		tables.add(uncitedTable);

		tabs.addTab(tabTitle("tab.title.unused_sources", unusedSources.size()),
			ToolDialogs.createVerdictTab(unusedSources.isEmpty(), I18N.t("text.citation_analysis.sources.ok"),
				I18N.tf("text.citation_analysis.sources.warn", unusedSources.size()), sourcesTable));
		tabs.addTab(tabTitle("tab.title.unused_documents", unusedDocuments.size()),
			ToolDialogs.createVerdictTab(unusedDocuments.isEmpty(), I18N.t("text.citation_analysis.documents.ok"),
				I18N.tf("text.citation_analysis.documents.warn", unusedDocuments.size()), documentsTable));
		tabs.addTab(tabTitle("tab.title.uncited_assertions", uncited.size()), createUncitedTab(analysis));
		tabs.setSelectedIndex(Math.min(selectedTab, tabs.getTabCount() - 1));

		summary.setText(GUIHelper.format(I18N.t("text.citation_analysis.summary"), analysis.citedSourceCount(),
			analysis.referencedDocumentCount()));
		updateButtons();
	}

	private static String tabTitle(final String key, final int count){
		return I18N.t(key) + " (" + count + ")";
	}

	/** The type of a record as the user reads it ("individual attribute"), not as it is written in the file. */
	private static String typeLabel(final FLEFRecord record){
		return typeLabel(record.getTag());
	}

	private static String typeLabel(final String tag){
		return I18N.t("confirmation.exist_record." + tag.toLowerCase(Locale.ROOT), tag);
	}


	/* ======================================================================
	 *                          Uncited assertions
	 * ====================================================================== */

	private RecordTableModel newUncitedModel(final List<FLEFRecord> records){
		return new RecordTableModel(records, new String[]{I18N.t("column.id"), I18N.t("column.record_type"), I18N.t("column.record")},
			CitationAnalysisDialog::typeLabel, record -> RecordDisplay.describe(record, model));
	}

	/** The summary by type and, behind it, the list of the records of one type. */
	private JPanel createUncitedTab(final CitationAnalysisTool.Analysis analysis){
		final List<FLEFRecord> uncited = analysis.uncited();
		final JPanel tab = ToolDialogs.createVerdictPanel(uncited.isEmpty(), (uncited.isEmpty()
			? I18N.t("text.citation_analysis.assertions.ok")
			: I18N.tf("text.citation_analysis.assertions.warn", uncited.size())));
		if(uncited.isEmpty()){
			uncitedCards = null;
			listedType = null;
			return tab;
		}

		uncitedCards = new JPanel(new CardLayout());
		uncitedCards.add(createSummaryCard(analysis), CARD_SUMMARY);
		uncitedCards.add(createListCard(), CARD_LIST);
		tab.add(uncitedCards, BorderLayout.CENTER);

		//what was open stays open after an edit
		if(listedType != null && (ALL_TYPES.equals(listedType) || analysis.assertionTotals().containsKey(listedType)))
			showList(listedType);
		else
			showSummary();
		return tab;
	}

	private JScrollPane createSummaryCard(final CitationAnalysisTool.Analysis analysis){
		final List<String[]> rows = new ArrayList<>();
		final List<String> keys = new ArrayList<>();
		rows.add(summaryRow(I18N.t("text.citation_analysis.all_types"), analysis.uncited().size(),
			analysis.assertionTotals().values().stream().mapToInt(Integer::intValue).sum()));
		keys.add(ALL_TYPES);
		for(final Map.Entry<String, Integer> entry : analysis.assertionTotals().entrySet()){
			final long count = analysis.uncited().stream()
				.filter(record -> entry.getKey().equalsIgnoreCase(record.getTag()))
				.count();
			rows.add(summaryRow(typeLabel(entry.getKey()), (int)count, entry.getValue()));
			keys.add(entry.getKey());
		}

		final String[] columns = {I18N.t("column.record_type"), I18N.t("column.uncited"), I18N.t("column.total"),
			I18N.t("column.coverage")};
		final JTable table = new JTable(new AbstractTableModel(){
			@Override
			public int getRowCount(){
				return rows.size();
			}

			@Override
			public int getColumnCount(){
				return columns.length;
			}

			@Override
			public String getColumnName(final int column){
				return columns[column];
			}

			@Override
			public Object getValueAt(final int row, final int column){
				return rows.get(row)[column];
			}
		});
		table.setRowHeight(24);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setToolTipText(I18N.t("tooltip.citation_analysis.summary"));
		final DefaultTableCellRenderer right = new DefaultTableCellRenderer();
		right.setHorizontalAlignment(SwingConstants.RIGHT);
		for(int column = 1; column < columns.length; column ++)
			table.getColumnModel().getColumn(column).setCellRenderer(right);
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final int row = table.rowAtPoint(e.getPoint());
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && row >= 0)
					showList(keys.get(row));
			}
		});
		return new JScrollPane(table);
	}

	/** @return	The columns of a row of the summary: the type, how many are uncited, how many there are, and the share that is cited. */
	private static String[] summaryRow(final String label, final int uncited, final int total){
		final String coverage = (total > 0? String.format(Locale.ROOT, "%.1f%%", 100. * (total - uncited) / total): "-");
		return new String[]{label, String.valueOf(uncited), String.valueOf(total), coverage};
	}

	private JPanel createListCard(){
		final JButton back = new JButton(I18N.t("button.back"));
		back.addActionListener(e -> showSummary());

		final JPanel bar = new JPanel(new MigLayout("ins 4 6 4 6,fillx", "[][][][grow,fill]", "[]"));
		bar.add(back);
		bar.add(listTitle);
		bar.add(new JLabel(I18N.l("label.search")));
		bar.add(searchBox, "growx");

		final JPanel card = new JPanel(new BorderLayout());
		card.add(bar, BorderLayout.NORTH);
		card.add(new JScrollPane(uncitedTable), BorderLayout.CENTER);
		return card;
	}

	private void showSummary(){
		listedType = null;
		((CardLayout)uncitedCards.getLayout()).show(uncitedCards, CARD_SUMMARY);
		updateButtons();
	}

	/** Shows the uncited records of a type (all the types for {@link #ALL_TYPES}); the rows are written when the table asks for them. */
	private void showList(final String type){
		listedType = type;

		final List<FLEFRecord> records = new ArrayList<>();
		for(int i = 0, count = allUncited.getRowCount(); i < count; i ++){
			final FLEFRecord record = allUncited.recordAt(i);
			if(ALL_TYPES.equals(type) || type.equalsIgnoreCase(record.getTag()))
				records.add(record);
		}
		listBase = (ALL_TYPES.equals(type)? allUncited: newUncitedModel(records));
		applyFilter();
		((CardLayout)uncitedCards.getLayout()).show(uncitedCards, CARD_LIST);
		updateButtons();
	}

	/**
	 * Shows the records of the list that match the search text, as the record selection dialog does: the same word-level matcher and
	 * the same modes (whole word, fuzzy, notes and citations), and the best matches first. With no text, the whole list. The matching
	 * runs away from the event dispatch thread, because the display text of tens of thousands of records has to be read.
	 */
	private void applyFilter(){
		if(listBase == null)
			return;

		if(searchWorker != null && !searchWorker.isDone())
			searchWorker.cancel(true);

		final String query = searchBox.query();
		if(query.isEmpty()){
			setListModel(listBase);
			return;
		}

		final Set<SearchMode> modes = searchBox.modes();
		final List<FLEFRecord> candidates = listBase.records();
		listTitle.setText(I18N.t("search.searching"));
		searchWorker = new SwingWorker<>(){
			@Override
			protected List<FLEFRecord> doInBackground(){
				return search(candidates, query, modes, this::isCancelled);
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				try{
					setListModel(newUncitedModel(get()));
				}
				catch(final Exception ignored){
					setListModel(listBase);
				}
			}
		};
		searchWorker.execute();
	}

	private List<FLEFRecord> search(final List<FLEFRecord> records, final String query, final Set<SearchMode> modes,
			final BooleanSupplier cancelled){
		record Scored(FLEFRecord record, String text, SearchMatcher.MatchScore match){}

		final boolean inNotes = modes.contains(SearchMode.NOTES_AND_CITATIONS);
		final List<Scored> scored = new ArrayList<>();
		for(final FLEFRecord record : records){
			if(cancelled.getAsBoolean())
				return List.of();

			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
			final String text = (handler != null? handler.getDisplayText(record, model): record.getId());
			final String target = (inNotes? text + '\n' + AttachedTextExtractor.extract(record): text);
			final SearchMatcher.MatchScore match = SearchMatcher.score(query, target, modes);
			if(match.passes())
				scored.add(new Scored(record, text, match));
		}
		scored.sort(Comparator.comparingInt((Scored s) -> s.match().matchedTokens()).reversed()
			.thenComparing(Comparator.comparingDouble((Scored s) -> s.match().score()).reversed())
			.thenComparing(Scored::text, String.CASE_INSENSITIVE_ORDER));

		final List<FLEFRecord> result = new ArrayList<>(scored.size());
		for(final Scored s : scored)
			result.add(s.record());
		return result;
	}

	private void setListModel(final RecordTableModel shown){
		if(listedType == null)
			return;

		uncitedTable.setModel(shown);
		configureColumns(uncitedTable);
		listTitle.setText((ALL_TYPES.equals(listedType)? I18N.t("text.citation_analysis.all_types"): typeLabel(listedType))
			+ " (" + shown.getRowCount() + ")");
		updateButtons();
	}

	/* ======================================================================
	 *                          Tables
	 * ====================================================================== */

	private JTable createRecordTable(final List<FLEFRecord> records, final String idColumn, final String firstColumn,
			final String secondColumn, final Function<FLEFRecord, String> first, final Function<FLEFRecord, String> second){
		final JTable table = new JTable(new RecordTableModel(records, new String[]{idColumn, firstColumn, secondColumn}, first, second));
		configureRecordTable(table);
		return table;
	}

	private void configureRecordTable(final JTable table){
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		configureColumns(table);
		table.getSelectionModel()
			.addListSelectionListener(e -> updateButtons());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openSelected();
			}
		});
	}

	private static void configureColumns(final JTable table){
		final TableColumnModel columns = table.getColumnModel();
		if(columns.getColumnCount() < 3)
			return;

		columns.getColumn(0).setPreferredWidth(110);
		columns.getColumn(1).setPreferredWidth(420);
		columns.getColumn(2).setPreferredWidth(220);
	}


	/* ======================================================================
	 *                          Buttons
	 * ====================================================================== */

	/** @return	The table of the tab in view. */
	private JTable activeRecordTable(){
		final int index = tabs.getSelectedIndex();
		return (index >= 0 && index < tables.size()? tables.get(index): null);
	}

	/** @return	Whether the tab in view is the uncited one and it shows the summary, not a list of records. */
	private boolean summaryShown(){
		return (tabs.getSelectedIndex() == UNCITED_TAB && uncitedCards != null && listedType == null);
	}

	private void updateButtons(){
		final JTable table = activeRecordTable();
		openRecord.setEnabled(table != null && !summaryShown() && table.getSelectedRow() >= 0);
		exportCsv.setEnabled(table != null && (summaryShown()? allUncited.getRowCount(): table.getModel().getRowCount()) > 0);
	}

	private void openSelected(){
		final JTable table = activeRecordTable();
		if(table == null || summaryShown() || table.getSelectedRow() < 0)
			return;

		final FLEFRecord record = ((RecordTableModel)table.getModel()).recordAt(table.convertRowIndexToModel(table.getSelectedRow()));
		ToolDialogs.openRecordEditor(this, model, record);

		reload();
	}


	/* ======================================================================
	 *                          Export
	 * ====================================================================== */

	/**
	 * Writes the rows of the tab in view to a CSV file: what the table shows, in the order and with the filter it has (the summary of
	 * the uncited tab exports every uncited record).
	 */
	private void exportToCsv(){
		final JTable table = activeRecordTable();
		if(table == null)
			return;

		final RecordTableModel records = (summaryShown()? allUncited: (RecordTableModel)table.getModel());
		final List<String[]> rows = new ArrayList<>();
		rows.add(records.columnNames());
		if(summaryShown())
			for(int row = 0, count = records.getRowCount(); row < count; row ++)
				rows.add(records.rowValues(row));
		else
			for(int viewRow = 0, count = table.getRowCount(); viewRow < count; viewRow ++)
				rows.add(records.rowValues(table.convertRowIndexToModel(viewRow)));

		final JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(I18N.t("dialog.citation_analysis.export.title"));
		chooser.setFileFilter(new FileNameExtensionFilter(I18N.t("file.filter.csv"), "csv"));
		chooser.setSelectedFile(new File("citation-analysis.csv"));
		if(chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;

		File target = chooser.getSelectedFile();
		if(!target.getName().toLowerCase(Locale.ROOT).endsWith(".csv"))
			target = new File(target.getParentFile(), target.getName() + ".csv");
		if(target.exists() && JOptionPane.showConfirmDialog(this, I18N.t("confirmation.overwrite.export.message"),
				I18N.t("confirmation.overwrite.export.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
				!= JOptionPane.YES_OPTION)
			return;

		try{
			//the byte order mark makes Excel read the accents right
			Files.writeString(target.toPath(), "﻿" + CsvFormat.format(rows), StandardCharsets.UTF_8);
		}
		catch(final IOException e){
			JOptionPane.showMessageDialog(this, I18N.tf("error.csv.export", target.getAbsolutePath(), e.getMessage()),
				I18N.t("error.title"), JOptionPane.ERROR_MESSAGE);
			return;
		}
		SavedFileNotice.show(this, I18N.t("dialog.citation_analysis.export.title"),
			I18N.tf("message.csv.exported", rows.size() - 1, target.getAbsolutePath()), target);
	}


	/* ======================================================================
	 *                          Clipboard
	 * ====================================================================== */

	private void copyToClipboard(){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tables.size(); i ++){
			final RecordTableModel records = (i == UNCITED_TAB? allUncited: (RecordTableModel)tables.get(i).getModel());
			sb.append(tabs.getTitleAt(i))
				.append(StringUtils.LF);
			for(int row = 0; row < records.getRowCount(); row ++)
				sb.append("  ").append(records.getValueAt(row, 0)).append("  ").append(records.getValueAt(row, 1)).append("  ")
					.append(records.getValueAt(row, 2))
					.append(StringUtils.LF);
			sb.append(StringUtils.LF);
		}

		final StringSelection selection = new StringSelection(sb.toString());
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(selection, selection);
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	/** The records of a list: what a row says is written the first time the table asks for it (a list can have tens of thousands). */
	private static final class RecordTableModel extends AbstractTableModel{

		private final List<FLEFRecord> records;
		private final String[] columns;
		private final Function<FLEFRecord, String> first;
		private final Function<FLEFRecord, String> second;
		private final String[][] cache;


		RecordTableModel(final List<FLEFRecord> records, final String[] columns, final Function<FLEFRecord, String> first,
				final Function<FLEFRecord, String> second){
			this.records = new ArrayList<>(records);
			this.columns = columns;
			this.first = first;
			this.second = second;
			cache = new String[records.size()][];
		}

		List<FLEFRecord> records(){
			return records;
		}

		FLEFRecord recordAt(final int row){
			return records.get(row);
		}

		String[] columnNames(){
			return columns.clone();
		}

		String[] rowValues(final int row){
			return new String[]{(String)getValueAt(row, 0), (String)getValueAt(row, 1), (String)getValueAt(row, 2)};
		}

		@Override
		public int getRowCount(){
			return records.size();
		}

		@Override
		public int getColumnCount(){
			return columns.length;
		}

		@Override
		public String getColumnName(final int column){
			return columns[column];
		}

		@Override
		public Object getValueAt(final int row, final int column){
			String[] values = cache[row];
			if(values == null){
				final FLEFRecord record = records.get(row);
				values = new String[]{record.getId(), StringUtils.defaultString(first.apply(record)),
					StringUtils.defaultString(second.apply(record))};
				cache[row] = values;
			}
			return values[column];
		}

	}

}
