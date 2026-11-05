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
package io.github.mtrevisan.familylegacy.ui.tools.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.dates.HistoricalDateAssistantDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.RowTableModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolDialogs;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


/**
 * Dialog that writes a date in every calendar of the converter at once: the user enters the year, month and day in one calendar
 * (Gregorian, Julian, Hijrah, Minguo, Thai Buddhist or Japanese) and the table shows the same day in all of them, with its
 * Julian Day Number and the day of the week.
 * <p>
 * The dates are those of the calendars as they are, without the reform of the Gregorian calendar (the Gregorian and Julian
 * calendars are the proleptic ones). For the calendars used in the sources of the genealogy (Venetan, Florentine, Pisan, the
 * years that begin at the Nativity or at Easter, the liturgical, the regnal, ...) and for the dates that have to be put in a
 * record, the dialog opens the Historical Date Assistant.
 */
final class CalendarConverterDialog extends JDialog{

	private final FLEFModel model;

	private final JComboBox<CalendarConversions.Calendar> sourceCalendarCombo = new JComboBox<>(CalendarConversions.Calendar.values());
	private final JSpinner yearSpinner;
	private final JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 12, 1));
	private final JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 31, 1));
	private final JLabel messageLabel = new JLabel(StringUtils.SPACE);
	private final JTable table;

	private RowTableModel tableModel;


	CalendarConverterDialog(final ToolContext context){
		super(context.owner(), I18N.t("dialog.calendar_converter.title"), ModalityType.APPLICATION_MODAL);

		model = context.model();

		final LocalDate today = LocalDate.now();
		yearSpinner = new JSpinner(new SpinnerNumberModel(today.getYear(), -4000, 9999, 1));
		yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
		monthSpinner.setValue(today.getMonthValue());
		daySpinner.setValue(today.getDayOfMonth());

		tableModel = new RowTableModel(columns(), List.of());
		table = ToolDialogs.createTable(tableModel, null);
		//the rows are in a fixed order, and a sorter would stay bound to the model it was created for
		table.setRowSorter(null);
		table.setAutoCreateRowSorter(false);
		table.setFocusable(false);

		final JButton assistant = new JButton(I18N.t("button.other_calendars"));
		assistant.setToolTipText(I18N.t("tooltip.other_calendars"));
		assistant.addActionListener(e -> new HistoricalDateAssistantDialog(this, HistoricalDateAssistantDialog.Tool.CONVERSION, null, null,
			model, null).setVisible(true));

		setLayout(new BorderLayout(8, 8));
		add(createForm(), BorderLayout.NORTH);
		add(new JScrollPane(table), BorderLayout.CENTER);
		add(ToolDialogs.createFooter(StringUtils.SPACE, assistant, ToolDialogs.createCopyButton(() -> tableModel.toText()),
			ToolDialogs.createCloseButton(this)), BorderLayout.SOUTH);

		setPreferredSize(new Dimension(620, 420));

		ToolDialogs.installEscapeToClose(this);

		pack();
		setLocationRelativeTo(context.owner());

		//the result follows every change of the input
		sourceCalendarCombo.addActionListener(e -> convert());
		yearSpinner.addChangeListener(e -> convert());
		monthSpinner.addChangeListener(e -> convert());
		daySpinner.addChangeListener(e -> convert());

		convert();
	}


	private JPanel createForm(){
		final JPanel form = new JPanel(new MigLayout("insets 8 8 0 8,fillx", "[][grow,fill][][grow,fill][][grow,fill]"));

		form.add(new JLabel(I18N.l("label.source_calendar")));
		form.add(sourceCalendarCombo, "span 5,growx,wrap");

		form.add(new JLabel(I18N.l("label.year")));
		form.add(yearSpinner);
		form.add(new JLabel(I18N.l("label.month")), "gapleft 10");
		form.add(monthSpinner);
		form.add(new JLabel(I18N.l("label.day")), "gapleft 10");
		form.add(daySpinner, "wrap");

		final JLabel note = new JLabel("<html><i>" + I18N.t("text.calendar_converter.note") + "</i></html>");
		form.add(note, "span 6,growx,wrap");

		messageLabel.setForeground(ToolDialogs.ERROR_COLOR);
		form.add(messageLabel, "span 6,growx");
		return form;
	}

	private static String[] columns(){
		return new String[]{I18N.t("column.calendar"), I18N.t("column.date")};
	}


	private void convert(){
		final CalendarConversions.Calendar source = (CalendarConversions.Calendar)sourceCalendarCombo.getSelectedItem();

		final List<Object[]> rows = new ArrayList<>();
		try{
			final long jdn = CalendarConversions.toJdn(source, (int)yearSpinner.getValue(), (int)monthSpinner.getValue(),
				(int)daySpinner.getValue());

			for(final CalendarConversions.Calendar calendar : CalendarConversions.Calendar.values()){
				String date;
				try{
					date = CalendarConversions.fromJdn(calendar, jdn);
				}
				catch(final IllegalArgumentException iae){
					//a calendar that cannot write this day does not hide the others
					date = iae.getMessage();
				}
				rows.add(new Object[]{calendar.label() + (calendar == source? " *": StringUtils.EMPTY), date});
			}
			rows.add(new Object[]{I18N.t("label.julian_day_number"), Long.toString(jdn)});
			rows.add(new Object[]{I18N.t("label.weekday"), CalendarConversions.weekday(jdn)});
			messageLabel.setText(StringUtils.SPACE);
		}
		catch(final IllegalArgumentException iae){
			messageLabel.setText(iae.getMessage());
		}

		tableModel = new RowTableModel(columns(), rows);
		table.setModel(tableModel);
		table.getColumnModel()
			.getColumn(0)
			.setPreferredWidth(180);
	}

}
