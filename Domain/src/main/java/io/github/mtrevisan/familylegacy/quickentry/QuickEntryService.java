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
package io.github.mtrevisan.familylegacy.quickentry;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.AuditStamp;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The common genealogical facts entered in one step, without knowing the records behind them.
 * <p>
 * The service writes the same records that the full editors write (it does not simplify the model, only the way to fill it): the
 * individual with its name, the {@code sex} attribute, and for the birth and the death an event, with its date and its place, and the
 * participation of the individual as the {@code principal} of the event. The advanced editors remain the way to change any of them.
 * <p>
 * A creation is <b>atomic</b>: the records are built apart and added to the model together at the end, so that nothing is added if
 * the input is wrong (and nothing is created if there is nothing to create).
 */
public final class QuickEntryService{

	/**
	 * Where a fact happened: a place of the file, or the name of a place that is not in the file yet. A name always makes a new place (flat,
	 * with that name, unless it is said to be inside others): looking for an existing place is up to the caller, which lets the user choose
	 * (see {@link PlaceLookup} and {@link PlaceChain}).
	 * <p>
	 * A new place may be inside other places, new or not: {@code Padova, Veneto, Italy} is the new place <i>Padova</i>, part of the new
	 * place <i>Veneto</i>, part of <i>Italy</i>, which may be a place of the file ({@code enclosingId}); each is part of the next one as an
	 * administrative jurisdiction.
	 *
	 * @param existingId	The id of a place of the file, {@code null} if none.
	 * @param newName	The name of a new place, {@code null} if none.
	 * @param enclosing	The names of the new places that contain the new place, from the nearest to the largest (empty if none).
	 * @param enclosingId	The id of the place of the file that contains the largest of the new places ({@code null} if none).
	 */
	public record PlaceRef(String existingId, String newName, List<String> enclosing, String enclosingId){

		/** No place. */
		public static final PlaceRef NONE = new PlaceRef(null, null, List.of(), null);


		public PlaceRef{
			enclosing = List.copyOf(enclosing);
		}

		public PlaceRef(final String existingId, final String newName){
			this(existingId, newName, List.of(), null);
		}


		/** @return	The place of the file with the given id ({@link #NONE} if the id is blank). */
		public static PlaceRef existing(final String id){
			return (StringUtils.isNotBlank(id)? new PlaceRef(id, null): NONE);
		}

		/** @return	A new place with the given name ({@link #NONE} if the name is blank). */
		public static PlaceRef named(final String name){
			return (StringUtils.isNotBlank(name)? new PlaceRef(null, StringUtils.normalizeSpace(name)): NONE);
		}

		/**
		 * @param name	The name of the new place (not blank).
		 * @param enclosing	The names of the new places that contain it, from the nearest to the largest.
		 * @param enclosingId	The id of the place of the file that contains the largest ({@code null} if none).
		 * @return	A new place inside others.
		 */
		public static PlaceRef inside(final String name, final List<String> enclosing, final String enclosingId){
			return new PlaceRef(null, StringUtils.normalizeSpace(name), enclosing, enclosingId);
		}

		/** @return	Whether there is no place. */
		public boolean isNone(){
			return (existingId == null && newName == null);
		}

	}

	/**
	 * When and where something happened.
	 *
	 * @param date	The {@code DateStructure} (see {@link DateStructureBuilder}), {@code null} if the date is not known.
	 * @param place	The place.
	 */
	public record LifeFact(FLEFRecord date, PlaceRef place){

		/** Nothing is known. */
		public static final LifeFact NONE = new LifeFact(null, PlaceRef.NONE);


		public LifeFact{
			place = (place != null? place: PlaceRef.NONE);
		}


		/** @return	Whether there is neither a date nor a place. */
		public boolean isEmpty(){
			return ((date == null || !date.hasData()) && place.isNone());
		}

	}

	/**
	 * What a creation added to the model.
	 *
	 * @param individualId	The id of the individual.
	 * @param recordIds	The ids of every record that was added (the individual included), in the order they were added.
	 */
	public record Created(String individualId, List<String> recordIds){

		public Created{
			recordIds = List.copyOf(recordIds);
		}

	}


	private static final String DIGITS = "0123456789";


	private QuickEntryService(){}


	/**
	 * Creates an individual from the full name as it is typed (see {@link FullNameParser}).
	 *
	 * @see #createIndividual(FLEFModel, ParsedName, SexType, LifeFact, LifeFact)
	 */
	public static Created createIndividual(final FLEFModel model, final String fullName, final SexType sex, final LifeFact birth,
			final LifeFact death){
		return createIndividual(model, FullNameParser.parse(fullName), sex, birth, death);
	}

