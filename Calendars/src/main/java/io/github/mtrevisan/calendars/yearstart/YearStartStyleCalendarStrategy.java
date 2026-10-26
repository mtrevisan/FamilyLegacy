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
package io.github.mtrevisan.calendars.yearstart;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngine;
import io.github.mtrevisan.calendars.astronomy.EphemerisEngineFactory;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarParser;
import io.github.mtrevisan.calendars.gregorian.GregorianDateInput;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * Anno Domini dating whose year does not begin on 1 January (the "stili" of Italian diplomatics).
 * <p>
 * Days and months are the ones of the Julian calendar up to 4 October 1582 and of the Gregorian calendar from
 * 15 October 1582, as adopted by the Italian states; only the numbering of the year differs from the modern one:
 * <ul>
 *   <li>an <i>anticipating</i> style starts the year N before 1 January of the year N (e.g. the Pisan style starts the
 *   year 1600 on 25 March 1599),</li>
 *   <li>a <i>postponing</i> style starts the year N after 1 January of the year N (e.g. the Florentine style starts the
 *   year 1600 on 25 March 1600, so that 10 January 1600 is 10 January 1601 in modern reckoning).</li>
 * </ul>
 * A dual year (e.g. "10 gennaio 1600/1") is already counted from 1 January, and is not shifted.
 */
public class YearStartStyleCalendarStrategy implements CalendarConverterPlugin{

	/** Style of the Incarnation, Florentine computation: the year starts on 25 March, after 1 January (Tuscany until 1749). */
	public static final YearStartStyleCalendarStrategy FLORENTINE = new YearStartStyleCalendarStrategy(3, 25, false);
	/** Style of the Incarnation, Pisan computation: the year starts on 25 March, before 1 January (Pisa until 1749). */
	public static final YearStartStyleCalendarStrategy PISAN = new YearStartStyleCalendarStrategy(3, 25, true);
	/** Style of the Nativity: the year starts on 25 December, before 1 January (Rome, Milan, and much of medieval Europe). */
	public static final YearStartStyleCalendarStrategy NATIVITY = new YearStartStyleCalendarStrategy(12, 25, true);
	/** Byzantine style: the year starts on 1 September, before 1 January (southern Italy, Apulia, Calabria). */
	public static final YearStartStyleCalendarStrategy BYZANTINE_STYLE = new YearStartStyleCalendarStrategy(9, 1, true);
	/** More veneto: the year starts on 1 March, after 1 January (Republic of Venice until 1797). */
	public static final YearStartStyleCalendarStrategy VENETAN = new YearStartStyleCalendarStrategy(3, 1, false);

	private static final CalendarReform REFORM = CalendarReform.ROME;


	private final int startMonth;
	private final int startDay;
	private final boolean anticipating;


	private YearStartStyleCalendarStrategy(final int startMonth, final int startDay, final boolean anticipating){
		this.startMonth = startMonth;
		this.startDay = startDay;
		this.anticipating = anticipating;
	}


	@Override
	public long parseToJdn(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.month() == 0){
			// a year alone stands for the first day of the year, in this style
			Indiction.validate(input, toCommonYear(year, startMonth, startDay));
			return toJdn(year, startMonth, startDay);
		}

		final int month = input.month();
		final int day = input.dayOrFirst();
		if(month < 0){
			final int m = CalendarSeason.gregorianSeasonMonth(month);
			final double approximateJdUT = toJdn(year, m, 1) - 0.5;
			final double targetLongitude = CalendarSeason.getGeneralTargetLongitude(month);

			final EphemerisEngine engine = EphemerisEngineFactory.getEngine();
			if(!engine.isAvailable())
				throw new UnsupportedOperationException("Year Start-style calendar requires the Ephemeris plugin.");

			final double exactSeasonJdUT = engine.findSolarLongitudeEvent(approximateJdUT, targetLongitude);

			return (long)Math.floor(exactSeasonJdUT + 0.5);
		}

		final int commonYear = (input.dualYear()? year: toCommonYear(year, month, day));
		input.validateLeapDay(REFORM.isLeapYear(commonYear));
		Indiction.validate(input, commonYear);
		return REFORM.toJdn(commonYear, month, day);
	}

	/**
	 * Converts a date of this style into its Julian Day Number.
	 *
	 * @param year  the year, numbered according to this style
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return the Julian Day Number
	 */
	public long toJdn(final int year, final int month, final int day){
		return REFORM.toJdn(toCommonYear(year, month, day), month, day);
	}

	/**
	 * Converts the year of this style into the year counted from 1 January.
	 *
	 * @param year  the year, numbered according to this style
	 * @param month the month (1 to 12)
	 * @param day   the day of the month
	 * @return the year counted from 1 January
	 */
	public int toCommonYear(final int year, final int month, final int day){
		final boolean onOrAfterStart = (month > startMonth || month == startMonth && day >= startDay);
		if(anticipating)
			return (onOrAfterStart? year - 1: year);
		return (onOrAfterStart? year: year + 1);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final GregorianDateInput input = GregorianCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
