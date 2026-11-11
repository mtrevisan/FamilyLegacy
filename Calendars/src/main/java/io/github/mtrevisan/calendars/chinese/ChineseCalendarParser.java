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
package io.github.mtrevisan.calendars.chinese;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A utility class to parse traditional Chinese calendar date strings into pure integer representations.
 * Handles historical Era names (niánhào) with reign years, Sexagenary Cycle (gānzhī) years,
 * intercalary month flags (rùn), and decade day prefixes.
 */
public final class ChineseCalendarParser{

	// 10 Celestial Stems (Tiangan)
	private static final String STEMS = "甲乙丙丁戊己庚辛壬癸";
	// 12 Terrestrial Branches (Dizhi)
	private static final String BRANCHES = "子丑寅卯辰巳午未申酉戌亥";

	// Lunar calendar
	private static final String NONGLI1 = "农历";
	private static final String NONGLI2 = "農曆";

	// Leap
	private static final String RUN1 = "闰";
	private static final String RUN2 = "閏";

	private static final String YUAN = "元";

	private static final String ZHENG = "正";
	private static final String LA1 = "腊";
	private static final String LA2 = "臘";
	// eleven
	private static final String SHIYI = "十一";
	// twelve
	private static final String SHIER = "十二";
	// twenty
	private static final String ERSHI = "二十";
	// thirty
	private static final String SANSHI = "三十";

	// Historical Era starting Gregorian year mapping database
	private static final Map<String, Integer> ERA_BASE_YEARS = new HashMap<>();
	static{
		// Ming Dynasty (not rigorous, but close enough)
		ERA_BASE_YEARS.put("洪武", 1368);
		ERA_BASE_YEARS.put("建文", 1399);
		ERA_BASE_YEARS.put("永乐", 1403);
		ERA_BASE_YEARS.put("洪熙", 1425);
		ERA_BASE_YEARS.put("宣德", 1426);
		ERA_BASE_YEARS.put("正统", 1436);
		ERA_BASE_YEARS.put("景泰", 1450);
		ERA_BASE_YEARS.put("天顺", 1457);
		ERA_BASE_YEARS.put("成化", 1465);
		ERA_BASE_YEARS.put("弘治", 1488);
		ERA_BASE_YEARS.put("正德", 1506);
		ERA_BASE_YEARS.put("嘉靖", 1522);
		ERA_BASE_YEARS.put("隆庆", 1567);
		ERA_BASE_YEARS.put("万历", 1573);
		ERA_BASE_YEARS.put("泰昌", 1620);
		ERA_BASE_YEARS.put("天启", 1621);
		ERA_BASE_YEARS.put("崇祯", 1628);

		// Qing Dynasty
		ERA_BASE_YEARS.put("顺治", 1644);
		ERA_BASE_YEARS.put("康熙", 1662);
		ERA_BASE_YEARS.put("雍正", 1723);
		ERA_BASE_YEARS.put("乾隆", 1736);
		ERA_BASE_YEARS.put("嘉庆", 1796);
		ERA_BASE_YEARS.put("道光", 1821);
		ERA_BASE_YEARS.put("咸丰", 1851);
		ERA_BASE_YEARS.put("同治", 1862);
		ERA_BASE_YEARS.put("光绪", 1875);
		ERA_BASE_YEARS.put("宣统", 1909);

		// Republic of China (Minguo)
		ERA_BASE_YEARS.put("民国", 1912);

		// Traditional characters, as written in the genealogies (jiapu)
		ERA_BASE_YEARS.put("永樂", 1403);
		ERA_BASE_YEARS.put("正統", 1436);
		ERA_BASE_YEARS.put("天順", 1457);
		ERA_BASE_YEARS.put("隆慶", 1567);
		ERA_BASE_YEARS.put("萬曆", 1573);
		ERA_BASE_YEARS.put("萬歷", 1573);
		ERA_BASE_YEARS.put("天啓", 1621);
		ERA_BASE_YEARS.put("天啟", 1621);
		ERA_BASE_YEARS.put("崇禎", 1628);
		ERA_BASE_YEARS.put("順治", 1644);
		ERA_BASE_YEARS.put("嘉慶", 1796);
		ERA_BASE_YEARS.put("咸豐", 1851);
		ERA_BASE_YEARS.put("光緒", 1875);
		ERA_BASE_YEARS.put("宣統", 1909);
		ERA_BASE_YEARS.put("民國", 1912);
	}

