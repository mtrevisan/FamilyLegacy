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
package io.github.mtrevisan.calendars.gregorian;


/**
 * Data container for parsed Gregorian calendar components.
 * Supports both structural monthly indices and exact negative seasonal tracking flags.
 *
 * @param year  The Gregorian year
 * @param month The Gregorian month (1 to 12 for standard months, negative integers for astronomical seasons, or 0 if missing)
 * @param day   The day of the month, or 0 if missing
 * @param dualYear Whether the year was written as a dual year (e.g. "1704/5"), and so is already counted from 1 January
 * @param indiction The indiction written next to the year (1 to 15), or 0 if missing (see {@link Indiction})
 */
public record GregorianDateInput(int year, int month, int day, boolean dualYear, int indiction){

	public GregorianDateInput(final int year, final int month, final int day){
		this(year, month, day, false, 0);
	}

	/**
	 * @return the month (or the seasonal flag), or the first month of the year if the month is missing
	 */
	public int monthOrFirst(){
		return (month != 0? month: 1);
	}

	/**
	 * @return the day of the month, or the first day of the month if the day is missing
	 */
	public int dayOrFirst(){
		return (day > 0? day: 1);
	}

	/**
	 * Verifies that the 29th of February falls in a leap year.
	 *
	 * @param leapYear whether the year is a leap year in the target calendar
	 * @throws IllegalArgumentException if the date is the 29th of February of a common year
	 */
	public void validateLeapDay(final boolean leapYear){
		if(month == 2 && day == 29 && !leapYear)
			throw new IllegalArgumentException("29 February does not exist in the common year " + year);
	}

}
