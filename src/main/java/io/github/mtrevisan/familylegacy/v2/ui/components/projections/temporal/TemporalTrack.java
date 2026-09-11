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

import org.apache.commons.lang3.StringUtils;

import java.util.List;


/**
 * A homogeneous group of {@link TemporalEntry} attached to a row.
 * <p>
 * Tracks partition the temporal entries of a row by their origin in the FLEF
 * protocol (events, attributes, context). The partition is used by the
 * renderer to lay out entries on separate sub‑lines within the row and by
 * the interaction handler to apply per‑track filters.
 * <p>
 * The record is immutable. The {@code entries} list is copied on
 * construction; passing {@code null} yields an empty track. Entries are not
 * sorted by the record itself: sorting, when needed, is performed by the
 * layout stage against a {@link TemporalAxis}.
 */
public record TemporalTrack(TemporalTrackType type, List<TemporalEntry> entries, String label){

	/**
	 * Compact constructor with normalization.
	 */
	public TemporalTrack{
		if(type == null)
			throw new IllegalArgumentException("Track type must not be null");
		entries = (entries != null? List.copyOf(entries): List.of());
		if(label == null)
			label = StringUtils.EMPTY;
	}


	/**
	 * Returns whether this track contains no entries.
	 *
	 * @return {@code true} if the track is empty
	 */
	public boolean isEmpty(){
		return entries.isEmpty();
	}

	/**
	 * Returns the number of entries in this track.
	 *
	 * @return the entry count
	 */
	public int size(){
		return entries.size();
	}

	/**
	 * Returns the sort order of this track within a row, delegating to
	 * {@link TemporalTrackType#getSortOrder()}.
	 *
	 * @return the sort order
	 */
	public int sortOrder(){
		return type.getSortOrder();
	}

	@Override
	public String toString(){
		return type + " (" + entries.size() + " entries)";
	}

}
