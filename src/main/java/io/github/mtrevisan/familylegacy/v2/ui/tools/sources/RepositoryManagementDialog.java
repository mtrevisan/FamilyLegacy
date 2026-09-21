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
package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RepositoryHandler;
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


/**
 * Modal dialog that lists every {@code RepositoryRecord} and allows the
 * user to create, edit, and delete repositories.
 */
public final class RepositoryManagementDialog extends JDialog{

	private final ToolContext context;
	private final RepositoryTableModel tableModel = new RepositoryTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(24);
	private final JLabel statusLabel = new JLabel(" ");


	public RepositoryManagementDialog(final ToolContext context){
		super(context.owner(), "Manage Repositories", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(800, 500));

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
		editButton.addActionListener(e -> openEditor(selectedRepositoryId()));
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
		table.getColumnModel().getColumn(0).setPreferredWidth(280);
		table.getColumnModel().getColumn(1).setPreferredWidth(220);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openEditor(selectedRepositoryId());
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Repositories"));
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
		final List<SourceHelper.RepositoryRow> rows = new ArrayList<>();
		for(final FLEFRecord repo : SourceHelper.listAllRepositories(model))
			rows.add(SourceHelper.toRepositoryRow(repo, model));
		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<RepositoryTableModel> sorter =
			(TableRowSorter<RepositoryTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends RepositoryTableModel, ? extends Integer> entry){
					final SourceHelper.RepositoryRow row = tableModel.getRow(entry.getIdentifier());
					return contains(row.name(), needle) || contains(row.place(), needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private static boolean contains(final String haystack, final String needle){
		return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
	}

	private String selectedRepositoryId(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return null;
		return tableModel.getRow(table.convertRowIndexToModel(viewRow)).id();
	}

	private void openEditor(final String repositoryId){
		final RepositoryHandler handler = RepositoryHandler.getInstance();
		final BaseRecordDialog dialog;
		if(repositoryId == null)
			dialog = handler.createNewDialog(this, context.model());
		else{
			final FLEFRecord record = context.model().getRecordById(repositoryId);
			if(record == null)
				return;
			dialog = handler.createEditDialog(this, context.model(), record);
		}
		dialog.setVisible(true);
		if(dialog.isSaved())
			reload();
	}

	private void deleteSelected(){
		final String repositoryId = selectedRepositoryId();
		if(repositoryId == null)
			return;
		final int confirm = JOptionPane.showConfirmDialog(this,
			"Delete repository " + repositoryId + "?",
			"Confirm Deletion",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;
		context.model().removeRecord(repositoryId);
		reload();
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " repository": " repositories"));
	}


	private static final class RepositoryTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Name", "Place", "Sources"};

		private final List<SourceHelper.RepositoryRow> rows = new ArrayList<>();

		void setRows(final List<SourceHelper.RepositoryRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		SourceHelper.RepositoryRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final SourceHelper.RepositoryRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.name();
				case 1 -> row.place();
				case 2 -> row.sourceCount();
				default -> "";
			};
		}
	}

}
