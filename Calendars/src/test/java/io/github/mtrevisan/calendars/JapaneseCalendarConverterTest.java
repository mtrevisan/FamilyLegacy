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


class JapaneseCalendarConverterTest{

	@Test
	void lastDayOfMeiji(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("29 July 1912"),
			CalendarType.JAPANESE.parseToJdn("明治45年7月29日"));
	}

	@Test
	void firstYearOfTaisho(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("30 July 1912"),
			CalendarType.JAPANESE.parseToJdn("大正元年7月30日"));
		// a year alone of the first year of an era starts at the accession
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("30 July 1912"), CalendarType.JAPANESE.parseToJdn("Taishō 1"));
	}

	@Test
	void hanNumerals(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("15 August 1945"),
			CalendarType.JAPANESE.parseToJdn("昭和二十年八月十五日"));
	}

	@Test
	void abbreviatedEras(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("7 January 1989"), CalendarType.JAPANESE.parseToJdn("S64.1.7"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("8 January 1989"), CalendarType.JAPANESE.parseToJdn("H1.1.8"));
	}

	@Test
	void romanizedEra(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 2024"), CalendarType.JAPANESE.parseToJdn("Reiwa 6"));
	}

	@Test
	void gregorianAdoption(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1873"),
			CalendarType.JAPANESE.parseToJdn("明治6年1月1日"));
		// the day before is the last day of the lunisolar calendar
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("31 December 1872"),
			CalendarType.JAPANESE.parseToJdn("明治5年12月2日"));
	}

	@Test
	void dateOutsideTheEra(){
		// 7 January 1989 is still Shōwa 64
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("Heisei 1 1 7"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("Meiji 46"));
	}

	@Test
	void missingEra(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("45年7月29日"));
	}

}
