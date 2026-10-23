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

import io.github.mtrevisan.calendars.eastasian.JapaneseLunisolarTable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;


/**
 * Historical events with both a Japanese and a Gregorian date, as given by ja.wikipedia and confirmed by the National
 * Astronomical Observatory of Japan (日本の暦日データベース).
 */
class JapaneseLunisolarCalendarTest{

	private static final String[][] EVENTS = {
		// Akō incident
		{"元禄14年3月14日", "21 April 1701"},
		{"元禄15年12月14日", "30 January 1703"},
		{"元禄16年2月4日", "20 March 1703"},
		// Genroku and Hōei earthquakes, Hōei eruption of Mount Fuji
		{"元禄16年11月23日", "31 December 1703"},
		{"宝永4年10月4日", "28 October 1707"},
		{"宝永4年11月23日", "16 December 1707"},
		{"宝永5年閏1月1日", "22 February 1708"},
		{"宝永5年閏1月27日", "19 March 1708"},
		// Matsuo Bashō
		{"元禄2年3月27日", "16 May 1689"},
		{"元禄7年10月12日", "28 November 1694"},
		// Tokugawa Yoshimune, Motoori Norinaga, Inō Tadataka, Katsushika Hokusai
		{"寛延4年6月20日", "12 July 1751"},
		{"享保15年5月7日", "21 June 1730"},
		{"享和元年9月29日", "5 November 1801"},
		{"延享2年1月11日", "11 February 1745"},
		{"文化15年4月13日", "17 May 1818"},
		{"宝暦10年9月23日", "31 October 1760"},
		{"明和9年2月29日", "1 April 1772"},
		{"天明3年7月8日", "5 August 1783"},
		{"寛政4年4月1日", "21 May 1792"},
		{"寛政6年6月23日", "19 July 1794"},
		{"文化3年3月4日", "22 April 1806"},
		{"文政13年8月4日", "20 September 1830"},
		{"天保8年2月19日", "25 March 1837"},
		// the Tenpō calendar takes effect
		{"天保15年1月1日", "18 February 1844"},
		// a regular 4th month in a year with a leap 4th month
		{"嘉永2年4月18日", "10 May 1849"},
		{"嘉永6年6月3日", "8 July 1853"},
		{"嘉永7年3月3日", "31 March 1854"},
		// dated with the era proclaimed later in the same year
		{"安政元年11月4日", "23 December 1854"},
		{"安政2年10月2日", "11 November 1855"},
		{"安政6年10月27日", "21 November 1859"},
		{"安政7年3月3日", "24 March 1860"},
		{"文久2年8月21日", "14 September 1862"},
		{"文久2年閏8月1日", "24 September 1862"},
		{"元治元年6月5日", "8 July 1864"},
		{"元治元年7月19日", "20 August 1864"},
		{"慶応3年10月14日", "9 November 1867"},
		{"慶応3年11月15日", "10 December 1867"},
		{"慶応3年12月9日", "3 January 1868"},
		{"慶応4年1月3日", "27 January 1868"},
		{"明治元年3月14日", "6 April 1868"},
		{"慶応4年4月11日", "3 May 1868"},
		{"慶応4年閏4月21日", "11 June 1868"},
		{"慶応4年5月15日", "4 July 1868"},
		{"慶応4年9月8日", "23 October 1868"},
		// the last day of the lunisolar calendar
		{"明治5年12月2日", "31 December 1872"}
	};

	@Test
	void historicalEvents(){
		Assertions.assertAll(Arrays.stream(EVENTS)
			.map(event -> () -> Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn(event[1]),
				CalendarType.JAPANESE.parseToJdn(event[0]), event[0])));
	}

	@Test
	void tableIsConsistent(){
		// every year ends the day before the next New Year
		for(int year = JapaneseLunisolarTable.firstYear(); year < JapaneseLunisolarTable.lastYear(); year ++)
			Assertions.assertEquals(JapaneseLunisolarTable.newYearJdn(year + 1),
				JapaneseLunisolarTable.newYearJdn(year) + JapaneseLunisolarTable.yearLength(year), "year " + year);
		// the lunisolar calendar ends on 31 December 1872
		final int last = JapaneseLunisolarTable.lastYear();
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("1 January 1873"),
			JapaneseLunisolarTable.newYearJdn(last) + JapaneseLunisolarTable.yearLength(last));
	}

	@Test
	void hanNumeralsAndNamedDays(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("8 July 1853"), CalendarType.JAPANESE.parseToJdn("嘉永六年六月三日"));
		// 元日 is the first day of the year
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("18 February 1844"), CalendarType.JAPANESE.parseToJdn("天保十五年元日"));
		// 師走 is the 12th month, 朔日 the first day
		Assertions.assertEquals(CalendarType.JAPANESE.parseToJdn("慶応3年12月1日"), CalendarType.JAPANESE.parseToJdn("慶応3年師走朔日"));
		// 晦日 is the last day of the month
		Assertions.assertEquals(CalendarType.JAPANESE.parseToJdn("慶応4年1月1日") - 1, CalendarType.JAPANESE.parseToJdn("慶応3年12月晦日"));
	}

	@Test
	void romanizedEra(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("24 March 1860"), CalendarType.JAPANESE.parseToJdn("Ansei 7 3 3"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("11 June 1868"), CalendarType.JAPANESE.parseToJdn("Keiō 4 uru 4 21"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 March 1837"), CalendarType.JAPANESE.parseToJdn("Tempo 8 2 19"));
	}

	@Test
	void yearAloneIsTheLunarNewYear(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 January 1868"), CalendarType.JAPANESE.parseToJdn("明治元年"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("25 January 1868"), CalendarType.JAPANESE.parseToJdn("慶応4年"));
	}

	@Test
	void invalidDates(){
		// the Senmyō calendar (before 1685)
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("貞享元年10月21日"));
		// 嘉永2年 has a leap 4th month, but no leap 5th month
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("嘉永2年閏5月1日"));
		// the 12th month of 明治5年 has only two days
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("明治5年12月3日"));
		// a short (29-day) month has no 30th day: 1685 month 1 is short
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("貞享2年1月30日"));
		// 慶応 lasted until its 4th year
		Assertions.assertThrows(IllegalArgumentException.class, () -> CalendarType.JAPANESE.parseToJdn("慶応5年1月1日"));
	}

}
