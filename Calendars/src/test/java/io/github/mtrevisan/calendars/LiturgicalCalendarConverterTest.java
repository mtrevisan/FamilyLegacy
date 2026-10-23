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

import io.github.mtrevisan.calendars.liturgical.Computus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.MonthDay;


class LiturgicalCalendarConverterTest{

	private static void assertSameDay(final String expectedGregorian, final String liturgical){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn(expectedGregorian), CalendarType.LITURGICAL.parseToJdn(liturgical),
			liturgical);
	}

	@Test
	void computus(){
		Assertions.assertEquals(MonthDay.of(4, 23), Computus.gregorianEaster(2000));
		Assertions.assertEquals(MonthDay.of(3, 31), Computus.gregorianEaster(2024));
		// the first Gregorian Easter
		Assertions.assertEquals(MonthDay.of(4, 10), Computus.gregorianEaster(1583));
		// the last Julian Easter in the Catholic countries
		Assertions.assertEquals(MonthDay.of(4, 15), Computus.julianEaster(1582));
		// the Orthodox Easter of 2024 was 5 May (Gregorian), 22 April (Julian)
		Assertions.assertEquals(MonthDay.of(4, 22), Computus.julianEaster(2024));
	}

	@Test
	void movableFeasts(){
		assertSameDay("31 March 2024", "Pasqua 2024");
		assertSameDay("14 February 2024", "Mercoledì delle Ceneri 2024");
		assertSameDay("24 March 2024", "Domenica delle Palme 2024");
		assertSameDay("28 March 2024", "Giovedì Santo 2024");
		assertSameDay("1 April 2024", "Lunedì dell'Angelo 2024");
		assertSameDay("7 April 2024", "Dominica in Albis 2024");
		assertSameDay("9 May 2024", "Ascension Day 2024");
		assertSameDay("19 May 2024", "Pentecoste 2024");
		assertSameDay("26 May 2024", "Trinity Sunday 2024");
		assertSameDay("30 May 2024", "Corpus Domini 2024");
		assertSameDay("10 March 2024", "Laetare 2024");
	}

	@Test
	void advent(){
		assertSameDay("1 December 2024", "Prima domenica di Avvento 2024");
		assertSameDay("3 December 2023", "Advent Sunday 2023");
		assertSameDay("15 December 2024", "Gaudete 2024");
		assertSameDay("15 December 2024", "terza domenica di Avvento 2024");
		assertSameDay("8 December 2024", "dominica secunda Adventus 2024");
	}

	@Test
	void fixedFeasts(){
		assertSameDay("11 November 1700", "festa di San Martino 1700");
		assertSameDay("29 September 1700", "Michaelmas 1700");
		assertSameDay("24 June 1700", "Natività di San Giovanni Battista 1700");
		assertSameDay("26 December 1700", "Santo Stefano 1700");
		assertSameDay("26 December 1700", "in festo Sancti Stephani 1700");
	}

	@Test
	void saintMatthiasInLeapYears(){
		assertSameDay("25 February 2024", "San Mattia 2024");
		assertSameDay("24 February 2023", "San Mattia 2023");
		assertSameDay("24 February 2024", "vigilia di San Mattia 2024");
	}

	@Test
	void weekdayAfterAndBefore(){
		// 11 November 2024 is a Monday
		assertSameDay("14 November 2024", "il giovedì dopo San Martino 2024");
		assertSameDay("18 November 2024", "lunedì dopo San Martino 2024");
		assertSameDay("4 November 2024", "Monday before Martinmas 2024");
		assertSameDay("23 March 2024", "Saturday before Palm Sunday 2024");
		// 29 September 2024 is a Sunday
		assertSameDay("1 October 2024", "feria tertia post festum Sancti Michaelis 2024");
	}

	@Test
	void numberedSundays(){
		assertSameDay("9 June 2024", "terza domenica dopo Pentecoste 2024");
		assertSameDay("9 June 2024", "Dominica tertia post Pentecosten 2024");
		assertSameDay("2 June 2024", "first Sunday after Trinity 2024");
		// 6 January 2024 is a Saturday
		assertSameDay("14 January 2024", "seconda domenica dopo l'Epifania 2024");
		assertSameDay("3 March 2024", "third Sunday in Lent 2024");
		assertSameDay("3 March 2024", "Dominica tertia Quadragesimae 2024");
	}

	@Test
	void vigilMorrowOctave(){
		assertSameDay("14 August 2024", "vigilia dell'Assunta 2024");
		assertSameDay("30 September 2024", "the morrow of Michaelmas 2024");
		assertSameDay("7 April 2024", "in octava Paschae 2024");
		assertSameDay("8 April 2024", "il lunedì dopo l'ottava di Pasqua 2024");
		assertSameDay("28 December 2024", "tre giorni dopo Natale 2024");
	}

	@Test
	void yearFormula(){
		assertSameDay("11 November 1612", "in festo Sancti Martini anno Domini 1612");
	}

	@Test
	void julianBeforeTheReform(){
		// 1500: Julian Easter on 19 April (Julian)
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("19 April 1500"), CalendarType.LITURGICAL.parseToJdn("Pasqua 1500"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("15 April 1582"), CalendarType.LITURGICAL.parseToJdn("Pasqua 1582"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 April 1583"), CalendarType.LITURGICAL.parseToJdn("Pasqua 1583"));
		// Saint Denis (9 October) did not exist in 1582
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.LITURGICAL.parseToJdn("San Dionigi 1582"));
	}

	@Test
	void julianLiturgy(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("5 May 2024"),
			CalendarType.LITURGICAL_JULIAN.parseToJdn("Easter 2024"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("29 September 1700"),
			CalendarType.LITURGICAL_JULIAN.parseToJdn("Michaelmas 1700"));
	}

	@Test
	void unknownFeast(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.LITURGICAL.parseToJdn("San Pancrazio della Luna 1700"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.LITURGICAL.parseToJdn("San Martino"));
	}

}
