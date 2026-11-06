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
import javax.swing.JLabel;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Shows what has been concluded, and how solidly, in two tabs:
 * <ul>
 *   <li><b>Status distribution</b>: how many conclusions there are for each proof status (the declared ones first, even if
 *       empty);</li>
 *   <li><b>Conclusions</b>: every conclusion with its status, issue and narrative, that can be filtered by status and opened
 *       (double-click, or "Open record"); the report is made again after a conclusion is edited.</li>
 * </ul>
 */
final class ProofSummaryDialog extends JDialog{

	/** The four proof statuses declared by the protocol, in reading order. */
	private static final List<String> DECLARED_STATUSES = List.of(ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE, ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED,
		ConclusionReader.ENUM_PROOF_STATUS_PROVEN, ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN);
	private static final String NOT_SET = "";


	private final FLEFModel model;
	private final JTabbedPane tabs = new JTabbedPane();
	private final JLabel verdict = new JLabel();
	private final JTable distributionTable;
	private final JTable conclusionsTable;
	private final JComboBox<String> statusFilter = new JComboBox<>();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JLabel summary = new JLabel();
	private final JPanel distributionTab = new JPanel(new BorderLayout());


	ProofSummaryDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.proof_summary.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);

		this.model = model;

		distributionTable = ToolDialogs.createTable(new RowTableModel(new String[]{I18N.t("column.status"), I18N.t("column.conclusions")}, List.of()),
			null);
		distributionTable.setAutoCreateRowSorter(false);
		conclusionsTable = ToolDialogs.createTable(new RowTableModel(columns(), List.of()), row -> openRow(row));
		conclusionsTable.getSelectionModel()
			.addListSelectionListener(e -> updateOpenButton());

		final JPanel header = new JPanel(new BorderLayout());
		header.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
		header.add(verdict, BorderLayout.WEST);
		distributionTab.add(header, BorderLayout.NORTH);
		distributionTab.add(new JScrollPane(distributionTable), BorderLayout.CENTER);

		statusFilter.addActionListener(e -> applyFilter());
		final JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		filterBar.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		filterBar.add(statusFilter);
		final JPanel conclusionsTab = new JPanel(new BorderLayout());
		conclusionsTab.add(filterBar, BorderLayout.NORTH);
		conclusionsTab.add(new JScrollPane(conclusionsTable), BorderLayout.CENTER);

		tabs.addTab(I18N.t("tab.title.proof_status"), distributionTab);
		tabs.addTab(I18N.t("tab.title.conclusions"), conclusionsTab);
		tabs.setPreferredSize(new Dimension(840, 500));
		tabs.addChangeListener(e -> updateOpenButton());

		openRecord.addActionListener(e -> {
			if(conclusionsTable.getSelectedRow() >= 0)
				openRow(conclusionsTable.convertRowIndexToModel(conclusionsTable.getSelectedRow()));
		});

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringSpace.EMPTY, openRecord, ToolDialogs.createCopyButton(() -> ((RowTableModel)conclusionsTable.getModel()).toText()),
			ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);

		reload();
	}


	private static String[] columns(){
		return new String[]{I18N.t("column.status"), I18N.t("column.conclusion"), I18N.t("column.issue"), I18N.t("column.narrative")};
	}

	/** The status as the user reads it ("conflicting evidence"), not as it is written in the file. */
	private static String statusLabel(final String status){
		return (NOT_SET.equals(status)? I18N.t("text.proof_summary.not_set"): I18N.t("enum.proof_status." + status, status));
	}

	/** The conclusions for each proof status: the declared ones first (even if empty), then the others found. */
	private Map<String, List<FLEFRecord>> group(){
		final Map<String, List<FLEFRecord>> byStatus = new LinkedHashMap<>();
		for(final String declared : DECLARED_STATUSES)
			byStatus.put(declared, new ArrayList<>());
		for(final FLEFRecord conclusion : ResearchHelper.listConclusions(model)){
			final String status = ResearchHelper.proofStatus(conclusion);
			final String key = (status != null && !status.isBlank()? status.toLowerCase(Locale.ROOT): NOT_SET);
			byStatus.computeIfAbsent(key, k -> new ArrayList<>())
				.add(conclusion);
		}
		return byStatus;
	}

	/** Makes the report again from the model: it is what it is now, also after a conclusion has been edited or deleted. */
	private void reload(){
		final Map<String, List<FLEFRecord>> byStatus = group();

		int total = 0;
		final List<Object[]> distributionRows = new ArrayList<>();
		final List<Object[]> conclusionRows = new ArrayList<>();
		final List<String> options = new ArrayList<>();
		options.add(I18N.t("text.proof_summary.all_statuses"));
		for(final Map.Entry<String, List<FLEFRecord>> entry : byStatus.entrySet()){
			final String label = statusLabel(entry.getKey());
			total += entry.getValue().size();
			distributionRows.add(new Object[]{label, entry.getValue().size()});
			options.add(label);
			for(final FLEFRecord conclusion : entry.getValue())
				conclusionRows.add(new Object[]{label, conclusion.getId(), ResearchHelper.conclusionIssue(conclusion),
					ResearchHelper.firstTextValue(conclusion, ConclusionReader.TAG_NARRATIVE)});
		}

		distributionTable.setModel(new RowTableModel(new String[]{I18N.t("column.status"), I18N.t("column.conclusions")}, distributionRows));
		distributionTable.getColumnModel().getColumn(0).setPreferredWidth(420);
		distributionTable.getColumnModel().getColumn(1).setPreferredWidth(120);
		conclusionsTable.setModel(new RowTableModel(columns(), conclusionRows));
		final TableColumnModel columns = conclusionsTable.getColumnModel();
		columns.getColumn(0).setPreferredWidth(150);
		columns.getColumn(1).setPreferredWidth(100);
		columns.getColumn(2).setPreferredWidth(200);
		columns.getColumn(3).setPreferredWidth(360);

		statusFilter.setModel(new javax.swing.DefaultComboBoxModel<>(options.toArray(new String[0])));
		verdict.setText(total == 0? I18N.t("text.proof_summary.empty"): I18N.tf("text.proof_summary.verdict", total));
		tabs.setTitleAt(1, I18N.t("tab.title.conclusions") + " (" + total + ")");
		summary.setText(GUIHelper.format(I18N.t("text.proof_summary.summary"), total));
		updateOpenButton();
	}

	private void applyFilter(){
		@SuppressWarnings("unchecked")
		final TableRowSorter<RowTableModel> sorter = (TableRowSorter<RowTableModel>)conclusionsTable.getRowSorter();
		final String selected = (String)statusFilter.getSelectedItem();
		sorter.setRowFilter(selected == null || statusFilter.getSelectedIndex() == 0
			? null
			: RowFilter.regexFilter("^" + Pattern.quote(selected) + "$", 0));
	}

	private void updateOpenButton(){
		openRecord.setEnabled(tabs.getSelectedIndex() == 1 && conclusionsTable.getSelectedRow() >= 0);
	}

	private void openRow(final int modelRow){
		final String id = (String)((RowTableModel)conclusionsTable.getModel()).rowAt(modelRow)[1];
		ToolDialogs.openRecordEditor(this, model, model.getRecordById(id));

		reload();
	}


	/** The empty text of a footer. */
	private static final class StringSpace{

		static final String EMPTY = " ";

	}

}
