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
package io.github.mtrevisan.familylegacy.ui.tools.events;

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.SearchBox;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Dialog that lists every event type — declared and custom — with the
 * number of events using it.
 * <p>
 * The list is read-only: the protocol declares the vocabulary, and
 * custom types are only recorded. The dialog is useful for auditing
 * which types are actually in use and which declared types are unused,
 * and it lets the user see every event of a given type in one click.
 */
public final class EventTypesDialog extends JDialog{

	//compared without regard to case, like the events of a type are listed
	private static final Set<String> EVENT_TYPES = Arrays.stream(EventReader.TYPES)
		.map(type -> type.toLowerCase(Locale.ROOT))
		.collect(Collectors.toSet());


	private final ToolContext context;
	private final TypeTableModel tableModel = new TypeTableModel();
	private final JTable table = new JTable(tableModel);
	private final SearchBox searchBox = new SearchBox(24, false);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public EventTypesDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.event_types.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(700, 500));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel(I18N.l("label.search")));
		toolbar.add(searchBox);
		searchBox.onChange(this::applyFilter);

		final JButton showEvents = new JButton(I18N.t("button.show_events"));
		showEvents.addActionListener(e -> showEventsOfSelectedType());
		toolbar.add(showEvents);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(300);
		columnModel.getColumn(1).setPreferredWidth(100);
		columnModel.getColumn(2).setPreferredWidth(100);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && table.getSelectedRow() >= 0)
					showEventsOfSelectedType();
			}
		});

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.event_types")));
		return scroll;
	}

	private JPanel createFooter(){
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		final JButton close = new JButton(I18N.t("button.close"));
		close.addActionListener(e -> dispose());
		footer.add(close);
		return footer;
	}


	private void reload(){
		//"Birth" and "birth" are the same type: counted together, shown as first written
		final Map<String, String> labels = new LinkedHashMap<>();
		final Map<String, Integer> counts = new LinkedHashMap<>();

		// Pre-populate with the declared types so the user sees them even
		// when unused.
		for(final String declared : EventReader.TYPES){
			final String key = declared.toLowerCase(Locale.ROOT);
			labels.put(key, declared);
			counts.put(key, 0);
		}

		// Count the events by type. Custom types appear here for the
		// first time.
		final FLEFModel model = context.model();
		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			final String type = EventHelper.eventType(event);
			if(StringUtils.isEmpty(type))
				continue;
			final String key = type.toLowerCase(Locale.ROOT);
			labels.putIfAbsent(key, type);
			counts.merge(key, 1, Integer::sum);
		}

		final List<TypeRow> rows = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : counts.entrySet())
			rows.add(new TypeRow(labels.get(entry.getKey()), entry.getValue(), EVENT_TYPES.contains(entry.getKey())));
		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
				@SuppressWarnings("unchecked")
		final TableRowSorter<TypeTableModel> sorter =
			(TableRowSorter<TypeTableModel>)table.getRowSorter();
		if(searchBox.isBlank())
			sorter.setRowFilter(null);
		else{
			final String query = searchBox.query();
			final Set<SearchMode> modes = searchBox.modes();
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends TypeTableModel, ? extends Integer> entry){
					final TypeRow row = tableModel.getRow(entry.getIdentifier());
					return SearchBox.matches(query, modes, row.type());
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private void showEventsOfSelectedType(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0){
			JOptionPane.showMessageDialog(this,
				I18N.t("message.select_type_first"), I18N.t("dialog.event_types.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String type = tableModel.getRow(table.convertRowIndexToModel(viewRow))
			.type();
		new EventsOfTypeDialog(this, context.model(), type)
			.setVisible(true);
	}

	private void updateStatus(final int count){
		statusLabel.setText(I18N.tf("status.event_types", count));
	}


	private record TypeRow(String type, int eventCount, boolean declared){}


	private static final class TypeTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.type"), I18N.t("column.events"), I18N.t("column.declared")};

		private final List<TypeRow> rows = new ArrayList<>();

		void setRows(final List<TypeRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		TypeRow getRow(final int index){
			return rows.get(index);
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final TypeRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.type();
				case 1 -> row.eventCount();
				case 2 -> I18N.t(row.declared()? "text.event_type.declared": "text.event_type.custom");
				default -> StringUtils.EMPTY;
			};
		}
	}

}
