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

import io.github.mtrevisan.calendars.assistant.AgeEstimator;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.LiturgicalTool;
import io.github.mtrevisan.calendars.assistant.Message;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.assistant.RomanDateTool;
import io.github.mtrevisan.calendars.assistant.SunTool;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The fixes of the review of the assistant tools, of the liturgical, regnal, Quaker and East Asian calendars. */
class AssistantReviewFixesTest{

	private static List<String> julian(final String expression, final int year){
		return RomanDateTool.interpret(expression, year).stream()
			.filter(s -> "julian".equals(s.calendarCode()))
			.map(DateSuggestion::normalizedDate)
			.toList();
	}


	//---------------------------------------------------------------- Roman dates

	@Test
	void bisSextumIsThe24thOfFebruaryInALeapYear(){
		//the bissextile day is inserted after the 23rd: the first sixth day is the 24th (bis sextum), the second the 25th
		Assertions.assertEquals(List.of("24 February 1504"), julian("ante diem bis sextum Kalendas Martias", 1504));
		//the plain sixth day is the 25th (the feast of Saint Matthias falls on it in the leap years), the 24th is the alternative
		Assertions.assertEquals(List.of("25 February 1504", "24 February 1504"), julian("ante diem VI Kal. Mart.", 1504));
		Assertions.assertEquals(List.of("24 February 1504", "23 February 1504"), julian("ante diem VII Kal. Mart.", 1504));
	}

	@Test
	void quintilisAndSextilis(){
		Assertions.assertEquals(List.of("1 July 1501"), julian("Kalendis Quintilibus", 1501));
		Assertions.assertEquals(List.of("1 August 1501"), julian("Kalendis Sextilibus", 1501));
		Assertions.assertEquals(List.of("5 July 1501"), julian("ante diem III Nonas Quintilis", 1501));
		Assertions.assertEquals(List.of("13 August 1501"), julian("Idibus Sextilis", 1501));
	}


	//---------------------------------------------------------------- liturgical tools

	@Test
	void feastReferenceFollowsTheReformInForce(){
		//Great Britain kept the Julian calendar and computus until 1752
		final DateSuggestion british = LiturgicalTool.resolveFeast("Easter 1700", CalendarReform.BRITAIN);
		final DateSuggestion roman = LiturgicalTool.resolveFeast("Easter 1700", CalendarReform.ROME);

		Assertions.assertEquals("julian", british.calendarCode());
		Assertions.assertEquals("gregorian", roman.calendarCode());
		Assertions.assertNotEquals(british.normalizedDate(), roman.normalizedDate());
		Assertions.assertEquals(CalendarType.LITURGICAL_JULIAN.parseToJdn("Easter 1700"),
			CalendarType.JULIAN.parseToJdn(british.normalizedDate()));
		Assertions.assertEquals(CalendarType.LITURGICAL.parseToJdn("Easter 1700"),
			CalendarType.GREGORIAN.parseToJdn(roman.normalizedDate()));
		//the same text, whatever the spacing
		Assertions.assertEquals(Message.text("Easter 1700"), roman.title());
	}

	@Test
	void holyThursdayIsMaundyThursday(){
		Assertions.assertEquals(CalendarType.LITURGICAL.parseToJdn("Maundy Thursday 2024"),
			CalendarType.LITURGICAL.parseToJdn("Holy Thursday 2024"));
		Assertions.assertEquals(CalendarType.LITURGICAL.parseToJdn("Easter 2024") - 3,
			CalendarType.LITURGICAL.parseToJdn("Holy Thursday 2024"));
		//the Ascension has its own names
		Assertions.assertEquals(CalendarType.LITURGICAL.parseToJdn("Easter 2024") + 39,
			CalendarType.LITURGICAL.parseToJdn("Ascension Thursday 2024"));
	}


	//---------------------------------------------------------------- relative dates

