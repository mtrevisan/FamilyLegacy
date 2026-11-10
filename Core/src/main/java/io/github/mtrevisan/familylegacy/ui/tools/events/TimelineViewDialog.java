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
import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.events.EventTimeline;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


/**
 * Dialog that lists the events in chronological order, with filters by date interval, participant and type of event.
 * <p>
 * This is not a graphical timeline: a table is more useful for genealogical work, because it supports sorting, filtering and
 * copying. A graphical timeline would be a separate projection, not a tool.
 * <p>
 * The events are placed by the first day their date can be, so an event with a partial date (a year, a month, an interval) is in
 * its place and not at the end; the events with no usable date come last. When a date interval is asked for, the events with no
 * usable date cannot be placed and are not listed: the status line says how many they are. If an individual is selected in the
 * view when the dialog opens, the list is the one of that individual.
 */
public final class TimelineViewDialog extends JDialog{

	private final ToolContext context;
	private final TimelineTableModel tableModel = new TimelineTableModel();
	private final JTable table = new JTable(tableModel);
	private final JTextField fromField = dateField();
	private final JTextField toField = dateField();
	private final JComboBox<String> typeCombo = new JComboBox<>();
	private final JTextField participantField = new JTextField(StringUtils.EMPTY, 22);
	private final JLabel statusLabel = new JLabel(StringUtils.SPACE);
	private final JButton openButton = new JButton(I18N.t("button.open_record"));

	private String participantId;


	public TimelineViewDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.timeline_view.title"), ModalityType.APPLICATION_MODAL);

		this.context = context;

		loadTypes();
		selectParticipant(selectedIndividual());

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


	private static JTextField dateField(){
		final JTextField field = new JTextField(StringUtils.EMPTY, 10);
		field.putClientProperty("JTextField.placeholderText", I18N.t("text.timeline.placeholder"));
		field.setToolTipText(I18N.t("text.timeline.placeholder"));
		return field;
	}

	private JPanel createToolbar(){
		final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
		toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

		toolbar.add(new JLabel(I18N.l("label.from")));
		toolbar.add(fromField);
		fromField.getDocument()
			.addDocumentListener(refreshListener());

		toolbar.add(new JLabel(I18N.l("label.to")));
		toolbar.add(toField);
		toField.getDocument()
			.addDocumentListener(refreshListener());

		toolbar.add(new JLabel(I18N.l("dialog.event.type")));
		toolbar.add(typeCombo);
		typeCombo.addActionListener(e -> reload());

		toolbar.add(new JLabel(I18N.l("label.participant")));
		participantField.setEditable(false);
		toolbar.add(participantField);

		final JButton chooseParticipant = new JButton(I18N.t("button.choose"));
		chooseParticipant.addActionListener(e -> chooseParticipant());
		toolbar.add(chooseParticipant);

		final JButton clearParticipant = new JButton(I18N.t("button.clear"));
		clearParticipant.addActionListener(e -> {
			selectParticipant(null);
			reload();
		});
		toolbar.add(clearParticipant);

		return toolbar;
	}

