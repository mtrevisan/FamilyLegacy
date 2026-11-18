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
package io.github.mtrevisan.calendars.parsi;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.GregorianCalendarStrategy;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.SeasonSolver;

import java.time.Year;


public class ParsiCalendarStrategy implements CalendarConverterPlugin{

	// Yazdegerdi era epoch (16 June 632 Julian)
	private static final long YAZDEGERDI_EPOCH_JDN = 1952063L;

	/** The Fasli year starts on 21 March of the Gregorian year YZ + 630. */
	private static final int FASLI_ERA_OFFSET = 630;
	private static final int FASLI_NEW_YEAR_MONTH = 3;
	private static final int FASLI_NEW_YEAR_DAY = 21;
	private static final int GATHA = 13;
	private static final int AVARDAD_SAL_GAH = 6;
	/** The Julian Day Number of 1 January 1970. */
	private static final long GREGORIAN_EPOCH_1970_JDN = 2440588L;
	private static final double DAYS_PER_YEAR = 365.2425;


	@Override
	public long parseToJdn(final String rawDate){
		// 1. Unpack structural components using the specialized text parser
		final ParsiDateInput input = ParsiCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = YearMonthDay.monthOrFirst(input.month());
		// a missing day stands for the first day of the month
		final int day = YearMonthDay.dayOrFirst(input.day());
		final ParsiVariant variant = input.variant();
		final long startOfYearJdn = startOfYearJdn(year, variant);
		if(month == GATHA && day == AVARDAD_SAL_GAH && !Year.isLeap(year + FASLI_ERA_OFFSET + 1L))
			throw new IllegalArgumentException("Avardad-sal-Gah (the sixth Gatha day) is only in the Fasli years containing"
				+ " a Gregorian 29 February: " + rawDate);

		if(month < 0){
			return seasonJdn(year, variant, month);
		}
		else
			// Standard arithmetic execution mapping path for typical regular Parsi months
			return startOfYearJdn + 30L * (month - 1L) + (day - 1L);
	}

	/**
	 * The year (365 days, or 366 for a Fasli leap year) contains each solstice and equinox once: the Gregorian month of the
	 * season is the anchor of the search, then the event that falls inside the year is chosen (the Shahenshahi and Kadmi
	 * years drift through the seasons, so their start does not tell the Gregorian year).
	 */
	private static long seasonJdn(final int year, final ParsiVariant variant, final int month){
		final long start = startOfYearJdn(year, variant);
		final long end = startOfYearJdn(year + 1, variant);

		final int seasonMonth = CalendarSeason.gregorianSeasonMonth(month);
		final int approximateGregorianYear = (int)Math.floor((start - GREGORIAN_EPOCH_1970_JDN) / DAYS_PER_YEAR) + 1970;
		for(int gregorianYear = approximateGregorianYear - 1; gregorianYear <= approximateGregorianYear + 1; gregorianYear ++){
			final long eventJdn = SeasonSolver.jdnNear(GregorianCalendarStrategy.toJdn(gregorianYear, seasonMonth, 1), month, "Parsi");
			if(eventJdn >= start && eventJdn < end)
				return eventJdn;
		}
		throw new IllegalArgumentException("The season does not fall in the Parsi year " + year);
	}

	/**
	 * @return the Julian Day Number of 1 Fravardin of the year
	 */
	private static long startOfYearJdn(final int year, final ParsiVariant variant){
		if(variant == ParsiVariant.FASLI)
			return GregorianCalendarStrategy.toJdn(year + FASLI_ERA_OFFSET, FASLI_NEW_YEAR_MONTH, FASLI_NEW_YEAR_DAY);

		return YAZDEGERDI_EPOCH_JDN + variant.getOffsetDays() + 365L * (year - 1L);
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final ParsiDateInput input = ParsiCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
