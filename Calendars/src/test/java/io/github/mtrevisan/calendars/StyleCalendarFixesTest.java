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
import io.github.mtrevisan.calendars.text.Ordinals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


/**
 * Tests of the Venetan, Easter-style, year-start, Berber parsers, the ordinals, the calendar types, and the metadata.
 */
class StyleCalendarFixesTest{

	private static long parse(final CalendarType type, final String date){
		return type.parseToJdn(date);
	}

	private static void assertRejected(final CalendarType type, final String date){
		Assertions.assertThrows(IllegalArgumentException.class, () -> type.parseToJdn(date), date);
	}


	//Venetan
	@Test
	void venetanSeasonsAreAnchoredOnTheirOwnMonth(){
		// the Venetan year 1500 runs from March 1500 to February 1501: the seasons are the ones of the civil year 1500
		Assertions.assertEquals(parse(CalendarType.JULIAN, "spring 1500"), parse(CalendarType.VENETAN, "primavera 1500"));
		Assertions.assertEquals(parse(CalendarType.JULIAN, "summer 1500"), parse(CalendarType.VENETAN, "istà 1500"));
		Assertions.assertEquals(parse(CalendarType.JULIAN, "autumn 1500"), parse(CalendarType.VENETAN, "autun 1500"));
		Assertions.assertEquals(parse(CalendarType.JULIAN, "winter 1500"), parse(CalendarType.VENETAN, "invèrno 1500"));
	}

	@Test
	void venetanKnownMonthIsNotRewritten(){
		// "far" is the autumn: it must not become "fèr" through the "-aro" suffix rule
		Assertions.assertEquals(parse(CalendarType.VENETAN, "autun 1500"), parse(CalendarType.VENETAN, "far 1500"));
		// the old forms of the months still work
		Assertions.assertEquals(parse(CalendarType.VENETAN, "5 febrèr 1500"), parse(CalendarType.VENETAN, "5 febraro 1500"));
	}

	@Test
	void venetanSeasonWithADayIsAnError(){
		assertRejected(CalendarType.VENETAN, "5 invèrno 1500");
		assertRejected(CalendarType.VENETAN, "5 autun 1500");
	}

