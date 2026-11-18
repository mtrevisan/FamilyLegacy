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
package io.github.mtrevisan.calendars.persian;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.text.ScriptText;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Parser for Persian (Solar Hijri) calendar dates.
 * <p>
 * Supported examples:
 * - ۲۲ بهمن ۱۳۵۷
 * - 22 Bahman 1357
 * - 1 Farvardin 1300
 * - ۱ فروردین ۱۳۰۰
 */
public final class PersianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Farvardin (Aries / Al-Hamal) - Commences at the Vernal Equinox
		register("فروردین", 1);           // Native Persian script
		register("Фарвардин", 1);         // Tajik Cyrillic script
		register("farvardin", 1);         // Standard Latin transliteration
		register("hamal", 1);             // Afghan Dari zodiac name (حمل)
		register("حمل", 1);                // Afghan Arabic script

		// Month 2: Ordibehesht (Taurus / Ath-Thawr)
		register("اردیبهشت", 2);
		register("Ордибеҳишт", 2);          // Tajik Cyrillic variant
		register("ordibehesht", 2);
		register("ordibehešt", 2);        // Scientific linguistic notation
		register("thawr", 2);             // Afghan Dari zodiac name (ثور)
		register("ثور", 2);
		register("sawr", 2);              // Phonetic Afghan variant

		// Month 3: Khordad (Gemini / Al-Jauza)
		register("خرداد", 3);
		register("Хурдод", 3);            // Tajik Cyrillic script
		register("khordad", 3);
		register("xordad", 3);            // Simplified common ASCII notation
		register("jauza", 3);             // Afghan Dari zodiac name (جوزا)
		register("جوزا", 3);
		register("jawza", 3);

		// Month 4: Tir (Cancer / As-Saratan) - Commences at the Estival Solstice
		register("تیر", 4);
		register("Тир", 4);               // Tajik Cyrillic script
		register("tir", 4);
		register("teer", 4);              // Common phonetic variant
		register("saratan", 4);           // Afghan Dari zodiac name (سرطان)
		register("سرطان", 4);

		// Month 5: Mordad / Amardad (Leo / Al-Asad)
		register("مرداد", 5);              // Colloquial/Secular Iranian script
		register("امرداد", 5);             // Purified philological Persian script
		register("Мурдод", 5);            // Tajik Cyrillic script
		register("mordad", 5);
		register("amardad", 5);           // Historical etymological variant
		register("asad", 5);              // Afghan Dari zodiac name (اسد)
		register("اسد", 5);

		// Month 6: Shahrivar (Virgo / As-Sunbula)
		register("شهریور", 6);
		register("Шаҳривар", 6);          // Tajik Cyrillic script
		register("shahrivar", 6);
		register("shahrewar", 6);         // Archaic text alignment variant
		register("sunbula", 6);           // Afghan Dari zodiac name (سنبله)
		register("سنبله", 6);

		// Month 7: Mehr (Libra / Al-Mizan) - Commences at the Autumnal Equinox
		register("مهر", 7);
		register("Меҳр", 7);              // Tajik Cyrillic script
		register("mehr", 7);
		register("mihr", 7);              // Pahlavi/Scientific short transliteration
		register("mizan", 7);             // Afghan Dari zodiac name (میزان)
		register("میزان", 7);

		// Month 8: Aban (Scorpio / Al-Aqrab)
		register("آبان", 8);
		register("Обон", 8);              // Tajik Cyrillic script
		register("aban", 8);
		register("aqrab", 8);             // Afghan Dari zodiac name (عقرب)
		register("عقرب", 8);

		// Month 9: Azar (Sagittarius / Al-Qaws)
		register("آذر", 9);
		register("Озар", 9);                // Tajik Cyrillic script
		register("azar", 9);
		register("qaws", 9);              // Afghan Dari zodiac name (قوس)
		register("قوس", 9);
		register("ghows", 9);             // Phonetic variant

		// Month 10: Dey (Capricorn / Al-Jady) - Commences at the Hibernal Solstice
		register("دی", 10);
		register("Дэй", 10);              // Tajik Cyrillic script
		register("dey", 10);
		register("day", 10);              // Common alternative spelling
		register("dei", 10);              // French/European notation variant
		register("jady", 10);             // Afghan Dari zodiac name (جدی)
		register("جدی", 10);
		register("jadi", 10);

		// Month 11: Bahman (Aquarius / Ad-Dalw)
		register("بهمن", 11);
		register("Баҳман", 11);           // Tajik Cyrillic script
		register("bahman", 11);
		register("dalw", 11);             // Afghan Dari zodiac name (دلو)
		register("دلو", 11);

		// Month 12: Esfand (Pisces / Al-Hut)
		register("اسفند", 12);
		register("Эсфанд", 12);           // Tajik Cyrillic script
		register("esfand", 12);
		register("asfand", 12);           // Classic vocalization spelling
		register("hut", 12);              // Afghan Dari zodiac name (حوت)
		register("حوت", 12);
		register("hoot", 12);             // Phonetic variant


		// Autumn
		register("paeez", CalendarSeason.SEASON_AUTUMN);
		register("payiz", CalendarSeason.SEASON_AUTUMN);
		register("پاییز", CalendarSeason.SEASON_AUTUMN);
		register("Хазон", CalendarSeason.SEASON_AUTUMN);         // Tajik Cyrillic word for Autumn
		register("khazon", CalendarSeason.SEASON_AUTUMN);

		// Winter
		register("zemestan", CalendarSeason.SEASON_WINTER);
		register("زمستان", CalendarSeason.SEASON_WINTER);
		register("Зимистон", CalendarSeason.SEASON_WINTER);      // Tajik Cyrillic word for Winter
		register("zimiston", CalendarSeason.SEASON_WINTER);

		// Spring
		register("bahar", CalendarSeason.SEASON_SPRING);
		register("بهار", CalendarSeason.SEASON_SPRING);
		register("Баҳор", CalendarSeason.SEASON_SPRING);         // Tajik Cyrillic word for Spring
		register("bahor", CalendarSeason.SEASON_SPRING);

		// Summer
		register("tabestan", CalendarSeason.SEASON_SUMMER);
		register("تابستان", CalendarSeason.SEASON_SUMMER);
		register("Тобистон", CalendarSeason.SEASON_SUMMER);      // Tajik Cyrillic variant
		register("توبستان", CalendarSeason.SEASON_SUMMER);
		register("toviston", CalendarSeason.SEASON_SUMMER);
	}

	private static void register(final String name, final int month){
		ScriptText.register(MONTHS, name, month);
	}


	private PersianCalendarParser(){}


	/**
	 * @return the normalized names of the months and of the seasons known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(MONTHS.keySet());
	}

	/**
	 * Parses a raw Persian text string into a validated structural model.
	 *
	 * @param rawDate the string representing the Persian date
	 * @return the components
	 * @throws IllegalArgumentException if the expression layout is invalid, or a season is given with a day
	 */
	public static YearMonthDay parse(final String rawDate){
		final ScriptText.Parsed parsed = ScriptText.parse(rawDate, "Persian", MONTHS, false);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = parsed.dayOrZero();

		// the actual length of Esfand depends on the year, and is checked by the calendar
		final int month = parsed.month();
		final int maxDay = (month >= 1 && month <= 6? 31: 30);
		if(parsed.day() != null && (day < 1 || day > maxDay))
			throw new IllegalArgumentException("The Persian month " + month + " has at most " + maxDay + " days: " + rawDate);

		return new YearMonthDay(parsed.year(), month, day);
	}

}
