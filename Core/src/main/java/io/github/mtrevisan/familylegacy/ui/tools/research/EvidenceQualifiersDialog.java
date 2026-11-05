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
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Shows how the evidence qualifiers are distributed, in four tabs: an overview (records with and without qualifiers), and one
 * tab for each of the three dimensions defined by the protocol (source type, information type, evidence type).
 */
final class EvidenceQualifiersDialog extends JDialog{

	private final List<RowTableModel> models = new ArrayList<>();
	private final FLEFModel recordModel;


	EvidenceQualifiersDialog(final Window owner, final FLEFModel recordModel, final Map<String, Integer> sourceTypes,
			final Map<String, Integer> informationTypes, final Map<String, Integer> evidenceTypes, final int withQualifiers,
			final List<FLEFRecord> missing){
		super(owner, I18N.t("dialog.evidence_qualifiers.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);

		this.recordModel = recordModel;
		final int withoutQualifiers = missing.size();

		final JTabbedPane tabs = new JTabbedPane();
		tabs.addTab(I18N.t("tab.title.overview"), createOverviewTab(withQualifiers, withoutQualifiers));
		tabs.addTab(I18N.t("tab.title.source_type"), createDimensionTab(sourceTypes));
		tabs.addTab(I18N.t("tab.title.information_type"), createDimensionTab(informationTypes));
		tabs.addTab(I18N.t("tab.title.evidence_type"), createDimensionTab(evidenceTypes));
		tabs.addTab(I18N.t("tab.title.without_qualifiers") + " (" + withoutQualifiers + ")", createMissingTab(missing));
		tabs.setPreferredSize(new Dimension(640, 420));

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(GUIHelper.format(I18N.t("text.evidence_qualifiers.summary"), withQualifiers, withQualifiers + withoutQualifiers),
			ToolDialogs.createCopyButton(() -> {
				final StringBuilder sb = new StringBuilder();
				for(int i = 0; i < models.size(); i ++)
					sb.append(tabs.getTitleAt(i))
						.append('\n')
						.append(models.get(i).toText())
						.append('\n');
				return sb.toString();
			}), ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	private JPanel createOverviewTab(final int withQualifiers, final int withoutQualifiers){
		final List<Object[]> rows = List.of(
			new Object[]{I18N.t("text.evidence_qualifiers.with"), withQualifiers},
			new Object[]{I18N.t("text.evidence_qualifiers.without"), withoutQualifiers});
		final RowTableModel model = new RowTableModel(new String[]{I18N.t("column.measure"), I18N.t("column.count")}, rows);
		models.add(model);
		final JTable table = ToolDialogs.createTable(model, null);
		table.setAutoCreateRowSorter(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);

		final boolean ok = (withoutQualifiers == 0);
		final JPanel panel = ToolDialogs.createVerdictPanel(ok, (ok
			? I18N.t("text.evidence_qualifiers.verdict.ok")
			: I18N.tf("text.evidence_qualifiers.verdict.warn", withoutQualifiers)));
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

	/** The records that have no qualifiers, by name: they are what is left to assess; a double-click opens one. */
	private JPanel createMissingTab(final List<FLEFRecord> missing){
		final List<Object[]> rows = new ArrayList<>();
		for(final FLEFRecord record : missing)
			rows.add(new Object[]{I18N.t("confirmation.exist_record." + record.getTag().toLowerCase(java.util.Locale.ROOT), record.getTag()),
				RecordDisplay.describe(record, recordModel), record.getId()});
		final RowTableModel model = new RowTableModel(new String[]{I18N.t("column.type"), I18N.t("column.record"), I18N.t("column.id")}, rows);
		models.add(model);

		if(rows.isEmpty())
			return ToolDialogs.createHeaderPanel(I18N.t("text.evidence_qualifiers.verdict.ok"), null);

		final JTable table = ToolDialogs.createTable(model, row -> ToolDialogs.openRecordEditor(this, recordModel,
			recordModel.getRecordById((String)model.rowAt(row)[2])));
		table.getColumnModel().getColumn(0).setPreferredWidth(160);
		table.getColumnModel().getColumn(1).setPreferredWidth(360);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

	private JPanel createDimensionTab(final Map<String, Integer> counts){
		final List<Object[]> rows = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : new TreeMap<>(counts).entrySet())
			rows.add(new Object[]{entry.getKey(), entry.getValue()});
		final RowTableModel model = new RowTableModel(new String[]{I18N.t("column.value"), I18N.t("column.count")}, rows);
		models.add(model);

		if(rows.isEmpty())
			return ToolDialogs.createHeaderPanel(I18N.t("text.evidence_qualifiers.no_value"), null);

		final JTable table = ToolDialogs.createTable(model, null);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);

		final JPanel panel = new JPanel(new BorderLayout());
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		return panel;
	}

}
