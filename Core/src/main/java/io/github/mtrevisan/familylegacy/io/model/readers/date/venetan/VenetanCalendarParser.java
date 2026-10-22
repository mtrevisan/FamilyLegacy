package io.github.mtrevisan.familylegacy.io.model.readers.date.venetan;

import io.github.mtrevisan.familylegacy.io.model.readers.date.UniversalDateConverter;
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
		MONTHS.put("auton", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("autun", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("autuno", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("otun", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("otuno", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("utun", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("utuno", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("ferdima", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("fardima", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("farnima", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("fraima", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("frima", UniversalDateConverter.SEASON_AUTUMN);
		MONTHS.put("fredima", UniversalDateConverter.SEASON_AUTUMN);
		// Invèrno (Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("invèrno", UniversalDateConverter.SEASON_WINTER);
		MONTHS.put("invièrno", UniversalDateConverter.SEASON_WINTER);
		MONTHS.put("inbèrno", UniversalDateConverter.SEASON_WINTER);
		MONTHS.put("divèr", UniversalDateConverter.SEASON_WINTER);
		// Vèrta (Spring) -> Maps to Vernal Equinox flag
		MONTHS.put("primavera", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("primavèra", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("primavèrta", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("vèrta", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("insuta", UniversalDateConverter.SEASON_SPRING);
		MONTHS.put("ansuda", UniversalDateConverter.SEASON_SPRING);
		// Istà (Summer) -> Maps to Estival Solstice flag
		MONTHS.put("istade", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("istae", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("istà", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("instade", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("instae", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("instà", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("estade", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("estae", UniversalDateConverter.SEASON_SUMMER);
		MONTHS.put("està", UniversalDateConverter.SEASON_SUMMER);
	}

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
		final String normalized = StringUtils.normalizeSpace(rawDate)
			.toLowerCase(Locale.ROOT);

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
		monthName = monthName.replaceAll("aro?", "èr");

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Venetian month or seasonal descriptor: " + monthName);

		// Operational fallback for seasons
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		// Julian framework boundary verification
		if(month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day count violates standard Julian calendar monthly limits.");

		return new VenetanDateInput(year, month, day);
	}

}
