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
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateService;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Builds the network of the groups around a group (view B of the group views, see {@link GroupNetwork}).
 * <p>
 * The groups reached from the center through at most {@code maxDepth} relationships ({@code part_of} and {@code associate}, in either
 * direction) are in the network, with the relationships between them. The view is always filtered like this, because the groups of a
 * file make a graph that is not readable as a whole. The kinds of relationship to follow can be chosen: a kind that is left out is neither
 * drawn nor followed. With a year, a relationship (or a membership) that has dates and does not cover that year is left out, one with no
 * date at all is kept: dropping it would hide a link only because nobody dated it.
 * <p>
 * The members of a group are the individuals with a {@code group_member} relationship to it in effect; if the members of the subgroups
 * count too, the individuals of every group that is part of it (directly or not) are added, each one once.
 * <p>
 * Records that are not visible under the current privacy policy are left out.
 */
public final class GroupNetworkBuilder{

	/** The default number of relationships away from the center. */
	public static final int DEFAULT_DEPTH = 2;


	private GroupNetworkBuilder(){}


	/**
	 * Builds the network with every kind of relationship and the direct members only.
	 *
	 * @param model     the model
	 * @param centerId  the identifier of the group at the center
	 * @param maxDepth  how many relationships away from the center a group can be (0 is the center alone)
	 * @param year      the year to restrict the network to, {@code null} for any
	 * @return	The network, empty if there is no such visible group.
	 */
	public static GroupNetwork build(final FLEFModel model, final String centerId, final int maxDepth, final Integer year){
		return build(model, centerId, maxDepth, year, EnumSet.allOf(GroupNetwork.Kind.class), false);
	}

