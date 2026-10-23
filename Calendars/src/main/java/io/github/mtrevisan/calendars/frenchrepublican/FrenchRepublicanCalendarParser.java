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
		// --- AUTUMN MONTHS (Mois d'automne) ---

		// Month 1: Vendémiaire (Vintage Month)
		MONTHS.put("vendémiaire", 1);
		MONTHS.put("vendemiaire", 1);
		MONTHS.put("vend.", 1);              // Common historical abbreviation
		MONTHS.put("vendem", 1);             // Alternate text abbreviation

		// Month 2: Brumaire (Fog Month)
		MONTHS.put("brumaire", 2);
		MONTHS.put("brum.", 2);              // Common historical abbreviation
		MONTHS.put("brum", 2);

		// Month 3: Frimaire (Sleet Month)
		MONTHS.put("frimaire", 3);
		MONTHS.put("frim.", 3);              // Common historical abbreviation
		MONTHS.put("frim", 3);

		// --- WINTER MONTHS (Mois d'hiver) ---

		// Month 4: Nivôse (Snow Month)
		MONTHS.put("nivôse", 4);
		MONTHS.put("nivose", 4);
		MONTHS.put("niv.", 4);               // Common historical abbreviation
		MONTHS.put("nivos", 4);

		// Month 5: Pluviôse (Rain Month)
		MONTHS.put("pluviôse", 5);
		MONTHS.put("pluviose", 5);
		MONTHS.put("pluv.", 5);              // Common historical abbreviation
		MONTHS.put("pluvios", 5);

		// Month 6: Ventôse (Wind Month)
		MONTHS.put("ventôse", 6);
		MONTHS.put("ventose", 6);
		MONTHS.put("vent.", 6);              // Common historical abbreviation
		MONTHS.put("ventos", 6);

		// --- SPRING MONTHS (Mois de printemps) ---

		// Month 7: Germinal (Seed Month)
		MONTHS.put("germinal", 7);
		MONTHS.put("germ.", 7);              // Common historical abbreviation
		MONTHS.put("germ", 7);

		// Month 8: Floréal (Blossom Month)
		MONTHS.put("floréal", 8);
		MONTHS.put("floreal", 8);
		MONTHS.put("flor.", 8);              // Common historical abbreviation
		MONTHS.put("flor", 8);

		// Month 9: Prairial (Meadow Month)
		MONTHS.put("prairial", 9);
		MONTHS.put("prair.", 9);             // Common historical abbreviation
		MONTHS.put("prair", 9);

		// --- SUMMER MONTHS (Mois d'été) ---

		// Month 10: Messidor (Harvest Month)
		MONTHS.put("messidor", 10);
		MONTHS.put("mess.", 10);             // Common historical abbreviation
		MONTHS.put("mess", 10);

		// Month 11: Thermidor (Heat Month)
		MONTHS.put("thermidor", 11);
		MONTHS.put("therm.", 11);            // Common historical abbreviation
		MONTHS.put("therm", 11);
		MONTHS.put("fervidor", 11);          // Rare early alternative name variant

		// Month 12: Fructidor (Fruit Month)
		MONTHS.put("fructidor", 12);
		MONTHS.put("fruct.", 12);            // Common historical abbreviation
		MONTHS.put("fruct", 12);

		// --- COMPLEMENTARY DAYS / SANS-CULOTTIDES (Jours complémentaires) ---

		// Month 13: Collective names
		MONTHS.put("sansculottides", 13);
		MONTHS.put("sans-culottides", 13);
		MONTHS.put("complémentaires", 13);
		MONTHS.put("complementaires", 13);
		MONTHS.put("jours complémentaires", 13);
		MONTHS.put("sans-culottides-days", 13);

		// Month 13: Specific legal individual names of the 5/6 festival days
		MONTHS.put("jour de la vertu", 13);   // Day 1: Virtue Day
		MONTHS.put("jour du génie", 13);     // Day 2: Genius Day
		MONTHS.put("jour du genie", 13);
		MONTHS.put("jour du travail", 13);   // Day 3: Labour Day
		MONTHS.put("jour de l'opinion", 13); // Day 4: Opinion Day
		MONTHS.put("jour des récompenses", 13); // Day 5: Rewards Day
		MONTHS.put("jour des recompenses", 13);
		MONTHS.put("jour de la révolution", 13); // Day 6: Leap-year Revolutionary Day
		MONTHS.put("jour de la revolution", 13);
		MONTHS.put("franciade", 13);         // The leap-year quadrennial cycle name


		// =============================================================================
		// REPUBLICAN SEASONAL ALIGNMENT (Aligned to structural Equinox/Solstice grids)
		// =============================================================================

		// Autumn
		MONTHS.put("automne", CalendarType.SEASON_AUTUMN);
		MONTHS.put("automne-season", CalendarType.SEASON_AUTUMN);

		// Winter
		MONTHS.put("hiver", CalendarType.SEASON_WINTER);
		MONTHS.put("hiver-season", CalendarType.SEASON_WINTER);

		// Spring
		MONTHS.put("printemps", CalendarType.SEASON_SPRING);
		MONTHS.put("printemps-season", CalendarType.SEASON_SPRING);

		// Summer
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
