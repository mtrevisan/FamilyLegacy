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
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.text.ScriptText;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Parser for Islamic (Umm al-Qura) calendar date expressions using exact astronomical mapping flags.
 * Supports canonical Hijri months and traditional lunar-drift seasonal markers.
 */
public final class IslamicCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Muharram (Sacred Month / New Year)
		register("محرم", 1);
		register("muharram", 1);
		register("moharram", 1);
		register("muharram al-haram", 1);	// Full formal ceremonial name

		// Month 2: Safar
		register("صفر", 2);
		register("safar", 2);
		register("saphar", 2);

		// Month 3: Rabi al-Awwal (The First Rabi)
		register("ربيع الأول", 3);
		register("rabi al-awwal", 3);
		register("rabi al-awal", 3);
		register("rabiul-awwal", 3);		// Combined phonetic variant
		register("rabiulawal", 3);			// Highly common Southeast Asian spelling
		register("rabi i", 3);				// Roman numeral standard shortcut

		// Month 4: Rabi al-Thani / Rabi al-Akhir (The Second/Last Rabi)
		register("ربيع الثاني", 4);
		register("ربيع الآخر", 4);			// Alternative native script variant
		register("rabi al-thani", 4);
		register("rabi al-akhir", 4);		// Critical missing alternative name
		register("rabi al-akhar", 4);
		register("rabiul-thani", 4);
		register("rabiul-akhir", 4);
		register("rabiulakhir", 4);			// Highly common variant
		register("rabi ii", 4);				// Roman numeral standard shortcut

		// Month 5: Jumada al-Awwal (The First Jumada)
		register("جمادى الأولى", 5);
		register("جمادى الأول", 5);
		register("jumada al-awwal", 5);
		register("jumada al-awal", 5);
		register("jumada i", 5);
		register("jumadil-awwal", 5);
		register("jumadilawal", 5);

		// Month 6: Jumada al-Thani / Jumada al-Akhirah (The Second/Last Jumada)
		register("جمادى الآخرة", 6);
		register("جمادى الثاني", 6);
		register("jumada al-thani", 6);
		register("jumada al-akhirah", 6);	// Critical missing alternative name
		register("jumada al-akhir", 6);
		register("jumada ii", 6);
		register("jumadil-thani", 6);
		register("jumadil-akhir", 6);
		register("jumadilakhir", 6);

		// Month 7: Rajab (Sacred Month)
		register("رجب", 7);
		register("rajab", 7);
		register("rajab al-murajjab", 7);	// Full ceremonial name

		// Month 8: Shaban
		register("شعبان", 8);
		register("shaban", 8);
		register("sha'ban", 8);				// Transliteration with explicit Ayn stop
		register("shaaban", 8);

		// Month 9: Ramadan (The Month of Fasting)
		register("رمضان", 9);
		register("ramadan", 9);
		register("ramadhan", 9);				// Highly common Southeast Asian/South Asian variant
		register("ramazan", 9);				// Turkish, Persian, and Urdu standard spelling

		// Month 10: Shawwal
		register("شوال", 10);
		register("shawwal", 10);
		register("shawal", 10);

		// Month 11: Dhu al-Qadah (Sacred Month)
		register("ذو القعدة", 11);
		register("dhu al-qadah", 11);
		register("dhu'l-qa'dah", 11);
		register("dhul-qadah", 11);
		register("zulqadah", 11);			// Common phonetic variant (Dhu -> Z)
		register("dhu al-qi'dah", 11);

		// Month 12: Dhu al-Hijjah (Sacred Month / Month of Hajj)
		register("ذو الحجة", 12);
		register("dhu al-hijjah", 12);
		register("dhu'l-hijjah", 12);
		register("dhul-hijjah", 12);
		register("zulhijjah", 12);			// Highly common colloquial variant
		register("dhu al-hajjah", 12);


		// =============================================================================
		// TRADITIONAL ARABIC SEASONAL FLAGS
		// =============================================================================

		// Al-Kharif (Autumn Cycle)
		register("al-kharif", CalendarSeason.SEASON_AUTUMN);
		register("kharif", CalendarSeason.SEASON_AUTUMN);
		register("الخريف", CalendarSeason.SEASON_AUTUMN);

		// Al-Shita (Winter Cycle)
		register("al-shita", CalendarSeason.SEASON_WINTER);
		register("shita", CalendarSeason.SEASON_WINTER);
		register("al-shitaa", CalendarSeason.SEASON_WINTER);
		register("الشتاء", CalendarSeason.SEASON_WINTER);

		// Al-Rabi (Spring Cycle)
		register("al-rabi", CalendarSeason.SEASON_SPRING);
		register("rabi", CalendarSeason.SEASON_SPRING);
		register("الربيع", CalendarSeason.SEASON_SPRING);

		// Al-Sayf / Al-Qayz (Summer Cycle / Extreme Heat Node)
		register("al-sayf", CalendarSeason.SEASON_SUMMER);
		register("sayf", CalendarSeason.SEASON_SUMMER);
		register("الصيف", CalendarSeason.SEASON_SUMMER);
		register("qayz", CalendarSeason.SEASON_SUMMER);
		register("al-qayz", CalendarSeason.SEASON_SUMMER);
		register("القيظ", CalendarSeason.SEASON_SUMMER);
	}

	private static void register(final String name, final int month){
		ScriptText.register(MONTHS, name, month);
	}


	private IslamicCalendarParser(){}


	/**
	 * @return the normalized names of the months and of the seasons known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(MONTHS.keySet());
	}

	/**
	 * Parses a raw Islamic date string text expression into a validated structural DTO.
	 *
	 * @param rawDate the string representing the Islamic date ("[day] month year", or "season year", or a year)
	 * @return the components
	 * @throws IllegalArgumentException if the expression violates text matching layout criteria, or a season is given with a day
	 */
	public static YearMonthDay parse(final String rawDate){
		final ScriptText.Parsed parsed = ScriptText.parse(rawDate, "Islamic", MONTHS, false);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = parsed.dayOrZero();
		if(parsed.day() != null && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Islamic lunar months cannot exceed 30 days.");

		return new YearMonthDay(parsed.year(), parsed.month(), day);
	}

}
