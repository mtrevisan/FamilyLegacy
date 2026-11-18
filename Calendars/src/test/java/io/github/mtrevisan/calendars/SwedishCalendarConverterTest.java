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


class SwedishCalendarConverterTest{

	@Test
	void julianBeforeMarch1700(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("28 February 1700"),
			CalendarType.SWEDISH.parseToJdn("28 February 1700"));
	}

	@Test
	void leapDay1700WasSkipped(){
		// Swedish 1 March 1700 is Julian 29 February 1700
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("29 February 1700"),
			CalendarType.SWEDISH.parseToJdn("1 March 1700"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.SWEDISH.parseToJdn("29 February 1700"));
	}

	@Test
	void oneDayAheadOfJulian(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("14 July 1705"),
			CalendarType.SWEDISH.parseToJdn("15 juli 1705"));
		// the leap day of 1704 was not skipped
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("28 February 1704"),
			CalendarType.SWEDISH.parseToJdn("29 February 1704"));
	}

	@Test
	void february30th1712(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("29 February 1712"),
			CalendarType.SWEDISH.parseToJdn("30 February 1712"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 March 1712"),
			CalendarType.SWEDISH.parseToJdn("1 March 1712"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.SWEDISH.parseToJdn("30 February 1711"));
	}

	@Test
	void gregorianAdoption1753(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("17 February 1753"),
			CalendarType.SWEDISH.parseToJdn("17 February 1753"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 March 1753"),
			CalendarType.SWEDISH.parseToJdn("1 March 1753"));
		Assertions.assertEquals(CalendarType.SWEDISH.parseToJdn("17 February 1753") + 1,
			CalendarType.SWEDISH.parseToJdn("1 March 1753"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.SWEDISH.parseToJdn("20 February 1753"));
	}

}
