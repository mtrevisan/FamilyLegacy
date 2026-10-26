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

import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


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
	private final Map<String, Integer> referenceCount = new HashMap<>();


	ModelIndex(final Collection<FLEFRecord> records, final ReferenceRewriter references){
		for(final FLEFRecord record : records){
			byId.put(record.getId(), record);

			switch(record.getTag()){
				case KIND_EVENT_PARTICIPATION -> indexParticipation(record);
				case KIND_RELATIONSHIP -> indexRelationship(record);
				case KIND_PLACE_RELATIONSHIP -> indexPlaceRelationship(record);
				case KIND_INDIVIDUAL_ATTRIBUTE -> {
					final String individual = value(record, KIND_INDIVIDUAL);
					if(individual != null)
						attributesOf.computeIfAbsent(individual, k -> new ArrayList<>())
							.add(record);
				}
				case null, default -> {}
			}
		}

		references.forEachReference(records, (owner, node, path, targetKind)
			-> referenceCount.merge(node.getValue(), 1, Integer::sum));
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
		if(type != null && type.endsWith(PART_OF_SUFFIX) && subject != null && target != null)
			placeParentsOf.computeIfAbsent(subject, k -> new LinkedHashSet<>())
				.add(target);
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
				keys.add(type + "|" + attributeValue + "|" + (placeId != null? placeId: ""));
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
