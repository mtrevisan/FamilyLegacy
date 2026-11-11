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
package io.github.mtrevisan.calendars.ethiopian;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for Ethiopian calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, transliterations, and native Amharic seasonal markers.
 */
public final class EthiopianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Mäskeräm (New Year / Enkutatash)
		MONTHS.put("መስከረም", 1);			// Native Fidäl script
		MONTHS.put("mäskeräm", 1);			// Scientific linguistic transliteration
		MONTHS.put("meskerem", 1);			// Standard ASCII variant
		MONTHS.put("maskaram", 1);			// Common phonetic variation

		// Month 2: Ṭəqəmt
		MONTHS.put("ጥቅምት", 2);
		MONTHS.put("ṭəqəmt", 2);
		MONTHS.put("tekemt", 2);
		MONTHS.put("tseqemt", 2);			// Common phonetic rendering of the explosive T (ጠ)
		MONTHS.put("tiqimt", 2);			// Alternate vowel notation

		// Month 3: ḫədar
		MONTHS.put("ህዳር", 3);
		MONTHS.put("ḫədar", 3);
		MONTHS.put("hedar", 3);
		MONTHS.put("hidar", 3);				// Most frequent English/Amharic media spelling

		// Month 4: Taḫśaś
		MONTHS.put("ታኅሣሥ", 4);
		MONTHS.put("taḫśaś", 4);
		MONTHS.put("tahasas", 4);
		MONTHS.put("tahsas", 4);			// Standard plain text variant

		// Month 5: Ṭərr
		MONTHS.put("ጥር", 5);
		MONTHS.put("ṭərr", 5);
		MONTHS.put("ter", 5);
		MONTHS.put("tir", 5);				// Alternate vowel notation

		// Month 6: Yäkatit
		MONTHS.put("የካቲት", 6);
		MONTHS.put("yäkatit", 6);
		MONTHS.put("yekatit", 6);
		MONTHS.put("yakatit", 6);

		// Month 7: Mäggabit
		MONTHS.put("መጋቢት", 7);
		MONTHS.put("maggabit", 7);
		MONTHS.put("megabit", 7);
		MONTHS.put("magabit", 7);

		// Month 8: Miyazya
		MONTHS.put("ሚያዝያ", 8);
		MONTHS.put("miyazya", 8);
		MONTHS.put("miyazia", 8);
		MONTHS.put("miazia", 8);

		// Month 9: Gənboṭ
		MONTHS.put("ግንቦት", 9);
		MONTHS.put("gənboṭ", 9);
		MONTHS.put("genbot", 9);
		MONTHS.put("ginbot", 9);			// Highly frequent alternate spelling

		// Month 10: Sänē
		MONTHS.put("ሰኔ", 10);
		MONTHS.put("sänē", 10);
		MONTHS.put("sene", 10);
		MONTHS.put("sanie", 10);

		// Month 11: Ḥamle
		MONTHS.put("ሐምሌ", 11);
		MONTHS.put("ḥamle", 11);
		MONTHS.put("hamle", 11);
		MONTHS.put("hamlie", 11);

		// Month 12: Nähase
		MONTHS.put("ነሐሴ", 12);
		MONTHS.put("nähase", 12);
		MONTHS.put("nehase", 12);
		MONTHS.put("nahase", 12);

		// Month 13: Pagumēn (Intercalary Month / 5 or 6 Epagomenal Days)
		MONTHS.put("ጳጉሜን", 13);
		MONTHS.put("pagumēn", 13);
		MONTHS.put("pagumen", 13);
		MONTHS.put("pagume", 13);			// Truncated spoken form


		// =============================================================================
		// ETHIOPIAN SEASONS ALIGNMENT (Traditional Agricultural & Literary Grid)
		// =============================================================================

		// Season 1: Bega (The Long Dry Season - Aligned to Winter Cross-grid)
		MONTHS.put("በጋ", CalendarSeason.SEASON_WINTER);
		MONTHS.put("bega", CalendarSeason.SEASON_WINTER);

		// Season 2: Belg (The Short Rainy Season / Spring Transition)
		MONTHS.put("በልግ", CalendarSeason.SEASON_SPRING);
		MONTHS.put("belg", CalendarSeason.SEASON_SPRING);

		// Season 3: Kremt (The Heavy Monsoon Rainy Season - Aligned to Summer Cross-grid)
		MONTHS.put("ክረምት", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("keremt", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("kremt", CalendarSeason.SEASON_SUMMER);

		// Season 4: Tseday (The Blossom Season / Post-Rain Autumn - FIXED and mapped to Autumn)
		MONTHS.put("ጸደይ", CalendarSeason.SEASON_AUTUMN);		// Native Amharic script for Autumn bloom
		MONTHS.put("tseday", CalendarSeason.SEASON_AUTUMN);	// Correct astronomical alignment
		MONTHS.put("ceday", CalendarSeason.SEASON_AUTUMN);	// Linguistic ISO variant
	}

	private static final NamedMonthDateParser PARSER = NamedMonthDateParser.builder("Ethiopian")
		.months(MONTHS)
		.monthLengths(MonthLengths.thirtyDayMonths(5, EthiopianCalendarStrategy::isLeapYear))
		.build();


	private EthiopianCalendarParser(){}


	/**
	 * Parses a raw Ethiopian text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Keremt 2015")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
