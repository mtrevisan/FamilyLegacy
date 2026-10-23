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
 * Sunrise tithis at Ujjain (Prokerala Hindu month grids) and at New Delhi (AstroSage day panchang), 2015-2024.
 */
class VikramSamvatCalendarConverterTest{

	private static void assertAll(final String[][] pairs){
		final List<String> errors = new ArrayList<>();
		for(final String[] pair : pairs){
			final long expected = CalendarType.GREGORIAN.parseToJdn(pair[1]);
			try{
				final long actual = CalendarType.VIKRAM_SAMVAT.parseToJdn(pair[0]);
				if(actual != expected)
					errors.add(pair[0] + ": expected " + pair[1] + ", was " + (actual - expected) + " days off");
			}
			catch(final RuntimeException re){
				errors.add(pair[0] + ": " + re);
			}
		}
		Assertions.assertTrue(errors.isEmpty(), errors.toString());
	}

	@Test
	void newYear(){
		assertAll(new String[][]{
			{"Vikram Samvat 2072", "21 March 2015"}, {"VS 2073", "8 April 2016"}, {"VS 2075", "18 March 2018"},
			{"VS 2076", "6 April 2019"}, {"VS 2077", "25 March 2020"}, {"VS 2079", "2 April 2022"},
			{"VS 2080 Chaitra Shukla Pratipada", "22 March 2023"}, {"VS 2081", "9 April 2024"}, {"VS 2082", "30 March 2025"}
		});
	}

	@Test
	void adhikaShravana2023(){
		assertAll(new String[][]{
			{"VS 2080 Adhika Shravana Shukla 1", "18 July 2023"},
			// Tritiya prevails at two sunrises: the first day
			{"VS 2080 Adhika Shravana Shukla 3", "20 July 2023"},
			{"VS 2080 Adhika Shravana Shukla 13", "31 July 2023"},
			{"VS 2080 Adhika Shravana Purnima", "1 August 2023"},
			{"VS 2080 Adhika Shravana Krishna Panchami", "6 August 2023"},
			{"VS 2080 Adhika Shravana Krishna 7", "7 August 2023"},
			{"VS 2080 Adhika Shravana Krishna 11", "11 August 2023"},
			{"VS 2080 Adhika Shravana Amavasya", "16 August 2023"},
			{"VS 2080 Nija Shravana Shukla 1", "17 August 2023"}
		});
	}

	@Test
	void adhikaAshvina2020(){
		assertAll(new String[][]{
			{"VS 2077 Adhik Ashwin Shukla 1", "18 September 2020"},
			{"VS 2077 Adhik Ashwin Shukla 4", "20 September 2020"},
			{"VS 2077 Adhik Ashwin Krishna 2", "3 October 2020"}
		});
	}

	@Test
	void regularMonths(){
		assertAll(new String[][]{
			{"VS 2072 Chaitra Shukla 4", "23 March 2015"},
			{"VS 2072 Chaitra Shukla Ekadashi", "30 March 2015"},
			// purnimanta Bhadrapada Krishna is the dark fortnight of amanta Shravana
			{"VS 2075 Bhadrapada Krishna Chaturdashi", "8 September 2018"},
			{"VS 2075 amanta Shravana Krishna 14", "8 September 2018"},
			{"VS 2075 Bhadrapada Shukla 14", "23 September 2018"},
			{"VS 2081 Ashvina Shukla 3", "5 October 2024"},
			{"VS 2081 Ashvina Shukla 13", "15 October 2024"},
			{"VS 2081 Kartika Krishna 14", "31 October 2024"},
			{"VS 2081 Kartika Amavasya", "1 November 2024"},
			{"VS 2081 Kartika sudi 1", "2 November 2024"}
		});
	}

	@Test
	void gujaratiNewYear(){
		assertAll(new String[][]{
			{"kartikadi 2072", "12 November 2015"}, {"kartikadi 2073", "31 October 2016"}, {"kartikadi 2074", "20 October 2017"},
			{"kartikadi 2075", "8 November 2018"}, {"kartikadi 2081", "2 November 2024"}, {"kartikadi 2082", "22 October 2025"}
		});
	}

	@Test
	void devanagari(){
		assertAll(new String[][]{
			{"विक्रम संवत् २०८० चैत्र शुक्ल प्रतिपदा", "22 March 2023"}
		});
	}

	@Test
	void invalidDates(){
		// no adhika Kartika in 2081
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> CalendarType.VIKRAM_SAMVAT.parseToJdn("VS 2081 Adhika Kartika Shukla 1"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.VIKRAM_SAMVAT.parseToJdn("VS 2081 Kartika Shukla 16"));
	}

}
