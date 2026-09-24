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
import io.github.mtrevisan.familylegacy.v2.ui.components.searches.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolDialogs;
import io.github.mtrevisan.familylegacy.v2.ui.tools.places.PlaceHelper;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


/**
 * Dialog that lists every event in chronological order, with filters by
 * date range, participant, and event type.
 * <p>
 * This is not a graphical timeline: a table is more useful for
 * genealogical work, because it supports sorting, filtering, and
 * copying. A graphical timeline would be a separate projection, not a
 * tool.
 * <p>
 * Events with a complete {@code YYYY-MM-DD} date are sorted by that
 * date. Events with a partial or missing date are placed at the end, in
 * a stable order, so they are not silently dropped.
 */
public final class TimelineViewDialog extends JDialog{

	private final ToolContext context;
	private final TimelineTableModel tableModel = new TimelineTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField fromField = new JTextField(StringUtils.EMPTY, 10);
	private final JTextField toField = new JTextField(StringUtils.EMPTY, 10);
	private final JTextField participantField = new JTextField(StringUtils.EMPTY, 14);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);

	private String participantId;


	public TimelineViewDialog(final ToolContext context){
		super(context.owner(), "Timeline View", ModalityType.APPLICATION_MODAL);

		this.context = context;

		setLayout(new BorderLayout(6, 6));
		add(createToolbar(), BorderLayout.NORTH);
		add(createTable(), BorderLayout.CENTER);
		add(createFooter(), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(1100, 600));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		reload();
	}


	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel("From:"));
		toolbar.add(fromField);
		fromField.getDocument().addDocumentListener(refreshListener());

		toolbar.add(new JLabel("To:"));
		toolbar.add(toField);
		toField.getDocument().addDocumentListener(refreshListener());

		toolbar.add(new JLabel("Participant:"));
		participantField.setEditable(false);
		toolbar.add(participantField);

		final JButton chooseParticipant = new JButton("Choose…");
		chooseParticipant.addActionListener(e -> chooseParticipant());
		toolbar.add(chooseParticipant);

		final JButton clearParticipant = new JButton("Clear");
		clearParticipant.addActionListener(e -> {
			participantId = null;
			participantField.setText(StringUtils.EMPTY);
			reload();
		});
		toolbar.add(clearParticipant);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(140);
		table.getColumnModel().getColumn(2).setPreferredWidth(260);
		table.getColumnModel().getColumn(3).setPreferredWidth(220);
		table.getColumnModel().getColumn(4).setPreferredWidth(320);

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder("Timeline"));
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


	private DocumentListener refreshListener(){
		return new DocumentListener(){
			@Override public void insertUpdate(final DocumentEvent e){ reload(); }
			@Override public void removeUpdate(final DocumentEvent e){ reload(); }
			@Override public void changedUpdate(final DocumentEvent e){ reload(); }
		};
	}


	/* ======================================================================
	 *                          Reload and filter
	 * ====================================================================== */

	private void reload(){
		final FLEFModel model = context.model();
		final Map<String, FLEFRecord> placesById = model.getPlacesById();
		final Map<String, List<EventHelper.Participation>> participationsByEvent =
			EventHelper.participationsByEvent(model);

		final LocalDate from = parseDate(fromField.getText());
		final LocalDate to = parseDate(toField.getText());

		final List<TimelineRow> rows = new ArrayList<>();
		for(final FLEFRecord event : EventHelper.listAllEvents(model)){
			final LocalDate date = EventHelper.eventDate(event);
			if(from != null && date != null && date.isBefore(from))
				continue;
			if(to != null && date != null && date.isAfter(to))
				continue;
			if(from != null && date == null)
				continue;
			if(to != null && date == null)
				continue;

			final List<EventHelper.Participation> participants =
				participationsByEvent.getOrDefault(event.getId(), List.of());

			if(participantId != null && !participatesIn(participants, participantId))
				continue;

			final String placeId = EventHelper.eventPlaceId(event);
			final String placeName = (placeId != null && placesById.containsKey(placeId)
				? PlaceHelper.displayName(placesById.get(placeId))
				: placeId);
			rows.add(new TimelineRow(
				date != null? date.toString(): EventHelper.eventDateRaw(event),
				EventHelper.eventType(event),
				describeParticipants(participants),
				placeName,
				EventHelper.eventDescription(event),
				date
			));
		}

		rows.sort(Comparator
			.comparing((TimelineRow r) -> r.sortDate() == null)
			.thenComparing(TimelineRow::sortDate,
				Comparator.nullsLast(Comparator.naturalOrder())));

		tableModel.setRows(rows);
		updateStatus(rows.size());
	}

	private static boolean participatesIn(final List<EventHelper.Participation> participants,
		final String participantId){
		for(final EventHelper.Participation p : participants)
			if(participantId.equals(p.participantId()))
				return true;
		return false;
	}

	private static String describeParticipants(final List<EventHelper.Participation> participants){
		if(participants.isEmpty())
			return StringUtils.EMPTY;
		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < participants.size(); i++){
			if(i > 0)
				sb.append(", ");
			sb.append(participants.get(i).participantId());
		}
		return sb.toString();
	}

	private boolean participatesIn(final FLEFRecord event, final String participantId){
		final FLEFModel model = context.model();
		for(final EventHelper.Participation p : EventHelper.participantsOf(event, model))
			if(participantId.equals(p.participantId()))
				return true;
		return false;
	}

	private String describeParticipants(final FLEFRecord event){
		final FLEFModel model = context.model();
		final List<EventHelper.Participation> participants = EventHelper.participantsOf(event, model);
		if(participants.isEmpty())
			return StringUtils.EMPTY;

		final StringBuilder sb = new StringBuilder();
		for(int i = 0; i < participants.size(); i++){
			if(i > 0)
				sb.append(", ");
			sb.append(participants.get(i).participantId());
		}
		return sb.toString();
	}

	private void chooseParticipant(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		final FLEFModel model = context.model();
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, model,
			(record, handler) -> chosen[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);
		if(chosen[0] != null){
			participantId = chosen[0].getId();
			participantField.setText(participantId);
			reload();
		}
	}

	private static LocalDate parseDate(final String text){
		if(text == null || text.isBlank())
			return null;
		try{
			return LocalDate.parse(text.trim());
		}
		catch(final Exception ignored){
			return null;
		}
	}

	private void updateStatus(final int count){
		statusLabel.setText(count + (count == 1? " event": " events"));
	}


	private record TimelineRow(String dateLabel, String type, String participants,
										String place, String description, LocalDate sortDate){}


	private static final class TimelineTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {"Date", "Type", "Participants", "Place", "Description"};

		private final List<TimelineRow> rows = new ArrayList<>();

		void setRows(final List<TimelineRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		@Override public int getRowCount(){ return rows.size(); }
		@Override public int getColumnCount(){ return COLUMNS.length; }
		@Override public String getColumnName(final int column){ return COLUMNS[column]; }

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final TimelineRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.dateLabel();
				case 1 -> row.type();
				case 2 -> row.participants();
				case 3 -> row.place();
				case 4 -> row.description();
				default -> StringUtils.EMPTY;
			};
		}
	}

}
