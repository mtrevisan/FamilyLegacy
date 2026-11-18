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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class RegnalCalendarConverterTest{

	@Test
	void georgeII(){
		// the regnal years of George II start on 11 June (accession 11 June 1727)
		final long expected = CalendarType.JULIAN.parseToJdn("5 March 1730");

		Assertions.assertEquals(expected, CalendarType.ENGLISH_REGNAL.parseToJdn("5 March 3 George II"));
		Assertions.assertEquals(expected, CalendarType.ENGLISH_REGNAL.parseToJdn("5 March 3 Geo. II"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("11 June 1729"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("the third year of the reign of George II"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("11 June 1729"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("anno regni regis Georgii secundi tertio"));
	}

	@Test
	void georgeIIAfterTheReformOf1752(){
		// the 26th year ran from 11 June 1752 to 21 June 1753, the 27th began on 22 June 1753 (the Julian 11 June)
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("22 June 1753"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("27 George II"));
		// the 26th year was longer than a year: 15 June occurs twice
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.ENGLISH_REGNAL.parseToJdn("15 June 26 George II"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("15 June 1753"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("15 June 1753, 26 George II"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("15 June 1752"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("15 June 1752, 26 George II"));
	}

	@Test
	void maryAfterJane(){
		// Jane reigned from 6 to 19 July 1553; the first year of Mary starts on 19 July, the second on 6 July 1554
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("19 July 1553"), CalendarType.ENGLISH_REGNAL.parseToJdn("1 Mary"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("10 July 1553"), CalendarType.ENGLISH_REGNAL.parseToJdn("10 July 1 Jane"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.ENGLISH_REGNAL.parseToJdn("10 July 1 Mary"));
	}

	@Test
	void victoria(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("20 June 1837"), CalendarType.ENGLISH_REGNAL.parseToJdn("20 June 1 Victoria"));
		// the last day of the first regnal year
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("19 June 1838"), CalendarType.ENGLISH_REGNAL.parseToJdn("19 June 1 Vict."));
	}

	@Test
	void johnCountsFromAscensionDay(){
		// Easter 1201 (Julian) was 25 March, Ascension 3 May
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("3 May 1201"), CalendarType.ENGLISH_REGNAL.parseToJdn("3 John"));
	}

	@Test
	void philipAndMary(){
		// "1 & 2 Philip and Mary": the second number is Mary's year, counted from 6 July 1553
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 August 1554"),
			CalendarType.ENGLISH_REGNAL.parseToJdn("1 August 1 & 2 Philip and Mary"));
	}

	@Test
	void readeptionOfHenryVI(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("9 October 1470"), CalendarType.ENGLISH_REGNAL.parseToJdn("49 Henry VI"));
	}

	@Test
	void dateOutsideTheReign(){
		// Edward VIII reigned less than a year
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.ENGLISH_REGNAL.parseToJdn("2 Edward VIII"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.ENGLISH_REGNAL.parseToJdn("5 March 3 Napoleon"));
	}

	@Test
	void pontificalYears(){
		// Pius V was crowned on 17 January 1566 (Julian)
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 March 1568"),
			CalendarType.PONTIFICAL.parseToJdn("pontificatus domini Pii papae quinti anno tertio, die 5 martii"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("17 January 1568"),
			CalendarType.PONTIFICAL.parseToJdn("anno III del pontificato di Pio V"));
	}

	@Test
	void interGravissimas(){
		// the bull of the Gregorian reform: 24 February 1582 (Julian), in the tenth year of Gregory XIII (crowned 25 May 1572)
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("24 February 1582"),
			CalendarType.PONTIFICAL.parseToJdn("die 24 februarii, pontificatus Gregorii XIII anno decimo"));
		// a year later the date is Gregorian
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("24 February 1583"),
			CalendarType.PONTIFICAL.parseToJdn("die 24 februarii, pontificatus Gregorii XIII anno undecimo"));
	}

	@Test
	void homonymousPopeAndAntipope(){
		// Clement VII: antipope (1378) and pope (1523)
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.PONTIFICAL.parseToJdn("anno septimo pontificatus Clementis VII"));
		// Clement VII (pope) was crowned on 26 November 1523 (Julian)
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("26 November 1529"),
			CalendarType.PONTIFICAL.parseToJdn("anno Domini 1529, anno septimo pontificatus Clementis VII"));
	}

	@Test
	void modernPopes(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("22 October 1978"),
			CalendarType.PONTIFICAL.parseToJdn("anno I pontificatus Ioannis Pauli II"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("18 May 2025"),
			CalendarType.PONTIFICAL.parseToJdn("anno primo pontificatus Leonis XIV"));
	}

}
