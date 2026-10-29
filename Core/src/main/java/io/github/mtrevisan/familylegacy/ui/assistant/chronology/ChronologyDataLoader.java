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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads the dates the chronology checks need from the model. The individuals hidden by the privacy settings are left out, as
 * they are from the views.
 */
final class ChronologyDataLoader{

	private static final String TAG_STATUS = "status";
	private static final String STATUS_ENDED = "ended";
	private static final String ROLE_PRINCIPAL = "principal";
	/** A relationship of a child with the parent it descends from: the generic "child" is taken as biological. */
	private static final Set<String> BIOLOGICAL_TYPES = Set.of(RelationshipReader.ENUM_TYPE_CHILD,
		RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
	private static final Set<String> MARRIAGE_UNION_TYPES = Set.of("spouse", "civil_spouse", "religious_spouse", "customary_spouse");


	private ChronologyDataLoader(){}


	static ChronologyData load(final FLEFModel model){
		final Map<String, FLEFRecord> individuals = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualHandler.TYPE))
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual))
				individuals.put(individual.getId(), individual);

		final List<ChronologyData.LifeEvent> events = loadEvents(model, individuals.keySet());
		final Map<String, ChronologyData.Person> persons = new LinkedHashMap<>();
		for(final FLEFRecord individual : individuals.values())
			persons.put(individual.getId(), loadPerson(individual, events));

		final List<ChronologyData.ParentLink> parents = new ArrayList<>();
		final List<ChronologyData.Union> unions = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE))
			loadRelationship(relationship, individuals.keySet(), parents, unions);
		return new ChronologyData(persons, events, parents, unions);
	}


	private static List<ChronologyData.LifeEvent> loadEvents(final FLEFModel model, final Set<String> individualIds){
		final List<ChronologyData.LifeEvent> events = new ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final FLEFRecord participant = FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT);
			final FLEFRecord reference = (participant != null? participant.getTheOnlyChild(): null);
			final String personId = (reference != null? reference.getValue(): null);
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			if(personId == null || eventId == null || !individualIds.contains(personId))
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			if(!EventHandler.isOfType(event))
				continue;

			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final FLEFRecord date = (dates.isEmpty()? null: dates.getFirst());
			events.add(new ChronologyData.LifeEvent(eventId, personId, EventReader.extractType(event), range(date), text(date),
				role == null || ROLE_PRINCIPAL.equals(role)));
		}
		return events;
	}

	/** The birth and the death of an individual are the events of that type the individual is the principal of. */
	private static ChronologyData.Person loadPerson(final FLEFRecord individual, final List<ChronologyData.LifeEvent> events){
		DateRange birth = null;
		DateRange death = null;
		String birthText = null;
		String deathText = null;
		for(final ChronologyData.LifeEvent event : events){
			if(!event.principal() || !individual.getId().equals(event.personId()) || event.date() == null)
				continue;

			//with several events of the same type the days they cover are used: a finding must hold for all of them
			if(EventReader.ENUM_TYPE_BIRTH.equals(event.type())){
				birth = (birth != null? birth.hull(event.date()): event.date());
				birthText = (birthText != null? birthText + " / " + event.dateText(): event.dateText());
			}
			else if(EventReader.ENUM_TYPE_DEATH.equals(event.type())){
				death = (death != null? death.hull(event.date()): event.date());
				deathText = (deathText != null? deathText + " / " + event.dateText(): event.dateText());
			}
		}

		final String name = IndividualReader.extractPrimaryFullname(individual);
		return new ChronologyData.Person(individual.getId(), (name != null && !name.isBlank()? name: individual.getId()),
			sex(individual), birth, death, birthText, deathText);
	}

	private static void loadRelationship(final FLEFRecord relationship, final Set<String> individualIds,
			final List<ChronologyData.ParentLink> parents, final List<ChronologyData.Union> unions){
		final String type = RelationshipReader.extractType(relationship);
		final String subjectId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
		final String targetId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
		if(type == null || subjectId == null || targetId == null || !individualIds.contains(subjectId)
				|| !individualIds.contains(targetId))
			return;

		if(RelationshipReader.isTypeChild(type))
			//the subject is the child, the target the parent
			parents.add(new ChronologyData.ParentLink(relationship.getId(), subjectId, targetId, BIOLOGICAL_TYPES.contains(type)));
		else if(RelationshipReader.isTypePartner(type)){
			final FLEFRecord from = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_FROM);
			final FLEFRecord to = FLEFRecordHelper.findChild(relationship, RelationshipReader.TAG_VALID_TO);
			final String status = FLEFRecordHelper.getChildValue(relationship, TAG_STATUS);
			unions.add(new ChronologyData.Union(relationship.getId(), subjectId, targetId, MARRIAGE_UNION_TYPES.contains(type),
				range(from), range(to), STATUS_ENDED.equals(status) && to == null, text(from)));
		}
	}


	private static SexType sex(final FLEFRecord individual){
		try{
			return IndividualReader.extractSex(individual);
		}
		catch(final IllegalArgumentException ignored){
			return SexType.UNKNOWN;
		}
	}

	/** @return the days of a date structure, or {@code null} if there is none or it cannot be read */
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
