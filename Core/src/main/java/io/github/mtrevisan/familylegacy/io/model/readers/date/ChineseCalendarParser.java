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


import java.util.Objects;


/**
 * A utility class to parse traditional Chinese calendar date strings into pure integer representations.
 * Handles the Sexagenary Cycle (Ganzhi) years, intercalary month flags (Run), and decade day prefixes.
 */
public final class ChineseCalendarParser{

	// 10 Celestial Stems (Tiangan)
	private static final String STEMS = "甲乙丙丁戊己庚辛壬癸";
	// 12 Terrestrial Branches (Dizhi)
	private static final String BRANCHES = "子丑寅卯辰巳午未申酉戌亥";


	private ChineseCalendarParser(){}

	/**
	 * Parses a string in the format "庚申年 正月 初一" or "庚申年 闰四月 廿二" into a discrete data record.
	 * Whitespace between Year, Month, and Day tokens is automatically stripped during validation.
	 *
	 * @param chineseDateString the raw traditional Chinese date string
	 * @param baseGregorianYear a reference anchor year to pin the 60-year cyclical loop (e.g., 1980)
	 * @return a structured ChineseDateInput instance ready for astronomical execution
	 */
	public static ChineseDateInput parse(final String chineseDateString, final int baseGregorianYear){
		Objects.requireNonNull(chineseDateString, "The input date string cannot be null.");

		// Normalize text by removing spaces and trailing metadata markers
		String cleanStr = chineseDateString.replaceAll("\\s+", "");

		// 1. Parse Year Component (Extracts the 60-year cyclical index)
		final int yearEndIndex = cleanStr.indexOf('年');
		if(yearEndIndex < 2){
			throw new IllegalArgumentException("Invalid string format: Missing structural year designation '年'.");
		}
		final String yearToken = cleanStr.substring(yearEndIndex - 2, yearEndIndex);
		final int cycleYear = parseCyclicalYear(yearToken, baseGregorianYear);
		cleanStr = cleanStr.substring(yearEndIndex + 1); // Truncate out the processed text

		// 2. Parse Intercalary Month Flag (Runyue)
		boolean isLeapMonth = false;
		if(cleanStr.startsWith("闰") || cleanStr.startsWith("閏")){
			isLeapMonth = true;
			cleanStr = cleanStr.substring(1);
		}

		// 3. Parse Month Component
		final int monthEndIndex = cleanStr.indexOf('月');
		if(monthEndIndex == -1){
			throw new IllegalArgumentException("Invalid string format: Missing structural month designation '月'.");
		}
		final String monthToken = cleanStr.substring(0, monthEndIndex);
		final int monthNumber = parseMonth(monthToken);
		cleanStr = cleanStr.substring(monthEndIndex + 1);

		// 4. Parse Day Component
		final int dayNumber = parseDay(cleanStr);

		return new ChineseDateInput(cycleYear, monthNumber, isLeapMonth, dayNumber);
	}

	private static int parseCyclicalYear(final String ganzhi, final int baseYear){
		final char stem = ganzhi.charAt(0);
		final char branch = ganzhi.charAt(1);

		final int stemIdx = STEMS.indexOf(stem);
		final int branchIdx = BRANCHES.indexOf(branch);

		if(stemIdx == -1 || branchIdx == -1){
			throw new IllegalArgumentException("The provided combination '" + ganzhi + "' contains invalid Stem/Branch characters.");
		}

		// Calculate the sessagesimal index within the [0, 59] boundary spectrum
		int sexagenaryIndex = (stemIdx - branchIdx);
		if(sexagenaryIndex < 0){
			sexagenaryIndex += 12;
		}
		sexagenaryIndex = (sexagenaryIndex * 5) + stemIdx;

		// Pin the relative index against a physical calendar baseline anchor
		// Historical reference point: Year 1984 was a '甲子' year (Sexagenary Index = 0)
		int anchorYear = 1984 + sexagenaryIndex;
		while(anchorYear > baseYear){
			anchorYear -= 60;
		}
		while(anchorYear < baseYear - 59){
			anchorYear += 60;
		}
		return anchorYear;
	}

	private static int parseMonth(final String monthToken){
		if(monthToken.equals("正")){
			return 1;
		}
		if(monthToken.equals("腊") || monthToken.equals("臘")){
			return 12;
		}
		if(monthToken.equals("十一")){
			return 11;
		}
		if(monthToken.equals("十二")){
			return 12;
		}

		final String numbers = "零一二三四五六七八九十";
		final int idx = numbers.indexOf(monthToken);
		if(idx != -1){
			return idx;
		}
		throw new IllegalArgumentException("Unrecognized Chinese calendar month name: " + monthToken);
	}

	private static int parseDay(final String dayToken){
		if(dayToken.isEmpty()){
			throw new IllegalArgumentException("Missing required day string component.");
		}
		if(dayToken.equals("三十")){
			return 30;
		}
		if(dayToken.equals("二十")){
			return 20;
		}

		final String digits = "零一二三四五六七八九";
		final char prefix = dayToken.charAt(0);
		final char unitChar = dayToken.charAt(1);
		final int unitValue = digits.indexOf(unitChar);

		if(unitValue == -1){
			throw new IllegalArgumentException("Invalid numeral unit tracking inside character: " + unitChar);
		}

		switch(prefix){
			case '初':
				return unitValue;
			case '十':
				return 10 + unitValue;
			case '廿':
			case '念':
				return 20 + unitValue;
			case '卅':
				return 30 + unitValue;
			default:
				throw new IllegalArgumentException("Unknown Chinese decade classification boundary: " + prefix);
		}
	}


	public static void main(String[] args){
		// Input text trace representing Feb 16, 1980 (Chinese New Year)
		String textInput = "庚申年 正月 初一";

		// 1. Pass text and set the search perspective to the 1980 macro-era
		ChineseDateInput decodedInput = ChineseCalendarParser.parse(textInput, 1980);
		System.out.println(decodedInput);
		// Console prints: Year: 1980, Month: 1 (Leap: false), Day: 1

		// 2. Feed directly into your astronomical calculations
		double exactJdUT = ChineseCalendarAstronomicalEngine.toJulianDateUT(
			decodedInput.getYear(),
			decodedInput.getMonth(),
			decodedInput.isLeapMonth(),
			decodedInput.getDay()
		);

		System.out.println("Resulting Julian Date UT: " + exactJdUT);
		System.out.println("Resulting Julian Date UT: 2444285.16666");
	}

}
