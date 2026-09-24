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
package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ReportDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import org.apache.commons.lang3.StringUtils;

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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


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

	private final ToolContext context;
	private final TypeTableModel tableModel = new TypeTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField searchField = new JTextField(20);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);


	public EventTypesDialog(final ToolContext context){
		super(context.owner(), "Manage Event Types", ModalityType.APPLICATION_MODAL);

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

		toolbar.add(new JLabel("Search:"));
		toolbar.add(searchField);
		searchField.getDocument().addDocumentListener(new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void removeUpdate(final DocumentEvent e){ applyFilter(); }
			@Override public void changedUpdate(final DocumentEvent e){ applyFilter(); }
		});

		final JButton showEvents = new JButton("Show events");
		showEvents.addActionListener(e -> showEventsOfSelectedType());
		toolbar.add(showEvents);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(300);
		table.getColumnModel().getColumn(1).setPreferredWidth(100);
		table.getColumnModel().getColumn(2).setPreferredWidth(100);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Event types"));
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
		final Map<String, Integer> counts = new LinkedHashMap<>();

		// Pre-populate with the declared types so the user sees them even
		// when unused.
		for(final String declared : EventHelper.DECLARED_EVENT_TYPES)
			counts.put(declared, 0);

		// Count the events by type. Custom types appear here for the
		// first time.
		final FLEFModel model = context.model();
		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			final String type = EventHelper.eventType(event);
			if(type == null || type.isBlank())
				continue;
			counts.merge(type, 1, Integer::sum);
		}

		final List<TypeRow> rows = new ArrayList<>();
		for(final Map.Entry<String, Integer> entry : counts.entrySet())
			rows.add(new TypeRow(entry.getKey(), entry.getValue(),
				EventHelper.DECLARED_EVENT_TYPES.contains(entry.getKey())));
		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private void applyFilter(){
		final String text = searchField.getText();
		@SuppressWarnings("unchecked")
		final TableRowSorter<TypeTableModel> sorter =
			(TableRowSorter<TypeTableModel>)table.getRowSorter();
		if(text == null || text.isBlank())
			sorter.setRowFilter(null);
		else{
			final String needle = text.trim().toLowerCase(Locale.ROOT);
			sorter.setRowFilter(new RowFilter<>(){
				@Override
				public boolean include(final Entry<? extends TypeTableModel, ? extends Integer> entry){
					final TypeRow row = tableModel.getRow(entry.getIdentifier());
					return row.type().toLowerCase(Locale.ROOT).contains(needle);
				}
			});
		}
		updateStatus(table.getRowCount());
	}

	private void showEventsOfSelectedType(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0){
			JOptionPane.showMessageDialog(this,
				"Select a type first.",
				"Manage Event Types", JOptionPane.WARNING_MESSAGE);
			return;
		}
		final String type = tableModel.getRow(table.convertRowIndexToModel(viewRow)).type();
		final StringBuilder body = new StringBuilder();
		body.append("<h1>Events of type <code>").append(ReportDialog.escape(type)).append("</code></h1>");
		int count = 0;
		body.append("<table>");
		body.append("<tr><th>Event</th><th>Date</th><th>Place</th><th>Description</th></tr>");
		final FLEFModel model = context.model();
		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			if(!type.equals(EventHelper.eventType(event)))
				continue;
			count++;
			body.append("<tr>");
			body.append("<td>").append(ReportDialog.escape(event.getId())).append("</td>");
			final String date = EventHelper.eventDateRaw(event);
			body.append("<td>").append(ReportDialog.escape(date != null? date: StringUtils.EMPTY)).append("</td>");
			final String placeId = EventHelper.eventPlaceId(event);
			body.append("<td>").append(ReportDialog.escape(placeId != null? placeId: StringUtils.EMPTY)).append("</td>");
			final String desc = EventHelper.eventDescription(event);
			body.append("<td>").append(ReportDialog.escape(desc != null? desc: StringUtils.EMPTY)).append("</td>");
			body.append("</tr>");
		}
		body.append("</table>");
		if(count == 0)
			body.append("<p class='hint'>No event uses this type.</p>");

		ReportDialog.showHtml(this, "Events — " + type,
			ReportDialog.document(body.toString()));
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " type": " types"));
	}


	private record TypeRow(String type, int eventCount, boolean declared){}


	private static final class TypeTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Type", "Events", "Declared"};

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
				case 2 -> row.declared()? "yes": "custom";
				default -> StringUtils.EMPTY;
			};
		}
	}

}
