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
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.events.EventToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;


/** The Calendar Converter and the Date Calculator: the conversions, the calculations and where they are in the menus. */
class DateToolsTest{

	@Test
	void theConversionGoesThroughTheJulianDayNumber(){
		//the J2000 epoch
		Assertions.assertEquals(2_451_545L, CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 2000, 1, 1));

		//the day after the 4th of October 1582 of the Julian calendar is the 15th of October of the Gregorian one
		final long julian = CalendarConversions.toJdn(CalendarConversions.Calendar.JULIAN, 1582, 10, 5);
		final long gregorian = CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 1582, 10, 15);
		Assertions.assertEquals(gregorian, julian);
		Assertions.assertEquals("1582-10-05", CalendarConversions.fromJdn(CalendarConversions.Calendar.JULIAN, gregorian));
		Assertions.assertEquals("1582-10-15", CalendarConversions.fromJdn(CalendarConversions.Calendar.GREGORIAN, julian));
	}

	@Test
	void everyCalendarIsReadAndWrittenWithoutLosingTheDay(){
		final long jdn = CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 1979, 11, 21);

		for(final CalendarConversions.Calendar calendar : CalendarConversions.Calendar.values())
			Assertions.assertFalse(CalendarConversions.fromJdn(calendar, jdn).isBlank(), calendar.name());

		//1 Muharram 1400 of the Hijrah calendar (Umm al-Qura)
		Assertions.assertEquals(jdn, CalendarConversions.toJdn(CalendarConversions.Calendar.HIJRAH, 1400, 1, 1));
		//the Minguo and the Thai Buddhist years are the Gregorian one minus 1911 and plus 543
		Assertions.assertEquals(jdn, CalendarConversions.toJdn(CalendarConversions.Calendar.MINGUO, 1979 - 1911, 11, 21));
		Assertions.assertEquals(jdn, CalendarConversions.toJdn(CalendarConversions.Calendar.THAI_BUDDHIST, 1979 + 543, 11, 21));
	}

	@Test
	void aDateThatDoesNotExistIsRefused(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 2023, 2, 30));
		//1900 is a leap year in the Julian calendar and not in the Gregorian one
		Assertions.assertDoesNotThrow(() -> CalendarConversions.toJdn(CalendarConversions.Calendar.JULIAN, 1900, 2, 29));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 1900, 2, 29));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarConversions.toJdn(CalendarConversions.Calendar.JULIAN, 1582, 2, 30));
	}

	@Test
	void theWeekdayIsInTheLanguageOfTheUser(){
		final long jdn = CalendarConversions.toJdn(CalendarConversions.Calendar.GREGORIAN, 2000, 1, 1);

		Assertions.assertEquals(DayOfWeek.SATURDAY.getDisplayName(TextStyle.FULL, Locale.getDefault()), CalendarConversions.weekday(jdn));
	}


	@Test
	void theAgeIsMeasuredInYearsMonthsAndDays(){
		final Period age = DateCalculations.age(LocalDate.of(1980, 2, 29), LocalDate.of(2020, 2, 28));

		Assertions.assertEquals(Period.of(39, 11, 30), age);
		Assertions.assertEquals("39 years, 11 months, 30 days", DateCalculations.formatPeriod(age));
		Assertions.assertEquals("1 year", DateCalculations.formatPeriod(Period.ofYears(1)));
		Assertions.assertEquals("0 days", DateCalculations.formatPeriod(Period.ZERO));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> DateCalculations.age(LocalDate.of(2000, 1, 2), LocalDate.of(2000, 1, 1)));
	}

	@Test
	void theIntervalIsSignedWhenTheDatesAreReversed(){
		Assertions.assertEquals("1 day", DateCalculations.interval(LocalDate.of(2019, 12, 31), LocalDate.of(2020, 1, 1)));
		Assertions.assertEquals("− 1 day", DateCalculations.interval(LocalDate.of(2020, 1, 1), LocalDate.of(2019, 12, 31)));
	}

	@Test
	void theBirthIsEstimatedFromTheAge(){
		Assertions.assertEquals(LocalDate.of(1828, 6, 15), DateCalculations.estimatedBirth(LocalDate.of(1900, 6, 15), 72, 0, 0));
		Assertions.assertEquals(LocalDate.of(1828, 3, 10), DateCalculations.estimatedBirth(LocalDate.of(1900, 6, 15), 72, 3, 5));
	}

	@Test
	void aDateThatIsNotADateIsSaidInsteadOfBeingIgnored(){
		Assertions.assertNull(DateCalculations.parseDate("  "));
		Assertions.assertEquals(LocalDate.of(1900, 1, 31), DateCalculations.parseDate(" 1900-01-31 "));
		final IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
			() -> DateCalculations.parseDate("1900-13-01"));
		Assertions.assertTrue(error.getMessage().contains("1900-13-01"), error.getMessage());
	}


	private static void collect(final Container container, final List<String> buttons, final List<JTable> tables){
		for(final Component child : container.getComponents()){
			if(child instanceof AbstractButton button)
				buttons.add(button.getText());
			if(child instanceof JTable table)
				tables.add(table);
			if(child instanceof Container nested)
				collect(nested, buttons, tables);
		}
	}

	@Test
	void theDialogsAreLinkedToTheHistoricalDateAssistant() throws InvocationTargetException, InterruptedException{
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "The dialogs need a display");

		final List<String> converterButtons = new ArrayList<>();
		final List<JTable> converterTables = new ArrayList<>();
		final List<String> calculatorButtons = new ArrayList<>();
		final List<String> assistantButtons = new ArrayList<>();
		SwingUtilities.invokeAndWait(() -> {
			final ToolContext context = new ToolContext(new FLEFModel(), null, null, null);
			final CalendarConverterDialog converter = new CalendarConverterDialog(context);
			collect(converter, converterButtons, converterTables);
			converter.dispose();
			final DateCalculatorDialog calculator = new DateCalculatorDialog(context);
			collect(calculator, calculatorButtons, new ArrayList<>());
			calculator.dispose();
			final HistoricalDateAssistantDialog assistant = new HistoricalDateAssistantDialog(null,
				HistoricalDateAssistantDialog.Tool.CONVERSION, null, null, new FLEFModel(), null);
			collect(assistant, assistantButtons, new ArrayList<>());
			assistant.dispose();
		});

		//every calendar, the Julian Day Number and the weekday, for today
		Assertions.assertEquals(CalendarConversions.Calendar.values().length + 2, converterTables.getFirst().getRowCount());
		Assertions.assertTrue(converterButtons.contains(I18N.t("button.other_calendars")), converterButtons.toString());
		Assertions.assertFalse(converterButtons.contains(I18N.t("button.close")), converterButtons.toString());
		Assertions.assertTrue(calculatorButtons.contains(I18N.t("button.other_calendars_and_ages")), calculatorButtons.toString());
		//opened from a menu there is no date to apply a suggestion to
		Assertions.assertFalse(assistantButtons.contains(I18N.t("dialog.date_assistant.apply")), assistantButtons.toString());
		Assertions.assertTrue(assistantButtons.contains(I18N.t("dialog.date_assistant.copy")), assistantButtons.toString());
	}

	@Test
	void theDateToolsAreInTheToolsMenuAndNotInTheEventMenu(){
		final List<String> dateTools = DateToolRegistry.tools().stream()
			.map(ToolOperation::getName)
			.toList();
		final List<String> eventTools = Stream.of(EventToolRegistry.primaryTools(), EventToolRegistry.referenceTools(),
				EventToolRegistry.analysisTools())
			.flatMap(List::stream)
			.map(ToolOperation::getName)
			.toList();

		Assertions.assertEquals(3, dateTools.size());
		Assertions.assertTrue(dateTools.stream().noneMatch(eventTools::contains), dateTools + " / " + eventTools);
		Assertions.assertTrue(dateTools.stream().noneMatch(String::isBlank));
	}

}
