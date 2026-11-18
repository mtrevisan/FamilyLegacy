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


class ByzantineStyleCalendarConverterTest{

	@Test
	void septemberStartsTheNextYear(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 September 1499"),
			CalendarType.BYZANTINE_STYLE.parseToJdn("1 settembre 1500"));
	}

	@Test
	void endOfAugust(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("31 August 1500"),
			CalendarType.BYZANTINE_STYLE.parseToJdn("31 agosto 1500"));
	}

	@Test
	void yearAloneStartsOnFirstSeptember(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 September 1499"),
			CalendarType.BYZANTINE_STYLE.parseToJdn("1500"));
	}

	@Test
	void afterGregorianReform(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("2 November 1699"),
			CalendarType.BYZANTINE_STYLE.parseToJdn("2 novembre 1700"));
	}

	@Test
	void daysSkippedByGregorianReform(){
		// 10 October 1583 of the Byzantine style is 10 October 1582
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BYZANTINE_STYLE.parseToJdn("10 ottobre 1583"));
	}

}
