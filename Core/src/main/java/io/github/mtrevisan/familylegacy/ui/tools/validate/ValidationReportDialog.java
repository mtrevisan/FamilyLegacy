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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.FLEFValidator;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.api.RecordDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Dialog that shows the report of a validation, grouped by the kind of problem.
 * <p>
 * The problems are in groups ("Circular parentage (2)", "Media files not found (300)"), because a file is corrected one kind of problem at
 * a time, and a kind that has hundreds of rows must not bury the others: the groups are closed when the report opens, so that the
 * kinds that there are, and how many problems each has, are what is seen first, and a group is opened or closed with a click on its
 * line (what was opened stays open when the report is made again). The groups with errors come first, then the ones with warnings only, and in each the smaller before the larger. A filter shows the errors
 * only, or the warnings only. What the grammar finds has no kind of its own, so it is grouped by the type of the record it is about.
 * <p>
 * Each row carries the severity, the message and, when applicable, the record the issue refers to, by what it is called. Rows with a
 * record are clickable: double-clicking them (or selecting them and pressing "Open record") opens the edit dialog for that record. The id
 * is provided by the validator as data, so no parsing of the message is needed here.
 * <p>
 * A detail panel at the bottom shows the full message of the selected row, and a "Re-run" button validates again, in the background and
 * on a copy of the model as it is then, and refreshes the table in place.
 */
public final class ValidationReportDialog extends JDialog{

	/** What the filter shows. */
	private enum Filter{
		ALL("text.validate.filter.all"), ERRORS("text.validate.filter.errors"), WARNINGS("text.validate.filter.warnings");

		private final String key;

		Filter(final String key){
			this.key = key;
		}

		boolean accepts(final FLEFValidator.ValidationError error){
			return switch(this){
				case ALL -> true;
				case ERRORS -> error.severity() == FLEFValidator.Severity.ERROR;
				case WARNINGS -> error.severity() == FLEFValidator.Severity.WARNING;
			};
		}

		@Override
		public String toString(){
			return I18N.t(key);
		}
	}


