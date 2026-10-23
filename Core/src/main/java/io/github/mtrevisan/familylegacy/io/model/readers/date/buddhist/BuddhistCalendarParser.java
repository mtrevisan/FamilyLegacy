package io.github.mtrevisan.familylegacy.io.model.readers.date.buddhist;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Buddhist calendar date expressions using exact astronomical mapping flags.
 * Supports standard month names, regional scripts, and localized seasonal markers.
 */
public final class BuddhistCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Standard Months (English transliterations and localized Thai definitions)
		MONTHS.put("makarakhom", 1);
		MONTHS.put("มกราคม", 1);
		MONTHS.put("kumphaphan", 2);
		MONTHS.put("กุมภาพันธ์", 2);
		MONTHS.put("minakhom", 3);
		MONTHS.put("มีนาคม", 3);
		MONTHS.put("mesayon", 4);
		MONTHS.put("เมษายน", 4);
		MONTHS.put("พฤษภาคม", 5);
		MONTHS.put("phruetsaphakhom", 5);
		MONTHS.put("mithunayon", 6);
		MONTHS.put("มิถุนายน", 6);
		MONTHS.put("karakhadakhom", 7);
		MONTHS.put("กรกฎาคม", 7);
		MONTHS.put("singhakhom", 8);
		MONTHS.put("สิงหาคม", 8);
		MONTHS.put("kanyayon", 9);
		MONTHS.put("กันยายน", 9);
		MONTHS.put("tulakhom", 10);
		MONTHS.put("ตุลาคม", 10);
		MONTHS.put("phruetsachikayon", 11);
		MONTHS.put("พฤศจิกายน", 11);
		MONTHS.put("thanwakhom", 12);
		MONTHS.put("ธันวาคม", 12);

		// Autumn / Autumnal Equinox cross-point alignment
		MONTHS.put("osen", CalendarType.SEASON_AUTUMN);
		// Winter / Hibernal Solstice cross-point alignment (Tied culturally to Hemanta/Cold cycle)
		MONTHS.put("hemanta", CalendarType.SEASON_WINTER);
		MONTHS.put("ru-du-naow", CalendarType.SEASON_WINTER);
		// Spring / Vernal Equinox cross-point alignment
		// Summer / Estival Solstice cross-point alignment (Tied culturally to Kimmhana/Hot cycle)
		MONTHS.put("kimmhana", CalendarType.SEASON_SUMMER);
		MONTHS.put("ru-du-nao", CalendarType.SEASON_SUMMER);
	}

	// Robust, exception-free Unicode pattern supporting optional day prefix and localized alphabets
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:(\\d+)\\s+)?([\\p{L}\\p{M}\\s-]+?)\\s+(\\d+)");


	private BuddhistCalendarParser(){}


	/**
	 * Parses a raw Buddhist text expression into a validated structural container using exact flags.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "Kimmhana 2500")
	 * @return a configured BuddhistDateInput instance
	 * @throws IllegalArgumentException if the text layout expression matches an invalid layout format
	 */
	public static BuddhistDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);

		final Matcher matcher = DATE_PATTERN.matcher(normalized);
		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Buddhist date format: " + rawDate);

		final String dayGroup = matcher.group(1);
		final String monthName = matcher.group(2)
			.toLowerCase(Locale.ROOT)
			.trim();
		final int year = Integer.parseInt(matcher.group(3));

		final Integer month = MONTHS.get(monthName);
		if(month == null)
			throw new IllegalArgumentException("Unknown Buddhist month or seasonal descriptor: " + monthName);

		// A missing day (month and year only, or a seasonal record) is kept as 0
		final int day = (dayGroup != null? Integer.parseInt(dayGroup): 0);

		if(dayGroup != null && month > 0 && (day < 1 || day > 31))
			throw new IllegalArgumentException("Day variable breaches standard monthly boundaries.");

		return new BuddhistDateInput(year, month, day);
	}

}
