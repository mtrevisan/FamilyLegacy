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
package io.github.mtrevisan.calendars.venetan;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for historical Venetian (more Veneto) calendar date expressions.
 * Supports historical Venetian month names, standard Italian items, and "MV" markers.
 */
public final class VenetanCalendarParser{

	private static final Pattern ARO_SUFFIX = Pattern.compile("aro?$");
	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Venetian Month Names (Year starting in March)
		MONTHS.put("marŧo", 1);
		MONTHS.put("marŧo mv", 1);
		MONTHS.put("marso", 1);
		MONTHS.put("marso mv", 1);
		MONTHS.put("marzo", 1);
		MONTHS.put("marzo mv", 1);
		MONTHS.put("april", 2);
		MONTHS.put("aprile", 2);
		MONTHS.put("abril", 2);
		MONTHS.put("abrile", 2);
		MONTHS.put("avril", 2);
		MONTHS.put("avrile", 2);
		MONTHS.put("averil", 2);
		MONTHS.put("averile", 2);
		MONTHS.put("veril", 2);
		MONTHS.put("verile", 2);
		MONTHS.put("mađo", 3);
		MONTHS.put("maxo", 3);
		MONTHS.put("mai", 3);
		MONTHS.put("maio", 3);
		MONTHS.put("majo", 3);
		MONTHS.put("magio", 3);
		MONTHS.put("đuño", 4);
		MONTHS.put("xuño", 4);
		MONTHS.put("junio", 4);
		MONTHS.put("giuño", 4);
		MONTHS.put("lujo", 5);
		MONTHS.put("lulio", 5);
		MONTHS.put("lugio", 5);
		MONTHS.put("luglio", 5);
		MONTHS.put("lulgio", 5);
		MONTHS.put("agosto", 6);
		MONTHS.put("agusto", 6);
		MONTHS.put("avosto", 6);
		MONTHS.put("aosto", 6);
		MONTHS.put("setenbre", 7);
		MONTHS.put("setènbre", 7);
		MONTHS.put("seténbare", 7);
		MONTHS.put("setenbro", 7);
		MONTHS.put("setenbrio", 7);
		MONTHS.put("september", 7);
		MONTHS.put("otore", 8);
		MONTHS.put("otobre", 8);
		MONTHS.put("otóbare", 8);
		MONTHS.put("otobro", 8);
		MONTHS.put("otobrio", 8);
		MONTHS.put("otubrio", 8);
		MONTHS.put("otovre", 8);
		MONTHS.put("noenbre", 9);
		MONTHS.put("novenbre", 9);
		MONTHS.put("novènbre", 9);
		MONTHS.put("novénbare", 9);
		MONTHS.put("novenbro", 9);
		MONTHS.put("novenbrio", 9);
		MONTHS.put("deŧenbre", 10);
		MONTHS.put("desenbre", 10);
		MONTHS.put("deŧènbre", 10);
		MONTHS.put("desènbre", 10);
		MONTHS.put("desénbare", 10);
		MONTHS.put("deŧenbro", 10);
		MONTHS.put("desenbro", 10);
		MONTHS.put("deŧenbrio", 10);
		MONTHS.put("desenbrio", 10);
		MONTHS.put("genèr", 11);
		MONTHS.put("genèr mv", 11);
		MONTHS.put("zenèr", 11);
		MONTHS.put("zenèr mv", 11);
		MONTHS.put("genaio", 11);
		MONTHS.put("genaio mv", 11);
		MONTHS.put("đanèr", 11);
		MONTHS.put("đanèr mv", 11);
		MONTHS.put("xanèr", 11);
		MONTHS.put("xanèr mv", 11);
		MONTHS.put("đenèr", 11);
		MONTHS.put("đenèr mv", 11);
		MONTHS.put("xenèr", 11);
		MONTHS.put("xenèr mv", 11);
		MONTHS.put("đeñèr", 11);
		MONTHS.put("đeñèr mv", 11);
		MONTHS.put("xeñèr", 11);
		MONTHS.put("xeñèr mv", 11);
		MONTHS.put("jeñèr", 11);
		MONTHS.put("jeñèr mv", 11);
		MONTHS.put("febrèr", 12);
		MONTHS.put("febrèr mv", 12);
		MONTHS.put("fevrèr", 12);
		MONTHS.put("fevrèr mv", 12);
		MONTHS.put("feverèr", 12);
		MONTHS.put("feverèr mv", 12);
		MONTHS.put("fevèr", 12);
		MONTHS.put("fevèr mv", 12);
		MONTHS.put("frevèr", 12);
		MONTHS.put("frevèr mv", 12);
		MONTHS.put("febraio", 12);
		MONTHS.put("febraio mv", 12);

