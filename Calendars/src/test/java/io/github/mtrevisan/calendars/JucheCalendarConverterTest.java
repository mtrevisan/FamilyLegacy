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


class JucheCalendarConverterTest{

	@Test
	void yearOneIs1912(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1912"), CalendarType.JUCHE.parseToJdn("주체1년"));
	}

	@Test
	void gregorianYearInParentheses(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("15 April 2023"),
			CalendarType.JUCHE.parseToJdn("주체112(2023)년 4월 15일"));
	}

	@Test
	void lastDayOfMonth(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("29 February 2024"), CalendarType.JUCHE.parseToJdn("Juche 113 2 晦日"));
	}

}
