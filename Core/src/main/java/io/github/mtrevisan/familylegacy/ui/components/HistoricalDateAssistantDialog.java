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
package io.github.mtrevisan.familylegacy.ui.components;

import io.github.mtrevisan.calendars.assistant.AgeEstimator;
import io.github.mtrevisan.calendars.assistant.CalendarConversionTool;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.LiturgicalTool;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.assistant.SunTool;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * A toolbox that helps the researcher to interpret and normalize historical dates.
 * <p>
 * The results are only suggestions, each with the reasoning behind it. Nothing is changed unless the user applies a
 * suggestion explicitly.
 * </p>
 */
public class HistoricalDateAssistantDialog extends JDialog{

	/** The tools of the assistant, in the order of the tabs. */
	public enum Tool{
		CONVERSION,
		LITURGICAL,
		SUN,
		RELATIVE,
		EVENT,
		AGE
	}

	private static final String[] REFORM_KEYS = {"rome", "france", "britain", "julian"};
	private static final CalendarReform[] REFORMS = {CalendarReform.ROME, CalendarReform.FRANCE, CalendarReform.BRITAIN, null};

	/** The Julian Day Number of 15 October 1582, the first day of the Gregorian calendar in the Catholic countries. */
	private static final long ROME_FIRST_GREGORIAN_JDN = 2_299_161L;


	/**
	 * An event of the model with a date.
	 *
	 * @param label           the description for the list
	 * @param shortLabel      the description for the explanation
	 * @param type            the type of the event
	 * @param minJdn          the earliest day of the date of the event
	 * @param maxJdn          the latest day of the date of the event
	 * @param countingCalendar the calendar to count years and months in (Julian or Gregorian)
	 * @param approximate     whether the date is approximate
	 */
	private record EventChoice(String label, String shortLabel, String type, long minJdn, long maxJdn, String countingCalendar,
			boolean approximate){

		String describe(){
			return shortLabel + (approximate? " (approximate)": StringUtils.EMPTY)
				+ (minJdn == maxJdn? StringUtils.EMPTY: " (from Julian Day Number " + minJdn + " to " + maxJdn
				+ "; reduced precisions use nominal lengths: month 31 days, year 366)");
		}

	}


	private final Consumer<DateSuggestion> applier;

	private final DefaultListModel<DateSuggestion> suggestionsModel = new DefaultListModel<>();
	private final JList<DateSuggestion> suggestionsList = new JList<>(suggestionsModel);
	private final JTextArea explanationArea = new JTextArea(10, 60);
	private final JLabel messageLabel = new JLabel(" ");
	private final JButton applyButton = new JButton(I18N.t("dialog.date_assistant.apply"));
	private final JButton copyButton = new JButton(I18N.t("dialog.date_assistant.copy"));
	private final JButton dismissButton = new JButton(I18N.t("dialog.date_assistant.dismiss"));


