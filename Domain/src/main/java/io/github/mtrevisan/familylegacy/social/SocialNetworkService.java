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
package io.github.mtrevisan.familylegacy.social;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.RecordDisplay;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityRef;
import io.github.mtrevisan.familylegacy.temporal.TemporalEntityType;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


/**
 * Builds a {@link SocialGraph} from a FLEF model, starting from a focus
 * entity and expanding outward up to a configured maximum degree.
 * <p>
 * <b>Edge sources.</b> The service considers three sources of social
 * relationships:
 * <ul>
 *   <li>{@code RelationshipRecord} instances with type {@code associate},
 *       {@code group_member} or {@code part_of};</li>
 *   <li>{@code EventParticipationRecord} instances whose role is a
 *       relational role (witness, officiant, executor, landlord, tenant,
 *       grantor, grantee, judge, accused, power_of_attorney, informant).
 *       Two participants of the same event are connected to each other,
 *       and the role carried by the edge is the role of the other
 *       participant as seen from the current node.</li>
 * </ul>
 * Purely parental and spousal types ({@code biological_child},
 * {@code civil_spouse}, …) are intentionally excluded: they are already
 * covered by the biological tree and the Ego Network views, and including
 * them here would duplicate information and overwhelm the social layout.
 * <p>
 * <b>Indices.</b> To avoid repeated full scans of the model, the service
 * builds three reverse indices once and caches them until
 * {@link #invalidate()} is called:
 * <ul>
 *   <li>{@code socialRelationshipsByEntityId} — for each entity, the
 *       {@code RelationshipRecord} instances that reference it as subject
 *       or target;</li>
 *   <li>{@code eventParticipationsByEntityId} — for each entity, the
 *       {@code EventParticipationRecord} instances that reference it as
 *       participant;</li>
 *   <li>{@code relationalParticipationsByEventId} — for each event, the
 *       subset of participations whose role is relational.</li>
 * </ul>
 * The service is not thread‑safe; it is meant to be used from the Swing
 * Event Dispatch Thread like the rest of the projection layer.
 */
public final class SocialNetworkService{

	private final FLEFModel model;

	private Map<String, List<FLEFRecord>> socialRelationshipsByEntityId;
	private Map<String, List<FLEFRecord>> eventParticipationsByEntityId;
	private Map<String, List<FLEFRecord>> relationalParticipationsByEventId;
	private boolean indicesBuilt;


	public SocialNetworkService(final FLEFModel model){
		this.model = model;
	}


