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
package io.github.mtrevisan.calendars.coptic;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for Coptic calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, transliterations, and native seasonal markers.
 */
public final class CopticCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Thout (Inundation begins)
		MONTHS.put("ⲑⲩⲧ", 1);		// Native Coptic script
		MONTHS.put("توت", 1);		// Egyptian Arabic script
		MONTHS.put("thout", 1);
		MONTHS.put("tout", 1);
		MONTHS.put("thoth", 1);		// Classical Greek/Egyptian form

		// Month 2: Paopi
		MONTHS.put("ⲡⲁⲟⲡⲓ", 2);
		MONTHS.put("بابة", 2);
		MONTHS.put("baba", 2);
		MONTHS.put("paopi", 2);
		MONTHS.put("babah", 2);		// Common Arabic phonetic transliteration

		// Month 3: Hathor
		MONTHS.put("ϩⲁⲑⲩⲣ", 3);
		MONTHS.put("هاتور", 3);
		MONTHS.put("hatour", 3);
		MONTHS.put("hathor", 3);
		MONTHS.put("hathoor", 3);

		// Month 4: Koiak
		MONTHS.put("كياك", 4);		// Egyptian Arabic script
		MONTHS.put("ⲕⲓⲁϩⲕ", 4);		// Coptic dialectal variant
		MONTHS.put("kiahk", 4);
		MONTHS.put("koiak", 4);
		MONTHS.put("choiak", 4);	// Standard Greco-Roman scientific spelling

		// Month 5: Tobi (Sowing begins)
		MONTHS.put("ⲧⲩⲓⲂ", 5);
		MONTHS.put("طوبة", 5);
		MONTHS.put("toba", 5);
		MONTHS.put("tobi", 5);
		MONTHS.put("touba", 5);		// French-Egyptian transliteration

		// Month 6: Meshir
		MONTHS.put("ⲥⲓϣⲓⲣ", 6);
		MONTHS.put("أمشير", 6);
		MONTHS.put("amshir", 6);
		MONTHS.put("meshir", 6);
		MONTHS.put("mocheir", 6);

		// Month 7: Paremhat
		MONTHS.put("ⲡⲁⲣⲉⲥϩⲁⲧ", 7);
		MONTHS.put("برمهات", 7);
		MONTHS.put("baramhat", 7);
		MONTHS.put("paremhat", 7);
		MONTHS.put("phamenoth", 7);	// Ancient Greek/Pharaonic variant

		// Month 8: Paremoude
		MONTHS.put("ⲡⲁⲣⲉⲥⲟⲩⲓⲉ", 8);
		MONTHS.put("برمودة", 8);
		MONTHS.put("baramouda", 8);
		MONTHS.put("paremoude", 8);
		MONTHS.put("pharmouthi", 8);	// Ancient Greek/Pharaonic variant

		// Month 9: Pashons (Harvest begins)
		MONTHS.put("ⲡⲁϣⲟⲛⲥ", 9);
		MONTHS.put("บะชังส์", 9);			// Transliterated Thai artifact
		MONTHS.put("بشنس", 9);			// Egyptian Arabic script
		MONTHS.put("bashans", 9);
		MONTHS.put("pashons", 9);
		MONTHS.put("pachon", 9);		// Classical short variant

		// Month 10: Paoni
		MONTHS.put("ⲡⲁⲱⲛⲓ", 10);
		MONTHS.put("بؤونة", 10);
		MONTHS.put("baouna", 10);
		MONTHS.put("paoni", 10);
		MONTHS.put("baounah", 10);

		// Month 11: Epip
		MONTHS.put("ⲉⲡⲏⲡ", 11);
		MONTHS.put("أبيب", 11);
		MONTHS.put("abep", 11);
		MONTHS.put("epip", 11);
		MONTHS.put("epeut", 11);

		// Month 12: Mesori
		MONTHS.put("ⲥⲉⲥⲱⲣⲓ", 12);
		MONTHS.put("مسرى", 12);
		MONTHS.put("mesra", 12);
		MONTHS.put("mesori", 12);
		MONTHS.put("mesore", 12);

		// Month 13: Pi Kogi Enavot (Intercalary Month / 5 or 6 Epagomenal Days)
		MONTHS.put("ⲡⲓⲕⲟⲩϫⲓ ⲛ̀ⲁⲃⲟⲧ", 13);		// Complete native Coptic ("The Little Month")
		MONTHS.put("النسيء", 13);				// Arabic official calendar name
		MONTHS.put("pi kogi enavot", 13);
		MONTHS.put("nasiee", 13);
		MONTHS.put("el-nasie", 13);
		MONTHS.put("epagomene", 13);			// Scientific Greek loan-word


		// Season 1: Inundation (Akhet / Autumn Cross-alignment)
		MONTHS.put("ⲁⲏⲓⲁ", CalendarSeason.SEASON_AUTUMN);		// Native Coptic script
		MONTHS.put("الفيضان", CalendarSeason.SEASON_AUTUMN);	// Arabic ("The Flood")
		MONTHS.put("ahia", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("akhet", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("akhed", CalendarSeason.SEASON_AUTUMN);

		// Season 2: Sowing / Procreation (Peret / Winter Cross-alignment)
		MONTHS.put("ⲧⲟⲕⲁ", CalendarSeason.SEASON_WINTER);		// Native Coptic script
		MONTHS.put("الزراعة", CalendarSeason.SEASON_WINTER);	// Arabic ("The Sowing")
		MONTHS.put("toka", CalendarSeason.SEASON_WINTER);
		MONTHS.put("peret", CalendarSeason.SEASON_WINTER);
		MONTHS.put("proyet", CalendarSeason.SEASON_WINTER);

		// Season 3: Spring / Transitional Equinox Grid (Optional Abstract Insertion)
		MONTHS.put("الربيع", CalendarSeason.SEASON_SPRING);	// Modern Arabic spring injection

		// Season 4: Harvest / Low Water (Shemu / Summer Cross-alignment)
		MONTHS.put("ϣⲱⲥ", CalendarSeason.SEASON_SUMMER);		// Native Coptic script
		MONTHS.put("الحصاد", CalendarSeason.SEASON_SUMMER);	// Arabic ("The Harvest")
		MONTHS.put("shom", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("shemu", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("shumu", CalendarSeason.SEASON_SUMMER);
	}

	private static final NamedMonthDateParser PARSER = NamedMonthDateParser.builder("Coptic")
		.months(MONTHS)
		.monthLengths(MonthLengths.thirtyDayMonths(5, CopticCalendarStrategy::isLeapYear))
		.build();


	private CopticCalendarParser(){}


	/**
	 * Parses a raw Coptic text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Shom 1730")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
