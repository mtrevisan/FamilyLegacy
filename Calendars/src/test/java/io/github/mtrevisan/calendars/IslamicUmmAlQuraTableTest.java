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

import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.islamic.IslamicCalendarAstronomicalEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.chrono.HijrahDate;


/**
 * The month starts of the Umm al-Qura calendar, against the official table published by Saudi Arabia, as embedded in the
 * Java Hijrah chronology (hijrah-config-Hijrah-umalqura). The engine embeds the table (1300-1600 AH) and agrees with it in
 * all the 3612 months; outside that range the astronomical criterion applies.
 */
class IslamicUmmAlQuraTableTest{

	private static long published(final int year, final int month){
		return LocalDate.from(HijrahDate.of(year, month, 1)).toEpochDay() + 2440588L;
	}

	@Test
	void changesOfCriterionStartAtTheirOfficialMonth(){
		//1 Muharram 1420 is 17 April 1999, 1 Muharram 1423 is 15 March 2002
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(1999, 4, 17), IslamicCalendarAstronomicalEngine.monthStartJdn(1420, 1));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2002, 3, 15), IslamicCalendarAstronomicalEngine.monthStartJdn(1423, 1));
		//the month before the change follows the previous criterion
		Assertions.assertEquals(published(1419, 12), IslamicCalendarAstronomicalEngine.monthStartJdn(1419, 12));
		Assertions.assertEquals(published(1422, 12), IslamicCalendarAstronomicalEngine.monthStartJdn(1422, 12));
	}

	@Test
	void everyMonthOfTheTableAgreesWithThePublishedTable(){
		for(int year = 1300; year <= 1600; year ++)
			for(int month = 1; month <= 12; month ++){
				Assertions.assertEquals(published(year, month), IslamicCalendarAstronomicalEngine.monthStartJdn(year, month),
					year + "/" + month);
				Assertions.assertEquals(HijrahDate.of(year, month, 1).lengthOfMonth(),
					IslamicCalendarAstronomicalEngine.monthLength(year, month), "length " + year + "/" + month);
			}
	}

	@Test
	void formerlyDifferentMonthsFollowTheTable(){
		Assertions.assertEquals(published(1427, 6), IslamicCalendarAstronomicalEngine.monthStartJdn(1427, 6));
		Assertions.assertEquals(published(1446, 6), IslamicCalendarAstronomicalEngine.monthStartJdn(1446, 6));
	}

	@Test
	void knownDates(){
		//1 Ramadan 1445 is 11 March 2024, 1 Muharram 1447 is 26 June 2025, 1 Muharram 1300 is 12 November 1882 (Gregorian)
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 3, 11), IslamicCalendarAstronomicalEngine.toJdn(1445, 9, 1));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2025, 6, 26), IslamicCalendarAstronomicalEngine.toJdn(1447, 1, 1));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(1882, 11, 12), IslamicCalendarAstronomicalEngine.toJdn(1300, 1, 1));
	}

	@Test
	void outsideTheTableTheAstronomicalCriterionStillWorks(){
		for(final int year : new int[]{1, 600, 1299, 1601, 1700}){
			for(int month = 1; month <= 12; month ++){
				final int length = IslamicCalendarAstronomicalEngine.monthLength(year, month);
				Assertions.assertTrue(length == 29 || length == 30, year + "/" + month + ": " + length);
			}
			final long yearDays = IslamicCalendarAstronomicalEngine.monthStartJdn(year + 1, 1)
				- IslamicCalendarAstronomicalEngine.monthStartJdn(year, 1);
			Assertions.assertTrue(yearDays == 354 || yearDays == 355, year + ": " + yearDays);
		}
		//contiguous with the table at both ends
		final long before = IslamicCalendarAstronomicalEngine.monthStartJdn(1300, 1) - IslamicCalendarAstronomicalEngine.monthStartJdn(1299, 12);
		Assertions.assertTrue(before == 29 || before == 30, "1299/12: " + before);
		final long after = IslamicCalendarAstronomicalEngine.monthStartJdn(1601, 1) - IslamicCalendarAstronomicalEngine.monthStartJdn(1600, 12);
		Assertions.assertTrue(after == 29 || after == 30, "1600/12: " + after);
	}

	@Test
	void day30NeedsAMonthOf30Days(){
		for(int month = 1; month <= 12; month ++){
			final int length = IslamicCalendarAstronomicalEngine.monthLength(1445, month);
			Assertions.assertTrue(length == 29 || length == 30);
			if(length == 29){
				final int m = month;
				Assertions.assertThrows(IllegalArgumentException.class, () -> IslamicCalendarAstronomicalEngine.toJdn(1445, m, 30));
				Assertions.assertEquals(IslamicCalendarAstronomicalEngine.monthStartJdn(1445, month) + 28,
					IslamicCalendarAstronomicalEngine.toJdn(1445, month, 29));
				return;
			}
			Assertions.assertEquals(IslamicCalendarAstronomicalEngine.monthStartJdn(1445, month) + 29,
				IslamicCalendarAstronomicalEngine.toJdn(1445, month, 30));
		}
		Assertions.fail("No month of 29 days in 1445");
	}

}
