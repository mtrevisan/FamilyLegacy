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
package io.github.mtrevisan.familylegacy.io.merger;

import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


/**
 * Lookups over the records of a model that the comparison of entities needs.
 * <p>
 * In FLEF the facts about an entity are not inside its record: births and deaths are EventRecords reached through
 * EventParticipationRecords, parents and spouses are RelationshipRecords, the members of a group are GROUP_MEMBER relationships, and
 * the hierarchy of places is made of PlaceRelationshipRecords. This index resolves those associations once.
 */
final class ModelIndex{

	static final String KIND_INDIVIDUAL = "individual";
	static final String KIND_GROUP = "group";
	static final String KIND_EVENT = "event";
	static final String KIND_EVENT_PARTICIPATION = "event_participation";
	static final String KIND_RELATIONSHIP = "relationship";
	static final String KIND_PLACE = "place";
	static final String KIND_PLACE_RELATIONSHIP = "place_relationship";
	static final String KIND_INDIVIDUAL_ATTRIBUTE = "individual_attribute";
	static final String KIND_GROUP_ATTRIBUTE = "group_attribute";
	static final String KIND_SOURCE = "source";

	static final String TAG_TYPE = "type";
	static final String TAG_DATE = "date";
	static final String TAG_PLACE = "place";
	static final String TAG_ROLE = "role";
	private static final String TAG_PARTICIPANT = "participant";
	private static final String TAG_EVENT = "event";
	private static final String TAG_SUBJECT = "subject";
	private static final String TAG_TARGET = "target";
	private static final String TAG_NAME = "name";
	private static final String TAG_SOURCE = "source";
	private static final String TAG_LOCATOR = "locator";
	private static final String TAG_RESOLVES = "resolves";
	private static final String TAG_PROOF_STATUS = "proof_status";
	private static final String TAG_AUDIT = "audit";
	private static final String TAG_CREATION = "creation";
	private static final String TAG_UPDATE = "update";
	static final String PROOF_STATUS_DISPROVEN = "disproven";
	private static final String TAG_VALUE = "value";

	static final String ROLE_PRINCIPAL = "principal";
	private static final String PART_OF_SUFFIX = "_part_of";


	private final Map<String, FLEFRecord> byId = new HashMap<>();
	private final Map<String, List<FLEFRecord>> participationsByParticipant = new HashMap<>();
	private final Map<String, List<FLEFRecord>> participationsByEvent = new HashMap<>();
	private final Map<String, Set<String>> parentsOf = new HashMap<>();
	private final Map<String, Set<String>> childrenOf = new HashMap<>();
	private final Map<String, List<FLEFRecord>> attributesOf = new HashMap<>();
	private final Map<String, Set<String>> spousesOf = new HashMap<>();
	private final Map<String, Set<String>> membersOf = new HashMap<>();
	private final Map<String, Set<String>> placeParentsOf = new HashMap<>();
	private final Map<String, Set<String>> placeChildrenOf = new HashMap<>();
	private final Map<String, Set<String>> placeSuccessionsOf = new HashMap<>();
	private final Map<String, Set<String>> usagesOf = new HashMap<>();
	private final Map<String, Set<String>> locatedUsagesOf = new HashMap<>();
	private final Set<String> disprovenIdentities = new HashSet<>();
	private final Map<String, Integer> referenceCount = new HashMap<>();


	ModelIndex(final Collection<FLEFRecord> records, final ReferenceRewriter references){
		final List<FLEFRecord> hypotheses = new ArrayList<>();
		final List<FLEFRecord> conclusions = new ArrayList<>();
		for(final FLEFRecord record : records){
			byId.put(record.getId(), record);

			switch(record.getTag()){
				case KIND_EVENT_PARTICIPATION -> indexParticipation(record);
				case KIND_RELATIONSHIP -> indexRelationship(record);
				case KIND_PLACE_RELATIONSHIP -> indexPlaceRelationship(record);
				case ProtocolRules.KIND_IDENTITY_HYPOTHESIS -> hypotheses.add(record);
				case ProtocolRules.KIND_CONCLUSION -> conclusions.add(record);
				case KIND_INDIVIDUAL_ATTRIBUTE -> {
					final String individual = value(record, KIND_INDIVIDUAL);
					if(individual != null)
						attributesOf.computeIfAbsent(individual, k -> new ArrayList<>())
							.add(record);
				}
				case null, default -> {}
			}
		}

		indexDisprovenIdentities(hypotheses, conclusions);

		// the participations are all indexed: the subjects of every assertion can be told
		references.forEachReference(records, (owner, parent, node, path, targetKind) -> {
			referenceCount.merge(node.getValue(), 1, Integer::sum);

			final boolean place = (KIND_PLACE.equals(targetKind) && !KIND_PLACE_RELATIONSHIP.equals(owner.getTag()));
			if(place || KIND_SOURCE.equals(targetKind))
				indexUsage(owner, parent, node.getValue(), targetKind);
		});
	}

