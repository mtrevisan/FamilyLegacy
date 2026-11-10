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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SourceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HandlerRegistry;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads from the model what the relationship checks need. The individuals and the groups hidden by the privacy settings are left out,
 * as they are from the views, and so are the relationships that involve them.
 */
final class RelationshipDataLoader{



	private static final String PLACE_OF = FLEFRecordHelper.composePath(PlaceHandler.TYPE, PlaceHandler.TYPE);
	private static final Set<String> ECCLESIASTICAL_TYPES = Set.of(PlaceReader.ENUM_TYPE_PARISH, PlaceReader.ENUM_TYPE_DIOCESE,
		PlaceReader.ENUM_TYPE_ARCHDIOCESE);


	private RelationshipDataLoader(){}


	static RelationshipData load(final FLEFModel model){
		final Map<String, FLEFRecord> filteredIndividuals = new LinkedHashMap<>();
		final List<FLEFRecord> individuals = model.getRecordsByType(IndividualHandler.TYPE);
		for(final FLEFRecord individual : individuals)
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual))
				filteredIndividuals.put(individual.getId(), individual);
		final Map<String, String> filteredGroups = new LinkedHashMap<>();
		final List<FLEFRecord> groups = model.getRecordsByType(GroupHandler.TYPE);
		for(final FLEFRecord group : groups)
			if(group.getId() != null && !PrivacyService.isHidden(model, group))
				filteredGroups.put(group.getId(), displayText(model, group));

		final List<RelationshipData.Event> events = loadEvents(model, filteredIndividuals.keySet());
		final Map<String, RelationshipData.Person> persons = new LinkedHashMap<>();
		final Map<String, List<FLEFRecord>> sexes = PreferredSex.indexByIndividual(model);
		for(final FLEFRecord individual : filteredIndividuals.values())
			persons.put(individual.getId(), loadPerson(individual, events, PreferredSex.of(individual, sexes, model)));

		return new RelationshipData(persons, filteredGroups, loadRelationships(model, persons.keySet(), filteredGroups.keySet()), events,
			clergy(model, persons.keySet()), ecclesiasticalPlaces(model), countries(model));
	}


	private static List<RelationshipData.Event> loadEvents(final FLEFModel model, final Set<String> individualIds){
		final Map<String, List<RelationshipData.Participant>> participants = new LinkedHashMap<>();
		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord participation : eventParticipations){
			final String personId = armValue(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(personId == null || eventId == null || !individualIds.contains(personId))
				continue;

			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			participants.computeIfAbsent(eventId, k -> new ArrayList<>())
				.add(new RelationshipData.Participant(personId, role == null || EventParticipationReader.ENUM_ROLE_PRINCIPAL.equals(role), role));
		}

		final List<RelationshipData.Event> events = new ArrayList<>();
		for(final Map.Entry<String, List<RelationshipData.Participant>> entry : participants.entrySet()){
			final FLEFRecord event = model.getRecordById(entry.getKey());
			if(!EventHandler.isOfType(event))
				continue;

			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final FLEFRecord date = (dates.isEmpty()? null: dates.getFirst());
			events.add(new RelationshipData.Event(entry.getKey(), EventReader.extractType(event), range(date), text(date),
				entry.getValue(), FLEFRecordHelper.getChildValue(event, PLACE_OF), sourceTitles(model, event)));
		}
		return events;
	}

	/** @return the {@code value} of the first structure of a record with that tag (a record can have several titles or names), or {@code null} */
	private static String firstValue(final FLEFRecord record, final String structure){
		final List<FLEFRecord> structures = FLEFRecordHelper.extractStructures(record, structure);
		return (structures.isEmpty()? null: FLEFRecordHelper.getChildValue(structures.getFirst(), "value"));
	}

	/** @return the titles of the sources that cite a record (an event) */
	private static List<String> sourceTitles(final FLEFModel model, final FLEFRecord record){
		final List<String> titles = new ArrayList<>();
		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(record, SourceCitationReader.TAG_SOURCE)){
			final FLEFRecord source = model.getRecordById(FLEFRecordHelper.getChildValue(citation, SourceCitationReader.TAG_SOURCE));
			final String title = (source != null? firstValue(source, SourceReader.TAG_TITLE): null);
			if(StringUtils.isNotBlank(title))
				titles.add(title.trim());
		}
		return titles;
	}

	/**
	 * @return the name of the largest place each place is part of (the country, if the places are described that far), in lower case,
	 * following the administrative relationships; a place that is part of no place is its own
	 */
	private static Map<String, String> countries(final FLEFModel model){
		final Map<String, String> names = new LinkedHashMap<>();
		for(final FLEFRecord place : model.getRecordsByType(PlaceHandler.TYPE))
			if(place.getId() != null)
				names.put(place.getId(), StringUtils.lowerCase(StringUtils.trimToEmpty(firstValue(place, "name"))));

		final Map<String, String> parentOf = new LinkedHashMap<>();
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipHandler.TYPE))
			if(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF.equals(PlaceRelationshipReader.extractType(relationship))){
				final String child = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_SUBJECT_PLACE);
				final String parent = FLEFRecordHelper.getChildValue(relationship, PlaceRelationshipReader.TAG_TARGET_PLACE);
				if(child != null && parent != null && !child.equals(parent))
					parentOf.putIfAbsent(child, parent);
			}

		final Map<String, String> countries = new LinkedHashMap<>();
		for(final String id : names.keySet()){
			String current = id;
			//the steps are limited, so that a circle of places (reported by the place checks) does not loop
			for(int step = 0; step < 20 && parentOf.containsKey(current); step ++)
				current = parentOf.get(current);
			countries.put(id, names.getOrDefault(current, ""));
		}
		return countries;
	}

	/** The individuals whose occupation says they are of the clergy. */
	private static Set<String> clergy(final FLEFModel model, final Set<String> individualIds){
		final Set<String> clergy = new LinkedHashSet<>();
		final List<FLEFRecord> attributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord attribute : attributes){
			final String personId = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL);
			final String value = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE);
			if(personId != null && value != null && individualIds.contains(personId)
					&& IndividualAttributeReader.ENUM_TYPE_OCCUPATION.equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE))
					&& StringUtils.containsAnyIgnoreCase(value, IndividualAttributeReader.CLERGY_OCCUPATIONS))
				clergy.add(personId);
		}
		return clergy;
	}

	/** The places of the church: parishes and dioceses, and the places that are part of another by an ecclesiastical relationship. */
	private static Set<String> ecclesiasticalPlaces(final FLEFModel model){
		final Set<String> places = new LinkedHashSet<>();
		final List<FLEFRecord> records = model.getRecordsByType(PlaceHandler.TYPE);
		for(final FLEFRecord place : records)
			if(place.getId() != null){
				//a place without a type is not ecclesiastical (and an immutable set does not accept null)
				final String type = FLEFRecordHelper.getChildValue(place, PlaceReader.TAG_TYPE);
				if(type != null && ECCLESIASTICAL_TYPES.contains(type))
					places.add(place.getId());
			}
		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships)
			if(PlaceRelationshipReader.ENUM_TYPE_ECCLESIASTICAL_PART_OF.equals(PlaceRelationshipReader.extractType(placeRelationship))){
				final String subject = FLEFRecordHelper.getChildValue(placeRelationship,
					FLEFRecordHelper.composePath(PlaceRelationshipReader.TAG_SUBJECT, PlaceHandler.TYPE));
				if(subject != null)
					places.add(subject);
			}
		return places;
	}

	private static RelationshipData.Person loadPerson(final FLEFRecord individual, final List<RelationshipData.Event> events,
			final SexType sex){
		//with several births the days they cover are used: a finding must hold for all of them
		DateRange birth = null;
		for(final RelationshipData.Event event : events)
			if(EventReader.ENUM_TYPE_BIRTH.equals(event.type()) && event.date() != null
					&& event.participants().stream().anyMatch(p -> p.principal() && p.personId().equals(individual.getId())))
				birth = (birth != null? birth.hull(event.date()): event.date());
		DateRange death = null;
		for(final RelationshipData.Event event : events)
			if(EventReader.ENUM_TYPE_DEATH.equals(event.type()) && event.date() != null
					&& event.participants().stream().anyMatch(p -> p.principal() && p.personId().equals(individual.getId())))
				death = (death != null? death.hull(event.date()): event.date());

		final String name = IndividualReader.extractPrimaryFullname(individual);
		return new RelationshipData.Person(individual.getId(), (name != null && !name.isBlank()? name + " [" + individual.getId() + "]": individual.getId()),
			sex, birth, death);
	}

	private static List<RelationshipData.Relationship> loadRelationships(final FLEFModel model, final Set<String> individualIds,
			final Set<String> groupIds){
		final List<RelationshipData.Relationship> filteredRelationships = new ArrayList<>();
		final List<FLEFRecord> relationships = model.getRecordsByType(RelationshipHandler.TYPE);
		for(final FLEFRecord relationship : relationships){
			final String type = RelationshipReader.extractType(relationship);
			final String[] subject = participant(relationship, RelationshipReader.TAG_SUBJECT, individualIds, groupIds);
			final String[] target = participant(relationship, RelationshipReader.TAG_TARGET, individualIds, groupIds);
			if(type == null || relationship.getId() == null || subject == null || target == null)
				continue;

			final FLEFRecord from = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
			filteredRelationships.add(new RelationshipData.Relationship(relationship.getId(), subject[0], kind(subject[1]), target[0],
				kind(target[1]), type, RelationshipReader.extractRole(relationship), range(from), range(to), text(from),
				RelationshipReader.ENUM_STATUS_ENDED.equals(FLEFRecordHelper.getChildValue(relationship, RelationshipReader.TAG_STATUS))));
		}
		return filteredRelationships;
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
		return (dateStructure != null? DateReader.extractPrettyPrintDate(dateStructure): StringUtils.EMPTY);
	}

}
