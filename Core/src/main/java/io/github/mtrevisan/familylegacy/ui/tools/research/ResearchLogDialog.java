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
import io.github.mtrevisan.familylegacy.io.model.readers.AuditReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchActivityReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.research.ResearchHelper;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Chronological view of every research activity.
 * <p>
 * Unlike the activity management dialog, this view is ordered by the activity's creation date (the oldest first), as a log is
 * read. Activities without a date are placed at the end, in a stable order, so nothing is silently dropped. A double-click (or
 * "Open record") opens the activity; the log is made again after it has been edited.
 * <p>
 * The question titles are resolved through a precomputed index, and the
 * activities are sorted with a comparator that does not re-query the
 * model.
 */
public final class ResearchLogDialog extends JDialog{

	private final ToolContext context;
	private final LogTableModel tableModel = new LogTableModel();
	private final JTable table = new JTable(tableModel);
	private final SearchBox searchBox = new SearchBox(24, false);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	public ResearchLogDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.research_log.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1200, 600));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		toolbar.add(new JLabel(I18N.l("label.filter")));
		toolbar.add(searchBox);
		searchBox.onChange(this::applyFilter);
		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2)
					openSelected();
			}
		});
		table.getSelectionModel()
			.addListSelectionListener(e -> openRecord.setEnabled(table.getSelectedRow() >= 0));
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(90);
		columnModel.getColumn(1).setPreferredWidth(230);
		columnModel.getColumn(2).setPreferredWidth(100);
		columnModel.getColumn(3).setPreferredWidth(100);
		columnModel.getColumn(4).setPreferredWidth(300);
		columnModel.getColumn(5).setPreferredWidth(120);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.research_log")));
		return scroll;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		openRecord.setEnabled(false);
		openRecord.addActionListener(e -> openSelected());
		footer.add(statusLabel);
		footer.add(openRecord);
		return footer;
	}


	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> questionsById = model.getQuestionsById();

		final List<LogRow> rows = new ArrayList<>();
		for(final FLEFRecord activity : ResearchHelper.listActivities(model)){
			final String questionId = ResearchHelper.firstTextValue(activity, ResearchActivityReader.TAG_QUESTION);
			final FLEFRecord q = (questionId != null? questionsById.get(questionId): null);
			rows.add(new LogRow(activity.getId(), creationDate(activity),
				q != null? ResearchHelper.questionTitle(q): questionId,
				label("type", ResearchHelper.activityType(activity)),
				label("status", ResearchHelper.status(activity)),
				ResearchHelper.action(activity),
				ResearchHelper.activityResult(activity)
			));
		}

		//chronological, the oldest first; the undated ones last, by identifier (the sort is stable, so the order is always the same)
		rows.sort(Comparator
			.comparing((LogRow r) -> r.date() == null, Boolean::compare)
			.thenComparing(r -> r.date() != null? r.date(): StringUtils.EMPTY)
			.thenComparing(LogRow::id));

		tableModel.setRows(rows);
		//(the filter stays in place: the count is the one of the rows shown)
		updateStatus(table.getRowCount());
	}

	/** The date the activity was created ({@code audit.creation.date}), or {@code null}. */
	private static String creationDate(final FLEFRecord activity){
		final FLEFRecord audit = FLEFRecordHelper.findChild(activity, ResearchActivityReader.TAG_AUDIT);
		final FLEFRecord creation = (audit != null? FLEFRecordHelper.findChild(audit, AuditReader.TAG_CREATION): null);
		final String date = (creation != null? ResearchHelper.firstTextValue(creation, AuditReader.TAG_DATE): null);
		return (StringUtils.isNotBlank(date)? date: null);
	}

	/** The value as the user reads it ("in progress"), not as it is written in the file. */
	private static String label(final String kind, final String value){
		return (StringUtils.isBlank(value)? value: I18N.t("enum.research_activity." + kind + "." + value, value));
	}

	private void openSelected(){
		final int selected = table.getSelectedRow();
		if(selected < 0)
			return;

		final LogRow row = tableModel.getRow(table.convertRowIndexToModel(selected));
		ToolDialogs.openRecordEditor(this, context.model(), context.model().getRecordById(row.id()));

		reload();
	}

	private void applyFilter(){
				@SuppressWarnings("unchecked")
		final TableRowSorter<LogTableModel> sorter =
			(TableRowSorter<LogTableModel>)table.getRowSorter();
		if(searchBox.isBlank())
			sorter.setRowFilter(null);
		else{
			final String query = searchBox.query();
			final Set<SearchMode> modes = searchBox.modes();
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends LogTableModel, ? extends Integer> entry){
					final LogRow row = tableModel.getRow(entry.getIdentifier());
					return SearchBox.matches(query, modes, row.date(), row.question(), row.activityType(), row.status(), row.action(),
						row.result());
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private void updateStatus(final int count){
		statusLabel.setText(I18N.tf("text.research_log.entries", count));
	}


	private record LogRow(String id, String date, String question, String activityType, String status, String action, String result){}


	private static final class LogTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.date"), I18N.t("column.question"), I18N.t("column.type"), I18N.t("column.status"), I18N.t("column.action"),
			I18N.t("column.result")};

		private final List<LogRow> rows = new ArrayList<>();

		void setRows(final List<LogRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		LogRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final LogRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.date();
				case 1 -> row.question();
				case 2 -> row.activityType();
				case 3 -> row.status();
				case 4 -> row.action();
				case 5 -> row.result();
				default -> StringUtils.EMPTY;
			};
		}
	}

}
