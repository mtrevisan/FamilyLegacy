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
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.text.DateFormulas;
import io.github.mtrevisan.calendars.text.Ordinals;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for historical Venetan (more Veneto) calendar date expressions.
 * Supports historical Venetan month names, standard Italian items, and "MV" markers.
 */
public final class VenetanCalendarParser{

	private static final Pattern ARO_SUFFIX = Pattern.compile("aro?$");
	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Venetan Month Names (Year starting in March)
		MONTHS.put("marŧo", 1);
		MONTHS.put("marŧo mv", 1);
		MONTHS.put("marso", 1);
		MONTHS.put("marso mv", 1);
		MONTHS.put("marzo", 1);
		MONTHS.put("marzo mv", 1);
		MONTHS.put("mar", 1);
		MONTHS.put("mar mv", 1);
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
		MONTHS.put("apr", 2);
		MONTHS.put("abr", 2);
		MONTHS.put("avr", 2);
		MONTHS.put("ave", 2);
		MONTHS.put("ver", 2);
		MONTHS.put("mađo", 3);
		MONTHS.put("maxo", 3);
		MONTHS.put("mai", 3);
		MONTHS.put("maio", 3);
		MONTHS.put("majo", 3);
		MONTHS.put("magio", 3);
		MONTHS.put("mađ", 3);
		MONTHS.put("max", 3);
		MONTHS.put("maj", 3);
		MONTHS.put("mag", 3);
		MONTHS.put("đuñ", 4);
		MONTHS.put("đuño", 4);
		MONTHS.put("xuño", 4);
		MONTHS.put("junio", 4);
		MONTHS.put("giuño", 4);
		MONTHS.put("xuñ", 4);
		MONTHS.put("jun", 4);
		MONTHS.put("giu", 4);
		MONTHS.put("lujo", 5);
		MONTHS.put("lulio", 5);
		MONTHS.put("lugio", 5);
		MONTHS.put("luglio", 5);
		MONTHS.put("lulgio", 5);
		MONTHS.put("luj", 5);
		MONTHS.put("lul", 5);
		MONTHS.put("lug", 5);
		MONTHS.put("lul", 5);
		MONTHS.put("agosto", 6);
		MONTHS.put("agusto", 6);
		MONTHS.put("avosto", 6);
		MONTHS.put("aosto", 6);
		MONTHS.put("ago", 6);
		MONTHS.put("agu", 6);
		MONTHS.put("avo", 6);
		MONTHS.put("aos", 6);
		MONTHS.put("setenbre", 7);
		MONTHS.put("setènbre", 7);
		MONTHS.put("seténbare", 7);
		MONTHS.put("setenbro", 7);
		MONTHS.put("setenbrio", 7);
		MONTHS.put("september", 7);
		MONTHS.put("set", 7);
		MONTHS.put("sep", 7);
		MONTHS.put("otore", 8);
		MONTHS.put("otobre", 8);
		MONTHS.put("otóbare", 8);
		MONTHS.put("otobro", 8);
		MONTHS.put("otobrio", 8);
		MONTHS.put("otubrio", 8);
		MONTHS.put("otovre", 8);
		MONTHS.put("oto", 8);
		MONTHS.put("otó", 8);
		MONTHS.put("otu", 8);
		MONTHS.put("noenbre", 9);
		MONTHS.put("novenbre", 9);
		MONTHS.put("novènbre", 9);
		MONTHS.put("novénbare", 9);
		MONTHS.put("novenbro", 9);
		MONTHS.put("novenbrio", 9);
		MONTHS.put("noe", 9);
		MONTHS.put("nov", 9);
		MONTHS.put("deŧenbre", 10);
		MONTHS.put("desenbre", 10);
		MONTHS.put("deŧènbre", 10);
		MONTHS.put("desènbre", 10);
		MONTHS.put("desénbare", 10);
		MONTHS.put("deŧenbro", 10);
		MONTHS.put("desenbro", 10);
		MONTHS.put("deŧenbrio", 10);
		MONTHS.put("desenbrio", 10);
		MONTHS.put("deŧ", 10);
		MONTHS.put("des", 10);
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
		MONTHS.put("gen", 11);
		MONTHS.put("gen mv", 11);
		MONTHS.put("zen", 11);
		MONTHS.put("zen mv", 11);
		MONTHS.put("gen", 11);
		MONTHS.put("gen mv", 11);
		MONTHS.put("đan", 11);
		MONTHS.put("đan mv", 11);
		MONTHS.put("xan", 11);
		MONTHS.put("xan mv", 11);
		MONTHS.put("đen", 11);
		MONTHS.put("đen mv", 11);
		MONTHS.put("xen", 11);
		MONTHS.put("xen mv", 11);
		MONTHS.put("đeñ", 11);
		MONTHS.put("đeñ mv", 11);
		MONTHS.put("xeñ", 11);
		MONTHS.put("xeñ mv", 11);
		MONTHS.put("jeñ", 11);
		MONTHS.put("jeñ mv", 11);
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
		MONTHS.put("feb", 12);
		MONTHS.put("feb mv", 12);
		MONTHS.put("fev", 12);
		MONTHS.put("fev mv", 12);
		MONTHS.put("fre", 12);
		MONTHS.put("fre mv", 12);
		MONTHS.put("feb", 12);
		MONTHS.put("feb mv", 12);

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

	/** Markers of the Venetan year (Mos Veneto) after the year, with no information (the months are already Venetan). */
	private static final List<String> MORE_VENETO_MARKERS = List.of("mv", "more veneto");

