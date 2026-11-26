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
package io.github.mtrevisan.familylegacy.io.model.readers.date;


/**
 * A date typed in a short notation (e.g. {@code abt 1850 ±2}, {@code bet 1850 and 1860}, {@code from 1850}), already split into the pieces of
 * a {@code DateValue}: it is what {@link DateShorthandReader#parse(String)} gives, ready to fill a date editor.
 * <p>
 * The calendar is not part of the notation: it is the one of the date that is being edited.
 * </p>
 *
 * @param kind	The shape of the date.
 * @param first	The point, the {@code not_before} bound (bounded) or the {@code from} bound (spanning), {@code null} if open.
 * @param second	{@code null} for a point, otherwise the {@code not_after} bound (bounded) or the {@code to} bound (spanning), {@code null}
 * 	if open.
 */
public record DateShorthand(Kind kind, Single first, Single second){

	/** The shape of the date value. */
	public enum Kind{

		/** A single point in time ({@code point}). */
		POINT,
		/** An uncertainty interval ({@code bounded}): {@code first} is the {@code not_before}, {@code second} the {@code not_after}. */
		BOUNDED,
		/** A period ({@code spanning}): {@code first} is the {@code from}, {@code second} the {@code to}. */
		SPANNING

	}

	/**
	 * A {@code SingleDate} (without calendar).
	 *
	 * @param fullDate	The full date, in the notation of the calendar (the {@code (language)} suffix included), or {@code null}.
	 * @param decade	The first year of the decade, or {@code null}.
	 * @param century	The ordinal of the century, or {@code null}.
	 * @param centuryPart	The part of the century (one of {@link DateReader#CENTURY_PARTS}), or {@code null}.
	 * @param basis	The basis of the approximation (one of {@link DateReader#BASES}), {@code null} if the date is not approximate.
	 * @param margin	The margin of the approximation, as an ISO 8601 duration (e.g. {@code P2Y}), or {@code null}.
	 */
	public record Single(String fullDate, Integer decade, Integer century, String centuryPart, String basis, String margin){

		/** @return	Whether the date is marked as approximate. */
		public boolean approximate(){
			return (basis != null);
		}

	}

}