	private final FLEFModel model;
	private final Supplier<FLEFModel> viewSupplier;
	private final Validation validator;
	private SwingWorker<List<FLEFValidator.ValidationError>, Void> worker;
	/** How far the validation is: shown only while it runs, so that the report can open at once and the user sees it advance. */
	private final JProgressBar progressBar = new JProgressBar(0, 100);
	private final ErrorTableModel tableModel;
	private final JTable table;
	private final JTextArea detail = new JTextArea();
	private final JLabel summary = new JLabel();
	private final JComboBox<Filter> filter = new JComboBox<>(Filter.values());
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));
	private final JButton copy = new JButton(I18N.t("button.copy_all"));
	private final JButton rerun = new JButton(I18N.t("button.rerun"));
	private final JButton close = new JButton(I18N.t("button.close"));


	/** Validates a copy of the model, telling how far it got. */
	@FunctionalInterface
	public interface Validation{

		/**
		 * @param view	A copy of the model that nobody changes.
		 * @param progress	Told, from time to time, the share of the work done (0 to 1); may be called from the thread of the validation.
		 * @return	What was found.
		 */
		List<FLEFValidator.ValidationError> validate(FLEFModel view, DoubleConsumer progress);
	}


	/**
	 * The report of a validation that is still to be made: the dialog opens at once, with a progress bar, and shows what the validation
	 * finds when it is done. It is what the user sees of a long validation: where it is, and that something is happening.
	 *
	 * @param owner	The window that owns the dialog.
	 * @param model	The model of the document, that the records are opened in.
	 * @param viewSupplier	Gives a copy of the model as it is now, that nobody changes (call it on the event dispatch thread).
	 * @param validation	Validates the copy: it is called away from the event dispatch thread.
	 */
	public static ValidationReportDialog validating(final Window owner, final FLEFModel model, final Supplier<FLEFModel> viewSupplier,
			final Validation validation){
		return new ValidationReportDialog(owner, model, null, viewSupplier, validation);
	}

	/**
	 * @param owner	The window that owns the dialog.
	 * @param model	The model of the document, that the records are opened in.
	 * @param errors	What the validation found.
	 * @param viewSupplier	Gives a copy of the model as it is now, that nobody changes (call it on the event dispatch thread).
	 * @param validator	Validates a copy of the model: it is called away from the event dispatch thread.
	 */
	public ValidationReportDialog(final Window owner, final FLEFModel model, final List<FLEFValidator.ValidationError> errors,
			final Supplier<FLEFModel> viewSupplier, final Function<FLEFModel, List<FLEFValidator.ValidationError>> validator){
		this(owner, model, errors, viewSupplier, (view, progress) -> validator.apply(view));
	}

	private ValidationReportDialog(final Window owner, final FLEFModel model, final List<FLEFValidator.ValidationError> errors,
			final Supplier<FLEFModel> viewSupplier, final Validation validator){
		super(owner, I18N.t("dialog.validate.title"), ModalityType.APPLICATION_MODAL);

		ToolDialogs.installEscapeToClose(this);

		this.model = model;
		this.viewSupplier = viewSupplier;
		this.validator = validator;
		tableModel = new ErrorTableModel(model);
		table = new JTable(tableModel);

		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(80);
		columnModel.getColumn(1).setPreferredWidth(600);
		columnModel.getColumn(2).setPreferredWidth(220);
		table.setDefaultRenderer(Object.class, new ErrorRenderer());
		table.getSelectionModel()
			.addListSelectionListener(e -> onSelectionChanged());
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				final int row = table.rowAtPoint(e.getPoint());
				if(row < 0)
					return;

				//a click on the line of a group opens or closes it, a double click on a problem opens its record
				if(tableModel.isHeader(row)){
					if(e.getClickCount() == 1)
						toggleGroup(row);
				}
				else if(e.getClickCount() == 2)
					openRecord(row);
			}
		});

		detail.setEditable(false);
		detail.setLineWrap(true);
		detail.setWrapStyleWord(true);
		detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		detail.setRows(4);
		detail.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

		filter.addActionListener(e -> {
			tableModel.setFilter((Filter)filter.getSelectedItem());
			detail.setText(StringUtils.EMPTY);
			openRecord.setEnabled(false);
		});
		final JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
		filterBar.add(new JLabel(I18N.t("text.validate.filter")));
		filterBar.add(filter);

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
		detailPanel.setBorder(BorderFactory.createTitledBorder(I18N.t("border.details")));
		detailPanel.add(new JScrollPane(detail), BorderLayout.CENTER);

		progressBar.setStringPainted(true);
		progressBar.setVisible(false);
		final JPanel status = new JPanel(new BorderLayout(8, 0));
		status.add(summary, BorderLayout.CENTER);
		status.add(progressBar, BorderLayout.EAST);

		final JPanel south = new JPanel(new BorderLayout());
		south.add(status, BorderLayout.NORTH);
		south.add(detailPanel, BorderLayout.CENTER);
		south.add(bottomButtons, BorderLayout.SOUTH);

		setLayout(new BorderLayout(8, 8));
		add(filterBar, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		setPreferredSize(new Dimension(900, 620));
		pack();
		setLocationRelativeTo(owner);

		if(errors != null)
			show(errors);
		else
			//the report is open before the validation is done: it shows how far it is
			refresh();
	}

	@Override
	public void dispose(){
		//a validation that nobody waits for any more stops
		if(worker != null)
			worker.cancel(true);

		super.dispose();
	}


	/* ======================================================================
	 *                          Refresh
	 * ====================================================================== */

	/** Validates again, away from the event dispatch thread (the file can be big), on a copy of the model as it is now. */
	private void refresh(){
		//a validation that is running is replaced
		if(worker != null)
			worker.cancel(true);

		rerun.setEnabled(false);
		openRecord.setEnabled(false);
		summary.setText(I18N.t("text.validate.running"));
		progressBar.setValue(0);
		progressBar.setVisible(true);

		final FLEFModel view = viewSupplier.get();
		final SwingWorker<List<FLEFValidator.ValidationError>, Void> current = new SwingWorker<>(){
			@Override
			protected List<FLEFValidator.ValidationError> doInBackground(){
				//setProgress is safe from this thread: the listener below receives the values on the event dispatch thread
				return validator.validate(view, fraction -> setProgress((int)Math.round(fraction * 100.)));
			}

			@Override
			protected void done(){
				if(isCancelled())
					return;

				progressBar.setVisible(false);
				rerun.setEnabled(true);
				try{
					show(get());
				}
				catch(final InterruptedException ex){
					Thread.currentThread().interrupt();
				}
				catch(final ExecutionException ex){
					final Throwable cause = (ex.getCause() != null? ex.getCause(): ex);
					summary.setText(I18N.l("dialog.validate.error") + StringUtils.SPACE + cause.getMessage());
				}
			}
		};
		current.addPropertyChangeListener(event -> {
			if("progress".equals(event.getPropertyName()) && event.getSource() == worker)
				progressBar.setValue((Integer)event.getNewValue());
		});
		worker = current;
		current.execute();
	}

	/** @return	The line above the details: valid, or how many errors and warnings (a file with warnings only is valid). */
	static String summaryOf(final List<FLEFValidator.ValidationError> errors){
		final long warnings = errors.stream()
			.filter(error -> error.severity() == FLEFValidator.Severity.WARNING)
			.count();
		if(errors.isEmpty())
			return I18N.t("text.validate.valid");
		if(warnings == 0)
			return I18N.tf("text.validate.issues", errors.size());
		if(warnings == errors.size())
			return I18N.tf("text.validate.only_warnings", warnings);
		return I18N.tf("text.validate.issues.mixed", errors.size() - warnings, warnings);
	}

	private void show(final List<FLEFValidator.ValidationError> errors){
		tableModel.setErrors(errors);
		summary.setText(summaryOf(errors));
		detail.setText(StringUtils.EMPTY);
		openRecord.setEnabled(false);
	}


	/* ======================================================================
	 *                          Selection and click
	 * ====================================================================== */

	private void toggleGroup(final int row){
		tableModel.toggle(row);
		detail.setText(StringUtils.EMPTY);
		openRecord.setEnabled(false);
	}

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
				I18N.tf("message.record_not_in_model", err.recordId()), I18N.t("dialog.validate.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null){
			JOptionPane.showMessageDialog(this,
				I18N.tf("message.no_editor_for_type", record.getTag()), I18N.t("dialog.validate.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

		final RecordDialog dialog = handler.createEditDialog(this, model, record);
		dialog.setVisible(true);
		if(dialog.isSaved())
			refresh();
	}


	/* ======================================================================
	 *                          Clipboard
	 * ====================================================================== */

	private void copyToClipboard(){
		final StringBuilder sb = new StringBuilder();
		sb.append("=== ")
			.append(I18N.t("text.validate.report_title"))
			.append(" ===")
			.append(StringUtils.LF);
		for(final Group group : tableModel.groups()){
			sb.append(StringUtils.LF)
				.append("== ").append(group.label()).append(" (").append(group.errors().size()).append(") ==")
				.append(StringUtils.LF);
			for(final FLEFValidator.ValidationError e : group.errors()){
				sb.append("  • ")
					.append(e.severity() == FLEFValidator.Severity.WARNING? "[" + I18N.t("text.validate.warning") + "] ": StringUtils.EMPTY)
					.append(e.message());
				if(e.hasRecord())
					sb.append("   [").append(e.recordId()).append(']');
				sb.append(StringUtils.LF);
			}
		}

		final StringSelection sel = new StringSelection(sb.toString());
		Toolkit.getDefaultToolkit()
			.getSystemClipboard()
			.setContents(sel, sel);
	}


	/* ======================================================================
	 *                          Groups
	 * ====================================================================== */

	/**
	 * The problems of one kind.
	 *
	 * @param kind	The key of the group.
	 * @param label	What the group is called.
	 * @param errors	Its problems, in the order they were found.
	 */
	record Group(String kind, String label, List<FLEFValidator.ValidationError> errors){

		/** @return	{@code true} if a problem of the group is an error: it is then an error group, otherwise a warning group. */
		boolean hasErrors(){
			return errors.stream()
				.anyMatch(error -> error.severity() == FLEFValidator.Severity.ERROR);
		}
	}

	/**
	 * Groups the problems by kind. What the grammar finds, which has no kind of its own, is grouped by the type of the record it is about
	 * (or "the file", if it is about no record).
	 *
	 * @param errors	The problems.
	 * @param model	The model, to know the type of a record.
	 * @return	The groups: the ones with errors first, then the ones with warnings only; in each the smaller before the larger, then by name.
	 */
	static List<Group> groupsOf(final List<FLEFValidator.ValidationError> errors, final FLEFModel model){
		final Map<String, List<FLEFValidator.ValidationError>> byKind = new LinkedHashMap<>();
		final Map<String, String> labels = new LinkedHashMap<>();
		for(final FLEFValidator.ValidationError error : errors){
			final String kind;
			final String label;
			if(!error.kind().isEmpty()){
				kind = error.kind();
				label = I18N.t("text.validate.kind." + kind, kind);
			}
			else{
				final FLEFRecord record = (error.hasRecord()? model.getRecordById(error.recordId()): null);
				if(record != null){
					kind = "grammar." + record.getTag().toLowerCase(Locale.ROOT);
					label = I18N.tf("text.validate.kind.grammar", I18N.t("confirmation.exist_record." + record.getTag().toLowerCase(Locale.ROOT),
						record.getTag()));
				}
				else{
					kind = "grammar";
					label = I18N.t("text.validate.kind.file");
				}
			}
			byKind.computeIfAbsent(kind, k -> new ArrayList<>())
				.add(error);
			labels.putIfAbsent(kind, label);
		}

		final List<Group> groups = new ArrayList<>();
		byKind.forEach((kind, list) -> groups.add(new Group(kind, labels.get(kind), list)));
		groups.sort(Comparator.comparing((Group group) -> !group.hasErrors())
			.thenComparingInt(group -> group.errors().size())
			.thenComparing(Group::label));
		return groups;
	}


	/* ======================================================================
	 *                          Table model and renderer
	 * ====================================================================== */

	/** A line of the table: the line of a group, or a problem. */
	private record Row(Group group, FLEFValidator.ValidationError error, boolean collapsed){

		boolean isHeader(){
			return (error == null);
		}
	}

	private static final class ErrorTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.severity"), I18N.t("column.error"), I18N.t("column.record")};

		private final FLEFModel model;
		private List<FLEFValidator.ValidationError> errors = List.of();
		private Filter filter = Filter.ALL;
		/** The kinds that the user opened: every group is closed until then, also the ones that a new validation finds. */
		private final Set<String> opened = new HashSet<>();
		private List<Group> groups = List.of();
		private final List<Row> rows = new ArrayList<>();


		ErrorTableModel(final FLEFModel model){
			this.model = model;
		}


		void setErrors(final List<FLEFValidator.ValidationError> newErrors){
			errors = List.copyOf(newErrors);
			rebuild();
		}

		void setFilter(final Filter newFilter){
			filter = newFilter;
			rebuild();
		}

		/** Opens the group of the line, or closes it if it is open. */
		void toggle(final int row){
			final Row line = rows.get(row);
			if(!opened.remove(line.group().kind()))
				opened.add(line.group().kind());
			rebuild();
		}

		/** @return	The groups that are shown (with the filter applied), also the closed ones. */
		List<Group> groups(){
			return groups;
		}

		boolean isHeader(final int row){
			return (row >= 0 && row < rows.size() && rows.get(row).isHeader());
		}

		/** @return	The problem of the line; {@code null} if it is the line of a group or there is no such line. */
		FLEFValidator.ValidationError errorAt(final int row){
			return (row >= 0 && row < rows.size()? rows.get(row).error(): null);
		}

		private void rebuild(){
			final List<FLEFValidator.ValidationError> shown = errors.stream()
				.filter(filter::accepts)
				.toList();
			groups = groupsOf(shown, model);
			rows.clear();
			for(final Group group : groups){
				final boolean isClosed = !opened.contains(group.kind());
				rows.add(new Row(group, null, isClosed));
				if(!isClosed)
					for(final FLEFValidator.ValidationError error : group.errors())
						rows.add(new Row(group, error, false));
			}
			fireTableDataChanged();
		}

		/** The record by what it is called (a person by name), not only by its identifier. */
		private String recordLabel(final String id){
			final FLEFRecord record = model.getRecordById(id);
			return (record != null? RecordDisplay.describe(record, model): id);
		}

		@Override
		public int getRowCount(){
			return rows.size();
		}

		@Override
		public int getColumnCount(){
			return COLUMNS.length;
		}

		@Override
		public String getColumnName(final int column){
			return COLUMNS[column];
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final Row row = rows.get(rowIndex);
			if(row.isHeader())
				return switch(columnIndex){
					case 0 -> I18N.t(row.group().hasErrors()? "text.validate.error": "text.validate.warning");
					case 1 -> (row.collapsed()? "\u25B6 ": "\u25BC ") + row.group().label() + " (" + row.group().errors().size() + ")";
					default -> StringUtils.EMPTY;
				};

			final FLEFValidator.ValidationError e = row.error();
			return switch(columnIndex){
				case 0 -> I18N.t(e.severity() == FLEFValidator.Severity.WARNING? "text.validate.warning": "text.validate.error");
				case 1 -> e.message();
				case 2 -> (e.hasRecord()? recordLabel(e.recordId()): "\u2014");
				default -> null;
			};
		}

	}


	private final class ErrorRenderer extends DefaultTableCellRenderer{

		private static final Color LINK_COLOR = new Color(0x1565C0);
		private static final Color HEADER_BACKGROUND = new Color(0xE8EAF0);


		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value,
			final boolean isSelected, final boolean hasFocus, final int row, final int column){
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(value != null)
				setToolTipText(value.toString());

			final boolean header = tableModel.isHeader(row);
			setFont(table.getFont().deriveFont(header? Font.BOLD: Font.PLAIN));
			if(isSelected){
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			}
			else{
				setBackground(header? HEADER_BACKGROUND: table.getBackground());
				if(column == 0)
					setForeground(I18N.t("text.validate.warning").equals(value)? ToolDialogs.WARN_COLOR: ToolDialogs.ERROR_COLOR);
				else if(column == 2 && value != null && !"\u2014".equals(value))
					setForeground(LINK_COLOR);
				else
					setForeground(table.getForeground());
			}
			return this;
		}

	}

}
