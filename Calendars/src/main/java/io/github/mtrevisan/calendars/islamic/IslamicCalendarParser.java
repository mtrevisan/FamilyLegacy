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
package io.github.mtrevisan.calendars.islamic;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Islamic (Umm al-Qura) calendar date expressions using exact astronomical mapping flags.
 * Supports canonical Hijri months and traditional lunar-drift seasonal markers.
 */
public final class IslamicCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Muharram (Sacred Month / New Year)
		MONTHS.put("محرم", 1);
		MONTHS.put("muharram", 1);
		MONTHS.put("moharram", 1);
		MONTHS.put("muharram al-haram", 1);	// Full formal ceremonial name

		// Month 2: Safar
		MONTHS.put("صفر", 2);
		MONTHS.put("safar", 2);
		MONTHS.put("saphar", 2);

		// Month 3: Rabi al-Awwal (The First Rabi)
		MONTHS.put("ربيع الأول", 3);
		MONTHS.put("rabi al-awwal", 3);
		MONTHS.put("rabi al-awal", 3);
		MONTHS.put("rabiul-awwal", 3);		// Combined phonetic variant
		MONTHS.put("rabiulawal", 3);			// Highly common Southeast Asian spelling
		MONTHS.put("rabi i", 3);				// Roman numeral standard shortcut

		// Month 4: Rabi al-Thani / Rabi al-Akhir (The Second/Last Rabi)
		MONTHS.put("ربيع الثاني", 4);
		MONTHS.put("ربيع الآخر", 4);			// Alternative native script variant
		MONTHS.put("rabi al-thani", 4);
		MONTHS.put("rabi al-akhir", 4);		// Critical missing alternative name
		MONTHS.put("rabi al-akhar", 4);
		MONTHS.put("rabiul-thani", 4);
		MONTHS.put("rabiul-akhir", 4);
		MONTHS.put("rabiulakhir", 4);			// Highly common variant
		MONTHS.put("rabi ii", 4);				// Roman numeral standard shortcut

		// Month 5: Jumada al-Awwal (The First Jumada)
		MONTHS.put("جمادى الأولى", 5);
		MONTHS.put("جمادى الأول", 5);
		MONTHS.put("jumada al-awwal", 5);
		MONTHS.put("jumada al-awal", 5);
		MONTHS.put("jumada i", 5);
		MONTHS.put("jumadil-awwal", 5);
		MONTHS.put("jumadilawal", 5);

		// Month 6: Jumada al-Thani / Jumada al-Akhirah (The Second/Last Jumada)
		MONTHS.put("جمادى الآخرة", 6);
		MONTHS.put("جمادى الثاني", 6);
		MONTHS.put("jumada al-thani", 6);
		MONTHS.put("jumada al-akhirah", 6);	// Critical missing alternative name
		MONTHS.put("jumada al-akhir", 6);
		MONTHS.put("jumada ii", 6);
		MONTHS.put("jumadil-thani", 6);
		MONTHS.put("jumadil-akhir", 6);
		MONTHS.put("jumadilakhir", 6);

		// Month 7: Rajab (Sacred Month)
		MONTHS.put("رجب", 7);
		MONTHS.put("rajab", 7);
		MONTHS.put("rajab al-murajjab", 7);	// Full ceremonial name

		// Month 8: Shaban
		MONTHS.put("شعبان", 8);
		MONTHS.put("shaban", 8);
		MONTHS.put("sha'ban", 8);				// Transliteration with explicit Ayn stop
		MONTHS.put("shaaban", 8);

		// Month 9: Ramadan (The Month of Fasting)
		MONTHS.put("رمضان", 9);
		MONTHS.put("ramadan", 9);
		MONTHS.put("ramadhan", 9);				// Highly common Southeast Asian/South Asian variant
		MONTHS.put("ramazan", 9);				// Turkish, Persian, and Urdu standard spelling

		// Month 10: Shawwal
		MONTHS.put("شوال", 10);
		MONTHS.put("shawwal", 10);
		MONTHS.put("shawal", 10);

		// Month 11: Dhu al-Qadah (Sacred Month)
		MONTHS.put("ذو القعدة", 11);
		MONTHS.put("dhu al-qadah", 11);
		MONTHS.put("dhu'l-qa'dah", 11);
		MONTHS.put("dhul-qadah", 11);
		MONTHS.put("zulqadah", 11);			// Common phonetic variant (Dhu -> Z)
		MONTHS.put("dhu al-qi'dah", 11);

		// Month 12: Dhu al-Hijjah (Sacred Month / Month of Hajj)
		MONTHS.put("ذو الحجة", 12);
		MONTHS.put("dhu al-hijjah", 12);
		MONTHS.put("dhu'l-hijjah", 12);
		MONTHS.put("dhul-hijjah", 12);
		MONTHS.put("zulhijjah", 12);			// Highly common colloquial variant
		MONTHS.put("dhu al-hajjah", 12);


		// =============================================================================
		// TRADITIONAL ARABIC SEASONAL FLAGS
		// =============================================================================

		// Al-Kharif (Autumn Cycle)
		MONTHS.put("al-kharif", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("kharif", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("الخريف", CalendarSeason.SEASON_AUTUMN);

		// Al-Shita (Winter Cycle)
		MONTHS.put("al-shita", CalendarSeason.SEASON_WINTER);
		MONTHS.put("shita", CalendarSeason.SEASON_WINTER);
		MONTHS.put("al-shitaa", CalendarSeason.SEASON_WINTER);
		MONTHS.put("الشتاء", CalendarSeason.SEASON_WINTER);

		// Al-Rabi (Spring Cycle)
		MONTHS.put("al-rabi", CalendarSeason.SEASON_SPRING);
		MONTHS.put("rabi", CalendarSeason.SEASON_SPRING);
		MONTHS.put("الربيع", CalendarSeason.SEASON_SPRING);

		// Al-Sayf / Al-Qayz (Summer Cycle / Extreme Heat Node)
		MONTHS.put("al-sayf", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("sayf", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("الصيف", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("qayz", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("al-qayz", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("القيظ", CalendarSeason.SEASON_SUMMER);
	}

	// Adaptive pattern supporting both standalone seasonal notations ("Al-Sayf 1420")
	// and explicit three-token expressions ("1 Ramadan 1420")
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([a-zA-Z0-7أ-ي\\s-]+?)\\s+(\\d+)");


	private IslamicCalendarParser(){}


	/**
	 * Parses a raw Islamic date string text expression into a validated structural DTO.
	 *
	 * @param rawDate the string representing the Islamic date
	 * @return a configured IslamicDateInput instance
	 * @throws IllegalArgumentException if the expression violates text matching layout criteria
	 */
	public static IslamicDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new IslamicDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Islamic date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Islamic month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Islamic lunar months cannot exceed 30 days.");

		return new IslamicDateInput(year, month, day);
	}

	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);
			if(c >= '۰' && c <= '۹')
				// Persian
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				// Arabic-Indic
				sb.append((char)('0' + c - '٠'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
