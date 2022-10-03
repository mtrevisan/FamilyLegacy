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
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.regex.Pattern;


/** A class for parsing dates from strings. */
public final class DateParser{

	private static final Pattern PATTERN_APPROX = RegexHelper.pattern("(?i)APP(?:RO)?X\\.?");
	private static final String DESCRIPTION_APPROX = "Approximated";
	private static final String TYPE_APPROX = "APPX";
	private static final Pattern PATTERN_AND = RegexHelper.pattern("(?i)AND");
	private static final String DESCRIPTION_AND = "And";
	private static final String TYPE_AND = "AND";

	private DateParser(){}


	/**
	 * Parse the string as date, with the default imprecise date handling preference of {@link DatePreciseness#PRECISE}.
	 *
	 * @param date	The date string.
	 * @return	The date, if it can be derived from the string.
	 */
	public static LocalDate parse(final String date){
		return parse(date, DatePreciseness.PRECISE);
	}

	/**
	 * Parse the string as date.
	 *
	 * @param date	The date string.
	 * @param preciseness	The preference for handling an imprecise date.
	 * @return	The date, if one can be derived from the string.
	 */
	public static LocalDate parse(final String date, final DatePreciseness preciseness){
		LocalDate localDate = null;
		if(StringUtils.isNotBlank(date)){
			final AbstractCalendarParser calendar = CalendarParserBuilder.getParser(date);
			localDate = calendar.parse(date, preciseness);
		}
		return localDate;
	}

	public static CalendarData toCalendarData(final String date){
		CalendarData data = null;
		if(StringUtils.isNotBlank(date)){
			final AbstractCalendarParser calendarParser = CalendarParserBuilder.getParser(date);
			data = calendarParser.extractComponents(date);
		}
		return data;
	}

	public static String formatDate(String date){
		String formattedDate = null;
		if(StringUtils.isNotBlank(date)){
			date = IntervalType.replaceAll(date);
			date = RegexHelper.replaceAll(date, PATTERN_APPROX, DESCRIPTION_APPROX);
			date = RegexHelper.replaceAll(date, PATTERN_AND, DESCRIPTION_AND);
			date = RegexHelper.replaceAll(date, Era.BCE.getPattern(), Era.BCE.toString());
			date = Era.replaceAll(date);
			date = GregorianMonth.replaceAll(date);
			date = FrenchRepublicanMonth.replaceAll(date);
			date = HebrewMonth.replaceAll(date);
			formattedDate = date;
		}
		return formattedDate;
	}

	public static String unformatDate(String date){
		String formattedDate = null;
		if(StringUtils.isNotBlank(date)){
			date = IntervalType.restoreAll(date);
			date = RegExUtils.replaceAll(date, DESCRIPTION_APPROX, TYPE_APPROX);
			date = RegExUtils.replaceAll(date, DESCRIPTION_AND, TYPE_AND);
			date = Era.restoreAll(date);
			date = GregorianMonth.restoreAll(date);
			date = FrenchRepublicanMonth.restoreAll(date);
			date = HebrewMonth.restoreAll(date);
			formattedDate = date;
		}
		return formattedDate;
	}

	public static String extractYear(final String date){
		String formattedDate = null;
		final LocalDate localDate = parse(date);
		if(localDate != null){
			formattedDate = StringUtils.EMPTY;
			if(AbstractCalendarParser.isApproximation(date))
				formattedDate = "~";
			else if(AbstractCalendarParser.isBefore(date))
				formattedDate = "<";
			else if(AbstractCalendarParser.isAfter(date))
				formattedDate = ">";
			formattedDate += localDate.getYear();
		}
		return formattedDate;
	}

}
