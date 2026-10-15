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
package io.github.mtrevisan.familylegacy.ui.tools.places;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ContextImpactReader;
import io.github.mtrevisan.familylegacy.io.model.readers.DateReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IdentityHypothesisReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.ui.handlers.ConclusionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ContextImpactHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.CulturalNormHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.EventParticipationHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.HistoricEventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IdentityHypothesisHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualAttributeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.PlaceRelationshipHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RecordTypeHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.RepositoryHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.ResearchQuestionHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared extraction utilities for {@code PlaceRecord} and
 * {@code PlaceRelationshipRecord}.
 * <p>
 * The methods here are the single source of truth for how a place field
 * is read from the model. Every tool in the {@code places} package goes
 * through this class, so a change to the FLEF structure only needs to be
 * reflected here.
 */
public final class PlaceHelper{

	private PlaceHelper(){}


	/** Returns every {@code PlaceRecord} in the model, in insertion order. */
	public static List<FLEFRecord> listAllPlaces(final FLEFModel model){
		return model.getRecordsByType(PlaceHandler.TYPE);
	}

	/** Returns every {@code PlaceRelationshipRecord} in the model. */
	public static List<FLEFRecord> listAllRelationships(final FLEFModel model){
		return model.getRecordsByType(PlaceRelationshipHandler.TYPE);
	}

	/**
	 * Returns the primary display name of a place. {@code PlaceRecord.name}
	 * is a list of {@code NameStructure}; the first one with a non-blank
	 * {@code value} is used. When no name is available, the place id is
	 * returned so the display stays readable.
	 */
	public static String displayName(final FLEFRecord place){
		if(place == null)
			return StringUtils.EMPTY;

		for(final FLEFRecord child : place.getChildren()){
			if(!PlaceReader.TAG_NAME.equalsIgnoreCase(child.getTag()))
				continue;

			final String value = PlaceReader.extractPrimaryName(child);
			if(value != null && !value.isBlank())
				return value;

			final FLEFRecord onlyChild = child.getTheOnlyChild();
			if(onlyChild != null && onlyChild.getValue() != null && !onlyChild.getValue().isBlank())
				return onlyChild.getValue();
		}
		return place.getId() != null? place.getId(): StringUtils.EMPTY;
	}

	/** Returns the type of a place, or {@code null} when missing. */
	public static String placeType(final FLEFRecord place){
		return (place != null? FLEFRecordHelper.getChildValue(place, PlaceReader.TAG_TYPE): null);
	}

	/** Returns the coordinates of a place, or {@code null} when missing. */
	public static String coordinates(final FLEFRecord place){
		if(place == null)
			return null;
		final FLEFRecord map = FLEFRecordHelper.findChild(place, PlaceReader.TAG_MAP);
		if(map == null)
			return null;
		return FLEFRecordHelper.getChildValue(map, PlaceReader.TAG_COORDINATES);
	}

	/**
	 * Returns the value of a nested {@code DateStructure} field, or
	 * {@code null} when the field is absent. The value is used as-is,
	 * without parsing.
	 */
	public static String dateValue(final FLEFRecord record, final String tag){
		if(record == null)
			return null;
		final FLEFRecord dateStruct = FLEFRecordHelper.findChild(record, tag);
		if(dateStruct == null)
			return null;
		final String value = FLEFRecordHelper.getChildValue(dateStruct, DateReader.TAG_VALUE);
		if(value != null)
			return value;
		final FLEFRecord onlyChild = dateStruct.getTheOnlyChild();
		return (onlyChild != null? onlyChild.getValue(): null);
	}

	/** Returns the referenced place id of a relationship endpoint. */
	public static String endpointPlaceId(final FLEFRecord relationship, final String tag){
		if(relationship == null)
			return null;
		return relationship.extractReferencedId(tag, PlaceHandler.TYPE);
	}

