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
package io.github.mtrevisan.familylegacy.ui.components.projections;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.ModelHistory;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.RecordDiffDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolDialogs;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Non-modal panel listing the changes made to the model in this session, the most recent first, one row per touched record.
 * <p>
 * Each row shows the readable name of the record (as it was at the time of the change, so removed records are named too), and the
 * list can be restricted to a record type. Steps that have been undone are shown greyed. Double-clicking a row (or selecting it
 * and pressing "Open record") opens the editor of the record, if it still exists. The list is refreshed whenever the window is
 * activated.
 */
final class RecentChangesDialog extends JDialog{

	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
		.withZone(ZoneId.systemDefault());
	private static final Color COLOR_UNDONE = Color.GRAY;

	private final Supplier<FLEFModel> modelSupplier;
	private final Supplier<List<ModelHistory.Entry>> historySupplier;

	private final ChangesTableModel tableModel = new ChangesTableModel();
	private final JTable table = new JTable(tableModel);
	private final JLabel summary = new JLabel();
	private final JComboBox<TypeItem> typeFilter = new JComboBox<>();
	private final JButton openRecord = new JButton(I18N.t("recent_changes.open_record"));
	private final JButton showDiff = new JButton(I18N.t("recent_changes.show_diff"));

	private final List<Row> allRows = new ArrayList<>();
	private boolean updatingFilter;


