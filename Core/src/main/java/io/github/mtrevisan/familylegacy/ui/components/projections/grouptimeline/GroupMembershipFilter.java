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
package io.github.mtrevisan.familylegacy.ui.components.projections.grouptimeline;

import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalTrackType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;


/**
 * Takes away the membership bars that do not pass a test from a model of the group views: what the filters by role and by year have in
 * common.
 * <p>
 * A row whose membership bars are all taken away disappears, unless it has other bars (the row of the group, or of the individual, with
 * its events and attributes). The domain of the axis is computed again on what is left.
 */
final class GroupMembershipFilter{

	private GroupMembershipFilter(){}


	/**
	 * @param model  the whole model of the view (can be {@code null})
	 * @param keep   whether a membership bar stays
	 * @return	The model with the bars that stay; the model itself if no bar is taken away.
	 */
	static TemporalProjectionModel apply(final TemporalProjectionModel model, final Predicate<TemporalEntry> keep){
		if(model == null || model.isEmpty())
			return model;

		final List<TemporalRow> rows = new ArrayList<>(model.rows().size());
		boolean changed = false;
		for(final TemporalRow row : model.rows()){
			final TemporalTrack membership = row.findTrack(TemporalTrackType.MEMBERSHIP);
			if(membership == null){
				rows.add(row);
				continue;
			}

			final List<TemporalEntry> kept = new ArrayList<>(membership.entries().size());
			for(final TemporalEntry entry : membership.entries())
				if(keep.test(entry))
					kept.add(entry);
			if(kept.size() == membership.entries().size()){
				rows.add(row);
				continue;
			}

			changed = true;
			final List<TemporalTrack> tracks = new ArrayList<>(row.tracks().size());
			boolean others = false;
			for(final TemporalTrack track : row.tracks())
				if(track.type() == TemporalTrackType.MEMBERSHIP)
					tracks.add(new TemporalTrack(TemporalTrackType.MEMBERSHIP, kept, track.label()));
				else{
					tracks.add(track);
					others |= !track.isEmpty();
				}
			if(!kept.isEmpty() || others)
				rows.add(new TemporalRow(row.entity(), tracks, row.sortOrder(), row.collapsed()));
		}
		if(!changed)
			return model;

		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		return new TemporalProjectionModel(ordered, model.connections(), model.bands(), model.impactLinks(),
			GroupTimelineBuilder.domainStart(ordered), GroupTimelineBuilder.domainEnd(ordered));
	}

}
