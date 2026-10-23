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

import io.github.mtrevisan.calendars.bahai.BahaiCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Reference dates from the Bahá'í World Centre ("Badí' dates 172 to 221 BE", and https://www.bahai.org/action/devotional-life/calendar).
 */
class BahaiCalendarConverterTest{

	@Test
	void westernPracticeBefore172(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("21 March 1844"), CalendarType.BAHAI.parseToJdn("1 BE"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("21 March 2014"), CalendarType.BAHAI.parseToJdn("1 Bahá 171"));
		// Ayyám-i-Há from 26 February to 1 March: five days in a Gregorian leap year
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("26 February 2012"), CalendarType.BAHAI.parseToJdn("1 Ayyám-i-Há 168"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 March 2012"), CalendarType.BAHAI.parseToJdn("5 Ayyám-i-Há 168"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("2 March 2012"), CalendarType.BAHAI.parseToJdn("1 ʻAláʼ 168"));
	}

	@Test
	void officialNawRuz(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("21 March 2015"), CalendarType.BAHAI.parseToJdn("172 B.E."));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("20 March 2016"), CalendarType.BAHAI.parseToJdn("1 Bahá 173"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("21 March 2026"), CalendarType.BAHAI.parseToJdn("1 Bahá 183"));
	}

	@Test
	void monthsOf183(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("9 April 2026"), CalendarType.BAHAI.parseToJdn("1 Jalál 183"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("17 May 2026"), CalendarType.BAHAI.parseToJdn("1 ‘Aẓamat 183"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("27 September 2026"), CalendarType.BAHAI.parseToJdn("1 Mashíyyat 183"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("31 December 2026"), CalendarType.BAHAI.parseToJdn("1 Sharaf 183"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("7 February 2027"), CalendarType.BAHAI.parseToJdn("1 Mulk 183"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("2 March 2027"), CalendarType.BAHAI.parseToJdn("1 ‘Alá’ 183"));
	}

	@Test
	void ayyamIHaLength(){
		// 182 BE: Ayyám-i-Há from 25 February to 1 March 2026 (5 days)
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 February 2026"), CalendarType.BAHAI.parseToJdn("1 Ayyám-i-Há 182"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 March 2026"), CalendarType.BAHAI.parseToJdn("5 Ayyám-i-Há 182"));
		// 183 BE: 4 days
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BAHAI.parseToJdn("5 Ayyám-i-Há 183"));
	}

	@Test
	void astronomicalRuleReproducesTheOfficialTable(){
		final List<String> differences = new ArrayList<>();
		for(int year = 172; year <= 221; year ++){
			final long official = BahaiCalendarStrategy.nawRuzJdn(year);
			final long computed = BahaiCalendarStrategy.computeNawRuzJdn(year);
			if(official != computed)
				differences.add(year + " BE: official " + official + ", computed " + computed);
		}

		Assertions.assertTrue(differences.isEmpty(), differences.toString());
	}

	@Test
	void invalidDates(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BAHAI.parseToJdn("20 Bahá 183"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BAHAI.parseToJdn("1 Ramadan 183"));
	}

}
