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
package io.github.mtrevisan.calendars.hebrew;

import io.github.mtrevisan.calendars.dateparsing.SeasonalDates;
import io.github.mtrevisan.calendars.dateparsing.YearMonthDay;
import io.github.mtrevisan.calendars.spi.CalendarConverterPlugin;
import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * The Hebrew calendar, by the rules of the fixed calendar of Hillel II (Reingold and Dershowitz, "Calendrical
 * Calculations"): the months are numbered as the parser does, as in a leap year (Tishrei = 1, ..., Adar I = 6, Adar II = 7,
 * Nisan = 8, ..., Elul = 13), so that Adar II is missing in the common years.
 */
public class HebrewCalendarStrategy implements CalendarConverterPlugin{

	/** Julian Day Number of the day before the first day (Monday 7 October 3761 BCE Julian) of the elapsed days. */
	private static final long EPOCH_BEFORE = 347997L;
	private static final long MONTHS_PER_CYCLE = 235L;
	/** The months of a cycle that precede the first year: the months elapsed are {@code (235 * year - 234) / 19}. */
	private static final long MONTHS_BEFORE_EPOCH = 234L;
	private static final int YEARS_PER_CYCLE = 19;
	/** The molad of Tishrei of year 1: the hours and the parts (of 1080 in an hour) after the sunset of the day before. */
	private static final long MOLAD_HOURS = 5L;
	private static final long MOLAD_PARTS = 204L;
	private static final long PARTS_PER_HOUR = 1080L;
	private static final long PARTS_PER_MONTH_HOUR = 793L;
	/** A molad at or after noon (18 hours from the sunset) postpones the new year (Molad Zaken). */
	private static final long NOON_PARTS = 19440L;
	/** Dehiyyat GaTaRaD: a Tuesday molad of a common year at or after 9 hours and 204 parts. */
	private static final long GATARAD_PARTS = 9924L;
	/** Dehiyyat BeTUTaKPaT: a Monday molad of the year after a leap year at or after 15 hours and 589 parts. */
	private static final long BETUTAKPAT_PARTS = 16789L;
	/** The weekdays of the elapsed days: the first elapsed day is a Monday, so that {@code day % 7} is 1 for a Monday. */
	private static final int SUNDAY = 0;
	private static final int MONDAY = 1;
	private static final int TUESDAY = 2;
	private static final int WEDNESDAY = 3;
	private static final int FRIDAY = 5;
	private static final int DAYS_IN_WEEK = 7;

	private static final int ADAR_I = 6;
	private static final int ADAR_II = 7;
	private static final int MONTHS_IN_LEAP_YEAR = 13;
	private static final int LEAP_YEAR_LENGTH_UNITS = 10;
	/** The Gregorian year in which the Hebrew year 1 started (3761 BCE, that is -3760 astronomically), minus one. */
	private static final int GREGORIAN_OFFSET = 3761;


	@Override
	public long parseToJdn(final String rawDate){
		// Note: Traditional Hebrew days start at sunset. This structural JDN anchors to the civil day portion.
		final YearMonthDay input = HebrewCalendarParser.parse(rawDate);
		final int year = input.year();

		if(input.isSeason())
			// the Tekufot of the year: the autumn and the winter in the Gregorian year in which the Hebrew year starts
			return SeasonalDates.ofAutumnYear("Hebrew", year - GREGORIAN_OFFSET, input.month());

		return hebrewNewYear(year) + daysBeforeMonth(year, input.monthOrDefault()) + (input.dayOrDefault() - 1);
	}

