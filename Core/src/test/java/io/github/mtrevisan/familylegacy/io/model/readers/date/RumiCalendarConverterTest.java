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


class RumiCalendarConverterTest{

	@Test
	void firstDayOfYear1300(){
		// CE = Rumi + 584, year starting on 1 March (1 Mar 1884 Julian)
		final long jdn = CalendarType.RUMI.parseToJdn("1 mart 1300");

		Assertions.assertEquals(2409249L, jdn);
	}

	@Test
	void thirtyFirstMarchIncident(){
		// 31 March Incident (13 Apr 1909 Gregorian)
		final long jdn = CalendarType.RUMI.parseToJdn("31 mart 1325");

		Assertions.assertEquals(2418410L, jdn);
	}

	@Test
	void lastJulianDay(){
		// Last Julian day before the 1917 reform (15 Feb 1917 Julian, 28 Feb 1917 Gregorian)
		final long jdn = CalendarType.RUMI.parseToJdn("15 subat 1332");

		Assertions.assertEquals(2421288L, jdn);
	}

	@Test
	void firstGregorianDay(){
		// 1917 reform, 15 Şubat 1332 was followed by 1 Mart 1333 (1 Mar 1917 Gregorian)
		final long jdn = CalendarType.RUMI.parseToJdn("1 mart 1333");

		Assertions.assertEquals(2421289L, jdn);
	}

	@Test
	void januaryStartedYear1334(){
		// From 1334 the year starts in January (1 Jan 1918 Gregorian)
		final long jdn = CalendarType.RUMI.parseToJdn("1 kanun-i sani 1334");

		Assertions.assertEquals(2421595L, jdn);
	}

}
