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
package io.github.mtrevisan.calendars.swedish;

import io.github.mtrevisan.calendars.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.gregorian.GregorianDateInput;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Calendar of Sweden (and Finland).
 * <ul>
 *   <li>Julian calendar up to 28 February 1700;</li>
 *   <li>from 1 March 1700 the Swedish calendar, one day ahead of the Julian one: the leap day of 1700 was skipped to
 *   approach the Gregorian calendar gradually, but the leap days of 1704 and 1708 were not;</li>
 *   <li>in 1712 the plan was abandoned and a 30 February was added, going back to the Julian calendar;</li>
 *   <li>the Gregorian calendar was adopted in 1753, when 17 February was followed by 1 March.</li>
 * </ul>
 */
public class SwedishCalendarStrategy implements CalendarConverterPlugin{

	private static final int SWEDISH_START_YEAR = 1700;
	private static final int SWEDISH_END_YEAR = 1712;
	private static final int GREGORIAN_ADOPTION_YEAR = 1753;
	/** The last day of the Julian calendar (17 February 1753), followed by 1 March 1753 */
	private static final int JULIAN_LAST_DAY = 17;
	private static final int FEBRUARY_MAX_DAYS = 30;


	@Override
	public long parseToJdn(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate, FEBRUARY_MAX_DAYS);
		final int year = input.year();
		final int month = input.monthOrFirst();
		final int day = input.dayOrFirst();

		if(month < 0){
			final int m = switch(month){
				case CalendarType.SEASON_SPRING -> 3;  // March
				case CalendarType.SEASON_SUMMER -> 6;  // June
				case CalendarType.SEASON_AUTUMN -> 9;  // September
				case CalendarType.SEASON_WINTER -> 12; // December
				default -> 6;
			};
			final double approximateJdUT = toJdn(year, m, 1) - 0.5;
			final double targetLongitude = CalendarType.getGeneralTargetLongitude(month);

			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Swedish calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}

		validate(year, month, day);
		Indiction.validate(input, year);
		return toJdn(year, month, day);
	}

	private static void validate(final int year, final int month, final int day){
		if(month != 2)
			return;

		if(day == 30 && year != SWEDISH_END_YEAR)
			throw new IllegalArgumentException("30 February exists only in the Swedish year 1712");
		if(day == 29 && !isLeapYear(year))
			throw new IllegalArgumentException("29 February does not exist in the Swedish year " + year);
		if(year == GREGORIAN_ADOPTION_YEAR && day > JULIAN_LAST_DAY)
			throw new IllegalArgumentException("The days from 18 to 28 February 1753 were skipped by the Gregorian reform");
	}

	private static boolean isLeapYear(final int year){
		if(year > GREGORIAN_ADOPTION_YEAR)
			return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
		// the leap day of 1700 was skipped
		return (year != SWEDISH_START_YEAR && Math.floorMod(year, 4) == 0);
	}

	/**
	 * Converts a Swedish date into its Julian Day Number.
	 *
	 * @param year  the year
	 * @param month the month (1 to 12)
	 * @param day   the day of the month (up to 30 in February 1712)
	 * @return the Julian Day Number
	 */
	public static long toJdn(final int year, final int month, final int day){
		if(year > GREGORIAN_ADOPTION_YEAR || year == GREGORIAN_ADOPTION_YEAR && month > 2)
			return GregorianCalendarStrategy.toJdn(year, month, day);

		final long julianJdn = JulianCalendarStrategy.toJdn(year, month, day);
		// from 1 March 1700 to 30 February 1712 the Swedish date is one day ahead of the Julian one
		final boolean swedishPeriod = (year > SWEDISH_START_YEAR && year < SWEDISH_END_YEAR
			|| year == SWEDISH_START_YEAR && month > 2
			|| year == SWEDISH_END_YEAR && month <= 2);
		return (swedishPeriod? julianJdn - 1: julianJdn);
	}

	@Override
	public CalendarType.CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate, FEBRUARY_MAX_DAYS);
		final Map<String, Boolean> components = new LinkedHashMap<>();
		components.put("year", (input.year() != 0));
		components.put("month", (input.month() != 0));
		components.put("day", (input.day() != 0));

		final boolean isComplete = (input.year() != 0 && input.month() != 0 && input.day() != 0);
		return new CalendarType.CalendarMetadata(components, isComplete);
	}

}
