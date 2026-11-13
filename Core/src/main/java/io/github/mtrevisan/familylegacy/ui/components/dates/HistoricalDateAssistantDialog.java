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

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.assistant.AgeEstimator;
import io.github.mtrevisan.calendars.assistant.AssistantInputException;
import io.github.mtrevisan.calendars.assistant.AssistantStateException;
import io.github.mtrevisan.calendars.assistant.CalendarConversionTool;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.LiturgicalTool;
import io.github.mtrevisan.calendars.assistant.Message;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.assistant.RomanDateTool;
import io.github.mtrevisan.calendars.assistant.SunTool;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.CalendarReforms;
import io.github.mtrevisan.familylegacy.ui.components.projections.chronomap.PlaceMapPickerDialog;
import org.jxmapviewer.viewer.GeoPosition;
import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.places.PlaceNameResolver;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
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
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
		AGE,
		ROMAN
	}

	private static final int MAX_COMBO_WIDTH = 260;
	private static final int MIN_COMBO_WIDTH = 80;

	private static final String[] REFORM_KEYS = {"rome", "france", "britain", "julian"};
	private static final CalendarReform[] REFORMS = {CalendarReform.ROME, CalendarReform.FRANCE, CalendarReform.BRITAIN, null};


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
			return shortLabel + (approximate? " (" + I18N.t("dialog.date_assistant.event.approximate") + ")": StringUtils.EMPTY)
				+ (minJdn == maxJdn? StringUtils.EMPTY: " (" + I18N.tf("dialog.date_assistant.event.range", String.valueOf(minJdn), String.valueOf(maxJdn)) + ")");
		}

	}


	private final Consumer<DateSuggestion> applier;

	private final DefaultListModel<DateSuggestion> suggestionsModel = new DefaultListModel<>();
	private final JList<DateSuggestion> suggestionsList = new JList<>(suggestionsModel);
	private final JTextArea explanationArea = new JTextArea(14, 64);
	private final JLabel messageLabel = new JLabel(StringUtils.SPACE);
	private final JButton applyButton = new JButton(I18N.t("dialog.date_assistant.apply"));
	private final JButton copyButton = new JButton(I18N.t("dialog.date_assistant.copy"));
	private final JButton dismissButton = new JButton(I18N.t("dialog.date_assistant.dismiss"));


	/**
	 * @param parent       the parent window
	 * @param initialTool  the tool to show first
	 * @param calendarCode the calendar currently entered, used to prefill the tools (can be {@code null})
	 * @param dateText     the date currently entered, used to prefill the tools (can be {@code null})
	 * @param model        the model, to choose the events the date is relative to (can be {@code null})
	 * @param applier      called when the user applies a suggestion; {@code null} when there is no date to apply it to (the assistant
	 *                     opened from a menu): the suggestions can only be read and copied
	 */
	public HistoricalDateAssistantDialog(final Window parent, final Tool initialTool, final String calendarCode, final String dateText,
			final FLEFModel model, final Consumer<DateSuggestion> applier){
		this(parent, initialTool, calendarCode, dateText, model, applier, null);
	}

	/**
	 * @param placeId      the id of the place of the event the date belongs to: if its jurisdiction has a known reform, that
	 *                     reform is preselected (can be {@code null}); the user can always choose another one
	 * @see #HistoricalDateAssistantDialog(Window, Tool, String, String, FLEFModel, Consumer)
	 */
	public HistoricalDateAssistantDialog(final Window parent, final Tool initialTool, final String calendarCode, final String dateText,
			final FLEFModel model, final Consumer<DateSuggestion> applier, final String placeId){
		super(parent, I18N.t("dialog.date_assistant.title"), ModalityType.APPLICATION_MODAL);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		this.applier = applier;

		initComponents(initialTool, calendarCode, dateText, loadEvents(model), loadPlaces(model),
			initialReform(model, placeId));

		compactCombos(getContentPane());

		//ESC closes the assistant from any field, without a button
		getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
			JComponent.WHEN_IN_FOCUSED_WINDOW);

		pack();
		setLocationRelativeTo(parent);
	}


	private void initComponents(final Tool initialTool, final String calendarCode, final String dateText,
			final List<EventChoice> events, final List<PlaceChoice> places, final CalendarReform initialReform){
		final JTabbedPane tools = new JTabbedPane();
		tools.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tools.addTab(I18N.t("dialog.date_assistant.tool.conversion"), createConversionPanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.liturgical"), createLiturgicalPanel(places, initialReform));
		tools.addTab(I18N.t("dialog.date_assistant.tool.sun"), createSunPanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.relative"), createRelativePanel(calendarCode, dateText));
		tools.addTab(I18N.t("dialog.date_assistant.tool.event"), createEventPanel(events));
		tools.addTab(I18N.t("dialog.date_assistant.tool.age"), createAgePanel(calendarCode, dateText, events));
		tools.addTab(I18N.t("dialog.date_assistant.tool.roman"), createRomanPanel());
		tools.setSelectedIndex(initialTool.ordinal());

		suggestionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final DefaultListCellRenderer baseRenderer = new DefaultListCellRenderer();
		suggestionsList.setCellRenderer((list, value, index, selected, focus) -> {
			final JLabel label = (JLabel)baseRenderer.getListCellRendererComponent(list,
				AssistantMessages.translate(value.title()) + (value.isApplicable()? ": " + summary(value): StringUtils.EMPTY), index,
				selected, focus);
			label.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
			return label;
		});
		suggestionsList.addListSelectionListener(e -> showSelected());

		explanationArea.setEditable(false);
		explanationArea.setLineWrap(true);
		explanationArea.setWrapStyleWord(true);
		explanationArea.setMargin(new Insets(6, 8, 6, 8));
		explanationArea.setFont(UIManager.getFont("Label.font").deriveFont(Font.PLAIN));

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
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(AssistantMessages.explanation(suggestion)), null);
		});
		dismissButton.addActionListener(e -> {
			final int index = suggestionsList.getSelectedIndex();
			if(index >= 0)
				suggestionsModel.remove(index);
			showSelected();
		});
		showSelected();

		final JPanel buttons = new JPanel(new MigLayout("ins 0", "[][][]"));
		if(applier != null)
			buttons.add(applyButton);
		buttons.add(copyButton);
		buttons.add(dismissButton);

		setLayout(new MigLayout("fill,wrap 1,insets 10", "[grow,fill]", "[]8[]8[100:100:150]8[grow]8[]"));
		add(new JLabel("<html><i>" + I18N.t("dialog.date_assistant.disclaimer") + "</i></html>"));
		add(tools);
		add(new JScrollPane(suggestionsList), "grow");
		add(new JScrollPane(explanationArea), "grow");
		add(messageLabel, "split 2");
		add(buttons, "align right");
	}


	/** The layout shared by the tools: label and field, label and field, and the button that runs the tool. */
	private static JPanel createToolPanel(){
		return new JPanel(new MigLayout("fillx,insets 10", "[][grow,fill]15[][grow,fill]15[sg run,fill]"));
	}

	private JPanel createConversionPanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 24);
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> CalendarConversionTool.convert((String)calendar.getSelectedItem(), date.getText())));

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date.calendar")));
		panel.add(calendar);
		panel.add(new JLabel(I18N.l("dialog.date.full_date")));
		panel.add(date);
		panel.add(run);
		return panel;
	}

	private JPanel createLiturgicalPanel(final List<PlaceChoice> places, final CalendarReform initialReform){
		final JSpinner year = createYearSpinner();
		final JComboBox<String> reform = createReformCombo();
		final JComboBox<PlaceChoice> placeCombo = new JComboBox<>(places.toArray(new PlaceChoice[0]));
		placeCombo.setToolTipText(I18N.t("dialog.date_assistant.reform.place.tooltip"));
		//choosing a place selects the reform of the historical jurisdiction the place belongs to
		placeCombo.addActionListener(e -> {
			final PlaceChoice choice = (PlaceChoice)placeCombo.getSelectedItem();
			if(choice != null && choice.reform() != null)
				reform.setSelectedIndex(reformIndex(choice.reform()));
		});
		placeCombo.setSelectedItem(null);
		//the reform of the place of the event, as a starting point: the combo stays editable, so the user can override it
		if(initialReform != null)
			reform.setSelectedIndex(reformIndex(initialReform));
		final JButton feasts = new JButton(I18N.t("dialog.date_assistant.run"));
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

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date_assistant.year")));
		panel.add(year, "w 90!");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.reform")));
		panel.add(reform, "wrap");
		if(!places.isEmpty()){
			panel.add(new JLabel(I18N.l("dialog.date_assistant.reform.place")), "skip 2");
			panel.add(placeCombo, "wrap");
		}
		panel.add(new JLabel(I18N.l("dialog.date_assistant.liturgical.feasts")), "span 4");
		panel.add(feasts, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.liturgical.expression")));
		panel.add(expression, "span 3");
		panel.add(resolve, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.liturgical.easter_distance")));
		panel.add(days, "w 90!");
		panel.add(direction, "span 2");
		panel.add(relative);
		return panel;
	}

	private JPanel createSunPanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 20);
		final JTextField latitude = new JTextField("45.44", 8);
		latitude.setToolTipText(I18N.t("dialog.date_assistant.sun.coordinates.tooltip"));
		final JTextField longitude = new JTextField("12.33", 8);
		longitude.setToolTipText(I18N.t("dialog.date_assistant.sun.coordinates.tooltip"));
		final JButton mapButton = new JButton(I18N.t("dialog.date_assistant.sun.map"));
		mapButton.setToolTipText(I18N.t("dialog.date_assistant.sun.map.tooltip"));
		mapButton.addActionListener(e -> pickOnMap((String)calendar.getSelectedItem(), date.getText(), latitude, longitude));
		final JTextField hours = new JTextField(6);
		final JComboBox<String> hourSystem = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.sun.hours.equal"),
			I18N.t("dialog.date_assistant.sun.hours.unequal")});
		hours.setToolTipText(I18N.t("dialog.date_assistant.sun.hours.tooltip"));
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> SunTool.analyze((String)calendar.getSelectedItem(), date.getText(),
			Double.parseDouble(latitude.getText().trim().replace(',', '.')), Double.parseDouble(longitude.getText().trim().replace(',', '.')),
			(hourSystem.getSelectedIndex() == 0? SunTool.HourSystem.EQUAL: SunTool.HourSystem.UNEQUAL),
			(StringUtils.isBlank(hours.getText())? null: Double.valueOf(hours.getText().trim().replace(',', '.'))))));

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date.calendar")));
		panel.add(calendar);
		panel.add(new JLabel(I18N.l("dialog.date.full_date")));
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.sun.latitude")));
		panel.add(latitude, "w 110!");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.sun.longitude")));
		panel.add(longitude, "w 110!");
		panel.add(mapButton, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.sun.hours")));
		panel.add(hours, "w 110!");
		panel.add(hourSystem, "span 2");
		panel.add(run);
		return panel;
	}

	/**
	 * Lets the user choose the place on the map of the chronomap, with the historical maps of the day entered, and writes its
	 * coordinates in the fields.
	 */
	private void pickOnMap(final String calendarCode, final String dateText, final JTextField latitude, final JTextField longitude){
		double jdn = Double.NaN;
		try{
			jdn = CalendarType.fromCode(calendarCode).parseToJdn(dateText);
		}
		catch(final RuntimeException ignored){
			//the date is not (yet) readable: only the modern map is shown
		}
		GeoPosition initial = null;
		try{
			initial = new GeoPosition(Double.parseDouble(latitude.getText().trim().replace(',', '.')),
				Double.parseDouble(longitude.getText().trim().replace(',', '.')));
		}
		catch(final RuntimeException ignored){
			//the coordinates are not (yet) readable: the map opens on the whole world
		}

		final PlaceMapPickerDialog picker = new PlaceMapPickerDialog(this, jdn, initial);
		picker.setVisible(true);
		final GeoPosition position = picker.selectedPosition();
		if(position != null){
			latitude.setText(PlaceMapPickerDialog.format(position.getLatitude()));
			longitude.setText(PlaceMapPickerDialog.format(position.getLongitude()));
		}
	}

	private JPanel createRelativePanel(final String calendarCode, final String dateText){
		final JComboBox<String> calendar = createCalendarCombo(calendarCode);
		final JTextField date = new JTextField(StringUtils.defaultString(dateText), 20);
		final JSpinner amount = new JSpinner(new SpinnerNumberModel(40, 0, 100_000, 1));
		final JComboBox<RelativeDateTool.Unit> unit = createUnitCombo();
		final JComboBox<String> direction = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.after"),
			I18N.t("dialog.date_assistant.before")});
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> RelativeDateTool.offset((String)calendar.getSelectedItem(), date.getText(),
			(int)amount.getValue(), (RelativeDateTool.Unit)unit.getSelectedItem(), direction.getSelectedIndex() == 0)));

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date.calendar")));
		panel.add(calendar);
		panel.add(new JLabel(I18N.l("dialog.date_assistant.relative.base")));
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.relative.distance")));
		panel.add(amount, "w 90!");
		panel.add(unit, "span 2,split 2");
		panel.add(direction);
		panel.add(run);
		return panel;
	}


	private JPanel createEventPanel(final List<EventChoice> events){
		final JComboBox<EventChoice> event = createEventCombo(events, false);
		final JSpinner amount = new JSpinner(new SpinnerNumberModel(3, 0, 100_000, 1));
		final JComboBox<RelativeDateTool.Unit> unit = createUnitCombo();
		final JComboBox<String> direction = new JComboBox<>(new String[]{I18N.t("dialog.date_assistant.after"),
			I18N.t("dialog.date_assistant.before")});
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> {
			final EventChoice choice = (EventChoice)event.getSelectedItem();
			if(choice == null)
				throw new IllegalArgumentException(I18N.t("dialog.date_assistant.event.none"));

			return RelativeDateTool.offsetFromRange(choice.minJdn(), choice.maxJdn(), Message.text(choice.describe()), (int)amount.getValue(),
				(RelativeDateTool.Unit)unit.getSelectedItem(), direction.getSelectedIndex() == 0);
		}));

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date_assistant.event")));
		panel.add(event, "span 4,wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.relative.distance")));
		panel.add(amount, "w 90!");
		panel.add(unit, "span 2,split 2");
		panel.add(direction);
		panel.add(run);
		return panel;
	}

	private JPanel createRomanPanel(){
		final JTextField expression = new JTextField(24);
		expression.setToolTipText(I18N.t("dialog.date_assistant.roman.expression.tooltip"));
		final JSpinner year = createYearSpinner();
		final JButton run = new JButton(I18N.t("dialog.date_assistant.run"));
		run.addActionListener(e -> compute(() -> RomanDateTool.interpret(expression.getText(), (int)year.getValue())));

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date_assistant.roman.expression")));
		panel.add(expression);
		panel.add(new JLabel(I18N.l("dialog.date_assistant.year")));
		panel.add(year, "w 90!");
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
				return AgeEstimator.birthFromAge(choice.minJdn(), choice.maxJdn(), choice.countingCalendar(), Message.text(choice.describe()),
					(int)years.getValue(), m, d, ordinal, eventName.getText().trim());
			return AgeEstimator.birthFromAge((String)calendar.getSelectedItem(), date.getText(), (int)years.getValue(), m, d,
				ordinal, eventName.getText().trim());
		}));

		//the age, as years, months and days, and how the years are counted
		final JPanel ageDetails = new JPanel(new MigLayout("ins 0", "[][]5[]5[][]5[]5[][]10[]"));
		ageDetails.add(years, "w 60!");
		ageDetails.add(new JLabel(I18N.t("dialog.date_assistant.age.years")));
		ageDetails.add(months, "w 40!");
		ageDetails.add(new JLabel(I18N.t("dialog.date_assistant.age.months")));
		ageDetails.add(days, "w 40!");
		ageDetails.add(new JLabel(I18N.t("dialog.date_assistant.age.days")));
		ageDetails.add(reading);

		final JPanel panel = createToolPanel();
		panel.add(new JLabel(I18N.l("dialog.date_assistant.event")));
		panel.add(event, "span 4,wrap");
		panel.add(new JLabel(I18N.l("dialog.date.calendar")));
		panel.add(calendar);
		panel.add(new JLabel(I18N.l("dialog.date.full_date")));
		panel.add(date, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.age.event_name")));
		panel.add(eventName, "wrap");
		panel.add(new JLabel(I18N.l("dialog.date_assistant.age")));
		panel.add(ageDetails, "span 3,growx");
		panel.add(run);
		return panel;
	}

	/** A combo of the events of the model that have a date; the first item is empty if the event is optional. */
	private static JComboBox<EventChoice> createEventCombo(final List<EventChoice> events, final boolean optional){
		final JComboBox<EventChoice> combo = new JComboBox<>();
		if(optional)
			combo.addItem(null);
		events.forEach(combo::addItem);
		//a file without dated events has nothing to choose from
		if(combo.getItemCount() > 0)
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
		final List<EventChoice> filteredEvents = new ArrayList<>();
		if(model == null)
			return filteredEvents;

		final List<FLEFRecord> events = model.getRecordsByType(EventHandler.TYPE);
		for(final FLEFRecord event : events){
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
			filteredEvents.add(new EventChoice(name + ", " + date + " [" + event.getId() + "]", name + StringUtils.SPACE + date, type, min, max,
				(min < CalendarReform.ROME.firstGregorianJdn()? CalendarType.JULIAN.getCode(): CalendarType.GREGORIAN.getCode()), span.isApproximate()));
		}
		filteredEvents.sort(Comparator.comparingLong(EventChoice::minJdn));
		return filteredEvents;
	}

	/** A place whose jurisdiction (the place itself or one of its containers) has a known Gregorian reform. */
	private record PlaceChoice(String label, CalendarReform reform){

		@Override
		public String toString(){
			return label;
		}

	}

	/** The places of the model whose reform is known, sorted by name. */
	private static List<PlaceChoice> loadPlaces(final FLEFModel model){
		final List<PlaceChoice> places = new ArrayList<>();
		if(model == null)
			return places;

		final PlaceNameResolver resolver = new PlaceNameResolver(model);
		for(final FLEFRecord place : model.getRecordsByType(PlaceReader.TYPE)){
			final Optional<CalendarReform> reform = resolver.calendarReform(place.getId());
			if(reform.isPresent())
				places.add(new PlaceChoice(resolver.fullName(place.getId()), reform.get()));
		}
		places.sort(Comparator.comparing(PlaceChoice::label, String.CASE_INSENSITIVE_ORDER));
		return places;
	}

	/** The reform of the jurisdiction of a place, or {@code null} if there is no place or its jurisdiction is not known. */
	private static CalendarReform initialReform(final FLEFModel model, final String placeId){
		if(model == null || StringUtils.isBlank(placeId))
			return null;

		return new PlaceNameResolver(model).calendarReform(placeId).orElse(null);
	}

	/** The index, in the reform combo, of the entry that has the given reform (the general choices come first). */
	private static int reformIndex(final CalendarReform reform){
		for(int i = 0; i < REFORMS.length; i ++)
			if(reform.equals(REFORMS[i]))
				return i;
		final List<String> jurisdictions = CalendarReforms.jurisdictions();
		for(int i = 0; i < jurisdictions.size(); i ++)
			if(reform.equals(CalendarReforms.resolve(jurisdictions.get(i)).orElse(null)))
				return REFORMS.length + i;
		return 0;
	}

	private static String summary(final DateSuggestion suggestion){
		if(suggestion.normalizedDate() != null)
			return suggestion.normalizedDate();

		return Objects.toString(suggestion.notBefore(), "…") + " – " + Objects.toString(suggestion.notAfter(), "…");
	}


	private static JComboBox<String> createCalendarCombo(final String calendarCode){
		final JComboBox<String> combo = new JComboBox<>(DateReader.CALENDARS);
		combo.setSelectedItem(StringUtils.isNotEmpty(calendarCode)? calendarCode: DateReader.ENUM_CALENDAR_GREGORIAN);
		//the items stay the calendar codes, the labels are localized
		final ListCellRenderer<? super String> base = combo.getRenderer();
		combo.setRenderer((list, value, index, selected, focus) -> base.getListCellRendererComponent(list,
			value == null? null: AssistantMessages.calendarName(value), index, selected, focus));
		return combo;
	}

	/** The units of a distance; the items stay the enum constants, the labels are localized. */
	private static JComboBox<RelativeDateTool.Unit> createUnitCombo(){
		final JComboBox<RelativeDateTool.Unit> combo = new JComboBox<>(RelativeDateTool.Unit.values());
		final ListCellRenderer<? super RelativeDateTool.Unit> base = combo.getRenderer();
		combo.setRenderer((list, value, index, selected, focus) -> {
			final Component component = base.getListCellRendererComponent(list, value, index, selected, focus);
			if(value != null && component instanceof final JLabel label)
				label.setText(I18N.t("dialog.date_assistant.unit." + value.name().toLowerCase(Locale.ROOT)));
			return component;
		});
		return combo;
	}

	private static JSpinner createYearSpinner(){
		final JSpinner spinner = new JSpinner(new SpinnerNumberModel(1500, 1, 9999, 1));
		spinner.setEditor(new JSpinner.NumberEditor(spinner, "#"));
		return spinner;
	}

	private static JComboBox<String> createReformCombo(){
		final List<String> jurisdictions = CalendarReforms.jurisdictions();
		final String[] labels = new String[REFORM_KEYS.length + jurisdictions.size()];
		for(int i = 0; i < REFORM_KEYS.length; i ++)
			labels[i] = I18N.t("dialog.date_assistant.reform." + REFORM_KEYS[i]);
		//the historical jurisdictions (lands, provinces, cantons) follow the general choices
		for(int i = 0; i < jurisdictions.size(); i ++)
			labels[REFORM_KEYS.length + i] = jurisdictions.get(i);
		return new JComboBox<>(labels);
	}

	private static CalendarReform selectedReform(final JComboBox<String> combo){
		final int index = combo.getSelectedIndex();
		if(index < REFORMS.length)
			return REFORMS[index];
		return CalendarReforms.resolve(CalendarReforms.jurisdictions().get(index - REFORMS.length)).orElseThrow();
	}


	/** Runs a tool and shows its suggestions, or the reason it failed. */
	private void compute(final Supplier<List<DateSuggestion>> tool){
		suggestionsModel.clear();
		messageLabel.setText(StringUtils.SPACE);
		messageLabel.setForeground(errorColor());
		try{
			tool.get()
				.forEach(suggestionsModel::addElement);
			if(!suggestionsModel.isEmpty())
				suggestionsList.setSelectedIndex(0);
		}
		catch(final AssistantStateException ase){
			messageLabel.setText(AssistantMessages.translate(ase.reason()));
		}
		catch(final IllegalStateException ise){
			messageLabel.setText(ise.getMessage());
		}
		catch(final UnsupportedOperationException ignored){
			messageLabel.setText(I18N.t("dialog.date_assistant.error.ephemeris"));
		}
		catch(final AssistantInputException aie){
			messageLabel.setText(I18N.t("dialog.date_assistant.error.input") + ": " + AssistantMessages.translate(aie.reason()));
		}
		catch(final IllegalArgumentException iae){
			messageLabel.setText(I18N.t("dialog.date_assistant.error.input") + ": " + iae.getMessage());
		}
		showSelected();
	}

	/**
	 * Keeps the combo boxes narrow, whatever the longest of their items is (an event label can be very long): they still grow with
	 * the window where the layout lets them, and the selected text is in the tooltip, as it can be truncated.
	 */
	private static void compactCombos(final Container container){
		for(final Component component : container.getComponents()){
			if(component instanceof final JComboBox<?> combo)
				compactCombo(combo);
			if(component instanceof final Container child)
				compactCombos(child);
		}
	}

	private static <T> void compactCombo(final JComboBox<T> combo){
		final Dimension size = combo.getPreferredSize();
		combo.setPreferredSize(new Dimension(Math.min(size.width, MAX_COMBO_WIDTH), size.height));
		//the minimum size of a combo is the width of its longest item, and the layout raises the preferred size to the minimum one:
		//without this the dialog is as wide as the longest event or place of the file
		combo.setMinimumSize(new Dimension(MIN_COMBO_WIDTH, size.height));
		//a combo with an explanation as tooltip keeps it
		if(combo.getToolTipText() != null)
			return;

		final Runnable showSelected = () -> {
			final T item = combo.getSelectedItem() != null? combo.getItemAt(combo.getSelectedIndex()): null;
			final Component rendered = combo.getRenderer().getListCellRendererComponent(new JList<>(), item, -1, false, false);
			combo.setToolTipText(rendered instanceof final JLabel label && StringUtils.isNotBlank(label.getText())? label.getText(): null);
		};
		combo.addActionListener(e -> showSelected.run());
		showSelected.run();
	}

	private static Color errorColor(){
		final Color color = UIManager.getColor("Component.error.focusedBorderColor");
		return (color != null? color: new Color(0xB0_00_20));
	}

	private void showSelected(){
		final DateSuggestion suggestion = suggestionsList.getSelectedValue();
		//until there is something to show, the area tells how to use the tool
		final boolean hint = (suggestion == null && StringUtils.isBlank(messageLabel.getText()));
		explanationArea.setForeground(hint? UIManager.getColor("Label.disabledForeground"): UIManager.getColor("TextArea.foreground"));
		explanationArea.setText(suggestion != null? AssistantMessages.explanation(suggestion)
			: (hint? I18N.t("dialog.date_assistant.hint"): StringUtils.EMPTY));
		explanationArea.setCaretPosition(0);
		applyButton.setEnabled(suggestion != null && suggestion.isApplicable());
		copyButton.setEnabled(suggestion != null);
		dismissButton.setEnabled(suggestion != null);
	}

}