	/**
	 * The pairs of records whose identity hypothesis is disproven by its most recent conclusion (by audit): the researcher judged
	 * them different entities, and they are never proposed again. A later conclusion that is not disproven reopens the pair.
	 */
	private void indexDisprovenIdentities(final List<FLEFRecord> hypotheses, final List<FLEFRecord> conclusions){
		if(hypotheses.isEmpty() || conclusions.isEmpty())
			return;

		// hypothesis id to {date, proof status} of its most recent conclusion
		final Map<String, String[]> latest = new HashMap<>();
		for(final FLEFRecord conclusion : conclusions){
			final String status = value(conclusion, TAG_PROOF_STATUS);
			final String date = lastAuditDate(conclusion);
			for(final FLEFRecord target : FieldMerger.children(conclusion, TAG_RESOLVES)){
				if(!ProtocolRules.KIND_IDENTITY_HYPOTHESIS.equals(armTag(target)) || armValue(target) == null)
					continue;

				final String[] previous = latest.get(armValue(target));
				if(previous == null || date.compareTo(previous[0]) >= 0)
					latest.put(armValue(target), new String[]{date, status});
			}
		}

		for(final FLEFRecord hypothesis : hypotheses){
			final String[] conclusion = latest.get(hypothesis.getId());
			final String pair = ProtocolRules.unorderedPair(ProtocolRules.identities(hypothesis));
			if(conclusion != null && PROOF_STATUS_DISPROVEN.equals(conclusion[1]) && pair != null)
				disprovenIdentities.add(pair);
		}
	}

	/** The date of the last change of a record: its last update, or its creation. */
	private static String lastAuditDate(final FLEFRecord record){
		String last = StringUtils.EMPTY;
		for(final FLEFRecord audit : FieldMerger.children(record, TAG_AUDIT)){
			for(final FLEFRecord creation : FieldMerger.children(audit, TAG_CREATION))
				last = max(last, value(creation, TAG_DATE));
			for(final FLEFRecord update : FieldMerger.children(audit, TAG_UPDATE))
				last = max(last, value(update, TAG_DATE));
		}
		return last;
	}

	private static String max(final String a, final String b){
		return (b != null && b.compareTo(a) > 0? b: a);
	}

	/**
	 * Whether the researcher concluded that two records are not the same entity.
	 */
	boolean isIdentityDisproven(final String idA, final String idB){
		return disprovenIdentities.contains(ProtocolRules.unorderedPair(List.of(idA, idB)));
	}

	/**
	 * Records that a place or a source is used by an assertion, as {@code subjects|kind:type}, and, for a source cited with a
	 * locator, as {@code subjects|kind:type|locator}.
	 */
	private void indexUsage(final FLEFRecord owner, final FLEFRecord parent, final String usedId, final String usedKind){
		final String subjects = subjectsOf(owner);
		if(subjects == null)
			return;

		final String type = value(owner, TAG_TYPE);
		final String usage = subjects + "|" + owner.getTag() + (type != null? ":" + type: StringUtils.EMPTY);
		usagesOf.computeIfAbsent(usedId, k -> new LinkedHashSet<>())
			.add(usage);
		if(KIND_SOURCE.equals(usedKind)){
			final String locator = TextSimilarity.normalize(value(parent, TAG_LOCATOR));
			if(!locator.isEmpty())
				locatedUsagesOf.computeIfAbsent(usedId, k -> new LinkedHashSet<>())
					.add(usage + "|" + locator);
		}
	}

