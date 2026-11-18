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
package io.github.mtrevisan.calendars;

import io.github.mtrevisan.calendars.assistant.AgeCluesTool;
import io.github.mtrevisan.calendars.assistant.AssistantInputException;
import io.github.mtrevisan.calendars.assistant.CalendarConversionTool;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.DayRange;
import io.github.mtrevisan.calendars.assistant.IndictionTool;
import io.github.mtrevisan.calendars.assistant.IntervalTool;
import io.github.mtrevisan.calendars.assistant.Message;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.assistant.WeekdayTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;


/** The interval between two dates, the ages put together, the checks of the weekday and of the indiction, and the dual dating. */
class DateIntervalAndChecksToolsTest{

	private static final String GREGORIAN = CalendarType.GREGORIAN.getCode();
	private static final String JULIAN = CalendarType.JULIAN.getCode();


	private static DayRange gregorian(final String date){
		return DayRange.parse(GREGORIAN, date);
	}

	/** The key of the message of the title of the only suggestion, and its arguments. */
	private static Message title(final List<DateSuggestion> suggestions){
		Assertions.assertEquals(1, suggestions.size());
		return suggestions.getFirst().title();
	}

	/** The period of a title of an interval, as the years, months and days of its parts. */
	private static String period(final Message message){
		return parts((Message)message.args().getFirst());
	}

	/** The numbers of the parts of a period message, separated by commas. */
	private static String parts(final Message period){
		return period.args().stream()
			.map(part -> ((Message)part).args().getFirst().toString())
			.reduce((a, b) -> a + "," + b)
			.orElseThrow();
	}


	@Test
	void theIntervalIsInYearsMonthsAndDays(){
		final Message title = title(IntervalTool.between(gregorian("29 February 1980"), gregorian("28 February 2020")));

		Assertions.assertEquals("date_assistant.title.interval", title.key());
		//39 years, 11 months, 30 days: the same as the Java period
		Assertions.assertEquals("39,11,30", period(title));
	}

	@Test
	void anIntervalInReverseOrderIsCountedBackwards(){
		final List<DateSuggestion> suggestions = IntervalTool.between(gregorian("1 January 2020"), gregorian("31 December 2019"));

		Assertions.assertEquals("1", period(title(suggestions)));
		Assertions.assertEquals("date_assistant.meaning.interval_reversed", suggestions.getFirst().meaning().key());
	}

	@Test
	void anIntervalWithAYearOnlyGivesTheClosestAndTheFarthest(){
		final Message title = title(IntervalTool.between(gregorian("1500"), gregorian("1 January 1502")));

		Assertions.assertEquals("date_assistant.title.interval_range", title.key());
		//from 31 December 1500 to 1 January 1502 at the closest, from 1 January 1500 at the farthest
		Assertions.assertEquals("1,1", parts((Message)title.args().get(0)));
		Assertions.assertEquals("2", parts((Message)title.args().get(1)));
	}

	@Test
	void theIntervalIsCountedInTheCalendarOfTheFirstDate(){
		//a Julian anniversary: 5 March 1450 to 5 March 1451 is one year even if the Gregorian dates are not on the same day
		final Message title = title(IntervalTool.between(DayRange.parse(JULIAN, "5 March 1450"), DayRange.parse(JULIAN, "5 March 1451")));

		Assertions.assertEquals("1", period(title));
	}


	@Test
	void severalAgesNarrowTheBirth(){
		//72 years old at the death on 15 June 1900: born from 16 June 1827 to 15 June 1828
		final AgeCluesTool.Clue death = new AgeCluesTool.Clue(gregorian("15 June 1900"), 72, null, null, false, "death");
		//30 years old on 1 January 1858: born from 2 January 1827 to 1 January 1828
		final AgeCluesTool.Clue census = new AgeCluesTool.Clue(gregorian("1 January 1858"), 30, null, null, false, "census");

		final DateSuggestion result = AgeCluesTool.intersect(List.of(death, census)).getFirst();

		Assertions.assertEquals("date_assistant.title.birth_range", result.title().key());
		Assertions.assertEquals("16 June 1827", result.notBefore());
		Assertions.assertEquals("1 January 1828", result.notAfter());
	}

