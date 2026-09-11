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

import java.util.List;


/**
 * A single horizontal lifeline in the General Temporal Projection.
 * <p>
 * A row is anchored to a row entity (individual, group or place) and
 * carries all the temporal tracks built for that entity. The row does not
 * contain relationship arcs; those are represented separately as
 * {@link TemporalConnection} instances and rendered between rows.
 * <p>
 * The record is immutable. {@code tracks} is copied on construction; passing
 * {@code null} yields an empty list. {@code sortOrder} is a hint used by the
 * layout stage to order rows within the projection; it may be recomputed at
 * any time without affecting the row's identity.
 */
public record TemporalRow(TemporalEntityRef entity, List<TemporalTrack> tracks, int sortOrder, boolean collapsed){

	/**
	 * Compact constructor with validation.
	 */
	public TemporalRow{
		if(entity == null)
			throw new IllegalArgumentException("Row entity must not be null");
		if(!entity.isRowEntity())
			throw new IllegalArgumentException("A row can only be anchored to a row entity, got: " + entity.type());
		tracks = (tracks != null? List.copyOf(tracks): List.of());
	}


	/**
	 * Returns whether this row has any entries across its tracks.
	 *
	 * @return {@code true} if at least one track contains at least one entry
	 */
	public boolean hasEntries(){
		for(final TemporalTrack track : tracks)
			if(!track.isEmpty())
				return true;
		return false;
	}

	/**
	 * Returns the track of the given type, or {@code null} if the row does
	 * not declare that track.
	 *
	 * @param type the track type to look for (must not be {@code null})
	 * @return the matching track, or {@code null}
	 */
	public TemporalTrack findTrack(final TemporalTrackType type){
		for(final TemporalTrack track : tracks)
			if(track.type() == type)
				return track;
		return null;
	}

	/**
	 * Returns a copy of this row with the given collapsed flag.
	 *
	 * @param collapsed the new collapsed state
	 * @return a new row
	 */
	public TemporalRow withCollapsed(final boolean collapsed){
		return new TemporalRow(entity, tracks, sortOrder, collapsed);
	}

	/**
	 * Returns a copy of this row with the given sort order.
	 *
	 * @param sortOrder the new sort order
	 * @return a new row
	 */
	public TemporalRow withSortOrder(final int sortOrder){
		return new TemporalRow(entity, tracks, sortOrder, collapsed);
	}

	@Override
	public String toString(){
		return entity + " [" + tracks.size() + " tracks]";
	}

}
