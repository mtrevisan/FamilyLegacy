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
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalExtractor;
import io.github.mtrevisan.familylegacy.temporal.TemporalIndices;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;

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
 * A group row has a single {@link io.github.mtrevisan.familylegacy.temporal.TemporalTrackType#MEMBERSHIP}
 * track with a bar for each period (more than one if the individual left and came back, or changed role), labeled and colored by the
 * role. A side of a membership that has no date is the limit of the life of the individual (birth, death), as far as it is known: a
 * membership with no date at all spans the whole life, and is labeled "dates unknown".
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
		if(individual == null || !IndividualReader.isOfType(individual) || PrivacyService.visible(model, List.of(individual)).isEmpty())
			return TemporalProjectionModel.empty();

		final Map<String, GroupTimelineBuilder.Membership> groups = new LinkedHashMap<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !RelationshipReader.isTypeGroupMember(type)
					|| !individualId.equals(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE)))
				continue;

			final String groupId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupReader.TYPE);
			final FLEFRecord group = (groupId != null? model.getRecordById(groupId): null);
			if(group == null || PrivacyService.visible(model, List.of(group)).isEmpty())
				continue;

			groups.computeIfAbsent(groupId, id -> new GroupTimelineBuilder.Membership(
					TemporalEntityRef.of(TemporalEntityType.GROUP, group, RecordDisplay.describe(group, model)),
					new ArrayList<>()))
				.entries()
				.add(GroupTimelineBuilder.entryOf(relationship, type));
		}

		final TemporalExtractor extractor = new TemporalExtractor(model, TemporalIndices.build(model));
		final TemporalEntityRef individualRef = TemporalEntityRef.of(TemporalEntityType.INDIVIDUAL, individual,
			RecordDisplay.describe(individual, model));
		final TemporalRow individualRow = new TemporalRow(individualRef, extractor.extractTracks(individualRef), 0, false);

		final List<TemporalRow> all = new ArrayList<>();
		if(individualRow.hasEntries())
			all.add(individualRow);
		all.addAll(GroupTimelineBuilder.toRows(groups));
		if(all.isEmpty())
			return TemporalProjectionModel.empty();

		final List<TemporalRow> rows = withinLife(all, individualRow);

		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		final NormalizedDate start = GroupTimelineBuilder.domainStart(ordered);
		final NormalizedDate end = GroupTimelineBuilder.domainEnd(ordered);
		return new TemporalProjectionModel(ordered, List.of(), GroupTimelineBuilder.contextBands(model, start, end), List.of(), start, end);
	}

	/**
	 * A side of a membership that no date tells is the limit of the life of the individual: it cannot begin before the birth nor go on
	 * after the death. A side with no such limit (no birth, or no death, known) is left as it is.
	 */
	private static List<TemporalRow> withinLife(final List<TemporalRow> rows, final TemporalRow individualRow){
		NormalizedDate birth = null;
		NormalizedDate death = null;
		for(final TemporalTrack track : individualRow.tracks())
			for(final TemporalEntry entry : track.entries()){
				if(birth == null && entry.hasType(EventReader.ENUM_TYPE_BIRTH))
					birth = entry.span().start();
				else if(death == null && entry.hasType(EventReader.ENUM_TYPE_DEATH))
					death = entry.span().effectiveEnd();
			}
		//with no known birth (death) the life is taken as beginning (ending) with the earliest (latest) date known of the individual
		if(birth == null || death == null){
			NormalizedDate earliest = null;
			NormalizedDate latest = null;
			for(final TemporalTrack track : individualRow.tracks()){
				if(track.type() == TemporalTrackType.CONTEXT)
					continue;

				for(final TemporalEntry entry : track.entries()){
					final NormalizedDate start = entry.span().start();
					final NormalizedDate end = entry.span().effectiveEnd();
					if(isDated(start) && (earliest == null || start.compareTo(earliest) < 0))
						earliest = start;
					if(isDated(end) && (latest == null || end.compareTo(latest) > 0))
						latest = end;
					if(isDated(start) && (latest == null || start.compareTo(latest) > 0))
						latest = start;
				}
			}
			if(birth == null)
				birth = earliest;
			if(death == null)
				death = latest;
		}
		if(birth == null && death == null)
			return rows;

		final List<TemporalRow> result = new ArrayList<>(rows.size());
		for(final TemporalRow row : rows){
			final List<TemporalTrack> tracks = new ArrayList<>(row.tracks().size());
			for(final TemporalTrack track : row.tracks()){
				final List<TemporalEntry> entries = new ArrayList<>(track.entries().size());
				for(final TemporalEntry entry : track.entries())
					//the historical context is not of the individual
					entries.add(track.type() == TemporalTrackType.CONTEXT || entry.hasType(EventReader.ENUM_TYPE_BIRTH)
						|| entry.hasType(EventReader.ENUM_TYPE_DEATH)? entry: withinLife(entry, birth, death));
				tracks.add(new TemporalTrack(track.type(), entries, track.label()));
			}
			result.add(new TemporalRow(row.entity(), tracks, row.sortOrder(), row.collapsed()));
		}
		return result;
	}

	private static TemporalEntry withinLife(final TemporalEntry entry, final NormalizedDate birth, final NormalizedDate death){
		final TemporalSpan span = entry.span();
		if(span.isPoint())
			return entry;

		NormalizedDate start = span.start();
		NormalizedDate end = span.end();
		if(birth != null && !isDated(start))
			start = birth;
		if(death != null && !isDated(end))
			end = death;
		if((start == span.start() && end == span.end()) || (start != null && end != null && start.compareTo(end) > 0))
			return entry;

		return new TemporalEntry(new TemporalSpan(span.kind(), start, end, span.status()), entry.label(), entry.type(), entry.role(),
			entry.sourceRecord(), entry.evidence());
	}

	private static boolean isDated(final NormalizedDate date){
		return (date != null && date.jdn() != Long.MIN_VALUE && date.jdn() != Long.MAX_VALUE);
	}

}
