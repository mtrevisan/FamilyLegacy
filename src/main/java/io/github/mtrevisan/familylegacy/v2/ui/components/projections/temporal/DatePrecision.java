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
 * Granularity of a normalized date used by the General Temporal Projection.
 * <p>
 * Corresponds to the alternatives of the FLEF {@code SingleDate} union:
 * {@code full_date} maps to {@link #DAY}, {@link #MONTH} or {@link #YEAR}
 * depending on how many fields are present; {@code decade} maps to
 * {@link #DECADE}; {@code century} maps to {@link #CENTURY}.
 * <p>
 * Precision is <i>declared</i>, not inferred from formatting. A normalized
 * value always carries its precision explicitly.
 */
public enum DatePrecision{

	/** Exact day. */
	DAY(0),

	/** Month only, day unknown. */
	MONTH(1),

	/** Year only, month and day unknown. */
	YEAR(2),

	/** A whole decade, identified by its starting year. */
	DECADE(3),

	/** A whole century, identified by its ordinal. */
	CENTURY(4);


	private final int granularity;


	DatePrecision(final int granularity){
		this.granularity = granularity;
	}


	/**
	 * Returns whether this precision is coarser than a full date.
	 * Reduced precisions are rendered as shaded bars rather than as
	 * pinpoint markers.
	 *
	 * @return {@code true} for {@link #DECADE} and {@link #CENTURY};
	 * {@code false} otherwise
	 */
	public boolean isReduced(){
		return (this == DECADE || this == CENTURY);
	}

	/**
	 * Returns whether this precision identifies a single calendar year or
	 * finer. Useful to decide whether an entry can be compared against
	 * yearly axis ticks.
	 *
	 * @return {@code true} for {@link #DAY}, {@link #MONTH}, {@link #YEAR};
	 * {@code false} for {@link #DECADE} and {@link #CENTURY}
	 */
	public boolean isYearOrFiner(){
		return (this == DAY || this == MONTH || this == YEAR);
	}

	/**
	 * Returns the granularity rank of this precision. Higher values mean
	 * coarser precision. Used to compare two normalized dates and to select
	 * the coarsest precision when composing a span.
	 *
	 * @return a non‑negative rank; {@code DAY == 0}, {@code CENTURY == 4}
	 */
	public int getGranularity(){
		return granularity;
	}

	/**
	 * Returns the coarser of two precisions.
	 *
	 * @param other the other precision (must not be {@code null})
	 * @return the precision with the higher granularity rank
	 */
	public DatePrecision coarsest(final DatePrecision other){
		return (granularity >= other.granularity? this: other);
	}

}