	/**
	 * The entities an assertion is about, sorted and joined: the principals of an event, the participant of a participation, the
	 * owner of an attribute, the two ends of a relationship, the record itself otherwise.
	 */
	private String subjectsOf(final FLEFRecord owner){
		final Set<String> subjects = new TreeSet<>();
		switch(owner.getTag()){
			case KIND_EVENT -> {
				final Set<String> all = new TreeSet<>();
				for(final FLEFRecord participation : participationsOfEvent(owner.getId())){
					final String participant = armValue(FieldMerger.first(participation, TAG_PARTICIPANT));
					if(participant == null)
						continue;

					all.add(participant);
					final String role = value(participation, TAG_ROLE);
					if(role == null || ROLE_PRINCIPAL.equals(role))
						subjects.add(participant);
				}
				if(subjects.isEmpty())
					subjects.addAll(all);
			}
			case KIND_EVENT_PARTICIPATION -> addIfPresent(subjects, armValue(FieldMerger.first(owner, TAG_PARTICIPANT)));
			case KIND_INDIVIDUAL_ATTRIBUTE -> addIfPresent(subjects, value(owner, KIND_INDIVIDUAL));
			case KIND_GROUP_ATTRIBUTE -> addIfPresent(subjects, value(owner, KIND_GROUP));
			case KIND_RELATIONSHIP -> {
				final String subject = armValue(FieldMerger.first(owner, TAG_SUBJECT));
				final String target = armValue(FieldMerger.first(owner, TAG_TARGET));
				// the direction matters: the child of a parent is not the parent of a child
				return (subject != null && target != null? subject + ">" + target: null);
			}
			default -> subjects.add(owner.getId());
		}
		return (subjects.isEmpty()? null: String.join(",", subjects));
	}

	private static void addIfPresent(final Set<String> set, final String value){
		if(value != null)
			set.add(value);
	}

	private void indexParticipation(final FLEFRecord participation){
		final String participant = armValue(FieldMerger.first(participation, TAG_PARTICIPANT));
		final String event = value(participation, TAG_EVENT);
		if(participant != null)
			participationsByParticipant.computeIfAbsent(participant, k -> new ArrayList<>())
				.add(participation);
		if(event != null)
			participationsByEvent.computeIfAbsent(event, k -> new ArrayList<>())
				.add(participation);
	}

	private void indexRelationship(final FLEFRecord relationship){
		final String type = value(relationship, TAG_TYPE);
		final String subject = armValue(FieldMerger.first(relationship, TAG_SUBJECT));
		final String target = armValue(FieldMerger.first(relationship, TAG_TARGET));
		if(type == null || subject == null || target == null)
			return;

		if(RelationshipReader.CHILD_TYPES.contains(type)){
			// the subject is the child of the target
			parentsOf.computeIfAbsent(subject, k -> new LinkedHashSet<>())
				.add(target);
			childrenOf.computeIfAbsent(target, k -> new LinkedHashSet<>())
				.add(subject);
		}
		else if(RelationshipReader.PARTNER_TYPES.contains(type)){
			spousesOf.computeIfAbsent(subject, k -> new LinkedHashSet<>())
				.add(target);
			spousesOf.computeIfAbsent(target, k -> new LinkedHashSet<>())
				.add(subject);
		}
		else if(RelationshipReader.isTypeGroupMember(type))
			membersOf.computeIfAbsent(target, k -> new LinkedHashSet<>())
				.add(subject);
	}

	private void indexPlaceRelationship(final FLEFRecord relationship){
		final String type = value(relationship, TAG_TYPE);
		final String subject = armValue(FieldMerger.first(relationship, TAG_SUBJECT));
		final String target = armValue(FieldMerger.first(relationship, TAG_TARGET));
		if(type == null || subject == null || target == null)
			return;

		if(type.endsWith(PART_OF_SUFFIX)){
			placeParentsOf.computeIfAbsent(subject, k -> new LinkedHashSet<>())
				.add(target);
			placeChildrenOf.computeIfAbsent(target, k -> new LinkedHashSet<>())
				.add(subject);
		}
		else{
			// succession, merger, division: the researcher keeps these places apart on purpose
			placeSuccessionsOf.computeIfAbsent(subject, k -> new LinkedHashSet<>())
				.add(target);
			placeSuccessionsOf.computeIfAbsent(target, k -> new LinkedHashSet<>())
				.add(subject);
		}
	}


