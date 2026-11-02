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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalExtractor;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalIndices;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds the model of the group career of an individual (view C of the group views): the memberships of a person in time, with the
 * role of each period. As for {@link GroupTimelineBuilder}, it is the model of the general temporal projection, shown by its panel.
 * <p>
 * The rows are, from the top:
 * <ol>
 *   <li>the individual, with its events (birth, marriage, death...), attributes (occupation...) and historical context, to read the
 *   career against the life;</li>
 *   <li>the groups it is a member of ({@code group_member}), one row each, the earliest first.</li>
 * </ol>
 * A group row has a single {@link io.github.mtrevisan.familylegacy.ui.components.projections.temporal.TemporalTrackType#MEMBERSHIP}
 * track with a bar for each period (more than one if the individual left and came back, or changed role), labeled and colored by the
 * role. A membership with no date is kept along the whole axis, labeled "dates unknown".
 * <p>
 * Records that are not visible under the current privacy policy are left out.
 */
public final class IndividualCareerBuilder{

	private IndividualCareerBuilder(){}


	/**
	 * @param model         the model
	 * @param individualId  the identifier of the individual
	 * @return	The model of the career of the individual, empty if there is no such visible individual or it has neither a membership nor a
	 * 	dated event or attribute.
	 */
	public static TemporalProjectionModel build(final FLEFModel model, final String individualId){
		final FLEFRecord individual = (individualId != null? model.getRecordById(individualId): null);
		if(individual == null || !IndividualHandler.isOfType(individual) || PrivacyService.visible(model, List.of(individual)).isEmpty())
			return TemporalProjectionModel.empty();

		final Map<String, GroupTimelineBuilder.Membership> groups = new LinkedHashMap<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipHandler.TYPE))){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !RelationshipReader.isTypeGroupMember(type)
					|| !individualId.equals(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE)))
				continue;

			final String groupId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupHandler.TYPE);
			final FLEFRecord group = (groupId != null? model.getRecordById(groupId): null);
			if(group == null || PrivacyService.visible(model, List.of(group)).isEmpty())
				continue;

			groups.computeIfAbsent(groupId, id -> new GroupTimelineBuilder.Membership(
					TemporalEntityRef.of(TemporalEntityType.GROUP, group, GroupTimelineBuilder.label(GroupHandler.getInstance(), model, group)),
					new ArrayList<>()))
				.entries()
				.add(GroupTimelineBuilder.entryOf(relationship, type));
		}

		final TemporalExtractor extractor = new TemporalExtractor(model, TemporalIndices.build(model));
		final TemporalEntityRef individualRef = TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, individual,
			GroupTimelineBuilder.label(IndividualHandler.getInstance(), model, individual));
		final TemporalRow individualRow = new TemporalRow(individualRef, extractor.extractTracks(individualRef), 0, false);

		final List<TemporalRow> rows = new ArrayList<>();
		if(individualRow.hasEntries())
			rows.add(individualRow);
		rows.addAll(GroupTimelineBuilder.toRows(groups));
		if(rows.isEmpty())
			return TemporalProjectionModel.empty();

		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		return new TemporalProjectionModel(ordered, List.of(), List.of(), List.of(), GroupTimelineBuilder.domainStart(ordered),
			GroupTimelineBuilder.domainEnd(ordered));
	}

}
