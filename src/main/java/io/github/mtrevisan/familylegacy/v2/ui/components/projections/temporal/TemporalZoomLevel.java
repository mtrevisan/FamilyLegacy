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
 * Zoom level of the temporal axis in the General Temporal Projection.
 * <p>
 * The levels are ordered from the widest (millennium) to the narrowest
 * (day). {@link #next()} returns the narrower level (zoom in) and
 * {@link #previous()} returns the wider level (zoom out), within the
 * bounds of the enumeration.
 */
public enum TemporalZoomLevel{

	/** Millennium scale. */
	MILLENNIUM(0),

	/** Century scale. */
	CENTURY(1),

	/** Decade scale. */
	DECADE(2),

	/** Year scale. */
	YEAR(3),

	/** Month scale. */
	MONTH(4),

	/** Day scale. */
	DAY(5);


	private final int ordinalIndex;


	TemporalZoomLevel(final int ordinalIndex){
		this.ordinalIndex = ordinalIndex;
	}


	/**
	 * Returns the narrower zoom level (one step toward {@link #DAY}).
	 *
	 * @return the next narrower level, or {@code this} if already at {@link #DAY}
	 */
	public TemporalZoomLevel next(){
		final TemporalZoomLevel[] values = values();
		return (ordinalIndex + 1 < values.length? values[ordinalIndex + 1]: this);
	}

	/**
	 * Returns the wider zoom level (one step toward {@link #MILLENNIUM}).
	 *
	 * @return the next wider level, or {@code this} if already at {@link #MILLENNIUM}
	 */
	public TemporalZoomLevel previous(){
		final TemporalZoomLevel[] values = values();
		return (ordinalIndex > 0? values[ordinalIndex - 1]: this);
	}

	/**
	 * Returns whether this zoom level is narrower than the given one.
	 *
	 * @param other the level to compare against (must not be {@code null})
	 * @return {@code true} if this level is narrower (closer to {@link #DAY})
	 */
	public boolean isNarrowerThan(final TemporalZoomLevel other){
		return (ordinalIndex > other.ordinalIndex);
	}

	/**
	 * Returns the natural date precision associated with this zoom level.
	 * Used by the axis renderer to align ticks with the entity data and by
	 * the span renderer to decide the visual treatment of low‑precision
	 * dates.
	 *
	 * @return the matching precision
	 */
	public DatePrecision toDatePrecision(){
		return switch(this){
			case MILLENNIUM, CENTURY -> DatePrecision.CENTURY;
			case DECADE -> DatePrecision.DECADE;
			case YEAR -> DatePrecision.YEAR;
			case MONTH -> DatePrecision.MONTH;
			case DAY -> DatePrecision.DAY;
		};
	}

}
