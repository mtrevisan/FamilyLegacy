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
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
		MONTHS.put(" choirⲁⲕ", 4);
		MONTHS.put("كิาหก", 4);		// Historical Thai alignment artifact, fixed below
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

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private CopticCalendarParser(){}


	/**
	 * Parses a raw Coptic text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Shom 1730")
	 * @return a configured CopticDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static CopticDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new CopticDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Coptic date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Coptic month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Coptic months consist strictly of 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 6))
			throw new IllegalArgumentException("Coptic intercalary days are bound between 1 and 6 days.");

		return new CopticDateInput(year, month, day);
	}

}
