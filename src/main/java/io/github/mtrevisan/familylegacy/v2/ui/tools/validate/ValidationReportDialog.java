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
package io.github.mtrevisan.familylegacy.v2.ui.tools.validate;

import io.github.mtrevisan.familylegacy.v2.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


/**
 * Dialog that shows the report of a FLEF validation, one row per issue.
 * <p>
 * Each row carries the message and, when applicable, the id of the
 * record the issue refers to. Rows with a record id are clickable:
 * double-clicking them (or selecting them and pressing "Open record")
 * opens the edit dialog for that record. The id is provided by the
 * validator as data, so no parsing of the message is needed here.
 * <p>
 * A detail panel at the bottom shows the full message of the selected
 * row, and a "Re-run" button re-executes the validation and refreshes
 * the table in place.
 */
public final class ValidationReportDialog extends JDialog{

	private final FLEFModel model;
	private final Supplier<List<FLEFValidator.ValidationError>> validator;

	private final ErrorTableModel tableModel = new ErrorTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextArea detail = new JTextArea();
	private final JLabel summary = new JLabel();

	private final JButton openRecord = new JButton("Open record");
	private final JButton copy = new JButton("Copy all");
	private final JButton rerun = new JButton("Re-run");
	private final JButton close = new JButton("Close");


	public ValidationReportDialog(final Window owner, final FLEFModel model,
		final Supplier<List<FLEFValidator.ValidationError>> validator){
		super(owner, "Validate File", ModalityType.APPLICATION_MODAL);
		this.model = model;
		this.validator = validator;

		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getColumnModel().getColumn(0).setPreferredWidth(640);
		table.getColumnModel().getColumn(1).setPreferredWidth(90);
		table.setDefaultRenderer(Object.class, new ErrorRenderer());
		table.getSelectionModel()
			.addListSelectionListener(e -> onSelectionChanged());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && table.getSelectedRow() >= 0)
					openRecord(table.getSelectedRow());
			}
		});

		detail.setEditable(false);
		detail.setLineWrap(true);
		detail.setWrapStyleWord(true);
		detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		detail.setRows(4);
		detail.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		openRecord.setEnabled(false);
		openRecord.addActionListener(e -> {
			if(table.getSelectedRow() >= 0)
				openRecord(table.getSelectedRow());
		});
		copy.addActionListener(e -> copyToClipboard());
		rerun.addActionListener(e -> refresh());
		close.addActionListener(e -> dispose());

		final JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottomButtons.add(openRecord);
		bottomButtons.add(copy);
		bottomButtons.add(rerun);
		bottomButtons.add(close);

		final JPanel detailPanel = new JPanel(new BorderLayout());
		detailPanel.setBorder(BorderFactory.createTitledBorder("Details"));
		detailPanel.add(new JScrollPane(detail), BorderLayout.CENTER);

		final JPanel south = new JPanel(new BorderLayout());
		south.add(summary, BorderLayout.NORTH);
		south.add(detailPanel, BorderLayout.CENTER);
		south.add(bottomButtons, BorderLayout.SOUTH);

		setLayout(new BorderLayout(8, 8));
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(900, 620));
		pack();
		setLocationRelativeTo(owner);

		refresh();
	}


	/* ======================================================================
	 *                          Refresh
	 * ====================================================================== */

	private void refresh(){
		final List<FLEFValidator.ValidationError> errors = validator.get();
		tableModel.setErrors(errors);

		summary.setText(errors.isEmpty()
			? "The file is valid. No errors found."
			: errors.size() + (errors.size() == 1? " issue found": " issues found"));
		detail.setText(StringUtils.EMPTY);
		openRecord.setEnabled(false);
	}


	/* ======================================================================
	 *                          Selection and click
	 * ====================================================================== */

	private void onSelectionChanged(){
		final int row = table.getSelectedRow();
		final FLEFValidator.ValidationError err = tableModel.errorAt(row);
		if(err == null){
			detail.setText(StringUtils.EMPTY);
			openRecord.setEnabled(false);
			return;
		}
		detail.setText(err.message());
		detail.setCaretPosition(0);
		openRecord.setEnabled(err.hasRecord());
	}

	private void openRecord(final int row){
		final FLEFValidator.ValidationError err = tableModel.errorAt(row);
		if(err == null || !err.hasRecord())
			return;

		final FLEFRecord record = model.getRecordById(err.recordId());
		if(record == null){
			JOptionPane.showMessageDialog(this,
				"Record '" + err.recordId() + "' is no longer in the model.",
				"Validate File", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null){
			JOptionPane.showMessageDialog(this,
				"No editor registered for record type '" + record.getTag() + "'.",
				"Validate File", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(this, model, record);
		dialog.setVisible(true);

		if(dialog.isSaved())
			refresh();
	}


	/* ======================================================================
	 *                          Clipboard
	 * ====================================================================== */

	private void copyToClipboard(){
		final StringBuilder sb = new StringBuilder();
		sb.append("=== Validation report ===")
			.append('\n')
			.append('\n');
		for(int i = 0; i < tableModel.getRowCount(); i ++){
			final FLEFValidator.ValidationError e = tableModel.errorAt(i);
			sb.append("  \u2022 ").append(e.message());
			if(e.hasRecord())
				sb.append("   [").append(e.recordId()).append(']');
			sb.append('\n');
		}
		final StringSelection sel = new StringSelection(sb.toString());
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(sel, sel);
	}


	/* ======================================================================
	 *                          Table model and renderer
	 * ====================================================================== */

	private static final class ErrorTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Error", "Record"};
		private final List<FLEFValidator.ValidationError> errors = new ArrayList<>();

		void setErrors(final List<FLEFValidator.ValidationError> newErrors){
			errors.clear();
			errors.addAll(newErrors);
			fireTableDataChanged();
		}

		FLEFValidator.ValidationError errorAt(final int row){
			return (row >= 0 && row < errors.size()? errors.get(row): null);
		}

		@Override public int getRowCount(){ return errors.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final FLEFValidator.ValidationError e = errors.get(rowIndex);
			return switch(columnIndex){
				case 0 -> e.message();
				case 1 -> (e.hasRecord()? e.recordId(): "\u2014");
				default -> null;
			};
		}

	}


	private static final class ErrorRenderer extends DefaultTableCellRenderer{

		private static final Color LINK_COLOR = new Color(0x1565C0);

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(value != null)
				setToolTipText(value.toString());

			if(isSelected){
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else{
				setBackground(table.getBackground());
				if(column == 1 && value != null && !"\u2014".equals(value))
					setForeground(LINK_COLOR);
				else
					setForeground(table.getForeground());
			}
			return this;
		}

	}

}
