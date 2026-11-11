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

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


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
		MONTHS.put("navasardi", 1);	// Common genitive case in historical texts
		MONTHS.put("nawasard", 1);		// Classical/Scientific transliteration

		// Month 2: Hori
		MONTHS.put("հոռի", 2);
		MONTHS.put("hori", 2);
		MONTHS.put("horhi", 2);
		MONTHS.put("hoṟi", 2);			// ISO/Scientific transliteration

		// Month 3: Sahmi
		MONTHS.put("սահմի", 3);
		MONTHS.put("sahmi", 3);

		// Month 4: Tre
		MONTHS.put("տրե", 4);
		MONTHS.put("tre", 4);
		MONTHS.put("dré", 4);			// Western Armenian pronunciation variant

		// Month 5: Khaghots
		MONTHS.put("քաղոց", 5);
		MONTHS.put("khaghots", 5);
		MONTHS.put("kałoc", 5);			// ISO 9985 transliteration
		MONTHS.put("xaghotz", 5);		// European phonetic spelling variant
		MONTHS.put("khaghotz", 5);

		// Month 6: Arats
		MONTHS.put("արաց", 6);
		MONTHS.put("arats", 6);
		MONTHS.put("arac", 6);			// Standard scientific transliteration

		// Month 7: Mehekan
		MONTHS.put("մեհեկան", 7);
		MONTHS.put("mehekan", 7);
		MONTHS.put("mehekani", 7);
		MONTHS.put("mehegan", 7);		// Western Armenian dialect variant

		// Month 8: Areg
		MONTHS.put("արեգ", 8);
		MONTHS.put("areg", 8);
		MONTHS.put("arek", 8);			// Western Armenian phonetic variant

		// Month 9: Ahekan
		MONTHS.put("ահեկան", 9);
		MONTHS.put("ahekan", 9);
		MONTHS.put("ahegan", 9);		// Western Armenian dialect variant

		// Month 10: Mareri
		MONTHS.put("մարերի", 10);
		MONTHS.put("mareri", 10);

		// Month 11: Margats
		MONTHS.put("մարգաց", 11);
		MONTHS.put("margats", 11);
		MONTHS.put("margas", 11);
		MONTHS.put("markats", 11);		// Western Armenian phonetic variant

		// Month 12: Hrotits
		MONTHS.put("հրոտից", 12);
		MONTHS.put("hrotits", 12);
		MONTHS.put("hrotic", 12);		// Scientific transliteration
		MONTHS.put("hrotitz", 12);

		// Month 13: Avelyats (Intercalary 5 epagomenal days)
		MONTHS.put("ավելյաց", 13);		// Modern Reformed Armenian orthography
		MONTHS.put("աւելեաց", 13);		// Classical Mesropian orthography
		MONTHS.put("avelyats", 13);
		MONTHS.put("aveliats", 13);
		MONTHS.put("awelyac", 13);		// Classical scientific transliteration
		MONTHS.put("aveleats", 13);

		// Armenian Seasons
		// Autumn
		MONTHS.put("աշուն", CalendarSeason.SEASON_AUTUMN);			// Nominativo (Ashun)
		MONTHS.put("աշնան", CalendarSeason.SEASON_AUTUMN);			// Genitive/Locative (Ashnan)
		MONTHS.put("ashun", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("ashnan", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("ašun", CalendarSeason.SEASON_AUTUMN);			// ISO 9985 format
		MONTHS.put("asoon", CalendarSeason.SEASON_AUTUMN);			// English phonetic variant

		// Winter
		MONTHS.put("ձմեռ", CalendarSeason.SEASON_WINTER);			// Nominative (Dzmer)
		MONTHS.put("ձմռան", CalendarSeason.SEASON_WINTER);			// Modern contracted genitive (Dzmran)
		MONTHS.put("dzmer", CalendarSeason.SEASON_WINTER);
		MONTHS.put("dzmeran", CalendarSeason.SEASON_WINTER);		// Uncontracted/Historical genitive form
		MONTHS.put("dzmran", CalendarSeason.SEASON_WINTER);
		MONTHS.put("jmer", CalendarSeason.SEASON_WINTER);			// Western Armenian dialect variant
		MONTHS.put("zmer", CalendarSeason.SEASON_WINTER);			// Common simple transliteration
		MONTHS.put("ձմեր", CalendarSeason.SEASON_WINTER);			// Frequent native misspelling variant

		// Spring
		MONTHS.put("գարուն", CalendarSeason.SEASON_SPRING);		// Nominative (Garun)
		MONTHS.put("գարնան", CalendarSeason.SEASON_SPRING);		// Genitive/Locative (Garnan)
		MONTHS.put("garun", CalendarSeason.SEASON_SPRING);
		MONTHS.put("garnan", CalendarSeason.SEASON_SPRING);
		MONTHS.put("karun", CalendarSeason.SEASON_SPRING);			// Western Armenian dialect variant
		MONTHS.put("karnan", CalendarSeason.SEASON_SPRING);		// Western Armenian genitive variant
		MONTHS.put("garoon", CalendarSeason.SEASON_SPRING);		// English phonetic variant

		// Summer
		MONTHS.put("ամառ", CalendarSeason.SEASON_SUMMER);			// Nominative (Amar)
		MONTHS.put("ամռան", CalendarSeason.SEASON_SUMMER);			// Genitive/Locative (Amran)
		MONTHS.put("amar", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("amran", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("amaṟ", CalendarSeason.SEASON_SUMMER);			// Scientific transliteration (trilled R)
	}

	private static final NamedMonthDateParser PARSER = NamedMonthDateParser.builder("Armenian")
		.months(MONTHS)
		.monthLengths(MonthLengths.thirtyDayMonths(5, null))
		.build();


	private ArmenianCalendarParser(){}


	/**
	 * Parses a raw Armenian text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "1 Navasard 1400")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
