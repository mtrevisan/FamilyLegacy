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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private RumiCalendarParser(){}


	/**
	 * Parses a raw Rumi string expression into a validated architectural container.
	 *
	 * @param rawDate the string representing the Rumi date
	 * @return a configured RumiDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static RumiDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new RumiDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Rumi date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Rumi month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches Rumi monthly limits.");

		return new RumiDateInput(year, month, day);
	}

}
