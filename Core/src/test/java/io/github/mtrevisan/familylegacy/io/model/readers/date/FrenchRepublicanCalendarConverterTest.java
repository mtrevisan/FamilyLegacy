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


class FrenchRepublicanCalendarConverterTest{

	@Test
	void epoch(){
		// 1 Vendémiaire An I (22 Sep 1792 Gregorian)
		final long jdn = CalendarType.FRENCH_REPUBLICAN.parseToJdn("1 Vendémiaire 1");

		Assertions.assertEquals(2375840L, jdn);
	}

	@Test
	void thermidorianReaction(){
		// 9 Thermidor An II (27 Jul 1794 Gregorian)
		final long jdn = CalendarType.FRENCH_REPUBLICAN.parseToJdn("9 Thermidor 2");

		Assertions.assertEquals(2376513L, jdn);
	}

	@Test
	void coupOf18Brumaire(){
		// 18 Brumaire An VIII (9 Nov 1799 Gregorian)
		final long jdn = CalendarType.FRENCH_REPUBLICAN.parseToJdn("18 Brumaire 8");

		Assertions.assertEquals(2378444L, jdn);
	}

	@Test
	void newYear14(){
		// 1 Vendémiaire An XIV, after the sextile years III, VII, XI (23 Sep 1805 Gregorian)
		final long jdn = CalendarType.FRENCH_REPUBLICAN.parseToJdn("1 Vendemiaire 14");

		Assertions.assertEquals(2380588L, jdn);
	}

}
