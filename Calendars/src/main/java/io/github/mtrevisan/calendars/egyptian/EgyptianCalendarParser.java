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
package io.github.mtrevisan.calendars.egyptian;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for ancient Egyptian civil calendar date expressions.
 * Supports standard month names and explicit epagomenal day markers.
 */
public final class EgyptianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// --- SEASON 1: AKHET (The Inundation / Flood) ---

		// Month 1: Thoth
		MONTHS.put("thoth", 1);
		MONTHS.put("thot", 1);
		MONTHS.put("dhwtj", 1);             // Hieroglyphic MdC transliteration (Thuty)
		MONTHS.put("tehuti", 1);            // Common phonetic Egyptological form

		// Month 2: Phaophi
		MONTHS.put("phaophi", 2);
		MONTHS.put("paopi", 2);
		MONTHS.put("p3-n-jpt", 2);          // Hieroglyphic MdC transliteration (Pa-en-Ipet)
		MONTHS.put("paonipt", 2);           // Demotic reconstruction variant

		// Month 3: Athyr
		MONTHS.put("athyr", 3);
		MONTHS.put("hathor", 3);
		MONTHS.put("hwt-hwr", 3);           // Hieroglyphic MdC transliteration (Hut-Heru)
		MONTHS.put("hatoor", 3);

		// Month 4: Choiak
		MONTHS.put("choiak", 4);
		MONTHS.put("kiahk", 4);
		MONTHS.put("k3-hr-k3", 4);          // Hieroglyphic MdC transliteration (Ka-her-Ka)
		MONTHS.put("khoiak", 4);

		// --- SEASON 2: PERET (Emergence / Sowing / Winter) ---

		// Month 5: Tybi
		MONTHS.put("tybi", 5);
		MONTHS.put("tobi", 5);
		MONTHS.put("t3-jbt", 5);            // Hieroglyphic MdC transliteration (Ta-Abeet)
		MONTHS.put("tibi", 5);

		// Month 6: Mechir
		MONTHS.put("mechir", 6);
		MONTHS.put("meshir", 6);
		MONTHS.put("mhir", 6);              // Hieroglyphic MdC transliteration
		MONTHS.put("mecheir", 6);           // Classical Ptolemaic Greek variant

		// Month 7: Phamenoth
		MONTHS.put("phamenoth", 7);
		MONTHS.put("paremhat", 7);
		MONTHS.put("p3-n-jmn-hwtp", 7);     // Hieroglyphic MdC (Pa-en-Amenhotep)
		MONTHS.put("phamenothis", 7);

		// Month 8: Pharmouthi
		MONTHS.put("pharmouthi", 8);
		MONTHS.put("paremoude", 8);
		MONTHS.put("p3-n-rnwtt", 8);        // Hieroglyphic MdC (Pa-en-Renenutet)
		MONTHS.put("pharmuthi", 8);

		// --- SEASON 3: SHEMU (Harvest / Low Water / Summer) ---

		// Month 9: Pachons
		MONTHS.put("pachons", 9);
		MONTHS.put("pashons", 9);
		MONTHS.put("p3-n-xnsj", 9);         // Hieroglyphic MdC transliteration (Pa-en-Khonsu)
		MONTHS.put("pachon", 9);

		// Month 10: Payni
		MONTHS.put("payni", 10);
		MONTHS.put("paoni", 10);
		MONTHS.put("p3-n-jnt", 10);         // Hieroglyphic MdC transliteration (Pa-en-Inet)
		MONTHS.put("pauni", 10);

		// Month 11: Epiphi
		MONTHS.put("epiphi", 11);
		MONTHS.put("epip", 11);
		MONTHS.put("jpt-jpt", 11);          // Hieroglyphic MdC transliteration (Ipet-Ipet)
		MONTHS.put("ephihi", 11);

		// Month 12: Mesore
		MONTHS.put("mesore", 12);
		MONTHS.put("mesori", 12);
		MONTHS.put("ms-wt-r-c", 12);        // Hieroglyphic MdC transliteration (Mesu-ut-Ra)
		MONTHS.put("mesra", 12);

		// --- INTERCALARY PERIOD: EPAGOMENAL DAYS ---

		// Month 13: The 5 extra days (6 in rare later Roman reforms)
		MONTHS.put("epagomenai", 13);       // Classical Greek feminine plural form
		MONTHS.put("epagomenal", 13);       // Modern international English derivative
		MONTHS.put("meskut", 13);           // Ancient Egyptian native term (mswt-ntrw / Birth of Gods)
		MONTHS.put("heru-renpet", 13);      // Alternate native phrase ("Days upon the year")
		MONTHS.put("hrw-rnpt", 13);         // Hieroglyphic MdC transliteration for Heru-Renpet


		// Season 1: Akhet (Inundation - Aligned to Autumn Cross-grid)
		MONTHS.put("akhet", CalendarType.SEASON_AUTUMN);
		MONTHS.put("3xt", CalendarType.SEASON_AUTUMN);            // Scientific Hieroglyphic notation
		MONTHS.put("axet", CalendarType.SEASON_AUTUMN);           // Simplified phonetic variant

		// Season 2: Peret (Emergence / Growing - Aligned to Winter Cross-grid)
		MONTHS.put("peret", CalendarType.SEASON_WINTER);
		MONTHS.put("prt", CalendarType.SEASON_WINTER);            // Scientific Hieroglyphic notation
		MONTHS.put("proyet", CalendarType.SEASON_WINTER);

		// Season 3: Spring Grid (Abstract astronomical interpolation if needed by parser)
		MONTHS.put("egyptian-spring", CalendarType.SEASON_SPRING);

		// Season 4: Shemu (Harvest / Low Water / Summer - FIXED from Spring to Summer)
		MONTHS.put("shemu", CalendarType.SEASON_SUMMER);          // Correct historical alignment
		MONTHS.put("shomu", CalendarType.SEASON_SUMMER);          // Phonetic variant
		MONTHS.put("smw", CalendarType.SEASON_SUMMER);            // Scientific Hieroglyphic notation
	}

	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?(\\S+)\\s+(\\d+)");


	private EgyptianCalendarParser(){}


	/**
	 * Parses a raw Egyptian text string into a validated data container.
	 *
	 * @param rawDate the date expression (e.g., "1 Thoth 1400" or "3 Meskut 1520")
	 * @return a structured EgyptianDateInput container
	 */
	public static EgyptianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new EgyptianDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Egyptian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT);
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Egyptian month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Standard Egyptian civil calendar parameter validation boundaries
		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Egyptian months must have exactly 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 5))
			throw new IllegalArgumentException("Epagomenal period consists strictly of 5 days.");

		return new EgyptianDateInput(year, month, day);
	}

}