	/**
	 * Builds a social graph centred on the given entity, expanding up to
	 * {@code filters.maxDegree()} edges.
	 *
	 * @param centerId the id of the focus entity; may be {@code null}
	 * @param filters  the filter configuration (must not be {@code null})
	 * @return the graph, or {@link SocialGraph#empty(SocialFilters)} when
	 * the center does not exist
	 */
	public SocialGraph build(final String centerId, final SocialFilters filters){
		if(filters == null)
			throw new IllegalArgumentException("Filters must not be null");
		if(StringUtils.isEmpty(centerId) || !model.hasRecord(centerId))
			return SocialGraph.empty(filters);

		ensureIndices();

		final FLEFRecord centerRecord = model.getRecordById(centerId);
		final TemporalEntityRef centerEntity = toEntityRef(centerRecord);
		if(centerEntity == null)
			return SocialGraph.empty(filters);

		final int maxDegree = filters.maxDegree();

		// BFS over the social graph.
		final Map<String, Integer> degreeById = new HashMap<>();
		final Map<String, SocialNodeRef> nodeById = new HashMap<>();

		final SocialNodeRef centerNode = new SocialNodeRef(centerEntity, 0,
			SocialRelationCategory.OTHER);
		degreeById.put(centerId, 0);
		nodeById.put(centerId, centerNode);

		final Deque<String> queue = new ArrayDeque<>();
		queue.add(centerId);

		while(!queue.isEmpty()){
			final String currentId = queue.poll();
			final int currentDegree = degreeById.get(currentId);
			if(currentDegree >= maxDegree)
				continue;

			final FLEFRecord currentRecord = model.getRecordById(currentId);
			if(currentRecord == null)
				continue;
			final TemporalEntityRef currentEntity = nodeById.get(currentId)
				.entity();

			for(final SocialEdgeRef edge : edgesIncidentTo(currentEntity, filters)){
				final TemporalEntityRef otherEntity = edge.other(currentEntity);
				if(otherEntity == null)
					continue;
				final String otherId = otherEntity.id();

				// Register the neighbour.
				final int nextDegree = currentDegree + 1;
				final Integer existingDegree = degreeById.get(otherId);
				if(existingDegree == null){
					degreeById.put(otherId, nextDegree);
					nodeById.put(otherId, new SocialNodeRef(otherEntity, nextDegree,
						SocialRelationCategory.OTHER));
					queue.add(otherId);
				}
			}
		}

		// Recompute the primary category of each node from its incident edges.
		final List<SocialNodeRef> nodes = new ArrayList<>(nodeById.size());
		for(final SocialNodeRef node : nodeById.values())
			nodes.add(node.withPrimaryCategory(computePrimaryCategory(node, filters)));

		// Collect all edges that connect two nodes present in the graph.
		final Set<String> presentIds = new HashSet<>(nodeById.keySet());
		final List<SocialEdgeRef> edges = new ArrayList<>();
		final Set<String> seenEdges = new HashSet<>();
		//sorted by id, so that which of the two views of a pair of participants is kept does not depend on the hash order
		for(final SocialNodeRef node : new TreeMap<>(nodeById).values())
			for(final SocialEdgeRef edge : edgesIncidentTo(node.entity(), filters)){
				//the edges of the participations of an event share the participation record: the pair tells them apart
				final String edgeKey = edge.sourceRecord()
					.getId() + "\u0000" + edge.source()
					.id() + "\u0000" + edge.target()
					.id();
				if(!seenEdges.add(edgeKey))
					continue;
				//the edges of the participations are undirected: A-B seen from A and B-A seen from B are the same connection, and
				//drawing and counting it twice would inflate edgeCount() and edgesBetween() (the role is that of the node it was seen from)
				if(!edge.isDirected() && EventParticipationReader.isOfType(edge.sourceRecord())){
					final String eventId = FLEFRecordHelper.getChildValue(edge.sourceRecord(), EventParticipationReader.TAG_EVENT);
					final String sourceId = edge.source().id();
					final String targetId = edge.target().id();
					final String pairKey = "pair\u0000" + eventId + "\u0000" + (sourceId.compareTo(targetId) <= 0?
						sourceId + "\u0000" + targetId: targetId + "\u0000" + sourceId);
					if(!seenEdges.add(pairKey))
						continue;
				}
				if(!presentIds.contains(edge.source()
					.id()) || !presentIds.contains(edge.target()
					.id()))
					continue;
				edges.add(edge);
			}

		return SocialGraph.of(centerNode, nodes, edges, maxDegree, filters);
	}

	/**
	 * Invalidates the cached indices. Must be called whenever the
	 * underlying FLEF model changes.
	 */
	public void invalidate(){
		socialRelationshipsByEntityId = null;
		eventParticipationsByEntityId = null;
		relationalParticipationsByEventId = null;
		indicesBuilt = false;
	}


	/* ======================================================================
	 *                          Edge extraction
	 * ====================================================================== */

	/**
	 * Returns all social edges incident to the given entity, already
	 * filtered by the given filters.
	 */
	private List<SocialEdgeRef> edgesIncidentTo(final TemporalEntityRef entity, final SocialFilters filters){
		final List<SocialEdgeRef> result = new ArrayList<>();

		// 1. Relationship-based edges.
		for(final FLEFRecord relationship : socialRelationshipsByEntityId.getOrDefault(entity.id(), List.of())){
			final SocialEdgeRef edge = buildRelationshipEdge(relationship);
			if(filters.accepts(edge))
				result.add(edge);
		}

		// 2. Event-participation-based edges.
		for(final FLEFRecord participation : eventParticipationsByEntityId.getOrDefault(entity.id(), List.of())){
			collectParticipationEdges(participation, entity, filters, result);
		}

		return result;
	}

