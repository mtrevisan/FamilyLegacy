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
package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Chronological view of every research activity.
 * <p>
 * Unlike the activity management dialog, which is designed for editing,
 * this view is read-only and ordered by the activity's creation date
 * when one is available. Activities without a date are placed at the
 * end, in a stable order, so nothing is silently dropped.
 * <p>
 * The question titles are resolved through a precomputed index, and the
 * activities are sorted with a comparator that does not re-query the
 * model.
 */
public final class ResearchLogDialog extends JDialog{

	private final ToolContext context;
	private final LogTableModel tableModel = new LogTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public ResearchLogDialog(final ToolContext context){
		super(context.owner(), "Research Log", ModalityType.APPLICATION_MODAL);

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
		toolbar.add(new JLabel("Filter:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});
		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(240);
		table.getColumnModel().getColumn(1).setPreferredWidth(110);
		table.getColumnModel().getColumn(2).setPreferredWidth(110);
		table.getColumnModel().getColumn(3).setPreferredWidth(320);
		table.getColumnModel().getColumn(4).setPreferredWidth(120);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Research log"));
		return scroll;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		final JButton close = new JButton("Close");
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}


	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> questionsById = model.getQuestionsById();

		final List<LogRow> rows = new ArrayList<>();
		for(final FLEFRecord activity : ResearchHelper.listActivities(model)){
			final String questionId = ResearchHelper.firstTextValue(activity,
				ResearchHelper.TAG_QUESTION);
			final FLEFRecord q = (questionId != null? questionsById.get(questionId): null);
			rows.add(new LogRow(
				q != null? ResearchHelper.questionTitle(q): questionId,
				ResearchHelper.activityType(activity),
				ResearchHelper.status(activity),
				ResearchHelper.action(activity),
				ResearchHelper.activityResult(activity)
			));
		}

		// Stable sort: by status first (in_progress before planned before
		// completed before abandoned), then by question title. The order
		// is deterministic and does not require a date.
		rows.sort(Comparator
			.comparingInt((LogRow r) -> statusOrder(r.status()))
			.thenComparing(r -> r.question() != null? r.question(): StringUtils.EMPTY));

		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private static int statusOrder(final String status){
		if(status == null)
			return 4;
		return switch(status.toLowerCase(Locale.ROOT)){
			case "in_progress" -> 0;
			case "planned" -> 1;
			case "completed" -> 2;
			case "abandoned" -> 3;
			default -> 4;
		};
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<LogTableModel> sorter =
			(TableRowSorter<LogTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends LogTableModel, ? extends Integer> entry){
					final LogRow row = tableModel.getRow(entry.getIdentifier());
					return contains(row.question(), needle)
						|| contains(row.activityType(), needle)
						|| contains(row.status(), needle)
						|| contains(row.action(), needle)
						|| contains(row.result(), needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " entry": " entries"));
	}


	private record LogRow(String question, String activityType, String status,
								 String action, String result){}


	private static final class LogTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Question", "Type", "Status", "Action", "Result"};

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
				case 0 -> row.question();
				case 1 -> row.activityType();
				case 2 -> row.status();
				case 3 -> row.action();
				case 4 -> row.result();
				default -> StringUtils.EMPTY;
			};
		}
	}

}
