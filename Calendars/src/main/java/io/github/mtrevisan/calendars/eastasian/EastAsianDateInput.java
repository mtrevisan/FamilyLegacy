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
package io.github.mtrevisan.calendars.eastasian;

import io.github.mtrevisan.calendars.spi.CalendarMetadata;


/**
 * Data container for a date counted in the years of an era.
 *
 * @param era       The identifier of the era the year is counted in
 * @param year      The year of the era (1 is the first year)
 * @param month     The month (1 to 12), or 0 if missing
 * @param leapMonth Whether the month is a leap month (閏月) of a lunisolar calendar
 * @param day       The day of the month, 0 if missing, or {@link #LAST_DAY} for the last day of the month (晦日)
 */
public record EastAsianDateInput(String era, int year, int month, boolean leapMonth, int day){

	/** The last day of the month, whatever its length (晦日, "misoka"). */
	public static final int LAST_DAY = -1;


	/**
	 * @return the month, or the first month of the year if the month is missing
	 */
	public int monthOrFirst(){
		return (month != 0? month: 1);
	}

	/**
	 * @return the day of the month, or the first day of the month if the day is missing ({@link #LAST_DAY} is kept)
	 */
	public int dayOrFirst(){
		return (day != 0? day: 1);
	}

	/**
	 * @return which components the date expression contains
	 */
	public CalendarMetadata toMetadata(){
		return CalendarMetadata.createFromYearMonthDay(year, month, day);
	}

}