	/**
	 * Creates an individual, with the facts that are known.
	 * <p>
	 * An unknown sex writes nothing, as the unknown sex is the absence of the attribute; a fact with neither date nor place writes nothing;
	 * a birth and a death in a place with the same name share the new place.
	 *
	 * @param model	The model to add the records to.
	 * @param name	The name (as the user confirmed it), possibly empty.
	 * @param sex	The sex ({@code null} is the unknown one).
	 * @param birth	The birth ({@code null} if not known).
	 * @param death	The death ({@code null} if not known).
	 * @return	What was added, {@code null} if there is nothing to create (no name, no known sex, no fact).
	 * @throws IllegalArgumentException	If a place is said to be in the file and is not (nothing is added).
	 */
	public static Created createIndividual(final FLEFModel model, final ParsedName name, final SexType sex, final LifeFact birth,
			final LifeFact death){
		final ParsedName personalName = (name != null? name: ParsedName.EMPTY);
		final SexType knownSex = (sex != null? sex: SexType.UNKNOWN);
		final LifeFact birthFact = (birth != null? birth: LifeFact.NONE);
		final LifeFact deathFact = (death != null? death: LifeFact.NONE);
		if(personalName.isEmpty() && knownSex == SexType.UNKNOWN && birthFact.isEmpty() && deathFact.isEmpty())
			return null;

		requireExistingPlace(model, birthFact.place());
		requireExistingPlace(model, deathFact.place());

		final List<FLEFRecord> records = new ArrayList<>();
		try{
			final String individualId = buildIndividual(model, records, personalName, knownSex, birthFact, deathFact, new HashMap<>());

			return new Created(individualId, commit(model, records));
		}
		catch(final RuntimeException re){
			release(model, records);
			throw re;
		}
	}

