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
package io.github.mtrevisan.calendars.rumi;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for Ottoman Rumi calendar expressions.
 * Supports classical Ottoman terms and modern Turkish equivalents.
 */
public final class RumiCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: Mart (March) - The anchor of the fiscal year
		MONTHS.put("mart", 1);
		MONTHS.put("مارت", 1);				// Ottoman Arabic script

		// Month 2: Nisan (April)
		MONTHS.put("nisan", 2);
		MONTHS.put("نيسان", 2);

		// Month 3: Mayıs (May)
		MONTHS.put("mayis", 3);
		MONTHS.put("mayıs", 3);
		MONTHS.put("مائيس", 3);

		// Month 4: Haziran (June)
		MONTHS.put("haziran", 4);
		MONTHS.put("حزيران", 4);

		// Month 5: Temmuz (July)
		MONTHS.put("temmuz", 5);
		MONTHS.put("تموز", 5);

		// Month 6: Ağustos (August)
		MONTHS.put("agustos", 6);
		MONTHS.put("ağustos", 6);
		MONTHS.put("اغستوس", 6);

		// Month 7: Eylül (September)
		MONTHS.put("eylul", 7);
		MONTHS.put("eylül", 7);
		MONTHS.put("ايلول", 7);

		// Month 8: Teşrin-i Evvel / Ekim (October)
		MONTHS.put("tesrin-i evvel", 8);
		MONTHS.put("teşrin-i evvel", 8);
		MONTHS.put("birinci tesrin", 8);		// Transitional 20th-century name ("First Teshrin")
		MONTHS.put("birinci teşrin", 8);
		MONTHS.put("tesrinevvel", 8);
		MONTHS.put("ekim", 8);
		MONTHS.put("تشرين اول", 8);			// Ottoman Arabic script

		// Month 9: Teşrin-i Sani / Kasım (November)
		MONTHS.put("tesrin-i sani", 9);
		MONTHS.put("teşrin-i sani", 9);
		MONTHS.put("ikinci tesrin", 9);		// Transitional name variant ("Second Teshrin")
		MONTHS.put("ikinci teşrin", 9);
		MONTHS.put("tesrinsani", 9);
		MONTHS.put("kasim", 9);
		MONTHS.put("kasım", 9);
		MONTHS.put("تشرين ثانى", 9);			// Ottoman Arabic script

		// Month 10: Kânûn-ı Evvel / Aralık (December)
		MONTHS.put("kanun-i evvel", 10);
		MONTHS.put("kânûn-ı evvel", 10);
		MONTHS.put("birinci kanun", 10);		// Transitional name variant ("First Kanun")
		MONTHS.put("birinci kânûn", 10);
		MONTHS.put("kanunevvel", 10);
		MONTHS.put("aralik", 10);
		MONTHS.put("aralık", 10);
		MONTHS.put("كانون اول", 10);			// Ottoman Arabic script

		// Month 11: Kânûn-ı Sani / Ocak (January)
		MONTHS.put("kanun-i sani", 11);
		MONTHS.put("kânûn-ı sani", 11);
		MONTHS.put("ikinci kanun", 11);		// Transitional name variant ("Second Kanun")
		MONTHS.put("ikinci kânûn", 11);
		MONTHS.put("son kanun", 11);			// Alternate transitional variant ("Last Kanun")
		MONTHS.put("son kânûn", 11);
		MONTHS.put("kanunsani", 11);
		MONTHS.put("ocak", 11);
		MONTHS.put("كانون ثانى", 11);			// Ottoman Arabic script

		// Month 12: Şubat (February)
		MONTHS.put("subat", 12);
		MONTHS.put("şubat", 12);
		MONTHS.put("شباط", 12);


		// Autumn
		MONTHS.put("güz", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("guz", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("sonbahar", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("خريف", CalendarSeason.SEASON_AUTUMN);			// Ottoman literary Arabic loan-word
		MONTHS.put("eylul-season", CalendarSeason.SEASON_AUTUMN);

		// Winter
		MONTHS.put("kış", CalendarSeason.SEASON_WINTER);
		MONTHS.put("kis", CalendarSeason.SEASON_WINTER);
		MONTHS.put("شتاء", CalendarSeason.SEASON_WINTER);			// Ottoman literary Arabic loan-word
		MONTHS.put("zemheri", CalendarSeason.SEASON_WINTER);		// Traditional folk peak winter token
		MONTHS.put("subat-season", CalendarSeason.SEASON_WINTER);

		// Spring
		MONTHS.put("bahar", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ilkbahar", CalendarSeason.SEASON_SPRING);
		MONTHS.put("بهار", CalendarSeason.SEASON_SPRING);			// Ottoman Persian loan-word script
		MONTHS.put("mart-season", CalendarSeason.SEASON_SPRING);

		// Summer
		MONTHS.put("yaz", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("صيف", CalendarSeason.SEASON_SUMMER);			// Ottoman literary Arabic loan-word
		MONTHS.put("haziran-season", CalendarSeason.SEASON_SUMMER);
	}

	private static final NamedMonthDateParser PARSER = NamedMonthDateParser.builder("Rumi")
		.months(MONTHS)
		.monthLengths(RumiCalendarStrategy::monthLength)
		.build();


	private RumiCalendarParser(){}


	/**
	 * Parses a raw Rumi text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "15 Mart 1330")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
