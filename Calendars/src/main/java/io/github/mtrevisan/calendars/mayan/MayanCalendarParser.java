package io.github.mtrevisan.calendars.mayan;

import io.github.mtrevisan.calendars.CalendarType;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parser for Mayan Long Count date notations and cardinal solar station expressions.
 * Implements safe, exception-free Unicode sequence properties.
 */
public final class MayanCalendarParser{

	private static final Map<String, Integer> STATIONS = new HashMap<>();
	static{
		// --- AUTUMN / WESTERN STATION (Chik'in / Aligned to Autumnal Equinox) ---
		STATIONS.put("chikin", CalendarType.SEASON_AUTUMN);    // Plain ASCII variant
		STATIONS.put("chik'in", CalendarType.SEASON_AUTUMN);   // Correct Mayan glottalized spelling
		STATIONS.put("chikin-k'in", CalendarType.SEASON_AUTUMN); // Full phrase ("Where the sun enters/sets")
		STATIONS.put("chikkin", CalendarType.SEASON_AUTUMN);   // Common phonetic variant
		STATIONS.put("ek", CalendarType.SEASON_AUTUMN);        // Black color token bound to the West

		// --- WINTER / SOUTHERN STATION (Nohol / Aligned to Hibernal Solstice) ---
		STATIONS.put("nohol", CalendarType.SEASON_WINTER);     // Correct Mayan noun for "South"
		STATIONS.put("noohol", CalendarType.SEASON_WINTER);    // Classic epigraphic spelling variant
		STATIONS.put("kan", CalendarType.SEASON_WINTER);        // Plain ASCII color token
		STATIONS.put("k'an", CalendarType.SEASON_WINTER);       // Yellow color token bound to the South

		// --- SPRING / EASTERN STATION (Lik'in / Aligned to Vernal Equinox) ---
		STATIONS.put("likin", CalendarType.SEASON_SPRING);     // Plain ASCII variant
		STATIONS.put("lik'in", CalendarType.SEASON_SPRING);    // Correct Mayan glottalized spelling
		STATIONS.put("lak'in", CalendarType.SEASON_SPRING);    // Classical epigraphic text variant
		STATIONS.put("ch'een", CalendarType.SEASON_SPRING);    // Transitional solar station notation
		STATIONS.put("chac", CalendarType.SEASON_SPRING);      // Plain ASCII color token
		STATIONS.put("chac", CalendarType.SEASON_SPRING);      // Red color token bound to the East
		STATIONS.put("ch'ak", CalendarType.SEASON_SPRING);

		// --- SUMMER / NORTHERN STATION (Xaman / Aligned to Estival Solstice) ---
		STATIONS.put("xaman", CalendarType.SEASON_SUMMER);     // FIXED: Correct Mayan noun for "North"
		STATIONS.put("shaman", CalendarType.SEASON_SUMMER);    // Modern phonetic Spanish spelling variant
		STATIONS.put("yax", CalendarType.SEASON_SUMMER);       // Green/Blue cosmic center prefix (Your original anchor)
		STATIONS.put("yaxk'in", CalendarType.SEASON_SUMMER);   // "New Sun / Green Sun" historical summer terminal name
		STATIONS.put("sac", CalendarType.SEASON_SUMMER);       // Plain ASCII color token
		STATIONS.put("sac", CalendarType.SEASON_SUMMER);       // White color token bound to the North
	}

	// Robust Unicode pattern handling custom text delimiters, spaces, dots, and numbers
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:([\\p{L}\\p{M}\\s'-]+?)\\s+)?([\\d.]+)");


	private MayanCalendarParser(){}


	/**
	 * Parses a raw Mayan string token sequence into a structural data container.
	 *
	 * @param rawDate The date expression (e.g., "13.0.0.0.0" or "Yax 2026")
	 * @return A configured MayanDateInput block carrying system parameters
	 * @throws IllegalArgumentException If text layout rules or token boundaries fail validation
	 */
	public static MayanDateInput parse(final String rawDate){
		final String normalized = StringUtils.normalizeSpace(rawDate);
		final Matcher matcher = DATE_PATTERN.matcher(normalized);

		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Mayan calendar string format: " + rawDate);

		final String stationGroup = matcher.group(1);
		final String numericGroup = matcher.group(2);

		if(stationGroup != null){
			// SEASONAL STATION PATHWAY (e.g., "Yax 2026")
			final String stationName = stationGroup.toLowerCase(Locale.ROOT).trim();
			final Integer stationFlag = STATIONS.get(stationName);

			if(stationFlag == null)
				throw new IllegalArgumentException("Unknown Mayan solar station or calendar token: " + stationName);

			// Store the flag inside the Baktun position, and the target Gregorian year in the Kin day position
			final int targetYear = Integer.parseInt(numericGroup);
			return new MayanDateInput(stationFlag, 0, 0, 0, targetYear);
		}
		else{
			// STANDARD LONG COUNT PATHWAY (e.g., "13.0.0.0.0")
			final String[] components = StringUtils.split(numericGroup, '.');
			if(components.length < 5)
				throw new IllegalArgumentException("Standard Mayan Long Count expects exactly 5 sequential dots cycles.");

			final int baktun = Integer.parseInt(components[0]);
			final int katun = Integer.parseInt(components[1]);
			final int tun = Integer.parseInt(components[2]);
			final int uinal = Integer.parseInt(components[3]);
			final int kin = Integer.parseInt(components[4]);

			return new MayanDateInput(baktun, katun, tun, uinal, kin);
		}
	}

}
