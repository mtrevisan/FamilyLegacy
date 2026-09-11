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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Computes the vertical layout of the General Temporal Projection.
 * <p>
 * The layout is <b>viewport‑independent</b>: it produces only vertical
 * coordinates (row positions, track positions, entry lanes). Horizontal
 * coordinates are resolved by the renderer against the {@link TemporalAxis}
 * on every repaint, so panning and zooming do not require a re‑layout.
 * <p>
 * Within each track, overlapping entries are assigned to separate
 * <b>lanes</b> using a greedy interval‑coloring pass, so that concurrent
 * events, attributes or contexts are drawn on distinct sub‑lines without
 * collision. The lane assignment is deterministic: entries are sorted by
 * start date, then by end date, and each entry is placed in the first lane
 * whose last entry ends before the new entry starts.
 * <p>
 * Instances are mutable: {@link #compute(TemporalProjectionModel)} replaces
 * the current layout. The class is not thread‑safe.
 */
public final class TemporalProjectionLayout{

	/** Width, in pixels, reserved on the left of the axis for the row labels. */
	public static final int ROW_HEADER_WIDTH = 220;

	/** Height, in pixels, of a single entry bar. */
	public static final int ENTRY_HEIGHT = 14;

	/** Vertical gap, in pixels, between two lanes inside the same track. */
	public static final int ENTRY_GAP = 2;

	/** Vertical padding, in pixels, above and below the entries of a track. */
	public static final int TRACK_PADDING = 4;

	/** Vertical gap, in pixels, between two consecutive tracks of the same row. */
	public static final int TRACK_GAP = 6;

	/** Vertical padding, in pixels, above and below the tracks of a row. */
	public static final int ROW_PADDING = 6;

	/** Vertical gap, in pixels, between two consecutive rows. */
	public static final int ROW_GAP = 12;

	/** Height, in pixels, of a collapsed row (header only). */
	public static final int COLLAPSED_ROW_HEIGHT = 22;


	private List<RowLayout> rows = List.of();
	private int totalHeight;


	/* ======================================================================
	 *                          Computation
	 * ====================================================================== */

	/**
	 * Recomputes the layout from the given projection model. Replaces any
	 * previously computed layout.
	 *
	 * @param model the projection model (may be {@code null})
	 */
	public void compute(final TemporalProjectionModel model){
		if(model == null || model.isEmpty()){
			rows = List.of();
			totalHeight = 0;
			return;
		}

		final List<RowLayout> computedRows = new ArrayList<>(model.rows()
			.size());
		int y = 0;
		for(final TemporalRow row : model.rows()){
			final RowLayout rowLayout = computeRow(row, y);
			computedRows.add(rowLayout);
			y += rowLayout.height() + ROW_GAP;
		}
		// Remove the trailing gap so that totalHeight hugs the content.
		if(!computedRows.isEmpty())
			y -= ROW_GAP;

		rows = Collections.unmodifiableList(computedRows);
		totalHeight = Math.max(0, y);
	}

	private RowLayout computeRow(final TemporalRow row, final int yStart){
		if(row.collapsed())
			return new RowLayout(row.entity(), yStart, COLLAPSED_ROW_HEIGHT, Map.of());

		final List<TemporalTrack> sortedTracks = new ArrayList<>(row.tracks());
		sortedTracks.sort(Comparator.comparingInt(TemporalTrack::sortOrder));

		final Map<TemporalTrackType, TrackLayout> trackLayouts = new LinkedHashMap<>();
		int y = yStart + ROW_PADDING;
		for(final TemporalTrack track : sortedTracks){
			if(track.isEmpty())
				continue;
			final TrackLayout trackLayout = computeTrack(track, y);
			trackLayouts.put(track.type(), trackLayout);
			y += trackLayout.height() + TRACK_GAP;
		}
		if(!trackLayouts.isEmpty())
			y -= TRACK_GAP;
		y += ROW_PADDING;

		return new RowLayout(row.entity(), yStart, y - yStart,
			Collections.unmodifiableMap(trackLayouts));
	}

	private TrackLayout computeTrack(final TemporalTrack track, final int yStart){
		// Sort by start date, then by end date; point spans have start == end.
		final List<TemporalEntry> sorted = new ArrayList<>(track.entries());
		sorted.sort((a, b) -> {
			int cmp = Long.compare(a.span().minJdn(), b.span().minJdn());
			if(cmp == 0)
				cmp = Long.compare(a.span().maxJdn(), b.span().maxJdn());
			return cmp;
		});

		// Greedy interval‑coloring: assign each entry to the first lane whose
		// last entry ends strictly before the new entry starts.
		final List<Long> laneEnds = new ArrayList<>();
		final Map<TemporalEntry, Integer> entryLane = new LinkedHashMap<>();
		for(final TemporalEntry entry : sorted){
			final long start = entry.span().minJdn();
			final long end = entry.span().maxJdn();

			int lane = -1;
			for(int i = 0; i < laneEnds.size(); i++){
				if(laneEnds.get(i) < start){
					lane = i;
					break;
				}
			}
			if(lane < 0){
				lane = laneEnds.size();
				laneEnds.add(end);
			}
			else
				laneEnds.set(lane, end);

			entryLane.put(entry, lane);
		}

		final int laneCount = Math.max(1, laneEnds.size());
		final int height = TRACK_PADDING * 2 + laneCount * ENTRY_HEIGHT + (laneCount - 1) * ENTRY_GAP;

		return new TrackLayout(track.type(), yStart, height, laneCount,
			Collections.unmodifiableMap(entryLane));
	}


	/* ======================================================================
	 *                          Accessors
	 * ====================================================================== */

	/**
	 * Returns the computed row layouts, in vertical order.
	 *
	 * @return an immutable list, never {@code null}
	 */
	public List<RowLayout> rows(){
		return rows;
	}

	/**
	 * Returns the total height of the projection in pixels, including all
	 * rows and gaps but excluding any outer padding.
	 *
	 * @return the total height
	 */
	public int totalHeight(){
		return totalHeight;
	}

	/**
	 * Returns whether the layout contains no rows.
	 *
	 * @return {@code true} if {@link #rows()} is empty
	 */
	public boolean isEmpty(){
		return rows.isEmpty();
	}

	/**
	 * Finds the layout of the row anchored to the given entity.
	 *
	 * @param entity the row entity to look for (must not be {@code null})
	 * @return the matching row layout, or {@code null}
	 */
	public RowLayout findRow(final TemporalEntityRef entity){
		for(final RowLayout row : rows)
			if(row.entity().equals(entity))
				return row;
		return null;
	}

	/**
	 * Returns the vertical center of the given row, or {@code -1} if the row
	 * is not part of the layout.
	 *
	 * @param entity the row entity
	 * @return the vertical center in pixels, or {@code -1}
	 */
	public int rowCenterY(final TemporalEntityRef entity){
		final RowLayout row = findRow(entity);
		return (row != null? row.y() + row.height() / 2: -1);
	}


	/* ======================================================================
	 *                          Nested records
	 * ====================================================================== */

	/**
	 * Vertical layout of a single row.
	 *
	 * @param entity the row entity
	 * @param y      the top edge of the row in pixels
	 * @param height the total height of the row in pixels
	 * @param tracks the per‑track layouts, keyed by track type; empty for
	 *               collapsed rows
	 */
	public record RowLayout(TemporalEntityRef entity, int y, int height, Map<TemporalTrackType, TrackLayout> tracks){

		public RowLayout{
			if(entity == null)
				throw new IllegalArgumentException("Row entity must not be null");
			tracks = (tracks != null? tracks: Map.of());
		}

		/**
		 * Returns the bottom edge of the row.
		 *
		 * @return {@code y + height}
		 */
		public int bottom(){
			return y + height;
		}

		/**
		 * Returns the layout of the given track, or {@code null} if the row
		 * does not declare it (empty tracks are omitted by the layout).
		 *
		 * @param type the track type
		 * @return the matching track layout, or {@code null}
		 */
		public TrackLayout findTrack(final TemporalTrackType type){
			return tracks.get(type);
		}
	}

	/**
	 * Vertical layout of a single track inside a row.
	 * <p>
	 * The map {@code entryLane} associates each entry with its lane index
	 * (0 = top lane). The vertical position of an entry within the track is:
	 * <pre>
	 *   entryY = track.y + TRACK_PADDING + lane * (ENTRY_HEIGHT + ENTRY_GAP)
	 * </pre>
	 * The renderer combines this vertical position with the horizontal
	 * extent obtained from {@link TemporalAxis#spanToRect(TemporalSpan, int, int)}.
	 *
	 * @param type      the track type
	 * @param y         the top edge of the track in pixels
	 * @param height    the total height of the track in pixels
	 * @param laneCount the number of lanes used by the entries of this track
	 * @param entryLane the lane assignment for each entry
	 */
	public record TrackLayout(TemporalTrackType type, int y, int height, int laneCount,
									  Map<TemporalEntry, Integer> entryLane){

		public TrackLayout{
			if(type == null)
				throw new IllegalArgumentException("Track type must not be null");
			if(laneCount < 0)
				throw new IllegalArgumentException("Lane count must not be negative");
			entryLane = (entryLane != null? entryLane: Map.of());
		}

		/**
		 * Returns the vertical position of an entry inside this track, or
		 * {@code -1} if the entry does not belong to this track.
		 *
		 * @param entry the entry to look up (must not be {@code null})
		 * @return the top edge in pixels, or {@code -1}
		 */
		public int entryY(final TemporalEntry entry){
			final Integer lane = entryLane.get(entry);
			if(lane == null)
				return -1;
			return y + TRACK_PADDING + lane * (ENTRY_HEIGHT + ENTRY_GAP);
		}
	}

}
