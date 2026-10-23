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

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.chinese.LunisolarAstronomicalEngine;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.text.TextNormalizer;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The Vietnamese lunisolar calendar (âm lịch), with the years numbered as the Gregorian year in which they mostly fall,
 * e.g. "ngày 1 tháng Giêng năm 2024", "mùng 5 tháng 4 nhuận năm 2020", "rằm tháng Chạp năm 1984", "15/8/2023".
 * <p>
 * The rules are those of the Chinese calendar (see {@link LunisolarAstronomicalEngine}), applied to the time of Vietnam:
 * UTC+8 (the time of China) up to 1967, UTC+7 from 1968, when North Vietnam adopted its own time for the calendar (so
 * the new year of 1968 was on 29 January in the North and on 30 January in the South, and the one of 1985 a month
 * before the Chinese one). Month names: Giêng (1st), Một (11th), Chạp (12th); the leap month is "nhuận".
 */
public class VietnameseCalendarStrategy implements CalendarConverterPlugin{

	private static final double JD_1968_01_01 = GregorianCalendarStrategy.toJdn(1968, 1, 1) - 0.5;

	private static final LunisolarAstronomicalEngine VIETNAM = new LunisolarAstronomicalEngine(
		VietnameseCalendarStrategy::utcOffset);

	private static final String DAY = "ngay";
	private static final Set<String> DAY_PREFIXES = Set.of("ngay", "mung", "mong");
	private static final String MONTH = "thang";
	private static final String YEAR = "nam";
	private static final String LEAP = "nhuan";
	private static final String FULL_MOON_DAY = "ram";
	private static final Map<String, Integer> MONTH_NAMES = Map.of("gieng", 1, "mot", 11, "chap", 12);
	private static final Set<String> FILLERS = Set.of("am", "lich", "al", "nam", "le", "vao", "luc");
	private static final int MIN_YEAR = 100;


	/** A parsed date: the year, the month (0 if missing), the leap flag, and the day (0 if missing). */
	private record LunarDate(int year, int month, boolean leap, int day){}


	@Override
	public long parseToJdn(final String rawDate){
		final LunarDate date = parse(rawDate);
		final int month = (date.month() != 0? date.month(): 1);
		final int day = (date.day() != 0? date.day(): 1);
		return VIETNAM.toJdn(date.year(), month, date.leap(), day);
	}

	/**
	 * @param jd the Julian Date (UT)
	 * @return the time of the Vietnamese calendar [hours]
	 */
	static double utcOffset(final double jd){
		return (jd < JD_1968_01_01? 8.: 7.);
	}

	private static LunarDate parse(final String rawDate){
		final String[] words = TextNormalizer.tokenize(rawDate);
		int year = 0;
		int month = 0;
		int day = 0;
		boolean leap = false;
		final List<Integer> bareNumbers = new ArrayList<>();
		for(int i = 0; i < words.length; i ++){
			final String word = words[i];
			final String next = (i + 1 < words.length? words[i + 1]: null);
			if(word.equals(LEAP))
				leap = true;
			else if(word.equals(FULL_MOON_DAY))
				day = 15;
			else if(DAY_PREFIXES.contains(word) && next != null && StringUtils.isNumeric(next)){
				day = Integer.parseInt(next);
				i ++;
			}
			else if(word.equals(MONTH) && next != null){
				if(next.equals(LEAP) && i + 2 < words.length){
					// "tháng nhuận 4"
					leap = true;
					i ++;
				}
				final String monthWord = words[i + 1];
				final Integer named = MONTH_NAMES.get(monthWord);
				if(named != null)
					month = named;
				else if(StringUtils.isNumeric(monthWord))
					month = Integer.parseInt(monthWord);
				else
					throw new IllegalArgumentException("Unknown Vietnamese month '" + monthWord + "': " + rawDate);
				i ++;
			}
			else if(word.equals(YEAR) && next != null && StringUtils.isNumeric(next)){
				year = Integer.parseInt(next);
				i ++;
			}
			else if(StringUtils.isNumeric(word))
				bareNumbers.add(Integer.parseInt(word));
			else if(!FILLERS.contains(word) && !word.equals(DAY))
				throw new IllegalArgumentException("Unknown word '" + word + "' in the Vietnamese date: " + rawDate);
		}

		// numeric layout: "15/8/2023" (day, month, year) or "2023-08-15"
		if(!bareNumbers.isEmpty()){
			final boolean yearFirst = (bareNumbers.getFirst() >= MIN_YEAR);
			if(yearFirst){
				year = bareNumbers.removeFirst();
				if(!bareNumbers.isEmpty() && month == 0)
					month = bareNumbers.removeFirst();
				if(!bareNumbers.isEmpty() && day == 0)
					day = bareNumbers.removeFirst();
			}
			else{
				if(bareNumbers.getLast() >= MIN_YEAR && year == 0)
					year = bareNumbers.removeLast();
				if(!bareNumbers.isEmpty() && day == 0 && bareNumbers.size() == 2)
					day = bareNumbers.removeFirst();
				if(!bareNumbers.isEmpty() && month == 0)
					month = bareNumbers.removeFirst();
			}
			if(!bareNumbers.isEmpty())
				throw new IllegalArgumentException("Invalid Vietnamese date: " + rawDate);
		}
		if(year < MIN_YEAR)
			throw new IllegalArgumentException("Missing year in the Vietnamese date: " + rawDate);
		if(month < 0 || month > 12)
			throw new IllegalArgumentException("Month must be between 1 and 12: " + rawDate);
		if(leap && month == 0)
			throw new IllegalArgumentException("Leap marker without a month: " + rawDate);
		return new LunarDate(year, month, leap, day);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final LunarDate date = parse(rawDate);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", true);
		components.put("month", (date.month() != 0));
		components.put("day", (date.day() != 0));
		return new CalendarType.CalendarMetadata(components, (date.month() != 0 && date.day() != 0));
	}

}
