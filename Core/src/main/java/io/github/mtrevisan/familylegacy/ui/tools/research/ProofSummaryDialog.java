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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Shows what has been concluded, and how solidly, in two tabs:
 * <ul>
 *   <li><b>Status distribution</b>: how many conclusions there are for each proof status (the declared ones first, even if
 *       empty);</li>
 *   <li><b>Conclusions</b>: every conclusion with its status, issue and narrative, that can be filtered by status and opened
 *       (double-click, or "Open record").</li>
 * </ul>
 */
final class ProofSummaryDialog extends JDialog{

	private final FLEFModel model;
	private final JTabbedPane tabs = new JTabbedPane();
	private final JTable conclusionsTable;
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	/**
	 * @param byStatus	The conclusions for each proof status.
	 * @param statuses	The statuses, in the order in which they have to be shown.
	 */
	ProofSummaryDialog(final Window owner, final FLEFModel model, final Map<String, List<FLEFRecord>> byStatus,
			final List<String> statuses){
		super(owner, I18N.t("dialog.proof_summary.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;

		int total = 0;
		final List<Object[]> distributionRows = new ArrayList<>();
		final List<Object[]> conclusionRows = new ArrayList<>();
		for(final String status : statuses){
			final List<FLEFRecord> conclusions = byStatus.getOrDefault(status, List.of());
			total += conclusions.size();
			distributionRows.add(new Object[]{status, conclusions.size()});
			for(final FLEFRecord conclusion : conclusions)
				conclusionRows.add(new Object[]{status, conclusion.getId(), ResearchHelper.conclusionIssue(conclusion),
					ResearchHelper.firstTextValue(conclusion, ConclusionReader.TAG_NARRATIVE)});
		}

		final RowTableModel distributionModel = new RowTableModel(new String[]{I18N.t("column.status"),
			I18N.t("column.conclusions")}, distributionRows);
		final JTable distributionTable = ToolDialogs.createTable(distributionModel, null);
		distributionTable.setAutoCreateRowSorter(false);
		distributionTable.getColumnModel().getColumn(0).setPreferredWidth(420);
		distributionTable.getColumnModel().getColumn(1).setPreferredWidth(120);

		final RowTableModel conclusionsModel = new RowTableModel(new String[]{I18N.t("column.status"),
			I18N.t("column.conclusion"), I18N.t("column.issue"), I18N.t("column.narrative")}, conclusionRows);
		conclusionsTable = ToolDialogs.createTable(conclusionsModel, row -> openRow(row));
		final TableColumnModel columns = conclusionsTable.getColumnModel();
		columns.getColumn(0).setPreferredWidth(150);
		columns.getColumn(1).setPreferredWidth(100);
		columns.getColumn(2).setPreferredWidth(200);
		columns.getColumn(3).setPreferredWidth(360);
		conclusionsTable.getSelectionModel()
			.addListSelectionListener(e -> updateOpenButton());

		final JPanel distributionTab = (total == 0
			? ToolDialogs.createHeaderPanel(I18N.t("text.proof_summary.empty"), null)
			: ToolDialogs.createHeaderPanel(I18N.tf("text.proof_summary.verdict", total), null));
		if(total > 0)
			distributionTab.add(new JScrollPane(distributionTable), BorderLayout.CENTER);

		tabs.addTab(I18N.t("tab.title.proof_status"), distributionTab);
		tabs.addTab(I18N.t("tab.title.conclusions") + " (" + total + ")", createConclusionsTab(conclusionsTable, statuses));
		tabs.setPreferredSize(new Dimension(840, 500));
		tabs.addChangeListener(e -> updateOpenButton());

		openRecord.addActionListener(e -> {
			if(conclusionsTable.getSelectedRow() >= 0)
				openRow(conclusionsTable.convertRowIndexToModel(conclusionsTable.getSelectedRow()));
		});
		updateOpenButton();

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(GUIHelper.format(I18N.t("text.proof_summary.summary"), total), openRecord,
			ToolDialogs.createCopyButton(conclusionsModel::toText), ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	/** The table of all the conclusions, with a filter on the status above it. */
	private static JPanel createConclusionsTab(final JTable table, final List<String> statuses){
		final String all = I18N.t("text.proof_summary.all_statuses");
		final List<String> options = new ArrayList<>();
		options.add(all);
		options.addAll(statuses);
		final JComboBox<String> filter = new JComboBox<>(options.toArray(new String[0]));
		filter.addActionListener(e -> {
			@SuppressWarnings("unchecked")
			final TableRowSorter<RowTableModel> sorter = (TableRowSorter<RowTableModel>)table.getRowSorter();
			final String selected = (String)filter.getSelectedItem();
			sorter.setRowFilter(all.equals(selected)? null: RowFilter.regexFilter("^" + java.util.regex.Pattern.quote(selected) + "$", 0));
		});

		final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		header.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		header.add(filter);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(header, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

	private void updateOpenButton(){
		openRecord.setEnabled(tabs.getSelectedIndex() == 1 && conclusionsTable.getSelectedRow() >= 0);
	}

	private void openRow(final int modelRow){
		final String id = (String)((RowTableModel)conclusionsTable.getModel()).rowAt(modelRow)[1];
		ToolDialogs.openRecordEditor(this, model, model.getRecordById(id));
	}

}
