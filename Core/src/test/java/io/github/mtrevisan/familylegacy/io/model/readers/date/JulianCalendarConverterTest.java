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


class JulianCalendarConverterTest{

	@Test
	void lastDayBeforeGregorianReform(){
		// Last Julian day in Catholic countries (4 Oct 1582 Julian, followed by 15 Oct 1582 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("4 October 1582");

		Assertions.assertEquals(2299160L, jdn);
	}

	@Test
	void newYear2000(){
		// 1 Jan 2000 Julian (14 Jan 2000 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("1 January 2000");

		Assertions.assertEquals(2451558L, jdn);
	}

	@Test
	void fallOfConstantinople(){
		// Fall of Constantinople (29 May 1453 Julian, 7 Jun 1453 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("29 May 1453");

		Assertions.assertEquals(2251915L, jdn);
	}

}
