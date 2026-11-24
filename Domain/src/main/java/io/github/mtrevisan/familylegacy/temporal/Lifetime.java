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
package io.github.mtrevisan.familylegacy.temporal;

import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;

import java.util.List;


/**
 * The days an individual lived, as far as the dates tell: from the first day of the birth to the last day of the death (a year, if the
 * date says only the year). A side that no birth (death) tells is taken from the earliest (latest) date of the individual, and said to be
 * <i>estimated</i>: the timelines use the lifetime to choose the historic events that overlap it, and do not compute an age from an
 * estimated birth.
 *
 * @param start	The first day (a Julian day number), {@link Long#MIN_VALUE} if the individual has no date at all.
 * @param end	The last day, {@link Long#MAX_VALUE} if the individual has no date at all.
 * @param startEstimated	Whether there is no birth: the start is the earliest date of the individual.
 * @param endEstimated	Whether there is no death: the end is the latest date of the individual.
 * @param birth	The birth, {@code null} if there is none that is dated.
 * @param death	The death, {@code null} if there is none that is dated.
 */
public record Lifetime(long start, long end, boolean startEstimated, boolean endEstimated, TemporalEntry birth, TemporalEntry death){

	/** The lifetime of an individual with no date. */
	public static final Lifetime UNKNOWN = new Lifetime(Long.MIN_VALUE, Long.MAX_VALUE, true, true, null, null);


	/**
	 * @param entries	The events and the attributes of the individual.
	 * @return	Its lifetime: {@link #UNKNOWN} if none of them is dated.
	 */
	public static Lifetime of(final List<TemporalEntry> entries){
		TemporalEntry birth = null;
		TemporalEntry death = null;
		long earliest = Long.MAX_VALUE;
		long latest = Long.MIN_VALUE;
		for(final TemporalEntry entry : entries){
			final TemporalSpan span = entry.span();
			final boolean datedStart = isDated(span.minJdn());
			final long last = lastDay(span);
			if(datedStart){
				earliest = Math.min(earliest, span.minJdn());
				latest = Math.max(latest, span.minJdn());
				if(birth == null && entry.hasType(EventReader.ENUM_TYPE_BIRTH))
					birth = entry;
			}
			if(isDated(last)){
				latest = Math.max(latest, last);
				earliest = Math.min(earliest, last);
				if(death == null && entry.hasType(EventReader.ENUM_TYPE_DEATH))
					death = entry;
			}
		}
		if(earliest == Long.MAX_VALUE || latest == Long.MIN_VALUE)
			return UNKNOWN;

		final long start = (birth != null? birth.span().minJdn(): earliest);
		final long end = (death != null? lastDay(death.span()): latest);
		return new Lifetime(start, Math.max(start, end), birth == null, death == null, birth, death);
	}

	/**
	 * @param span	A span.
	 * @return	The last day of the span: the last day of the period for a point (a date with only the year goes to the end of the year),
	 * 	the end for an interval, {@link Long#MAX_VALUE} if it has none.
	 */
	public static long lastDay(final TemporalSpan span){
		if(span.isPoint())
			return span.start().jdn() + span.start().precisionSpanInDays() - 1;
		return span.maxJdn();
	}

	private static boolean isDated(final long jdn){
		return (jdn != Long.MIN_VALUE && jdn != Long.MAX_VALUE);
	}


	/** @return	Whether any date of the individual is known. */
	public boolean isKnown(){
		return (start != Long.MIN_VALUE);
	}

	/**
	 * @param span	A span.
	 * @return	Whether the span has a day in common with the lifetime (an open side is as far as it can be); never if the lifetime is unknown.
	 */
	public boolean overlaps(final TemporalSpan span){
		return (isKnown() && span.minJdn() <= end && lastDay(span) >= start);
	}

	/**
	 * @param jdn	A day.
	 * @return	The age in whole years at that day, {@code null} if the birth is not known, or the day is before the birth or after a known death.
	 */
	public Integer ageAt(final long jdn){
		if(startEstimated || !isDated(jdn) || jdn < start || (!endEstimated && jdn > end))
			return null;

		return (int)((jdn - start) / 365.2425);
	}

}
