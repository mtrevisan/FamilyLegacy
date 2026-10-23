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


/**
 * Julian Easter: 18 April 1400, 3 April 1401, 26 March 1402, 15 April 1403.
 */
class EasterStyleCalendarConverterTest{

	@Test
	void yearAloneStartsOnEaster(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("18 April 1400"), CalendarType.EASTER_STYLE.parseToJdn("1400"));
	}

	@Test
	void afterEasterOfTheYear(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("20 April 1400"),
			CalendarType.EASTER_STYLE.parseToJdn("20 avril 1400"));
	}

	@Test
	void januaryBelongsToTheNextCommonYear(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("10 January 1401"),
			CalendarType.EASTER_STYLE.parseToJdn("10 janvier 1400"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("1 April 1401"),
			CalendarType.EASTER_STYLE.parseToJdn("1 avril 1400"));
	}

	@Test
	void dayMissingFromTheYear(){
		// the year 1400 runs from 18 April 1400 to 2 April 1401
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.EASTER_STYLE.parseToJdn("10 avril 1400"));
	}

	@Test
	void dayOccurringTwice(){
		// the year 1402 runs from 26 March 1402 to 14 April 1403
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.EASTER_STYLE.parseToJdn("5 avril 1402"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 April 1402"),
			CalendarType.EASTER_STYLE.parseToJdn("5 avril 1402 après Pâques"));
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("5 April 1403"),
			CalendarType.EASTER_STYLE.parseToJdn("5 avril 1402 avant Pâques"));
	}

	@Test
	void contradictoryQualifier(){
		// 20 April 1400 is only after Easter
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.EASTER_STYLE.parseToJdn("20 avril 1400 avant Pâques"));
	}

}
