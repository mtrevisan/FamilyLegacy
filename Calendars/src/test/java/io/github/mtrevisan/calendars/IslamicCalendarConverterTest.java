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


class IslamicCalendarConverterTest{

	@Test
	void ramadan1445(){
		// 1 Ramadan 1445 AH, Umm al-Qura (11 Mar 2024 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("1 ramadan 1445");

		Assertions.assertEquals(2460381L, jdn);
	}

	@Test
	void ramadan1445Arabic(){
		// 1 Ramadan 1445 AH, Umm al-Qura, Arabic script and digits (11 Mar 2024 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("١ رمضان ١٤٤٥");

		Assertions.assertEquals(2460381L, jdn);
	}

	@Test
	void eidAlFitr1445(){
		// Eid al-Fitr, 1 Shawwal 1445 AH, Umm al-Qura (10 Apr 2024 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("1 shawwal 1445");

		Assertions.assertEquals(2460411L, jdn);
	}

	@Test
	void newYear1446(){
		// 1 Muharram 1446 AH, Umm al-Qura (7 Jul 2024 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("1 muharram 1446");

		Assertions.assertEquals(2460499L, jdn);
	}

	@Test
	void newYear1423(){
		// 1 Muharram 1423 AH, Umm al-Qura (15 Mar 2002 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("1 muharram 1423");

		Assertions.assertEquals(2452349L, jdn);
	}

	@Test
	void newYear1420(){
		// 1 Muharram 1420 AH, Umm al-Qura (17 Apr 1999 Gregorian)
		final long jdn = CalendarType.ISLAMIC.parseToJdn("1 muharram 1420");

		Assertions.assertEquals(2451286L, jdn);
	}

}
