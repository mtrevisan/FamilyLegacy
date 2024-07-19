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
package io.github.mtrevisan.familylegacy.flef.helpers.parsers;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;


/** When a range or imprecise date value is found, what is the preference for handling it? */
public enum DatePreciseness{

	/** Return the earliest reasonable value for the interpreted date or range. */
	FAVOR_EARLIEST{
		@Override
		LocalDate applyToMonth(final LocalDate date){
			return date.withDayOfMonth(1);
		}

		@Override
		LocalDate applyToYear(final LocalDate date){
			return date.withDayOfYear(1);
		}

		@Override
		LocalDate applyToRange(final String[] dates, final BiFunction<String, DatePreciseness, LocalDate> parser){
			return parser.apply(dates[0], this);
		}
	},

	/** Return the latest reasonable value for the interpreted date or range. */
	FAVOR_LATEST{
		@Override
		LocalDate applyToMonth(final LocalDate date){
			//last day of month
			return date.withDayOfMonth(1)
				.plusMonths(1)
				.plusDays(-1);
		}

		@Override
		LocalDate applyToYear(final LocalDate date){
			//last day of year
			return date.withMonth(Month.DECEMBER.getValue())
				.withDayOfMonth(Month.DECEMBER.maxLength());
		}

		@Override
		LocalDate applyToRange(final String[] dates, final BiFunction<String, DatePreciseness, LocalDate> parser){
			return parser.apply(dates[1], this);
		}
	},

	/**
	 * Return the midpoint between the earliest and latest possible values for the interpreted date or range. For example, if a
	 * value of "1900" is supplied, the value returned is 1900-07-02 (July 2, 1900). "JUL 1900" is supplied, the value returned
	 * is 1900-07-15 (July 15). If the supplied value is not a range (i.e., there is only one date), return as precise a value
	 * as possible.
	 */
	FAVOR_MIDPOINT{
		@Override
		LocalDate applyToMonth(final LocalDate date){
			//middle day of month
			return date.withDayOfMonth(date.lengthOfMonth() / 2);
		}

		@Override
		LocalDate applyToYear(final LocalDate date){
			//middle day of year
			return date.withDayOfYear(date.lengthOfYear() / 2);
		}

		@Override
		LocalDate applyToRange(final String[] dates, final BiFunction<String, DatePreciseness, LocalDate> parser){
			final LocalDate d1 = parser.apply(dates[0], FAVOR_EARLIEST);
			final LocalDate d2 = parser.apply(dates[1], FAVOR_LATEST);
			return getMidpointOfDateRange(d1, d2);
		}

		/**
		 * Get the midpoint between two dates
		 *
		 * @param d1	first date
		 * @param d2	second date
		 * @return	the midpoint between the two dates
		 */
		private LocalDate getMidpointOfDateRange(final LocalDate d1, final LocalDate d2){
			final long daysBetween = ChronoUnit.DAYS.between(d1, d2);
			return LocalDate.from(d1)
				.plusDays(daysBetween / 2);
		}
	},

	/**
	 * Return as precise a date as possible. For ranges and periods where more than one date is supplied (e.g., FROM 17 JUL 2016
	 * TO 31 JUL 2016), use the first of the two dates.
	 */
	PRECISE{
		@Override
		LocalDate applyToMonth(final LocalDate date){
			return date;
		}

		@Override
		LocalDate applyToYear(final LocalDate date){
			return date;
		}

		@Override
		LocalDate applyToRange(final String[] dates, final BiFunction<String, DatePreciseness, LocalDate> parser){
			return parser.apply(dates[0], this);
		}
	};


	abstract LocalDate applyToMonth(final LocalDate date);

	abstract LocalDate applyToYear(final LocalDate date);

	abstract LocalDate applyToRange(final String[] dates, final BiFunction<String, DatePreciseness, LocalDate> parser);

}