	@Test
	void aNegativeDistanceIsAnError(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> RelativeDateTool.offset("gregorian", "10 January 2024", -2, RelativeDateTool.Unit.DAYS, true));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> RelativeDateTool.offsetFromRange(2460000L, 2460010L, Message.text("birth"), -1, RelativeDateTool.Unit.WEEKS, false));
		Assertions.assertDoesNotThrow(
			() -> RelativeDateTool.offset("gregorian", "10 January 2024", 0, RelativeDateTool.Unit.DAYS, true));
	}


	//---------------------------------------------------------------- the age

	@Test
	void ordinalYearUsesTheRightSuffix(){
		//the suffix is chosen by the user interface (an ordinal pattern): the tool only hands over the number
		for(final int ordinal : new int[]{1, 2, 3, 11, 12, 13, 21, 72, 103, 111}){
			final DateSuggestion birth = AgeEstimator.birthFromAge("julian", "10 January 1500", ordinal, null,
				null, true, "death").getFirst();

			Assertions.assertTrue(birth.assumptions().contains(Message.of("date_assistant.assumption.ordinal_year", ordinal, ordinal - 1)),
				ordinal + ": " + birth.assumptions());
		}
	}

	@Test
	void aBareYearOfAnotherCalendarIsNotACivilYear(){
		//the Hebrew year 5786 is the Gregorian 2025/26: a person aged 30 was born in 1995 or 1996, not in the year 5756
		final DateSuggestion birth = AgeEstimator.birthFromAge("hebrew", "5786", 30, null, null, false, "death").getFirst();

		final String range = birth.notBefore() + " " + birth.notAfter();
		Assertions.assertTrue(range.contains("1995") || range.contains("1996"), range);
		Assertions.assertFalse(range.contains("5756"), range);
	}

	@Test
	void theYearOfTheReformSpansTheJulianAndGregorianPart(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("gregorian", "1582", 10, null, null, false, "death").getFirst();

		final long first = CalendarReform.ROME.toJdn(1582, 1, 1);
		final long last = CalendarReform.ROME.toJdn(1582, 12, 31);
		//the year has 355 days, the ten days of the reform being skipped
		Assertions.assertEquals(354L, last - first);
		Assertions.assertEquals("date_assistant.step.event_date_range", birth.steps().getFirst().key());
		Assertions.assertTrue(birth.steps().getFirst().args().containsAll(List.of(first, last)), birth.steps().getFirst().toString());
	}

	@Test
	void yearsBeforeChristAreWrittenBC(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("gregorian", "1 January 10 BC", 5, null, null, false, "death")
			.getFirst();

		final String written = (birth.normalizedDate() != null? birth.normalizedDate(): birth.notBefore());
		Assertions.assertTrue(written.endsWith(" BC"), written);
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn(written), CalendarType.GREGORIAN.parseToJdn(written));
	}


	//---------------------------------------------------------------- the sun

	@Test
	void shiftedDateOfAnotherCalendarIsWrittenInTheCalendarInForce(){
		//1 Tishrei 5784 is 16 September 2023: the day after is a Gregorian date, not a (proleptic) Julian one
		final List<DateSuggestion> suggestions = SunTool.analyze("hebrew", "1 Tishrei 5784", 45.4, 12.3, SunTool.HourSystem.EQUAL, 2.);

		final DateSuggestion shifted = suggestions.getLast();
		Assertions.assertEquals(Message.of("date_assistant.title.sunset"), shifted.title());
		Assertions.assertEquals("gregorian", shifted.calendarCode());
		Assertions.assertEquals("17 September 2023", shifted.normalizedDate());
	}

	@Test
	void shiftedDateOfACivilCalendarKeepsItsCalendar(){
		final List<DateSuggestion> suggestions = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.EQUAL, 2.);

		Assertions.assertEquals("julian", suggestions.getLast().calendarCode());
	}


	//---------------------------------------------------------------- regnal dates

	@Test
	void edwardIVDoesNotReignDuringTheReadeption(){
		//the 5th of February 1471 (11th year of Edward IV) falls within the readeption of Henry VI: not Edward's day
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.ENGLISH_REGNAL.parseToJdn("5 February 10 Edward IV"));
		//before and after it the years of Edward IV are unbroken from 4 March 1461
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 June 1471"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("1 June 11 Edward IV"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 June 1469"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("1 June 9 Edward IV"));
		//a year alone is not ambiguous between the two parts of the reign
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("4 March 1470"), CalendarType.ENGLISH_REGNAL.parseToJdn("10 Edward IV"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("11 April 1471") + 1,
			CalendarType.ENGLISH_REGNAL.parseToJdn("11 Edward IV"));
		//the readeption itself, in the 49th year of Henry VI
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 February 1471"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("5 February 49 Henry VI"));
	}

	@Test
	void popesAreLoadedFromTheTable(){
		Assertions.assertDoesNotThrow(() -> CalendarType.PONTIFICAL.parseToJdn("anno III del pontificato di Pio V"));
	}


	//---------------------------------------------------------------- Quaker dates

	@Test
	void dualYearMustBeTheNextYear(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("4 January 1703"), CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/3"));
		Assertions.assertEquals(CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/3"),
			CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/1703"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/5"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/1"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/1710"));
	}

	@Test
	void quakerWordOrdinals(){
		Assertions.assertEquals(CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/3"),
			CalendarType.QUAKER.parseToJdn("the fourth of the eleventh month 1702/3"));
		Assertions.assertEquals(CalendarType.QUAKER.parseToJdn("21 da 3 mo 1700"),
			CalendarType.QUAKER.parseToJdn("the twenty-first day of the third month 1700"));
		Assertions.assertEquals(CalendarType.QUAKER.parseToJdn("30 da 3 mo 1700"),
			CalendarType.QUAKER.parseToJdn("thirtieth day third month 1700"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("the foo of the third month 1700"));
	}

}
