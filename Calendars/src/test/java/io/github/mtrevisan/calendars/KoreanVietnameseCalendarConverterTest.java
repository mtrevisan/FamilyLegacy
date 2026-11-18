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


class KoreanVietnameseCalendarConverterTest{

	@Test
	void seollal(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 February 2024"), CalendarType.KOREAN.parseToJdn("음력 2024년 1월 1일"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("22 January 2023"), CalendarType.KOREAN.parseToJdn("lunar 2023 1 1"));
	}

	@Test
	void seollal1997DiffersFromChineseNewYear(){
		// the new moon fell just after midnight in Korea (UTC+9) and just before it in China (UTC+8)
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("8 February 1997"), CalendarType.KOREAN.parseToJdn("1997년 1월 1일"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("7 February 1997"), CalendarType.CHINESE.parseToJdn("1997年 丁丑年 正月 初一"));
	}

	@Test
	void tet(){
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 February 2024"),
			CalendarType.VIETNAMESE.parseToJdn("ngày 1 tháng Giêng năm 2024"));
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("10 February 2024"), CalendarType.VIETNAMESE.parseToJdn("1/1/2024"));
	}

	@Test
	void tet1985DiffersFromChineseNewYear(){
		// with UTC+7 the leap month fell differently: Tết came a month before the Chinese new year (20 February 1985)
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("21 January 1985"), CalendarType.VIETNAMESE.parseToJdn("mùng 1 tháng Giêng năm 1985"));
	}

	@Test
	void tet1968(){
		// North Vietnam (UTC+7) celebrated on 29 January, the South (UTC+8) on 30 January
		Assertions.assertEquals(CalendarType.GREGORIAN.parseToJdn("29 January 1968"), CalendarType.VIETNAMESE.parseToJdn("tháng Giêng năm 1968"));
	}

}
