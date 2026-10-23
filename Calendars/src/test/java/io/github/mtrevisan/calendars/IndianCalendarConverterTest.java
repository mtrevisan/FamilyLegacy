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


class IndianCalendarConverterTest{

	@Test
	void newYear1945(){
		// Indian national calendar, Chaitra 1 falls on 22 March in common years (22 Mar 2023 Gregorian)
		final long jdn = CalendarType.INDIAN.parseToJdn("1 Chaitra 1945");

		Assertions.assertEquals(2460026L, jdn);
	}

	@Test
	void newYear1946(){
		// Indian national calendar, Chaitra 1 falls on 21 March in leap years (21 Mar 2024 Gregorian)
		final long jdn = CalendarType.INDIAN.parseToJdn("1 Chaitra 1946");

		Assertions.assertEquals(2460391L, jdn);
	}

	@Test
	void pausa1946(){
		// Indian national calendar, Pausa 1 falls on 22 December (22 Dec 2024 Gregorian)
		final long jdn = CalendarType.INDIAN.parseToJdn("1 Pausa 1946");

		Assertions.assertEquals(2460667L, jdn);
	}

}
