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

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.text.ScriptText;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Parser for Zoroastrian Parsi (Yezdegirdi) calendar dates.
 * <p>
 * Supported examples:
 * - 1 Fravardin 1390
 * - ۲۲ فروردین ۱۳۹۰
 * - 3 Gatha 1385
 * - 1 Fravardin 1390 Kadmi
 * - 6 Gatha 1385 Fasli
 * </p>
 * <p>
 * The reckoning (Shahenshahi, Kadmi, or Fasli) can follow the year; it defaults to Shahenshahi.
 * </p>
 */
public final class ParsiCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Fravardin / Frawardin
		register("fravardin", 1);
		register("frawardin", 1);		// Middle Persian / Pahlavi transliteration
		register("farvardin", 1);		// Modern Iranian Solar Pahlavi variant
		register("فروردین", 1);
		register("ફરવરદીન", 1);

		// Month 2: Ardibehest / Ardwahisht
		register("ardibehest", 2);
		register("ardibahesht", 2);
		register("ardwahisht", 2);		// Classical Avestan/Pahlavi linguistic root
		register("اردیبهشت", 2);
		register("અર્દિબહેશ્ત", 2);

		// Month 3: Khordad / Horvadat
		register("khordad", 3);
		register("xordad", 3);			// Scientific Latin notation variant
		register("خرداد", 3);
		register("ખોરદાદ", 3);

		// Month 4: Tir / Tishtrya
		register("tir", 4);
		register("tishtrya", 4);			// Sacred Avestan star-deity name
		register("تیر", 4);
		register("તીર", 4);

		// Month 5: Amardad / Ameretat (The Immortal Cycle)
		register("amardad", 5);
		register("ameretat", 5);			// Exact theological Avestan form
		register("امرداد", 5);			// Strict traditional Zoroastrian Persian spelling
		register("مرداد", 5);				// Modern secular Iranian variant (often parsed)
		register("અમરદાદ", 5);

		// Month 6: Sharivar / Shahrewar
		register("sharivar", 6);
		register("shahrivar", 6);
		register("shahrewar", 6);		// Middle Persian linguistic spelling
		register("شهریور", 6);
		register("શહરીવર", 6);

		// Month 7: Meheer / Mithra (The Equinox Turning Point)
		register("meher", 7);
		register("mihr", 7);				// Pahlavi standard short notation
		register("mehar", 7);
		register("مهر", 7);
		register("મેહર", 7);

		// Month 8: Ava / Aban
		register("ava", 8);
		register("avan", 8);				// Colloquial Parsi Gujarati suffix format
		register("aban", 8);				// Iranian continental alternative
		register("آبان", 8);
		register("આવાં", 8);

		// Month 9: Adar / Atar (The Sacred Fire)
		register("adar", 9);
		register("azar", 9);				// Modern Persian phonetic variation
		register("atar", 9);				// Pure Avestan fire stem
		register("آذر", 9);
		register("આદર", 9);

		// Month 10: Dae / Dadvah
		register("dae", 10);
		register("dai", 10);
		register("day", 10);
		register("دی", 10);
		register("દએ", 10);

		// Month 11: Bahman / Vohuman
		register("bahman", 11);
		register("vohuman", 11);			// Pahlavi ethical concept term ("Good Mind")
		register("بهمن", 11);
		register("બહમન", 11);

		// Month 12: Aspandarmad / Spandarmad
		register("aspandarmad", 12);
		register("aspandarmard", 12);
		register("spandarmad", 12);			// Middle Persian standard entry
		register("esfand", 12);				// Simplified modern Iranian solar variant
		register("اسفند", 12);
		register("અસ્પંદારમદ", 12);

		// Month 13: Intercalary Period (The 5 Holy Gatha Days / Muktad Period)
		register("gatha", 13);
		register("panje", 13);
		register("panjah", 13);
		register("andargah", 13);			// Historical Persian astrological name
		register("پنجه", 13);
		register("ગાથા", 13);
		register("મુક્તાદ", 13);					// Parsi Gujarati colloquial term for festival days (Muktad)


		// --- Autumn Cycle (Padiz / Patiz) ---
		register("padiz", CalendarSeason.SEASON_AUTUMN);
		register("patiz", CalendarSeason.SEASON_AUTUMN);
		register("autumn", CalendarSeason.SEASON_AUTUMN);
		register("ayathrem", CalendarSeason.SEASON_AUTUMN);
		register("paitishahya", CalendarSeason.SEASON_AUTUMN);

		// --- Winter Cycle (Zayan / Vayan) ---
		register("zayan", CalendarSeason.SEASON_WINTER);
		register("vayan", CalendarSeason.SEASON_WINTER);
		register("winter", CalendarSeason.SEASON_WINTER);
		register("maidhyairya", CalendarSeason.SEASON_WINTER);

		// --- Spring Cycle (Vahar / Wahar) ---
		register("vahar", CalendarSeason.SEASON_SPRING);
		register("wahar", CalendarSeason.SEASON_SPRING);
		register("spring", CalendarSeason.SEASON_SPRING);
		register("maidyozarem", CalendarSeason.SEASON_SPRING);
		register("hamaspathmaedaya", CalendarSeason.SEASON_SPRING);	// Year-end/Pre-Equinox transitional festival

		// --- Summer Cycle (Hamin) ---
		register("hamin", CalendarSeason.SEASON_SUMMER);
		register("summer", CalendarSeason.SEASON_SUMMER);
		register("maidhyoshema", CalendarSeason.SEASON_SUMMER);	// Mid-Summer Solstice harvest festival token
	}

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

		// Fasli Variant Setup (Seasonal calendar, the year starting on 21 March)
		VARIANTS.put("fasli", ParsiVariant.FASLI);
		VARIANTS.put("fasili", ParsiVariant.FASLI);
		VARIANTS.put("bastani", ParsiVariant.FASLI);
	}

	private static void register(final String name, final int month){
		ScriptText.register(MONTHS, name, month);
	}


	private ParsiCalendarParser(){}


	/**
	 * @return the normalized names of the months and of the seasons known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(MONTHS.keySet());
	}

	/**
	 * Parses a raw Parsi text string into a structured model.
	 *
	 * @param rawDate the string representing the Parsi date
	 * @return a container with year, month, and day fields
	 * @throws IllegalArgumentException if the expression layout is invalid, or a season is given with a day
	 */
	public static ParsiDateInput parse(final String rawDate){
		//the zero-width non-joiner, optional inside the Persian words (e.g. "اردی‌بهشت"), is removed by the normalization
		final ScriptText.Parsed match = ScriptText.parse(rawDate, "Parsi", MONTHS, true);
		final int month = match.month();

		// Without an explicit reckoning, default to the Shahenshahi one, followed by most Parsis in India
		final ParsiVariant variant = (match.suffix() != null
			? VARIANTS.get(match.suffix())
			: ParsiVariant.SHAHENSHAHI);
		if(variant == null)
			throw new IllegalArgumentException("Unknown Parsi reckoning: " + match.suffix());

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = match.dayOrZero();

		// Validation boundaries
		if(match.day() != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Parsi months must have exactly 30 days.");
		// The sixth Gatha day (Avardad-sal-Gah) of the Fasli leap years is checked by the calendar
		final int gathaDays = (variant == ParsiVariant.FASLI? 6: 5);
		if(match.day() != null && month == 13 && (day < 1 || day > gathaDays))
			throw new IllegalArgumentException("Gatha intercalary period must be between 1 and " + gathaDays + " days.");

		return new ParsiDateInput(match.year(), month, day, variant);
	}

}
