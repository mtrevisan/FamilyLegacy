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


class BerberCalendarConverterTest{

	@Test
	void yennayerIsJulianNewYear(){
		// 1 Yennayer 2976 = Julian 1 January 2026 = Gregorian 14 January 2026
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("14 January 2026"), CalendarType.BERBER.parseToJdn("2976"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("14 January 2026"), CalendarType.BERBER.parseToJdn("1 Yennayer 2976"));
		// in the 19th century the Julian calendar was 12 days behind
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("13 January 1850"), CalendarType.BERBER.parseToJdn("1 Yennayer 2800"));
	}

	@Test
	void wikipediaReferenceDate(){
		// en.wikipedia "Berber calendar": Thursday 22 October 2026 = 9 Tubeṛ 2976
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("22 October 2026"), CalendarType.BERBER.parseToJdn("9 Tubeṛ 2976"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("22 October 2026"), CalendarType.BERBER.parseToJdn("9 ktuber 2976"));
	}

	@Test
	void dialectalMonthNames(){
		final long expected = CalendarType.JULIAN.parseToJdn("5 August 2026");

		Assertions.assertEquals(expected, CalendarType.BERBER.parseToJdn("5 Ɣuct 2976"));
		Assertions.assertEquals(expected, CalendarType.BERBER.parseToJdn("5 ghusht 2976"));
		Assertions.assertEquals(expected, CalendarType.BERBER.parseToJdn("5 tamyur 2976"));
	}

	@Test
	void julianLeapDay(){
		// 2100 is a Julian leap year (2100 + 950 = 3050)
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("29 February 2100"), CalendarType.BERBER.parseToJdn("29 Furar 3050"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BERBER.parseToJdn("29 Furar 2977"));
	}

}
