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
package io.github.mtrevisan.calendars.regnal;

import io.github.mtrevisan.calendars.gregorian.CalendarReform;
import io.github.mtrevisan.calendars.gregorian.JulianCalendarStrategy;
import io.github.mtrevisan.calendars.liturgical.Computus;

import java.util.List;


/**
 * A reign (or pontificate) whose years date the documents.
 *
 * @param id         an identifier of the reign
 * @param names      the normalized names of the ruler (one or more words each)
 * @param ordinal    the ordinal of the ruler (e.g. 5 for Pius V), or 0 if the ruler has none
 * @param countStart the day from which the regnal years are counted, in the calendar of the reform
 * @param validFrom  the first day on which the reign dates the documents
 * @param validTo    the last day of the reign, or {@code null} if it is ongoing
 * @param rule       how the regnal years begin
 */
public record Reign(String id, List<String> names, int ordinal, CivilDate countStart, CivilDate validFrom,
		CivilDate validTo, YearRule rule){

	/** How the regnal years begin. */
	public enum YearRule{
		/** On the anniversary of the start (on the same nominal day, also after the Gregorian reform). */
		ANNIVERSARY,
		/** On Ascension Day (King John of England). */
		ASCENSION,
		/** On the anniversary of the start, written as two years ("1 & 2 Philip and Mary"), the second one counting. */
		DOUBLE_ANNIVERSARY
	}

	/**
	 * A day of the civil calendar (Julian before the reform, Gregorian after).
	 *
	 * @param year  the year
	 * @param month the month
	 * @param day   the day
	 */
	public record CivilDate(int year, int month, int day){

		static CivilDate parse(final String iso){
			final String[] fields = iso.split("-");
			return new CivilDate(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
		}

		long toJdn(final CalendarReform reform){
			return reform.toJdn(year, month, day);
		}

	}


	/**
	 * Computes the first day of a regnal year.
	 *
	 * @param regnalYear         the regnal year (from 1)
	 * @param reform             the Gregorian reform in force
	 * @param julianAnniversary  whether a reign started before the reform keeps its anniversaries in the Julian calendar
	 *                           after the reform (as George II, whose years began on 22 June, the Julian 11 June, from 1753)
	 * @return the Julian Day Number of the first day of the regnal year
	 */
	public long yearStartJdn(final int regnalYear, final CalendarReform reform, final boolean julianAnniversary){
		if(rule == YearRule.ASCENSION){
			if(regnalYear == 1)
				return countStart.toJdn(reform);

			// Ascension Day, 39 days after Easter
			return Computus.easterJdn(countStart.year() + regnalYear - 1, reform) + 39;
		}

		final int year = countStart.year() + regnalYear - 1;
		final int month = countStart.month();
		int day = countStart.day();
		if(julianAnniversary && !reform.isGregorian(countStart.year(), countStart.month(), countStart.day()))
			return JulianCalendarStrategy.toJdn(year, month, day);

		// an anniversary on a day that does not exist (29 February, or a day skipped by the reform) moves to the next day
		while(true){
			try{
				return reform.toJdn(year, month, day);
			}
			catch(final IllegalArgumentException iae){
				if(day > reform.monthLength(year, month))
					return reform.toJdn(year, month, reform.monthLength(year, month)) + 1;
				day ++;
			}
		}
	}

	/**
	 * @param jdn    a Julian Day Number
	 * @param reform the Gregorian reform in force
	 * @return whether the day falls within the reign
	 */
	public boolean contains(final long jdn, final CalendarReform reform){
		return (jdn >= validFrom.toJdn(reform) && (validTo == null || jdn <= validTo.toJdn(reform)));
	}

}
