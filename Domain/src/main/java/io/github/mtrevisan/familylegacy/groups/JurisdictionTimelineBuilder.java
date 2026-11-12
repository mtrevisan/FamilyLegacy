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
package io.github.mtrevisan.familylegacy.groups;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalExtractor;
import io.github.mtrevisan.familylegacy.temporal.TemporalIndices;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds the model of the timeline of the jurisdictions of a place: the same model of the general temporal projection, so that its
 * layout, axis and renderers show it.
 * <p>
 * The rows are, from the top:
 * <ol>
 *   <li>the place itself, with the historical context that touches it;</li>
 *   <li>the places it is related to, as the subject of a {@code place_relationship} (it is part of them, administratively,
 *   ecclesiastically, judicially, ... or it is the successor of them), one row each;</li>
 *   <li>the places that are related to it, as the target of a {@code place_relationship} (they are part of it, or are its
 *   successors), one row each.</li>
 * </ol>
 * Each row has a single {@link io.github.mtrevisan.familylegacy.temporal.TemporalTrackType#MEMBERSHIP}
 * track, with a bar for each relationship, from its {@code valid_from} to its {@code valid_to}; a place that changed jurisdiction,
 * that is a parish that went from a diocese to another, has the two bars on the two rows, one after the other. The bar is colored
 * and labeled by the type of the relationship (the jurisdiction), so that the legend of the group views also hides a kind of
 * jurisdiction; a bar of a relationship that points to the place, and not away from it, says "incoming". A relationship with no date is
 * kept and drawn along the whole axis, labeled "dates unknown", as in the group views (see {@link GroupTimelineBuilder}).
 * <p>
 * A relationship that has no end is still in force: its bar ends today, as in the group views (see
 * {@link GroupTimelineBuilder#ongoing(TemporalEntry)}).
 * <p>
 * Records that are not visible under the current privacy policy are left out.
 */
public final class JurisdictionTimelineBuilder{

	/** The text added to the label of a relationship that points to the place, and does not go away from it. */
	public static final String INCOMING = "incoming";


	private JurisdictionTimelineBuilder(){}


	/**
	 * @param model    the model
	 * @param placeId  the identifier of the place
	 * @return	The model of the timeline of the jurisdictions of the place, empty if there is no such visible place.
	 */
	public static TemporalProjectionModel build(final FLEFModel model, final String placeId){
		final FLEFRecord place = (placeId != null? model.getRecordById(placeId): null);
		if(!PlaceReader.isOfType(place) || PrivacyService.visible(model, List.of(place)).isEmpty())
			return TemporalProjectionModel.empty();

		final TemporalExtractor extractor = new TemporalExtractor(model, TemporalIndices.build(model));

		final TemporalEntityRef placeRef = TemporalEntityRef.of(TemporalEntityType.PLACE, place,
			RecordDisplay.describe(place, model));
		final List<TemporalRow> placeRows = new ArrayList<>(1);
		final TemporalRow placeRow = new TemporalRow(placeRef, extractor.extractTracks(placeRef), 0, false);
		if(placeRow.hasEntries())
			placeRows.add(placeRow);

		final Map<String, GroupTimelineBuilder.Membership> outgoing = new LinkedHashMap<>();
		final Map<String, GroupTimelineBuilder.Membership> incoming = new LinkedHashMap<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(PlaceRelationshipReader.TYPE))){
			final String type = PlaceRelationshipReader.extractType(relationship);
			final String subjectId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE);
			final String targetId = relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE);
			if(type == null || subjectId == null || targetId == null || subjectId.equals(targetId))
				continue;

			final boolean isOutgoing = placeId.equals(subjectId);
			if(!isOutgoing && !placeId.equals(targetId))
				continue;

			final String otherId = (isOutgoing? targetId: subjectId);
			final FLEFRecord other = model.getRecordById(otherId);
			if(!PlaceReader.isOfType(other) || PrivacyService.visible(model, List.of(other)).isEmpty())
				continue;

			(isOutgoing? outgoing: incoming).computeIfAbsent(otherId, id -> new GroupTimelineBuilder.Membership(
					TemporalEntityRef.of(TemporalEntityType.PLACE, other, RecordDisplay.describe(other, model)),
					new ArrayList<>()))
				.entries()
				.add(jurisdictionEntry(relationship, type, !isOutgoing));
		}

		final List<TemporalRow> rows = new ArrayList<>(placeRows);
		rows.addAll(GroupTimelineBuilder.toRows(outgoing));
		rows.addAll(GroupTimelineBuilder.toRows(incoming));

		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		final NormalizedDate start = GroupTimelineBuilder.domainStart(ordered);
		final NormalizedDate end = GroupTimelineBuilder.domainEnd(ordered);
		return new TemporalProjectionModel(ordered, List.of(), GroupTimelineBuilder.contextBands(model, start, end), List.of(), start, end);
	}

	/**
	 * @param relationship	The place relationship.
	 * @param type			Its type (the jurisdiction).
	 * @param incoming		Whether it points to the place shown.
	 * @return	The bar of the relationship: the type is also its "role", that colors it and the legend lists.
	 */
	public static TemporalEntry jurisdictionEntry(final FLEFRecord relationship, final String type, final boolean incoming){
		final TemporalEntry entry = GroupTimelineBuilder.ongoing(GroupTimelineBuilder.entryOf(relationship, type));
		return new TemporalEntry(entry.span(), (incoming? entry.label() + " (" + INCOMING + ")": entry.label()), type, type, relationship,
			null);
	}

}
