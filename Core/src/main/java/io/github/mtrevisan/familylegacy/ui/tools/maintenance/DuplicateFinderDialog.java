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
package io.github.mtrevisan.familylegacy.ui.tools.maintenance;

import io.github.mtrevisan.familylegacy.comparison.IndividualFacts;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.DuplicateFinderService;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDiffDialog;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Dialog that finds possible duplicate individuals and lets the user
 * inspect the candidates, compare them side by side, and open the
 * record editor.
 * <p>
 * The search is delegated to {@link DuplicateFinderService} and runs in
 * a {@link SwingWorker}, so the dialog stays responsive on large files.
 * The candidate table can be filtered by a free-text search and sorted
 * by any column.
 */
public final class DuplicateFinderDialog extends JDialog{

	private final ToolContext context;
	private final CandidateTableModel tableModel = new CandidateTableModel();
	private final JTable table = new JTable(tableModel);
	private final SearchBox searchBox = new SearchBox(24, false);
	private final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(
		DuplicateFinderService.DEFAULT_THRESHOLD, 0, 500, 5));
	private final JSpinner maxResultsSpinner = new JSpinner(new SpinnerNumberModel(
		DuplicateFinderService.DEFAULT_MAX_RESULTS, 10, 100000, 50));
	private final JCheckBox sameSexCheck = new JCheckBox(I18N.t("checkbox.same_sex_only"), true);
	private final JCheckBox skipExistingCheck = new JCheckBox(I18N.t("checkbox.skip_existing"), true);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	/** How far the search is, so that the user knows when the data will be there: shown only while it runs. */
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final JTextArea detailsArea = new JTextArea();

	private List<DuplicateFinderService.Candidate> allCandidates = List.of();
	private SwingWorker<List<DuplicateFinderService.Candidate>, Void> searchWorker;


	public DuplicateFinderDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.duplicates.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createCenter(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1200, 720));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		runSearch();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new GridBagLayout());
		toolbar.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 4, 2, 4);
		gbc.anchor = GridBagConstraints.WEST;

		gbc.gridx = 0; gbc.gridy = 0;
		toolbar.add(new JLabel(I18N.l("label.threshold")), gbc);
		gbc.gridx = 1;
		toolbar.add(thresholdSpinner, gbc);

		gbc.gridx = 2;
		toolbar.add(new JLabel(I18N.l("label.max_results")), gbc);
		gbc.gridx = 3;
		toolbar.add(maxResultsSpinner, gbc);

		gbc.gridx = 4;
		toolbar.add(sameSexCheck, gbc);
		gbc.gridx = 5;
		toolbar.add(skipExistingCheck, gbc);

		gbc.gridx = 6;
		final JButton searchButton = new JButton(I18N.t("button.search"));
		searchButton.addActionListener(e -> runSearch());
		toolbar.add(searchButton, gbc);

		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
		gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
		toolbar.add(new JLabel(I18N.l("label.duplicates_filter")), gbc);
		gbc.gridy = 2;
		toolbar.add(searchBox, gbc);
		searchBox.onChange(this::applyFilter);

		return toolbar;
	}

	private JSplitPane createCenter(){
		//more than one pair can be selected: the facts of all their individuals are compared (the details are the ones of the first)
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(60);
		columnModel.getColumn(1).setPreferredWidth(180);
		columnModel.getColumn(2).setPreferredWidth(180);
		columnModel.getColumn(3).setPreferredWidth(200);
		columnModel.getColumn(4).setPreferredWidth(360);

		final ListSelectionModel selectionModel = table.getSelectionModel();
		selectionModel.addListSelectionListener(e -> {
			if(!e.getValueIsAdjusting())
				updateDetails();
		});
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					compareSelected();
			}
		});

		final JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.candidates")));

		detailsArea.setEditable(false);
		detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		detailsArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane detailsScroll = new JScrollPane(detailsArea);
		detailsScroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.details")));

		final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
			tableScroll, detailsScroll);
		split.setResizeWeight(0.65);
		split.setDividerLocation(0.65);
		split.setContinuousLayout(true);
		return split;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));

		final JButton compareFacts = new JButton(I18N.t("button.compare_facts"));
		compareFacts.addActionListener(e -> compareFactsOfSelected());
		footer.add(compareFacts);

		final JButton compare = new JButton(I18N.t("button.compare_records"));
		compare.addActionListener(e -> compareSelected());
		footer.add(compare);

		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		footer.add(progressBar);
		footer.add(statusLabel);


		return footer;
	}


	/* ======================================================================
	 *                          Search
	 * ====================================================================== */

	private void runSearch(){
		final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
		options.threshold = (Integer)thresholdSpinner.getValue();
		options.maxResults = (Integer)maxResultsSpinner.getValue();
		options.requireSameSex = sameSexCheck.isSelected();
		options.skipExistingHypotheses = skipExistingCheck.isSelected();

		//the bar says it, with its percentage: the status line has nothing to add while the search runs
		statusLabel.setText(StringUtils.SPACE);
		progressBar.setValue(0);
		progressBar.setVisible(true);
		tableModel.setRows(List.of());

		final FLEFModel model = context.model();
		//a new search replaces the running one: its result must not overwrite the one of the new search
		if(searchWorker != null)
			searchWorker.cancel(true);
		searchWorker = new SwingWorker<>(){
			@Override
			protected List<DuplicateFinderService.Candidate> doInBackground(){
				//setProgress is safe from this thread: the listener below receives the values on the event dispatch thread
				return DuplicateFinderService.find(model, options, fraction -> setProgress((int)Math.round(fraction * 100.)));
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				progressBar.setVisible(false);
				try{
					allCandidates = get();
					tableModel.setRows(allCandidates);
					applyFilter();
					statusLabel.setText(I18N.plural("text.duplicates.candidates", allCandidates.size()));
				}
				catch(final Exception ex){
					statusLabel.setText(I18N.tf("text.search.failed", ex.getMessage()));
				}
			}
		};
		searchWorker.addPropertyChangeListener(event -> {
			if("progress".equals(event.getPropertyName()) && searchWorker != null && event.getSource() == searchWorker){
				progressBar.setValue((Integer)event.getNewValue());
			}
		});
		searchWorker.execute();
	}

	@Override
	public void dispose(){
		if(searchWorker != null)
			searchWorker.cancel(true);

		super.dispose();
	}

	private void applyFilter(){
				@SuppressWarnings("unchecked")
		final TableRowSorter<CandidateTableModel> sorter =
			(TableRowSorter<CandidateTableModel>)table.getRowSorter();
		if(searchBox.isBlank())
			sorter.setRowFilter(null);
		else{
			final String query = searchBox.query();
			final Set<SearchMode> modes = searchBox.modes();
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends CandidateTableModel, ? extends Integer> entry){
					final DuplicateFinderService.Candidate c =
						tableModel.getRow(entry.getIdentifier());
					return SearchBox.matches(query, modes, c.left().displayName(), c.right().displayName(), c.left().id(), c.right().id())
						|| matchesAnyFeature(c, query, modes);
				}
			});
		}
		statusLabel.setText(I18N.tf("text.duplicates.shown", table.getRowCount(), allCandidates.size()));
	}

	private static boolean matchesAnyFeature(final DuplicateFinderService.Candidate c,
		final String query, final Set<SearchMode> modes){
		for(final DuplicateFinderService.FeatureMatch m : c.matches())
			if(SearchBox.matches(query, modes, m.detail()))
				return true;
		return false;
	}


	/* ======================================================================
	 *                          Details and actions
	 * ====================================================================== */

	private void updateDetails(){
		final DuplicateFinderService.Candidate c = selectedCandidate();
		if(c == null){
			detailsArea.setText(StringUtils.EMPTY);
			return;
		}

		final StringBuilder sb = new StringBuilder();
		sb.append(I18N.l("column.score"))
			.append(StringUtils.SPACE)
			.append(c.score())
			.append(StringUtils.LF)
			.append(StringUtils.LF);
		sb.append(I18N.l("column.left"))
			.append(StringUtils.SPACE)
			.append(c.left().displayName())
			.append(StringUtils.SPACE)
			.append('[')
			.append(c.left().id())
			.append(']')
			.append(StringUtils.LF);
		sb.append(I18N.l("column.right"))
			.append(StringUtils.SPACE)
			.append(c.right().displayName())
			.append(StringUtils.SPACE)
			.append('[')
			.append(c.right().id())
			.append(']')
			.append(StringUtils.LF)
			.append(StringUtils.LF);

		sb.append(I18N.l("text.duplicates.matched_features"))
			.append(StringUtils.LF);
		for(final DuplicateFinderService.FeatureMatch m : c.matches())
			sb.append("  ").append(String.format("%+d", m.score())).append("  ")
				.append(m.feature())
				.append(" — ")
				.append(m.detail())
				.append(StringUtils.LF);

		if(!c.differences().isEmpty()){
			sb.append(StringUtils.LF)
				.append(I18N.l("column.differences"))
				.append(StringUtils.LF);
			for(final String d : c.differences())
				sb.append("  - ")
					.append(d)
					.append(StringUtils.LF);
		}
		detailsArea.setText(sb.toString());
		detailsArea.setCaretPosition(0);
	}

	private DuplicateFinderService.Candidate selectedCandidate(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;

		return tableModel.getRow(table.convertRowIndexToModel(viewRow));
	}

	/** The most individuals the facts are compared of: more would be a column too narrow to read. */
	private static final int MAX_COMPARED = 6;

	/**
	 * Compares the facts of the individuals of the selected pairs, side by side: the birth, the death, the parents, the spouses, the
	 * children, the occupations and the rest, that the comparison of the records (the text of the individual only) does not show.
	 */
	private void compareFactsOfSelected(){
		final List<String> ids = new ArrayList<>();
		for(final int viewRow : table.getSelectedRows()){
			final DuplicateFinderService.Candidate candidate = tableModel.getRow(table.convertRowIndexToModel(viewRow));
			for(final String id : List.of(candidate.left().id(), candidate.right().id()))
				if(!ids.contains(id))
					ids.add(id);
		}
		if(ids.size() < 2)
			return;

		final int truncated = (ids.size() > MAX_COMPARED? MAX_COMPARED: 0);
		final List<String> compared = ids.subList(0, Math.min(ids.size(), MAX_COMPARED));
		final FLEFModel model = context.model();
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		//the indexes of the model are built to read the facts: on a large file it takes a moment, not on the event dispatch thread
		new SwingWorker<IndividualFacts.Comparison, Void>(){
			@Override
			protected IndividualFacts.Comparison doInBackground(){
				return IndividualFacts.compare(model, compared);
			}

			@Override
			protected void done(){
				setCursor(Cursor.getDefaultCursor());
				try{
					final IndividualFacts.Comparison comparison = get();
					if(!comparison.isEmpty())
						new FactsComparisonDialog(DuplicateFinderDialog.this, comparison, truncated).setVisible(true);
				}
				catch(final Exception ignored){
					//the comparison is canceled, or the model changed: nothing to show
				}
			}
		}.execute();
	}

	private void compareSelected(){
		final DuplicateFinderService.Candidate c = selectedCandidate();
		if(c == null)
			return;

		final FLEFModel model = context.model();
		final FLEFRecord left = model.getRecordById(c.left().id());
		final FLEFRecord right = model.getRecordById(c.right().id());
		if(left == null || right == null)
			return;
		RecordDiffDialog.showComparison(this,
			I18N.tf("text.duplicates.compare", c.left().displayName(), c.right().displayName()),
			left, right, model);
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class CandidateTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.score"), I18N.t("column.left"), I18N.t("column.right"), I18N.t("column.matched"),
			I18N.t("column.differences")};

		private final List<DuplicateFinderService.Candidate> rows = new ArrayList<>();

		void setRows(final List<DuplicateFinderService.Candidate> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		DuplicateFinderService.Candidate getRow(final int index){
			return rows.get(index);
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final DuplicateFinderService.Candidate c = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> c.score();
				case 1 -> c.left().displayName() + " [" + c.left().id() + "]";
				case 2 -> c.right().displayName() + " [" + c.right().id() + "]";
				case 3 -> summarizeMatches(c);
				case 4 -> String.join(", ", c.differences());
				default -> StringUtils.EMPTY;
			};
		}

		private static String summarizeMatches(final DuplicateFinderService.Candidate c){
			final StringBuilder sb = new StringBuilder();
			for(int i = 0; i < c.matches().size(); i ++){
				if(i > 0)
					sb.append(", ");
				sb.append(c.matches().get(i).feature());
			}
			return sb.toString();
		}
	}

}