	FLEFRecord record(final String id){
		return byId.get(id);
	}

	/**
	 * Events of the given types whose principal is the given entity; a participation without role counts as principal.
	 */
	List<FLEFRecord> principalEvents(final String participantId, final Set<String> eventTypes){
		final List<FLEFRecord> events = new ArrayList<>(0);
		for(final FLEFRecord participation : participationsByParticipant.getOrDefault(participantId, List.of())){
			final String role = value(participation, TAG_ROLE);
			if(role != null && !ROLE_PRINCIPAL.equals(role))
				continue;

			final FLEFRecord event = byId.get(value(participation, TAG_EVENT));
			if(event != null && value(event, TAG_TYPE) != null && eventTypes.contains(value(event, TAG_TYPE)) && !events.contains(event))
				events.add(event);
		}
		return events;
	}

	List<TemporalSpan> principalEventSpans(final String participantId, final Set<String> eventTypes){
		final List<TemporalSpan> spans = new ArrayList<>(0);
		for(final FLEFRecord event : principalEvents(participantId, eventTypes)){
			final FLEFRecord date = FieldMerger.first(event, TAG_DATE);
			final TemporalSpan span = span(date);
			if(span != null)
				spans.add(span);
		}
		return spans;
	}

	/**
	 * Ids of the places where the events of the given types of an entity took place.
	 */
	Set<String> principalEventPlaces(final String participantId, final Set<String> eventTypes){
		final Set<String> places = new LinkedHashSet<>(0);
		for(final FLEFRecord event : principalEvents(participantId, eventTypes)){
			final FLEFRecord placeCitation = FieldMerger.first(event, TAG_PLACE);
			final String place = (placeCitation != null? value(placeCitation, TAG_PLACE): null);
			if(place != null)
				places.add(place);
		}
		return places;
	}

	List<FLEFRecord> participationsOfEvent(final String eventId){
		return participationsByEvent.getOrDefault(eventId, List.of());
	}

	Set<String> parentsOf(final String individualId){
		return parentsOf.getOrDefault(individualId, Set.of());
	}

	Set<String> childrenOf(final String individualId){
		return childrenOf.getOrDefault(individualId, Set.of());
	}

	/**
	 * The citations ({@code source id|locator}) of the assertions about an individual as their subject: the record and its names,
	 * its attributes, and the events where it is principal with their participations. Relationships and the events where it has
	 * another role are left out: a baptism entry is also cited for the parents and the godparents.
	 */
	Set<String> subjectCitations(final String individualId){
		final Set<String> citations = new LinkedHashSet<>();
		final FLEFRecord individual = byId.get(individualId);
		if(individual != null){
			addCitations(individual, citations);
			for(final FLEFRecord name : FieldMerger.children(individual, TAG_NAME))
				addCitations(name, citations);
		}
		for(final FLEFRecord attribute : attributesOf.getOrDefault(individualId, List.of()))
			addCitations(attribute, citations);
		for(final FLEFRecord participation : participationsByParticipant.getOrDefault(individualId, List.of())){
			final String role = value(participation, TAG_ROLE);
			if(role != null && !ROLE_PRINCIPAL.equals(role))
				continue;

			addCitations(participation, citations);
			final FLEFRecord event = byId.get(value(participation, TAG_EVENT));
			if(event != null)
				addCitations(event, citations);
		}
		return citations;
	}

	private static void addCitations(final FLEFRecord owner, final Set<String> citations){
		for(final FLEFRecord citation : FieldMerger.children(owner, TAG_SOURCE)){
			final String source = value(citation, TAG_SOURCE);
			if(source != null)
				citations.add(source + "|" + TextSimilarity.normalize(value(citation, TAG_LOCATOR)));
		}
	}

	/**
	 * The attributes of an individual, as {@code type|value}.
	 */
	Set<String> attributeKeys(final String individualId){
		final Set<String> keys = new LinkedHashSet<>();
		for(final FLEFRecord attribute : attributesOf.getOrDefault(individualId, List.of())){
			final String type = value(attribute, TAG_TYPE);
			final String attributeValue = TextSimilarity.normalize(value(attribute, TAG_VALUE));
			final FLEFRecord place = FieldMerger.first(attribute, TAG_PLACE);
			final String placeId = (place != null? value(place, TAG_PLACE): null);
			if(type != null && (!attributeValue.isEmpty() || placeId != null))
				keys.add(type + "|" + attributeValue + "|" + (placeId != null? placeId: StringUtils.EMPTY));
		}
		return keys;
	}

