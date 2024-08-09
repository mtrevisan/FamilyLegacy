/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.helpers.parsers;

import io.github.mtrevisan.familylegacy.flef.helpers.RegexHelper;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class for converting Hebrew dates into Gregorian dates.
 * All dates processed as if before sunset.
 */
class HebrewCalendarParser extends AbstractCalendarParser{

	private static final String PARAM_DAY = "day";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_YEAR = "year";
	private static final String PATTERN_DATE_DAY = "(?:(?<" + PARAM_DAY + ">\\d{1,2}) )?";
	private static final String PATTERN_DATE_MONTH = "(?:(?<" + PARAM_MONTH + ">[A-Z]+) )?";
	private static final String PATTERN_DATE_YEAR = "(?:(?<" + PARAM_YEAR + ">\\d{1,4}))";
	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^" + PATTERN_DATE_DAY + PATTERN_DATE_MONTH
		+ PATTERN_DATE_YEAR + "$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new HebrewCalendarParser();
	}


	public static AbstractCalendarParser getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	protected LocalDate getDate(final CharSequence date, final DatePreciseness preciseness) throws IllegalArgumentException{
		LocalDate localDate = null;
		final Matcher matcher = RegexHelper.matcher(date, PATTERN_DATE);
		if(matcher.find()){
			final String day = matcher.group(PARAM_DAY);
			final String month = matcher.group(PARAM_MONTH);
			final String year = matcher.group(PARAM_YEAR);

			try{
				final int y = Integer.parseInt(year);
				final HebrewMonth m = (month != null? HebrewMonth.fromAbbreviation(month): null);
				final Integer d = (day != null? Integer.valueOf(day): null);

				localDate = managePreciseness(y, m, d, preciseness);
			}
			catch(final NullPointerException ignored){}
		}
		return localDate;
	}

	private LocalDate managePreciseness(final int year, HebrewMonth month, Integer day, final DatePreciseness preciseness)
			throws IllegalArgumentException{
		if(preciseness == null)
			throw new IllegalArgumentException("Unknown value for date handling preference");

		if(day == null){
			switch(preciseness){
				case FAVOR_LATEST -> {
					if(month == null)
						month = HebrewMonth.ELUL;
					day = getMonthLength(year, month);
				}
				case FAVOR_MIDPOINT -> {
					if(month == null)
						month = HebrewMonth.ADAR;
					day = getMonthLength(year, month) / 2;
				}
				case PRECISE, FAVOR_EARLIEST -> {
					if(month == null)
						month = HebrewMonth.TISHREI;
					day = 1;
				}
			}
		}
		return convertToGregorian(year, month, day);
	}

	/**
	 * This function converts a Hebrew date into the Gregorian date.
	 *
	 * @param year	The hebrew year.
	 * @param month	The hebrew month abbreviation.
	 * @param day	The day within the month.
	 * @return	The date in Gregorian form.
	 */
	private LocalDate convertToGregorian(final int year, final HebrewMonth month, final int day){
		LocalDate c = getFirstDayOfHebrewYear(year);

		//now count up days within the year
		final int monthNumber = month.ordinal();
		for(int m = 1; m <= monthNumber; m ++){
			final int monthLength = getMonthLength(year, HebrewMonth.values()[m - 1]);
			c = c.plusDays(monthLength);
		}
		c = c.plusDays(day - 1);

		return c;
	}

	/**
	 * Get the number of days in the month and year requested.
	 *
	 * @param year	The hebrew year.
	 * @param month	The Hebrew month.
	 * @return	The number of days in the month on the specified year.
	 */
	private int getMonthLength(final int year, final HebrewMonth month){
		final int yearLength = getLengthOfYear(year);
		final int monthNumber = month.ordinal() + 1;
		//the regular length of a non-leap Hebrew year is 354 days. The regular length of a Hebrew leap year is 384 days.
		//if the year is shorter by one less day, it is called a haser year. Kislev on a haser year has 29 days. If the year is
		//longer by one day, it is called a shalem year. Cheshvan on a shalem year is 30 days.
		int monthLength = 0;
		switch(monthNumber){
			case 1, 5, 8, 10, 12 -> monthLength = 30;
			case 4, 7, 9, 11, 13 -> monthLength = 29;
			case 6 -> {
				final boolean leapYear = isLeapYear(year);
				monthLength = (leapYear? 30: 0);
			}
			case 2 -> {
				final boolean shalemYear = (yearLength == 355 || yearLength == 385);
				monthLength = (shalemYear? 30: 29);
			}
			case 3 -> {
				final boolean haserYear = (yearLength == 353 || yearLength == 383);
				monthLength = (haserYear? 29: 30);
			}
		}
		return monthLength;
	}

	/**
	 * Get the Gregorian Date corresponding to the first day of a given Hebrew year (1 Tishrei).
	 *
	 * @param year	The hebrew year.
	 * @return	The gregorian date of the first day of the hebrew year supplied.
	 */
	private LocalDate getFirstDayOfHebrewYear(final int year){
		//Calculate how many days, hours and chalakim (1/1080th of an hour, about 3.333 secs) it has been from the molad (start of
		//new moon) at the beginning of the year.
		//The period between one new moon to the next is 29 days, 12 hours and 793 chalakim. We must multiply that by the amount of
		//months that transpired since the first molad. Then we add the time of the first molad (Monday, 5 hours and 204 chalakim).
		final int monthsSinceFirstMolad = getMonthsSinceFirstMolad(year);
		int chalakim = 793 * monthsSinceFirstMolad + 204;
		//carry the excess Chalakim over to the hours
		int hours = (int)Math.floor(chalakim / 1080.);
		chalakim %= 1080;

		hours += monthsSinceFirstMolad * 12 + 5;

		//carry the excess hours over to the days
		int days = (int)Math.floor(hours / 24.);
		hours %= 24;

		days += 29 * monthsSinceFirstMolad + 2;

		//figure out which day of the week the molad occurs. Shabbos is 0, other days of week are 1-based
		int dayOfWeek = days % 7;

		//In a perfect world, Rosh Hashanah would be on the day of the molad. The Hebrew calendar makes four exceptions where we
		//push off Rosh Hashanah one or two days. This is done to prevent three situations. Without explaining why, the three
		//situations are:
		//1) We don't want Rosh Hashanah to come out on Sunday, Wednesday or Friday
		//2) We don't want Rosh Hashanah to be on the day of the molad if the molad occurs after the beginning of 18th hour.
		//3) We want to limit years to specific lengths. For non-leap years, we limit it to either 353, 354 or 355 days. For leap
		//years, we limit it to either 383, 384 or 385 days. If setting Rosh Hashanah to the day of the molad will cause this year,
		//or the previous year to fall outside these lengths, we push off Rosh Hashanah to get the year back to a valid length.
		//This code handles these exceptions.
		if(!isLeapYear(year) && dayOfWeek == 3 && hours * 1080 + chalakim >= 9 * 1080 + 204){
			//this prevents the year from being 356 days. We have to push Rosh Hashanah off two days because if we pushed it off
			//only one day, Rosh Hashanah would come out on a Wednesday. Check the Hebrew year 5745 for an example
			dayOfWeek = 5;
			days += 2;
		}
		else if(isLeapYear(year - 1) && dayOfWeek == 2 && hours * 1080 + chalakim >= 15 * 1080 + 589){
			//this prevents the previous year from being 382 days. Check the Hebrew Year 5766 for an example. If Rosh Hashanah was
			//not pushed off a day then 5765 would be 382 days
			dayOfWeek = 3;
			days += 1;
		}
		else{
			//see rule 2 above. Check the Hebrew year 5765 for an example
			if(hours >= 18){
				dayOfWeek += 1;
				dayOfWeek %= 7;
				days += 1;
			}
			//see rule 1 above. Check the Hebrew year 5765 for an example
			if(dayOfWeek == 1 || dayOfWeek == 4 || dayOfWeek == 6){
				dayOfWeek += 1;
				dayOfWeek %= 7;
				days += 1;
			}
		}

		//adjust by the number of days since creation for 1 Jan 1900 - starting point for making date adjustments since Java dates are around the epoch
		days -= 2067025;

		LocalDate c = LocalDate.of(1900, Month.JANUARY, 1)
			.plusDays(days);

		//Sep 14, 1752, when Gregorian was adopted by England and its colonies
		final LocalDate gregorianReformation = LocalDate.of(1752, Month.SEPTEMBER, 14);
		//adjust for the Gregorian Reformation if needed
		if(c.isBefore(gregorianReformation))
			c = c.plusDays(-10);

		return c;
	}

	/**
	 * This function gets the number of days of a Hebrew year.
	 *
	 * @param year	The Hebrew year.
	 * @return	The number of days in the year.
	 */
	private int getLengthOfYear(final int year){
		final LocalDate thisNewYear = getFirstDayOfHebrewYear(year);
		final LocalDate nextNewYear = getFirstDayOfHebrewYear(year + 1);
		return (int)ChronoUnit.DAYS.between(thisNewYear, nextNewYear);
	}

	/**
	 * This function returns how many months there has been from the first Molad of the year supplied.
	 *
	 * @param year	The Hebrew year.
	 * @return	The number of months since the first Molad.
	 */
	private int getMonthsSinceFirstMolad(final int year){
		//the months of this year haven't happened yet, so go back a year
		int y = year - 1;

		//get how many 19-year cycles there has been and multiply it by 235 (which is the number of months in a 19-year cycle)
		int result = (int)(Math.floor(y / 19.) * 235.);

		//get the remaining years after the last complete 19-year cycle
		y = yearInLeapCycle(y);

		//add 12 months for each of those years...
		result += 12 * y;

		//and then add the extra months to account for the leap years
		if(y >= 17)
			result += 6;
		else if(y >= 14)
			result += 5;
		else if(y >= 11)
			result += 4;
		else if(y >= 8)
			result += 3;
		else if(y >= 6)
			result += 2;
		else if(y >= 3)
			result += 1;

		return result;
	}

	/**
	 * This function returns if a given year is a leap year.
	 *
	 * @param year	The Hebrew year.
	 * @return	Whether the hebrew year supplied is a leap year.
	 */
	private boolean isLeapYear(final int year){
		final int yearInCycle = yearInLeapCycle(year);
		return (yearInCycle == 3 || yearInCycle == 6 || yearInCycle == 8 || yearInCycle == 11 || yearInCycle == 14
			|| yearInCycle == 17 || yearInCycle == 0);
	}

	/**
	 * Find out which year we are within the leap-year cycle. Since the cycle lasts 19 years, the 19th year of the cycle will return 0.
	 *
	 * @param year	The Hebrew year.
	 * @return	Which year within the cycle we're in. The 19th year of the cycle is zero.
	 */
	private int yearInLeapCycle(final int year){
		return year % 19;
	}

}
