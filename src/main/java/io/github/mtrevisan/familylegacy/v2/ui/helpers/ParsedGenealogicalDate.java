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
package io.github.mtrevisan.familylegacy.v2.ui.helpers;

import java.time.LocalDate;


/**
 * Represents a parsed genealogical date with precision and approximation metadata.
 *
 * @param isoDate      the normalized ISO date
 * @param precision    the temporal precision (e.g., EXACT, YEAR_ONLY)
 * @param approximate  whether the date is marked as approximate/uncertain
 * @param rawInput     the original raw string input
 * @param calendarType the calendar system used
 */
public record ParsedGenealogicalDate(
	LocalDate isoDate,
	DatePrecision precision,
	boolean approximate,
	String rawInput,
	CalendarType calendarType
)implements Comparable<ParsedGenealogicalDate>{

	/** Regular expression matching common genealogical approximation indicators. */
	public static final String APPROXIMATION_REGEX = "(?i)\\b(circa|ca\\.?|c\\.?|abt\\.?|about|approx\\.?)\\b|[?~]";


	@Override
	public int compareTo(final ParsedGenealogicalDate other){
		if(other == null)
			return 1;
		if(this.isoDate == null && other.isoDate == null)
			return 0;
		if(this.isoDate == null)
			return -1;
		if(other.isoDate == null)
			return 1;
		return this.isoDate.compareTo(other.isoDate);
	}


	public enum DatePrecision{
		EXACT,
		YEAR_MONTH,
		MONTH_DAY,
		YEAR_ONLY
	}

}
