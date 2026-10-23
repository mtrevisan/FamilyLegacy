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
package io.github.mtrevisan.calendars.parsi;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Zoroastrian Parsi (Yezdegirdi) calendar dates.
 * <p>
 * Supported examples:
 * - 1 Fravardin 1390
 * - ۲۲ فروردین ۱۳۹۰
 * - 3 Gatha 1385
 * - 1 Fravardin 1390 Kadmi
 * </p>
 * <p>
 * The reckoning (Shahenshahi or Kadmi) can follow the year; it defaults to Shahenshahi.
 * </p>
 */
// TODO FASLI calendar?
public final class ParsiCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Fravardin / Frawardin
		MONTHS.put("fravardin", 1);
		MONTHS.put("frawardin", 1);          // Middle Persian / Pahlavi transliteration
		MONTHS.put("farvardin", 1);         // Modern Iranian Solar Pahlavi variant
		MONTHS.put("فروردین", 1);
		MONTHS.put("ફરવરદીન", 1);

		// Month 2: Ardibehest / Ardwahisht
		MONTHS.put("ardibehest", 2);
		MONTHS.put("ardibahesht", 2);
		MONTHS.put("ardwahisht", 2);        // Classical Avestan/Pahlavi linguistic root
		MONTHS.put("اردیبهشت", 2);
		MONTHS.put("અર્દિբહેશ્ત", 2);

		// Month 3: Khordad / Horvadat
		MONTHS.put("khordad", 3);
		MONTHS.put("xordad", 3);            // Scientific Latin notation variant
		MONTHS.put("khordad", 3);
		MONTHS.put("خرداد", 3);
		MONTHS.put("ขੋਰદાદ", 3);
		MONTHS.put("ખોરદાદ", 3);            // Native Gujarati character correction

		// Month 4: Tir / Tishtrya
		MONTHS.put("tir", 4);
		MONTHS.put("tishtrya", 4);          // Sacred Avestan star-deity name
		MONTHS.put("تیر", 4);
		MONTHS.put("તીર", 4);

		// Month 5: Amardad / Ameretat (The Immortal Cycle)
		MONTHS.put("amardad", 5);
		MONTHS.put("ameretat", 5);          // Exact theological Avestan form
		MONTHS.put("امرداد", 5);             // Strict traditional Zoroastrian Persian spelling
		MONTHS.put("مرداد", 5);              // Modern secular Iranian variant (often parsed)
		MONTHS.put("અમרદાદ", 5);

		// Month 6: Sharivar / Shahrewar
		MONTHS.put("sharivar", 6);
		MONTHS.put("shahrivar", 6);
		MONTHS.put("shahrewar", 6);         // Middle Persian linguistic spelling
		MONTHS.put("شهریور", 6);
		MONTHS.put("શહરીવર", 6);

		// Month 7: Meheer / Mithra (The Equinox Turning Point)
		MONTHS.put("meher", 7);
		MONTHS.put("mihr", 7);              // Pahlavi standard short notation
		MONTHS.put("mehar", 7);
		MONTHS.put("مهر", 7);
		MONTHS.put("મેહર", 7);

		// Month 8: Ava / Aban
		MONTHS.put("ava", 8);
		MONTHS.put("avan", 8);              // Colloquial Parsi Gujarati suffix format
		MONTHS.put("aban", 8);              // Iranian continental alternative
		MONTHS.put("آبان", 8);
		MONTHS.put("આવાં", 8);

		// Month 9: Adar / Atar (The Sacred Fire)
		MONTHS.put("adar", 9);
		MONTHS.put("azar", 9);              // Modern Persian phonetic variation
		MONTHS.put("atar", 9);              // Pure Avestan fire stem
		MONTHS.put("آذر", 9);
		MONTHS.put("આદર", 9);

		// Month 10: Dae / Dadvah
		MONTHS.put("dae", 10);
		MONTHS.put("dai", 10);
		MONTHS.put("day", 10);
		MONTHS.put("دی", 10);
		MONTHS.put("દએ", 10);

		// Month 11: Bahman / Vohuman
		MONTHS.put("bahman", 11);
		MONTHS.put("vohuman", 11);          // Pahlavi ethical concept term ("Good Mind")
		MONTHS.put("بهمن", 11);
		MONTHS.put("બહમન", 11);

		// Month 12: Aspandarmad / Spandarmad
		MONTHS.put("aspandarmad", 12);
		MONTHS.put("aspandarmard", 12);
		MONTHS.put("spandarmad", 12);       // Middle Persian standard entry
		MONTHS.put("esfand", 12);           // Simplified modern Iranian solar variant
		MONTHS.put("اسفند", 12);
		MONTHS.put("અસ્પંદારܡદ", 12);

		// Month 13: Intercalary Period (The 5 Holy Gatha Days / Muktad Period)
		MONTHS.put("gatha", 13);
		MONTHS.put("panje", 13);
		MONTHS.put("panjah", 13);
		MONTHS.put("andargah", 13);         // Historical Persian astrological name
		MONTHS.put("پنجه", 13);
		MONTHS.put("ગાથા", 13);
		MONTHS.put("મુક્તાદ", 13);          // Parsi Gujarati colloquial term for festival days (Muktad)


		// --- Autumn Cycle (Padiz / Patiz) ---
		MONTHS.put("padiz", CalendarType.SEASON_AUTUMN);
		MONTHS.put("patiz", CalendarType.SEASON_AUTUMN);
		MONTHS.put("autumn", CalendarType.SEASON_AUTUMN);
		MONTHS.put("ayathrem", CalendarType.SEASON_AUTUMN);       // CORRECTED: Autumn home-coming harvest feast
		MONTHS.put("paitishahya", CalendarType.SEASON_AUTUMN);    // CORRECTED: Late Summer / Early Autumn harvest feast

		// --- Winter Cycle (Zayan / Vayan) ---
		MONTHS.put("zayan", CalendarType.SEASON_WINTER);
		MONTHS.put("vayan", CalendarType.SEASON_WINTER);
		MONTHS.put("winter", CalendarType.SEASON_WINTER);
		MONTHS.put("maidhyairya", CalendarType.SEASON_WINTER);    // CORRECTED: Mid-Winter Solstice Gahambar flag

		// --- Spring Cycle (Vahar / Wahar) ---
		MONTHS.put("vahar", CalendarType.SEASON_SPRING);
		MONTHS.put("wahar", CalendarType.SEASON_SPRING);
		MONTHS.put("spring", CalendarType.SEASON_SPRING);
		MONTHS.put("maidyozarem", CalendarType.SEASON_SPRING);    // CORRECTED: Mid-Spring sowing festival token
		MONTHS.put("hamaspathmaedaya", CalendarType.SEASON_SPRING); // Year-end/Pre-Equinox transitional festival

		// --- Summer Cycle (Hamin) ---
		MONTHS.put("hamin", CalendarType.SEASON_SUMMER);
		MONTHS.put("summer", CalendarType.SEASON_SUMMER);
		MONTHS.put("maidhyoshema", CalendarType.SEASON_SUMMER);   // Mid-Summer Solstice harvest festival token
	}

	private static final String ZERO_WIDTH_NON_JOINER = "‌";

	private static final Map<String, ParsiVariant> VARIANTS = new HashMap<>();
	static{
		// Shahenshahi Variant Setup (Mobile traditional calendar counting from Yazdegerd III era)
		VARIANTS.put("shahenshahi", ParsiVariant.SHAHENSHAHI);
		VARIANTS.put("shahanshahi", ParsiVariant.SHAHENSHAHI);
		VARIANTS.put("shehenshahi", ParsiVariant.SHAHENSHAHI);
		VARIANTS.put("shenshai", ParsiVariant.SHAHENSHAHI);
		VARIANTS.put("shensai", ParsiVariant.SHAHENSHAHI);

		// Kadmi Variant Setup (One month ahead of Shenshai due to historical leap-year differences)
		VARIANTS.put("kadmi", ParsiVariant.KADMI);
		VARIANTS.put("kadimi", ParsiVariant.KADMI);
		VARIANTS.put("qadimi", ParsiVariant.KADMI);
		VARIANTS.put("qadmi", ParsiVariant.KADMI);
	}

	// Adaptive pattern supporting both standalone seasonal notations ("Hamin 1320")
	// and explicit three-token expressions ("1 Fravardin 1390"), with an optional trailing reckoning ("1 Fravardin 1390 Kadmi")
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)(?:\\s+([\\p{L}\\p{M}]+))?");


	private ParsiCalendarParser(){}


	/**
	 * Parses a raw Parsi text string into a structured model.
	 *
	 * @param rawDate the string representing the Parsi date
	 * @return a container with year, month, and day fields
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static ParsiDateInput parse(final String rawDate){
		// The zero-width non-joiner is optional inside Persian words (e.g. "اردی‌بهشت")
		final String normalized = StringUtils.normalizeSpace(convertDigitsToAscii(rawDate)
			.replace(ZERO_WIDTH_NON_JOINER, StringUtils.EMPTY));

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new ParsiDateInput(Integer.parseInt(normalized), 0, 0, ParsiVariant.SHAHENSHAHI);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Parsi date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));
		final String variantName = matcher.group(4);

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Parsi month or seasonal descriptor: " + monthName);

		// Without an explicit reckoning, default to the Shahenshahi one, followed by most Parsis in India
		final ParsiVariant variant = (variantName != null
			? VARIANTS.get(variantName.toLowerCase(Locale.ROOT))
			: ParsiVariant.SHAHENSHAHI);
		if(variant == null)
			throw new IllegalArgumentException("Unknown Parsi reckoning: " + variantName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Validation boundaries
		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Parsi months must have exactly 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Gatha intercalary period must be between 1 and 5 days.");

		return new ParsiDateInput(year, month, day, variant);
	}

	/**
	 * Converts Gujarati, Persian, and Arabic digits to standard ASCII numbers.
	 */
	private static String convertDigitsToAscii(final String text){
		final StringBuilder sb = new StringBuilder(text.length());
		for(int i = 0; i < text.length(); i ++){
			final char c = text.charAt(i);

			if(c >= '۰' && c <= '۹')
				// Persian-Arabic
				sb.append((char)('0' + c - '۰'));
			else if(c >= '٠' && c <= '٩')
				// Indic
				sb.append((char)('0' + c - '٠'));
			else if(c >= '૦' && c <= '૯')
				// Gujarati native numbers
				sb.append((char)('0' + c - '૦'));
			else
				sb.append(c);
		}
		return sb.toString();
	}

}
