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
import io.github.mtrevisan.calendars.assistant.CalendarConversionTool;
import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.LiturgicalTool;
import io.github.mtrevisan.calendars.assistant.RelativeDateTool;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.assistant.SunTool;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


class DateAssistantToolsTest{

	private static DateSuggestion byCalendar(final List<DateSuggestion> suggestions, final String calendarCode){
		return suggestions.stream()
			.filter(s -> calendarCode.equals(s.calendarCode()))
			.findFirst()
			.orElseThrow();
	}


	@Test
	void conversionRoundTrips(){
		for(final String date : new String[]{"12 January 1400", "5 October 1582", "15 October 1582", "29 February 2024",
				"1 March 1700", "31 December 1999"}){
			final long jdn = CalendarType.GREGORIAN.parseToJdn(date);
			final List<DateSuggestion> suggestions = CalendarConversionTool.convert("gregorian", date);

			Assertions.assertEquals(2, suggestions.size());
			for(final DateSuggestion suggestion : suggestions)
				Assertions.assertEquals(jdn, CalendarType.fromCode(suggestion.calendarCode()).parseToJdn(suggestion.normalizedDate()),
					date + " -> " + suggestion.normalizedDate());
		}
	}

	@Test
	void conversionAcrossTheReform(){
		//4 October 1582 (Julian) was followed by 15 October 1582 (Gregorian)
		Assertions.assertEquals("14 October 1582",
			byCalendar(CalendarConversionTool.convert("julian", "4 October 1582"), "gregorian").normalizedDate());
		Assertions.assertEquals("4 October 1582",
			byCalendar(CalendarConversionTool.convert("gregorian", "14 October 1582"), "julian").normalizedDate());
		Assertions.assertEquals("4 January 1000",
			byCalendar(CalendarConversionTool.convert("gregorian", "9 January 1000"), "julian").normalizedDate());
	}

