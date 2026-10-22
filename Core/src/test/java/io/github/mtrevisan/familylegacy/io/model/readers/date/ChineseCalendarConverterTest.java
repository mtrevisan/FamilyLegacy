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


class ChineseCalendarConverterTest{

	@Test
	void newYear1964(){
		// Chinese New Year 1964 (13 Feb 1964 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("1964年 农历甲辰年正月初一");

		Assertions.assertEquals(2438439L, jdn);
	}

	@Test
	void newYear2024(){
		// Chinese New Year 2024 (10 Feb 2024 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("2024年 甲辰年 正月 初一");

		Assertions.assertEquals(2460351L, jdn);
	}

	@Test
	void midAutumnFestival2024(){
		// Mid-Autumn Festival 2024 (15th Day of 8th Month: 17 Sep 2024 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("2024年 甲辰年 八月 十五");

		Assertions.assertEquals(2460571L, jdn);
	}

	@Test
	void dragonBoatFestival2023(){
		// Dragon Boat Festival 2023 (5th Day of 5th Month: 22 Jun 2023 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("2023年 癸卯年 五月 初五");

		Assertions.assertEquals(2460118L, jdn);
	}

	@Test
	void prcProclamation(){
final long start = System.currentTimeMillis();
		// Proclamation of the PRC (2 Oct 1949 Gregorian -> 11th Day of 8th Month)
		final long jdn = CalendarType.CHINESE.parseToJdn("民国38年 八月 十一");
// 50121 ms
//  5797 ms
System.out.println("PRC proclamation: " + (System.currentTimeMillis() - start) + " ms");

		Assertions.assertEquals(2433192L, jdn);
	}

	@Test
	void newYear1900(){
		// Chinese New Year 1900 (31 Jan 1900 Gregorian)
		// Tests pre-1928 Beijing Local Mean Time offset (+7:45:40)
		final long jdn = CalendarType.CHINESE.parseToJdn("光绪二十六年 庚子年 正月 初一");

		Assertions.assertEquals(2415051L, jdn);
	}

	@Test
	void leapMonthYear(){
		// Leap Month Year Test: Chinese New Year 2020 (25 Jan 2020 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("2020年 庚子年 正月 初一");

		Assertions.assertEquals(2458874L, jdn);
	}

	@Test
	void lastDayOf12MonthBeforeNewYear2025(){
		// Last Day of 12th Month before Chinese New Year 2025 (28 Jan 2025 Gregorian)
		final long jdn = CalendarType.CHINESE.parseToJdn("2024年 甲辰年 腊月 三十");

		Assertions.assertEquals(2460705L, jdn);
	}

}
