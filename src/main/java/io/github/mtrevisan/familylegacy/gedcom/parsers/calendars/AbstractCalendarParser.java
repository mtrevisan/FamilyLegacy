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
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class AbstractCalendarParser{

	private static final String FORMAT_FROM = "FROM";
	private static final String FORMAT_TO = "TO";
	private static final String FORMAT_BEFORE = "BEF(?:ORE|\\.)?";
	private static final String FORMAT_AFTER = "AFT(?:ER|\\.)?";
	private static final String FORMAT_BETWEEN = "BET(?:WEEN|\\.)?|BTW\\.?";
	private static final String FORMAT_AND = "AND";

	private static final String FORMAT_RANGE_PREFIX = "(" + FORMAT_FROM + "|" + FORMAT_BEFORE + "|" + FORMAT_AFTER + "|" + FORMAT_BETWEEN + "|" + FORMAT_TO + ") ";
	private static final String FORMAT_DAY_MONTH_YEAR = "(\\d{1,2} )?([A-Z]{3,4} )?\\d{1,4}(\\/\\d{2})? ?([AB]C|[AB]\\.C\\.|B?CE|(?:B\\.)?C\\.E\\.)?";

	private static final Pattern PATTERN_RANGE_INFIX = RegexHelper.pattern("(?i) (" + FORMAT_AND + "|" + FORMAT_TO + ") ");

	private static final Pattern PATTERN_PREFIXES_APPROXIMATIONS = RegexHelper.pattern("(?i)((?:(?:ABT|APP(?:RO)?X|CALC?|EST)\\.?|ABOUT)) ");
	private static final Pattern PATTERN_PREFIXES_RANGE = RegexHelper.pattern("(?i)((?:" + FORMAT_BETWEEN + "|" + FORMAT_FROM + ")) ");
	private static final Pattern PATTERN_INTERPRETED = RegexHelper.pattern("(?i)^INT\\.?\\s+((\\d{1,2} )?([A-Z]{3,4} )?(\\d{1,4}(/\\d{2})?)?)\\s+\\([^)]+\\)$");
	private static final Pattern PATTERN_INTERPRETATION_TEXT = RegexHelper.pattern("\\(?:([^)]+)\\)$");

	/** The regex pattern that identifies two-date range or period. Works for Gregorian, Julian, and Hebrew years. */
	private static final Pattern PATTERN_TWO_DATES = RegexHelper.pattern("(?i)^" + FORMAT_RANGE_PREFIX
		+ FORMAT_DAY_MONTH_YEAR + " (" + FORMAT_AND + "|" + FORMAT_TO + ") " + FORMAT_DAY_MONTH_YEAR + "$");

	private static final Pattern PATTERN_PREFIXES_FROM_TO_BEFORE_AFTER = RegexHelper.pattern("(?i)((?:" + FORMAT_FROM + "|" + FORMAT_BEFORE + "|" + FORMAT_AFTER + "|" + FORMAT_TO + ")) ");

	private static final Pattern PATTERN_PREFIXES_BEFORE = RegexHelper.pattern("(?i)(?:" + FORMAT_BEFORE + ") ");
	private static final Pattern PATTERN_PREFIXES_AFTER = RegexHelper.pattern("(?i)(?:" + FORMAT_AFTER + ") ");


	public abstract CalendarType getCalendarType();

	public CalendarData extractComponents(final CharSequence date){
		String plainDate = CalendarParserBuilder.removeCalendarType(date);

		final String[] rawComponents = StringUtils.splitByWholeSeparator(plainDate, StringUtils.SPACE);
		final IntervalType intervalType = (rawComponents.length > 0? IntervalType.createFromDate(rawComponents[0]): null);
		final Qualification fromQualification = (rawComponents.length > 1? Qualification.createFromDate(rawComponents[1]): null);
		final String interpretedFrom = extractInterpretedFrom(plainDate);

		plainDate = removeApproximations(plainDate);
		plainDate = removeOpenEndedRangesAndPeriods(plainDate);

		final CalendarData response = new CalendarData()
			.withCalendarType(getCalendarType())
			.withIntervalType(intervalType)
			.withInterpretedFrom(interpretedFrom);
		if(isRange(plainDate)){
			final String[] range = getDatesFromRangeOrPeriod(plainDate);
			if(range == null)
				return null;

			final DateData fromDate = extractSingleDateComponents(range[0]);
			response.withFromDate(fromDate)
				.withFromQualification(fromQualification);
			final DateData toDate = extractSingleDateComponents(range[1]);
			Qualification toQualification = null;
			for(int i = 2; i < rawComponents.length; i ++){
				toQualification = Qualification.createFromDate(rawComponents[i]);
				if(toQualification != null)
					break;
			}
			response.withToDate(toDate)
				.withToQualification(toQualification);
		}
		else{
			final DateData singleDate = extractSingleDateComponents(plainDate);
			response.withFromDate(singleDate)
				.withFromQualification(fromQualification);
		}
		return response;
	}

	protected abstract DateData extractSingleDateComponents(final String singleDate);

	public abstract LocalDate parse(final String date, final DatePreciseness preciseness);

	public abstract int parseMonth(final String month);

	public boolean isRange(final CharSequence date){
		return RegexHelper.matches(date, PATTERN_TWO_DATES);
	}

	/**
	 * Get the preferred date from a range or period, for Gregorian/Julian dates.
	 *
	 * @param dateRange	The date range string.
	 * @param preciseness	The preferred method of handling the range.
	 * @return	The date, or null if no date could be parsed from the data.
	 */
	protected final LocalDate getDateFromRangeOrPeriod(final String dateRange, final DatePreciseness preciseness){
		if(preciseness == null)
			throw new IllegalArgumentException("Unexpected value for imprecise date preference");

		final String[] range = getDatesFromRangeOrPeriod(dateRange);
		if(range == null)
			return null;

		//calculate the dates from the two strings, based on what's preferred
		return preciseness.applyToRange(range, this::parse);
	}

	/**
	 * Get the preferred date from a range or period, for Gregorian/Julian dates.
	 *
	 * @param dateRange	The date range string.
	 * @return	The date, or null if no date could be parsed from the data.
	 */
	protected String[] getDatesFromRangeOrPeriod(final String dateRange){
		//split the string into two dates
		final String[] dates = splitDates(dateRange);
		if(dates.length != 2)
			return null;

		return dates;
	}

	/**
	 * Split a two-date string, removing prefixes, and return an array of two date strings.
	 *
	 * @param date	The date string containing two dates.
	 * @return	An array of two strings, or an empty array if the supplied {@code dateString} value does not contain the
	 *		{@code splitOn} delimiter value. Never returns null.
	 */
	private String[] splitDates(String date){
		date = RegexHelper.clear(date, PATTERN_PREFIXES_RANGE);
		return RegexHelper.split(date, PATTERN_RANGE_INFIX);
	}

	public static boolean isApproximation(final CharSequence date){
		return RegexHelper.find(date, PATTERN_PREFIXES_APPROXIMATIONS);
	}

	public static boolean isBefore(final CharSequence date){
		return RegexHelper.find(date, PATTERN_PREFIXES_BEFORE);
	}

	public static boolean isAfter(final CharSequence date){
		return RegexHelper.find(date, PATTERN_PREFIXES_AFTER);
	}

	/**
	 * Return a version of the string with approximation prefixes removed, including handling for interpreted dates.
	 *
	 * @param date	The date string.
	 * @return	A version of the string with approximation prefixes removed.
	 */
	protected String removeApproximations(String date){
		date = RegexHelper.clear(date, PATTERN_PREFIXES_APPROXIMATIONS);
		date = RegexHelper.replaceAll(date, PATTERN_INTERPRETED, "$1");
		return date;
	}

	protected String extractInterpretedFrom(final CharSequence date){
		final Matcher matcher = RegexHelper.matcher(date, PATTERN_INTERPRETATION_TEXT);
		return (matcher.find()? matcher.group(0): null);
	}

	/**
	 * Remove the prefixes for open-ended date ranges with only one date (e.g., "BEF 1900", "FROM 1756", "AFT 2000").
	 *
	 * @param date	The date string.
	 * @return	The same date string with range/period prefixes removed, but only if it's an open-ended period or range.
	 */
	protected String removeOpenEndedRangesAndPeriods(final String date){
		return (!RegexHelper.matches(date, PATTERN_TWO_DATES)? RegexHelper.clear(date, PATTERN_PREFIXES_FROM_TO_BEFORE_AFTER): date);
	}

}
