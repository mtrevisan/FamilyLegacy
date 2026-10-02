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
package io.github.mtrevisan.familylegacy.ui.tools.statistics;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;


/**
 * Result dialog of the statistics tool. Shows three tabs, each with one
 * or more tables:
 * <ul>
 *   <li><b>Completeness</b>: a table of categories; double-clicking a
 *       row opens the list of records in that category, where a record
 *       can be opened in the editor;</li>
 *   <li><b>Distribution</b>: two tables, top surnames (clustered) and
 *       top given names;</li>
 *   <li><b>Source coverage</b>: a table of per-type coverage and a
 *       table of most cited sources. Sources are clickable and show
 *       their full name, not a truncated one.</li>
 * </ul>
 * The dialog receives an already computed {@link Statistics}; it never
 * touches the model except to open a record editor on double-click.
 * <p>
 * A shared status label at the bottom of the dialog reports the number
 * of selected rows, the sum of their occurrences, and the percentage
 * of that sum over the relevant total (surnames, given names, records,
 * or citations, depending on the active table). The label is cleared
 * whenever the user switches tab, so a stale selection from the
 * previous tab never lingers.
 */
public final class StatisticsDialog extends JDialog{

	private static final Color LINK_COLOR = new Color(0x1565C0);

	/** Column index (in view coordinates) of the numeric count in each table. */
	private static final int COUNT_COLUMN_COMPLETENESS = 1;
	private static final int COUNT_COLUMN_NAMES = 2;
	private static final int COUNT_COLUMN_SOURCES = 2;


	private final FLEFModel model;

	/** Shared status label; updated by the selection listeners of the tables. */
	private final JLabel selectionSummary = new JLabel(StringUtils.SPACE);


