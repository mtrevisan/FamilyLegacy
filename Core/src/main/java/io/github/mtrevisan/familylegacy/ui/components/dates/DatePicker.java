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
package io.github.mtrevisan.familylegacy.ui.components.dates;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;


/**
 * A calendar that pops up under a text field and writes the day chosen in it as {@code YYYY-MM-DD}: the field stays a plain text field,
 * where the date can still be typed (and, where the field accepts them, as a month or a year).
 * <p>
 * The calendar opens on the month of the date in the field (today if the field is empty or has something else). It is the
 * proleptic Gregorian calendar, the one of the ISO dates.
 * </p>
 */
public final class DatePicker{

	private static final int ISO_DATE_LENGTH = 10;
	private static final int MIN_YEAR = 1;
	private static final int MAX_YEAR = 9999;
	private static final String CALENDAR_SYMBOL = "📅";


	private DatePicker(){}


	/**
	 * @param field	The text field.
	 * @return	The field with the button that opens the calendar at its right.
	 */
	public static JComponent withPicker(final JTextField field){
		final JButton button = new JButton(CALENDAR_SYMBOL);
		button.setMargin(new Insets(1, 4, 1, 4));
		button.setFocusable(false);
		button.setToolTipText(I18N.t("dialog.date_picker.tooltip"));
		button.addActionListener(e -> {
			final JPopupMenu popup = new JPopupMenu();
			popup.add(new Calendar(field, popup, dateOf(field.getText())));
			popup.show(field, 0, field.getHeight());
		});

		final JPanel panel = new JPanel(new BorderLayout(2, 0));
		panel.setOpaque(false);
		panel.add(field, BorderLayout.CENTER);
		panel.add(button, BorderLayout.EAST);
		return panel;
	}

	/** @return	The day the text starts with ({@code YYYY-MM-DD}), or {@code null}. */
	static LocalDate dateOf(final String text){
		final String stripped = StringUtils.strip(text);
		if(StringUtils.isEmpty(stripped))
			return null;

		try{
			return LocalDate.parse(StringUtils.left(stripped, ISO_DATE_LENGTH));
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}


	private static final class Calendar extends JPanel{

		private final JTextField field;
		private final JPopupMenu popup;
		private final LocalDate selected;
		private final JComboBox<String> months = new JComboBox<>();
		private final JSpinner year;
		private final JPanel days = new JPanel(new GridLayout(0, 7, 2, 2));
		private boolean updating;


		private Calendar(final JTextField field, final JPopupMenu popup, final LocalDate selected){
			super(new BorderLayout(4, 4));

			this.field = field;
			this.popup = popup;
			this.selected = selected;

			final LocalDate shown = (selected != null? selected: LocalDate.now());
			for(final Month month : Month.values())
				months.addItem(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
			months.setSelectedIndex(shown.getMonthValue() - 1);
			months.setFocusable(false);
			year = new JSpinner(new SpinnerNumberModel(shown.getYear(), MIN_YEAR, MAX_YEAR, 1));
			year.setEditor(new JSpinner.NumberEditor(year, "#"));
			months.addActionListener(e -> rebuild());
			year.addChangeListener(e -> rebuild());

			final JButton previous = new JButton("◀");
			previous.setFocusable(false);
			previous.addActionListener(e -> move(-1));
			final JButton next = new JButton("▶");
			next.setFocusable(false);
			next.addActionListener(e -> move(1));

			final JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
			header.add(previous);
			header.add(months);
			header.add(year);
			header.add(next);

			final JButton today = new JButton(I18N.t("dialog.date_picker.today"));
			today.setFocusable(false);
			today.addActionListener(e -> choose(LocalDate.now()));
			final JButton clear = new JButton(I18N.t("button.clear"));
			clear.setFocusable(false);
			clear.addActionListener(e -> {
				field.setText(StringUtils.EMPTY);
				popup.setVisible(false);
			});
			final JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
			footer.add(today);
			footer.add(clear);

			setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
			add(header, BorderLayout.NORTH);
			add(days, BorderLayout.CENTER);
			add(footer, BorderLayout.SOUTH);

			rebuild();
		}

		private void move(final int delta){
			final LocalDate target = LocalDate.of((Integer)year.getValue(), months.getSelectedIndex() + 1, 1)
				.plusMonths(delta);
			if(target.getYear() < MIN_YEAR || target.getYear() > MAX_YEAR)
				return;

			updating = true;
			year.setValue(target.getYear());
			months.setSelectedIndex(target.getMonthValue() - 1);
			updating = false;
			rebuild();
		}

		private void rebuild(){
			if(updating)
				return;

			days.removeAll();
			final DayOfWeek first = WeekFields.of(Locale.getDefault())
				.getFirstDayOfWeek();
			for(int i = 0; i < 7; i ++){
				final JLabel label = new JLabel(first.plus(i).getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault()),
					SwingConstants.CENTER);
				days.add(label);
			}

			final LocalDate firstOfMonth = LocalDate.of((Integer)year.getValue(), months.getSelectedIndex() + 1, 1);
			final int offset = Math.floorMod(firstOfMonth.getDayOfWeek().getValue() - first.getValue(), 7);
			for(int i = 0; i < offset; i ++)
				days.add(new JLabel());

			final LocalDate today = LocalDate.now();
			for(int day = 1, count = firstOfMonth.lengthOfMonth(); day <= count; day ++){
				final LocalDate date = firstOfMonth.withDayOfMonth(day);
				final JButton button = new JButton(Integer.toString(day));
				button.setMargin(new Insets(1, 1, 1, 1));
				button.setFocusable(false);
				if(date.equals(selected))
					button.setSelected(true);
				if(date.equals(selected) || date.equals(today))
					button.setFont(button.getFont().deriveFont(java.awt.Font.BOLD));
				button.addActionListener(e -> choose(date));
				days.add(button);
			}
			days.revalidate();
			days.repaint();
			popup.pack();
		}

		private void choose(final LocalDate date){
			field.setText(date.toString());
			popup.setVisible(false);
			field.requestFocusInWindow();
		}

	}

}