	@Test
	void conversionFromYearStartStyle(){
		final List<DateSuggestion> suggestions = CalendarConversionTool.convert("venetan", "15 febraro 1499 mv");

		//February 1499 more veneto is February 1500 of the common style
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("15 February 1500"),
			CalendarType.JULIAN.parseToJdn(byCalendar(suggestions, "julian").normalizedDate()));
		Assertions.assertEquals("julian", suggestions.getFirst().calendarCode());
		Assertions.assertFalse(suggestions.getFirst().assumptions().isEmpty());
	}

	@Test
	void feastsGregorian(){
		final Map<String, DateSuggestion> feasts = LiturgicalTool.feasts(2024, CalendarReform.ROME).stream()
			.collect(Collectors.toMap(DateSuggestion::title, Function.identity()));

		Assertions.assertEquals("14 February 2024", feasts.get("Ash Wednesday").normalizedDate());
		Assertions.assertEquals("31 March 2024", feasts.get("Easter Sunday").normalizedDate());
		Assertions.assertEquals("9 May 2024", feasts.get("Ascension").normalizedDate());
		Assertions.assertEquals("19 May 2024", feasts.get("Pentecost").normalizedDate());
		Assertions.assertEquals("1 December 2024", feasts.get("First Sunday of Advent").normalizedDate());
		Assertions.assertEquals("6 January 2024", feasts.get("Epiphany").normalizedDate());
	}

	@Test
	void feastsJulianComputus(){
		final Map<String, DateSuggestion> feasts = LiturgicalTool.feasts(2024, null).stream()
			.collect(Collectors.toMap(DateSuggestion::title, Function.identity()));

		//Orthodox Easter of 2024 is 5 May Gregorian, that is 22 April Julian
		Assertions.assertEquals("22 April 2024", feasts.get("Easter Sunday").normalizedDate());
		Assertions.assertEquals("julian", feasts.get("Easter Sunday").calendarCode());
		Assertions.assertEquals("10 June 2024", feasts.get("Pentecost").normalizedDate());
	}

	@Test
	void feastsAgreeWithTheLiturgicalCalendar(){
		for(final int year : new int[]{1300, 1500, 1582, 1583, 1752, 1800, 2000, 2024}){
			final Map<String, DateSuggestion> feasts = LiturgicalTool.feasts(year, CalendarReform.ROME).stream()
				.collect(Collectors.toMap(DateSuggestion::title, Function.identity()));

			assertFeast(feasts, "Easter Sunday", "Pasqua " + year, year);
			assertFeast(feasts, "Ash Wednesday", "Mercoledì delle Ceneri " + year, year);
			assertFeast(feasts, "Pentecost", "Pentecoste " + year, year);
			assertFeast(feasts, "First Sunday of Advent", "Avvento " + year, year);
		}
	}

	private static void assertFeast(final Map<String, DateSuggestion> feasts, final String title, final String expression,
			final int year){
		final DateSuggestion feast = feasts.get(title);
		Assertions.assertEquals(CalendarType.LITURGICAL.parseToJdn(expression),
			CalendarType.fromCode(feast.calendarCode()).parseToJdn(feast.normalizedDate()), expression + " (" + year + ")");
	}

	@Test
	void feastReference(){
		final DateSuggestion suggestion = LiturgicalTool.resolveFeast("Giovedì Santo 2024", CalendarReform.ROME);

		Assertions.assertEquals("28 March 2024", suggestion.normalizedDate());
		Assertions.assertEquals("gregorian", suggestion.calendarCode());
		Assertions.assertThrows(IllegalArgumentException.class, () -> LiturgicalTool.resolveFeast("not a feast", CalendarReform.ROME));
	}

	@Test
	void relativeToEaster(){
		Assertions.assertEquals("3 April 2024", LiturgicalTool.relativeToEaster(2024, 3, CalendarReform.ROME).normalizedDate());
		Assertions.assertEquals("17 March 2024", LiturgicalTool.relativeToEaster(2024, -14, CalendarReform.ROME).normalizedDate());
		Assertions.assertTrue(LiturgicalTool.relativeToEaster(2024, 1, CalendarReform.ROME).title().startsWith("1 day after"));
	}

	@Test
	void relativeToDate(){
		final List<DateSuggestion> after = RelativeDateTool.offset("gregorian", "28 February 2024", 2, RelativeDateTool.Unit.DAYS,
			true);
		Assertions.assertEquals("1 March 2024", byCalendar(after, "gregorian").normalizedDate());

		final List<DateSuggestion> before = RelativeDateTool.offset("gregorian", "10 January 2024", 2, RelativeDateTool.Unit.WEEKS,
			false);
		Assertions.assertEquals("27 December 2023", byCalendar(before, "gregorian").normalizedDate());

		//40 days after a birth on 1 March 1500 (Julian)
		final List<DateSuggestion> churching = RelativeDateTool.offset("julian", "1 March 1500", 40, RelativeDateTool.Unit.DAYS,
			true);
		Assertions.assertEquals("10 April 1500", byCalendar(churching, "julian").normalizedDate());
	}

	@Test
	void sunAnalysis(){
		Assumptions.assumeTrue(EphemerisEngineFactory.getEngine().isAvailable(), "The Ephemeris plugin is not installed");

		final List<DateSuggestion> suggestions = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.EQUAL,
			1.);

		Assertions.assertEquals(3, suggestions.size());
		Assertions.assertNull(suggestions.getFirst().normalizedDate());
		Assertions.assertEquals("12 January 1400", suggestions.get(1).normalizedDate());
		Assertions.assertEquals("13 January 1400", suggestions.get(2).normalizedDate());
	}

	@Test
	void sunAnalysisBeforeSunset(){
		Assumptions.assumeTrue(EphemerisEngineFactory.getEngine().isAvailable(), "The Ephemeris plugin is not installed");

		final List<DateSuggestion> suggestions = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.EQUAL,
			-1.);

		//before sunset the date is the same under every reckoning
		Assertions.assertEquals(2, suggestions.size());
		Assertions.assertEquals("12 January 1400", suggestions.get(1).normalizedDate());
	}

	@Test
	void sunAnalysisAfterMidnight(){
		Assumptions.assumeTrue(EphemerisEngineFactory.getEngine().isAvailable(), "The Ephemeris plugin is not installed");

		//in January at 45°N the sunset is about 16:40 local mean time: 8 hours later is after midnight
		final List<DateSuggestion> suggestions = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.EQUAL,
			8.);

		Assertions.assertEquals("13 January 1400", suggestions.get(1).normalizedDate());
		Assertions.assertTrue(suggestions.get(1).title().contains("after midnight"));
		Assertions.assertEquals("13 January 1400", suggestions.get(2).normalizedDate());
	}

	@Test
	void sunAnalysisUnequalHours(){
		Assumptions.assumeTrue(EphemerisEngineFactory.getEngine().isAvailable(), "The Ephemeris plugin is not installed");

		final DateSuggestion equal = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.EQUAL, 6.).get(1);
		final DateSuggestion unequal = SunTool.analyze("julian", "12 January 1400", 45.4, 12.3, SunTool.HourSystem.UNEQUAL, 6.)
			.get(1);

		//the winter night is longer than 12 hours, so the temporal hours are longer than 60 minutes: the event is later
		Assertions.assertTrue(unequal.explanation().contains("Temporal hour"));
		Assertions.assertNotEquals(equal.meaning(), unequal.meaning());
		Assertions.assertTrue(unequal.steps().stream().anyMatch(s -> s.startsWith("Night:")));
	}

	@Test
	void birthFromAgeInYears(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("julian", "10 January 1500", 72, null, null, false, "death").getFirst();

		//born on 11 January 1427 the person is still 72 on 10 January 1500, on 10 January 1428 it has just turned 72
		Assertions.assertTrue(birth.isBounded());
		Assertions.assertEquals("11 January 1427", birth.notBefore());
		Assertions.assertEquals("10 January 1428", birth.notAfter());
		Assertions.assertEquals("julian", birth.calendarCode());
	}

	@Test
	void birthFromExactAge(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("gregorian", "15 March 1700", 30, 2, 5, false, "marriage").getFirst();

		Assertions.assertFalse(birth.isBounded());
		Assertions.assertEquals("10 January 1670", birth.normalizedDate());
	}

	@Test
	void birthFromAgeInMonths(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("gregorian", "15 March 1700", 30, 2, null, false, "death").getFirst();

		//30 years and 2 months old on 15 March 1700: born from 16 December 1669 to 15 January 1670
		Assertions.assertEquals("16 December 1669", birth.notBefore());
		Assertions.assertEquals("15 January 1670", birth.notAfter());
	}

	@Test
	void birthFromOrdinalAge(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("julian", "10 January 1500", 72, null, null, true, "death").getFirst();

		//in the 72nd year of life means 71 years completed
		Assertions.assertEquals("11 January 1428", birth.notBefore());
		Assertions.assertEquals("10 January 1429", birth.notAfter());
	}

	@Test
	void birthFromAgeAtYear(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("julian", "1500", 72, null, null, false, "death").getFirst();

		Assertions.assertEquals("2 January 1427", birth.notBefore());
		Assertions.assertEquals("31 December 1428", birth.notAfter());
	}

	@Test
	void birthFromAgeLeapDay(){
		final DateSuggestion birth = AgeEstimator.birthFromAge("gregorian", "29 February 2024", 1, null, null, false, "census")
			.getFirst();

		//a person born on 1 March 2022 is 1 year and 11 months old on 29 February 2024
		Assertions.assertEquals("1 March 2022", birth.notBefore());
		Assertions.assertEquals("28 February 2023", birth.notAfter());
	}

	@Test
	void birthFromAgeNotValid(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> AgeEstimator.birthFromAge("gregorian", "1 January 1900", -1, null, null, false, "death"));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> AgeEstimator.birthFromAge("gregorian", "1 January 1900", 0, null, null, true, "death"));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> AgeEstimator.birthFromAge("gregorian", "1 January 1900", 5, 12, null, false, "death"));
	}

	@Test
	void relativeToRange(){
		final long from = CalendarType.GREGORIAN.parseToJdn("1 March 1500");
		final long to = CalendarType.GREGORIAN.parseToJdn("31 March 1500");

		final DateSuggestion range = RelativeDateTool.offsetFromRange(from, to, "birth", 40, RelativeDateTool.Unit.DAYS, true).getFirst();
		Assertions.assertTrue(range.isBounded());
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 March 1500") + 40,
			CalendarType.fromCode(range.calendarCode()).parseToJdn(range.notBefore()));
		Assertions.assertEquals(from + 40 + 30, CalendarType.fromCode(range.calendarCode()).parseToJdn(range.notAfter()));

		//a single day gives the usual candidates
		Assertions.assertEquals(2, RelativeDateTool.offsetFromRange(from, from, "birth", 40, RelativeDateTool.Unit.DAYS, true).size());
	}

	@Test
	void explanation(){
		final DateSuggestion suggestion = CalendarConversionTool.convert("gregorian", "12 January 1400").getLast();
		final String explanation = suggestion.explanation();

		Assertions.assertTrue(explanation.contains("Interpreted meaning:"));
		Assertions.assertTrue(explanation.contains("Assumptions:"));
		Assertions.assertTrue(explanation.contains("Conversion steps:"));
		Assertions.assertTrue(explanation.contains("Normalized date: " + suggestion.normalizedDate()));
		Assertions.assertTrue(suggestion.isApplicable());
	}

}
