/**
*
*/
package io.github.mtrevisan.familylegacy.io.model.readers.date.sovieteternal;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Soviet Eternal calendar date expressions using exact astronomical mapping flags.
 * Supports canonical month names, administrative holiday markers, and native seasonal terms.
 */
public final class SovietEternalCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Months
		MONTHS.put("январь", 1);
		MONTHS.put("февраль", 2);
		MONTHS.put("март", 3);
		MONTHS.put("апрель", 4);
		MONTHS.put("май", 5);
		MONTHS.put("июнь", 6);
		MONTHS.put("июль", 7);
		MONTHS.put("август", 8);
		MONTHS.put("сентябрь", 9);
		MONTHS.put("октябрь", 10);
		MONTHS.put("ноябрь", 11);
		MONTHS.put("декабрь", 12);

		// NATIVE HISTORICAL SOVIET SEASONAL FLAGS
		// Ósen (Autumn) -> Maps to Autumnal Equinox flag
		MONTHS.put("осен", CalendarType.SEASON_AUTUMN);
		MONTHS.put("осень", CalendarType.SEASON_AUTUMN);
		// Zimá (Winter) -> Maps to Hibernal Solstice flag
		MONTHS.put("зима", CalendarType.SEASON_WINTER);
		// Vesná (Spring) -> Maps to Vernal Equinox flag
		MONTHS.put("весна", CalendarType.SEASON_SPRING);
		// Leto (Summer) -> Maps to Estival Solstice flag
		MONTHS.put("лето", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and world alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private SovietEternalCalendarParser(){}


	/**
	 * Parses a raw Soviet string expression into a validated architectural container using exact flags.
	 *
	 * @param rawDate the string representing the Soviet Revolutionary date
	 * @return a configured SovietDateInput instance
	 * @throws IllegalArgumentException if the expression layout is invalid
	 */
	public static SovietEternalDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches()){
			throw new IllegalArgumentException("Invalid Soviet Eternal date format: " + rawDate);
		}

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Soviet month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null) ? Integer.parseInt(dayGroup) : 0;

		if(dayGroup != null && month > 0 && (day < 1 || day > 31)){
			throw new IllegalArgumentException("Day variable breaches monthly operational limits.");
		}

		return new SovietEternalDateInput(year, month, day);
	}

}