	private static final Map<String, Integer> SEASONS = new HashMap<>();
	static{
		// Autumn Cycle (Solar Longitude 135° to 180° - Commences at Liqiu)
		SEASONS.put("秋", CalendarSeason.SEASON_AUTUMN);			// Simplified & Traditional character
		SEASONS.put("秋天", CalendarSeason.SEASON_AUTUMN);			// Common colloquial form (Qiutian)
		SEASONS.put("秋季", CalendarSeason.SEASON_AUTUMN);			// Formal seasonal form (Qiuji)
		SEASONS.put("立秋", CalendarSeason.SEASON_AUTUMN);			// "Start of Autumn" Solar Term (Liqiu)
		SEASONS.put("qiu", CalendarSeason.SEASON_AUTUMN);			// Standard Pinyin (no tones)
		SEASONS.put("qiutian", CalendarSeason.SEASON_AUTUMN);	// Colloquial Pinyin
		SEASONS.put("qiuji", CalendarSeason.SEASON_AUTUMN);		// Formal Pinyin
		SEASONS.put("liqiu", CalendarSeason.SEASON_AUTUMN);		// Solar term Pinyin
		SEASONS.put("li-ch'iu", CalendarSeason.SEASON_AUTUMN);	// Wade-Giles transliteration

		// Winter Cycle (Solar Longitude 225° to 270° - Commences at Lidong)
		SEASONS.put("冬", CalendarSeason.SEASON_WINTER);			// Simplified & Traditional character
		SEASONS.put("冬天", CalendarSeason.SEASON_WINTER);			// Common colloquial form (Dongtian)
		SEASONS.put("冬季", CalendarSeason.SEASON_WINTER);			// Formal seasonal form (Dongji)
		SEASONS.put("立冬", CalendarSeason.SEASON_WINTER);			// "Start of Winter" Solar Term (Lidong)
		SEASONS.put("dong", CalendarSeason.SEASON_WINTER);		// Standard Pinyin
		SEASONS.put("dongtian", CalendarSeason.SEASON_WINTER);	// Colloquial Pinyin
		SEASONS.put("dongji", CalendarSeason.SEASON_WINTER);		// Formal Pinyin
		SEASONS.put("lidong", CalendarSeason.SEASON_WINTER);		// Solar term Pinyin
		SEASONS.put("li-tung", CalendarSeason.SEASON_WINTER);	// Wade-Giles transliteration

		// Spring Cycle (Solar Longitude 315° to 360°/0° - Commences at Lichun)
		SEASONS.put("春", CalendarSeason.SEASON_SPRING);			// Simplified & Traditional character
		SEASONS.put("春天", CalendarSeason.SEASON_SPRING);			// Common colloquial form (Chuntian)
		SEASONS.put("春季", CalendarSeason.SEASON_SPRING);			// Formal seasonal form (Chunji)
		SEASONS.put("立春", CalendarSeason.SEASON_SPRING);			// "Start of Spring" Solar Term (Lichun)
		SEASONS.put("chun", CalendarSeason.SEASON_SPRING);		// Standard Pinyin
		SEASONS.put("chuntian", CalendarSeason.SEASON_SPRING);	// Colloquial Pinyin
		SEASONS.put("chunji", CalendarSeason.SEASON_SPRING);		// Formal Pinyin
		SEASONS.put("lichun", CalendarSeason.SEASON_SPRING);		// Solar term Pinyin
		SEASONS.put("li-ch'un", CalendarSeason.SEASON_SPRING);	// Wade-Giles transliteration

		// Summer Cycle (Solar Longitude 45° to 90° - Commences at Lixia)
		SEASONS.put("夏", CalendarSeason.SEASON_SUMMER);			// Simplified Chinese character
		SEASONS.put("夏天", CalendarSeason.SEASON_SUMMER);			// Common colloquial form (Xiatian)
		SEASONS.put("夏季", CalendarSeason.SEASON_SUMMER);			// Formal seasonal form (Xiaji)
		SEASONS.put("立夏", CalendarSeason.SEASON_SUMMER);			// "Start of Summer" Solar Term (Lixia)
		SEASONS.put("xia", CalendarSeason.SEASON_SUMMER);			// Standard Pinyin
		SEASONS.put("xiatian", CalendarSeason.SEASON_SUMMER);	// Colloquial Pinyin
		SEASONS.put("xiaji", CalendarSeason.SEASON_SUMMER);		// Formal Pinyin
		SEASONS.put("lixia", CalendarSeason.SEASON_SUMMER);		// Solar term Pinyin
		SEASONS.put("li-hsia", CalendarSeason.SEASON_SUMMER);	// Wade-Giles transliteration
	}

