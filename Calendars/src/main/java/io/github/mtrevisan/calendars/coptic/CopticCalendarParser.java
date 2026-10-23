package io.github.mtrevisan.calendars.coptic;

import io.github.mtrevisan.calendars.CalendarType;
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
		// Standard Coptic Months (Transliterated Bohairic/Sahidic parameters)
		MONTHS.put("thout", 1);
		MONTHS.put("tout", 1);
		MONTHS.put("baba", 2);
		MONTHS.put("paopi", 2);
		MONTHS.put("hatour", 3);
		MONTHS.put("hathor", 3);
		MONTHS.put("kiahk", 4);
		MONTHS.put("koiak", 4);
		MONTHS.put("toba", 5);
		MONTHS.put("tobi", 5);
		MONTHS.put("amshir", 6);
		MONTHS.put("meshir", 6);
		MONTHS.put("baramhat", 7);
		MONTHS.put("paremhat", 7);
		MONTHS.put("baramouda", 8);
		MONTHS.put("paremoude", 8);
		MONTHS.put("bashans", 9);
		MONTHS.put("pashons", 9);
		MONTHS.put("baouna", 10);
		MONTHS.put("paoni", 10);
		MONTHS.put("abep", 11);
		MONTHS.put("epip", 11);
		MONTHS.put("mesra", 12);
		MONTHS.put("mesori", 12);
		// Intercalary Month (Little Month / Epagomenal Days)
		MONTHS.put("pi kogi enavot", 13);
		MONTHS.put("nasiee", 13);
		MONTHS.put("el-nasie", 13);

		// NATIVE COPTIC AGRICULTURAL SEASONAL FLAGS
		// Ahia (Inundation / Autumn) -> Maps to Autumnal Equinox flag
		MONTHS.put("ahia", CalendarType.SEASON_AUTUMN);
		MONTHS.put("akhet", CalendarType.SEASON_AUTUMN);
		// Toka (Sowing / Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("toka", CalendarType.SEASON_WINTER);
		MONTHS.put("peret", CalendarType.SEASON_WINTER);
		// Spring / Vernal Equinox cross-point alignment
		// Shom (Harvest / Summer) -> Maps to Estival Solstice flag
		MONTHS.put("shom", CalendarType.SEASON_SUMMER);
		MONTHS.put("shemu", CalendarType.SEASON_SUMMER);
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
