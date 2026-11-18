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
package io.github.mtrevisan.calendars.liturgical;


/**
 * A feast of the liturgical year, either on a fixed day or movable.
 *
 * @param id        an identifier of the feast
 * @param kind      how the day of the feast is computed
 * @param month     the month of a fixed feast (1 to 12)
 * @param day       the day of a fixed feast
 * @param offset    the days from Easter Sunday, or from the first Sunday of Advent, of a movable feast
 * @param leapShift whether a fixed feast at the end of February moves forward by one day in leap years (the bissextile
 *                  day was inserted after 23 February, so that, e.g., Saint Matthias falls on 25 February)
 */
public record Feast(String id, Kind kind, int month, int day, int offset, boolean leapShift){

	/** How the day of a feast is computed. */
	public enum Kind{
		/** On a fixed day of the year. */
		FIXED,
		/** A number of days from Easter Sunday. */
		EASTER,
		/** A number of days from the first Sunday of Advent (the Sunday from 27 November to 3 December). */
		ADVENT
	}


	static Feast fixed(final String id, final int month, final int day){
		return new Feast(id, Kind.FIXED, month, day, 0, false);
	}

	static Feast fixedWithLeapShift(final String id, final int month, final int day){
		return new Feast(id, Kind.FIXED, month, day, 0, true);
	}

	static Feast easter(final String id, final int offset){
		return new Feast(id, Kind.EASTER, 0, 0, offset, false);
	}

	static Feast advent(final String id, final int offset){
		return new Feast(id, Kind.ADVENT, 0, 0, offset, false);
	}

}
