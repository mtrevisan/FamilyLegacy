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

import io.github.mtrevisan.familylegacy.services.RegexHelper;

import java.time.LocalDate;
import java.time.Month;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class for parsing French Republican Calendar dates.
 * <p>Calculations based on Romme method.</p>
 * <p>Only supports dates on or after September 22, 1792 (Gregorian).</p>
 */
class FrenchRepublicanCalendarParser extends AbstractCalendarParser{

	private static final String PARAM_DAY = "day";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_YEAR = "year";
	private static final String PATTERN_DATE_DAY = "(?:(?<" + PARAM_DAY + ">\\d{1,2}) )?";
	private static final String PATTERN_DATE_MONTH = "(?:(?<" + PARAM_MONTH + ">[A-Z]+) )?";
	private static final String PATTERN_DATE_YEAR = "(?:(?<" + PARAM_YEAR + ">\\d{1,4}))";
	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^" + PATTERN_DATE_DAY + PATTERN_DATE_MONTH
		+ PATTERN_DATE_YEAR + "$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new FrenchRepublicanCalendarParser();
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
				final FrenchRepublicanMonth m = (month != null? FrenchRepublicanMonth.fromAbbreviation(month): null);
				final Integer d = (day != null? Integer.valueOf(day): null);

				localDate = managePreciseness(y, m, d, preciseness);
			}
			catch(final NullPointerException ignored){}
		}
		return localDate;
	}

	private LocalDate managePreciseness(final int year, FrenchRepublicanMonth month, Integer day, final DatePreciseness preciseness)
			throws IllegalArgumentException{
		if(preciseness == null)
			throw new IllegalArgumentException("Unknown value for date handling preference: " + preciseness);

		if(day == null){
			switch(preciseness){
				case FAVOR_LATEST:
					if(month == null){
						month = FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS;
						day = (isLeapYear(year)? 6: 5);
					}
					else if(month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS)
						day = (isLeapYear(year)? 6: 5);
					else
						day = 30;
					break;

				case FAVOR_MIDPOINT:
					if(month == null){
						month = FrenchRepublicanMonth.GERMINAL;
						day = 1;
					}
					else if(month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS)
						day = (isLeapYear(year)? 3: 2);
					else
						day = 15;
					break;

				case PRECISE:
				case FAVOR_EARLIEST:
					if(month == null)
						month = FrenchRepublicanMonth.VENDEMIAIRE;
					day = 1;
			}
		}
		return convertToGregorian(year, month, day);
	}

	/**
	 * This function converts a French Republican date into the Gregorian date.
	 *
	 * @param year	The French Republican year (french year 1 corresponds to Gregorian year 1792).
	 * @param month	The French Republican month abbreviation.
	 * @param day	The day within the month.
	 * @return	The date in Gregorian form.
	 */
	private LocalDate convertToGregorian(final int year, final FrenchRepublicanMonth month, final int day){
		if(year < 1 || month == null || day < 1
				//there were never more than 6 days in Jour Complementairs, and that was only on leap years
				|| month == FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS && day > 6
				//all the other months had 30 days
				|| month != FrenchRepublicanMonth.JOUR_COMPLEMENTAIRS && day > 30)
			return null;

		//start just before beginning of French Republican time - 21 SEP 1792
		LocalDate date = LocalDate.of(1792, Month.SEPTEMBER, 21);

		//add years already passed
		for(int i = 1; i <= year - 1; i ++){
			//365 days per year
			date = date.plusDays(365);

			if(isLeapYear(i))
				//add a leap day
				date = date.plusDays(1);
		}

		//add 30 days per month
		return date.plusDays(30l * month.ordinal() + day);
	}

	/**
	 * Is the French Republican year supplied a French Leap Year? Uses the Romme rule, which says:
	 * <ul>
	 * <li>Years III, VII, XI, XV, and XX are to be leap years</li>
	 * <li>After that, every four years; <strong>but</strong> if the French year is divisible by 100 it must also be divisible by
	 * 400 to be a leap year (much like Gregorian).
	 * </ul>
	 *
	 * @param year	The French Republican Year.
	 * @return	Whether it's a French Leap Year.
	 */
	private boolean isLeapYear(final int year){
		return (year == 3 || year == 7 || year == 11 || year == 15
			|| year >= 20 && year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
	}

}
