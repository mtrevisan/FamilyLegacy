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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class GregorianCalendarParser extends AbstractCalendarParser{

	public static final String DOUBLE_ENTRY_YEAR_SEPARATOR = "/";

	private static final String PARAM_DAY = "day";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_YEAR = "year";
	private static final String PARAM_DOUBLE_ENTRY_YEAR = "doubleEntryYear";
	private static final String PARAM_ERA = "era";
	private static final String PATTERN_DATE_DAY = "(?:(?<" + PARAM_DAY + ">\\d{1,2}) )?";
	private static final String PATTERN_DATE_MONTH = "(?:(?<" + PARAM_MONTH + ">[A-Z]+) )?";
	private static final String PATTERN_DATE_YEAR = "(?:(?<" + PARAM_YEAR + ">\\d{1,4}))";
	private static final String PATTERN_DATE_DOUBLE_YEAR = "(?:\\" + DOUBLE_ENTRY_YEAR_SEPARATOR
		+ "(?<" + PARAM_DOUBLE_ENTRY_YEAR + ">\\d{2})?)?";
	private static final String PATTERN_DATE_ERA = "(?: (?<" + PARAM_ERA + ">[ABCE.]+))?";
	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^" + PATTERN_DATE_DAY + PATTERN_DATE_MONTH
		+ PATTERN_DATE_YEAR + PATTERN_DATE_DOUBLE_YEAR + PATTERN_DATE_ERA + "$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new GregorianCalendarParser();
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
			final String doubleEntryYear = matcher.group(PARAM_DOUBLE_ENTRY_YEAR);
			final String year = resolveEnglishCalendarSwitchYear(matcher.group(PARAM_YEAR), doubleEntryYear);
			final Era era = Era.fromDate(matcher.group(PARAM_ERA));

			try{
				int y = Integer.parseInt(year);
				if(era == Era.BCE)
					y = 1 - y;
				int m = (month != null? GregorianMonth.fromAbbreviation(month).ordinal() + 1: 1);
				int d = (day != null? Integer.parseInt(day): 1);
				if(m == 2 && d == 29){
					m = 3;
					d = 1;
				}

				localDate = LocalDate.of(y, m, d);

				localDate = managePreciseness(day, month, localDate, preciseness);
			}
			catch(final DateTimeException | NullPointerException ignored){}
		}
		return localDate;
	}

	/**
	 * Resolve a date in double-dated format, for the old/new dates preceding the English calendar switch of {@code 1752}.
	 * <p>
	 * Because of the switch to the Gregorian Calendar in {@code 1752} in England and its colonies, and the corresponding change of the
	 * first day of the year, it's not uncommon for dates in the range between {@code 1582} and {@code 1752} to be written using a
	 * double-dated format, showing the old and new dates simultaneously. For example, today we would render George Washington's birthday as
	 * {@code 22 FEB 1732}. However, in {@code 1760} or so, one might have written it as {@code Feb 22 1731/32}, thus be
	 * entered as {@code 22 FEB 1731/32}.
	 * </p>
	 *
	 * @param year	The year.
	 * @param doubleEntryYear	The double-entry for the year.
	 * @return	The year, resolved to a Gregorian year.
	 */
	private String resolveEnglishCalendarSwitchYear(final String year, final String doubleEntryYear){
		int y = Integer.parseInt(year);
		if(1582 <= y && y <= 1752 && doubleEntryYear != null){
			final int yy = Integer.parseInt(doubleEntryYear);
			//handle century boundary
			if(yy == 0 && y % 100 == 99)
				y ++;

			return (y / 100) + doubleEntryYear;
		}
		return year;
	}

	private LocalDate managePreciseness(final String day, final String month, final LocalDate localDate,
			final DatePreciseness preciseness) throws IllegalArgumentException{
		if(preciseness == null)
			throw new IllegalArgumentException("Unknown value for date handling preference");

		return (localDate != null && day == null?
			(month != null? preciseness.applyToMonth(localDate): preciseness.applyToYear(localDate)):
			localDate);
	}

}