	/**
	 * @param parent       the parent window
	 * @param initialTool  the tool to show first
	 * @param calendarCode the calendar currently entered, used to prefill the tools (can be {@code null})
	 * @param dateText     the date currently entered, used to prefill the tools (can be {@code null})
	 * @param model        the model, to choose the events the date is relative to (can be {@code null})
	 * @param applier      called when the user applies a suggestion
	 */
	public HistoricalDateAssistantDialog(final Window parent, final Tool initialTool, final String calendarCode, final String dateText,
			final FLEFModel model, final Consumer<DateSuggestion> applier){
		super(parent, I18N.t("dialog.date_assistant.title"), ModalityType.APPLICATION_MODAL);

		this.applier = applier;

		initComponents(initialTool, calendarCode, dateText, loadEvents(model));

		//ESC closes the assistant from any field, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(final Tool initialTool, final String calendarCode, final String dateText,
			final List<EventChoice> events){
		final JTabbedPane tools = new JTabbedPane();
		tools.addTab(I18N.t("dialog.date_assistant.tool.conversion"), createConversionPanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.liturgical"), createLiturgicalPanel());
		tools.addTab(I18N.t("dialog.date_assistant.tool.sun"), createSunPanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.relative"), createRelativePanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.event"), createEventPanel(events));
		tools.addTab(I18N.t("dialog.date_assistant.tool.age"), createAgePanel(calendarCode, dateText, events));
		tools.setSelectedIndex(initialTool.ordinal());

		suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		suggestionsList.setCellRenderer((list, value, index, selected, focus) -> {
			final JLabel label = new JLabel(value.title() + (value.isApplicable()? ": " + summary(value): StringUtils.EMPTY));
			label.setOpaque(true);
			label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
			return label;
		});
		suggestionsList.addListSelectionListener(e -> showSelected());

		explanationArea.setEditable(false);
		explanationArea.setLineWrap(true);
		explanationArea.setWrapStyleWord(true);

		applyButton.addActionListener(e -> {
			final DateSuggestion suggestion = suggestionsList.getSelectedValue();
			if(suggestion != null){
				applier.accept(suggestion);
				dispose();
			}
		});
		copyButton.addActionListener(e -> {
			final DateSuggestion suggestion = suggestionsList.getSelectedValue();
			if(suggestion != null)
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(suggestion.explanation()), null);
		});
		dismissButton.addActionListener(e -> {
			final int index = suggestionsList.getSelectedIndex();
			if(index >= 0)
				suggestionsModel.remove(index);
			showSelected();
		});
		showSelected();

		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[][][]"));
		buttons.add(applyButton);
		buttons.add(copyButton);
		buttons.add(dismissButton);

