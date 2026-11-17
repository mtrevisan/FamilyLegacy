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

import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;

import java.util.List;


/**
 * Immutable snapshot of a timeline drawn by the temporal panel (the group timeline, the career of an individual, the timeline of the
 * jurisdictions).
 * <p>
 * It gathers:
 * <ul>
 *   <li>{@code rows} — the horizontal lifelines, one per row entity;</li>
 *   <li>{@code bands} — contextual background bands;</li>
 *   <li>{@code domainStart} / {@code domainEnd} — the overall temporal extent, or {@code null} when the model is empty.</li>
 * </ul>
 * The record is immutable: all lists are copied on construction.
 */
public record TemporalProjectionModel(
	List<TemporalRow> rows,
	List<TemporalContextBand> bands,
	NormalizedDate domainStart,
	NormalizedDate domainEnd
){

	/**
	 * Compact constructor with normalization.
	 */
	public TemporalProjectionModel{
		rows = (rows != null? List.copyOf(rows): List.of());
		bands = (bands != null? List.copyOf(bands): List.of());
	}


	/**
	 * Returns an empty model with no rows or bands, and a {@code null} temporal domain.
	 *
	 * @return an empty model
	 */
	public static TemporalProjectionModel empty(){
		return new TemporalProjectionModel(List.of(), List.of(), null, null);
	}


	/**
	 * Returns whether this model contains no rows.
	 *
	 * @return {@code true} if there are no rows
	 */
	public boolean isEmpty(){
		return rows.isEmpty();
	}

	/**
	 * Returns whether the temporal domain of this model has been computed.
	 * An empty model has no domain.
	 *
	 * @return {@code true} if both domain bounds are non‑null
	 */
	public boolean hasDomain(){
		return (domainStart != null && domainEnd != null);
	}

	/**
	 * Finds the row anchored to the given entity.
	 *
	 * @param entity the entity to look for(must not be {@code null})
	 * @return the matching row, or {@code null}
	 */
	public TemporalRow findRow(final TemporalEntityRef entity){
		for(final TemporalRow row : rows)
			if(row.entity().equals(entity))
				return row;
		return null;
	}

	/**
	 * Returns whether the given entity owns a row in this model.
	 *
	 * @param entity the entity to test (must not be {@code null})
	 * @return {@code true} if a row exists for the entity
	 */
	public boolean hasRow(final TemporalEntityRef entity){
		return (findRow(entity) != null);
	}

	/**
	 * Returns the number of entries across all rows.
	 * Bands are not counted.
	 *
	 * @return the total entry count
	 */
	public int totalEntryCount(){
		int count = 0;
		for(final TemporalRow row : rows)
			for(final TemporalTrack track : row.tracks())
				count += track.size();
		return count;
	}


	@Override
	public String toString(){
		return "TemporalProjectionModel[rows=" + rows.size()
			+ ", bands=" + bands.size()
			+ ", domain=" + (hasDomain()? domainStart + " .. " + domainEnd: "(empty)")
			+ "]";
	}

}