	/**
	 * Builds the records of an individual (the individual, its {@code sex} attribute, the events of its birth and death with their
	 * participations and the new places) and puts them in the list, not in the model: whoever builds them adds them together, so that
	 * what is made of many individuals (a family) is atomic too.
	 *
	 * @param model	The model (to find the ids and the places).
	 * @param records	Where the records are put.
	 * @param newPlaces	The new places that were built already, by name (the new places of the individual are put in it too).
	 * @return	The id of the individual.
	 */
	static String buildIndividual(final FLEFModel model, final List<FLEFRecord> records, final ParsedName personalName,
			final SexType knownSex, final LifeFact birthFact, final LifeFact deathFact, final Map<String, String> newPlaces){
		final FLEFRecord individual = FLEFRecord.createMainRecord(IndividualReader.TYPE, IndividualReader.ID_PREFIX, model);
		records.add(individual);
		final FLEFRecord nameRecord = personalName.toRecord();
		if(nameRecord != null)
			individual.addChild(nameRecord);
		individual.addChild(AuditStamp.now());

		if(knownSex != SexType.UNKNOWN)
			records.add(FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individual.getId()))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, knownSex.getRawSex()))
				.addChild(AuditStamp.now()));

		addLifeEvent(model, records, individual.getId(), EventReader.ENUM_TYPE_BIRTH, birthFact, newPlaces);
		addLifeEvent(model, records, individual.getId(), EventReader.ENUM_TYPE_DEATH, deathFact, newPlaces);
		return individual.getId();
	}

	/** Puts the records in the model, together. @return	Their ids, in order. */
	static List<String> commit(final FLEFModel model, final List<FLEFRecord> records){
		final List<String> ids = new ArrayList<>(records.size());
		for(final FLEFRecord record : records){
			model.addRecord(record);
			ids.add(record.getId());
		}
		return ids;
	}

	/** Gives back the ids of the records that were built and not added to the model (something went wrong). */
	static void release(final FLEFModel model, final List<FLEFRecord> records){
		for(final FLEFRecord record : records)
			if(!model.hasRecord(record.getId()))
				FLEFRecord.releaseReservedId(prefixOf(record));
	}


	/**
	 * Adds the birth and the death of an individual that is being created (or that already exists), the way {@link #createIndividual}
	 * does: an event for each known fact, the participation of the individual as its {@code principal}, the new places. The individual
	 * does not need to be in the model yet (an editor adds it when it is confirmed), but the events must not be added before that.
	 * <p>
	 * It is atomic as well: nothing is added if a place of the file is not there.
	 *
	 * @param model	The model to add the records to.
	 * @param individualId	The id of the individual.
	 * @param birth	The birth ({@code null} if not known).
	 * @param death	The death ({@code null} if not known).
	 * @return	The ids of the records that were added, in order (empty if there is no known fact).
	 * @throws IllegalArgumentException	If a place is said to be in the file and is not.
	 */
	public static List<String> addLifeEvents(final FLEFModel model, final String individualId, final LifeFact birth, final LifeFact death){
		return addLifeEvents(model, individualId, birth, death, null);
	}

	/**
	 * The same as {@link #addLifeEvents(FLEFModel, String, LifeFact, LifeFact)}, with the burial too.
	 *
	 * @param burial	The burial ({@code null} if not known).
	 */
	public static List<String> addLifeEvents(final FLEFModel model, final String individualId, final LifeFact birth, final LifeFact death,
			final LifeFact burial){
		final LifeFact birthFact = (birth != null? birth: LifeFact.NONE);
		final LifeFact deathFact = (death != null? death: LifeFact.NONE);
		final LifeFact burialFact = (burial != null? burial: LifeFact.NONE);
		if(birthFact.isEmpty() && deathFact.isEmpty() && burialFact.isEmpty())
			return List.of();

		requireExistingPlace(model, birthFact.place());
		requireExistingPlace(model, deathFact.place());
		requireExistingPlace(model, burialFact.place());

		final List<FLEFRecord> records = new ArrayList<>();
		final Map<String, String> newPlaces = new HashMap<>();
		addLifeEvent(model, records, individualId, EventReader.ENUM_TYPE_BIRTH, birthFact, newPlaces);
		addLifeEvent(model, records, individualId, EventReader.ENUM_TYPE_DEATH, deathFact, newPlaces);
		addLifeEvent(model, records, individualId, EventReader.ENUM_TYPE_BURIAL, burialFact, newPlaces);

		final List<String> ids = new ArrayList<>(records.size());
		for(final FLEFRecord record : records){
			model.addRecord(record);
			ids.add(record.getId());
		}
		return ids;
	}


	/**
	 * Adds the occupation of an individual that is being created (or that exists already): an attribute of the type {@code occupation}
	 * with the text as its value. The individual does not need to be in the model yet (an editor adds it when it is confirmed).
	 *
	 * @param model	The model to add the record to.
	 * @param individualId	The id of the individual.
	 * @param occupation	The occupation (blank: nothing is added).
	 * @return	The ids of the records that were added (empty if the occupation is blank).
	 */
	public static List<String> addOccupation(final FLEFModel model, final String individualId, final String occupation){
		final String text = StringUtils.defaultString(StringUtils.normalizeSpace(occupation));
		if(text.isEmpty())
			return List.of();

		final FLEFRecord attribute = buildOccupation(model, individualId, text);
		model.addRecord(attribute);
		return List.of(attribute.getId());
	}

	/** @return	The attribute of the occupation, not added to the model. */
	static FLEFRecord buildOccupation(final FLEFModel model, final String individualId, final String text){
		return FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, individualId))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_OCCUPATION))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, text))
			.addChild(AuditStamp.now());
	}

	static void requireExistingPlace(final FLEFModel model, final PlaceRef place){
		requirePlaceOfTheFile(model, place.existingId());
		requirePlaceOfTheFile(model, place.enclosingId());
	}

	private static void requirePlaceOfTheFile(final FLEFModel model, final String id){
		if(id == null)
			return;

		final FLEFRecord record = model.getRecordById(id);
		if(record == null || !Strings.CI.equals(PlaceReader.TYPE, record.getTag()))
			throw new IllegalArgumentException("Not a place of the file: " + id);
	}

	/** Builds the event (with its place, if new) and the participation of the individual as its principal; nothing if the fact is empty. */
	private static void addLifeEvent(final FLEFModel model, final List<FLEFRecord> records, final String individualId, final String type,
			final LifeFact fact, final Map<String, String> newPlaces){
		addEvent(model, records, List.of(individualId), type, fact, newPlaces);
	}

	/** Builds the event (with its place, if new) and the participation of each individual as a principal; nothing if the fact is empty. */
	static void addEvent(final FLEFModel model, final List<FLEFRecord> records, final List<String> individualIds, final String type,
			final LifeFact fact, final Map<String, String> newPlaces){
		if(type == null || fact.isEmpty())
			return;

		final FLEFRecord event = FLEFRecord.createMainRecord(EventReader.TYPE, EventReader.ID_PREFIX, model);
		records.add(event);
		event.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		if(fact.date() != null && fact.date().hasData())
			event.addChild(fact.date());
		final String placeId = resolvePlaceId(model, records, fact.place(), newPlaces);
		if(placeId != null)
			event.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceCitationReader.TAG_PLACE, placeId)));
		event.addChild(AuditStamp.now());

		for(final String individualId : individualIds)
			records.add(FLEFRecord.createMainRecord(EventParticipationReader.TYPE, EventParticipationReader.ID_PREFIX, model)
				.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
					.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, individualId)))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL))
				.addChild(AuditStamp.now()));
	}

	/**
	 * @param relationshipType	The type of a relationship between two individuals (see {@link RelationshipReader}).
	 * @return	The type of the event that tells when and where the relationship began ({@code marriage} for the kinds of spouse,
	 * 	{@code engagement} for the engaged partners), {@code null} if there is none to say (the cohabiting partners, the relationships
	 * 	that are not a union): a relationship and the event that started it are two different things, and some have no event.
	 */
	public static String unionEventType(final String relationshipType){
		if(relationshipType == null)
			return null;

		return switch(relationshipType){
			case RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE,
				RelationshipReader.ENUM_TYPE_CUSTOMARY_SPOUSE -> EventReader.ENUM_TYPE_MARRIAGE;
			case RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER -> EventReader.ENUM_TYPE_ENGAGEMENT;
			default -> null;
		};
	}

	/**
	 * Adds the event that started a union (the marriage, the engagement) of two individuals, with the participation of each as a
	 * {@code principal}: the relationship itself is created by whoever links the individuals (the views that show it must know), the event
	 * is the date and the place that a relationship does not have. Atomic like the other creations.
	 *
	 * @param model	The model to add the records to.
	 * @param firstId	The id of an individual.
	 * @param secondId	The id of the other individual.
	 * @param relationshipType	The type of the relationship between them (see {@link #unionEventType}).
	 * @param fact	When and where the union began ({@code null} if not known).
	 * @return	The ids of the records that were added, in order (empty if the relationship has no event, or nothing is known).
	 * @throws IllegalArgumentException	If a place is said to be in the file and is not.
	 */
	public static List<String> addUnionEvent(final FLEFModel model, final String firstId, final String secondId, final String relationshipType,
			final LifeFact fact){
		final String type = unionEventType(relationshipType);
		final LifeFact union = (fact != null? fact: LifeFact.NONE);
		if(type == null || union.isEmpty())
			return List.of();

		requireExistingPlace(model, union.place());

		final List<FLEFRecord> records = new ArrayList<>();
		addEvent(model, records, List.of(firstId, secondId), type, union, new HashMap<>());
		final List<String> ids = new ArrayList<>(records.size());
		for(final FLEFRecord record : records){
			model.addRecord(record);
			ids.add(record.getId());
		}
		return ids;
	}

	/**
	 * @return	The id of the place: the one of the file, or a new one (built, with the new places that contain it, each part of the next as
	 * 	an administrative jurisdiction); the new places are shared by the facts that name them the same way, inside the same places.
	 * 	{@code null} if there is none.
	 */
	static String resolvePlaceId(final FLEFModel model, final List<FLEFRecord> records, final PlaceRef place,
			final Map<String, String> newPlaces){
		if(place.existingId() != null)
			return place.existingId();
		if(place.newName() == null)
			return null;

		//from the largest new place to the place: each is part of the one before it
		final List<String> names = new ArrayList<>(place.enclosing().reversed());
		names.add(place.newName());
		String parentId = place.enclosingId();
		final StringBuilder path = new StringBuilder(StringUtils.defaultString(place.enclosingId()));
		for(final String name : names){
			path.append('\u0000').append(PlaceLookup.fold(name));
			final String key = path.toString();
			String id = newPlaces.get(key);
			if(id == null){
				final FLEFRecord record = FLEFRecord.createMainRecord(PlaceReader.TYPE, PlaceReader.ID_PREFIX, model)
					.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
						.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name)))
					.addChild(AuditStamp.now());
				records.add(record);
				id = record.getId();
				newPlaces.put(key, id);
				if(parentId != null)
					records.add(FLEFRecord.createMainRecord(PlaceRelationshipReader.TYPE, PlaceRelationshipReader.ID_PREFIX, model)
						.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
							.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, id)))
						.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
							.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, parentId)))
						.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
							PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF))
						.addChild(AuditStamp.now()));
			}
			parentId = id;
		}
		return parentId;
	}

	/** @return	The prefix of the id of the record (its id without the number). */
	static String prefixOf(final FLEFRecord record){
		return StringUtils.stripEnd(record.getId(), DIGITS);
	}

}
