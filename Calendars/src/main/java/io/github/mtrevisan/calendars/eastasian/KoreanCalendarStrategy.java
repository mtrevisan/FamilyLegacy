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
package io.github.mtrevisan.calendars.eastasian;

import io.github.mtrevisan.calendars.chinese.LunisolarAstronomicalEngine;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;

import java.util.Map;


/**
 * The Korean lunisolar calendar (음력), with the years numbered as the Gregorian year in which they mostly fall, e.g.
 * "음력 2024년 1월 1일", "1966년 윤3월 5일", "lunar 1997 1 1".
 * <p>
 * The rules are those of the Chinese calendar (see {@link LunisolarAstronomicalEngine}), applied to the time of Korea,
 * as in the calendar of the Korea Astronomy and Space Science Institute: Seoul mean time up to March 1908, UTC+8:30 up to
 * 1911 and from 21 March 1954 to 9 August 1961, UTC+9 otherwise. So the Korean new year or leap month sometimes differs
 * from the Chinese one (e.g. Seollal 1997 fell on 8 February, the Chinese new year on 7 February).
 */
public class KoreanCalendarStrategy implements CalendarConverterPlugin{

	private static final String LUNAR = "lunar";
	private static final Map<String, String> MARKERS = Map.of("음력", LUNAR, "陰曆", LUNAR, "阴历", LUNAR, "lunar", LUNAR);

	/** Seoul (126° 58′ E) mean time [hours]. */
	private static final double SEOUL_MEAN_TIME = (126. + 58. / 60.) / 15.;
	private static final double JD_1908_04_01 = GregorianCalendarStrategy.toJdn(1908, 4, 1) - 0.5;
	private static final double JD_1912_01_01 = GregorianCalendarStrategy.toJdn(1912, 1, 1) - 0.5;
	private static final double JD_1954_03_21 = GregorianCalendarStrategy.toJdn(1954, 3, 21) - 0.5;
	private static final double JD_1961_08_10 = GregorianCalendarStrategy.toJdn(1961, 8, 10) - 0.5;

	private static final LunisolarAstronomicalEngine KOREA = new LunisolarAstronomicalEngine(KoreanCalendarStrategy::utcOffset);


	@Override
	public long parseToJdn(final String rawDate){
		final EastAsianDateInput input = EastAsianEraDateParser.parse(rawDate, MARKERS, LUNAR);
		final int month = input.monthOrFirst();
		final int day = (input.day() == EastAsianDateInput.LAST_DAY
			? KOREA.monthLength(input.year(), month, input.leapMonth())
			: input.dayOrFirst());
		return KOREA.toJdn(input.year(), month, input.leapMonth(), day);
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the time of Korea [hours]
	 */
	static double utcOffset(final double jd){
		if(jd < JD_1908_04_01)
			return SEOUL_MEAN_TIME;
		if(jd < JD_1912_01_01 || jd >= JD_1954_03_21 && jd < JD_1961_08_10)
			return 8.5;
		return 9.;
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		return EastAsianEraDateParser.parse(rawDate, MARKERS, LUNAR)
			.toMetadata();
	}

}
