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
package io.github.mtrevisan.calendars.dateparsing;


/**
 * The components of a date written with a named month, as read by {@link NamedMonthDateParser}.
 *
 * @param year  the year (from 1)
 * @param month the month (from 1), a negative {@code CalendarSeason} flag for a season (an astronomical event, with no
 *              day), or {@code 0} if missing
 * @param day   the day of the month, or {@code 0} if missing
 */
public record YearMonthDay(int year, int month, int day){

	/**
	 * @param month the month, or {@code 0} if missing
	 * @return the month, or the first one of the year if missing
	 */
	public static int monthOrFirst(final int month){
		return (month != 0? month: 1);
	}

	/**
	 * @param day the day, or {@code 0} if missing
	 * @return the day, or the first one of the month if missing
	 */
	public static int dayOrFirst(final int day){
		return (day > 0? day: 1);
	}

	/** @return the month, or the first one of the year if missing */
	public int monthOrDefault(){
		return monthOrFirst(month);
	}

	/** @return the day, or the first one of the month if missing */
	public int dayOrDefault(){
		return dayOrFirst(day);
	}

	/** @return whether the date is a season (an equinox or a solstice) of the year */
	public boolean isSeason(){
		return (month < 0);
	}

}
