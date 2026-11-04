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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.helpers.GUIHelper;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Lists the events that use a given event type (event, date, place, description). A row can be opened (double-click, or
 * "Open record") to edit the event.
 */
final class EventsOfTypeDialog extends JDialog{

	private final FLEFModel model;
	private final JTable table;
	private final JButton openRecord = new JButton(I18N.t("button.open_record"));


	EventsOfTypeDialog(final Window owner, final FLEFModel model, final String type){
		super(owner, I18N.tf("dialog.events_of_type.title", type), ModalityType.APPLICATION_MODAL);
		ToolDialogs.installEscapeToClose(this);
		this.model = model;

		final List<Object[]> rows = new ArrayList<>();
		for(final FLEFRecord event : EventHelper.listAllEvents(model))
			if(type.equalsIgnoreCase(EventHelper.eventType(event)))
				rows.add(new Object[]{event.getId(), EventHelper.eventDateRaw(event), EventHelper.eventPlaceId(event),
					EventHelper.eventDescription(event)});
		final RowTableModel tableModel = new RowTableModel(new String[]{I18N.t("column.event"), I18N.t("column.date"),
			I18N.t("column.place"), I18N.t("column.description")}, rows);
		table = ToolDialogs.createTable(tableModel, row -> openRow(row));
		final TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(100);
		columns.getColumn(1).setPreferredWidth(140);
		columns.getColumn(2).setPreferredWidth(140);
		columns.getColumn(3).setPreferredWidth(380);
		table.getSelectionModel()
			.addListSelectionListener(e -> openRecord.setEnabled(table.getSelectedRow() >= 0));

		final boolean empty = rows.isEmpty();
		final JPanel content = ToolDialogs.createHeaderPanel((empty
			? I18N.t("text.events_of_type.empty")
			: I18N.tf("text.events_of_type.verdict", rows.size(), type)), null);
		if(!empty)
			content.add(new JScrollPane(table), BorderLayout.CENTER);
		content.setPreferredSize(new Dimension(760, empty? 90: 440));

		openRecord.setEnabled(false);
		openRecord.addActionListener(e -> {
			if(table.getSelectedRow() >= 0)
				openRow(table.convertRowIndexToModel(table.getSelectedRow()));
		});

		setLayout(new BorderLayout(8, 8));
		add(content, BorderLayout.CENTER);
		add(ToolDialogs.createFooter(GUIHelper.format(I18N.t("text.events_of_type.summary"), type), openRecord,
			ToolDialogs.createCopyButton(tableModel::toText), ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}


	private void openRow(final int modelRow){
		final String eventId = (String)((RowTableModel)table.getModel()).rowAt(modelRow)[0];
		ToolDialogs.openRecordEditor(this, model, model.getRecordById(eventId));
	}

}
