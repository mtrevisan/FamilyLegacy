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
	void birthOfIsaacNewton(){
		// Isaac Newton was born on 25 Dec 1642 Old Style (4 Jan 1643 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("25 December 1642");

		Assertions.assertEquals(2321157L, jdn);
	}

	@Test
	void birthOfBenjaminFranklinDualDated(){
		// Benjamin Franklin was born on 6 Jan 1705/6 Old Style, with the year starting on 25 March (17 Jan 1706 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("6 January 1705/6");

		Assertions.assertEquals(2344180L, jdn);
	}

	@Test
	void birthOfGeorgeWashingtonDualDated(){
		// George Washington was born on February 11, 1731/2 Old Style (22 Feb 1732 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("February 11, 1731/2");

		Assertions.assertEquals(2353712L, jdn);
	}

	@Test
	void fallOfConstantinople(){
		// Fall of Constantinople (29 May 1453 Julian, 7 Jun 1453 Gregorian)
		final long jdn = CalendarType.JULIAN.parseToJdn("29 May 1453");

		Assertions.assertEquals(2251915L, jdn);
	}

}
