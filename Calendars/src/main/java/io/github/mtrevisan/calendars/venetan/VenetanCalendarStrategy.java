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
package io.github.mtrevisan.calendars.venetan;

import io.github.mtrevisan.calendars.astronomy.CalendarSeason;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.gregorian.Indiction;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;
import io.github.mtrevisan.calendars.spi.DateInterpretation;
import io.github.mtrevisan.calendars.spi.SeasonSolver;
import io.github.mtrevisan.calendars.yearstart.YearStartStyleCalendarStrategy;

import java.util.List;
import java.util.Locale;


public class VenetanCalendarStrategy implements CalendarConverterPlugin{

	/** Number of months between the civil (January first) and the Venetan (March first) numbering of the months. */
	private static final int MONTH_SHIFT = 2;
	private static final int MONTHS_IN_YEAR = 12;


	@Override
	public long parseToJdn(final String rawDate){
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		final int year = input.year();
		// a missing month stands for the first month of the year
		final int month = YearMonthDay.monthOrFirst(input.month());
		// a missing day stands for the first day of the month
		final int day = YearMonthDay.dayOrFirst(input.day());

		if(month < 0){
			// the season starts near the first day of its own month, converted into the Venetan numbering (March is month 1)
			final int venetanMonth = CalendarSeason.gregorianSeasonMonth(month) - MONTH_SHIFT;
			return SeasonSolver.jdnNear(toJdn(year, venetanMonth, 1), month, "Venetan");
		}

		// the indiction is checked against the civil year (counted from 1 January)
		final int civilMonth = civilMonth(month);
		Indiction.validate(input.indiction(), YearStartStyleCalendarStrategy.VENETAN.toCommonYear(year, civilMonth, day),
			(input.month() != 0? civilMonth: 0));
		return toJdn(year, month, day);
	}

	/**
	 * Converts the more Veneto months (1 = March, ..., 12 = February) into the civil ones.
	 */
	private static int civilMonth(final int month){
		return (month <= MONTHS_IN_YEAR - MONTH_SHIFT? month + MONTH_SHIFT: month - (MONTHS_IN_YEAR - MONTH_SHIFT));
	}

	private static long toJdn(final int year, final int month, final int day){
		// January and February (months 11 and 12) belong to the next civil year, which the year-start style takes care of.
		// The Republic of Venice adopted the Gregorian calendar in October 1582 (4 October was followed by 15 October),
		// while keeping the more Veneto start of the year
		return YearStartStyleCalendarStrategy.VENETAN.toJdn(year, civilMonth(month), day);
	}

	/**
	 * The words of the Venetan months and seasons are not shared with other months or seasons, so no expression has
	 * more than one reading.
	 */
	@Override
	public List<DateInterpretation> getInterpretations(final String rawDate, final Locale locale){
		return List.of();
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final VenetanDateInput input = VenetanCalendarParser.parse(rawDate);
		// the parser always requires the year, which can be the astronomical year 0 (1 BC)
		return CalendarMetadata.createFromYearMonthDay(true, input.month(), input.day());
	}

}
