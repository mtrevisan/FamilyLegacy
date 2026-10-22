package io.github.mtrevisan.familylegacy.io.model.readers.date.hebrew;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Hebrew calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, leap variants, and traditional Tekufah seasonal markers.
 */
public final class HebrewCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Hebrew Months (Civil order starting from Tishrei)
		MONTHS.put("tishrei", 1);
		MONTHS.put("tishri", 1);
		MONTHS.put("cheshvan", 2);
		MONTHS.put("heshvan", 2);
		MONTHS.put("marcheshvan", 2);
		MONTHS.put("kislev", 3);
		MONTHS.put("tevet", 4);
		MONTHS.put("tebeth", 4);
		MONTHS.put("shevat", 5);
		MONTHS.put("shebat", 5);
		// Adar in common years, Adar I in leap years
		MONTHS.put("adar", 6);
		MONTHS.put("adar i", 6);
		MONTHS.put("adar aleph", 6);
		MONTHS.put("adar ii", 7);
		MONTHS.put("adar bet", 7);
		// Shifting position index maps based on leap states
		MONTHS.put("nisan", 8);
		MONTHS.put("iyar", 9);
		MONTHS.put("sivan", 10);
		MONTHS.put("tammuz", 11);
		MONTHS.put("tamuz", 11);
		MONTHS.put("av", 12);
		MONTHS.put("elul", 13);

		// Tekufat Tishrei (Autumnal Equinox) -> Maps to Autumnal Equinox flag
		MONTHS.put("tekufat tishrei", CalendarType.SEASON_AUTUMN);
		MONTHS.put("tekufat tishri", CalendarType.SEASON_AUTUMN);
		// Tekufat Tevet (Hibernal Solstice) -> Maps to Hibernal Solstice flag
		MONTHS.put("tekufat tevet", CalendarType.SEASON_WINTER);
		MONTHS.put("tekufat tebeth", CalendarType.SEASON_WINTER);
		// Tekufat Nisan (Vernal Equinox) -> Maps to Vernal Equinox flag
		MONTHS.put("tekufat nisan", CalendarType.SEASON_SPRING);
		// Tekufat Tammuz (Estival Solstice) -> Maps to Estival Solstice flag
		MONTHS.put("tekufat tammuz", CalendarType.SEASON_SUMMER);
		MONTHS.put("tekufat tamuz", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private HebrewCalendarParser(){}


	/**
	 * Parses a raw Hebrew text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Tekufat Nisan 5786")
	 * @return a configured HebrewDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static HebrewDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Hebrew date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Hebrew month or Tekufah descriptor: " + monthName);

		// Operational fallback for standalone seasonal records
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 1);

		if(month > 0 && (day < 1 || day > 30))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new HebrewDateInput(year, month, day);
	}

}