	private SocialEdgeRef buildRelationshipEdge(final FLEFRecord relationship){
		final String type = RelationshipReader.extractType(relationship);
		if(type == null || !RelationshipReader.SOCIAL_RELATIONSHIP_TYPES.contains(type.toLowerCase(Locale.ROOT)))
			return null;

		final TemporalEntityRef subject = resolveParticipant(relationship, RelationshipReader.TAG_SUBJECT);
		final TemporalEntityRef target = resolveParticipant(relationship, RelationshipReader.TAG_TARGET);
		if(subject == null || target == null || subject.equals(target))
			return null;

		final String role = FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_ROLE);
		final SocialRelationCategory category = classifyRelationship(type, role);
		final SocialEdgeDirection direction = directionOf(type);
		final TemporalSpan span = buildSpan(relationship);
		return new SocialEdgeRef(subject, target, type.toLowerCase(Locale.ROOT), role, category, direction, span, relationship);
	}

	private void collectParticipationEdges(final FLEFRecord participation, final TemporalEntityRef perspective,
		final SocialFilters filters, final List<SocialEdgeRef> output){
		final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
		if(eventId == null)
			return;

		final List<FLEFRecord> participants = relationalParticipationsByEventId.getOrDefault(eventId, List.of());
		if(participants.size() < 2)
			return;

		final String currentRole = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
		if(!isRelationalRole(currentRole))
			return;

		for(final FLEFRecord otherParticipation : participants){
			if(otherParticipation == participation)
				continue;
			final String otherId = extractParticipantId(otherParticipation);
			if(otherId == null || otherId.equals(perspective.id()))
				continue;

			final FLEFRecord otherRecord = model.getRecordById(otherId);
			if(otherRecord == null)
				continue;
			final TemporalEntityRef otherEntity = toEntityRef(otherRecord);
			if(otherEntity == null)
				continue;

			final String otherRole = FLEFRecordHelper.getChildValue(otherParticipation, EventParticipationReader.TAG_ROLE);
			final SocialRelationCategory category = SocialRoleClassifier.classify(otherRole,
				SocialRelationCategory.RELIGIOUS);
			final TemporalSpan span = buildSpan(participation);

			final SocialEdgeRef edge = new SocialEdgeRef(
				perspective, otherEntity,
				"event_" + safeLower(eventTagOf(otherParticipation)),
				(otherRole != null? otherRole: StringUtils.EMPTY),
				category, SocialEdgeDirection.UNDIRECTED, span, participation
			);
			if(filters.accepts(edge))
				output.add(edge);
		}
	}


	/* ======================================================================
	 *                          Classification helpers
	 * ====================================================================== */

	private static SocialRelationCategory classifyRelationship(final String type, final String role){
		final String normalizedType = type.toLowerCase(Locale.ROOT);
		final SocialRelationCategory emptyRoleFallback = switch(normalizedType){
			case RelationshipReader.ENUM_TYPE_GROUP_MEMBER, RelationshipReader.ENUM_TYPE_PART_OF -> SocialRelationCategory.COMMUNITY;
			default -> SocialRelationCategory.OTHER;
		};
		return SocialRoleClassifier.classify(role, emptyRoleFallback);
	}

	private static SocialEdgeDirection directionOf(final String type){
		return switch(type.toLowerCase(Locale.ROOT)){
			case RelationshipReader.ENUM_TYPE_GROUP_MEMBER, RelationshipReader.ENUM_TYPE_PART_OF -> SocialEdgeDirection.DIRECTED;
			default -> SocialEdgeDirection.UNDIRECTED;
		};
	}

	private static boolean isRelationalRole(final String role){
		if(StringUtils.isEmpty(role))
			return false;

		return EventParticipationReader.RELATIONAL_EVENT_ROLES.contains(role.trim()
			.toLowerCase(Locale.ROOT));
	}


	/* ======================================================================
	 *                          Span building
	 * ====================================================================== */

	private TemporalSpan buildSpan(final FLEFRecord relationship){
		final FLEFRecord from = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
		final FLEFRecord to = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
		final TemporalSpan span = DateNormalizer.combineBounds(from, to);
		if(span != null)
			return span;

		// Fallback: relationships with no date at all are represented as an
		// unbounded span, so they remain visible in the network regardless of
		// the temporal filter.
		return TemporalSpan.unbounded(TemporalSpan.STATUS_UNKNOWN);
	}


	/* ======================================================================
	 *                          Primary category computation
	 * ====================================================================== */

	private SocialRelationCategory computePrimaryCategory(final SocialNodeRef node, final SocialFilters filters){
		final Map<SocialRelationCategory, Integer> counts = new HashMap<>();
		for(final SocialEdgeRef edge : edgesIncidentTo(node.entity(), filters))
			counts.merge(edge.category(), 1, Integer::sum);
		if(counts.isEmpty())
			return SocialRelationCategory.OTHER;

		SocialRelationCategory best = SocialRelationCategory.OTHER;
		int bestCount = -1;
		for(final Map.Entry<SocialRelationCategory, Integer> entry : counts.entrySet()){
			if(entry.getValue() > bestCount
				|| (entry.getValue() == bestCount && entry.getKey().ordinal() < best.ordinal())){
				best = entry.getKey();
				bestCount = entry.getValue();
			}
		}
		return best;
	}


	/* ======================================================================
	 *                          Index building
	 * ====================================================================== */

	private void ensureIndices(){
		if(indicesBuilt)
			return;

		final Map<String, List<FLEFRecord>> filteredRelationships = new HashMap<>();
		final List<FLEFRecord> relationships = PrivacyService.visible(model, model.getRecordsByType(RelationshipReader.TYPE));
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !RelationshipReader.SOCIAL_RELATIONSHIP_TYPES.contains(type.toLowerCase(Locale.ROOT)))
				continue;

			final String subjectId = extractParticipantId(relationship, RelationshipReader.TAG_SUBJECT);
			final String objectId = extractParticipantId(relationship, RelationshipReader.TAG_TARGET);
			if(subjectId != null)
				filteredRelationships.computeIfAbsent(subjectId, k -> new ArrayList<>())
					.add(relationship);
			if(objectId != null)
				filteredRelationships.computeIfAbsent(objectId, k -> new ArrayList<>())
					.add(relationship);
		}

		final Map<String, List<FLEFRecord>> participationsByEntity = new HashMap<>();
		final Map<String, List<FLEFRecord>> participationsByEvent = new HashMap<>();
		final List<FLEFRecord> participations = PrivacyService.visible(model, model.getRecordsByType(EventParticipationReader.TYPE));
		for(final FLEFRecord participation : participations){
			final String participantId = extractParticipantId(participation);
			if(participantId != null)
				participationsByEntity.computeIfAbsent(participantId, k -> new ArrayList<>())
					.add(participation);

			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			if(!isRelationalRole(role))
				continue;
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(eventId != null)
				participationsByEvent.computeIfAbsent(eventId, k -> new ArrayList<>())
					.add(participation);
		}

		socialRelationshipsByEntityId = filteredRelationships;
		eventParticipationsByEntityId = participationsByEntity;
		relationalParticipationsByEventId = participationsByEvent;
		indicesBuilt = true;
	}


	/* ======================================================================
	 *                          Reference resolution
	 * ====================================================================== */

	private TemporalEntityRef toEntityRef(final FLEFRecord record){
		if(record == null)
			return null;
		final String tag = record.getTag();
		if(tag == null)
			return null;
		final TemporalEntityType type = switch(record.getTag().toUpperCase(Locale.ROOT)){
			case IndividualReader.TYPE -> TemporalEntityType.INDIVIDUAL;
			case GroupReader.TYPE -> TemporalEntityType.GROUP;
			default -> null;
		};
		if(type == null)
			return null;
		return new TemporalEntityRef(type, record.getId(), record, RecordDisplay.describe(record, model));
	}

	private TemporalEntityRef resolveParticipant(final FLEFRecord record, final String fieldTag){
		final String id = extractParticipantId(record, fieldTag);
		if(id == null)
			return null;
		final FLEFRecord participantRecord = model.getRecordById(id);
		if(participantRecord == null)
			return null;
		return toEntityRef(participantRecord);
	}

	private static String extractParticipantId(final FLEFRecord record, final String fieldTag){
		final FLEFRecord field = FLEFRecordHelper.findChild(record, fieldTag);
		if(field == null)
			return null;
		final FLEFRecord ref = field.getTheOnlyChild();
		return (ref != null? ref.getValue(): null);
	}

	private static String extractParticipantId(final FLEFRecord participation){
		return EventParticipationReader.extractParticipantRef(participation);
	}

	private static String eventTagOf(final FLEFRecord participation){
		final FLEFRecord eventRef = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_EVENT);
		if(eventRef == null)
			return StringUtils.EMPTY;

		final FLEFRecord ref = eventRef.getTheOnlyChild();
		return (ref != null && ref.getTag() != null? ref.getTag(): StringUtils.EMPTY);
	}

	private static String safeLower(final String s){
		return (s != null? s.toLowerCase(Locale.ROOT): StringUtils.EMPTY);
	}

}
