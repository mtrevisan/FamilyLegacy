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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.dialogs.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/**
 * Shows how the sources and the documents are used in the file, one tab for each topic:
 * <ul>
 *   <li><b>Unused sources</b>: sources that no record cites;</li>
 *   <li><b>Unused documents</b>: documents that no source or extract references;</li>
 *   <li><b>Uncited assertions</b>: how many records of each type carry no citation.</li>
 * </ul>
 * The rows of the first two tabs can be opened (double-click, or "Open record") to fix the problem on the spot.
 */
final class CitationAnalysisDialog extends JDialog{



	private final FLEFModel model;
	private final List<FLEFRecord> unusedSources;
	private final List<FLEFRecord> unusedDocuments;
	private final Map<String, Integer> uncited;

	private final JTabbedPane tabs = new JTabbedPane();
	private final JTable sourcesTable;
	private final JTable documentsTable;
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	CitationAnalysisDialog(final Window owner, final FLEFModel model, final List<FLEFRecord> unusedSources,
			final List<FLEFRecord> unusedDocuments, final Map<String, Integer> uncited, final int citedSourceCount,
			final int referencedDocumentCount){
		super(owner, I18N.t("dialog.citation_analysis.title"), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;
		this.unusedSources = unusedSources;
		this.unusedDocuments = unusedDocuments;
		this.uncited = uncited;

		sourcesTable = createRecordTable(unusedSources, I18N.t("column.id"), I18N.t("column.title"), I18N.t("column.author"),
			SourceHelper::sourceTitle, SourceHelper::sourceAuthor);
		documentsTable = createRecordTable(unusedDocuments, I18N.t("column.id"), I18N.t("column.uri"),
			I18N.t("column.description"), SourceHelper::documentDisplayUri, SourceHelper::documentDescription);

		tabs.addTab(tabTitle("tab.title.unused_sources", unusedSources.size()),
			ToolDialogs.createVerdictTab(unusedSources.isEmpty(), I18N.t("text.citation_analysis.sources.ok"),
				I18N.tf("text.citation_analysis.sources.warn", unusedSources.size()), sourcesTable));
		tabs.addTab(tabTitle("tab.title.unused_documents", unusedDocuments.size()),
			ToolDialogs.createVerdictTab(unusedDocuments.isEmpty(), I18N.t("text.citation_analysis.documents.ok"),
				I18N.tf("text.citation_analysis.documents.warn", unusedDocuments.size()), documentsTable));
		tabs.addTab(tabTitle("tab.title.uncited_assertions", uncited.values().stream().mapToInt(Integer::intValue).sum()),
			createUncitedTab());
		tabs.setPreferredSize(new Dimension(820, 520));
		tabs.addChangeListener(e -> updateOpenButton());

		sourcesTable.getSelectionModel()
			.addListSelectionListener(e -> updateOpenButton());
		documentsTable.getSelectionModel()
			.addListSelectionListener(e -> updateOpenButton());

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
		footer.add(new JLabel(GUIHelper.format(I18N.t("text.citation_analysis.summary"), citedSourceCount, referencedDocumentCount)),
			BorderLayout.WEST);
		footer.add(buttons, BorderLayout.EAST);

		setLayout(new BorderLayout(8, 8));
		add(tabs, BorderLayout.CENTER);
		add(footer, BorderLayout.SOUTH);

		updateOpenButton();

		pack();
		setLocationRelativeTo(owner);
	}


	private static String tabTitle(final String key, final int count){
		return I18N.t(key) + " (" + count + ")";
	}

	private JPanel createUncitedTab(){
		final JTable table = new JTable(new UncitedTableModel(uncited));
		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(500);
		columns.getColumn(1).setPreferredWidth(120);

		return ToolDialogs.createVerdictTab(uncited.isEmpty(), I18N.t("text.citation_analysis.assertions.ok"),
			I18N.t("text.citation_analysis.assertions.warn"), table);
	}

	private JTable createRecordTable(final List<FLEFRecord> records, final String idColumn, final String firstColumn,
			final String secondColumn, final Function<FLEFRecord, String> first, final Function<FLEFRecord, String> second){
		final JTable table = new JTable(new RecordTableModel(records, new String[]{idColumn, firstColumn, secondColumn}, first, second));
		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(110);
		columns.getColumn(1).setPreferredWidth(420);
		columns.getColumn(2).setPreferredWidth(220);
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

	/** @return	The table of the tab in view, if its rows are records that can be opened. */
	private JTable activeRecordTable(){
		final int index = tabs.getSelectedIndex();
		return (index == 0? sourcesTable: (index == 1? documentsTable: null));
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
	}


	/* ======================================================================
	 *                          Clipboard
	 * ====================================================================== */

	private void copyToClipboard(){
		final StringBuilder sb = new StringBuilder();
		appendSection(sb, tabs.getTitleAt(0), unusedSources, SourceHelper::sourceTitle);
		appendSection(sb, tabs.getTitleAt(1), unusedDocuments, SourceHelper::documentDisplayUri);
		sb.append(tabs.getTitleAt(2))
			.append(StringUtils.LF);
		for(final Map.Entry<String, Integer> e : uncited.entrySet())
			sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append(StringUtils.LF);

		final StringSelection selection = new StringSelection(sb.toString());
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(selection, selection);
	}

	private static void appendSection(final StringBuilder sb, final String title, final List<FLEFRecord> records,
			final Function<FLEFRecord, String> text){
		sb.append(title)
			.append(StringUtils.LF);
		for(final FLEFRecord record : records)
			sb.append("  ").append(record.getId()).append("  ").append(StringUtils.defaultString(text.apply(record)))
				.append(StringUtils.LF);
		sb.append(StringUtils.LF);
	}


	/* ======================================================================
	 *                          Table models
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


	private static final class UncitedTableModel extends AbstractTableModel{

		private final List<Map.Entry<String, Integer>> rows;


		UncitedTableModel(final Map<String, Integer> uncited){
			rows = new ArrayList<>(uncited.entrySet());
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return 2;
		}

		@Override
		public String getColumnName(final int column){
			return (column == 0? I18N.t("column.record_type"): I18N.t("column.uncited_records"));
		}

		@Override
		public Class<?> getColumnClass(final int column){
			return (column == 0? String.class: Integer.class);
		}

		@Override
		public Object getValueAt(final int row, final int column){
			final Map.Entry<String, Integer> entry = rows.get(row);
			return (column == 0? entry.getKey(): entry.getValue());
		}

	}

}
