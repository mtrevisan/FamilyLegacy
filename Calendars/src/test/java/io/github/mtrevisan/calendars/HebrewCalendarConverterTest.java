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


class HebrewCalendarConverterTest{

	@Test
	void roshHashanah5784(){
		// Rosh Hashanah (16 Sep 2023 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("1 Tishrei 5784");

		Assertions.assertEquals(2460204L, jdn);
	}

	@Test
	void roshHashanah5785(){
		// Rosh Hashanah (3 Oct 2024 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("1 Tishrei 5785");

		Assertions.assertEquals(2460587L, jdn);
	}

	@Test
	void roshHashanah5786(){
		// Rosh Hashanah (23 Sep 2025 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("1 Tishrei 5786");

		Assertions.assertEquals(2460942L, jdn);
	}

	@Test
	void purimInCommonYear(){
		// Purim, 5785 is a common year (14 Mar 2025 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("14 Adar 5785");

		Assertions.assertEquals(2460749L, jdn);
	}

	@Test
	void passoverInLeapYear(){
		// Passover, 5784 is a leap year (23 Apr 2024 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("15 Nisan 5784");

		Assertions.assertEquals(2460424L, jdn);
	}

	@Test
	void passoverInCommonYear(){
		// Passover, 5785 is a common year (13 Apr 2025 Gregorian)
		final long jdn = CalendarType.HEBREW.parseToJdn("15 Nisan 5785");

		Assertions.assertEquals(2460779L, jdn);
	}

}
