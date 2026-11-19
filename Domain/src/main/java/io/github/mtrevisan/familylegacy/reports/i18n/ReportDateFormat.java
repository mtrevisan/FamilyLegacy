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
package io.github.mtrevisan.familylegacy.reports.i18n;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;


/**
 * How the dates of a report are written. It applies to the dates of the Gregorian calendar that are stored as {@code yyyy-mm-dd},
 * {@code yyyy-mm} or {@code yyyy}; a date written otherwise (the original text, another calendar) is left as it is.
 */
public enum ReportDateFormat{

	/** As stored (ISO, {@code 1888-05-01}). */
	AS_RECORDED(null, null, null),
	/** {@code 1 May 1888}, with the month in the language of the report. */
	DAY_MONTH_YEAR("d MMMM yyyy", "MMMM yyyy", "yyyy"),
	/** {@code May 1, 1888}, with the month in the language of the report. */
	MONTH_DAY_YEAR("MMMM d, yyyy", "MMMM yyyy", "yyyy"),
	/** {@code 1888}. */
	YEAR_ONLY("yyyy", "yyyy", "yyyy");


	private final String dayPattern;
	private final String monthPattern;
	private final String yearPattern;


	ReportDateFormat(final String dayPattern, final String monthPattern, final String yearPattern){
		this.dayPattern = dayPattern;
		this.monthPattern = monthPattern;
		this.yearPattern = yearPattern;
	}


	/**
	 * @param value	The stored value of a date.
	 * @param locale	The language of the report.
	 * @return	The date written in this format; the value itself if it is not one this format can write.
	 */
	public String format(final String value, final Locale locale){
		if(dayPattern == null || value == null)
			return value;

		try{
			final LocalDate day = parseDay(value);
			if(day != null)
				return DateTimeFormatter.ofPattern(dayPattern, locale)
					.format(day);

			final YearMonth month = parseMonth(value);
			if(month != null)
				return DateTimeFormatter.ofPattern(monthPattern, locale)
					.format(month);

			final int year = parseYear(value);
			if(year > 0)
				return DateTimeFormatter.ofPattern(yearPattern, locale)
					.format(Year.of(year));
		}
		catch(final DateTimeException ignored){
			//not a date this format can write
		}
		return value;
	}

	private static LocalDate parseDay(final String value){
		try{
			final LocalDate date = LocalDate.parse(value.strip());
			return (date.getYear() > 0? date: null);
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}

	private static YearMonth parseMonth(final String value){
		try{
			final YearMonth month = YearMonth.parse(value.strip());
			return (month.getYear() > 0? month: null);
		}
		catch(final DateTimeParseException ignored){
			return null;
		}
	}

	/** @return	The year if the value is only a year of four digits, else 0. */
	private static int parseYear(final String value){
		final String text = value.strip();
		if(text.length() != 4)
			return 0;
		for(int i = 0; i < text.length(); i ++)
			if(!Character.isDigit(text.charAt(i)))
				return 0;
		return Integer.parseInt(text);
	}

}
