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
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.temporal.TemporalContextBand;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntry;
import io.github.mtrevisan.familylegacy.temporal.TemporalExtractor;
import io.github.mtrevisan.familylegacy.temporal.TemporalIndices;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionModel;
import io.github.mtrevisan.familylegacy.temporal.TemporalProjectionService;
import io.github.mtrevisan.familylegacy.temporal.TemporalRow;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrack;
import io.github.mtrevisan.familylegacy.temporal.TemporalTrackType;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Builds the model of the timeline of a group (view A of the group views): the same model of the general temporal projection, so
 * that its layout, axis and renderers show it.
 * <p>
 * The rows are, from the top:
 * <ol>
 *   <li>the group itself, with its own events, attributes and historical context;</li>
 *   <li>the groups it is part of ({@code part_of} another group), one row each;</li>
 *   <li>its subgroups ({@code part_of} the group), one row each;</li>
 *   <li>its members ({@code group_member} of the group), one row each, the earliest first.</li>
 * </ol>
 * A subgroup or a member has a single {@link TemporalTrackType#MEMBERSHIP} track, with an entry for each membership (an
 * individual that is member more than once, or with other roles in time, has all the bars on its row). The entry spans from
 * {@code valid_from} to {@code valid_to} of the relationship, is labeled and colored by the role, and is open on the side that has no
 * date. A membership with no date at all is kept and drawn along the whole axis, labeled "dates unknown": dropping it would hide a
 * member. The status of the relationship gives the look of the bar (fainter and dashed if {@code ended}, as it always was
 * otherwise) and an "ended" in the label. The axis always has a domain, even with open or undated bars (see {@link #domain(List)}).
 * <p>
 * A membership that has no end and is not marked as ended is still in force: its bar ends today (see {@link #ongoing(TemporalEntry)}),
 * so that it does not go on beyond today when the axis is zoomed out; a membership with no date at all begins at the left edge of the
 * window and ends today.
 * <p>
 * Records that are not visible under the current privacy policy are left out.
 */
public final class GroupTimelineBuilder{

	/** The text added to the label of a membership with no date. */
	public static final String DATES_UNKNOWN = "dates unknown";
	/** The text added to the label of a membership that has ended. */
	public static final String STATUS_ENDED_LABEL = "ended";
	/** The text added to the label of a membership that has begun and has no end: it goes on to today. */
	public static final String ONGOING = "ongoing";

	private static final String TAG_STATUS = RelationshipReader.TAG_STATUS;
	private static final long UNIX_EPOCH_JDN = 2_440_588L;
	/** The span of an axis that no date tells: a century. */
	private static final long FALLBACK_SPAN_DAYS = 36_525L;


	private GroupTimelineBuilder(){}


	/**
	 * @param model    the model
	 * @param groupId  the identifier of the group
	 * @return	The model of the timeline of the group, empty if there is no such visible group.
	 */
	public static TemporalProjectionModel build(final FLEFModel model, final String groupId){
		final FLEFRecord group = (groupId != null? model.getRecordById(groupId): null);
		if(group == null || !GroupReader.isOfType(group) || PrivacyService.visible(model, List.of(group)).isEmpty())
			return TemporalProjectionModel.empty();

		final TemporalExtractor extractor = new TemporalExtractor(model, TemporalIndices.build(model));

		final TemporalEntityRef groupRef = TemporalEntityRef.of(TemporalEntityType.GROUP, group, RecordDisplay.describe(group, model));
		final List<TemporalRow> groupRows = new ArrayList<>(1);
		final TemporalRow groupRow = new TemporalRow(groupRef, extractor.extractTracks(groupRef), 0, false);
		if(groupRow.hasEntries())
			groupRows.add(groupRow);

		final Map<String, Membership> parents = new LinkedHashMap<>();
		final Map<String, Membership> subgroups = new LinkedHashMap<>();
		final Map<String, Membership> members = new LinkedHashMap<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null)
				continue;

			final String targetGroupId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupReader.TYPE);
			//the groups this group is part of
			if(RelationshipReader.isTypePartOf(type) && targetGroupId != null && !groupId.equals(targetGroupId)
					&& groupId.equals(relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupReader.TYPE))){
				final FLEFRecord parent = model.getRecordById(targetGroupId);
				if(parent != null && GroupReader.isOfType(parent) && !PrivacyService.visible(model, List.of(parent)).isEmpty())
					parents.computeIfAbsent(targetGroupId, id -> new Membership(
							TemporalEntityRef.of(TemporalEntityType.GROUP, parent, RecordDisplay.describe(parent, model)),
							new ArrayList<>()))
						.entries()
						.add(ongoing(entryOf(relationship, type)));
				continue;
			}
			if(!groupId.equals(targetGroupId))
				continue;

			final boolean member = RelationshipReader.isTypeGroupMember(type);
			final boolean subgroup = RelationshipReader.isTypePartOf(type);
			if(!member && !subgroup)
				continue;

			//a group_member may also have a group as subject (a subgroup)
			final String individualId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			final String subjectGroupId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupReader.TYPE);
			final boolean isSubgroup = (subjectGroupId != null && individualId == null);
			final String subjectId = (isSubgroup? subjectGroupId: individualId);
			if(subjectId == null || subjectId.equals(groupId))
				continue;

			final FLEFRecord subject = model.getRecordById(subjectId);
			if(subject == null || PrivacyService.visible(model, List.of(subject)).isEmpty())
				continue;

			final Map<String, Membership> target = (isSubgroup? subgroups: members);
			target.computeIfAbsent(subjectId, id -> new Membership(
					TemporalEntityRef.of((isSubgroup? TemporalEntityType.GROUP: TemporalEntityType.INDIVIDUAL), subject,
						RecordDisplay.describe(subject, model)),
					new ArrayList<>()))
				.entries()
				.add(ongoing(entryOf(relationship, type)));
		}

		final List<TemporalRow> rows = new ArrayList<>(groupRows);
		rows.addAll(toRows(parents));
		rows.addAll(toRows(subgroups));
		rows.addAll(toRows(members));

		final List<TemporalRow> ordered = new ArrayList<>(rows.size());
		for(int i = 0; i < rows.size(); i ++)
			ordered.add(rows.get(i)
				.withSortOrder(i));
		final NormalizedDate start = domainStart(ordered);
		final NormalizedDate end = domainEnd(ordered);
		return new TemporalProjectionModel(ordered, List.of(), contextBands(model, start, end), List.of(), start, end);
	}


	/** One subgroup or member with its memberships. */
	public record Membership(TemporalEntityRef entity, List<TemporalEntry> entries){}


	public static TemporalEntry entryOf(final FLEFRecord relationship, final String type){
		final String status = statusOf(relationship);
		final TemporalSpan dated = DateNormalizer.combineBounds(
			FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM),
			FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO), status);
		final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);

		final String name = (StringUtils.isNotBlank(role)? role: type);
		final TemporalSpan span = (dated != null? dated: TemporalSpan.unbounded(status));
		final StringBuilder label = new StringBuilder(name);
		if(TemporalSpan.STATUS_ENDED.equals(status))
			label.append(" (").append(STATUS_ENDED_LABEL).append(')');
		if(dated == null)
			label.append(" (").append(DATES_UNKNOWN).append(')');
		return new TemporalEntry(span, label.toString(), type, role, relationship, null);
	}

	/**
	 * A membership that is still in force ends today: a bar must not go on beyond today, whatever the zoom or the position of the window.
	 *
	 * @param entry	The bar of a membership.
	 * @return	The bar with its end set to today if it has no end, is not marked as ended and does not begin after today; if it has a start,
	 * 	"ongoing" is added to the label. A bar with no date at all begins at the edge of the window as before. The bar itself is returned
	 * 	otherwise.
	 */
	public static TemporalEntry ongoing(final TemporalEntry entry){
		final TemporalSpan span = entry.span();
		final long today = today();
		final boolean dated = isDated(span.start());
		if(TemporalSpan.STATUS_ENDED.equals(span.status()) || span.isPoint() || span.maxJdn() != Long.MAX_VALUE
				|| (dated && span.start().jdn() > today))
			return entry;

		final NormalizedDate start = (span.start() != null? span.start(): NormalizedDate.exact(Long.MIN_VALUE, DatePrecision.YEAR));
		return new TemporalEntry(TemporalSpan.spanning(start, NormalizedDate.exact(today, DatePrecision.DAY), span.status()),
			(dated? entry.label() + " (" + ONGOING + ")": entry.label()), entry.type(), entry.role(), entry.sourceRecord(), entry.evidence());
	}

	private static long today(){
		return LocalDate.now().toEpochDay() + UNIX_EPOCH_JDN;
	}

	/**
	 * @param relationship	The relationship of a membership.
	 * @return	Its status: {@code active} or {@code ended} as the relationship says, {@code unknown} if it says nothing else.
	 */
	public static String statusOf(final FLEFRecord relationship){
		final String status = FLEFRecordHelper.getChildValue(relationship, TAG_STATUS);
		return (TemporalSpan.STATUS_ACTIVE.equalsIgnoreCase(status)? TemporalSpan.STATUS_ACTIVE
			: (TemporalSpan.STATUS_ENDED.equalsIgnoreCase(status)? TemporalSpan.STATUS_ENDED: TemporalSpan.STATUS_UNKNOWN));
	}

	/** @return	The rows of the given memberships: the earliest first, those with no date last, then by name. */
	public static List<TemporalRow> toRows(final Map<String, Membership> memberships){
		final List<TemporalRow> rows = new ArrayList<>(memberships.size());
		for(final Membership membership : memberships.values())
			rows.add(new TemporalRow(membership.entity(),
				List.of(new TemporalTrack(TemporalTrackType.MEMBERSHIP, membership.entries(), "Membership")), 0, false));

		rows.sort(Comparator.comparingLong(GroupTimelineBuilder::firstStart)
			.thenComparing(row -> row.entity().displayLabel(), String.CASE_INSENSITIVE_ORDER)
			.thenComparing(row -> row.entity().id()));
		return rows;
	}

	private static long firstStart(final TemporalRow row){
		long first = Long.MAX_VALUE;
		for(final TemporalTrack track : row.tracks())
			for(final TemporalEntry entry : track.entries())
				if(isDated(entry.span().start()))
					first = Math.min(first, entry.span().start().jdn());
		return first;
	}


	/**
	 * The historical context of the view: the historic events and the cultural norms that overlap the axis, the earliest first. They are
	 * the background of the bars and do not stretch the axis.
	 *
	 * @param model  the model
	 * @param start  the first date of the axis ({@code null} if there is none)
	 * @param end    the last date of the axis
	 * @return	The bands to draw behind the rows.
	 */
	public static List<TemporalContextBand> contextBands(final FLEFModel model, final NormalizedDate start, final NormalizedDate end){
		if(start == null || end == null)
			return List.of();

		final List<TemporalContextBand> bands = new ArrayList<>();
		for(final TemporalContextBand band : new TemporalProjectionService(model).contextBands())
			if(band.span().maxJdn() >= start.jdn() && band.span().minJdn() <= end.jdn())
				bands.add(band);
		bands.sort(Comparator.comparingLong((TemporalContextBand band) -> band.span().minJdn())
			.thenComparing(TemporalContextBand::label, String.CASE_INSENSITIVE_ORDER));
		return bands;
	}


	/* ======================================================================
	 *                          Domain
	 * ====================================================================== */

	/** The unbounded span of a membership with no date has sentinel bounds: they must not stretch the axis. */
	private static boolean isDated(final NormalizedDate date){
		return (date != null && date.jdn() != Long.MIN_VALUE && date.jdn() != Long.MAX_VALUE);
	}

	/** @return	The first date of the axis: the earliest date of the bars (see {@link #domain(List)}). */
	public static NormalizedDate domainStart(final List<TemporalRow> rows){
		return domain(rows)[0];
	}

	/** @return	The last date of the axis: the latest date of the bars (see {@link #domain(List)}). */
	public static NormalizedDate domainEnd(final List<TemporalRow> rows){
		return domain(rows)[1];
	}

	/**
	 * The extent of the axis: from the earliest to the latest date of the bars. A side that no bar dates is made up, so that the axis
	 * always has a domain, and the bars with a side open or with no date are not left out of a view that is empty: a membership that has no
	 * end lasts to today (or a year after its start if that is in the future), one that has no start begins a century before its end, and
	 * a view with no date at all shows the last century. If some bar is open (no end) but others are dated, the axis goes on after the start of
	 * the latest open one, by a fifth of what is dated (a year at least), so that the bar is seen.
	 *
	 * @param rows	The rows of the view.
	 * @return	The first and last date of the axis, both {@code null} if there is no row.
	 */
	public static NormalizedDate[] domain(final List<TemporalRow> rows){
		if(rows.isEmpty())
			return new NormalizedDate[]{null, null};

		NormalizedDate min = null;
		NormalizedDate max = null;
		//the latest start of a bar that has no end: it has to be seen, so the axis goes on after it
		long latestOpenStart = Long.MIN_VALUE;
		for(final TemporalRow row : rows)
			for(final TemporalTrack track : row.tracks())
				for(final TemporalEntry entry : track.entries()){
					final NormalizedDate start = entry.span().start();
					if(isDated(start) && (min == null || start.compareTo(min) < 0))
						min = start;
					final NormalizedDate end = entry.span().effectiveEnd();
					if(isDated(end) && (max == null || end.compareTo(max) > 0))
						max = end;
					if(isDated(start) && !isDated(end))
						latestOpenStart = Math.max(latestOpenStart, start.jdn());
				}
		if(min != null && max != null && latestOpenStart != Long.MIN_VALUE){
			//a fifth of what is dated, a year at least
			final long needed = latestOpenStart + Math.max(365L, (max.jdn() - min.jdn()) / 5L);
			if(max.jdn() < needed)
				max = NormalizedDate.exact(needed, DatePrecision.DAY);
		}

		final long today = LocalDate.now().toEpochDay() + UNIX_EPOCH_JDN;
		if(min == null && max == null)
			return new NormalizedDate[]{NormalizedDate.exact(today - FALLBACK_SPAN_DAYS, DatePrecision.DAY),
				NormalizedDate.exact(today, DatePrecision.DAY)};
		if(min == null)
			return new NormalizedDate[]{NormalizedDate.exact(max.jdn() - FALLBACK_SPAN_DAYS, DatePrecision.DAY), max};
		if(max == null)
			return new NormalizedDate[]{min, NormalizedDate.exact(Math.max(today, min.jdn() + 365L), DatePrecision.DAY)};
		return new NormalizedDate[]{min, max};
	}

}
