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

import java.util.regex.Matcher;
import java.util.regex.Pattern;


class JulianCalendarParser extends GregorianCalendarParser{

	private static final String PARAM_DAY = "day";
	private static final String PARAM_MONTH = "month";
	private static final String PARAM_YEAR = "year";
	private static final String PARAM_ERA = "era";
	private static final String PATTERN_DATE_DAY = "(?:(?<" + PARAM_DAY + ">\\d{1,2}) )?";
	private static final String PATTERN_DATE_MONTH = "(?:(?<" + PARAM_MONTH + ">[A-Z]+) )?";
	private static final String PATTERN_DATE_YEAR = "(?:(?<" + PARAM_YEAR + ">\\d{1,4})";
	private static final String PATTERN_DATE_ERA = "(?: (?<" + PARAM_ERA + ">[ABCE.]+))?";
	private static final Pattern PATTERN_DATE = RegexHelper.pattern("(?i)^" + PATTERN_DATE_DAY + PATTERN_DATE_MONTH
		+ PATTERN_DATE_YEAR + PATTERN_DATE_ERA + "$");


	private static class SingletonHelper{
		private static final AbstractCalendarParser INSTANCE = new JulianCalendarParser();
	}


	public static AbstractCalendarParser getInstance(){
		return SingletonHelper.INSTANCE;
	}

	@Override
	public CalendarType getCalendarType(){
		return CalendarType.JULIAN;
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
			final String era = matcher.group(PARAM_ERA);

			if(day != null)
				date.withDay(Integer.parseInt(day));
			if(month != null)
				date.withMonth(Integer.parseInt(month));
			if(year != null)
				date.withYear(Integer.parseInt(year));
			if(era != null)
				date.withEra(Era.fromDate(era));
		}
		return date;
	}

}
