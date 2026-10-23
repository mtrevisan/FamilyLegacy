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
package io.github.mtrevisan.familylegacy.io.model.readers.date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class GregorianCalendarConverterTest{

	@Test
	void firstDayOfGregorianReform(){
		// First day of the Gregorian calendar (15 Oct 1582 Gregorian)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 October 1582");

		Assertions.assertEquals(2299161L, jdn);
	}

	@Test
	void j2000Epoch(){
		// J2000.0 epoch day (1 Jan 2000 Gregorian, Meeus, Astronomical Algorithms)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("1 Gennaio 2000");

		Assertions.assertEquals(2451545L, jdn);
	}

	@Test
	void leapDay2024(){
		// Leap day (29 Feb 2024 Gregorian)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("29 February 2024");

		Assertions.assertEquals(2460370L, jdn);
	}

	@Test
	void vernalEquinox2024(){
		// Vernal equinox 2024 (20 Mar 2024 03:06 UT)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("spr 2024");

		Assertions.assertEquals(2460390L, jdn);
	}

	@Test
	void summerSolstice2024(){
		// Summer solstice 2024 (20 Jun 2024 20:51 UT)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("sum 2024");

		Assertions.assertEquals(2460482L, jdn);
	}

	@Test
	void englishDualEpoch(){
		long jdn = CalendarType.GREGORIAN.parseToJdn("12 FEB 1704/5");

		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("12 2 1705"), jdn);


		jdn = CalendarType.GREGORIAN.parseToJdn("12 AUG 1752/53");

		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("12 8 1752"), jdn);
	}


	@Test
	void monthNameIsCaseInsensitive(){
		// 15 Jan 1953 Gregorian
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 GENNAIO 1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void abbreviatedMonthName(){
		// 15 Jan 1953 Gregorian
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 Jan 1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void abbreviatedMonthNameWithDot(){
		// 15 Sep 1953 Gregorian
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 Sept. 1953");

		Assertions.assertEquals(2434636L, jdn);
	}

	@Test
	void frenchMonthNameWithDiacritics(){
		// 15 Aug 1953 Gregorian
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 août 1953");

		Assertions.assertEquals(2434605L, jdn);
	}

	@Test
	void russianGenitiveMonthName(){
		// 15 Jan 1953 Gregorian, Cyrillic script
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 января 1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void greekGenitiveMonthName(){
		// 15 Jan 1953 Gregorian, Greek script
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15 Ιανουαρίου 1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void numericWithSlashes(){
		// 15 Jan 1953 Gregorian, day/month/year
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15/01/1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void numericWithDots(){
		// 15 Jan 1953 Gregorian, day.month.year
		final long jdn = CalendarType.GREGORIAN.parseToJdn("15.01.1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void isoDate(){
		// 15 Jan 1953 Gregorian, ISO 8601
		final long jdn = CalendarType.GREGORIAN.parseToJdn("1953-01-15");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void monthDayYearWithComma(){
		// 15 Jan 1953 Gregorian, US order
		final long jdn = CalendarType.GREGORIAN.parseToJdn("January 15, 1953");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void extraWhitespace(){
		// 15 Jan 1953 Gregorian
		final long jdn = CalendarType.GREGORIAN.parseToJdn("  15   January   1953  ");

		Assertions.assertEquals(2434393L, jdn);
	}

	@Test
	void monthAndYear(){
		// Without the day, the first day of the month (1 Aug 1953 Gregorian)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("August 1953");

		Assertions.assertEquals(2434591L, jdn);
	}

	@Test
	void abbreviatedMonthAndYear(){
		// Without the day, the first day of the month (1 Aug 2026 Gregorian)
		final long jdn = CalendarType.GREGORIAN.parseToJdn("Aug 2026");

		Assertions.assertEquals(2461254L, jdn);
	}

	@Test
	void monthAndYearIsIncomplete(){
		final CalendarType.CalendarMetadata metadata = CalendarType.GREGORIAN.getMetadata("August 1953");

		Assertions.assertTrue(metadata.nativeComponents().get("year"));
		Assertions.assertTrue(metadata.nativeComponents().get("month"));
		Assertions.assertFalse(metadata.nativeComponents().get("day"));
		Assertions.assertFalse(metadata.isComplete());
	}

	@Test
	void fullDateIsComplete(){
		final CalendarType.CalendarMetadata metadata = CalendarType.GREGORIAN.getMetadata("15 January 1953");

		Assertions.assertTrue(metadata.isComplete());
	}

	@Test
	void dayOutOfRange(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("32 January 1953"));
	}

	@Test
	void dayZero(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("0 January 1953"));
	}

	@Test
	void dayBeyondMonthLength(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("31 February 2023"));
	}

	@Test
	void leapDayInCommonYear(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("29 February 2023"));
	}

	@Test
	void numericMonthOutOfRange(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("15 13 1953"));
	}

	@Test
	void unknownMonthName(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("15 Foo 1953"));
	}

	@Test
	void missingYear(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("January"));
	}

	@Test
	void blankDate(){
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.GREGORIAN.parseToJdn("  "));
	}

}