	private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{1,4})年");

	/** 4 AD is the year of the first sexagenary combination (jiazi, 甲子), as 1984 and 2044. */
	private static final int SEXAGENARY_EPOCH_YEAR = 4;
	private static final int SEXAGENARY_CYCLE = 60;

	private static final String DIGITS = "一二三四五六七八九";


	private ChineseCalendarParser(){}


	/**
	 * Parses a string in formats like "2020年 庚子年 正月 初一" or "康熙三十五年八月初二" into a discrete data record.
	 * Whitespace between tokens is automatically stripped during validation.
	 * <p>
	 * The year needs an anchor: an era (nianhao, e.g. 康熙三十五年), or a numeric year (e.g. 2020年), optionally followed by
	 * the sexagenary year (庚子年), that is checked against it. A sexagenary year alone is ambiguous (it repeats every 60
	 * years) and is rejected. A season (or solar term) takes no month nor day.
	 * </p>
	 *
	 * @param chineseDateString the raw traditional Chinese date string
	 * @return a structured ChineseDateInput instance ready for astronomical execution
	 * @throws IllegalArgumentException if the string is not valid, the year is not anchored, the stem and the branch of a
	 * 	sexagenary year do not match, or a season is given with a month or a day
	 */
	public static ChineseDateInput parse(final String chineseDateString){
		// Normalize text by removing spaces
		String cleanStr = StringUtils.deleteWhitespace(chineseDateString);
		cleanStr = stripLunarPrefix(cleanStr);

		// Check if the prefix maps to a direct seasonal/Jieqi token property: the longest matching term wins
		Integer seasonalFlag = null;
		final String lowerCase = cleanStr.toLowerCase(Locale.ROOT);
		String seasonTerm = null;
		for(final String term : SEASONS.keySet())
			if(lowerCase.startsWith(term) && (seasonTerm == null || term.length() > seasonTerm.length()))
				seasonTerm = term;
		if(seasonTerm != null){
			seasonalFlag = SEASONS.get(seasonTerm);
			cleanStr = stripLunarPrefix(cleanStr.substring(seasonTerm.length()));
		}

		final int cycleYear;

		// Check for an explicit numeric year prefix (e.g., "2024年")
		Integer explicitYear = null;
		final Matcher yearMatcher = YEAR_PATTERN.matcher(cleanStr);
		if(yearMatcher.find()){
			explicitYear = Integer.parseInt(yearMatcher.group(1));
			cleanStr = cleanStr.substring(yearMatcher.end());
		}

		// 1. Check if the string contains a historical Era name (Nianhao): the one that comes first, the longest if tied
		String matchedEra = null;
		int eraIndex = -1;
		for(final String era : ERA_BASE_YEARS.keySet()){
			final int idx = cleanStr.indexOf(era);
			if(idx != -1 && (matchedEra == null || idx < eraIndex || idx == eraIndex && era.length() > matchedEra.length())){
				eraIndex = idx;
				matchedEra = era;
			}
		}

		if(matchedEra != null){
			// Parse Era-based year format (e.g., "康熙三十五年" or "民国38年")
			cleanStr = cleanStr.substring(eraIndex + matchedEra.length());
			final int yearEndIndex = cleanStr.indexOf('年');
			if(yearEndIndex == -1)
				throw new IllegalArgumentException("Invalid string format: Missing structural year designation '年' after era.");

			final String reignYearToken = cleanStr.substring(0, yearEndIndex);
			final int reignYear = (StringUtils.isNumeric(reignYearToken)
				? Integer.parseInt(reignYearToken)
				: parseChineseNumber(reignYearToken));
			if(reignYear < 1)
				throw new IllegalArgumentException("Invalid reign year '" + reignYearToken + "'.");
			cycleYear = ERA_BASE_YEARS.get(matchedEra) + reignYear - 1;
			cleanStr = cleanStr.substring(yearEndIndex + 1);
		}
		else if(explicitYear != null)
			cycleYear = explicitYear;
		else
			throw new IllegalArgumentException("The year needs an era (e.g. 康熙三十五年) or a numeric year (e.g. 2020年 庚子年):"
				+ " a sexagenary year alone repeats every 60 years.");

		// Optional sexagenary year, checked against the anchored one
		cleanStr = stripLunarPrefix(cleanStr);
		if(cleanStr.length() >= 3 && STEMS.indexOf(cleanStr.charAt(0)) >= 0 && BRANCHES.indexOf(cleanStr.charAt(1)) >= 0
				&& cleanStr.charAt(2) == '年'){
			if(sexagenaryIndex(cleanStr.substring(0, 2)) != Math.floorMod(cycleYear - SEXAGENARY_EPOCH_YEAR, SEXAGENARY_CYCLE))
				throw new IllegalArgumentException("Era year and Ganzhi year are inconsistent: " + chineseDateString);

			cleanStr = cleanStr.substring(3);
		}

		// Handle conditional routing branching rules
		if(seasonalFlag != null){
			// SEASONAL JUNCTION CONTEXT: standard lunisolar month and day are not allowed, the day is kept as 0
			if(!cleanStr.isEmpty())
				throw new IllegalArgumentException("A season cannot be given with a month or a day: " + chineseDateString);

			return new ChineseDateInput(cycleYear, seasonalFlag, false, 0);
		}

		// 2. Parse Intercalary Month Flag (Runyue)
		boolean isLeapMonth = false;
		if(cleanStr.startsWith(RUN1) || cleanStr.startsWith(RUN2)){
			isLeapMonth = true;
			cleanStr = cleanStr.substring(1);
		}

		// 3. Parse Month Component
		final int monthEndIndex = cleanStr.indexOf('月');
		if(monthEndIndex == -1)
			throw new IllegalArgumentException("Invalid string format: Missing structural month designation '月'.");

		final String monthToken = cleanStr.substring(0, monthEndIndex);
		final int monthNumber = parseMonth(monthToken);
		cleanStr = cleanStr.substring(monthEndIndex + 1);

		// 4. Parse Day Component
		final int dayNumber = parseDay(cleanStr);

		return new ChineseDateInput(cycleYear, monthNumber, isLeapMonth, dayNumber);
	}

	private static String stripLunarPrefix(final String text){
		return (text.startsWith(NONGLI1) || text.startsWith(NONGLI2)? text.substring(NONGLI1.length()): text);
	}

	/**
	 * @param ganzhi the stem and the branch
	 * @return the index (0-59) in the sexagenary cycle
	 * @throws IllegalArgumentException if a character is not a stem or a branch, or the stem and the branch have different parity
	 */
	private static int sexagenaryIndex(final String ganzhi){
		final int stemIdx = STEMS.indexOf(ganzhi.charAt(0));
		final int branchIdx = BRANCHES.indexOf(ganzhi.charAt(1));

		if(stemIdx == -1 || branchIdx == -1)
			throw new IllegalArgumentException("The provided combination '" + ganzhi + "' contains invalid Stem/Branch characters.");
		if((stemIdx - branchIdx) % 2 != 0)
			throw new IllegalArgumentException("The combination '" + ganzhi + "' does not exist in the sexagenary cycle:"
				+ " stem and branch must have the same parity.");

		// Calculate the sexagenary index within the [0, 59] boundary spectrum
		return Math.floorMod(stemIdx - branchIdx, 12) * 5 + stemIdx;
	}

	private static int parseChineseNumber(final String token){
		if(token.equals(YUAN))
			return 1;

		int total = 0;
		int intermediate = 0;
		for(int i = 0; i < token.length(); i ++){
			final char c = token.charAt(i);
			final int digitVal = DIGITS.indexOf(c);
			if(digitVal != -1)
				intermediate = digitVal + 1;
			else if(c == '十'){
				if(intermediate == 0)
					intermediate = 1;
				total += intermediate * 10;
				intermediate = 0;
			}
			else if(c == '百'){
				if(intermediate == 0)
					intermediate = 1;
				total += intermediate * 100;
				intermediate = 0;
			}
			else
				throw new IllegalArgumentException("Invalid character '" + c + "' in the Chinese number '" + token + "'.");
		}
		total += intermediate;
		return total;
	}

	private static int parseMonth(final String monthToken){
		return switch(monthToken){
			case ZHENG -> 1;
			case LA1, LA2 -> 12;
			case SHIYI -> 11;
			case SHIER -> 12;
			default -> parseChineseNumber(monthToken);
		};
	}

	private static int parseDay(final String dayToken){
		if(dayToken.isEmpty())
			throw new IllegalArgumentException("Missing required day string component.");

		if(dayToken.equals(ERSHI))
			return 20;
		if(dayToken.equals(SANSHI))
			return 30;
		if(dayToken.length() == 1)
			// a single character: 十 (10), 廿 (20), 卅 (30), or a unit (1-9)
			return switch(dayToken.charAt(0)){
				case '十' -> 10;
				case '廿', '念' -> 20;
				case '卅' -> 30;
				default -> parseChineseNumber(dayToken);
			};

		final char prefix = dayToken.charAt(0);
		final char unitChar = dayToken.charAt(1);
		final int unitValue = DIGITS.indexOf(unitChar);

		return switch(prefix){
			case '初' -> {
				// 初一 .. 初九, and 初十 (the tenth day)
				if(unitChar == '十' && dayToken.length() == 2)
					yield 10;
				if(unitValue == -1 || dayToken.length() != 2)
					throw new IllegalArgumentException("Invalid day '" + dayToken + "'.");
				yield unitValue + 1;
			}
			case '十' -> 10 + (unitValue != -1? unitValue + 1: 0);
			case '廿', '念' -> 20 + (unitValue != -1? unitValue + 1: 0);
			case '卅' -> 30 + (unitValue != -1? unitValue + 1: 0);
			default -> parseChineseNumber(dayToken);
		};
	}

}
