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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;


/**
 * Reference dates: Drik Panchang, Prokerala, AstroSage, Hamro Patro, West Bengal government holiday lists, Bangla
 * Academy (2015-2026).
 */
class IndianSolarCalendarsTest{

	private static void assertAll(final CalendarType type, final String[][] pairs){
		final List<String> errors = new ArrayList<>();
		for(final String[] pair : pairs){
			final long expected = CalendarType.GREGORIAN.parseToJdn(pair[1]);
			try{
				final long actual = type.parseToJdn(pair[0]);
				if(actual != expected)
					errors.add(pair[0] + ": expected " + pair[1] + ", was " + (actual - expected) + " days off");
			}
			catch(final RuntimeException re){
				errors.add(pair[0] + ": " + re.getMessage());
			}
		}
		Assertions.assertTrue(errors.isEmpty(), errors.toString());
	}

	@Test
	void westBengalPohelaBoishakh(){
		assertAll(CalendarType.BENGALI, new String[][]{
			{"1 Baishakh 1422", "15 April 2015"}, {"1 Baishakh 1423", "14 April 2016"}, {"1 Baishakh 1424", "15 April 2017"},
			{"1 Baishakh 1425", "15 April 2018"}, {"1 Baishakh 1426", "15 April 2019"}, {"1 Baishakh 1427", "14 April 2020"},
			{"1 Baishakh 1428", "15 April 2021"}, {"1 Baishakh 1429", "15 April 2022"}, {"1 Baishakh 1430", "15 April 2023"},
			{"১ বৈশাখ ১৪৩১", "14 April 2024"}, {"1 Baishakh 1432", "15 April 2025"}, {"1 Baishakh 1433", "15 April 2026"}
		});
	}

	@Test
	void tamilPuthandu(){
		assertAll(CalendarType.TAMIL, new String[][]{
			{"1 Chithirai 1937", "14 April 2015"}, {"1 Chithirai 1938", "14 April 2016"}, {"1 Chithirai 1939", "14 April 2017"},
			{"1 Chithirai 1940", "14 April 2018"}, {"1 Chithirai 1941", "14 April 2019"}, {"1 Chithirai 1942", "14 April 2020"},
			{"1 Chithirai 1943", "14 April 2021"}, {"1 Chithirai 1944", "14 April 2022"}, {"1 Chithirai 1945", "14 April 2023"},
			{"1 சித்திரை 1946", "14 April 2024"}, {"1 Chithirai 1947", "14 April 2025"}, {"1 Chithirai 1948", "14 April 2026"}
		});
	}

	@Test
	void malayalamNewYear(){
		// 1 Chingam 1199 (2023) is 17 August per Prokerala, 18 August per Drik Panchang: left out
		assertAll(CalendarType.MALAYALAM, new String[][]{
			{"1 Chingam 1191", "17 August 2015"}, {"1 Chingam 1192", "17 August 2016"}, {"1 Chingam 1193", "17 August 2017"},
			{"1 Chingam 1194", "17 August 2018"}, {"1 Chingam 1195", "17 August 2019"}, {"1 Chingam 1196", "17 August 2020"},
			{"1 Chingam 1197", "17 August 2021"}, {"1 Chingam 1198", "17 August 2022"}, {"1 ചിങ്ങം 1200", "17 August 2024"},
			{"1 Chingam 1201", "17 August 2025"}, {"1 Chingam 1202", "17 August 2026"}
		});
	}

	@Test
	void nepaliNewYear(){
		assertAll(CalendarType.NEPALI, new String[][]{
			{"1 Baisakh 2072", "14 April 2015"}, {"1 Baisakh 2073", "13 April 2016"}, {"1 Baisakh 2074", "14 April 2017"},
			{"1 Baisakh 2075", "14 April 2018"}, {"1 Baisakh 2076", "14 April 2019"}, {"1 Baisakh 2077", "13 April 2020"},
			{"1 Baisakh 2078", "14 April 2021"}, {"1 Baisakh 2079", "14 April 2022"}, {"1 Baisakh 2080", "14 April 2023"},
			{"2081/01/01", "13 April 2024"}, {"२०८२ बैशाख १", "14 April 2025"}, {"1 Baisakh 2083 BS", "14 April 2026"},
			// medic/bikram-sambat test vectors
			{"1 Baisakh 2007", "13 April 1950"}, {"1 Baisakh 2008", "14 April 1951"}, {"1 Baisakh 2020", "14 April 1963"},
			{"1 Baisakh 2045", "13 April 1988"}
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.NEPALI.parseToJdn("32 Baisakh 2080"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.NEPALI.parseToJdn("1 Baisakh 1950"));
	}

	@Test
	void bangladeshRevisedCalendar(){
		assertAll(CalendarType.BENGALI_BANGLADESH, new String[][]{
			{"1 Boishakh 1431", "14 April 2024"},
			// the fixed anchors of the 2019 revision
			{"8 Falgun 1431", "21 February 2025"}, {"12 Choitro 1431", "26 March 2025"}, {"1 Poush 1431", "16 December 2024"},
			{"1 Kartik 1426", "17 October 2019"}, {"1 Magh 1431", "15 January 2025"}, {"1 Choitro 1430", "15 March 2024"},
			// the 1987 rule: Ashshin had 30 days
			{"1 Kartik 1425", "16 October 2018"}
		});
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.BENGALI_BANGLADESH.parseToJdn("30 Falgun 1431"));
	}

}
