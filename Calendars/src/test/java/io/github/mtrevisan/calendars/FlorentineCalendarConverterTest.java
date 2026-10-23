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


class FlorentineCalendarConverterTest{

	@Test
	void januaryBelongsToThePreviousYear(){
		// The Florentine year 1600 starts on 25 March 1600, so January 1600 is January 1601 in modern reckoning
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 January 1601"),
			CalendarType.FLORENTINE.parseToJdn("10 gennaio 1600"));
	}

	@Test
	void dayBeforeAnnunciation(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("24 March 1601"),
			CalendarType.FLORENTINE.parseToJdn("24 marzo 1600"));
	}

	@Test
	void annunciationStartsTheYear(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 March 1600"),
			CalendarType.FLORENTINE.parseToJdn("25 marzo 1600"));
	}

	@Test
	void yearAloneStartsOnAnnunciation(){
		// Before the Gregorian reform the days are Julian
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("25 March 1450"), CalendarType.FLORENTINE.parseToJdn("1450"));
	}

	@Test
	void dualYearIsNotShifted(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("10 January 1500"),
			CalendarType.FLORENTINE.parseToJdn("10 gennaio 1499/1500"));
	}

	@Test
	void latinGenitiveMonth(){
		Assertions.assertEquals(CalendarType.JULIAN.parseToJdn("15 February 1500"),
			CalendarType.FLORENTINE.parseToJdn("15 Februarii 1499"));
	}

	@Test
	void leapDayFollowsTheModernYear(){
		// 29 February 1599 Florentine is 29 February 1600 Gregorian (leap year)
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("29 February 1600"),
			CalendarType.FLORENTINE.parseToJdn("29 febbraio 1599"));
		// 29 February 1699 Florentine would be 29 February 1700 Gregorian (common year)
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.FLORENTINE.parseToJdn("29 febbraio 1699"));
	}

	@Test
	void gregorianReform(){
		Assertions.assertEquals(CalendarType.FLORENTINE.parseToJdn("4 ottobre 1582") + 1,
			CalendarType.FLORENTINE.parseToJdn("15 ottobre 1582"));
	}

	@Test
	void daysSkippedByGregorianReform(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.FLORENTINE.parseToJdn("10 ottobre 1582"));
	}

}
