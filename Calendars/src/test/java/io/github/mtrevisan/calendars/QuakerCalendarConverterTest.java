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


class QuakerCalendarConverterTest{

	@Test
	void eleventhMonthIsJanuaryBefore1752(){
		final long expected = CalendarType.JULIAN.parseToJdn("4 January 1703");

		Assertions.assertEquals(expected, CalendarType.QUAKER.parseToJdn("4th day 11th month 1702/3"));
		Assertions.assertEquals(expected, CalendarType.QUAKER.parseToJdn("4 da 11 mo 1702"));
		Assertions.assertEquals(expected, CalendarType.QUAKER.parseToJdn("the fourth of the eleventh month 1702"));
		Assertions.assertEquals(expected, CalendarType.QUAKER.parseToJdn("11mo 4 1702"));
	}

	@Test
	void firstMonthIsMarchBefore1752(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("25 March 1700"),
			CalendarType.QUAKER.parseToJdn("25th 1st mo. 1700"));
		// before Lady Day the year is the next one in modern reckoning
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("10 March 1701"),
			CalendarType.QUAKER.parseToJdn("10th day 1st month 1700"));
	}

	@Test
	void compoundOrdinal(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("21 May 1690"),
			CalendarType.QUAKER.parseToJdn("twenty-first day of the third month 1690"));
	}

	@Test
	void yearAloneStartsOnLadyDay(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("25 March 1702"), CalendarType.QUAKER.parseToJdn("1702"));
	}

	@Test
	void firstMonthIsJanuaryFrom1752(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("15 March 1760"),
			CalendarType.QUAKER.parseToJdn("15th 3rd month 1760"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1760"), CalendarType.QUAKER.parseToJdn("1760"));
	}

	@Test
	void gregorianReform1752(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("2 September 1752"),
			CalendarType.QUAKER.parseToJdn("2 da 9 mo 1752"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("14 September 1752"),
			CalendarType.QUAKER.parseToJdn("14 da 9 mo 1752"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("5 da 9 mo 1752"));
	}

	@Test
	void monthNameIsNotQuaker(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("4 January 1702"));
	}

	@Test
	void dayBeyondMonthLength(){
		// the 2nd month of 1760 is February
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.QUAKER.parseToJdn("30th 2nd month 1760"));
	}

}