	/**
	 * Typographical variants of the Venetan spelling and their standard form, applied in order (e.g. "dh" is written for
	 * "đ").
	 */
	private static final String[][] TYPOGRAPHIC_VARIANTS = {{"dh", "đ"}, {"th", "ŧ"}, {"jh", "ɉ"}, {"ɉ", "j"}, {"ƚe", "l"},
		{"ƚi", "l"}, {"gn", "ñ"}};

	// Pattern mapping: "Day Month Year" (the year can be dual, e.g. "1500/1") with optional trailing "MV" or "More Veneto"
	// markers
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\S+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)(?:/(\\d+))?(?:\\s+(?:"
		+ DateFormulas.alternation(MORE_VENETO_MARKERS) + "))?", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

	/** First Venetan month that belongs to the next civil year (January). */
	private static final int FIRST_MONTH_AFTER_NEW_YEAR = 11;
	/** The Venetan months with a dual year: January and February. */
	private static final int LAST_VENETAN_MONTH = 12;


	private VenetanCalendarParser(){}


	/**
	 * Parses a raw Venetan date string into a configured structural container.
	 * <p>
	 * The date can be written with the formulas of the documents ("adì", "die ... mensis", "A.D."), the ordinal day ("5th",
	 * "1°", "primo"), the era before Christ (year numbers are then astronomical), the "MV" marker, a dual year in January
	 * and February (e.g. "5 zenèr 1500/1", the second year being the civil one), and the language between parentheses.
	 * A season cannot have a day.
	 * </p>
	 *
	 * @param rawDate the date expression (e.g., "15 Febraro 1700" or "22 Gennaio 1530 MV")
	 * @return a configured VenetoDateInput instance
	 * @throws IllegalArgumentException if text layouts or string formatting constraints fail
	 */
	public static VenetanDateInput parse(final String rawDate){
		if(StringUtils.isBlank(rawDate))
			throw new IllegalArgumentException("Date string context cannot be empty.");

		// the indiction is checked against the year by the calendar, after the conversion
		final Indiction.Extraction extraction = Indiction.extract(GregorianCalendarParser.removeLanguage(rawDate));
		// a formula word that is also a month (e.g. "des" for December) is not removed
		final DateFormulas.Era era = DateFormulas.extractEra(DateFormulas.removeFormulas(extraction.text(),
			word -> findMonth(word) != null));
		final int indiction = extraction.indiction();
		final boolean beforeChrist = era.beforeChrist();
		final String normalized = StringUtils.normalizeSpace(era.text())
			.toLowerCase(Locale.ROOT);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new VenetanDateInput(toAstronomicalYear(normalized, beforeChrist), 0, 0, indiction);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Venetan (more Veneto) date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2).trim();
		final int year = toAstronomicalYear(matcher.group(3), beforeChrist);

		final int month = resolveMonth(monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		int day = 0;
		if(dayGroup != null){
			if(month < 0)
				throw new IllegalArgumentException("A season cannot have a day: " + rawDate);

			day = Ordinals.parse(dayGroup);
			// Julian framework boundary verification
			if(day < 1 || day > 31)
				throw new IllegalArgumentException("Invalid day in the Venetan date: " + rawDate);
		}

		final String dualYear = matcher.group(4);
		if(dualYear != null)
			validateDualYear(rawDate, matcher.group(3), dualYear, month, beforeChrist);

		return new VenetanDateInput(year, month, day, indiction, dualYear != null);
	}

	/**
	 * Looks up a month name as written first, so that a word that is a month is not rewritten (e.g. the autumn "far" is
	 * not the February "fèr"), then with the typographical variants of the spelling standardized, then with the suffix of
	 * the old forms of the months (e.g. "febraro") turned into the Venetan one.
	 */
	private static int resolveMonth(final String monthName){
		final Integer month = findMonth(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Venetan month or seasonal descriptor: " + monthName);
		return month;
	}

	/** @return	The month (or the season) that a name says, {@code null} if it is not one (see {@link #resolveMonth(String)}). */
	private static Integer findMonth(final String monthName){
		Integer month = MONTHS.get(monthName);
		if(month != null)
			return month;

		String standard = monthName;
		for(final String[] variant : TYPOGRAPHIC_VARIANTS)
			standard = StringUtils.replace(standard, variant[0], variant[1]);
		month = MONTHS.get(standard);
		if(month == null)
			month = MONTHS.get(ARO_SUFFIX.matcher(standard)
				.replaceAll("èr"));
		return month;
	}

	/**
	 * Checks a dual year: it is valid only in January and February (the months of the civil year after the Venetan one),
	 * and the second (civil) year must follow the first one, written fully or with its last 1 to 3 digits.
	 */
	private static void validateDualYear(final String rawDate, final String year, final String dualYear, final int month,
			final boolean beforeChrist){
		if(month < FIRST_MONTH_AFTER_NEW_YEAR || month > LAST_VENETAN_MONTH)
			throw new IllegalArgumentException("A dual year is valid only in January and February: " + rawDate);

		final long next = Long.parseLong(year) + (beforeChrist? -1: 1);
		final int length = dualYear.length();
		final long modulus = (length >= 4? Long.MAX_VALUE: (long)Math.pow(10, length));
		if(Long.parseLong(dualYear) != Math.floorMod(next, modulus))
			throw new IllegalArgumentException("The second year of a dual year must follow the first one: " + rawDate);
	}

	private static int toAstronomicalYear(final String year, final boolean beforeChrist){
		if(year.length() > MAX_YEAR_DIGITS)
			throw new IllegalArgumentException("Year too large: " + year);

		return DateFormulas.toAstronomicalYear(Integer.parseInt(year), beforeChrist);
	}

	/** Maximum number of digits of a year. */
	private static final int MAX_YEAR_DIGITS = 6;

}
