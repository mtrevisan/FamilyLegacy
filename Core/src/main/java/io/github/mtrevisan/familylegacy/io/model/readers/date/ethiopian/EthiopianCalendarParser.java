package io.github.mtrevisan.familylegacy.io.model.readers.date.ethiopian;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Ethiopian calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, transliterations, and native Amharic seasonal markers.
 */
public final class EthiopianCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Ethiopian Months (Transliterated Amharic parameters)
		MONTHS.put("mäskeräm", 1);
		MONTHS.put("meskerem", 1);
		MONTHS.put("ṭəqəmt", 2);
		MONTHS.put("tekemt", 2);
		MONTHS.put("ḫədar", 3);
		MONTHS.put("hedar", 3);
		MONTHS.put("taḫśaś", 4);
		MONTHS.put("tahasas", 4);
		MONTHS.put("ṭərr", 5);
		MONTHS.put("ter", 5);
		MONTHS.put("yäkatit", 6);
		MONTHS.put("yekatit", 6);
		MONTHS.put("maggabit", 7);
		MONTHS.put("megabit", 7);
		MONTHS.put("miyazya", 8);
		MONTHS.put("miyazia", 8);
		MONTHS.put("gənboṭ", 9);
		MONTHS.put("genbot", 9);
		MONTHS.put("sänē", 10);
		MONTHS.put("sene", 10);
		MONTHS.put("ḥamle", 11);
		MONTHS.put("hamle", 11);
		MONTHS.put("nähase", 12);
		MONTHS.put("nehase", 12);

		// Intercalary Month (Little Month / Epagomenal Days)
		MONTHS.put("pagumēn", 13);
		MONTHS.put("pagumen", 13);

		// Bega (The Dry Season / Winter)
		MONTHS.put("bega", CalendarType.SEASON_WINTER);
		// Tseday / Belg (Spring Transition) -> Maps to Vernal Equinox flag
		MONTHS.put("tseday", CalendarType.SEASON_SPRING);
		MONTHS.put("belg", CalendarType.SEASON_SPRING);
		// Keremt (The Heavy Rainy Season / Summer) -> Maps to Estival Solstice flag
		MONTHS.put("keremt", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private EthiopianCalendarParser(){}


	/**
	 * Parses a raw Ethiopian text string into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Keremt 2015")
	 * @return a configured EthiopianDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static EthiopianDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Ethiopian date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Ethiopian month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Ethiopian months consist strictly of 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 6))
			throw new IllegalArgumentException("Pagumēn intercalary days are bound between 1 and 6 days.");

		return new EthiopianDateInput(year, month, day);
	}

}
