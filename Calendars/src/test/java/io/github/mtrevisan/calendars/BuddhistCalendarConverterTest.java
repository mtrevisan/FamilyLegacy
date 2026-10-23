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


class BuddhistCalendarConverterTest{

	@Test
	void newYear2567(){
		// Thai solar calendar, BE = CE + 543 (1 Jan 2024 Gregorian)
		final long jdn = CalendarType.BUDDHIST.parseToJdn("1 มกราคม 2567");

		Assertions.assertEquals(2460311L, jdn);
	}

	@Test
	void siameseRevolution(){
		// Siamese revolution (24 Jun 1932 Gregorian)
		final long jdn = CalendarType.BUDDHIST.parseToJdn("24 mithunayon 2475");

		Assertions.assertEquals(2426883L, jdn);
	}

	@Test
	void aprilNewYearUntil1940(){
		// up to 2483 BE the year started on 1 April: January 2482 BE is January 1940
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1940"), CalendarType.BUDDHIST.parseToJdn("1 มกราคม 2482"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 April 1940"), CalendarType.BUDDHIST.parseToJdn("2483"));
		// 2483 BE had no January to March
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BUDDHIST.parseToJdn("1 มกราคม 2483"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1941"), CalendarType.BUDDHIST.parseToJdn("1 มกราคม 2484"));
	}

	@Test
	void thaiDigitsAndEraMarkers(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1941"),
			CalendarType.BUDDHIST.parseToJdn("๑ มกราคม พ.ศ. ๒๔๘๔"));
		// Rattanakosin era: RS 130 started on 1 April 1911
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 April 1911"), CalendarType.BUDDHIST.parseToJdn("ร.ศ. 130"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1912"),
			CalendarType.BUDDHIST.parseToJdn("1 มกราคม ร.ศ. 130"));
	}

}
