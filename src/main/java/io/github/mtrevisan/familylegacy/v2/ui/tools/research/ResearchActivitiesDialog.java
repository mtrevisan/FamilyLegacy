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
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.ResearchActivityHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Modal dialog that lists every {@code ResearchActivityRecord} with its
 * question, type, status, action, and result.
 * <p>
 * The question titles are resolved through a precomputed index, so the
 * loop over the activities does not scan the whole question list on each
 * row.
 */
public final class ResearchActivitiesDialog extends JDialog{

	private final ToolContext context;
	private final ActivityTableModel tableModel = new ActivityTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(" ");


	public ResearchActivitiesDialog(final ToolContext context){
		super(context.owner(), "Research Activities", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1200, 560));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel("Search:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});

		final JButton newButton = new JButton("New…");
		newButton.addActionListener(e -> openEditor(null));
		toolbar.add(newButton);

		final JButton editButton = new JButton("Edit…");
		editButton.addActionListener(e -> openEditor(selectedId()));
		toolbar.add(editButton);

		final JButton deleteButton = new JButton("Delete");
		deleteButton.addActionListener(e -> deleteSelected());
		toolbar.add(deleteButton);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(240);
		table.getColumnModel().getColumn(1).setPreferredWidth(120);
		table.getColumnModel().getColumn(2).setPreferredWidth(110);
		table.getColumnModel().getColumn(3).setPreferredWidth(280);
		table.getColumnModel().getColumn(4).setPreferredWidth(120);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedId());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Research activities"));
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

		final List<ResearchHelper.ActivityRow> rows = new ArrayList<>();
		for(final FLEFRecord activity : ResearchHelper.listActivities(model))
			rows.add(ResearchHelper.toActivityRow(activity, questionsById));
		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<ActivityTableModel> sorter =
			(TableRowSorter<ActivityTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends ActivityTableModel, ? extends Integer> entry){
					final ResearchHelper.ActivityRow row = tableModel.getRow(entry.getIdentifier());
					return contains(row.questionTitle(), needle)
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

	private String selectedId(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;
		return tableModel.getRow(table.convertRowIndexToModel(viewRow)).id();
	}

	private void openEditor(final String activityId){
		final ResearchActivityHandler handler = ResearchActivityHandler.getInstance();
		final BaseRecordDialog dialog;
		if(activityId == null)
			dialog = handler.createNewDialog(this, context.model());
		else{
			final FLEFRecord record = context.model().getRecordById(activityId);
			if(record == null)
				return;
			dialog = handler.createEditDialog(this, context.model(), record);
		}
		dialog.setVisible(true);
		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final String activityId = selectedId();
		if(activityId == null)
			return;
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Delete research activity " + activityId + "?",
			"Confirm Deletion",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;
		context.model().removeRecord(activityId);
		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " activity": " activities"));
	}


	private static final class ActivityTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Question", "Type", "Status", "Action", "Result"};

		private final List<ResearchHelper.ActivityRow> rows = new ArrayList<>();

		void setRows(final List<ResearchHelper.ActivityRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		ResearchHelper.ActivityRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final ResearchHelper.ActivityRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.questionTitle() != null? row.questionTitle(): row.questionId();
				case 1 -> row.activityType();
				case 2 -> row.status();
				case 3 -> row.action();
				case 4 -> row.result();
				default -> "";
			};
		}
	}

}