	/**
	 * Computes the Julian Day Number of Rosh HaShanah (1 Tishrei) of a Hebrew year, from the molad of Tishrei and the
	 * postponements (dehiyyot): Molad Zaken, GaTaRaD and BeTUTaKPaT (that look at the weekday and the time of the molad
	 * itself), then ADU (Rosh HaShanah is never a Sunday, a Wednesday, or a Friday).
	 *
	 * @param year the Hebrew year (from 1)
	 * @return the Julian Day Number
	 */
	static long hebrewNewYear(final long year){
		final long monthsElapsed = Math.floorDiv(MONTHS_PER_CYCLE * year - MONTHS_BEFORE_EPOCH, (long)YEARS_PER_CYCLE);
		final long partsElapsed = MOLAD_PARTS + PARTS_PER_MONTH_HOUR * (monthsElapsed % PARTS_PER_HOUR);
		final long hoursElapsed = MOLAD_HOURS + 12L * monthsElapsed + PARTS_PER_MONTH_HOUR * (monthsElapsed / PARTS_PER_HOUR)
			+ partsElapsed / PARTS_PER_HOUR;

		// the day (counted from the epoch, a Monday) and the time of the molad
		long day = 1L + 29L * monthsElapsed + hoursElapsed / 24L;
		final long parts = (hoursElapsed % 24L) * PARTS_PER_HOUR + (partsElapsed % PARTS_PER_HOUR);
		final int moladWeekday = (int)(day % DAYS_IN_WEEK);

		if(parts >= NOON_PARTS)
			day ++;
		else if(moladWeekday == TUESDAY && !isLeapYear(year) && parts >= GATARAD_PARTS)
			// a Wednesday, that ADU moves to Thursday
			day += 2L;
		else if(moladWeekday == MONDAY && isLeapYear(year - 1) && parts >= BETUTAKPAT_PARTS)
			day ++;

		final int weekday = (int)(day % DAYS_IN_WEEK);
		if(weekday == SUNDAY || weekday == WEDNESDAY || weekday == FRIDAY)
			day ++;

		return day + EPOCH_BEFORE;
	}

	/**
	 * @param year the Hebrew year
	 * @return whether the year is a leap year, of thirteen months (the 3rd, 6th, 8th, 11th, 14th, 17th and 19th of the 19
	 * 	years cycle)
	 */
	static boolean isLeapYear(final long year){
		return (Math.floorMod(7L * year + 1L, (long)YEARS_PER_CYCLE) < 7L);
	}

	/**
	 * @param year the Hebrew year
	 * @return the number of days of the year (353, 354, 355, 383, 384, or 385)
	 */
	static int yearLength(final int year){
		return (int)(hebrewNewYear(year + 1L) - hebrewNewYear(year));
	}

	/**
	 * The length of a month, according to the length of the year: Cheshvan has 30 days in a complete year, Kislev has 29
	 * in a deficient one.
	 *
	 * @param year  the Hebrew year
	 * @param month the month, numbered as in a leap year (Adar I = 6, Adar II = 7, Nisan = 8, ..., Elul = 13)
	 * @return the days of the month, or {@code 0} if it does not exist in the year (the second Adar of a common year)
	 */
	static int monthLength(final int year, final int month){
		if(month < 1 || month > MONTHS_IN_LEAP_YEAR)
			return 0;

		final boolean leap = isLeapYear(year);
		if(month == ADAR_II && !leap)
			return 0;

		return switch(month){
			case 1, 5, 8, 10, 12 -> 30;
			case 2 -> (yearLength(year) % LEAP_YEAR_LENGTH_UNITS == 5? 30: 29);
			case 3 -> (yearLength(year) % LEAP_YEAR_LENGTH_UNITS == 3? 29: 30);
			case ADAR_I -> (leap? 30: 29);
			default -> 29;
		};
	}

	/**
	 * Computes the cumulative elapsed days in a Hebrew year before the given month.
	 * Dynamically adjusts for deficient, regular, and complete year lengths.
	 */
	private static int daysBeforeMonth(final int year, final int month){
		int days = 0;
		for(int m = 1; m < month; m ++)
			days += monthLength(year, m);
		return days;
	}

	@Override
	public CalendarMetadata getMetadata(final String rawDate){
		final YearMonthDay input = HebrewCalendarParser.parse(rawDate);
		return CalendarMetadata.createFromYearMonthDay(input.year(), input.month(), input.day());
	}

}