		// Autun (Autumn) -> Maps to Autumnal Equinox flag
		MONTHS.put("auton", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("autun", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("autuno", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("aut", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("otun", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("otuno", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("otu", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("utun", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("utuno", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("utu", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("ferdima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fer", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fardima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("farnima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("far", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fraima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fra", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("frima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fri", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fredima", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("fre", CalendarSeason.SEASON_AUTUMN);
		// Invèrno (Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("invèrno", CalendarSeason.SEASON_WINTER);
		MONTHS.put("invièrno", CalendarSeason.SEASON_WINTER);
		MONTHS.put("inv", CalendarSeason.SEASON_WINTER);
		MONTHS.put("inbèrno", CalendarSeason.SEASON_WINTER);
		MONTHS.put("inb", CalendarSeason.SEASON_WINTER);
		MONTHS.put("divèr", CalendarSeason.SEASON_WINTER);
		MONTHS.put("div", CalendarSeason.SEASON_WINTER);
		// Vèrta (Spring) -> Maps to Vernal Equinox flag
		MONTHS.put("primavera", CalendarSeason.SEASON_SPRING);
		MONTHS.put("primavèra", CalendarSeason.SEASON_SPRING);
		MONTHS.put("primavèrta", CalendarSeason.SEASON_SPRING);
		MONTHS.put("pri", CalendarSeason.SEASON_SPRING);
		MONTHS.put("vèrta", CalendarSeason.SEASON_SPRING);
		MONTHS.put("vèr", CalendarSeason.SEASON_SPRING);
		MONTHS.put("insuta", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ins", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ansuda", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ans", CalendarSeason.SEASON_SPRING);
		// Istà (Summer) -> Maps to Estival Solstice flag
		MONTHS.put("istade", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("istae", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("istà", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("ist", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("instade", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("instae", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("instà", CalendarSeason.SEASON_SUMMER);
//		MONTHS.put("ins", CalendarSeason.SEASON_SUMMER);		// `Instà` does not have an abbreviation
		MONTHS.put("estade", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("estae", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("està", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("est", CalendarSeason.SEASON_SUMMER);
	}

	/** Words of the date formulas that carry no information (e.g. "adì 5 marzo 1500", "die 5 mensis martii"). */
	private static final Pattern FORMULA_WORDS = Pattern.compile(
		"(?iu)(?<![\\p{L}])(?:adì|adi|addì|addi|a dì|die|mensis|anno|domini|de|del|di)(?![\\p{L}])");

	// Pattern mapping: "Day Month Year" with optional trailing "MV" or "More Veneto" markers
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\S+)\\s+)?([a-zA-Zđŧɉƚñèéàòóù\\s-]+?)\\s+(\\d+)(?:\\s+(?:mv|more veneto))?", Pattern.CASE_INSENSITIVE);


	private VenetanCalendarParser(){}


	/**
	 * Parses a raw Venetian date string into a configured structural container.
	 *
	 * @param rawDate the date expression (e.g., "15 Febraro 1700" or "22 Gennaio 1530 MV")
	 * @return a configured VenetoDateInput instance
	 * @throws IllegalArgumentException if text layouts or string formatting constraints fail
	 */
	public static VenetanDateInput parse(final String rawDate){
		// the indiction is checked against the year by the calendar, after the conversion
		final Indiction.Extraction extraction = Indiction.extract(rawDate);
		final int indiction = extraction.indiction();
		final String normalized = StringUtils.normalizeSpace(FORMULA_WORDS.matcher(extraction.text()).replaceAll(StringUtils.SPACE))
			.toLowerCase(Locale.ROOT);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new VenetanDateInput(Integer.parseInt(normalized), 0, 0, indiction);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Venetian (more Veneto) date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		String monthName = matcher.group(2).trim();
		final int year = Integer.parseInt(matcher.group(3));
		// Standard Venetian typographical normalization steps
		monthName = monthName.replace("dh", "đ")
			.replace("th", "ŧ")
			.replace("jh", "ɉ")
			.replace("ɉ", "j")
			.replace("ƚe", "l")
			.replace("ƚi", "l")
			.replace("gn", "ñ");
		monthName = ARO_SUFFIX.matcher(monthName)
			.replaceAll("èr");

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Venetian month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		// Julian framework boundary verification
		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day count violates standard Julian calendar monthly limits.");

		return new VenetanDateInput(year, month, day, indiction);
	}

}
