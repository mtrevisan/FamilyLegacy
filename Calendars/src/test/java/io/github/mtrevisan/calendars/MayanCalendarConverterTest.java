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


class MayanCalendarConverterTest{

	@Test
	void baseDate(){
		// Long Count base date, GMT correlation 584283 (11 Aug 3114 BCE proleptic Gregorian)
		final long jdn = CalendarType.MAYAN.parseToJdn("0.0.0.0.0");

		Assertions.assertEquals(584283L, jdn);
	}

	@Test
	void endOfThirteenthBaktun(){
		// 13.0.0.0.0 4 Ajaw 3 K'ank'in, GMT correlation 584283 (21 Dec 2012 Gregorian)
		final long jdn = CalendarType.MAYAN.parseToJdn("13.0.0.0.0");

		Assertions.assertEquals(2456283L, jdn);
	}

	@Test
	void dayBeforeEndOfThirteenthBaktun(){
		// 20 Dec 2012 Gregorian
		final long jdn = CalendarType.MAYAN.parseToJdn("12.19.19.17.19");

		Assertions.assertEquals(2456282L, jdn);
	}

	@Test
	void summerSolstice2024(){
		// Yax station, summer solstice 2024 (20 Jun 2024 20:51 UT)
		final long jdn = CalendarType.MAYAN.parseToJdn("Yax 2024");

		Assertions.assertEquals(2460482L, jdn);
	}

}
