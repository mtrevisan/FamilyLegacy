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


class EthiopianCalendarConverterTest{

	@Test
	void epoch(){
		// Amete Mihret epoch, 276 years before the Coptic one (29 Aug 8 CE Julian, Reingold & Dershowitz, Calendrical Calculations)
		final long jdn = CalendarType.ETHIOPIAN.parseToJdn("1 Meskerem 1");

		Assertions.assertEquals(1724221L, jdn);
	}

	@Test
	void enkutatash2016(){
		// Enkutatash (12 Sep 2023 Gregorian)
		final long jdn = CalendarType.ETHIOPIAN.parseToJdn("1 Meskerem 2016");

		Assertions.assertEquals(2460200L, jdn);
	}

	@Test
	void enkutatash2017(){
		// Enkutatash (11 Sep 2024 Gregorian)
		final long jdn = CalendarType.ETHIOPIAN.parseToJdn("1 Meskerem 2017");

		Assertions.assertEquals(2460565L, jdn);
	}

}
