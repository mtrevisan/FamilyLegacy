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
package io.github.mtrevisan.familylegacy.ui.tools.consistency;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.quality.ConsistencyService;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Shows the result of the structural consistency check: a verdict, one row for each issue (severity, record, description), and
 * the number of individuals and relationships checked. A row can be opened (double-click, or "Open record") to fix the issue.
 */
final class ConsistencyCheckDialog extends JDialog{

	private final FLEFModel model;
	private final JTable table;
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	ConsistencyCheckDialog(final Window owner, final FLEFModel model, final ConsistencyService.Report report){
		super(owner, I18N.t("dialog.consistency.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;

		final List<Object[]> rows = new ArrayList<>();
		for(final ConsistencyService.Issue issue : report.issues())
			rows.add(new Object[]{issue.severity().name(), issue.recordId(), issue.message()});
		final RowTableModel tableModel = new RowTableModel(new String[]{I18N.t("column.severity"), I18N.t("column.record"),
			I18N.t("column.description")}, rows);
		table = ToolDialogs.createTable(tableModel, row -> openRow(row));
		final TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(90);
		columns.getColumn(1).setPreferredWidth(110);
		columns.getColumn(2).setPreferredWidth(560);
		columns.getColumn(0).setCellRenderer(new SeverityRenderer());
		table.getSelectionModel()
			.addListSelectionListener(e -> openRecord.setEnabled(table.getSelectedRow() >= 0));

		final JPanel content = ToolDialogs.createVerdictPanel(report.isClean(), (report.isClean()
			? I18N.t("text.consistency.ok")
			: I18N.tf("text.consistency.warn", report.issues().size())));
		if(!report.isClean())
			content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.setPreferredSize(new Dimension(820, report.isClean()? 90: 480));

		openRecord.setEnabled(false);
		openRecord.addActionListener(e -> {
			if(table.getSelectedRow() >= 0)
				openRow(table.convertRowIndexToModel(table.getSelectedRow()));
		});

		setLayout(new BorderLayout(8, 8));
		add(content, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(GUIHelper.format(I18N.t("text.consistency.summary"), report.individualsChecked(),
			report.relationshipsChecked()), openRecord, ToolDialogs.createCopyButton(tableModel::toText),
			ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	private void openRow(final int modelRow){
		final String recordId = (String)((RowTableModel)table.getModel()).rowAt(modelRow)[1];
		ToolDialogs.openRecordEditor(this, model, model.getRecordById(recordId));
	}


	/** Colors the severity: red for an error, orange for a warning. */
	private static final class SeverityRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if(!isSelected)
				setForeground(ConsistencyService.Severity.ERROR.name().equals(value)? ToolDialogs.ERROR_COLOR: ToolDialogs.WARN_COLOR);
			return this;
		}

	}

}
