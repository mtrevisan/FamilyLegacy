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


class ReformedJulianCalendarConverterTest{

	@Test
	void agreesWithGregorianIn2000(){
		// The Revised Julian calendar agrees with the Gregorian one from 1 Mar 1600 to 28 Feb 2800 (1 Jan 2000 Gregorian)
		final long jdn = CalendarType.REFORMED_JULIAN.parseToJdn("1 January 2000");

		Assertions.assertEquals(2451545L, jdn);
	}

	@Test
	void lastCommonDayWithGregorian(){
		// Last day of agreement with the Gregorian calendar (28 Feb 2800 Gregorian)
		final long jdn = CalendarType.REFORMED_JULIAN.parseToJdn("28 February 2800");

		Assertions.assertEquals(2743797L, jdn);
	}

	@Test
	void firstDivergenceFromGregorian(){
		// 2800 is not a Revised Julian leap year (2800 mod 900 = 100) but it is a Gregorian one
		// (1 Mar 2800 Revised Julian = 29 Feb 2800 Gregorian)
		final long jdn = CalendarType.REFORMED_JULIAN.parseToJdn("1 March 2800");

		Assertions.assertEquals(2743798L, jdn);
	}

}
