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

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

	// Chine republic
	private static final String MINGUO = "民国";

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
		SEASONS.put("秋", CalendarType.SEASON_AUTUMN);        // Simplified & Traditional character
		SEASONS.put("秋天", CalendarType.SEASON_AUTUMN);      // Common colloquial form (Qiutian)
		SEASONS.put("秋季", CalendarType.SEASON_AUTUMN);      // Formal seasonal form (Qiuji)
		SEASONS.put("立秋", CalendarType.SEASON_AUTUMN);      // "Start of Autumn" Solar Term (Liqiu)
		SEASONS.put("qiu", CalendarType.SEASON_AUTUMN);       // Standard Pinyin (no tones)
		SEASONS.put("qiutian", CalendarType.SEASON_AUTUMN);   // Colloquial Pinyin
		SEASONS.put("qiuji", CalendarType.SEASON_AUTUMN);     // Formal Pinyin
		SEASONS.put("liqiu", CalendarType.SEASON_AUTUMN);     // Solar term Pinyin
		SEASONS.put("li-ch'iu", CalendarType.SEASON_AUTUMN);  // Wade-Giles transliteration

		// Winter Cycle (Solar Longitude 225° to 270° - Commences at Lidong)
		SEASONS.put("冬", CalendarType.SEASON_WINTER);        // Simplified & Traditional character
		SEASONS.put("冬天", CalendarType.SEASON_WINTER);      // Common colloquial form (Dongtian)
		SEASONS.put("冬季", CalendarType.SEASON_WINTER);      // Formal seasonal form (Dongji)
		SEASONS.put("立冬", CalendarType.SEASON_WINTER);      // "Start of Winter" Solar Term (Lidong)
		SEASONS.put("dong", CalendarType.SEASON_WINTER);      // Standard Pinyin
		SEASONS.put("dongtian", CalendarType.SEASON_WINTER);  // Colloquial Pinyin
		SEASONS.put("dongji", CalendarType.SEASON_WINTER);    // Formal Pinyin
		SEASONS.put("lidong", CalendarType.SEASON_WINTER);    // Solar term Pinyin
		SEASONS.put("li-tung", CalendarType.SEASON_WINTER);   // Wade-Giles transliteration

		// Spring Cycle (Solar Longitude 315° to 360°/0° - Commences at Lichun)
		SEASONS.put("春", CalendarType.SEASON_SPRING);        // Simplified & Traditional character
		SEASONS.put("春天", CalendarType.SEASON_SPRING);      // Common colloquial form (Chuntian)
		SEASONS.put("春季", CalendarType.SEASON_SPRING);      // Formal seasonal form (Chunji)
		SEASONS.put("立春", CalendarType.SEASON_SPRING);      // "Start of Spring" Solar Term (Lichun)
		SEASONS.put("chun", CalendarType.SEASON_SPRING);      // Standard Pinyin
		SEASONS.put("chuntian", CalendarType.SEASON_SPRING);  // Colloquial Pinyin
		SEASONS.put("chunji", CalendarType.SEASON_SPRING);    // Formal Pinyin
		SEASONS.put("lichun", CalendarType.SEASON_SPRING);    // Solar term Pinyin
		SEASONS.put("li-ch'un", CalendarType.SEASON_SPRING);  // Wade-Giles transliteration

		// Summer Cycle (Solar Longitude 45° to 90° - Commences at Lixia)
		SEASONS.put("夏", CalendarType.SEASON_SUMMER);        // Simplified Chinese character
		SEASONS.put("夏天", CalendarType.SEASON_SUMMER);      // Common colloquial form (Xiatian)
		SEASONS.put("夏季", CalendarType.SEASON_SUMMER);      // Formal seasonal form (Xiaji)
		SEASONS.put("立夏", CalendarType.SEASON_SUMMER);      // "Start of Summer" Solar Term (Lixia)
		SEASONS.put("xia", CalendarType.SEASON_SUMMER);       // Standard Pinyin
		SEASONS.put("xiatian", CalendarType.SEASON_SUMMER);   // Colloquial Pinyin
		SEASONS.put("xiaji", CalendarType.SEASON_SUMMER);     // Formal Pinyin
		SEASONS.put("lixia", CalendarType.SEASON_SUMMER);     // Solar term Pinyin
		SEASONS.put("li-hsia", CalendarType.SEASON_SUMMER);   // Wade-Giles transliteration
	}

	private static final Pattern YEAR_PATTERN = Pattern.compile("^(\\d{4})年");

	private static final String DIGITS = "一二三四五六七八九";


	private ChineseCalendarParser(){}


	/**
	 * Parses a string in formats like "庚申年 正月 初一" or "康熙三十五年八月初二" into a discrete data record.
	 * Whitespace between tokens is automatically stripped during validation.
	 *
	 * @param chineseDateString the raw traditional Chinese date string
	 * @return a structured ChineseDateInput instance ready for astronomical execution
	 */
	public static ChineseDateInput parse(final String chineseDateString){
		// Normalize text by removing spaces
		String cleanStr = chineseDateString.replaceAll("\\s+", StringUtils.EMPTY);
		if(cleanStr.startsWith(NONGLI1) || cleanStr.startsWith(NONGLI2))
			cleanStr = cleanStr.substring(2);

		// Check if the prefix maps to a direct seasonal/Jieqi token property
		Integer seasonalFlag = null;
		final Set<String> terms = SEASONS.keySet();
		for(final String term : terms)
			if(cleanStr.startsWith(term)){
				seasonalFlag = SEASONS.get(term);

				cleanStr = cleanStr.substring(term.length());

				break;
			}

		final int cycleYear;

		// Check for an explicit numeric year prefix (e.g., "2024年")
		Integer explicitYear = null;
		final Matcher yearMatcher = YEAR_PATTERN.matcher(cleanStr);
		if(yearMatcher.find()){
			explicitYear = Integer.parseInt(yearMatcher.group(1));
			cleanStr = cleanStr.substring(yearMatcher.end());
		}

		// 1. Check if the string contains a historical Era name (Nianhao)
		String matchedEra = null;
		int eraIndex = -1;
		final Set<String> eras = ERA_BASE_YEARS.keySet();
		for(final String era : eras){
			final int idx = cleanStr.indexOf(era);
			if(idx != -1){
				eraIndex = idx;
				matchedEra = era;

				break;
			}
		}

		if(matchedEra != null){
			// Parse Era-based year format (e.g., "康熙三十五年" or "民国38年")
			cleanStr = cleanStr.substring(eraIndex + matchedEra.length());
			final int yearEndIndex = cleanStr.indexOf('年');
			if(yearEndIndex == -1)
				throw new IllegalArgumentException("Invalid string format: Missing structural year designation '年' after era.");

			final String reignYearToken = cleanStr.substring(0, yearEndIndex);
			final int reignYear = (matchedEra.equals(MINGUO) && StringUtils.isNumeric(reignYearToken))
				? Integer.parseInt(reignYearToken)
				: parseChineseNumber(reignYearToken);
			cycleYear = ERA_BASE_YEARS.get(matchedEra) + reignYear - 1;
			cleanStr = cleanStr.substring(yearEndIndex + 1);
		}
		else{
			// Parse standard Ganzhi cyclical year format (e.g., "庚申年")
			final int yearEndIndex = cleanStr.indexOf('年');
			if(yearEndIndex < 2)
				throw new IllegalArgumentException("Invalid string format: Missing structural year designation '年'.");

			final String yearToken = cleanStr.substring(yearEndIndex - 2, yearEndIndex);
			cycleYear = parseCyclicalYear(yearToken, explicitYear);
			cleanStr = cleanStr.substring(yearEndIndex + 1);
		}

		// Handle conditional routing branching rules
		if(seasonalFlag != null)
			// SEASONAL JUNCTION CONTEXT: Skip standard lunisolar moon-day parsing operations
			// The day is not specified (kept as 0)
			return new ChineseDateInput(cycleYear, seasonalFlag, false, 0);

		// 2. Parse Intercalary Month Flag (Runyue)
		boolean isLeapMonth = false;
		if(cleanStr.startsWith(RUN1) || cleanStr.startsWith(RUN2)){
			isLeapMonth = true;
			cleanStr = cleanStr.substring(1);
		}

		if(cleanStr.length() >= 3){
			final String possibleGanzhi = cleanStr.substring(0, 2);

			if(STEMS.indexOf(possibleGanzhi.charAt(0)) >= 0 && BRANCHES.indexOf(possibleGanzhi.charAt(1)) >= 0
					&& cleanStr.charAt(2) == '年'){
				// Optional consistency check
				final int ganzhiYear = parseCyclicalYear(possibleGanzhi, cycleYear);

				if(ganzhiYear != cycleYear)
					throw new IllegalArgumentException("Era year and Ganzhi year are inconsistent.");

				cleanStr = cleanStr.substring(3);
			}
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

	private static int parseCyclicalYear(final String ganzhi, final Integer explicitYear){
		final char stem = ganzhi.charAt(0);
		final char branch = ganzhi.charAt(1);

		final int stemIdx = STEMS.indexOf(stem);
		final int branchIdx = BRANCHES.indexOf(branch);

		if(stemIdx == -1 || branchIdx == -1)
			throw new IllegalArgumentException("The provided combination '" + ganzhi + "' contains invalid Stem/Branch characters.");

		// Calculate the sexagenary index within the [0, 59] boundary spectrum
		int sexagenaryIndex = (stemIdx - branchIdx);
		if(sexagenaryIndex < 0)
			sexagenaryIndex += 12;
		sexagenaryIndex = (sexagenaryIndex * 5) + stemIdx;

		// If an explicit year is provided (e.g., from an era or numerical indicator),
		// use it as a base to anchor the correct 60-year cycle.
		final int baseAnchor = (explicitYear != null? explicitYear: 1984);

		int anchorYear = baseAnchor + ((sexagenaryIndex - (baseAnchor - 1984) % 60 + 60) % 60);
		// If the calculated distance exceeds 30 years from the explicit year, normalize to the nearest 60-year occurrence
		if(explicitYear != null){
			while(anchorYear - explicitYear > 30)
				anchorYear -= 60;
			while(explicitYear - anchorYear > 30)
				anchorYear += 60;
		}

		return anchorYear;
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
		}
		total += intermediate;
		return total;
	}

	private static int parseMonth(final String monthToken){
		if(monthToken.equals(ZHENG))
			return 1;
		if(monthToken.equals(LA1) || monthToken.equals(LA2))
			return 12;
		if(monthToken.equals(SHIYI))
			return 11;
		if(monthToken.equals(SHIER))
			return 12;

		return parseChineseNumber(monthToken);
	}

	private static int parseDay(final String dayToken){
		if(dayToken.isEmpty())
			throw new IllegalArgumentException("Missing required day string component.");

		if(dayToken.equals(ERSHI))
			return 20;
		if(dayToken.equals(SANSHI))
			return 30;

		final char prefix = dayToken.charAt(0);
		final char unitChar = dayToken.charAt(1);
		final int unitValue = DIGITS.indexOf(unitChar);

		if(unitValue == -1 && prefix != '初' && prefix != '十' && prefix != '廿' && prefix != '卅')
			return parseChineseNumber(dayToken);

		return switch(prefix){
			case '初' -> unitValue + 1;
			case '十' -> 10 + (unitValue != -1? unitValue + 1: 0);
			case '廿', '念' -> 20 + (unitValue != -1? unitValue + 1: 0);
			case '卅' -> 30 + (unitValue != -1? unitValue + 1: 0);
			default -> parseChineseNumber(dayToken);
		};
	}


	public static void main(String[] args){
		// Test 1: Cyclical year string format
		final ChineseDateInput input1 = ChineseCalendarParser.parse("庚申年 正月 初一");
		System.out.println(input1);
		System.out.println("Year: 1980, Month: 1 (Leap: false), Day: 1");

		final double exactJdUT1 = ChineseCalendarAstronomicalEngine.toJulianDateUT(
			input1.year(),
			input1.month(),
			input1.leapMonth(),
			input1.day()
		);
		System.out.println("Resulting Julian Date UT: " + exactJdUT1);
		System.out.println("Resulting Julian Date UT: 2444477.1666666665");


		// Test 2: Historical Imperial Era format
		final ChineseDateInput input2 = ChineseCalendarParser.parse("康熙三十五年八月初二");
		System.out.println(input2);
		System.out.println("Year: 1696, Month: 8 (Leap: false), Day: 2");

		final double exactJdUT2 = ChineseCalendarAstronomicalEngine.toJulianDateUT(
			input2.year(),
			input2.month(),
			input2.leapMonth(),
			input2.day()
		);
		System.out.println("Resulting Julian Date UT: " + exactJdUT2);
		System.out.println("Resulting Julian Date UT: 2340973.176620375");
	}

}
