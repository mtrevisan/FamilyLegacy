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
package io.github.mtrevisan.calendars.byzantine;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.NamedMonthDateParser;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;

import java.util.HashMap;
import java.util.Map;


/**
 * Parser for Byzantine calendar date expressions.
 * Supports structural Greek names, Latin transliterations, and localized numerical scripts.
 */
public final class ByzantineCalendarParser{

	private static final Map<String, Integer> MONTHS = new HashMap<>();
	static{
		// Month 1: September
		MONTHS.put("september", 1);
		MONTHS.put("septembrio", 1);        // Medieval Latin dative/ablative
		MONTHS.put("σεπτέμβριος", 1);       // Modern Greek
		MONTHS.put("σεπτέμβριον", 1);       // Byzantine Katharevousa form
		MONTHS.put("septembrios", 1);        // Greek Romanization
		MONTHS.put("ruyen", 1);             // Old Church Slavonic (OCS) agricultural name

		// Month 2: October
		MONTHS.put("october", 2);
		MONTHS.put("octobrio", 2);
		MONTHS.put("οκτώβριος", 2);
		MONTHS.put("ὀκτώβριος", 2);        // Polytonic/Byzantine spelling variant
		MONTHS.put("οκτώβριον", 2);         // Byzantine Katharevousa form
		MONTHS.put("oktobrios", 2);
		MONTHS.put("listopad", 2);          // Old Church Slavonic (OCS) agricultural name

		// Month 3: November
		MONTHS.put("november", 3);
		MONTHS.put("nouember", 3);          // Classical/Medieval Latin spelling
		MONTHS.put("νοέμβριος", 3);
		MONTHS.put("νοέμβριον", 3);         // Byzantine Katharevousa form
		MONTHS.put("noembrios", 3);
		MONTHS.put("gruden", 3);            // Old Church Slavonic (OCS) agricultural name

		// Month 4: December
		MONTHS.put("december", 4);
		MONTHS.put("deceamber", 4);         // Common Medieval spelling variation
		MONTHS.put("δεκέμβριος", 4);
		MONTHS.put("δεκέμβριον", 4);        // Byzantine Katharevousa form
		MONTHS.put("dekembrios", 4);
		MONTHS.put("studen", 4);            // Old Church Slavonic (OCS) agricultural name

		// Month 5: January
		MONTHS.put("january", 5);
		MONTHS.put("ianuarius", 5);         // Ecclesiastical Latin nominative
		MONTHS.put("ιανουάριος", 5);
		MONTHS.put("ιανουάριον", 5);        // Byzantine Katharevousa form
		MONTHS.put("ianouarios", 5);
		MONTHS.put("prosinets", 5);         // Old Church Slavonic (OCS) agricultural name

		// Month 6: February
		MONTHS.put("february", 6);
		MONTHS.put("februarius", 6);
		MONTHS.put("φεβρουάριος", 6);
		MONTHS.put("φεβρουάριον", 6);       // Byzantine Katharevousa form
		MONTHS.put("phebrouarios", 6);      // Phonetic/Erasmian Greek Romanization
		MONTHS.put("sechen", 6);            // Old Church Slavonic (OCS) agricultural name

		// Month 7: March
		MONTHS.put("march", 7);
		MONTHS.put("martius", 7);           // Ecclesiastical Latin nominative
		MONTHS.put("μάρτιος", 7);
		MONTHS.put("μάρτιον", 7);           // Byzantine Katharevousa form
		MONTHS.put("martios", 7);
		MONTHS.put("sukhov", 7);            // Old Church Slavonic (OCS) agricultural name

		// Month 8: April
		MONTHS.put("april", 8);
		MONTHS.put("aprilis", 8);
		MONTHS.put("απρίλιος", 8);
		MONTHS.put("απρίλιον", 8);          // Byzantine Katharevousa form
		MONTHS.put("aprilios", 8);
		MONTHS.put("tsveten", 8);           // Old Church Slavonic (OCS) agricultural name

		// Month 9: May
		MONTHS.put("may", 9);
		MONTHS.put("maius", 9);
		MONTHS.put("μάιος", 9);
		MONTHS.put("μάϊος", 9);             // Byzantine Polytonic dieresis variant
		MONTHS.put("maios", 9);
		MONTHS.put("traven", 9);            // Old Church Slavonic (OCS) agricultural name

		// Month 10: June
		MONTHS.put("june", 10);
		MONTHS.put("iunius", 10);
		MONTHS.put("ιούνιος", 10);
		MONTHS.put("ιούνιον", 10);          // Byzantine Katharevousa form
		MONTHS.put("iounios", 10);
		MONTHS.put("izok", 10);             // Old Church Slavonic (OCS) agricultural name

		// Month 11: July
		MONTHS.put("july", 11);
		MONTHS.put("iulius", 11);
		MONTHS.put("ιούλιος", 11);
		MONTHS.put("ιούλιον", 11);          // Byzantine Katharevousa form
		MONTHS.put("ioulios", 11);
		MONTHS.put("cherven", 11);          // Old Church Slavonic (OCS) agricultural name

		// Month 12: August
		MONTHS.put("august", 12);
		MONTHS.put("augustus", 12);
		MONTHS.put("αύγουστος", 12);
		MONTHS.put("αὔγουστος", 12);        // Byzantine Polytonic spelling variant
		MONTHS.put("αύγουστον", 12);         // Byzantine Katharevousa form
		MONTHS.put("avgoustos", 12);
		MONTHS.put("zarevo", 12);           // Old Church Slavonic (OCS) agricultural name


		// Autumn
		MONTHS.put("φθινόπωρο", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("φθινόπωρον", CalendarSeason.SEASON_AUTUMN); // Byzantine/Katharevousa nominal neuter
		MONTHS.put("phthinoporo", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("phthinoporon", CalendarSeason.SEASON_AUTUMN);
		MONTHS.put("osen", CalendarSeason.SEASON_AUTUMN);         // Old Church Slavonic (OCS) autumn

		// Winter
		MONTHS.put("χειμώνας", CalendarSeason.SEASON_WINTER);
		MONTHS.put("χειμών", CalendarSeason.SEASON_WINTER);     // Classical Byzantine nominative masculine
		MONTHS.put("cheimonas", CalendarSeason.SEASON_WINTER);
		MONTHS.put("cheimon", CalendarSeason.SEASON_WINTER);
		MONTHS.put("zima", CalendarSeason.SEASON_WINTER);        // Old Church Slavonic (OCS) winter

		// Spring
		MONTHS.put("άνοιξη", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ἔαρ", CalendarSeason.SEASON_SPRING);          // Pure Attic/Byzantine literary term for Spring
		MONTHS.put("ἄνοιξις", CalendarSeason.SEASON_SPRING);      // Byzantine Katharevousa nominal variant
		MONTHS.put("anoixi", CalendarSeason.SEASON_SPRING);
		MONTHS.put("ear", CalendarSeason.SEASON_SPRING);
		MONTHS.put("vesna", CalendarSeason.SEASON_SPRING);       // Old Church Slavonic (OCS) spring

		// Summer
		MONTHS.put("θέρος", CalendarSeason.SEASON_SUMMER);       // Classical Byzantine agricultural summer
		MONTHS.put("καλοκαίρι", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("theros", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("kalokairi", CalendarSeason.SEASON_SUMMER);
		MONTHS.put("leto", CalendarSeason.SEASON_SUMMER);
	}

	private static final NamedMonthDateParser PARSER = NamedMonthDateParser.builder("Byzantine")
		.months(MONTHS)
		.monthLengths(ByzantineCalendarStrategy::monthLength)
		.build();


	private ByzantineCalendarParser(){}


	/**
	 * Parses a raw Byzantine text expression into its year, month and day. A year alone stands for the first day of the
	 * year; a season (an equinox or a solstice) is a negative flag and takes no day.
	 *
	 * @param rawDate the string representing the calendar date (e.g., "15 September 6500")
	 * @return the components of the date
	 * @throws IllegalArgumentException if the text is empty, cannot be parsed, or names a day that does not exist
	 */
	public static YearMonthDay parse(final String rawDate){
		return PARSER.parse(rawDate);
	}

}