	@Test
	void severalAgesThatAgreeOnADayGiveTheDay(){
		final AgeCluesTool.Clue death = new AgeCluesTool.Clue(gregorian("15 June 1900"), 72, 0, 0, false, "death");
		final AgeCluesTool.Clue census = new AgeCluesTool.Clue(gregorian("15 June 1850"), 22, null, null, false, "census");

		final DateSuggestion result = AgeCluesTool.intersect(List.of(death, census)).getFirst();

		Assertions.assertEquals("15 June 1828", result.normalizedDate());
	}

	@Test
	void agesThatContradictOneAnotherAreSaid(){
		final AgeCluesTool.Clue death = new AgeCluesTool.Clue(gregorian("15 June 1900"), 72, null, null, false, "death");
		final AgeCluesTool.Clue marriage = new AgeCluesTool.Clue(gregorian("15 June 1850"), 40, null, null, false, "marriage");

		final DateSuggestion result = AgeCluesTool.intersect(List.of(death, marriage)).getFirst();

		Assertions.assertEquals("date_assistant.title.clues_conflict", result.title().key());
		Assertions.assertFalse(result.isApplicable());
	}

	@Test
	void oneAgeIsNotEnoughToIntersect(){
		final AgeCluesTool.Clue death = new AgeCluesTool.Clue(gregorian("15 June 1900"), 72, null, null, false, "death");

		Assertions.assertThrows(AssistantInputException.class, () -> AgeCluesTool.intersect(List.of(death)));
	}


	@Test
	void theWeekdayOfADateIsChecked(){
		//31 December 1853 was a Saturday
		Assertions.assertEquals("date_assistant.title.weekday_ok",
			title(WeekdayTool.check(GREGORIAN, "31 December 1853", DayOfWeek.SATURDAY)).key());

		final DateSuggestion wrong = WeekdayTool.check(GREGORIAN, "31 December 1853", DayOfWeek.SUNDAY).getFirst();
		Assertions.assertEquals("date_assistant.title.weekday_wrong", wrong.title().key());
		//the nearest Sunday is the following day
		Assertions.assertTrue(wrong.steps().stream().anyMatch(step -> "date_assistant.step.weekday_nearest_after".equals(step.key())
			&& step.args().contains("1 January 1854")), wrong.steps().toString());
	}

	@Test
	void theWeekdayIsCheckedAlsoInTheOtherCivilCalendar(){
		//1 March 1700 was a Monday in the Gregorian calendar and a Friday in the Julian one (12 March Gregorian)
		final DateSuggestion gregorian = WeekdayTool.check(GREGORIAN, "1 March 1700", DayOfWeek.FRIDAY).getFirst();

		Assertions.assertEquals("date_assistant.title.weekday_wrong", gregorian.title().key());
		Assertions.assertTrue(gregorian.steps().stream().anyMatch(step -> "date_assistant.step.weekday_other_calendar".equals(step.key())),
			gregorian.steps().toString());
		Assertions.assertEquals("date_assistant.title.weekday_ok", title(WeekdayTool.check(JULIAN, "1 March 1700", DayOfWeek.FRIDAY)).key());
	}

	@Test
	void aYearIsNotADayForTheWeekday(){
		Assertions.assertThrows(AssistantInputException.class, () -> WeekdayTool.check(GREGORIAN, "1850", DayOfWeek.MONDAY));
	}


	@Test
	void theIndictionOfADateIsChecked(){
		//1450 is the 13th indiction (the 14th from September in the Greek and Bedan styles)
		Assertions.assertEquals("date_assistant.title.indiction_ok", title(IndictionTool.check(JULIAN, "5 March 1450", 13)).key());
		Assertions.assertEquals("date_assistant.title.indiction_wrong", title(IndictionTool.check(JULIAN, "5 March 1450", 14)).key());
		//a date from September onwards agrees with the indiction of the year and with the following one
		Assertions.assertEquals("date_assistant.title.indiction_ok", title(IndictionTool.check(JULIAN, "5 October 1450", 14)).key());
		Assertions.assertEquals("date_assistant.title.indiction_ok", title(IndictionTool.check(JULIAN, "5 October 1450", 13)).key());
	}

