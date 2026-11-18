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
package io.github.mtrevisan.calendars.mayan;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.text.ScriptText;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
		register("chikin", CalendarSeason.SEASON_AUTUMN);		// Plain ASCII variant
		register("chik'in", CalendarSeason.SEASON_AUTUMN);		// Correct Mayan glottalized spelling
		register("chikin-k'in", CalendarSeason.SEASON_AUTUMN);	// Full phrase ("Where the sun enters/sets")
		register("chikkin", CalendarSeason.SEASON_AUTUMN);		// Common phonetic variant
		register("ek", CalendarSeason.SEASON_AUTUMN);				// Black color token bound to the West

		// --- WINTER / SOUTHERN STATION (Nohol / Aligned to Hibernal Solstice) ---
		register("nohol", CalendarSeason.SEASON_WINTER);			// Correct Mayan noun for "South"
		register("noohol", CalendarSeason.SEASON_WINTER);		// Classic epigraphic spelling variant
		register("kan", CalendarSeason.SEASON_WINTER);			// Plain ASCII color token
		register("k'an", CalendarSeason.SEASON_WINTER);			// Yellow color token bound to the South

		// --- SPRING / EASTERN STATION (Lik'in / Aligned to Vernal Equinox) ---
		register("likin", CalendarSeason.SEASON_SPRING);			// Plain ASCII variant
		register("lik'in", CalendarSeason.SEASON_SPRING);		// Correct Mayan glottalized spelling
		register("lak'in", CalendarSeason.SEASON_SPRING);		// Classical epigraphic text variant
		register("ch'een", CalendarSeason.SEASON_SPRING);		// Transitional solar station notation
		register("chac", CalendarSeason.SEASON_SPRING);			// Red color token bound to the East
		register("ch'ak", CalendarSeason.SEASON_SPRING);

		// --- SUMMER / NORTHERN STATION (Xaman / Aligned to Estival Solstice) ---
		register("xaman", CalendarSeason.SEASON_SUMMER);
		register("shaman", CalendarSeason.SEASON_SUMMER);		// Modern phonetic Spanish spelling variant
		register("yax", CalendarSeason.SEASON_SUMMER);			// Green/Blue cosmic center prefix (Your original anchor)
		register("yaxk'in", CalendarSeason.SEASON_SUMMER);		// "New Sun / Green Sun" historical summer terminal name
		register("sac", CalendarSeason.SEASON_SUMMER);			// White color token bound to the North
	}

	// Robust Unicode pattern handling custom text delimiters, spaces, dots, and numbers
	private static final Pattern DATE_PATTERN = Pattern.compile("(?:([\\p{L}\\p{M}\\s'-]+?)\\s+)?([\\d.]+)");

	/** The number of components of a Long Count (baktun, katun, tun, uinal, kin). */
	private static final int LONG_COUNT_COMPONENTS = 5;
	/** The number of katuns in a baktun, of tuns in a katun, and of kins in an uinal. */
	private static final int CYCLE_LENGTH = 20;
	/** The number of uinals in a tun. */
	private static final int UINALS_PER_TUN = 18;


	private static void register(final String name, final int station){
		ScriptText.register(STATIONS, name, station);
	}


	private MayanCalendarParser(){}


	/**
	 * @return the normalized names of the solar stations known by the parser
	 */
	public static Set<String> knownNames(){
		return Collections.unmodifiableSet(STATIONS.keySet());
	}

	/**
	 * Parses a raw Mayan string token sequence into a structural data container.
	 *
	 * @param rawDate The date expression (e.g., "13.0.0.0.0" or "Yax 2026")
	 * @return A configured MayanDateInput block carrying system parameters
	 * @throws IllegalArgumentException If text layout rules or token boundaries fail validation: a Long Count must have
	 * 	exactly five components, with the katun and the tun in 0-19, the uinal in 0-17, and the kin in 0-19
	 */
	public static MayanDateInput parse(final String rawDate){
		final String normalized = ScriptText.normalize(rawDate);
		final Matcher matcher = DATE_PATTERN.matcher(normalized);

		if(!matcher.matches())
			throw new IllegalArgumentException("Invalid Mayan calendar string format: " + rawDate);

		final String stationGroup = matcher.group(1);
		final String numericGroup = matcher.group(2);

		if(stationGroup != null){
			// SEASONAL STATION PATHWAY (e.g., "Yax 2026")
			final String stationName = stationGroup.trim();
			final Integer stationFlag = STATIONS.get(stationName);

			if(stationFlag == null)
				throw new IllegalArgumentException("Unknown Mayan solar station or calendar token: " + stationName);
			if(!StringUtils.isNumeric(numericGroup))
				throw new IllegalArgumentException("A solar station needs a year: " + rawDate);

			// Store the flag inside the Baktun position, and the target Gregorian year in the Kin day position
			final int targetYear = ScriptText.parseNumber(numericGroup);
			return new MayanDateInput(stationFlag, 0, 0, 0, targetYear);
		}
		else{
			// STANDARD LONG COUNT PATHWAY (e.g., "13.0.0.0.0")
			final String[] components = StringUtils.splitPreserveAllTokens(numericGroup, '.');
			if(components.length != LONG_COUNT_COMPONENTS)
				throw new IllegalArgumentException("Standard Mayan Long Count expects exactly " + LONG_COUNT_COMPONENTS
					+ " components separated by dots: " + rawDate);
			for(final String component : components)
				if(!StringUtils.isNumeric(component))
					throw new IllegalArgumentException("Invalid component '" + component + "' in the Mayan Long Count: " + rawDate);

			final int baktun = ScriptText.parseNumber(components[0]);
			final int katun = ScriptText.parseNumber(components[1]);
			final int tun = ScriptText.parseNumber(components[2]);
			final int uinal = ScriptText.parseNumber(components[3]);
			final int kin = ScriptText.parseNumber(components[4]);
			if(katun >= CYCLE_LENGTH || tun >= CYCLE_LENGTH || uinal >= UINALS_PER_TUN || kin >= CYCLE_LENGTH)
				throw new IllegalArgumentException("Mayan Long Count component out of range (katun and tun 0-19, uinal 0-17, kin 0-19): "
					+ rawDate);

			return new MayanDateInput(baktun, katun, tun, uinal, kin);
		}
	}

}
