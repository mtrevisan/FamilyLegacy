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
package io.github.mtrevisan.calendars.frenchrepublican;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for French Republican calendar date expressions using exact astronomical mapping flags.
 * Supports revolutionary months, Sansculottides complementary days, and seasonal markers.
 */
public final class FrenchRepublicanCalendarParser{

	/** The month of the complementary days (Sansculottides). */
	private static final int COMPLEMENTARY_MONTH = 13;

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


		// =============================================================================
		// REPUBLICAN SEASONAL ALIGNMENT (Aligned to structural Equinox/Solstice grids)
		// =============================================================================

		// Autumn
		MONTHS.put("automne", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("automne-season", CalendarSeason.SEASON_AUTUMN);

		// Winter
		MONTHS.put("hiver", CalendarSeason.SEASON_WINTER);
		MONTHS.put("hiver-season", CalendarSeason.SEASON_WINTER);

		// Spring
		MONTHS.put("printemps", CalendarSeason.SEASON_SPRING);
		MONTHS.put("printemps-season", CalendarSeason.SEASON_SPRING);

		// Summer
		MONTHS.put("été", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("ete", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("été-season", CalendarSeason.SEASON_SUMMER);
	}

	/**
	 * The names of the complementary days (Sansculottides), as of the decree of 24 October 1793: the days of the
	 * thirteenth month, from the first (the Virtue) to the fifth (the Rewards), and the sixth one (the Revolution, in the
	 * sextile years only). The day of a name is the one of the month, so that the Revolution is not valid in a common year.
	 */
	private static final Map<String, NamedMonthDateParser.NamedDay> NAMED_DAYS = new HashMap<>();
	static{
		registerDay(1, "jour de la vertu", "fête de la vertu", "fete de la vertu", "la vertu", "les vertus");
		registerDay(2, "jour du génie", "jour du genie", "fête du génie", "fete du genie", "le génie", "le genie");
		registerDay(3, "jour du travail", "fête du travail", "fete du travail", "le travail");
		registerDay(4, "jour de l'opinion", "fête de l'opinion", "fete de l'opinion", "l'opinion");
		registerDay(5, "jour des récompenses", "jour des recompenses", "fête des récompenses", "fete des recompenses",
			"les récompenses", "les recompenses");
		registerDay(6, "jour de la révolution", "jour de la revolution", "fête de la révolution", "fete de la revolution",
			"la révolution", "la revolution");
	}
	/** The names that are known, but do not identify a day. */
	private static final Map<String, String> REJECTED = Map.of(
		"franciade", "The Franciade is the period of four years, not a day: name the sixth complementary day");

	private static final NamedMonthDateParser PARSER =NamedMonthDateParser.builder("French Republican")
		.months(MONTHS)
		.namedDays(NAMED_DAYS)
		.rejected(REJECTED)
		.monthLengths(MonthLengths.thirtyDayMonths(5, FrenchRepublicanCalendarStrategy::isLeapYear))
		.build();


	private FrenchRepublicanCalendarParser(){}


	/** The days of the thirteenth month: the complementary day {@code day} has the given names. */
	private static void registerDay(final int day, final String... names){
		final NamedMonthDateParser.NamedDay namedDay = new NamedMonthDateParser.NamedDay(COMPLEMENTARY_MONTH, day, 1);
		for(final String name : names)
			NAMED_DAYS.put(name, namedDay);
	}


	/**
	 * Parses a raw French Republican text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "9 Thermidor 2")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