	@Test
	void theIndictionOfAWholeYearIsCheckedOverTheYear(){
		Assertions.assertEquals("date_assistant.title.indiction_ok", title(IndictionTool.check(JULIAN, "1450", 14)).key());
		Assertions.assertEquals("date_assistant.title.indiction_wrong", title(IndictionTool.check(JULIAN, "1450", 3)).key());
	}

	@Test
	void anInvalidIndictionIsRefused(){
		Assertions.assertThrows(AssistantInputException.class, () -> IndictionTool.check(JULIAN, "5 March 1450", 16));
		Assertions.assertThrows(AssistantInputException.class, () -> IndictionTool.years(0, 1400, 1500));
	}

	@Test
	void theYearsOfAnIndictionAreFound(){
		final DateSuggestion result = IndictionTool.years(13, 1430, 1470).getFirst();

		Assertions.assertEquals("date_assistant.title.indiction_years", result.title().key());
		//one year in fifteen: 1435, 1450 and 1465
		Assertions.assertEquals("1435, 1450, 1465", ((Message)result.title().args().get(1)).args().getFirst());
		Assertions.assertFalse(result.isApplicable());
	}

	@Test
	void anIntervalTooShortForTheIndictionHasNoYear(){
		Assertions.assertEquals("date_assistant.title.indiction_years_none", title(IndictionTool.years(5, 1450, 1451)).key());
		Assertions.assertThrows(AssistantInputException.class, () -> IndictionTool.years(5, 1500, 1450));
	}


	@Test
	void monthsAndYearsAreAddedInTheCalendar(){
		//nine months before the baptism of 15 December 1850
		final DateSuggestion before = byCalendar(RelativeDateTool.offset(GREGORIAN, "15 December 1850", 9, RelativeDateTool.Unit.MONTHS, false));
		Assertions.assertEquals("15 March 1850", before.normalizedDate());
		//the day that does not exist becomes the last day of the month
		Assertions.assertEquals("29 February 2024", byCalendar(RelativeDateTool.offset(GREGORIAN, "31 January 2024", 1,
			RelativeDateTool.Unit.MONTHS, true)).normalizedDate());
		Assertions.assertEquals("1 March 1500", byCalendar(RelativeDateTool.offset(GREGORIAN, "1 March 1450", 50,
			RelativeDateTool.Unit.YEARS, true)).normalizedDate());
	}

	private static DateSuggestion byCalendar(final List<DateSuggestion> suggestions){
		return suggestions.stream()
			.filter(s -> GREGORIAN.equals(s.calendarCode()))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void aJulianDateBeforeLadyDayIsGivenAlsoInOldStyle(){
		//12 February 1749 (counted from 1 January) is 1748 where the year began on 25 March: written 1748/49
		final List<DateSuggestion> suggestions = CalendarConversionTool.convertWithDualDating(JULIAN, "12 February 1749");

		final DateSuggestion dual = suggestions.stream()
			.filter(s -> "date_assistant.title.dual_dating".equals(s.title().key()))
			.findFirst()
			.orElseThrow();
		Assertions.assertEquals("1748/49", dual.title().args().get(2));
		Assertions.assertFalse(dual.isApplicable());

		//from 25 March, and after the adoption in Great Britain, there is nothing to say
		Assertions.assertTrue(CalendarConversionTool.convertWithDualDating(JULIAN, "26 March 1749").stream()
			.noneMatch(s -> "date_assistant.title.dual_dating".equals(s.title().key())));
		Assertions.assertTrue(CalendarConversionTool.convertWithDualDating(GREGORIAN, "12 February 1760").stream()
			.noneMatch(s -> "date_assistant.title.dual_dating".equals(s.title().key())));
	}

}
