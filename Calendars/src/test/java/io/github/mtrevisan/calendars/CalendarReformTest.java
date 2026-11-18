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

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * The dates of the reforms are the ones published in "Adoption of the Gregorian calendar" (Wikipedia, citing the
 * national sources): the day after the last Julian day is the first Gregorian day, so the two have consecutive Julian Day
 * Numbers, and the gap between the two calendars is the one of the century (10, 11, 12, or 13 days).
 */
class CalendarReformTest{

	private static void assertSwitch(final CalendarReform reform, final int gap){
		final long lastJulian = JulianCalendarStrategy.toJdn(reform.lastJulianYear(), reform.lastJulianMonth(),
			reform.lastJulianDay());
		final long firstGregorian = GregorianCalendarStrategy.toJdn(reform.firstGregorianYear(), reform.firstGregorianMonth(),
			reform.firstGregorianDay());

		// the first Gregorian day follows the last Julian one...
		Assertions.assertEquals(lastJulian + 1, firstGregorian);
		// ...and the reform removed exactly the difference between the two calendars
		final long julianOfFirstGregorian = JulianCalendarStrategy.toJdn(reform.firstGregorianYear(),
			reform.firstGregorianMonth(), reform.firstGregorianDay());
		Assertions.assertEquals(gap, julianOfFirstGregorian - firstGregorian);

		Assertions.assertEquals(lastJulian, reform.toJdn(reform.lastJulianYear(), reform.lastJulianMonth(), reform.lastJulianDay()));
		Assertions.assertEquals(firstGregorian,
			reform.toJdn(reform.firstGregorianYear(), reform.firstGregorianMonth(), reform.firstGregorianDay()));
	}


	@Test
	void sixteenthCentury(){
		assertSwitch(CalendarReform.ROME, 10);
		assertSwitch(CalendarReform.FRANCE, 10);
	}

	@Test
	void britain(){
		assertSwitch(CalendarReform.BRITAIN, 11);
	}

	@Test
	void protestantStatesOf1700(){
		// 18 February 1700 followed by 1 March 1700 (Protestant Germany, Denmark-Norway)
		assertSwitch(CalendarReform.GERMANY_PROTESTANT, 11);
		Assertions.assertSame(CalendarReform.GERMANY_PROTESTANT, CalendarReform.DENMARK);
		// the Dutch provinces: 12 July (Gelderland), 12 December (Overijssel, Utrecht), 12 January 1701 (Friesland, Groningen)
		assertSwitch(CalendarReform.NETHERLANDS_GELDERLAND, 11);
		assertSwitch(CalendarReform.NETHERLANDS_UTRECHT, 11);
		assertSwitch(CalendarReform.NETHERLANDS_FRISIA, 11);
	}

	@Test
	void twentiethCentury(){
		assertSwitch(CalendarReform.BULGARIA, 13);
		assertSwitch(CalendarReform.RUSSIA, 13);
		assertSwitch(CalendarReform.ROMANIA, 13);
		assertSwitch(CalendarReform.GREECE, 13);
	}

	@Test
	void sweden1753(){
		assertSwitch(CalendarReform.SWEDEN, 11);
	}

	@Test
	void publishedDates(){
		// the decree of 24 January 1918: 1 February 1918 became 14 February 1918 (Russia)
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(1918, 2, 14), CalendarReform.RUSSIA.toJdn(1918, 2, 14));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(1918, 2, 14) - 1, CalendarReform.RUSSIA.toJdn(1918, 1, 31));
		// the Greek 1 March 1923 follows 15 February
		Assertions.assertEquals(CalendarReform.GREECE.toJdn(1923, 2, 15) + 1, CalendarReform.GREECE.toJdn(1923, 3, 1));
		// the Julian leap day of 1700 still existed in the Julian calendar, and Germany skipped it
		Assertions.assertEquals(JulianCalendarStrategy.toJdn(1700, 2, 18) + 1, CalendarReform.GERMANY_PROTESTANT.toJdn(1700, 3, 1));
	}

	@Test
	void skippedDays(){
		Assertions.assertTrue(CalendarReform.RUSSIA.isSkipped(1918, 2, 1));
		Assertions.assertTrue(CalendarReform.RUSSIA.isSkipped(1918, 2, 13));
		Assertions.assertFalse(CalendarReform.RUSSIA.isSkipped(1918, 1, 31));
		Assertions.assertFalse(CalendarReform.RUSSIA.isSkipped(1918, 2, 14));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarReform.RUSSIA.toJdn(1918, 2, 5));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarReform.GREECE.toJdn(1923, 2, 20));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarReform.GERMANY_PROTESTANT.toJdn(1700, 2, 25));
	}

	@Test
	void leapYearsAndMonthLengths(){
		Assertions.assertTrue(JulianCalendarStrategy.isLeapYear(1900));
		Assertions.assertFalse(GregorianCalendarStrategy.isLeapYear(1900));
		Assertions.assertTrue(JulianCalendarStrategy.isLeapYear(-4));
		Assertions.assertEquals(29, GregorianCalendarStrategy.monthLength(2, true));
		Assertions.assertEquals(28, GregorianCalendarStrategy.monthLength(2, false));
		Assertions.assertEquals(30, GregorianCalendarStrategy.monthLength(11, true));
		Assertions.assertEquals(29, CalendarReform.RUSSIA.monthLength(1920, 2));
		Assertions.assertEquals(28, CalendarReform.RUSSIA.monthLength(1919, 2));
	}

}
