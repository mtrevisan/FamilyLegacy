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
package io.github.mtrevisan.calendars.assistant;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.calendars.gregorian.CalendarReform;

import java.util.ArrayList;
import java.util.List;


/** Adds or subtracts days or weeks to a date (e.g. "40 days after the birth"). */
public final class RelativeDateTool{

	/** The unit of the distance. */
	public enum Unit{
		DAYS(1, "day"),
		WEEKS(7, "week");

		private final int days;
		private final String label;

		Unit(final int days, final String label){
			this.days = days;
			this.label = label;
		}
	}


	private RelativeDateTool(){}


	/**
	 * Computes a date at a distance from another one.
	 *
	 * @param calendarCode the code of the calendar of the base date
	 * @param baseDate     the base date, in the notation of the calendar
	 * @param amount       the distance (not negative)
	 * @param unit         the unit of the distance
	 * @param after        {@code true} for a date after the base one, {@code false} for before
	 * @return the candidates (Julian and Gregorian)
	 * @throws IllegalArgumentException if the base date cannot be parsed
	 */
	public static List<DateSuggestion> offset(final String calendarCode, final String baseDate, final int amount, final Unit unit,
			final boolean after){
		final CalendarType type = CalendarType.fromCode(calendarCode);
		final long base = type.parseToJdn(baseDate);
		return offsetFromRange(base, base, "\"" + baseDate + "\" (" + type.getCode() + ")", amount, unit, after);
	}

	/**
	 * Computes a date at a distance from another one, that may be known only as a range (e.g. the date of an event that is
	 * approximate, bounded, or known to the year only).
	 *
	 * @param minJdn    the earliest Julian Day Number of the base date
	 * @param maxJdn    the latest Julian Day Number of the base date
	 * @param baseLabel the description of the base date, for the explanation
	 * @param amount    the distance (not negative)
	 * @param unit      the unit of the distance
	 * @param after     {@code true} for a date after the base one, {@code false} for before
	 * @return for a single day, the candidates (Julian and Gregorian); for a range, the range of the result
	 */
	public static List<DateSuggestion> offsetFromRange(final long minJdn, final long maxJdn, final String baseLabel,
			final int amount, final Unit unit, final boolean after){
		final long days = (long)amount * unit.days;
		final long shift = (after? days: -days);

		final String distance = amount + " " + unit.label + (amount == 1? "": "s");
		final String meaning = distance + (after? " after ": " before ") + baseLabel;
		final List<String> assumptions = new ArrayList<>();
		assumptions.add("Days are counted exclusively of the base day, so that 1 day after is the following day");
		assumptions.add("Ancient and medieval sources often count inclusively (the base day is the first): adjust by one day if so");
		final List<String> steps = new ArrayList<>();
		if(minJdn == maxJdn){
			steps.add("Base date " + baseLabel + ": Julian Day Number " + minJdn);
			steps.add((after? "Added ": "Subtracted ") + distance + " (" + days + " days): Julian Day Number " + (minJdn + shift));
			return CalendarConversionTool.fromJdn(minJdn + shift, meaning, assumptions, steps);
		}

		assumptions.add("The base date is not a single day: the result is a range with the same uncertainty");
		steps.add("Base date " + baseLabel + ": from Julian Day Number " + minJdn + " to " + maxJdn);
		steps.add((after? "Added ": "Subtracted ") + distance + " (" + days + " days) to both: from " + (minJdn + shift) + " to "
			+ (maxJdn + shift));
		final String calendar = CivilDates.calendarInForce(minJdn + shift, CalendarReform.ROME);
		final String notBefore = CivilDates.format(minJdn + shift, calendar);
		final String notAfter = CivilDates.format(maxJdn + shift, calendar);
		steps.add("Written in the " + calendar + " calendar: from " + notBefore + " to " + notAfter);
		return List.of(new DateSuggestion("Range of the result", meaning, assumptions, calendar, steps, null, notBefore, notAfter));
	}

}