	/**
	 * Deletes the given place and all the relationships that
	 * involve it, in a single operation. The relationships are removed
	 * first, so the model is never left with a dangling reference.
	 *
	 * @param placeId the place to delete
	 * @param model        the model
	 */
	public static void deletePlace(final String placeId, final FLEFModel model){
		if(placeId == null)
			return;

		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds = relationshipIds(placeId, model);
		for(final Map.Entry<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> entry : relationshipIds.entrySet())
			for(final FLEFRecord record : entry.getValue())
				model.removeRecord(record.getId());

		model.removeRecord(placeId);
	}

	/**
	 * Returns the ids of every relationship in which the given
	 * place appears cited.
	 */
	public static Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> relationshipIds(final String placeId, final FLEFModel model){
		final Map<Class<? extends RecordTypeHandler<?>>, List<FLEFRecord>> result = new HashMap<>();
		if(placeId == null)
			return result;

		final List<FLEFRecord> eventParticipations = model.getRecordsByType(EventParticipationHandler.TYPE);
		for(final FLEFRecord eventParticipation : eventParticipations){
			final String participant = eventParticipation.extractReferencedId(EventParticipationReader.TAG_EVENT, PlaceHandler.TYPE);
			if(placeId.equals(participant))
				result.computeIfAbsent(EventParticipationHandler.class, k -> new ArrayList<>())
					.add(eventParticipation);
		}

		//TODO place citation
		final List<FLEFRecord> events = model.getRecordsByType(EventHandler.TYPE);
		for(final FLEFRecord event : events){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(event, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(EventHandler.class, k -> new ArrayList<>())
					.add(event);
		}
		final List<FLEFRecord> individualAttributes = model.getRecordsByType(IndividualAttributeHandler.TYPE);
		for(final FLEFRecord individualAttribute : individualAttributes){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(individualAttribute, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(IndividualAttributeHandler.class, k -> new ArrayList<>())
					.add(individualAttribute);
		}
		final List<FLEFRecord> groupAttributes = model.getRecordsByType(GroupAttributeHandler.TYPE);
		for(final FLEFRecord groupAttribute : groupAttributes){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(groupAttribute, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(GroupAttributeHandler.class, k -> new ArrayList<>())
					.add(groupAttribute);
		}
		final List<FLEFRecord> repositories = model.getRecordsByType(RepositoryHandler.TYPE);
		for(final FLEFRecord repository : repositories){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(repository, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(RepositoryHandler.class, k -> new ArrayList<>())
					.add(repository);
		}
		final List<FLEFRecord> culturalNorms = model.getRecordsByType(CulturalNormHandler.TYPE);
		for(final FLEFRecord culturalNorm : culturalNorms){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(culturalNorm, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(CulturalNormHandler.class, k -> new ArrayList<>())
					.add(culturalNorm);
		}
		final List<FLEFRecord> sources = model.getRecordsByType(SourceHandler.TYPE);
		for(final FLEFRecord source : sources){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(source, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(SourceHandler.class, k -> new ArrayList<>())
					.add(source);
		}
		final List<FLEFRecord> historicEvents = model.getRecordsByType(HistoricEventHandler.TYPE);
		for(final FLEFRecord historicEvent : historicEvents){
			final FLEFRecord place = FLEFRecordHelper.extractStructureWithReference(historicEvent, EventReader.TAG_PLACE);
			if(place != null && placeId.equals(FLEFRecordHelper.getChildValue(place, PlaceCitationReader.TAG_PLACE)))
				result.computeIfAbsent(HistoricEventHandler.class, k -> new ArrayList<>())
					.add(historicEvent);
		}

		final List<FLEFRecord> placeRelationships = model.getRecordsByType(PlaceRelationshipHandler.TYPE);
		for(final FLEFRecord placeRelationship : placeRelationships){
			final String subjectId = placeRelationship.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualHandler.TYPE);
			final String objectId = placeRelationship.extractReferencedId(RelationshipReader.TAG_OBJECT, IndividualHandler.TYPE);
			if(placeId.equals(subjectId) || placeId.equals(objectId))
				result.computeIfAbsent(PlaceRelationshipHandler.class, k -> new ArrayList<>())
					.add(placeRelationship);
		}

		final List<FLEFRecord> contextImpacts = model.getRecordsByType(ContextImpactHandler.TYPE);
		for(final FLEFRecord contextImpact : contextImpacts){
			final String target = contextImpact.extractReferencedId(ContextImpactReader.TAG_TARGET, PlaceHandler.TYPE);
			if(placeId.equals(target))
				result.computeIfAbsent(ContextImpactHandler.class, k -> new ArrayList<>())
					.add(contextImpact);
		}

		final List<FLEFRecord> researchQuestions = model.getRecordsByType(ResearchQuestionHandler.TYPE);
		for(final FLEFRecord researchQuestion : researchQuestions){
			final List<FLEFRecord> targets = FLEFRecordHelper.findChildren(researchQuestion, ResearchQuestionReader.TAG_TARGET, PlaceHandler.TYPE);
			for(final FLEFRecord target : targets)
				if(placeId.equals(target)){
					result.computeIfAbsent(ResearchQuestionHandler.class, k -> new ArrayList<>())
						.add(researchQuestion);

					break;
				}
		}

		final List<FLEFRecord> conclusions = model.getRecordsByType(ConclusionHandler.TYPE);
		for(final FLEFRecord conclusion : conclusions){
			final List<FLEFRecord> resolves = FLEFRecordHelper.findChildren(conclusion, ConclusionReader.TAG_RESOLVES, PlaceHandler.TYPE);
			for(final FLEFRecord resolve : resolves)
				if(placeId.equals(resolve)){
					result.computeIfAbsent(ConclusionHandler.class, k -> new ArrayList<>())
						.add(conclusion);

					break;
				}
		}

		final List<FLEFRecord> identityHypotheses = model.getRecordsByType(IdentityHypothesisHandler.TYPE);
		for(final FLEFRecord identityHypothesis : identityHypotheses){
			final List<FLEFRecord> identities = FLEFRecordHelper.findChildren(identityHypothesis, IdentityHypothesisReader.TAG_IDENTITY, PlaceHandler.TYPE);
			for(final FLEFRecord identity : identities)
				if(placeId.equals(identity.getId())){
					result.computeIfAbsent(IdentityHypothesisHandler.class, k -> new ArrayList<>())
						.add(identityHypothesis);

					break;
				}
		}

		return result;
	}


	/** A row of the place management table. */
	public record PlaceRow(String id, String name, String type, String parents, String children, String coordinates){}

	/**
	 * Builds a compact row for the management table. Parents and children
	 * are rendered as a comma-separated list of display names, truncated
	 * to keep the row readable.
	 */
	public static PlaceRow toRow(final FLEFRecord place, final FLEFModel model,
		final Map<String, FLEFRecord> placesById){
		final String id = place.getId();
		final String name = displayName(place);
		final String type = placeType(place);
		final String coords = coordinates(place);

		final List<String> parentNames = new ArrayList<>();
		final List<String> childNames = new ArrayList<>();
		for(final FLEFRecord rel : listAllRelationships(model)){
			final String subjectId = endpointPlaceId(rel, PlaceRelationshipReader.TAG_SUBJECT);
			final String objectId = endpointPlaceId(rel, PlaceRelationshipReader.TAG_OBJECT);
			if(id.equals(subjectId) && objectId != null){
				final FLEFRecord child = placesById.get(objectId);
				if(child != null)
					childNames.add(displayName(child));
			}
			if(id.equals(objectId) && subjectId != null){
				final FLEFRecord parent = placesById.get(subjectId);
				if(parent != null)
					parentNames.add(displayName(parent));
			}
		}

		return new PlaceRow(id, name, type,
			join(parentNames), join(childNames), coords);
	}

	private static String join(final List<String> values){
		if(values.isEmpty())
			return StringUtils.EMPTY;

		if(values.size() <= 3)
			return String.join(", ", values);

		return values.get(0) + ", " + values.get(1) + ", " + values.get(2)
			+ " (+" + (values.size() - 3) + ")";
	}

}
