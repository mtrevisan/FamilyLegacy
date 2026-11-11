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

import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.text.DateFormulas;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Locale;


class GregorianParserFixesTest{

	private static long gregorian(final String date){
		return CalendarType.GREGORIAN.parseToJdn(date);
	}

	private static void assertRejected(final CalendarType type, final String date){
		Assertions.assertThrows(IllegalArgumentException.class, () -> type.parseToJdn(date), date);
	}


	@Test
	void beforeChristMarkersAreCaseInsensitive(){
		final long expected = gregorian("15 March 44 BC");

		for(final String date : new String[]{"15 March 44 a.C.", "15 March 44 A.C.", "15 March 44 a. C.", "15 March 44 A. C.",
				"15 March 44 a.c.", "15 March 44 ac", "15 March 44 bc", "15 March 44 B.C.", "15 March 44 BCE", "15 March 44 b.c.e.",
				"15 March 44 a. de C.", "15 March 44 a. C.", "15 March 44 a. C."})
			Assertions.assertEquals(expected, gregorian(date), date);
	}

	@Test
	void yearZeroIsAPresentYear(){
		// 1 BC is the astronomical year 0: it is not a missing year
		final CalendarMetadata metadata = CalendarType.GREGORIAN.getMetadata("1 January 1 BC");

		Assertions.assertTrue(metadata.nativeComponents().get(CalendarMetadata.COMPONENT_YEAR));
		Assertions.assertTrue(metadata.isComplete());
		Assertions.assertTrue(CalendarMetadata.createFromYearMonthDay(true, 3, 4).isComplete());
		Assertions.assertFalse(CalendarMetadata.createFromYearMonthDay(false, 3, 4).isComplete());
	}

	@Test
	void dualYear(){
		Assertions.assertEquals(gregorian("12 2 1705"), gregorian("12 FEB 1704/5"));
		Assertions.assertEquals(gregorian("12 2 1705"), gregorian("12 FEB 1704/05"));
		Assertions.assertEquals(gregorian("12 2 1705"), gregorian("12 FEB 1704/1705"));
		Assertions.assertEquals(gregorian("1 1 1700"), gregorian("1 Jan 1699/00"));
		Assertions.assertEquals(gregorian("10 3 1710"), gregorian("10 Mar 1709/10"));
		Assertions.assertEquals(gregorian("24 3 1705"), gregorian("24 March 1704/5"));
	}

	@Test
	void dualYearMustFollowTheFirstYear(){
		assertRejected(CalendarType.GREGORIAN, "12 FEB 1704/6");
		assertRejected(CalendarType.GREGORIAN, "12 FEB 1704/1706");
		assertRejected(CalendarType.GREGORIAN, "12 FEB 1704/4");
		assertRejected(CalendarType.GREGORIAN, "12 FEB 1704/04");
	}

	@Test
	void dualYearIsValidOnlyBeforeTheStartOfTheYear(){
		assertRejected(CalendarType.GREGORIAN, "25 March 1704/5");
		assertRejected(CalendarType.GREGORIAN, "12 April 1704/5");
		assertRejected(CalendarType.GREGORIAN, "12 AUG 1752/53");
		assertRejected(CalendarType.JULIAN, "12 Dec 1704/5");
	}

	@Test
	void seasonWithADayIsAnError(){
		assertRejected(CalendarType.GREGORIAN, "5 inverno 1500");
		assertRejected(CalendarType.GREGORIAN, "5 winter 1500");
		assertRejected(CalendarType.JULIAN, "5 inverno 1500");
		assertRejected(CalendarType.REFORMED_JULIAN, "5 inverno 1500");
		assertRejected(CalendarType.FLORENTINE, "5 inverno 1500");
		assertRejected(CalendarType.SWEDISH, "5 inverno 1700");
		assertRejected(CalendarType.EASTER_STYLE, "5 inverno 1500");
		// the season alone is fine
		Assertions.assertDoesNotThrow(() -> gregorian("inverno 1500"));
	}

	@Test
	void languageCodeNextToAYearAloneIsNotTheLanguageOfTheDate(){
		// "(ca)" (circa, or Catalan) after a year alone does not declare a language
		Assertions.assertEquals(gregorian("1500"), gregorian("1500 (ca)"));
		// next to a full date it does
		Assertions.assertEquals(gregorian("1 11 2006"), gregorian("1 pay 2006 (asturiano)"));
	}

	@Test
	void monthOfDifferentLanguagesIsChosenAlwaysInTheSameWay(){
		// "pay" is November in Asturian and July in Nuer: the language code that comes first wins
		final long first = gregorian("1 pay 2006");

		Assertions.assertEquals(gregorian("1 11 2006"), first);
		for(int i = 0; i < 5; i ++)
			Assertions.assertEquals(first, gregorian("1 pay 2006"));
	}

	@Test
	void interpretationsNeverThrow(){
		for(final String date : new String[]{"est 1850", "hydref 1500", "yaz 2024", "pay 2006", "1 pay 2006", "lis 1850",
				"her 1500", "pro 1500", "1500", "xyz 1500"})
			for(final CalendarType type : new CalendarType[]{CalendarType.GREGORIAN, CalendarType.JULIAN, CalendarType.SWEDISH,
					CalendarType.EASTER_STYLE, CalendarType.FLORENTINE, CalendarType.VENETAN, CalendarType.BERBER})
				Assertions.assertDoesNotThrow(() -> type.getInterpretations(date, Locale.ITALIAN), date + " " + type);
	}

	@Test
	void formulasAndMarkers(){
		Assertions.assertEquals("5 marzo 1500", DateFormulas.removeFormulas("adì 5 de marzo del 1500"));
		Assertions.assertEquals("5 marzo 1500", DateFormulas.removeFormulas("die 5 mensis marzo anno Domini 1500 A.D."));
		Assertions.assertEquals("5 marzo", DateFormulas.removeFormulas("5th marzo"));
		Assertions.assertTrue(DateFormulas.extractEra("44 a. C.").beforeChrist());
		Assertions.assertFalse(DateFormulas.extractEra("44").beforeChrist());
		Assertions.assertEquals(0, DateFormulas.toAstronomicalYear(1, true));
		Assertions.assertThrows(IllegalArgumentException.class, () -> DateFormulas.toAstronomicalYear(0, true));
	}

}
