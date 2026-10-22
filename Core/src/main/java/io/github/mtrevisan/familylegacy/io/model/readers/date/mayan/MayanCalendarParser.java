package io.github.mtrevisan.familylegacy.io.model.readers.date.mayan;

import io.github.mtrevisan.familylegacy.io.model.readers.date.CalendarType;
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
		// Chikin (West / Autumnal Equinox structural cross-point)
		STATIONS.put("chikin", CalendarType.SEASON_AUTUMN);
		STATIONS.put("chik'in", CalendarType.SEASON_AUTUMN);
		// K'an / Nohol (South / Hibernal Solstice structural cross-point)
		STATIONS.put("kan", CalendarType.SEASON_WINTER);
		STATIONS.put("k'an", CalendarType.SEASON_WINTER);
		// Lik'in (East / Vernal Equinox structural cross-point)
		STATIONS.put("likin", CalendarType.SEASON_SPRING);
		STATIONS.put("lik'in", CalendarType.SEASON_SPRING);
		// Yax (North / Estival Solstice structural cross-point)
		STATIONS.put("yax", CalendarType.SEASON_SUMMER);
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