	@Test
	void venetanOrdinalDay(){
		final long expected = parse(CalendarType.VENETAN, "5 marzo 1500");

		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "5th marzo 1500"));
		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "5º marzo 1500"));
		Assertions.assertEquals(parse(CalendarType.VENETAN, "1 marzo 1500"), parse(CalendarType.VENETAN, "1° marzo 1500"));
		Assertions.assertEquals(parse(CalendarType.VENETAN, "1 marzo 1500"), parse(CalendarType.VENETAN, "primo marzo 1500"));
		assertRejected(CalendarType.VENETAN, "zz marzo 1500");
		assertRejected(CalendarType.VENETAN, "32 marzo 1500");
	}

	@Test
	void venetanEraFormulasAndLanguage(){
		final long expected = parse(CalendarType.VENETAN, "5 marzo 1500");

		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "adì 5 marzo 1500 A.D."));
		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "5 marzo 1500 (italiano)"));
		Assertions.assertEquals(parse(CalendarType.VENETAN, "5 marzo 1500"), parse(CalendarType.VENETAN, "5 marzo 1500 MV"));
		// before Christ: the Venetan year 44 BC is the astronomical year -43, as the Julian calendar written in the same way
		Assertions.assertEquals(parse(CalendarType.JULIAN, "5 March 44 BC"), parse(CalendarType.VENETAN, "5 marzo 44 a.C."));
		Assertions.assertEquals(parse(CalendarType.VENETAN, "5 marzo 0"), parse(CalendarType.VENETAN, "5 marzo 1 BC"));
		// the year of the metadata can be the astronomical year 0
		Assertions.assertTrue(CalendarType.VENETAN.getMetadata("5 marzo 1 BC").isComplete());
		Assertions.assertTrue(CalendarType.VENETAN.getInterpretations("5 marzo 1500", Locale.ITALIAN).isEmpty());
	}

	@Test
	void venetanDualYear(){
		// January and February of the civil year 1501 belong to the Venetan year 1500
		final long expected = parse(CalendarType.VENETAN, "5 zenèr 1500");

		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "5 zenèr 1500/1"));
		Assertions.assertEquals(expected, parse(CalendarType.VENETAN, "5 zenèr 1500/1501"));
		// the Julian 5 January 1501 is the Gregorian 15 January
		Assertions.assertEquals(parse(CalendarType.GREGORIAN, "15 1 1501"), parse(CalendarType.VENETAN, "5 zenèr 1500/1"));
		assertRejected(CalendarType.VENETAN, "5 marzo 1500/1");
		assertRejected(CalendarType.VENETAN, "5 zenèr 1500/2");
	}

	@Test
	void venetanHugeYearIsRejected(){
		assertRejected(CalendarType.VENETAN, "5 marzo 99999999999999");
		assertRejected(CalendarType.VENETAN, "99999999999999");
	}


	//Easter style and year start
	@Test
	void easterStyleYearAloneAndSeasons(){
		Assertions.assertEquals(parse(CalendarType.EASTER_STYLE, "1500"), parse(CalendarType.EASTER_STYLE, "1500"));
		assertRejected(CalendarType.EASTER_STYLE, "summer 1500");
		assertRejected(CalendarType.EASTER_STYLE, "inverno 1500");
	}

	@Test
	void easterStyleDualYearIsAlreadyFromJanuary(){
		Assertions.assertEquals(parse(CalendarType.GREGORIAN, "12 2 1705"), parse(CalendarType.EASTER_STYLE, "12 febbraio 1704/5"));
		// a qualifier adds nothing, as the date is not ambiguous
		Assertions.assertEquals(parse(CalendarType.GREGORIAN, "12 2 1705"),
			parse(CalendarType.EASTER_STYLE, "12 febbraio 1704/5 avant Pâques"));
		assertRejected(CalendarType.EASTER_STYLE, "12 febbraio 1704/6");
	}

	@Test
	void easterStyleQualifiersComeFromTheTokens(){
		final long before = parse(CalendarType.EASTER_STYLE, "10 april 1500 before Easter");

		Assertions.assertEquals(before, parse(CalendarType.EASTER_STYLE, "10 april 1500 avant Pâques"));
		Assertions.assertEquals(before, parse(CalendarType.EASTER_STYLE, "10 april 1500 ante Pascha"));
		Assertions.assertEquals(before, parse(CalendarType.EASTER_STYLE, "10 april 1500 prima di Pasqua"));
		Assertions.assertEquals(before, parse(CalendarType.EASTER_STYLE, "10 april 1500 vor Ostern"));
		Assertions.assertEquals(parse(CalendarType.EASTER_STYLE, "1 may 1500 after Easter"),
			parse(CalendarType.EASTER_STYLE, "1 may 1500 après Pâques"));
	}

	@Test
	void yearStartMonthAloneIsItsFirstDay(){
		// documented behaviour: a month alone stands for its first day, also when the month contains the start of the year
		Assertions.assertEquals(parse(CalendarType.FLORENTINE, "1 marzo 1600"), parse(CalendarType.FLORENTINE, "marzo 1600"));
		Assertions.assertEquals(parse(CalendarType.NATIVITY, "1 dicembre 1500"), parse(CalendarType.NATIVITY, "dicembre 1500"));
	}


	//Ordinals
	@Test
	void ordinalsFalsePositives(){
		Assertions.assertEquals(-1, Ordinals.parse("non"));
		Assertions.assertEquals(-1, Ordinals.parse("sei"));
		Assertions.assertEquals(-1, Ordinals.parse("un"));
		Assertions.assertEquals(-1, Ordinals.parse("quart"));
		Assertions.assertEquals(-1, Ordinals.parse("3i"));
		Assertions.assertEquals(9, Ordinals.parse("nono"));
		Assertions.assertEquals(9, Ordinals.parse("nona"));
		Assertions.assertEquals(9, Ordinals.parse("nonus"));
		Assertions.assertEquals(3, Ordinals.parse("3e"));
		Assertions.assertEquals(1, Ordinals.parse("una"));
		Assertions.assertEquals(6, Ordinals.parse("six"));
	}


	//Berber
	@Test
	void berberTwoWordsAreMonthAndYear(){
		// "5 2976" is the month 5 (Mayyu), never a day
		Assertions.assertEquals(parse(CalendarType.BERBER, "mayyu 2976"), parse(CalendarType.BERBER, "5 2976"));
		Assertions.assertEquals(parse(CalendarType.BERBER, "1 mayyu 2976"), parse(CalendarType.BERBER, "5 2976"));
		assertRejected(CalendarType.BERBER, "13 2976");
		assertRejected(CalendarType.BERBER, "5th 2976");
	}

	@Test
	void berberOrdinalDayAndLeapYears(){
		Assertions.assertEquals(parse(CalendarType.BERBER, "5 yennayer 2976"), parse(CalendarType.BERBER, "5th yennayer 2976"));
		// 2978 - 950 = 2028 is a Julian leap year
		Assertions.assertDoesNotThrow(() -> parse(CalendarType.BERBER, "29 furar 2978"));
		assertRejected(CalendarType.BERBER, "29 furar 2977");
		assertRejected(CalendarType.BERBER, "1 yennayer 99999999999");
		assertRejected(CalendarType.BERBER, "1 yennayer 9999999");
	}


	//CalendarType
	@Test
	void strictCalendarLookup(){
		Assertions.assertEquals(Optional.of(CalendarType.HEBREW), CalendarType.findByCode("HEBREW"));
		Assertions.assertEquals(Optional.empty(), CalendarType.findByCode("klingon"));
		Assertions.assertEquals(Optional.empty(), CalendarType.findByCode(null));
		Assertions.assertEquals(CalendarType.HEBREW, CalendarType.fromCodeOrThrow("hebrew"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.fromCodeOrThrow("klingon"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.fromCodeOrThrow(null));
		// the lenient lookup still defaults to the Gregorian calendar
		Assertions.assertEquals(CalendarType.GREGORIAN, CalendarType.fromCode("klingon"));
		Assertions.assertEquals(CalendarType.GREGORIAN, CalendarType.fromCode(null));
	}

	@Test
	void sunlightPhaseOfUnknownCalendarIsRejected(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.getSunlightPhase("klingon", "1 January 2000",
			45., 10., io.github.mtrevisan.calendars.astronomy.Zenith.OFFICIAL));
	}


	//Metadata
	@Test
	void metadataKeepsTheOrderAndCopiesTheMap(){
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("z", true);
		components.put("a", false);
		components.put("m", true);
		final CalendarMetadata metadata = new CalendarMetadata(components, false);
		components.put("late", true);

		Assertions.assertEquals(List.of("z", "a", "m"), List.copyOf(metadata.nativeComponents().keySet()));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> metadata.nativeComponents().put("x", true));
	}

}
