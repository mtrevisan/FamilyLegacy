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
import io.github.mtrevisan.familylegacy.sources.SourceHelper;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;


/**
 * Shows how the sources and the documents are used in the file, one tab for each topic:
 * <ul>
 *   <li><b>Unused sources</b>: sources that no record cites;</li>
 *   <li><b>Unused documents</b>: documents that no source or extract references;</li>
 *   <li><b>Uncited assertions</b>: the records (by name) of the analysed types that carry no citation.</li>
 * </ul>
 * The rows can be opened (double-click, or "Open record") to fix the problem on the spot; the analysis is made again afterwards, so
 * the lists are what the file is now.
 */
final class CitationAnalysisDialog extends JDialog{

	private final FLEFModel model;

	private final JTabbedPane tabs = new JTabbedPane();
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JLabel summary = new JLabel(StringUtils.SPACE);
	private final List<JTable> tables = new ArrayList<>();


	CitationAnalysisDialog(final Window owner, final FLEFModel model){
		super(owner, I18N.t("dialog.citation_analysis.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;

		tabs.setPreferredSize(new Dimension(820, 520));
		tabs.addChangeListener(e -> updateOpenButton());

		openRecord.addActionListener(e -> openSelected());
		final JButton copy = new JButton(I18N.t("button.copy_all"));
		copy.addActionListener(e -> copyToClipboard());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(openRecord);
		buttons.add(copy);
		buttons.add(close);

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


	/** Reads the model again and builds the tabs, keeping the one in view. */
	private void reload(){
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
		final JTable uncitedTable = createRecordTable(uncited, I18N.t("column.id"), I18N.t("column.record_type"),
			I18N.t("column.record"), CitationAnalysisDialog::typeLabel, record -> RecordDisplay.describe(record, model));
		tables.add(sourcesTable);
		tables.add(documentsTable);
		tables.add(uncitedTable);

		tabs.addTab(tabTitle("tab.title.unused_sources", unusedSources.size()),
			ToolDialogs.createVerdictTab(unusedSources.isEmpty(), I18N.t("text.citation_analysis.sources.ok"),
				I18N.tf("text.citation_analysis.sources.warn", unusedSources.size()), sourcesTable));
		tabs.addTab(tabTitle("tab.title.unused_documents", unusedDocuments.size()),
			ToolDialogs.createVerdictTab(unusedDocuments.isEmpty(), I18N.t("text.citation_analysis.documents.ok"),
				I18N.tf("text.citation_analysis.documents.warn", unusedDocuments.size()), documentsTable));
		tabs.addTab(tabTitle("tab.title.uncited_assertions", uncited.size()),
			ToolDialogs.createVerdictTab(uncited.isEmpty(), I18N.t("text.citation_analysis.assertions.ok"),
				I18N.tf("text.citation_analysis.assertions.warn", uncited.size()), uncitedTable));
		tabs.setSelectedIndex(Math.min(selectedTab, tabs.getTabCount() - 1));

		summary.setText(GUIHelper.format(I18N.t("text.citation_analysis.summary"), analysis.citedSourceCount(),
			analysis.referencedDocumentCount()));
		updateOpenButton();
	}

	private static String tabTitle(final String key, final int count){
		return I18N.t(key) + " (" + count + ")";
	}

	/** The type of a record as the user reads it ("individual attribute"), not as it is written in the file. */
	private static String typeLabel(final FLEFRecord record){
		return I18N.t("confirmation.exist_record." + record.getTag().toLowerCase(Locale.ROOT), record.getTag());
	}

	private JTable createRecordTable(final List<FLEFRecord> records, final String idColumn, final String firstColumn,
			final String secondColumn, final Function<FLEFRecord, String> first, final Function<FLEFRecord, String> second){
		final JTable table = new JTable(new RecordTableModel(records, new String[]{idColumn, firstColumn, secondColumn}, first, second));
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(110);
		columns.getColumn(1).setPreferredWidth(420);
		columns.getColumn(2).setPreferredWidth(220);
		table.getSelectionModel()
			.addListSelectionListener(e -> updateOpenButton());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openSelected();
			}
		});
		return table;
	}


	/* ======================================================================
	 *                          Open record
	 * ====================================================================== */

	/** @return	The table of the tab in view. */
	private JTable activeRecordTable(){
		final int index = tabs.getSelectedIndex();
		return (index >= 0 && index < tables.size()? tables.get(index): null);
	}

	private void updateOpenButton(){
		final JTable table = activeRecordTable();
		openRecord.setEnabled(table != null && table.getSelectedRow() >= 0);
	}

	private void openSelected(){
		final JTable table = activeRecordTable();
		if(table == null || table.getSelectedRow() < 0)
			return;

		final FLEFRecord record = ((RecordTableModel)table.getModel()).recordAt(table.convertRowIndexToModel(table.getSelectedRow()));
		ToolDialogs.openRecordEditor(this, model, record);

		reload();
	}


	/* ======================================================================
	 *                          Clipboard
	 * ====================================================================== */

	private void copyToClipboard(){
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < tables.size(); i ++){
			final RecordTableModel records = (RecordTableModel)tables.get(i).getModel();
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

	private static final class RecordTableModel extends AbstractTableModel{

		private final List<FLEFRecord> records;
		private final String[] columns;
		private final Function<FLEFRecord, String> first;
		private final Function<FLEFRecord, String> second;


		RecordTableModel(final List<FLEFRecord> records, final String[] columns, final Function<FLEFRecord, String> first,
				final Function<FLEFRecord, String> second){
			this.records = new ArrayList<>(records);
			this.columns = columns;
			this.first = first;
			this.second = second;
		}

		FLEFRecord recordAt(final int row){
			return records.get(row);
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
			final FLEFRecord record = records.get(row);
			return switch(column){
				case 0 -> record.getId();
				case 1 -> StringUtils.defaultString(first.apply(record));
				default -> StringUtils.defaultString(second.apply(record));
			};
		}

	}

}