	private JScrollPane createTable(){
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(22);
		table.setAutoCreateRowSorter(true);
		final TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(140);
		columnModel.getColumn(1).setPreferredWidth(110);
		columnModel.getColumn(2).setPreferredWidth(260);
		columnModel.getColumn(3).setPreferredWidth(260);
		columnModel.getColumn(4).setPreferredWidth(280);

		table.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(final MouseEvent e){
				if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e))
					openSelected();
			}
		});
		table.getSelectionModel()
			.addListSelectionListener(e -> openButton.setEnabled(table.getSelectedRow() >= 0));

		final JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createTitledBorder(I18N.t("border.timeline")));
		return scroll;
	}

	private JPanel createFooter(){
		openButton.setEnabled(false);
		openButton.addActionListener(e -> openSelected());

		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		footer.setBorder(BorderFactory.createEmptyBorder(2, 6, 6, 6));
		footer.add(statusLabel);
		footer.add(openButton);
		footer.add(ToolDialogs.createCopyButton(tableModel::toText));
		return footer;
	}


	private DocumentListener refreshListener(){
		return new DocumentListener(){
			@Override
			public void insertUpdate(final DocumentEvent e){
				reload();
			}

			@Override
			public void removeUpdate(final DocumentEvent e){
				reload();
			}

			@Override
			public void changedUpdate(final DocumentEvent e){
				reload();
			}
		};
	}


	/* ======================================================================
	 *                          Reload and filter
	 * ====================================================================== */

	private void loadTypes(){
		//the types in use, "Birth" and "birth" being the same
		final Map<String, String> types = new TreeMap<>();
		for(final FLEFRecord event : EventHelper.listAllEvents(context.model())){
			final String type = EventHelper.eventType(event);
			if(StringUtils.isNotBlank(type))
				types.putIfAbsent(type.toLowerCase(Locale.ROOT), type);
		}

		typeCombo.addItem(I18N.t("search.combo.any"));
		types.values()
			.forEach(typeCombo::addItem);
	}

	private void reload(){
		final FLEFModel model = context.model();

		final LocalDate from = parseBound(fromField, false);
		final LocalDate to = parseBound(toField, true);
		final String type = (typeCombo.getSelectedIndex() > 0? (String)typeCombo.getSelectedItem(): null);

		final EventTimeline.Result result = EventTimeline.build(model, from, to, participantId, type);

		final List<TimelineRow> rows = new ArrayList<>(result.entries().size());
		for(final EventTimeline.Entry entry : result.entries()){
			final FLEFRecord event = entry.event();
			final EventTimeline.DayRange range = entry.range();
			rows.add(new TimelineRow(event.getId(),
				new DateCell(StringUtils.defaultString(EventHelper.eventDateRaw(event)), (range != null? range.sortKey(): Long.MAX_VALUE)),
				StringUtils.defaultString(EventHelper.eventType(event)),
				StringUtils.defaultString(EventSummary.placeName(event, model)),
				StringUtils.defaultString(EventSummary.participantNames(event, model)),
				StringUtils.defaultString(EventHelper.eventDescription(event))));
		}

		tableModel.setRows(rows);
		updateStatus(rows.size(), result.undatedHidden());
	}

	/**
	 * Reads a limit of the interval: a year ({@code 1850}), a month ({@code 1850-03}) or a day ({@code 1850-03-02}). The start of the
	 * period is taken for the lower limit, the end for the upper one. A text that is not a date does not limit the list and is
	 * written in the color of the errors.
	 */
	private static LocalDate parseBound(final JTextField field, final boolean upper){
		field.setForeground(UIManager.getColor("TextField.foreground"));
		field.setToolTipText(I18N.t("text.timeline.placeholder"));

		final String text = field.getText().trim();
		if(text.isEmpty())
			return null;

		try{
			return LocalDate.parse(text);
		}
		catch(final DateTimeParseException ignored){
			//not a day
		}
		try{
			final YearMonth month = YearMonth.parse(text);
			return (upper? month.atEndOfMonth(): month.atDay(1));
		}
		catch(final DateTimeParseException ignored){
			//not a month
		}
		try{
			final Year year = Year.parse(text);
			return (upper? year.atDay(year.length()): year.atDay(1));
		}
		catch(final DateTimeParseException ignored){
			//not a year
		}

		field.setForeground(ToolDialogs.ERROR_COLOR);
		field.setToolTipText(I18N.tf("text.timeline.invalid_date", text));
		return null;
	}

	private FLEFRecord selectedIndividual(){
		final String id = context.selectedEntityId();
		final FLEFRecord record = context.model().getRecordById(id);
		return (record != null && IndividualHandler.isOfType(record)? record: null);
	}

	private void selectParticipant(final FLEFRecord individual){
		participantId = (individual != null? individual.getId(): null);
		participantField.setText(individual != null
			? HandlerRegistry.getHandler(IndividualHandler.TYPE).getDisplayText(individual, context.model())
			: StringUtils.EMPTY);
	}

	private void chooseParticipant(){
		final FLEFRecord[] chosen = new FLEFRecord[1];
		@SuppressWarnings("unchecked")
		final RecordSelectionDialog dialog = RecordSelectionDialog.create(
			this, context.model(),
			(record, handler) -> chosen[0] = record,
			IndividualHandler.class);
		dialog.setVisible(true);

		if(chosen[0] != null){
			selectParticipant(chosen[0]);

			reload();
		}
	}

	private void openSelected(){
		final int viewRow = table.getSelectedRow();
		if(viewRow < 0)
			return;

		final String eventId = tableModel.rows.get(table.convertRowIndexToModel(viewRow))
			.id();
		ToolDialogs.openRecordEditor(this, context.model(), context.model().getRecordById(eventId));

		reload();
	}

	private void updateStatus(final int count, final int undatedHidden){
		final String text = I18N.tf("status.timeline", count);
		statusLabel.setText(undatedHidden > 0? text + " · " + I18N.tf("text.timeline.undated_hidden", undatedHidden): text);
	}


	/** The date of a row: the text of the date, ordered by the first day the date can be. */
	private record DateCell(String text, long sortKey) implements Comparable<DateCell>{

		@Override
		public int compareTo(final DateCell other){
			return Long.compare(sortKey, other.sortKey);
		}

		@Override
		public String toString(){
			return text;
		}

	}

	private record TimelineRow(String id, DateCell date, String type, String place, String participants, String description){}


	private static final class TimelineTableModel extends AbstractTableModel{

		private static final String[] COLUMNS = {I18N.t("column.date"), I18N.t("column.type"), I18N.t("column.place"),
			I18N.t("column.participants"), I18N.t("column.description")};

		private final List<TimelineRow> rows = new ArrayList<>();

		void setRows(final List<TimelineRow> rows){
			this.rows.clear();
			this.rows.addAll(rows);
			fireTableDataChanged();
		}

		/** @return	The rows, as tab-separated lines (the titles first), for the clipboard. */
		String toText(){
			final StringBuilder sb = new StringBuilder(String.join("\t", COLUMNS))
				.append(StringUtils.LF);
			for(final TimelineRow row : rows)
				sb.append(row.date()).append('\t')
					.append(row.type()).append('\t')
					.append(row.place()).append('\t')
					.append(row.participants()).append('\t')
					.append(row.description())
					.append(StringUtils.LF);
			return sb.toString();
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
		public Class<?> getColumnClass(final int column){
			return (column == 0? DateCell.class: String.class);
		}

		@Override
		public Object getValueAt(final int rowIndex, final int columnIndex){
			final TimelineRow row = rows.get(rowIndex);
			return switch(columnIndex){
				case 0 -> row.date();
				case 1 -> row.type();
				case 2 -> row.place();
				case 3 -> row.participants();
				case 4 -> row.description();
				default -> StringUtils.EMPTY;
			};
		}

	}

}
