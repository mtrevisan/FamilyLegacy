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
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;


/**
 * The facts of two or more individuals one under the other (see {@link IndividualFacts}): a row for each fact, a column for each
 * individual. The facts that differ are marked in amber, the ones that only some individuals have in grey, and a check box hides the
 * ones that are the same, to read only what tells the individuals apart.
 */
public final class FactsComparisonDialog extends JDialog{

	/** The facts that differ. */
	static final Color COLOR_DIFFERENT = new Color(255, 236, 179);
	/** The facts that only some individuals have. */
	static final Color COLOR_MISSING = new Color(232, 232, 232);
	private static final int LABEL_COLUMN_WIDTH = 150;
	private static final int MINIMUM_COLUMN_WIDTH = 140;

	private final IndividualFacts.Comparison comparison;
	private final FactsTableModel tableModel;
	private final JTable table;
	private final JCheckBox onlyDifferences = new JCheckBox(I18N.t("text.facts.only_differences"));


	/**
	 * @param owner	the window it is shown over
	 * @param comparison	the facts to show
	 * @param truncated	how many individuals are shown if there were more than it can show, {@code 0} if all of them are
	 */
	public FactsComparisonDialog(final Window owner, final IndividualFacts.Comparison comparison, final int truncated){
		super(owner, I18N.t("text.facts.title"), ModalityType.APPLICATION_MODAL);

		this.comparison = comparison;
		tableModel = new FactsTableModel(comparison);
		table = new JTable(tableModel);

		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setTableHeader(table.getTableHeader());
		table.getTableHeader()
			.setReorderingAllowed(false);
		table.setCellSelectionEnabled(true);
		table.setDefaultRenderer(Object.class, new WrapRenderer());
		table.getColumnModel()
			.getColumn(0)
			.setPreferredWidth(LABEL_COLUMN_WIDTH);
		for(int column = 1; column < table.getColumnCount(); column ++)
			table.getColumnModel()
				.getColumn(column)
				.setPreferredWidth(Math.max(MINIMUM_COLUMN_WIDTH, 700 / Math.max(1, table.getColumnCount() - 1)));
		//the rows are as tall as their text needs, and follow the width of the columns
		table.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(final ComponentEvent e){
				fitRowHeights();
			}
		});

		final JPanel top = new JPanel(new BorderLayout(6, 2));
		top.setBorder(BorderFactory.createEmptyBorder(8, 8, 2, 8));
		final JLabel legend = new JLabel("<html>" + escape(I18N.t("text.facts.legend")) + "</html>");
		top.add(legend, BorderLayout.CENTER);
		final JPanel options = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		options.add(onlyDifferences);
		if(truncated > 0)
			options.add(new JLabel("   " + I18N.tf("text.facts.truncated", truncated)));
		top.add(options, BorderLayout.SOUTH);
		onlyDifferences.addActionListener(e -> {
			tableModel.setOnlyDifferences(onlyDifferences.isSelected());
			fitRowHeights();
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		footer.add(close);

		setLayout(new BorderLayout(0, 0));
		add(top, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		ToolDialogs.installEscapeToClose(this);

		setPreferredSize(new Dimension(Math.min(1300, 220 + 260 * comparison.individuals().size()), 560));
		pack();
		setLocationRelativeTo(owner);
		fitRowHeights();
	}


	/** The rows that are shown. */
	List<IndividualFacts.Row> shownRows(){
		return tableModel.rows;
	}

	void setOnlyDifferences(final boolean only){
		onlyDifferences.setSelected(only);
		tableModel.setOnlyDifferences(only);
		fitRowHeights();
	}

	JTable table(){
		return table;
	}

	/** Gives each row the height of its tallest cell, for the width the columns have now. */
	private void fitRowHeights(){
		final JTextArea measure = new JTextArea();
		measure.setLineWrap(true);
		measure.setWrapStyleWord(true);
		measure.setFont(table.getFont());
		for(int row = 0; row < table.getRowCount(); row ++){
			int height = table.getRowHeight();
			for(int column = 0; column < table.getColumnCount(); column ++){
				measure.setText(String.valueOf(table.getValueAt(row, column)));
				measure.setSize(Math.max(1, table.getColumnModel().getColumn(column).getWidth() - 4), Short.MAX_VALUE);
				height = Math.max(height, measure.getPreferredSize().height + 4);
			}
			if(table.getRowHeight(row) != height)
				table.setRowHeight(row, height);
		}
	}

	private static String escape(final String text){
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}


	/** The table: the facts down, the individuals across. */
	private static final class FactsTableModel extends AbstractTableModel{

		private final IndividualFacts.Comparison comparison;
		private final List<IndividualFacts.Row> rows = new ArrayList<>();


		private FactsTableModel(final IndividualFacts.Comparison comparison){
			this.comparison = comparison;
			rows.addAll(comparison.rows());
		}


		private void setOnlyDifferences(final boolean only){
			rows.clear();
			for(final IndividualFacts.Row row : comparison.rows())
				if(!only || row.state() != IndividualFacts.State.SAME)
					rows.add(row);
			fireTableDataChanged();
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return 1 + comparison.individuals().size();
		}

		@Override
		public String getColumnName(final int column){
			return (column == 0? StringUtils.EMPTY: comparison.names().get(column - 1));
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final IndividualFacts.Row fact = rows.get(row);
			return (column == 0? fact.label(): fact.values().get(column - 1));
		}

		private IndividualFacts.State stateOf(final int row){
			return rows.get(row).state();
		}

	}


	/** A cell that wraps its text and is colored by the state of its row. */
	private static final class WrapRenderer extends JTextArea implements TableCellRenderer{

		private WrapRenderer(){
			setLineWrap(true);
			setWrapStyleWord(true);
			setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
			setOpaque(true);
		}


		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean selected, final boolean focus,
				final int row, final int column){
			setText(value != null? value.toString(): StringUtils.EMPTY);
			setFont(column == 0? table.getFont().deriveFont(Font.BOLD): table.getFont());

			final IndividualFacts.State state = ((FactsTableModel)table.getModel()).stateOf(row);
			Color background = table.getBackground();
			if(column > 0 && state == IndividualFacts.State.DIFFERENT)
				background = COLOR_DIFFERENT;
			else if(column > 0 && state == IndividualFacts.State.MISSING)
				background = COLOR_MISSING;
			setBackground(selected? table.getSelectionBackground(): background);
			setForeground(selected? table.getSelectionForeground(): table.getForeground());
			return this;
		}

	}

}
