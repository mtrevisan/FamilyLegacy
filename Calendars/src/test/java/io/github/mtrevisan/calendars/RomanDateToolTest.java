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

import io.github.mtrevisan.calendars.assistant.DateSuggestion;
import io.github.mtrevisan.calendars.assistant.RomanDateTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class RomanDateToolTest{

	private static List<String> julian(final String expression, final int year){
		return RomanDateTool.interpret(expression, year).stream()
			.filter(s -> "julian".equals(s.calendarCode()))
			.map(DateSuggestion::normalizedDate)
			.toList();
	}


	@Test
	void kalendsNonesIdes(){
		Assertions.assertEquals(List.of("1 January 1501"), julian("Kalendis Ianuariis", 1501));
		Assertions.assertEquals(List.of("7 March 1501"), julian("Nonis Martiis", 1501));
		Assertions.assertEquals(List.of("7 May 1501"), julian("Nonas Maias", 1501));
		Assertions.assertEquals(List.of("5 April 1501"), julian("Nonis Aprilibus", 1501));
		Assertions.assertEquals(List.of("15 March 1501"), julian("Idibus Martiis", 1501));
		Assertions.assertEquals(List.of("13 November 1501"), julian("Idus Novembres", 1501));
		Assertions.assertEquals(List.of("15 October 1501"), julian("Id. Oct.", 1501));
	}

	@Test
	void daysBefore(){
		Assertions.assertEquals(List.of("28 February 1501"), julian("pridie Kalendas Martias", 1501));
		Assertions.assertEquals(List.of("5 March 1501"), julian("ante diem III Nonas Martias", 1501));
		Assertions.assertEquals(List.of("6 March 1501"), julian("pridie Nonas Martias", 1501));
		Assertions.assertEquals(List.of("14 February 1501"), julian("a.d. XVI Kal. Mart.", 1501));
		Assertions.assertEquals(List.of("13 March 1501"), julian("a. d. III Id. Mart.", 1501));
		Assertions.assertEquals(List.of("2 March 1501"), julian("ante diem VI Nonas Martias", 1501));
		Assertions.assertEquals(List.of("11 June 1501"), julian("ante diem III Idus Iunias", 1501));
	}

	@Test
	void kalendsOfJanuaryBelongToTheNextYear(){
		Assertions.assertEquals(List.of("31 December 1501"), julian("pridie Kalendas Ianuarias", 1501));
		Assertions.assertEquals(List.of("30 December 1500"), julian("ante diem III Kal. Ian.", 1500));
	}

	@Test
	void leapYearHasBothReckonings(){
		//the 24 February is the classical sixth day before the Kalends of March, the 25 the arithmetic one
		Assertions.assertEquals(List.of("25 February 1504", "24 February 1504"), julian("ante diem VI Kal. Mart.", 1504));
		Assertions.assertEquals(List.of("24 February 1504", "23 February 1504"), julian("ante diem VII Kal. Mart.", 1504));
		Assertions.assertEquals(List.of("25 February 1504"), julian("ante diem bis sextum Kalendas Martias", 1504));
		//after the leap day there is no difference
		Assertions.assertEquals(List.of("28 February 1504"), julian("ante diem III Kal. Mart.", 1504));
		Assertions.assertEquals(List.of("29 February 1504"), julian("pridie Kal. Mart.", 1504));
	}

	@Test
	void englishWords(){
		Assertions.assertEquals(List.of("15 March 1501"), julian("the Ides of March", 1501));
		Assertions.assertEquals(List.of("28 February 1501"), julian("the day before the Kalends of March", 1501));
		Assertions.assertEquals(List.of("1 April 1501"), julian("Kalends of April", 1501));
	}

	@Test
	void theGregorianCandidateIsTheConversion(){
		final List<DateSuggestion> suggestions = RomanDateTool.interpret("Idibus Martiis", 1501);

		Assertions.assertEquals(2, suggestions.size());
		final long jdn = CalendarType.JULIAN.parseToJdn("15 March 1501");
		for(final DateSuggestion suggestion : suggestions)
			Assertions.assertEquals(jdn, CalendarType.fromCode(suggestion.calendarCode()).parseToJdn(suggestion.normalizedDate()));
	}

	@Test
	void invalidExpressions(){
		for(final String expression : new String[]{"", "ante diem III Martias", "Kalendis", "ante diem XX Kalendas Martias",
				"ante diem VII Nonas Ianuarias", "ante diem III Idus Martiis Aprilis", "foo Kalendas Martias"})
			Assertions.assertThrows(IllegalArgumentException.class, () -> RomanDateTool.interpret(expression, 1501), expression);
		Assertions.assertThrows(IllegalArgumentException.class, () -> RomanDateTool.interpret("Idibus Martiis", 0));
		//bis only in a leap year
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> RomanDateTool.interpret("ante diem bis sextum Kalendas Martias", 1501));
	}

}
