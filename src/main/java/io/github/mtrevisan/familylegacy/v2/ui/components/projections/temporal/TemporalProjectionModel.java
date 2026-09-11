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
 * Immutable snapshot of the entire General Temporal Projection.
 * <p>
 * Produced by the projection service and consumed by the layout, renderer
 * and interaction layers. It gathers:
 * <ul>
 *   <li>{@code rows} — the horizontal lifelines, one per row entity;</li>
 *   <li>{@code connections} — temporal arcs between rows;</li>
 *   <li>{@code bands} — contextual background bands;</li>
 *   <li>{@code impactLinks} — links between bands and their targets;</li>
 *   <li>{@code domainStart} / {@code domainEnd} — the overall temporal
 *       extent, computed from all elements, or {@code null} when the model
 *       is empty.</li>
 * </ul>
 * The record is immutable: all lists are copied on construction. The domain
 * bounds are computed by the {@link #of(List, List, List, List)} factory and
 * are consistent with the content of the model at construction time.
 */
public record TemporalProjectionModel(
	List<TemporalRow> rows,
	List<TemporalConnection> connections,
	List<TemporalContextBand> bands,
	List<ContextImpactLink> impactLinks,
	NormalizedDate domainStart,
	NormalizedDate domainEnd
){

	/**
	 * Compact constructor with normalization.
	 */
	public TemporalProjectionModel{
		rows = (rows != null? List.copyOf(rows): List.of());
		connections = (connections != null? List.copyOf(connections): List.of());
		bands = (bands != null? List.copyOf(bands): List.of());
		impactLinks = (impactLinks != null? List.copyOf(impactLinks): List.of());
	}


	/**
	 * Creates a model and computes its temporal domain from the given
	 * elements.
	 *
	 * @param rows        the row lifelines; may be {@code null}
	 * @param connections the temporal arcs; may be {@code null}
	 * @param bands       the contextual background bands; may be {@code null}
	 * @param impactLinks the links between bands and their targets; may be {@code null}
	 * @return a fully populated, immutable model
	 */
	public static TemporalProjectionModel of(final List<TemporalRow> rows,
		final List<TemporalConnection> connections,
		final List<TemporalContextBand> bands,
		final List<ContextImpactLink> impactLinks){
		final NormalizedDate start = computeDomainStart(rows, connections, bands);
		final NormalizedDate end = computeDomainEnd(rows, connections, bands);
		return new TemporalProjectionModel(rows, connections, bands, impactLinks, start, end);
	}

	/**
	 * Returns an empty model with no rows, connections, bands or impact
	 * links, and a {@code null} temporal domain.
	 *
	 * @return an empty model
	 */
	public static TemporalProjectionModel empty(){
		return new TemporalProjectionModel(List.of(), List.of(), List.of(), List.of(), null, null);
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
	 * @param entity the entity to look for (must not be {@code null})
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
	 * Returns the number of entries across all rows and connections.
	 * Bands and impact links are not counted.
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


	/* ======================================================================
	 *                       Domain computation
	 * ====================================================================== */

	private static NormalizedDate computeDomainStart(final List<TemporalRow> rows,
		final List<TemporalConnection> connections, final List<TemporalContextBand> bands){
		NormalizedDate min = null;
		if(rows != null)
			for(final TemporalRow row : rows)
				for(final TemporalTrack track : row.tracks())
					for(final TemporalEntry entry : track.entries())
						min = minOrNull(min, entry.span().start());
		if(connections != null)
			for(final TemporalConnection connection : connections)
				min = minOrNull(min, connection.span().start());
		if(bands != null)
			for(final TemporalContextBand band : bands)
				min = minOrNull(min, band.span().start());
		return min;
	}

	private static NormalizedDate computeDomainEnd(final List<TemporalRow> rows,
		final List<TemporalConnection> connections, final List<TemporalContextBand> bands){
		NormalizedDate max = null;
		if(rows != null)
			for(final TemporalRow row : rows)
				for(final TemporalTrack track : row.tracks())
					for(final TemporalEntry entry : track.entries())
						max = maxOrNull(max, entry.span().effectiveEnd());
		if(connections != null)
			for(final TemporalConnection connection : connections)
				max = maxOrNull(max, connection.span().effectiveEnd());
		if(bands != null)
			for(final TemporalContextBand band : bands)
				max = maxOrNull(max, band.span().effectiveEnd());
		return max;
	}

	private static NormalizedDate minOrNull(final NormalizedDate current, final NormalizedDate candidate){
		if(candidate == null)
			return current;
		return (current == null || candidate.compareTo(current) < 0? candidate: current);
	}

	private static NormalizedDate maxOrNull(final NormalizedDate current, final NormalizedDate candidate){
		if(candidate == null)
			return current;
		return (current == null || candidate.compareTo(current) > 0? candidate: current);
	}


	@Override
	public String toString(){
		return "TemporalProjectionModel[rows=" + rows.size()
			+ ", connections=" + connections.size()
			+ ", bands=" + bands.size()
			+ ", impacts=" + impactLinks.size()
			+ ", domain=" + (hasDomain()? domainStart + " .. " + domainEnd: "(empty)")
			+ "]";
	}

}