	public StatisticsDialog(final Window owner, final Statistics stats, final FLEFModel model){
		super(owner, "Statistics", ModalityType.APPLICATION_MODAL);
		this.model = model;

		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Completeness", createCompletenessTab(stats));
		tabs.addTab("Distribution", createDistributionTab(stats));
		tabs.addTab("Source coverage", createCoverageTab(stats));
		tabs.setPreferredSize(new Dimension(920, 620));

		// Clear the summary on tab switch: a selection in a hidden tab
		// is no longer visible, so the summary would be misleading.
		tabs.addChangeListener(e -> selectionSummary.setText(StringUtils.SPACE));

		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(close);

		final JPanel footer = new JPanel(new BorderLayout(8, 0));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 8, 6, 6));
		footer.add(selectionSummary, BorderLayout.WEST);
		footer.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/* ======================================================================
	 *                          Completeness
	 * ====================================================================== */

	private JPanel createCompletenessTab(final Statistics stats){
		final CompletenessTableModel model = new CompletenessTableModel(stats.completeness());
		final JTable table = new JTable(model);
		table.setRowHeight(24);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.getColumnModel().getColumn(1).setPreferredWidth(80);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
					final int row = table.getSelectedRow();
					if(row >= 0)
						openCategory(model.categoryAt(row));
				}
			}
		});
		attachSelectionSummary(table, COUNT_COLUMN_COMPLETENESS, stats.completeness().totalIndividuals());

		final JPanel header = new JPanel(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		header.add(new JLabel(GUIHelper.format("Individuals: %,d   Events: %,d   Sources: %,d",
			stats.completeness().totalIndividuals(),
			stats.completeness().totalEvents(),
			stats.completeness().totalSources())), BorderLayout.WEST);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

	private void openCategory(final Statistics.Completeness.Category category){
		if(category == null || category.records().isEmpty())
			return;
		new MissingRecordsDialog(this, category.label(), model, category.records())
			.setVisible(true);
	}


	/* ======================================================================
	 *                          Distribution
	 * ====================================================================== */

	private JPanel createDistributionTab(final Statistics stats){
		final NameTableModel surnames = new NameTableModel(stats.distribution()
			.surnames());
		final NameTableModel given = new NameTableModel(stats.distribution()
			.givenNames());

		final JTable surnameTable = new JTable(surnames);
		surnameTable.setRowHeight(22);
		surnameTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		surnameTable.getColumnModel().getColumn(1).setPreferredWidth(360);
		surnameTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		surnameTable.setDefaultRenderer(Object.class, new LinkCellRenderer(false));
		surnameTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		attachSelectionSummary(surnameTable, COUNT_COLUMN_NAMES, stats.distribution().totalSurnames());

		final JTable givenTable = new JTable(given);
		givenTable.setRowHeight(22);
		givenTable.getColumnModel().getColumn(0).setPreferredWidth(120);
		givenTable.getColumnModel().getColumn(1).setPreferredWidth(360);
		givenTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		givenTable.setDefaultRenderer(Object.class, new LinkCellRenderer(false));
		givenTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		attachSelectionSummary(givenTable, COUNT_COLUMN_NAMES, stats.distribution().totalGivenNames());

		final JScrollPane surnameScroll = new JScrollPane(surnameTable);
		surnameScroll.setBorder(BorderFactory.createTitledBorder("Top 20 surnames"));
		final JScrollPane givenScroll = new JScrollPane(givenTable);
		givenScroll.setBorder(BorderFactory.createTitledBorder("Top 20 given names"));

		final JPanel header = new JPanel(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		header.add(new JLabel(GUIHelper.format("Sex: %,d male, %,d female, %,d unknown",
			stats.distribution().male(),
			stats.distribution().female(),
			stats.distribution().unknownSex())), BorderLayout.WEST);

		final JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.add(header, BorderLayout.NORTH);

		final JPanel tables = new JPanel(new GridLayout(2, 1, 8, 8));
		tables.add(surnameScroll);
		tables.add(givenScroll);
		panel.add(tables, BorderLayout.CENTER);
		return panel;
	}


	/* ======================================================================
	 *                          Coverage
	 * ====================================================================== */

	private JPanel createCoverageTab(final Statistics stats){
		final CoverageTableModel coverageModel = new CoverageTableModel(stats.coverage()
			.byType());
		final SourceUseTableModel sourcesModel = new SourceUseTableModel(stats.coverage()
			.topSources());

		final JTable coverageTable = new JTable(coverageModel);
		coverageTable.setRowHeight(22);
		coverageTable.getColumnModel().getColumn(0).setPreferredWidth(200);
		coverageTable.getColumnModel().getColumn(1).setPreferredWidth(80);
		coverageTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		coverageTable.getColumnModel().getColumn(3).setPreferredWidth(100);
		coverageTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		final JTable sourcesTable = new JTable(sourcesModel);
		sourcesTable.setRowHeight(22);
		sourcesTable.getColumnModel().getColumn(0).setPreferredWidth(80);
		sourcesTable.getColumnModel().getColumn(1).setPreferredWidth(560);
		sourcesTable.getColumnModel().getColumn(2).setPreferredWidth(80);
		sourcesTable.setDefaultRenderer(Object.class, new LinkCellRenderer(true));
		sourcesTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		attachSelectionSummary(sourcesTable, COUNT_COLUMN_SOURCES, stats.coverage().totalCitations());
		sourcesTable.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)){
					final int row = sourcesTable.getSelectedRow();
					if(row >= 0)
						openSource(sourcesModel.sourceAt(row));
				}
			}
		});

		final JScrollPane coverageScroll = new JScrollPane(coverageTable);
		coverageScroll.setBorder(BorderFactory.createTitledBorder("Coverage by record type"));
		final JScrollPane sourcesScroll = new JScrollPane(sourcesTable);
		sourcesScroll.setBorder(BorderFactory.createTitledBorder(
			"Most cited sources (double-click to open)"));

		final JPanel tables = new JPanel(new GridLayout(2, 1, 8, 8));
		tables.add(coverageScroll);
		tables.add(sourcesScroll);

		final JPanel panel = new JPanel(new BorderLayout(8, 8));
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		panel.add(tables, BorderLayout.CENTER);
		return panel;
	}

	private void openSource(final Statistics.SourceUse source){
		if(source == null)
			return;
		new MissingRecordsDialog(this, "Source " + source.sourceId(), model,
			List.of(model.getRecordById(source.sourceId())))
			.setVisible(true);
	}


	/* ======================================================================
	 *                          Selection summary
	 * ====================================================================== */

	/**
	 * Attaches a selection listener to the given table so that the
	 * shared status label reports the number of selected rows, the sum
	 * of their occurrences, and the percentage of that sum over the
	 * given total.
	 * <p>
	 * Only the table that currently has focus drives the summary: the
	 * distribution tab has two tables side by side, and updating the
	 * label from a table the user is not interacting with would be
	 * confusing.
	 *
	 * @param table       the table to monitor
	 * @param countColumn the index (in view coordinates) of the column
	 *                    whose values should be summed
	 * @param total       the denominator for the percentage, or 0 to
	 *                    omit the percentage
	 */
	private void attachSelectionSummary(final JTable table, final int countColumn, final long total){
		final ListSelectionListener listener = e -> {
			if(e.getValueIsAdjusting())
				return;
			if(table != focusedTable())
				return;
			updateSummary(table, countColumn, total);
		};
		table.getSelectionModel()
			.addListSelectionListener(listener);
	}

	private static JTable focusedTable(){
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager()
			.getFocusOwner();
		return (focusOwner instanceof JTable t? t: null);
	}

	private void updateSummary(final JTable table, final int countColumn, final long total){
		final int[] rows = table.getSelectedRows();
		if(rows.length == 0){
			selectionSummary.setText(StringUtils.SPACE);
			return;
		}

		long sum = 0;
		for(final int viewRow : rows){
			final int modelRow = table.convertRowIndexToModel(viewRow);
			final Object value = table.getModel()
				.getValueAt(modelRow, countColumn);
			sum += parseCount(value);
		}

		if(total > 0){
			final double percent = 100. * sum / total;
			selectionSummary.setText(GUIHelper.format(
				"%d selected — %,d total occurrences (%.1f%% of %,d)",
				rows.length, sum, percent, total));
		}
		else{
			selectionSummary.setText(GUIHelper.format(
				"%d selected — %,d total occurrences",
				rows.length, sum));
		}
	}

	/**
	 * Parses a cell value as a count. The distribution and coverage
	 * tables expose their numeric columns as {@code String} formatted
	 * with a space ({@code U+00A0}) as the grouping separator (e.g.
	 * {@code 1 234}), so spaces are stripped before parsing. When the
	 * value is not numeric, 0 is returned so the sum of the other rows
	 * is still meaningful.
	 */
	private static long parseCount(final Object value){
		if(value == null)
			return 0;
		if(value instanceof Number n)
			return n.longValue();
		final String s = value.toString()
			.replace(",", StringUtils.EMPTY)
			.replace('\u00A0', ' ')
			.replace(StringUtils.SPACE, StringUtils.EMPTY)
			.trim();
		try{
			return Long.parseLong(s);
		}
		catch(final NumberFormatException ex){
			return 0;
		}
	}


	/* ======================================================================
	 *                          Table models
	 * ====================================================================== */

	private static final class CompletenessTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Category", "Count"};
		private final List<Statistics.Completeness.Category> rows;

		CompletenessTableModel(final Statistics.Completeness completeness){
			this.rows = completeness.categories();
		}

		Statistics.Completeness.Category categoryAt(final int row){
			return (row >= 0 && row < rows.size()? rows.get(row): null);
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
		public String getColumnName(final int c){
			return COLUMNS[c];
		}

		@Override
		public Object getValueAt(final int r, final int c){
			final Statistics.Completeness.Category cat = rows.get(r);
			return switch(c){
				case 0 -> cat.label();
				case 1 -> GUIHelper.format("%,d", cat.count());
				default -> null;
			};
		}

		@Override
		public boolean isCellEditable(final int r, final int c){
			return false;
		}
	}

	private static final class NameTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Canonical", "Variants", "Total"};
		private final List<Statistics.NameGroup> rows;

		NameTableModel(final List<Statistics.NameGroup> rows){
			this.rows = rows;
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
		public String getColumnName(final int c){
			return COLUMNS[c];
		}

		@Override
		public Object getValueAt(final int r, final int c){
			final Statistics.NameGroup g = rows.get(r);
			return switch(c){
				case 0 -> g.canonical();
				case 1 -> String.join(" / ", g.variants());
				case 2 -> GUIHelper.format("%,d", g.count());
				default -> null;
			};
		}
	}

	private static final class CoverageTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Record type", "Total", "Cited", "Coverage"};
		private final List<Statistics.TypeCoverage> rows;

		CoverageTableModel(final List<Statistics.TypeCoverage> rows){
			this.rows = rows;
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
		public String getColumnName(final int c){
			return COLUMNS[c];
		}

		@Override
		public Object getValueAt(final int r, final int c){
			final Statistics.TypeCoverage t = rows.get(r);
			return switch(c){
				case 0 -> t.tag();
				case 1 -> GUIHelper.format("%,d", t.total());
				case 2 -> GUIHelper.format("%,d", t.cited());
				case 3 -> GUIHelper.format("%.1f%%", t.percentage());
				default -> null;
			};
		}
	}

	private static final class SourceUseTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Id", "Source", "Citations"};
		private final List<Statistics.SourceUse> rows;

		SourceUseTableModel(final List<Statistics.SourceUse> rows){
			this.rows = rows;
		}

		Statistics.SourceUse sourceAt(final int row){
			return (row >= 0 && row < rows.size()? rows.get(row): null);
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
		public String getColumnName(final int c){
			return COLUMNS[c];
		}

		@Override
		public Object getValueAt(final int r, final int c){
			final Statistics.SourceUse s = rows.get(r);
			return switch(c){
				case 0 -> s.sourceId();
				case 1 -> s.displayName();
				case 2 -> GUIHelper.format("%,d", s.count());
				default -> null;
			};
		}
	}


	/**
	 * Renders the source name column and, when {@code linkColumns}, the
	 * source id column in a link colour. Also sets a tooltip with the
	 * full value, so a source whose display name is wider than the
	 * column can still be read.
	 */
	private static final class LinkCellRenderer extends DefaultTableCellRenderer{

		private final boolean linkColumns;

		LinkCellRenderer(final boolean linkColumns){
			this.linkColumns = linkColumns;
		}

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(value != null)
				setToolTipText(value.toString());

			if(!isSelected){
				if(linkColumns && (column == 0 || column == 1))
					setForeground(LINK_COLOR);
				else
					setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			return this;
		}
	}

}