	RecentChangesDialog(final Window owner, final Supplier<FLEFModel> modelSupplier,
			final Supplier<List<ModelHistory.Entry>> historySupplier){
		super(owner, I18N.t("recent_changes.title"), ModalityType.MODELESS);
		ToolDialogs.installEscapeToClose(this);

		this.modelSupplier = modelSupplier;
		this.historySupplier = historySupplier;

		table.setRowHeight(22);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final int[] widths = {80, 90, 160, 260, 90};
		for(int i = 0; i < widths.length; i ++)
			table.getColumnModel()
				.getColumn(i)
				.setPreferredWidth(widths[i]);
		table.setDefaultRenderer(Object.class, new ChangeRenderer());
		table.getSelectionModel()
			.addListSelectionListener(e -> {
				final Row selected = tableModel.rowAt(table.getSelectedRow());
				openRecord.setEnabled(isOpenable(selected));
				showDiff.setEnabled(selected != null);
			});
		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && table.getSelectedRow() >= 0)
					openRecord(tableModel.rowAt(table.getSelectedRow()));
			}
		});

		typeFilter.addActionListener(e -> {
			if(!updatingFilter)
				applyFilter();
		});

		openRecord.setEnabled(false);
		openRecord.addActionListener(e -> openRecord(tableModel.rowAt(table.getSelectedRow())));
		showDiff.setEnabled(false);
		showDiff.addActionListener(e -> showDiff(tableModel.rowAt(table.getSelectedRow())));
		final JButton refresh = new JButton(I18N.t("recent_changes.refresh"));
		refresh.addActionListener(e -> refresh());
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());

		final JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		filterPanel.add(new JLabel(I18N.t("recent_changes.filter.type")));
		filterPanel.add(typeFilter);

		final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		buttons.add(showDiff);
		buttons.add(openRecord);
		buttons.add(refresh);
		buttons.add(close);

		final JPanel south = new JPanel(new BorderLayout());
		south.add(summary, BorderLayout.NORTH);
		south.add(buttons, BorderLayout.SOUTH);

		setLayout(new BorderLayout(8, 8));
		add(filterPanel, BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(south, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter(){
			@Override
			public void windowActivated(final WindowEvent e){
				refresh();
			}
		});

		setPreferredSize(new Dimension(760, 450));
		pack();
		setLocationRelativeTo(owner);

		refresh();
	}


	void refresh(){
		final FLEFModel model = modelSupplier.get();

		allRows.clear();
		for(final ModelHistory.Entry entry : historySupplier.get())
			for(final ModelHistory.Change change : entry.changes())
				allRows.add(new Row(entry, change, typeLabel(change), recordName(change, model)));

		//rebuild the filter, keeping the current selection if still available
		final TypeItem selected = (TypeItem)typeFilter.getSelectedItem();
		final Map<String, TypeItem> types = new LinkedHashMap<>();
		for(final Row row : allRows)
			types.putIfAbsent(row.change().tag(), new TypeItem(row.change().tag(), row.typeLabel()));
		final List<TypeItem> items = new ArrayList<>(types.values());
		items.sort(Comparator.comparing(TypeItem::label));

		updatingFilter = true;
		try{
			typeFilter.removeAllItems();
			typeFilter.addItem(TypeItem.ALL);
			items.forEach(typeFilter::addItem);
			typeFilter.setSelectedItem(selected != null && types.containsKey(selected.tag())? selected: TypeItem.ALL);
		}
		finally{
			updatingFilter = false;
		}

		applyFilter();
	}

	private void applyFilter(){
		final TypeItem selected = (TypeItem)typeFilter.getSelectedItem();
		final List<Row> rows = new ArrayList<>();
		for(final Row row : allRows)
			if(selected == null || selected.tag() == null || selected.tag().equals(row.change().tag()))
				rows.add(row);
		tableModel.setRows(rows);

		summary.setText(I18N.plural("recent_changes.summary", rows.size()));
		openRecord.setEnabled(false);
		showDiff.setEnabled(false);
	}

	private static String typeLabel(final ModelHistory.Change change){
		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(change.tag());
		if(handler != null)
			return handler.getLabel();
		return (change.id() == null? I18N.t("recent_changes.type.header"): change.tag());
	}

	/** @return	The readable name of the record as it was at the time of the change. */
	private static String recordName(final ModelHistory.Change change, final FLEFModel model){
		if(change.id() == null)
			return "-";

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(change.tag());
		if(handler != null){
			try{
				final String text = handler.getDisplayText(change.record(), model);
				if(text != null && !text.isBlank())
					return text;
			}
			catch(final RuntimeException ignored){
				//the snapshot could not be rendered: fall back to the id
			}
		}
		return change.id();
	}

	private boolean isOpenable(final Row row){
		return (row != null && row.change().id() != null && row.change().kind() != ModelHistory.Kind.REMOVED);
	}

	/** Shows the record as it was before and after the change, side by side (read-only). */
	private void showDiff(final Row row){
		if(row == null)
			return;

		RecordDiffDialog.showComparison(this, I18N.tf("recent_changes.diff_title", row.typeLabel(), row.name()),
			row.change().before(), row.change().after());
	}

	private void openRecord(final Row row){
		if(!isOpenable(row))
			return;

		final FLEFModel model = modelSupplier.get();
		final FLEFRecord record = model.getRecordById(row.change().id());
		if(record == null){
			JOptionPane.showMessageDialog(this, I18N.tf("recent_changes.record_missing", row.change().id()),
				getTitle(), JOptionPane.WARNING_MESSAGE);
			return;
		}

		final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
		if(handler == null){
			JOptionPane.showMessageDialog(this, I18N.tf("recent_changes.no_editor", record.getTag()),
				getTitle(), JOptionPane.WARNING_MESSAGE);
			return;
		}

		final BaseRecordDialog dialog = handler.createEditDialog(this, model, record);
		dialog.setVisible(true);
		refresh();
	}


	private record Row(ModelHistory.Entry entry, ModelHistory.Change change, String typeLabel, String name){}

	/** An entry of the type filter ({@code tag} is {@code null} for "all types"). */
	private record TypeItem(String tag, String label){

		static final TypeItem ALL = new TypeItem(null, I18N.t("recent_changes.filter.all"));


		@Override
		public String toString(){
			return label;
		}

	}

	private static final class ChangesTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("recent_changes.column.time"), I18N.t("recent_changes.column.change"),
			I18N.t("recent_changes.column.type"), I18N.t("recent_changes.column.record"),
			I18N.t("recent_changes.column.status")};

		private final List<Row> rows = new ArrayList<>();


		void setRows(final List<Row> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		Row rowAt(final int index){
			return (index >= 0 && index < rows.size()? rows.get(index): null);
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
			return switch(columnIndex){
				case 0 -> TIME_FORMATTER.format(row.entry().time());
				case 1 -> I18N.t("recent_changes.kind." + row.change().kind().name().toLowerCase(Locale.ROOT));
				case 2 -> row.typeLabel();
				case 3 -> row.name();
				default -> I18N.t(row.entry().undone()? "recent_changes.status.undone": "recent_changes.status.applied");
			};
		}

	}

	private final class ChangeRenderer extends DefaultTableCellRenderer{

		@Override
		public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected,
				final boolean hasFocus, final int row, final int column){
			final Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			final Row data = tableModel.rowAt(row);
			if(!isSelected)
				component.setForeground(data != null && data.entry().undone()? COLOR_UNDONE: table.getForeground());
			return component;
		}

	}

}