		setLayout(new MigLayout("fill,wrap 1", "[grow,fill]", "[]5[]5[100:100:150]5[grow]5[]"));
		add(new JLabel("<html><i>" + I18N.t("dialog.date_assistant.disclaimer") + "</i></html>"));
		add(tools);
		add(new JScrollPane(suggestionsList), "grow");
		add(new JScrollPane(explanationArea), "grow");
		add(messageLabel, "split 2");
		add(buttons, "align right");
	}


	private JPanel createConversionPanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 24);
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> CalendarConversionTool.convert((String)calendar.getSelectedItem(), date.getText())));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill][]"));
		panel.add(new JLabel(I18N.t("dialog.date.calendar") + ":"));
		panel.add(calendar);
		panel.add(new JLabel(I18N.t("dialog.date.full_date") + ":"), "gapleft 10");
		panel.add(date, "growx");
		panel.add(run);
		return panel;
	}

	private JPanel createLiturgicalPanel(){
		final JSpinner year = createYearSpinner();
		final JComboBox<String> reform = createReformCombo();
		final JButton feasts = new JButton(I18N.t("dialog.date_assistant.liturgical.feasts"));
		feasts.addActionListener(e -> compute(() -> LiturgicalTool.feasts((int)year.getValue(), selectedReform(reform))));

		final JTextField expression = new JTextField(24);
		expression.setToolTipText(I18N.t("dialog.date_assistant.liturgical.expression.tooltip"));
		final JButton resolve = new JButton(I18N.t("dialog.date_assistant.run"));
		resolve.addActionListener(e -> compute(() -> List.of(LiturgicalTool.resolveFeast(expression.getText(), selectedReform(reform)))));

		final JSpinner days = new JSpinner(new SpinnerNumberModel(3, 0, 400, 1));
		final JComboBox<String> direction = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.after"),
			I18N.t("dialog.date_assistant.before")});
		final JButton relative = new JButton(I18N.t("dialog.date_assistant.run"));
		relative.addActionListener(e -> compute(() -> List.of(LiturgicalTool.relativeToEaster((int)year.getValue(),
			(direction.getSelectedIndex() == 0? 1: -1) * (long)(int)days.getValue(), selectedReform(reform)))));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill][][grow,fill]"));
		panel.add(new JLabel(I18N.t("dialog.date_assistant.year") + ":"));
		panel.add(year);
		panel.add(new JLabel(I18N.t("dialog.date_assistant.reform") + ":"), "gapleft 10");
		panel.add(reform, "wrap");
		panel.add(feasts, "span 4,wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.liturgical.expression") + ":"));
		panel.add(expression, "span 2,growx");
		panel.add(resolve, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.liturgical.easter_distance") + ":"));
		panel.add(days, "split 3,growx");
		panel.add(direction);
		panel.add(relative);
		return panel;
	}

	private JPanel createSunPanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 20);
		final JTextField latitude = new JTextField("45.44", 8);
		final JTextField longitude = new JTextField("12.33", 8);
		final JTextField hours = new JTextField(6);
		final JComboBox<String> hourSystem = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.sun.hours.equal"),
			I18N.t("dialog.date_assistant.sun.hours.unequal")});
		hours.setToolTipText(I18N.t("dialog.date_assistant.sun.hours.tooltip"));
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> SunTool.analyze((String)calendar.getSelectedItem(), date.getText(),
			Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()),
			(hourSystem.getSelectedIndex() == 0? SunTool.HourSystem.EQUAL: SunTool.HourSystem.UNEQUAL),
			(StringUtils.isBlank(hours.getText())? null: Double.valueOf(hours.getText().trim().replace(',', '.'))))));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill][][grow,fill]"));
		panel.add(new JLabel(I18N.t("dialog.date.calendar") + ":"));
		panel.add(calendar);
		panel.add(new JLabel(I18N.t("dialog.date.full_date") + ":"), "gapleft 10");
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.sun.latitude") + ":"));
		panel.add(latitude);
		panel.add(new JLabel(I18N.t("dialog.date_assistant.sun.longitude") + ":"), "gapleft 10");
		panel.add(longitude, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.sun.hours") + ":"));
		panel.add(hours, "split 2,growx");
		panel.add(hourSystem);
		panel.add(run, "skip 1");
		return panel;
	}

	private JPanel createRelativePanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 20);
		final JSpinner amount = new JSpinner(new SpinnerNumberModel(40, 0, 100_000, 1));
		final JComboBox<RelativeDateTool.Unit> unit = new JComboBox<>(RelativeDateTool.Unit.values());
		final JComboBox<String> direction = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.after"),
			I18N.t("dialog.date_assistant.before")});
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> RelativeDateTool.offset((String)calendar.getSelectedItem(), date.getText(),
			(int)amount.getValue(), (RelativeDateTool.Unit)unit.getSelectedItem(), direction.getSelectedIndex() == 0)));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill][][grow,fill]"));
		panel.add(new JLabel(I18N.t("dialog.date.calendar") + ":"));
		panel.add(calendar);
		panel.add(new JLabel(I18N.t("dialog.date_assistant.relative.base") + ":"), "gapleft 10");
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.relative.distance") + ":"));
		panel.add(amount, "split 3,growx");
		panel.add(unit);
		panel.add(direction);
		panel.add(run, "skip 1");
		return panel;
	}


	private JPanel createEventPanel(final List<EventChoice> events){
		final JComboBox<EventChoice> event = createEventCombo(events, false);
		final JSpinner amount = new JSpinner(new SpinnerNumberModel(3, 0, 100_000, 1));
		final JComboBox<RelativeDateTool.Unit> unit = new JComboBox<>(RelativeDateTool.Unit.values());
		final JComboBox<String> direction = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.after"),
			I18N.t("dialog.date_assistant.before")});
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> {
			final EventChoice choice = (EventChoice)event.getSelectedItem();
			if(choice == null)
				throw new IllegalArgumentException(I18N.t("dialog.date_assistant.event.none"));

			return RelativeDateTool.offsetFromRange(choice.minJdn(), choice.maxJdn(), choice.describe(), (int)amount.getValue(),
				(RelativeDateTool.Unit)unit.getSelectedItem(), direction.getSelectedIndex() == 0);
		}));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill]"));
		panel.add(new JLabel(I18N.t("dialog.date_assistant.event") + ":"));
		panel.add(event, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.relative.distance") + ":"));
		panel.add(amount, "split 4,growx");
		panel.add(unit);
		panel.add(direction);
		panel.add(run);
		return panel;
	}

	private JPanel createAgePanel(final String calendarCode, final String dateText, final List<EventChoice> events){
		final JComboBox<EventChoice> event = createEventCombo(events, true);
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 20);
		date.setToolTipText(I18N.t("dialog.date_assistant.age.date.tooltip"));
		final JTextField eventName = new JTextField(I18N.t("dialog.date_assistant.age.event_name.default"), 12);
		final JSpinner years = new JSpinner(new SpinnerNumberModel(72, 0, 150, 1));
		final JTextField months = new JTextField(4);
		final JTextField days = new JTextField(4);
		final JComboBox<String> reading = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.age.completed"),
			I18N.t("dialog.date_assistant.age.ordinal")});
		reading.setToolTipText(I18N.t("dialog.date_assistant.age.ordinal.tooltip"));
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> {
			final Integer m = parseOptionalInt(months.getText());
			final Integer d = parseOptionalInt(days.getText());
			final boolean ordinal = (reading.getSelectedIndex() == 1);
			final EventChoice choice = (EventChoice)event.getSelectedItem();
			if(choice != null)
				return AgeEstimator.birthFromAge(choice.minJdn(), choice.maxJdn(), choice.countingCalendar(), choice.describe(),
					(int)years.getValue(), m, d, ordinal, eventName.getText().trim());
			return AgeEstimator.birthFromAge((String)calendar.getSelectedItem(), date.getText(), (int)years.getValue(), m, d,
				ordinal, eventName.getText().trim());
		}));

		final JPanel panel = new JPanel(new MigLayout("fillx", "[][grow,fill][][grow,fill]"));
		panel.add(new JLabel(I18N.t("dialog.date_assistant.event") + ":"));
		panel.add(event, "span 3,wrap");
		panel.add(new JLabel(I18N.t("dialog.date.calendar") + ":"));
		panel.add(calendar);
		panel.add(new JLabel(I18N.t("dialog.date.full_date") + ":"), "gapleft 10");
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.age.event_name") + ":"));
		panel.add(eventName, "wrap");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.age") + ":"));
		panel.add(years, "split 6,growx");
		panel.add(new JLabel(I18N.t("dialog.date_assistant.age.months")));
		panel.add(months);
		panel.add(new JLabel(I18N.t("dialog.date_assistant.age.days")));
		panel.add(days);
		panel.add(reading, "span 2");
		panel.add(run, "skip 2,wrap");
		return panel;
	}

	/** A combo of the events of the model that have a date; the first item is empty if the event is optional. */
	private static JComboBox<EventChoice> createEventCombo(final List<EventChoice> events, final boolean optional){
		final JComboBox<EventChoice> combo = new JComboBox<>();
		if(optional)
			combo.addItem(null);
		events.forEach(combo::addItem);
		combo.setSelectedIndex(0);
		combo.setRenderer((list, value, index, selected, focus) -> {
			final JLabel label = new JLabel(value != null? value.label(): I18N.t("dialog.date_assistant.event.manual"));
			label.setOpaque(true);
			label.setBackground(selected? list.getSelectionBackground(): list.getBackground());
			label.setForeground(selected? list.getSelectionForeground(): list.getForeground());
			return label;
		});
		return combo;
	}

	private static Integer parseOptionalInt(final String text){
		return (StringUtils.isBlank(text)? null: Integer.valueOf(text.trim()));
	}

	/** The events of the model whose date is known to the day, month or year, or is a closed range, sorted by date. */
	private static List<EventChoice> loadEvents(final FLEFModel model){
		final List<EventChoice> events = new ArrayList<>();
		if(model == null)
			return events;

		for(final FLEFRecord event : model.getRecordsByType(EventHandler.TYPE)){
			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			if(dates.isEmpty())
				continue;

			final TemporalSpan span;
			try{
				span = DateNormalizer.normalize(dates.getFirst());
			}
			catch(final RuntimeException ignored){
				continue;
			}
			if(span == null || span.minJdn() == Long.MIN_VALUE || span.maxJdn() == Long.MAX_VALUE)
				continue;

			final long min = span.minJdn();
			final long max = (span.isPoint()? min + span.start().precisionSpanInDays() - 1: span.maxJdn());
			final String type = StringUtils.defaultString(EventReader.extractType(event));
			final String description = EventReader.extractDescription(event);
			final String date = DateReader.extractPrettyPrintDate(dates.getFirst());
			final String name = type + (StringUtils.isNotBlank(description)? " (" + description + ")": StringUtils.EMPTY);
			events.add(new EventChoice(name + ", " + date + " [" + event.getId() + "]", name + " " + date, type, min, max,
				(min < ROME_FIRST_GREGORIAN_JDN? "julian": "gregorian"), span.isApproximate()));
		}
		events.sort(Comparator.comparingLong(EventChoice::minJdn));
		return events;
	}

	private static String summary(final DateSuggestion suggestion){
		if(suggestion.normalizedDate() != null)
			return suggestion.normalizedDate();

		return StringUtils.defaultString(suggestion.notBefore(), "…") + " – " + StringUtils.defaultString(suggestion.notAfter(), "…");
	}


	private static JComboBox<String> createCalendarCombo(final String calendarCode){
		final JComboBox<String> combo = new JComboBox<>(DateReader.CALENDARS);
		combo.setSelectedItem(StringUtils.isNotEmpty(calendarCode)? calendarCode: DateReader.ENUM_CALENDAR_GREGORIAN);
		return combo;
	}

	private static JSpinner createYearSpinner(){
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1500, 1, 9999, 1));
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		return spinner;
	}

	private static JComboBox<String> createReformCombo(){
		final String[] labels = new String[REFORM_KEYS.length];
		for(int i = 0; i < labels.length; i ++)
			labels[i] = I18N.t("dialog.date_assistant.reform." + REFORM_KEYS[i]);
		return new JComboBox<>(labels);
	}

	private static CalendarReform selectedReform(final JComboBox<String> combo){
		return REFORMS[combo.getSelectedIndex()];
	}


	/** Runs a tool and shows its suggestions, or the reason it failed. */
	private void compute(final Supplier<List<DateSuggestion>> tool){
		suggestionsModel.clear();
		messageLabel.setText(" ");
		try{
			tool.get()
				.forEach(suggestionsModel::addElement);
			if(!suggestionsModel.isEmpty())
				suggestionsList.setSelectedIndex(0);
		}
		catch(final IllegalStateException ise){
			messageLabel.setText(ise.getMessage());
		}
		catch(final UnsupportedOperationException ignored){
			messageLabel.setText(I18N.t("dialog.date_assistant.error.ephemeris"));
		}
		catch(final IllegalArgumentException iae){
			messageLabel.setText(I18N.t("dialog.date_assistant.error.input") + ": " + iae.getMessage());
		}
		showSelected();
	}

	private void showSelected(){
		final DateSuggestion suggestion = suggestionsList.getSelectedValue();
		explanationArea.setText(suggestion != null? suggestion.explanation(): StringUtils.EMPTY);
		explanationArea.setCaretPosition(0);
		applyButton.setEnabled(suggestion != null && suggestion.isApplicable());
		copyButton.setEnabled(suggestion != null);
		dismissButton.setEnabled(suggestion != null);
	}

}
