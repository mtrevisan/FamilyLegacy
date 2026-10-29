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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RelationshipHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Reads from the model what the conclusion checks need. The individuals hidden by the privacy settings are left out, as they are
 * from the views.
 */
final class ConclusionDataLoader{

	private static final String ROLE_PRINCIPAL = "principal";
	private static final String TAG_IDENTITY_INDIVIDUAL = "individual";


	private ConclusionDataLoader(){}


	static ConclusionData load(final FLEFModel model){
		final Map<String, ConclusionData.Person> persons = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualHandler.TYPE))
			if(individual.getId() != null && !PrivacyService.isHidden(model, individual)){
				final String name = IndividualReader.extractPrimaryFullname(individual);
				persons.put(individual.getId(), new ConclusionData.Person(individual.getId(),
					(name != null && !name.isBlank()? name: individual.getId()), sex(individual)));
			}

		return new ConclusionData(persons, loadEvents(model, persons.keySet()), loadParents(model, persons.keySet()),
			loadAttributes(model, persons.keySet()), loadHypotheses(model), loadConclusions(model));
	}


	/** The births and deaths: for each individual, the events of that type they are the principal of. */
	private static List<ConclusionData.PrincipalEvent> loadEvents(final FLEFModel model, final Set<String> individualIds){
		final Map<String, Set<String>> recordIds = new LinkedHashMap<>();
		final Map<String, String[]> keys = new LinkedHashMap<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationHandler.TYPE)){
			final String personId = armValue(FLEFRecordHelper.findChild(participation, EventParticipationReader.TAG_PARTICIPANT));
			final String eventId = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT);
			final String role = FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_ROLE);
			if(personId == null || eventId == null || !individualIds.contains(personId)
					|| (role != null && !ROLE_PRINCIPAL.equals(role)))
				continue;

			final FLEFRecord event = model.getRecordById(eventId);
			final String type = (EventHandler.isOfType(event)? EventReader.extractType(event): null);
			if(!EventReader.ENUM_TYPE_BIRTH.equals(type) && !EventReader.ENUM_TYPE_DEATH.equals(type))
				continue;

			final String key = personId + "|" + eventId;
			keys.putIfAbsent(key, new String[]{personId, eventId, type});
			final Set<String> ids = recordIds.computeIfAbsent(key, k -> new LinkedHashSet<>());
			ids.add(eventId);
			if(participation.getId() != null)
				ids.add(participation.getId());
		}

		final List<ConclusionData.PrincipalEvent> events = new ArrayList<>();
		for(final Map.Entry<String, String[]> entry : keys.entrySet()){
			final FLEFRecord event = model.getRecordById(entry.getValue()[1]);
			final List<FLEFRecord> dates = FLEFRecordHelper.extractStructures(event, EventReader.TAG_DATE);
			final FLEFRecord date = (dates.isEmpty()? null: dates.getFirst());
			events.add(new ConclusionData.PrincipalEvent(entry.getValue()[1], entry.getValue()[0], entry.getValue()[2], range(date),
				(date != null? DateReader.extractPrettyPrintDate(date): ""), recordIds.get(entry.getKey()),
				FLEFRecordHelper.findChildren(event, EventReader.TAG_SOURCE).size(),
				FLEFRecordHelper.getChildValue(event, EventReader.TAG_CONFIDENCE)));
		}
		return events;
	}

	private static List<ConclusionData.ParentLink> loadParents(final FLEFModel model, final Set<String> individualIds){
		final List<ConclusionData.ParentLink> parents = new ArrayList<>();
		for(final FLEFRecord relationship : model.getRecordsByType(RelationshipHandler.TYPE)){
			final String type = RelationshipReader.extractType(relationship);
			if(type == null || !RelationshipReader.isTypeChild(type))
				continue;

			//the subject is the child, the target the parent
			final String childId = relationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String parentId = relationship.extractReferencedId(RelationshipReader.TAG_TARGET, IndividualHandler.TYPE);
			if(childId != null && parentId != null && relationship.getId() != null && individualIds.contains(childId)
					&& individualIds.contains(parentId))
				parents.add(new ConclusionData.ParentLink(relationship.getId(), childId, parentId, type));
		}
		return parents;
	}

	private static List<ConclusionData.Attribute> loadAttributes(final FLEFModel model, final Set<String> individualIds){
		final List<ConclusionData.Attribute> attributes = new ArrayList<>();
		for(final FLEFRecord attribute : model.getRecordsByType(IndividualAttributeHandler.TYPE)){
			final String personId = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL);
			final String type = FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE);
			if(attribute.getId() != null && personId != null && type != null && individualIds.contains(personId))
				attributes.add(new ConclusionData.Attribute(attribute.getId(), personId, type,
					FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE)));
		}
		return attributes;
	}

	/** The hypotheses between two individuals. */
	private static List<ConclusionData.Hypothesis> loadHypotheses(final FLEFModel model){
		final List<ConclusionData.Hypothesis> hypotheses = new ArrayList<>();
		for(final FLEFRecord hypothesis : model.getRecordsByType(IdentityHypothesisHandler.TYPE)){
			final List<String> candidates = new ArrayList<>();
			for(final FLEFRecord identity : FLEFRecordHelper.findChildren(hypothesis, IdentityHypothesisReader.TAG_IDENTITY))
				if(!identity.getChildren().isEmpty() && TAG_IDENTITY_INDIVIDUAL.equals(identity.getChildren().getFirst().getTag())
						&& armValue(identity) != null)
					candidates.add(armValue(identity));
			if(hypothesis.getId() != null && candidates.size() == 2)
				hypotheses.add(new ConclusionData.Hypothesis(hypothesis.getId(), candidates.get(0), candidates.get(1)));
		}
		return hypotheses;
	}

	private static List<ConclusionData.Conclusion> loadConclusions(final FLEFModel model){
		final List<ConclusionData.Conclusion> conclusions = new ArrayList<>();
		for(final FLEFRecord conclusion : model.getRecordsByType(ConclusionHandler.TYPE)){
			final Set<String> resolves = new LinkedHashSet<>();
			for(final FLEFRecord target : FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES))
				if(armValue(target) != null)
					resolves.add(armValue(target));
			conclusions.add(new ConclusionData.Conclusion(conclusion.getId(), resolves,
				armValue(FLEFRecordHelper.findChild(conclusion, ConclusionReader.TAG_PREFERRED)),
				ConclusionReader.extractProofStatus(conclusion)));
		}
		return conclusions;
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

}
