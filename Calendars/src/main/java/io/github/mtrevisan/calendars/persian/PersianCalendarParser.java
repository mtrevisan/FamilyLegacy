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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
		MONTHS.put("فروردین", 1);           // Native Persian script
		MONTHS.put("Фарвардин", 1);         // Tajik Cyrillic script
		MONTHS.put("farvardin", 1);         // Standard Latin transliteration
		MONTHS.put("hamal", 1);             // Afghan Dari zodiac name (حمل)
		MONTHS.put("حمل", 1);                // Afghan Arabic script

		// Month 2: Ordibehesht (Taurus / Ath-Thawr)
		MONTHS.put("اردیبهشت", 2);
		MONTHS.put("Орدیбеҳишт", 2);        // Tajik Cyrillic variant
		MONTHS.put("ordibehesht", 2);
		MONTHS.put("ordibehešt", 2);        // Scientific linguistic notation
		MONTHS.put("thawr", 2);             // Afghan Dari zodiac name (ثور)
		MONTHS.put("ثور", 2);
		MONTHS.put("sawr", 2);              // Phonetic Afghan variant

		// Month 3: Khordad (Gemini / Al-Jauza)
		MONTHS.put("خرداد", 3);
		MONTHS.put("Хурдод", 3);            // Tajik Cyrillic script
		MONTHS.put("khordad", 3);
		MONTHS.put("xordad", 3);            // Simplified common ASCII notation
		MONTHS.put("jauza", 3);             // Afghan Dari zodiac name (جوزا)
		MONTHS.put("جوزا", 3);
		MONTHS.put("jawza", 3);

		// Month 4: Tir (Cancer / As-Saratan) - Commences at the Estival Solstice
		MONTHS.put("تیر", 4);
		MONTHS.put("Тир", 4);               // Tajik Cyrillic script
		MONTHS.put("tir", 4);
		MONTHS.put("teer", 4);              // Common phonetic variant
		MONTHS.put("saratan", 4);           // Afghan Dari zodiac name (سرطان)
		MONTHS.put("سرطان", 4);

		// Month 5: Mordad / Amardad (Leo / Al-Asad)
		MONTHS.put("مرداد", 5);              // Colloquial/Secular Iranian script
		MONTHS.put("امرداد", 5);             // Purified philological Persian script
		MONTHS.put("Мурдод", 5);            // Tajik Cyrillic script
		MONTHS.put("mordad", 5);
		MONTHS.put("amardad", 5);           // Historical etymological variant
		MONTHS.put("asad", 5);              // Afghan Dari zodiac name (اسد)
		MONTHS.put("اسد", 5);

		// Month 6: Shahrivar (Virgo / As-Sunbula)
		MONTHS.put("شهریور", 6);
		MONTHS.put("Шаҳривар", 6);          // Tajik Cyrillic script
		MONTHS.put("shahrivar", 6);
		MONTHS.put("shahrewar", 6);         // Archaic text alignment variant
		MONTHS.put("sunbula", 6);           // Afghan Dari zodiac name (سنبله)
		MONTHS.put("سنبله", 6);

		// Month 7: Mehr (Libra / Al-Mizan) - Commences at the Autumnal Equinox
		MONTHS.put("مهر", 7);
		MONTHS.put("Меҳр", 7);              // Tajik Cyrillic script
		MONTHS.put("mehr", 7);
		MONTHS.put("mihr", 7);              // Pahlavi/Scientific short transliteration
		MONTHS.put("mizan", 7);             // Afghan Dari zodiac name (میزان)
		MONTHS.put("میزان", 7);

		// Month 8: Aban (Scorpio / Al-Aqrab)
		MONTHS.put("آبان", 8);
		MONTHS.put("Обон", 8);              // Tajik Cyrillic script
		MONTHS.put("aban", 8);
		MONTHS.put("aqrab", 8);             // Afghan Dari zodiac name (عقرب)
		MONTHS.put("عقرب", 8);

		// Month 9: Azar (Sagittarius / Al-Qaws)
		MONTHS.put("آذر", 9);
		MONTHS.put("Оذر", 9);               // Tajik Cyrillic script
		MONTHS.put("azar", 9);
		MONTHS.put("qaws", 9);              // Afghan Dari zodiac name (قوس)
		MONTHS.put("قوس", 9);
		MONTHS.put("ghows", 9);             // Phonetic variant

		// Month 10: Dey (Capricorn / Al-Jady) - Commences at the Hibernal Solstice
		MONTHS.put("دی", 10);
		MONTHS.put("Дэй", 10);              // Tajik Cyrillic script
		MONTHS.put("dey", 10);
		MONTHS.put("day", 10);              // Common alternative spelling
		MONTHS.put("dei", 10);              // French/European notation variant
		MONTHS.put("jady", 10);             // Afghan Dari zodiac name (جدی)
		MONTHS.put("جدی", 10);
		MONTHS.put("jadi", 10);

		// Month 11: Bahman (Aquarius / Ad-Dalw)
		MONTHS.put("بهمن", 11);
		MONTHS.put("Баҳման", 11);           // Tajik Cyrillic script
		MONTHS.put("bahman", 11);
		MONTHS.put("dalw", 11);             // Afghan Dari zodiac name (دلو)
		MONTHS.put("دلو", 11);

		// Month 12: Esfand (Pisces / Al-Hut)
		MONTHS.put("اسفند", 12);
		MONTHS.put("Эсфанд", 12);           // Tajik Cyrillic script
		MONTHS.put("esfand", 12);
		MONTHS.put("asfand", 12);           // Classic vocalization spelling
		MONTHS.put("hut", 12);              // Afghan Dari zodiac name (حوت)
		MONTHS.put("حوت", 12);
		MONTHS.put("hoot", 12);             // Phonetic variant


		// Autumn
		MONTHS.put("paeez", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("payiz", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("پاییز", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("Хазон", CalendarSeason.SEASON_AUTUMN);         // Tajik Cyrillic word for Autumn
		MONTHS.put("khazon", CalendarSeason.SEASON_AUTUMN);

		// Winter
		MONTHS.put("zemestan", CalendarSeason.SEASON_WINTER);
		MONTHS.put("زمستان", CalendarSeason.SEASON_WINTER);
		MONTHS.put("Зимистон", CalendarSeason.SEASON_WINTER);      // Tajik Cyrillic word for Winter
		MONTHS.put("zimiston", CalendarSeason.SEASON_WINTER);

		// Spring
		MONTHS.put("bahar", CalendarSeason.SEASON_SPRING);
		MONTHS.put("بهار", CalendarSeason.SEASON_SPRING);
		MONTHS.put("Баҳор", CalendarSeason.SEASON_SPRING);         // Tajik Cyrillic word for Spring
		MONTHS.put("bahor", CalendarSeason.SEASON_SPRING);

		// Summer
		MONTHS.put("tabestan", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("تابستان", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("Тоβистон", CalendarSeason.SEASON_SUMMER);      // Tajik Cyrillic variant
		MONTHS.put("توبستان", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("toviston", CalendarSeason.SEASON_SUMMER);
	}

	// Adaptive pattern supporting both standalone seasonal notations ("Bahar 1357")
	// and explicit three-token expressions ("22 Bahman 1357")
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([a-zA-Zآ-ی\\s-]+?)\\s+(\\d+)");


	private PersianCalendarParser(){}


	/**
	 * Parses a raw Persian text string into a validated structural model.
	 *
	 * @param rawDate the string representing the Persian date
	 * @return a configured PersianDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static PersianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate));

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new PersianDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Persian date: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Persian month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && month <= 6 && (day < 1 || day > 31))
			throw new IllegalArgumentException("The first six Persian months consist strictly of 31 days.");
		if(dayGroup != null && month >= 7 && month <= 11 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Persian months 7 through 11 consist strictly of 30 days.");
		if(dayGroup != null && month == 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("The final Persian month cannot exceed 30 days.");

		return new PersianDateInput(year, month, day);
	}

	/**
	 * Converts Persian and Arabic-Indic digits to ASCII digits.
	 */
	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);

			if(c >= '۰' && c <= '۹')
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				sb.append((char)('0' + c - '٠'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
