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


class PisanCalendarConverterTest{

	@Test
	void annunciationStartsTheNextYear(){
		// The Pisan year 1601 starts on 25 March 1600
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("26 March 1600"),
			CalendarType.PISAN.parseToJdn("26 marzo 1601"));
	}

	@Test
	void dayBeforeAnnunciation(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("24 March 1600"),
			CalendarType.PISAN.parseToJdn("24 marzo 1600"));
	}

	@Test
	void januaryMatchesTheModernYear(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 January 1600"),
			CalendarType.PISAN.parseToJdn("10 gennaio 1600"));
	}

	@Test
	void yearAloneStartsOnThePreviousAnnunciation(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 March 1599"), CalendarType.PISAN.parseToJdn("1600"));
	}

	@Test
	void oneYearAheadOfFlorentineAfterAnnunciation(){
		// Pisan and Florentine years differ by one between 25 March and 31 December
		Assertions.assertEquals(CalendarType.FLORENTINE.parseToJdn("8 settembre 1520"),
			CalendarType.PISAN.parseToJdn("8 settembre 1521"));
	}

}
