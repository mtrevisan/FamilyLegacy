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
package io.github.mtrevisan.familylegacy.gedcom.parsers.calendars;

import io.github.mtrevisan.familylegacy.services.RegexHelper;

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
	private static final String PATTERN_DATE_YEAR = "(?:(?<" + PARAM_YEAR + ">\\d{1,4})";
	private static final String PATTERN_DATE_DOUBLE_YEAR = "(?:\\/(?<" + PARAM_DOUBLE_ENTRY_YEAR + ">\\d{2}))?)?";
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
	public CalendarType getCalendarType(){
		return CalendarType.GREGORIAN;
	}

	@Override
	protected DateData extractSingleDateComponents(final String singleDate){
		final DateData date = new DateData();
		final String plainDate = CalendarParserBuilder.removeCalendarType(singleDate);
		final Matcher matcher = RegexHelper.matcher(plainDate, PATTERN_DATE);
		if(matcher.find()){
			final String day = matcher.group(PARAM_DAY);
			final String month = matcher.group(PARAM_MONTH);
			final String year = matcher.group(PARAM_YEAR);
			final String doubleEntryYear = matcher.group(PARAM_DOUBLE_ENTRY_YEAR);
			final String era = matcher.group(PARAM_ERA);

			if(day != null)
				date.withDay(Integer.parseInt(day));
			if(month != null)
				date.withMonth(Integer.parseInt(month));
			if(year != null)
				date.withYear(Integer.parseInt(year));
			if(doubleEntryYear != null)
				date.withDoubleEntryYear(Integer.parseInt(doubleEntryYear));
			if(era != null)
				date.withEra(Era.fromDate(era));
		}
		return date;
	}

	/**
	 * Parse a Gregorian/Julian/unknown date string.
	 *
	 * @param date	The date string to parse.
	 * @param preciseness	The preference for handling an imprecise date.
	 * @return	The date, if one can be derived from the string.
	 */
	@Override
	public LocalDate parse(final String date, final DatePreciseness preciseness){
		String plainDate = CalendarParserBuilder.removeCalendarType(date);

		plainDate = removeApproximations(plainDate);
		plainDate = removeOpenEndedRangesAndPeriods(plainDate);

		return (isRange(plainDate)? getDateFromRangeOrPeriod(plainDate, preciseness): getDate(plainDate, preciseness));
	}

	private LocalDate getDate(final CharSequence date, final DatePreciseness preciseness) throws IllegalArgumentException{
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
				final int m = (month != null? GregorianMonth.fromAbbreviation(month).ordinal() + 1: 1);
				final int d = (day != null? Integer.parseInt(day): 1);

				try{
					localDate = LocalDate.of(y, m, d);
				}
				catch(final DateTimeException e){
					if(m == 2 && d == 29)
						localDate = LocalDate.of(y, 3, 1);
				}

				localDate = managePreciseness(day, month, localDate, preciseness);
			}
			catch(final NullPointerException ignored){}
		}
		return localDate;
	}

	/**
	 * Resolve a date in double-dated format, for the old/new dates preceding the English calendar switch of 1752.
	 * <p>
	 * Because of the switch to the Gregorian Calendar in 1752 in England and its colonies, and the corresponding change of the
	 * first day of the year, it's not uncommon for dates in the range between 1582 and 1752 to be written using a double-dated
	 * format, showing the old and new dates simultaneously. For example, today we would render George Washington's birthday in
	 * GEDCOM format as <code>22 FEB 1732</code>. However, in 1760 or so, one might have written it as Feb 22 1731/32, thus be
	 * entered into a GEDCOM field as <code>22 FEB 1731/32</code>.
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

	@Override
	public int parseMonth(final String month){
		final GregorianMonth m = GregorianMonth.fromAbbreviation(month);
		return (m != null? m.ordinal(): -1);
	}

}
