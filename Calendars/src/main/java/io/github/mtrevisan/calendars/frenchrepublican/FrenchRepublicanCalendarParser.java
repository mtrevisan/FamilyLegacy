package io.github.mtrevisan.calendars.frenchrepublican;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for French Republican calendar date expressions using exact astronomical mapping flags.
 * Supports revolutionary months, Sansculottides complementary days, and seasonal markers.
 */
public final class FrenchRepublicanCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Autumn Months
		MONTHS.put("vendémiaire", 1);
		MONTHS.put("vendemiaire", 1);
		MONTHS.put("brumaire", 2);
		MONTHS.put("frimaire", 3);
		// Winter Months
		MONTHS.put("nivôse", 4);
		MONTHS.put("nivose", 4);
		MONTHS.put("pluviôse", 5);
		MONTHS.put("pluviose", 5);
		MONTHS.put("ventôse", 6);
		MONTHS.put("ventose", 6);
		// Spring Months
		MONTHS.put("germinal", 7);
		MONTHS.put("floréal", 8);
		MONTHS.put("floreal", 8);
		MONTHS.put("prairial", 9);
		// Summer Months
		MONTHS.put("messidor", 10);
		MONTHS.put("thermidor", 11);
		MONTHS.put("fructidor", 12);
		// Complementary Year-End Days
		MONTHS.put("sansculottides", 13);
		MONTHS.put("complémentaires", 13);
		MONTHS.put("complementaires", 13);

		// Automne (Autumn) -> Maps to Autumnal Equinox flag
		MONTHS.put("automne", CalendarType.SEASON_AUTUMN);
		MONTHS.put("automne-season", CalendarType.SEASON_AUTUMN);
		// Hiver (Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("hiver", CalendarType.SEASON_WINTER);
		MONTHS.put("hiver-season", CalendarType.SEASON_WINTER);
		// Printemps (Spring) -> Maps to Vernal Equinox flag
		MONTHS.put("printemps", CalendarType.SEASON_SPRING);
		MONTHS.put("printemps-season", CalendarType.SEASON_SPRING);
		// Été (Summer) -> Maps to Estival Solstice flag
		MONTHS.put("été", CalendarType.SEASON_SUMMER);
		MONTHS.put("ete", CalendarType.SEASON_SUMMER);
		MONTHS.put("été-season", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private FrenchRepublicanCalendarParser(){}

	/**
	 * Parses a raw French Republican string expression into a validated architectural container using exact flags.
	 *
	 * @param rawDate the string representing the Revolutionary date
	 * @return a configured FrenchRepublicanDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static FrenchRepublicanDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		// A year alone (e.g. the start of a decade or century) stands for the first day of the year
		if(StringUtils.isNumeric(normalized))
			return new FrenchRepublicanDateInput(Integer.parseInt(normalized), 0, 0);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid French Republican date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown French Republican month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && month <= 12 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Standard Revolutionary months consist strictly of 30 days.");
		if(dayGroup != null && month == 13 && (day < 1 || day > 6))
			throw new IllegalArgumentException("Sansculottides complementary days are bound between 1 and 6 days.");

		return new FrenchRepublicanDateInput(year, month, day);
	}

}
