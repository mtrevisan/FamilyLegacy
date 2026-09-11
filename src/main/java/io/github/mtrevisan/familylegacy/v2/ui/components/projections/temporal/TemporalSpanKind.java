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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.temporal;

/**
 * Mirrors the three alternatives of the FLEF {@code DateValue} union
 * ({@code point}, {@code bounded}, {@code spanning}) and drives the visual
 * encoding of a temporal span.
 */
public enum TemporalSpanKind{

	/**
	 * A single point in time. Rendered as a filled marker. Reduced‑precision
	 * forms (decade, century) are still {@code POINT} but are rendered as
	 * a shaded bar rather than as a pinpoint.
	 */
	POINT,

	/**
	 * The exact date is unknown but is known to fall within an interval.
	 * The interval expresses <i>uncertainty</i>, not duration. Rendered as
	 * a bar with faded extremities.
	 */
	BOUNDED,

	/**
	 * The fact itself extends across an interval. The interval expresses
	 * the <i>actual duration</i> of the event, status, relationship or
	 * condition. Rendered as a bar with sharp extremities.
	 */
	SPANNING;


	/**
	 * Returns whether this kind represents a duration rather than a point.
	 *
	 * @return {@code true} for {@link #BOUNDED} and {@link #SPANNING};
	 * {@code false} for {@link #POINT}
	 */
	public boolean isInterval(){
		return (this != POINT);
	}

}
