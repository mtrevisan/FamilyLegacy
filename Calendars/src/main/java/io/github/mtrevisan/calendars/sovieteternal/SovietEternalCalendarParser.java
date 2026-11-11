/**
*
*/
package io.github.mtrevisan.calendars.sovieteternal;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.MonthLengths;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for Soviet Eternal calendar date expressions using exact astronomical mapping flags.
 * Supports canonical month names, administrative holiday markers, and native seasonal terms.
 */
public final class SovietEternalCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: January (Январь)
		MONTHS.put("январь", 1);           // Native Cyrillic script
		MONTHS.put("yanvar", 1);            // Standard Romanization
		MONTHS.put("januar", 1);            // Alternate European phonetic variant

		// Month 2: February (Февраль)
		MONTHS.put("февраль", 2);
		MONTHS.put("fevral", 2);
		MONTHS.put("fevral'", 2);           // Scientific transliteration showing soft sign

		// Month 3: March (Март)
		MONTHS.put("март", 3);
		MONTHS.put("mart", 3);

		// Month 4: April (Апрель)
		MONTHS.put("апрель", 4);
		MONTHS.put("aprel", 4);
		MONTHS.put("aprel'", 4);

		// Month 5: May (Май)
		MONTHS.put("май", 5);
		MONTHS.put("may", 5);
		MONTHS.put("maj", 5);

		// Month 6: June (Июнь)
		MONTHS.put("июнь", 6);
		MONTHS.put("iyun", 6);
		MONTHS.put("ijun", 6);

		// Month 7: July (Июль)
		MONTHS.put("июль", 7);
		MONTHS.put("iyul", 7);
		MONTHS.put("ijul", 7);

		// Month 8: August (Август)
		MONTHS.put("август", 8);
		MONTHS.put("avgust", 8);

		// Month 9: September (Сентябрь)
		MONTHS.put("сентябрь", 9);
		MONTHS.put("sentyabr", 9);
		MONTHS.put("sentjabr", 9);

		// Month 10: October (Октябрь)
		MONTHS.put("октябрь", 10);
		MONTHS.put("oktyabr", 10);
		MONTHS.put("oktjabr", 10);

		// Month 11: November (Ноябрь)
		MONTHS.put("ноябрь", 11);
		MONTHS.put("noyabr", 11);
		MONTHS.put("nojabr", 11);

		// Month 12: December (Декабрь)
		MONTHS.put("декабрь", 12);
		MONTHS.put("dekabr", 12);
		MONTHS.put("dekabr'", 12);

		// Autumn
		MONTHS.put("осень", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("осен", CalendarSeason.SEASON_AUTUMN);            // Truncated phonetic token
		MONTHS.put("osen", CalendarSeason.SEASON_AUTUMN);

		// Winter
		MONTHS.put("зима", CalendarSeason.SEASON_WINTER);
		MONTHS.put("zima", CalendarSeason.SEASON_WINTER);

		// Spring
		MONTHS.put("весна", CalendarSeason.SEASON_SPRING);
		MONTHS.put("vesna", CalendarSeason.SEASON_SPRING);

		// Summer
		MONTHS.put("лето", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("leto", CalendarSeason.SEASON_SUMMER);
	}

	/**
	 * The five revolutionary holidays that stood outside the five-day weeks of the calendar (1929-1931), on their Gregorian
	 * day: Lenin Day (22 January), the Labor Days (1 and 2 May), and the Industry Days (7 and 8 November); written "2 labor
	 * days 1930" for the second one. The leap day is 29 February, valid in the leap years only.
	 */
	private static final Map<String, NamedMonthDateParser.NamedDay> NAMED_DAYS = Map.of(
		"день ленина", new NamedMonthDateParser.NamedDay(1, 22, 1),
		"lenin day", new NamedMonthDateParser.NamedDay(1, 22, 1),
		"дни труда", new NamedMonthDateParser.NamedDay(5, 1, 2),
		"labor days", new NamedMonthDateParser.NamedDay(5, 1, 2),
		"labour days", new NamedMonthDateParser.NamedDay(5, 1, 2),
		"индустриальные дни", new NamedMonthDateParser.NamedDay(11, 7, 2),
		"industry days", new NamedMonthDateParser.NamedDay(11, 7, 2),
		"високосный день", new NamedMonthDateParser.NamedDay(2, 29, 1));
	/** The names that are known, but do not identify a day. */
	private static final Map<String, String> REJECTED = Map.ofEntries(
		Map.entry("revolutionary-holidays", "The collective name of the revolutionary holidays does not identify a day"),
		Map.entry("holiday-days", "The collective name of the revolutionary holidays does not identify a day"),
		Map.entry("желтый", "A shift colour of the production week is not a date"),
		Map.entry("yellow", "A shift colour of the production week is not a date"),
		Map.entry("розовый", "A shift colour of the production week is not a date"),
		Map.entry("pink", "A shift colour of the production week is not a date"),
		Map.entry("красный", "A shift colour of the production week is not a date"),
		Map.entry("red", "A shift colour of the production week is not a date"),
		Map.entry("фиолетовый", "A shift colour of the production week is not a date"),
		Map.entry("purple", "A shift colour of the production week is not a date"),
		Map.entry("зеленый", "A shift colour of the production week is not a date"),
		Map.entry("green", "A shift colour of the production week is not a date"));

	private static final NamedMonthDateParser PARSER =NamedMonthDateParser.builder("Soviet Eternal")
		.months(MONTHS)
		.namedDays(NAMED_DAYS)
		.rejected(REJECTED)
		.monthLengths(MonthLengths::gregorian)
		.build();


	private SovietEternalCalendarParser(){}


	/**
	 * Parses a raw Soviet Eternal text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "7 ноябрь 1930")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
