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


class VenetanCalendarConverterTest{

	@Test
	void firstDayOfYearBeforeGregorianReform(){
		// More veneto year starts on 1 March (1 Mar 1500 Julian)
		final long jdn = CalendarType.VENETAN.parseToJdn("1 marzo 1500");

		Assertions.assertEquals(2268993L, jdn);
	}

	@Test
	void aprilBeforeGregorianReform(){
		// 1 Apr 1500 Julian
		final long jdn = CalendarType.VENETAN.parseToJdn("1 aprile 1500");

		Assertions.assertEquals(2269024L, jdn);
	}

	@Test
	void februaryBeforeGregorianReform(){
		// February belongs to the previous more veneto year (15 Feb 1500 Julian)
		final long jdn = CalendarType.VENETAN.parseToJdn("15 febraro 1499 mv");

		Assertions.assertEquals(2268978L, jdn);
	}

	@Test
	void firstDayOfYearAfterGregorianReform(){
		// 1 Mar 1700 Gregorian
		final long jdn = CalendarType.VENETAN.parseToJdn("1 marzo 1700");

		Assertions.assertEquals(2342032L, jdn);
	}

	@Test
	void februaryAfterGregorianReform(){
		// The Republic of Venice adopted the Gregorian calendar in October 1582 (15 Feb 1701 Gregorian)
		final long jdn = CalendarType.VENETAN.parseToJdn("15 febraro 1700 mv");

		Assertions.assertEquals(2342383L, jdn);
	}

}
