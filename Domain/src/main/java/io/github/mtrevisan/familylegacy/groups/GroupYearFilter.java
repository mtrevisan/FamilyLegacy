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
package io.github.mtrevisan.familylegacy.groups;

import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;

import java.time.LocalDate;


/**
 * The filter by year of the group views: the membership bars that were not in effect in a year are taken away.
 * <p>
 * A bar with dates that does not cover the year is taken away, one with no date at all is kept (dropping it would hide a membership only
 * because nobody dated it), and so is one open on a side. The rows left with no bar disappear (see {@link GroupMembershipFilter}). The year
 * is a year of the proleptic Gregorian calendar.
 */
public final class GroupYearFilter{

	private static final long UNIX_EPOCH_JDN = 2_440_588L;


	private GroupYearFilter(){}


	/**
	 * @param model  the whole model of the view (can be {@code null})
	 * @param year   the year, {@code null} for any
	 * @return	The model with the bars in effect in the year; the model itself if there is no year or no bar is taken away.
	 */
	public static TemporalProjectionModel filter(final TemporalProjectionModel model, final Integer year){
		if(model == null || year == null)
			return model;

		final long[] range = yearRange(year);
		return GroupMembershipFilter.apply(model, entry -> covers(entry.span(), range));
	}

	/**
	 * @param span   the span of a bar or relationship
	 * @param range  the first and last JDN of a year (see {@link #yearRange(int)})
	 * @return	Whether the span is not all outside the year.
	 */
	public static boolean covers(final TemporalSpan span, final long[] range){
		return (span.minJdn() <= range[1] && span.maxJdn() >= range[0]);
	}

	/**
	 * @param year	The year (proleptic Gregorian).
	 * @return	The first and last JDN of the year.
	 */
	public static long[] yearRange(final int year){
		return new long[]{
			LocalDate.of(year, 1, 1).toEpochDay() + UNIX_EPOCH_JDN,
			LocalDate.of(year, 12, 31).toEpochDay() + UNIX_EPOCH_JDN
		};
	}

}
