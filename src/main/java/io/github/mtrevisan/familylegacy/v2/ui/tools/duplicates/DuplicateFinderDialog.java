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
package io.github.mtrevisan.familylegacy.v2.ui.tools.duplicates;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.RecordDiffDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
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
import java.util.Locale;


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
	private final JTextField filterField = new JTextField(20);
	private final JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(
		DuplicateFinderService.DEFAULT_THRESHOLD, 0, 500, 5));
	private final JSpinner maxResultsSpinner = new JSpinner(new SpinnerNumberModel(
		DuplicateFinderService.DEFAULT_MAX_RESULTS, 10, 100000, 50));
	private final JCheckBox sameSexCheck = new JCheckBox("Same sex only", true);
	private final JCheckBox skipExistingCheck = new JCheckBox("Skip existing hypotheses", true);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final JTextArea detailsArea = new JTextArea();

	private List<DuplicateFinderService.Candidate> allCandidates = List.of();


	public DuplicateFinderDialog(final ToolContext context){
		super(context.owner(), "Find Similar Individuals", ModalityType.APPLICATION_MODAL);

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
		toolbar.add(new JLabel("Threshold:"), gbc);
		gbc.gridx = 1;
		toolbar.add(thresholdSpinner, gbc);

		gbc.gridx = 2;
		toolbar.add(new JLabel("Max results:"), gbc);
		gbc.gridx = 3;
		toolbar.add(maxResultsSpinner, gbc);

		gbc.gridx = 4;
		toolbar.add(sameSexCheck, gbc);
		gbc.gridx = 5;
		toolbar.add(skipExistingCheck, gbc);

		gbc.gridx = 6;
		final JButton searchButton = new JButton("Search");
		searchButton.addActionListener(e -> runSearch());
		toolbar.add(searchButton, gbc);

		gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 6;
		gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
		toolbar.add(new JLabel("Filter (matches name or matched features):"), gbc);
		gbc.gridy = 2;
		toolbar.add(filterField, gbc);
		filterField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});

		return toolbar;
	}

	private JSplitPane createCenter(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(60);
		table.getColumnModel().getColumn(1).setPreferredWidth(180);
		table.getColumnModel().getColumn(2).setPreferredWidth(180);
		table.getColumnModel().getColumn(3).setPreferredWidth(200);
		table.getColumnModel().getColumn(4).setPreferredWidth(360);

		table.getSelectionModel().addListSelectionListener(e -> {
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
		tableScroll.setBorder(BorderFactory.createTitledBorder("Candidates"));

		detailsArea.setEditable(false);
		detailsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
		detailsArea.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		final JScrollPane detailsScroll = new JScrollPane(detailsArea);
		detailsScroll.setBorder(BorderFactory.createTitledBorder("Details"));

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

		final JButton compare = new JButton("Compare records…");
		compare.addActionListener(e -> compareSelected());
		footer.add(compare);

		footer.add(statusLabel);

		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		footer.add(close);

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

		statusLabel.setText("Searching…");
		tableModel.setRows(List.of());

		final FLEFModel model = context.model();
		new SwingWorker<List<DuplicateFinderService.Candidate>, Void>(){
			@Override
			protected List<DuplicateFinderService.Candidate> doInBackground(){
				return DuplicateFinderService.find(model, options);
			}

			@Override
			protected void done(){
				try{
					allCandidates = get();
					tableModel.setRows(allCandidates);
					applyFilter();
					statusLabel.setText(allCandidates.size()
						+ (allCandidates.size() == 1? " candidate": " candidates"));
				}
				catch(final Exception ex){
					statusLabel.setText("Search failed: " + ex.getMessage());
				}
			}
		}.execute();
	}

	private void applyFilter(){
		final String text = filterField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<CandidateTableModel> sorter =
			(TableRowSorter<CandidateTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends CandidateTableModel, ? extends Integer> entry){
					final DuplicateFinderService.Candidate c =
						tableModel.getRow(entry.getIdentifier());
					return contains(c.left().displayName(), needle)
						|| contains(c.right().displayName(), needle)
						|| contains(c.left().id(), needle)
						|| contains(c.right().id(), needle)
						|| matchesAnyFeature(c, needle);
				}
			});
		}
		statusLabel.setText(table.getRowCount() + " shown, "
			+ allCandidates.size() + " total");
	}

	private static boolean matchesAnyFeature(final DuplicateFinderService.Candidate c,
		final String needle){
		for(final DuplicateFinderService.FeatureMatch m : c.matches())
			if(contains(m.detail(), needle))
				return true;
		return false;
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
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
		sb.append("Score: ").append(c.score()).append("\n\n");
		sb.append("Left:  ").append(c.left().displayName())
			.append(" [").append(c.left().id()).append("]\n");
		sb.append("Right: ").append(c.right().displayName())
			.append(" [").append(c.right().id()).append("]\n\n");

		sb.append("Matched features:\n");
		for(final DuplicateFinderService.FeatureMatch m : c.matches())
			sb.append("  +").append(m.score()).append("  ")
				.append(m.feature()).append(" — ").append(m.detail()).append('\n');

		if(!c.differences().isEmpty()){
			sb.append("\nDifferences:\n");
			for(final String d : c.differences())
				sb.append("  - ").append(d).append('\n');
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

	private void compareSelected(){
		final DuplicateFinderService.Candidate c = selectedCandidate();
		if(c == null)
			return;
		final FLEFRecord left = context.model().getRecordById(c.left().id());
		final FLEFRecord right = context.model().getRecordById(c.right().id());
		if(left == null || right == null)
			return;
		RecordDiffDialog.showComparison(this,
			"Compare " + c.left().displayName() + " vs " + c.right().displayName(),
			left, right);
	}


	/* ======================================================================
	 *                          Table model
	 * ====================================================================== */

	private static final class CandidateTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Score", "Left", "Right", "Matched", "Differences"};

		private final List<DuplicateFinderService.Candidate> rows = new ArrayList<>();

		void setRows(final List<DuplicateFinderService.Candidate> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		DuplicateFinderService.Candidate getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

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
			for(int i = 0; i < c.matches().size(); i++){
				if(i > 0)
					sb.append(", ");
				sb.append(c.matches().get(i).feature());
			}
			return sb.toString();
		}
	}

}
