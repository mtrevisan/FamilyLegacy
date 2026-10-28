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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads from the model what the relationship checks need. The individuals and the groups hidden by the privacy settings are left out,
 * as they are from the views, and so are the relationships that involve them.
 */
final class RelationshipDataLoader{

	private static final String ROLE_PRINCIPAL = "principal";
	private static final String TAG_STATUS = "status";
	private static final String STATUS_ENDED = "ended";


	private RelationshipDataLoader(){}


	static RelationshipData load(final FLEFModel model){
		final Map<String, FLEFRecord> individuals = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualHandler.TYPE))
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual))
				individuals.put(individual.getId(), individual);
		final Map<String, String> groups = new LinkedHashMap<>();
		for(final FLEFRecord group : model.getRecordsByType(GroupHandler.TYPE))
			if(group.getId() != null && !PrivacyService.isHidden(model, group))
				groups.put(group.getId(), displayText(model, group));

		final List<RelationshipData.Event> events = loadEvents(model, individuals.keySet());
		final Map<String, RelationshipData.Person> persons = new LinkedHashMap<>();
		for(final FLEFRecord individual : individuals.values())
			persons.put(individual.getId(), loadPerson(individual, events));

		return new RelationshipData(persons, groups, loadRelationships(model, persons.keySet(), groups.keySet()), events);
	}


	private static List<RelationshipData.Event> loadEvents(final FLEFModel model, final Set<String> individualIds){
		final Map<String, List<RelationshipData.Participant>> participants = new LinkedHashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String personId = armValue(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(personId == null || eventId == null || !individualIds.contains(personId))
				continue;

			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			participants.computeIfAbsent(eventId, k -> new ArrayList<>())
				.add(new RelationshipData.Participant(personId, role == null || ROLE_PRINCIPAL.equals(role)));
		}

		final List<RelationshipData.Event> events = new ArrayList<>();
		for(final Map.Entry<String, List<RelationshipData.Participant>> entry : participants.entrySet()){
			final FLEFRecord event = model.getRecordById(entry.getKey());
			if(!EventHandler.isOfType(event))
				continue;

			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final FLEFRecord date = (dates.isEmpty()? null: dates.getFirst());
			events.add(new RelationshipData.Event(entry.getKey(), EventReader.extractType(event), range(date), text(date),
				entry.getValue()));
		}
		return events;
	}

	private static RelationshipData.Person loadPerson(final FLEFRecord individual, final List<RelationshipData.Event> events){
		//with several births the days they cover are used: a finding must hold for all of them
		DateRange birth = null;
		for(final RelationshipData.Event event : events)
			if(EventReader.ENUM_TYPE_BIRTH.equals(event.type()) && event.date() != null
					&& event.participants().stream().anyMatch(p -> p.principal() && p.personId().equals(individual.getId())))
				birth = (birth != null? birth.hull(event.date()): event.date());

		final String name = IndividualReader.extractPrimaryFullname(individual);
		return new RelationshipData.Person(individual.getId(), (name != null && !name.isBlank()? name: individual.getId()),
			sex(individual), birth);
	}

	private static List<RelationshipData.Relationship> loadRelationships(final FLEFModel model, final Set<String> individualIds,
			final Set<String> groupIds){
		final List<RelationshipData.Relationship> relationships = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = RelationshipReader.extractType(relationship);
			final String[] subject = participant(relationship, RelationshipReader.TAG_SUBJECT, individualIds, groupIds);
			final String[] target = participant(relationship, RelationshipReader.TAG_TARGET, individualIds, groupIds);
			if(type == null || relationship.getId() == null || subject == null || target == null)
				continue;

			final FLEFRecord from = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
			relationships.add(new RelationshipData.Relationship(relationship.getId(), subject[0], kind(subject[1]), target[0],
				kind(target[1]), type, RelationshipReader.extractRole(relationship), range(from), range(to), text(from),
				STATUS_ENDED.equals(FLEFRecordHelper.getChildValue(relationship, TAG_STATUS))));
		}
		return relationships;
	}

	/** @return the identifier and the type of the individual or group a relationship names, or {@code null} if it is neither, or hidden */
	private static String[] participant(final FLEFRecord relationship, final String tag, final Set<String> individualIds,
			final Set<String> groupIds){
		final String individual = relationship.extractReferencedId(tag, IndividualHandler.TYPE);
		if(individual != null)
			return (individualIds.contains(individual)? new String[]{individual, IndividualHandler.TYPE}: null);

		final String group = relationship.extractReferencedId(tag, GroupHandler.TYPE);
		return (group != null && groupIds.contains(group)? new String[]{group, GroupHandler.TYPE}: null);
	}

	private static RelationshipData.Kind kind(final String handlerType){
		return (GroupHandler.TYPE.equals(handlerType)? RelationshipData.Kind.GROUP: RelationshipData.Kind.INDIVIDUAL);
	}

	private static String displayText(final FLEFModel model, final FLEFRecord record){
		try{
			final RecordTypeHandler<?> handler = HandlerRegistry.getHandler(record.getTag());
			final String text = (handler != null? handler.getDisplayText(record, model): null);
			return (text != null && !text.isBlank()? text: record.getId());
		}
		catch(final RuntimeException ignored){
			return record.getId();
		}
	}

	/** @return the value of the alternative a "oneof" holds (an Xref), or {@code null} */
	private static String armValue(final FLEFRecord union){
		return (union == null || union.getChildren().isEmpty()? null: union.getChildren().getFirst().getValue());
	}

	private static SexType sex(final FLEFRecord individual){
		try{
			return IndividualReader.extractSex(individual);
		}
		catch(final IllegalArgumentException ignored){
			return SexType.UNKNOWN;
		}
	}

	private static DateRange range(final FLEFRecord dateStructure){
		if(dateStructure == null)
			return null;

		try{
			return DateRange.of(DateNormalizer.normalize(dateStructure));
		}
		catch(final RuntimeException ignored){
			return null;
		}
	}

	private static String text(final FLEFRecord dateStructure){
		return (dateStructure != null? DateReader.extractPrettyPrintDate(dateStructure): "");
	}

}
