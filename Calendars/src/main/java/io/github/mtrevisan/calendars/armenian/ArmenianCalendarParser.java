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
package io.github.mtrevisan.calendars.armenian;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for traditional Armenian calendar date expressions.
 * Supports classical transliterations and explicit intercalary boundaries.
 */
public final class ArmenianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Navasard (Historically starts in August)
		MONTHS.put("նավասարդ", 1);
		MONTHS.put("navasard", 1);
		MONTHS.put("navasardi", 1); // Common genitive case in historical texts
		MONTHS.put("nawasard", 1);  // Classical/Scientific transliteration

		// Month 2: Hori
		MONTHS.put("հոռի", 2);
		MONTHS.put("hori", 2);
		MONTHS.put("horhi", 2);
		MONTHS.put("hoṟi", 2);      // ISO/Scientific transliteration

		// Month 3: Sahmi
		MONTHS.put("սահմի", 3);
		MONTHS.put("sahmi", 3);

		// Month 4: Tre
		MONTHS.put("տրե", 4);
		MONTHS.put("tre", 4);
		MONTHS.put("dré", 4);      // Western Armenian pronunciation variant

		// Month 5: Khaghots
		MONTHS.put("քաղոց", 5);
		MONTHS.put("khaghots", 5);
		MONTHS.put("kałoc", 5);     // ISO 9985 transliteration
		MONTHS.put("xaghotz", 5);    // European phonetic spelling variant
		MONTHS.put("khaghotz", 5);

		// Month 6: Arats
		MONTHS.put("արաց", 6);
		MONTHS.put("arats", 6);
		MONTHS.put("arac", 6);      // Standard scientific transliteration

		// Month 7: Mehekan
		MONTHS.put("մեհեկան", 7);
		MONTHS.put("mehekan", 7);
		MONTHS.put("mehekani", 7);
		MONTHS.put("mehegan", 7);   // Western Armenian dialect variant

		// Month 8: Areg
		MONTHS.put("արեգ", 8);
		MONTHS.put("areg", 8);
		MONTHS.put("arek", 8);      // Western Armenian phonetic variant

		// Month 9: Ahekan
		MONTHS.put("ահեկան", 9);
		MONTHS.put("ahekan", 9);
		MONTHS.put("ahegan", 9);    // Western Armenian dialect variant

		// Month 10: Mareri
		MONTHS.put("մարերի", 10);
		MONTHS.put("mareri", 10);

		// Month 11: Margats
		MONTHS.put("մարգաց", 11);
		MONTHS.put("margats", 11);
		MONTHS.put("margas", 11);
		MONTHS.put("markats", 11);  // Western Armenian phonetic variant

		// Month 12: Hrotits
		MONTHS.put("հրոտից", 12);
		MONTHS.put("hrotits", 12);
		MONTHS.put("hrotic", 12);    // Scientific transliteration
		MONTHS.put("hrotitz", 12);

		// Month 13: Avelyats (Intercalary 5 epagomenal days)
		MONTHS.put("ավելյաց", 13);   // Modern Reformed Armenian orthography
		MONTHS.put("աւելեաց", 13);   // Classical Mesropian orthography
		MONTHS.put("avelyats", 13);
		MONTHS.put("aveliats", 13);
		MONTHS.put("awelyac", 13);   // Classical scientific transliteration
		MONTHS.put("aveleats", 13);

		// Armenian Seasons
		// Autumn
		MONTHS.put("աշուն", CalendarType.SEASON_AUTUMN);      // Nominativo (Ashun)
		MONTHS.put("աշնան", CalendarType.SEASON_AUTUMN);     // Genitive/Locative (Ashnan)
		MONTHS.put("ashun", CalendarType.SEASON_AUTUMN);
		MONTHS.put("ashnan", CalendarType.SEASON_AUTUMN);
		MONTHS.put("ašun", CalendarType.SEASON_AUTUMN);       // ISO 9985 format
		MONTHS.put("asoon", CalendarType.SEASON_AUTUMN);      // English phonetic variant

		// Winter
		MONTHS.put("ձմեռ", CalendarType.SEASON_WINTER);      // Nominative (Dzmer)
		MONTHS.put("ձմռան", CalendarType.SEASON_WINTER);     // Modern contracted genitive (Dzmran)
		MONTHS.put("dzmer", CalendarType.SEASON_WINTER);
		MONTHS.put("dzmeran", CalendarType.SEASON_WINTER);    // Uncontracted/Historical genitive form
		MONTHS.put("dzmran", CalendarType.SEASON_WINTER);
		MONTHS.put("jmer", CalendarType.SEASON_WINTER);       // Western Armenian dialect variant
		MONTHS.put("zmer", CalendarType.SEASON_WINTER);       // Common simple transliteration
		MONTHS.put("ձմեր", CalendarType.SEASON_WINTER);      // Frequent native misspelling variant

		// Spring
		MONTHS.put("գարուն", CalendarType.SEASON_SPRING);    // Nominative (Garun)
		MONTHS.put("գարնան", CalendarType.SEASON_SPRING);    // Genitive/Locative (Garnan)
		MONTHS.put("garun", CalendarType.SEASON_SPRING);
		MONTHS.put("garnan", CalendarType.SEASON_SPRING);
		MONTHS.put("karun", CalendarType.SEASON_SPRING);      // Western Armenian dialect variant
		MONTHS.put("karnan", CalendarType.SEASON_SPRING);     // Western Armenian genitive variant
		MONTHS.put("garoon", CalendarType.SEASON_SPRING);     // English phonetic variant

		// Summer
		MONTHS.put("ամառ", CalendarType.SEASON_SUMMER);      // Nominative (Amar)
		MONTHS.put("ամռան", CalendarType.SEASON_SUMMER);     // Genitive/Locative (Amran)
		MONTHS.put("amar", CalendarType.SEASON_SUMMER);
		MONTHS.put("amran", CalendarType.SEASON_SUMMER);
		MONTHS.put("amaṟ", CalendarType.SEASON_SUMMER);       // Scientific transliteration (trilled R)
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\S+)\\s+(\\d+)");


	private ArmenianCalendarParser(){}


	/**
	 * Parses a raw Armenian text expression into a validated structural container.
	 *
	 * @param rawDate the date string (e.g., "1 Navasard 1400" or "3 Avelyats 1450")
	 * @return a configured ArmenianDateInput instance
	 */
	public static ArmenianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new ArmenianDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Armenian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Armenian month or seasonal descriptor: " + monthName);

		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Defensive boundary checks matching physical constraints
		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Armenian months must have exactly 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Avelyats intercalary period consists strictly of 5 days.");

		return new ArmenianDateInput(year, month, day);
	}

}