	/**
	 * @param model                  the model
	 * @param centerId               the identifier of the group at the center
	 * @param maxDepth               how many relationships away from the center a group can be (0 is the center alone)
	 * @param year                   the year to restrict the network to, {@code null} for any
	 * @param kinds                  the kinds of relationship to show and follow
	 * @param includeSubgroupMembers whether the members of the subgroups count for a group
	 * @return	The network, empty if there is no such visible group.
	 */
	public static GroupNetwork build(final FLEFModel model, final String centerId, final int maxDepth, final Integer year,
			final Set<GroupNetwork.Kind> kinds, final boolean includeSubgroupMembers){
		final FLEFRecord center = (centerId != null? model.getRecordById(centerId): null);
		if(!GroupReader.isOfType(center) || PrivacyService.visible(model, List.of(center)).isEmpty())
			return GroupNetwork.empty();

		final long[] yearJdns = (year != null? GroupYearFilter.yearRange(year): null);
		final int depth = Math.max(0, maxDepth);

		final List<GroupNetwork.Edge> allEdges = new ArrayList<>();
		final Map<String, List<String>> neighbors = new HashMap<>();
		//the groups that are part of a group, whatever the kinds shown: they are what the members of the subgroups are counted from
		final Map<String, List<String>> parts = new HashMap<>();
		final List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			final GroupNetwork.Kind kind = (RelationshipReader.isTypePartOf(type)? GroupNetwork.Kind.PART_OF
				: (RelationshipReader.isTypeAssociate(type)? GroupNetwork.Kind.ASSOCIATE: null));
			if(kind == null)
				continue;

			final String firstId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, GroupReader.TYPE);
			final String secondId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupReader.TYPE);
			if(firstId == null || secondId == null || firstId.equals(secondId) || !isVisibleGroup(model, firstId)
					|| !isVisibleGroup(model, secondId) || !inEffect(relationship, yearJdns))
				continue;

			if(kind == GroupNetwork.Kind.PART_OF)
				parts.computeIfAbsent(secondId, id -> new ArrayList<>())
					.add(firstId);
			if(!kinds.contains(kind))
				continue;

			allEdges.add(new GroupNetwork.Edge(kind, firstId, secondId, relationship,
				FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE), datesOf(relationship)));
			neighbors.computeIfAbsent(firstId, id -> new ArrayList<>())
				.add(secondId);
			neighbors.computeIfAbsent(secondId, id -> new ArrayList<>())
				.add(firstId);
		}

		//the groups at most `depth` relationships away from the center
		final Set<String> included = new HashSet<>();
		included.add(centerId);
		final Deque<String> queue = new ArrayDeque<>();
		final Map<String, Integer> distance = new HashMap<>();
		queue.add(centerId);
		distance.put(centerId, 0);
		while(!queue.isEmpty()){
			final String id = queue.poll();
			final int next = distance.get(id) + 1;
			if(next > depth)
				continue;

			for(final String neighbor : neighbors.getOrDefault(id, List.of()))
				if(included.add(neighbor)){
					distance.put(neighbor, next);
					queue.add(neighbor);
				}
		}

		final Map<String, Set<String>> individuals = individualsByGroup(model, yearJdns);
		final List<GroupNetwork.Node> nodes = new ArrayList<>(included.size());
		for(final String id : included){
			final FLEFRecord record = model.getRecordById(id);
			nodes.add(new GroupNetwork.Node(id, RecordDisplay.describe(record, model), record,
				countMembers(id, individuals, parts, includeSubgroupMembers), id.equals(centerId)));
		}
		nodes.sort(Comparator.comparing(GroupNetwork.Node::center).reversed()
			.thenComparing(GroupNetwork.Node::label, String.CASE_INSENSITIVE_ORDER)
			.thenComparing(GroupNetwork.Node::id));

		final List<GroupNetwork.Edge> edges = new ArrayList<>();
		for(final GroupNetwork.Edge edge : allEdges)
			if(included.contains(edge.firstId()) && included.contains(edge.secondId()))
				edges.add(edge);
		return new GroupNetwork(centerId, depth, year, nodes, edges);
	}


	private static boolean isVisibleGroup(final FLEFModel model, final String id){
		final FLEFRecord group = model.getRecordById(id);
		return (GroupReader.isOfType(group) && !PrivacyService.visible(model, List.of(group)).isEmpty());
	}

	/** The individuals that are members of each group, with a membership in effect (with no date, or covering the year). */
	private static Map<String, Set<String>> individualsByGroup(final FLEFModel model, final long[] yearJdns){
		final Map<String, Set<String>> individualsByGroup = new HashMap<>();
		for(final FLEFRecord relationship : PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE))){
			final String type = RelationshipReader.extractType(relationship);
			if(!RelationshipReader.isTypeGroupMember(type))
				continue;

			final String groupId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, GroupReader.TYPE);
			final String individualId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE);
			if(groupId == null || individualId == null || !inEffect(relationship, yearJdns))
				continue;

			final FLEFRecord individual = model.getRecordById(individualId);
			if(individual == null || PrivacyService.visible(model, List.of(individual)).isEmpty())
				continue;

			individualsByGroup.computeIfAbsent(groupId, id -> new HashSet<>())
				.add(individualId);
		}
		return individualsByGroup;
	}

	/** The members of a group: its own, and if wanted those of every group that is part of it, directly or not (a cycle is not followed). */
	private static int countMembers(final String groupId, final Map<String, Set<String>> individuals, final Map<String, List<String>> parts,
			final boolean includeSubgroupMembers){
		if(!includeSubgroupMembers)
			return individuals.getOrDefault(groupId, Set.of()).size();

		final Set<String> members = new HashSet<>();
		final Set<String> visited = new HashSet<>();
		final Deque<String> pending = new ArrayDeque<>();
		pending.add(groupId);
		while(!pending.isEmpty()){
			final String id = pending.poll();
			if(!visited.add(id))
				continue;

			members.addAll(individuals.getOrDefault(id, Set.of()));
			pending.addAll(parts.getOrDefault(id, List.of()));
		}
		return members.size();
	}

	/** @return	The text of the dates of the relationship ("from ... to ..."), empty if it has none. */
	private static String datesOf(final FLEFRecord relationship){
		final String from = dateText(relationship, RelationshipReader.TAG_VALID_FROM);
		final String to = dateText(relationship, RelationshipReader.TAG_VALID_TO);
		if(StringUtils.isNotEmpty(from) && StringUtils.isNotEmpty(to))
			return from + " - " + to;
		if(StringUtils.isNotEmpty(from))
			return from + " - ";
		return (StringUtils.isNotEmpty(to)? " - " + to: StringUtils.EMPTY);
	}

	private static String dateText(final FLEFRecord relationship, final String tag){
		try{
			return StringUtils.defaultString(DateService.getDateDisplayText(FLEFRecordHelper.findChild(relationship, FLEFRecordHelper.composePath(tag, DateReader.TAG_VALUE))));
		}
		catch(final RuntimeException ignored){
			//a date that cannot be read is not shown
			return StringUtils.EMPTY;
		}
	}

	/** @return	Whether the relationship is in effect in the year: it has no date, or its dates are not all outside the year. */
	private static boolean inEffect(final FLEFRecord relationship, final long[] yearJdns){
		if(yearJdns == null)
			return true;

		final TemporalSpan span = DateNormalizer.combineBounds(
			FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM),
			FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO));
		return (span == null || GroupYearFilter.covers(span, yearJdns));
	}

}
