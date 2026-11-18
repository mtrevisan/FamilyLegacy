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

import io.github.mtrevisan.calendars.chinese.ChineseCalendarParser;
import io.github.mtrevisan.calendars.chinese.ChineseDateInput;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ChineseCalendarFixesTest{

	private static long parse(final String date){
		return CalendarType.CHINESE.parseToJdn(date);
	}


	/**
	 * Gregorian-Lunar Calendar Conversion Tables of the Hong Kong Observatory (https://www.hko.gov.hk/en/gts/time/calendar/):
	 * 2033/11/22 11th month, 2033/12/22 11th month (the leap one), 2034/1/20 12th month, 2034/2/19 1st month of 2034.
	 */
	@Test
	void leapEleventhMonthOf2033(){
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2033, 11, 22), parse("2033年 十一月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2033, 12, 22), parse("2033年 閏十一月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2034, 1, 20), parse("2033年 十二月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2034, 2, 19), parse("2034年 正月 初一"));
		//the leap month has 29 days (22 December to 19 January), the 11th 30 (22 November to 21 December)
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2034, 1, 19), parse("2033年 閏十一月 廿九"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> parse("2033年 閏十一月 三十"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2033, 12, 21), parse("2033年 十一月 三十"));
		//the months of the year before the solstice are not touched
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2033, 1, 31), parse("2033年 正月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2033, 10, 23), parse("2033年 十月 初一"));
		//a leap month that does not exist
		Assertions.assertThrows(IllegalArgumentException.class, () -> parse("2033年 閏十二月 初一"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> parse("2033年 閏三月 初一"));
	}

	/** Hong Kong Observatory: 2020 has a leap 4th month (2020/5/23), 2023 a leap 2nd month (2023/3/22). */
	@Test
	void leapMonthsOfTheFollowingCycleDoNotDisturbTheOtherYears(){
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2020, 5, 23), parse("2020年 閏四月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 3, 22), parse("2023年 閏二月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 12, 13), parse("2023年 十一月 初一"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 1, 11), parse("2023年 十二月 初一"));
	}

	@Test
	void tenthDayAndSingleCharacterDays(){
		//the 8th month of 2024 starts on 3 September
		final long start = GregorianCalendarStrategy.toJdn(2024, 9, 3);
		Assertions.assertEquals(start + 9, parse("2024年 八月 初十"));
		Assertions.assertEquals(start + 9, parse("2024年 八月 十"));
		Assertions.assertEquals(start, parse("2024年 八月 初一"));
		Assertions.assertEquals(start + 19, parse("2024年 八月 廿"));
		Assertions.assertEquals(start + 14, parse("2024年 八月 十五"));
		Assertions.assertEquals(start + 28, parse("2024年 八月 廿九"));
		Assertions.assertEquals(10, ChineseCalendarParser.parse("2024年 八月 初十").day());
		Assertions.assertEquals(10, ChineseCalendarParser.parse("2024年 八月 十").day());
		Assertions.assertEquals(20, ChineseCalendarParser.parse("2024年 八月 廿").day());
		Assertions.assertEquals(30, ChineseCalendarParser.parse("2024年 八月 卅").day());
		Assertions.assertEquals(5, ChineseCalendarParser.parse("2024年 八月 五").day());
		//no StringIndexOutOfBounds on short tokens
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("2024年 八月 初"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("2024年 八月 "));
	}

	@Test
	void seasonPrefixTakesTheLongestTerm(){
		final ChineseDateInput autumn = ChineseCalendarParser.parse("qiutian 2024年");
		Assertions.assertEquals(-1, autumn.month());
		Assertions.assertEquals(-1, ChineseCalendarParser.parse("QIU 2024年").month());
		Assertions.assertEquals(-1, ChineseCalendarParser.parse("liqiu 2024年").month());
		Assertions.assertEquals(-3, ChineseCalendarParser.parse("Li-Ch'un 2024年").month());
		Assertions.assertEquals(-3, ChineseCalendarParser.parse("春天 2024年").month());
		Assertions.assertEquals(-2, ChineseCalendarParser.parse("冬季 2024年").month());
		//Lichun 2024 was on 4 February, Liqiu on 7 August
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 2, 4), parse("立春 2024年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 8, 7), parse("立秋 2024年"));
	}

	/**
	 * Start of the seasons (Lichun, Lixia, Liqiu, Lidong; the parser supports only these four solar terms, not the
	 * equinoxes/solstices) in Beijing time, as published by the Chinese Wikipedia pages of the terms
	 * (https://zh.wikipedia.org/wiki/立春, /立夏, /立秋, /立冬, from the Purple Mountain Observatory / astronomical data,
	 * UTC listed, +8 h applied). The civil day is the one of Beijing, not of UT: the cases marked * fall in the
	 * Beijing evening/early morning where the UT day is the previous one.
	 */
	@Test
	void seasonalTermsFollowTheBeijingCivilDay(){
		//Lichun: 2019-02-04 11:14, 2020-02-04 17:03, 2021-02-03 22:58*, 2022-02-04 04:51*, 2025-02-03 22:10*, 2026-02-04 04:02*
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2019, 2, 4), parse("立春 2019年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2020, 2, 4), parse("立春 2020年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2021, 2, 3), parse("立春 2021年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2022, 2, 4), parse("立春 2022年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2025, 2, 3), parse("立春 2025年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2026, 2, 4), parse("立春 2026年"));
		//Lixia: 2019-05-06 03:02*, 2020-05-05 08:51, 2021-05-05 14:47, 2022-05-05 20:25, 2023-05-06 02:18*, 2024-05-05 08:10
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2019, 5, 6), parse("立夏 2019年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2020, 5, 5), parse("立夏 2020年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2021, 5, 5), parse("立夏 2021年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2022, 5, 5), parse("立夏 2022年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 5, 6), parse("立夏 2023年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 5, 5), parse("lixia 2024年"));
		//Liqiu: 2019-08-08 03:13*, 2020-08-07 09:06, 2022-08-07 20:29, 2023-08-08 02:23*, 2024-08-07 08:09
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2019, 8, 8), parse("立秋 2019年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2020, 8, 7), parse("立秋 2020年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2022, 8, 7), parse("立秋 2022年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 8, 8), parse("立秋 2023年"));
		//Lidong: 2019-11-08 01:24*, 2020-11-07 07:13, 2022-11-07 18:45, 2023-11-08 00:35*, 2024-11-07 06:20, 2026-11-07 17:52
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2019, 11, 8), parse("立冬 2019年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2020, 11, 7), parse("立冬 2020年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2022, 11, 7), parse("立冬 2022年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2023, 11, 8), parse("lidong 2023年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2024, 11, 7), parse("立冬 2024年"));
		Assertions.assertEquals(GregorianCalendarStrategy.toJdn(2026, 11, 7), parse("立冬 2026年"));
	}

	@Test
	void sexagenaryYearNeedsAnAnchor(){
		//a sexagenary year alone repeats every 60 years
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("甲辰年 正月 初一"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("庚申年 正月 初一"));
		//a numeric year alone is enough
		Assertions.assertEquals(new ChineseDateInput(2024, 1, false, 1), ChineseCalendarParser.parse("2024年 正月 初一"));
		Assertions.assertEquals(new ChineseDateInput(2024, 1, false, 1), ChineseCalendarParser.parse("2024年 甲辰年 正月 初一"));
		//the era anchors it too
		Assertions.assertEquals(new ChineseDateInput(1900, 1, false, 1), ChineseCalendarParser.parse("光绪二十六年 庚子年 正月 初一"));
		Assertions.assertEquals(new ChineseDateInput(1696, 8, false, 2), ChineseCalendarParser.parse("康熙三十五年八月初二"));
	}

	@Test
	void sexagenaryYearIsValidated(){
		//stem and branch of a different parity do not exist
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("2024年 甲丑年 正月 初一"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("2024年 乙子年 正月 初一"));
		//a valid combination of another year
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("2024年 乙巳年 正月 初一"));
		Assertions.assertThrows(IllegalArgumentException.class, () -> ChineseCalendarParser.parse("光绪二十六年 甲辰年 正月 初一"));
	}

}