	/**
	 * The events where an individual is principal, other than the given types, as {@code type|first day}.
	 */
	Set<String> otherPrincipalEventKeys(final String individualId, final Set<String> excludedTypes){
		final Set<String> keys = new LinkedHashSet<>();
		for(final FLEFRecord participation : participationsByParticipant.getOrDefault(individualId, List.of())){
			final String role = value(participation, TAG_ROLE);
			if(role != null && !ROLE_PRINCIPAL.equals(role))
				continue;

			final FLEFRecord event = byId.get(value(participation, TAG_EVENT));
			final String type = (event != null? value(event, TAG_TYPE): null);
			if(type == null || excludedTypes.contains(type))
				continue;

			final FLEFRecord date = FieldMerger.first(event, TAG_DATE);
			final TemporalSpan span = span(date);
			if(span != null && span.minJdn() != Long.MIN_VALUE)
				keys.add(type + "|" + span.minJdn());
		}
		return keys;
	}

	Set<String> spousesOf(final String individualId){
		return spousesOf.getOrDefault(individualId, Set.of());
	}

	Set<String> membersOf(final String groupId){
		return membersOf.getOrDefault(groupId, Set.of());
	}

	Set<String> placeParentsOf(final String placeId){
		return placeParentsOf.getOrDefault(placeId, Set.of());
	}

	/**
	 * Every place a place is part of, at any level (its parish, its municipality, its province, …).
	 */
	Set<String> placeAncestorsOf(final String placeId){
		final Set<String> ancestors = new LinkedHashSet<>();
		final Deque<String> pending = new ArrayDeque<>(placeParentsOf(placeId));
		while(!pending.isEmpty()){
			final String ancestor = pending.poll();
			// cycles should not exist, but must not loop
			if(!ancestor.equals(placeId) && ancestors.add(ancestor))
				pending.addAll(placeParentsOf(ancestor));
		}
		return ancestors;
	}

	Set<String> placeChildrenOf(final String placeId){
		return placeChildrenOf.getOrDefault(placeId, Set.of());
	}

	/** Places linked to a place by succession, merger, division, or transfer of territory. */
	Set<String> placeSuccessionsOf(final String placeId){
		return placeSuccessionsOf.getOrDefault(placeId, Set.of());
	}

	/**
	 * How a place or a source is used, as {@code subjects|kind:type} (e.g. {@code I12|event:birth}).
	 */
	Set<String> usagesOf(final String id){
		return usagesOf.getOrDefault(id, Set.of());
	}

	/**
	 * How a source is cited with a locator, as {@code subjects|kind:type|locator}.
	 */
	Set<String> locatedUsagesOf(final String sourceId){
		return locatedUsagesOf.getOrDefault(sourceId, Set.of());
	}

	/**
	 * How many references, from any record, point to the given record.
	 */
	int referenceCount(final String id){
		return referenceCount.getOrDefault(id, 0);
	}


	/**
	 * The span of a {@code DateStructure}, or {@code null} when it is missing or invalid (e.g. a range that ends before it starts):
	 * an invalid date is no evidence.
	 */
	static TemporalSpan span(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return null;

		try{
			return DateNormalizer.normalize(dateStructure);
		}
		catch(final IllegalArgumentException ignored){
			return null;
		}
	}

	/**
	 * The value of the only arm of a union (e.g. {@code participant.individual I1} gives {@code I1}).
	 */
	static String armValue(final FLEFRecord union){
		if(union == null || union.getChildren().isEmpty())
			return null;
		return union.getChildren().getFirst().getValue();
	}

	static String armTag(final FLEFRecord union){
		if(union == null || union.getChildren().isEmpty())
			return null;
		return union.getChildren().getFirst().getTag();
	}

	static String value(final FLEFRecord parent, final String tag){
		final FLEFRecord child = FieldMerger.first(parent, tag);
		return (child != null? child.getValue(): null);
	}

}
