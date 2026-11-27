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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
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
 * The quick entry of a small family in one step: a person (new, or one that is in the file), the partner, the union (its type, when and
 * where it began) and the children. Only what is filled is created: a person with nothing in it is not.
 * <p>
 * It writes the records that the full editors write: the individuals, the events of their births and deaths, the marriage or engagement event of
 * the union with both partners as principals, and says which relationships link them, to be created by whoever knows the views
 * ({@link #addRelationships} does it in the model when there is no view): a child is a child of the person and, if there is a partner,
 * of the partner. The records are built apart and added together: nothing is added if the input is wrong.
 */
public final class FamilyQuickEntry{

	/**
	 * A person of the family.
	 *
	 * @param existingId	The id of an individual of the file, {@code null} for a new one.
	 * @param name	The name of a new individual (as the user confirmed it, possibly empty).
	 * @param sex	The sex of a new individual ({@code null}: unknown).
	 * @param birth	The birth of a new individual ({@code null}: not known).
	 * @param death	The death of a new individual ({@code null}: not known).
	 */
	public record Person(String existingId, ParsedName name, SexType sex, QuickEntryService.LifeFact birth, QuickEntryService.LifeFact death){

		public Person{
			name = (name != null? name: ParsedName.EMPTY);
			sex = (sex != null? sex: SexType.UNKNOWN);
			birth = (birth != null? birth: QuickEntryService.LifeFact.NONE);
			death = (death != null? death: QuickEntryService.LifeFact.NONE);
		}

		/** A person that is not known to be dead. */
		public Person(final String existingId, final ParsedName name, final SexType sex, final QuickEntryService.LifeFact birth){
			this(existingId, name, sex, birth, null);
		}


		/** @return	An individual of the file. */
		public static Person existing(final String id){
			return new Person(id, null, null, null, null);
		}

		/** @return	Whether there is nothing of the person: no individual of the file and nothing known of a new one. */
		public boolean isEmpty(){
			return (existingId == null && name.isEmpty() && sex == SexType.UNKNOWN && birth.isEmpty() && death.isEmpty());
		}

	}

	/**
	 * What to create.
	 *
	 * @param first	The first person (the one the family is of): required if there is a partner or a child.
	 * @param partner	The partner ({@code null} or empty: none).
	 * @param unionType	The type of the relationship between the two (see {@link RelationshipReader#PARTNER_TYPES}).
	 * @param union	When and where the union began ({@code null}: not known); written only for the types that began with an event.
	 * @param childType	The type of the relationship of the children with their parents (see {@link RelationshipReader#CHILD_TYPES}).
	 * @param children	The children (the empty ones are left out).
	 * @param father	The father of the first person ({@code null} or empty: none).
	 * @param mother	The mother of the first person ({@code null} or empty: none).
	 * @param fatherType	The type of the relationship of the first person with the father (see {@link RelationshipReader#CHILD_TYPES}).
	 * @param motherType	The type of the relationship of the first person with the mother.
	 */
	public record Input(Person first, Person partner, String unionType, QuickEntryService.LifeFact union, String childType,
			List<Person> children, Person father, Person mother, String fatherType, String motherType){

		/** A family without the parents of the first person. */
		public Input(final Person first, final Person partner, final String unionType, final QuickEntryService.LifeFact union,
				final String childType, final List<Person> children){
			this(first, partner, unionType, union, childType, children, null, null, null, null);
		}

		public Input{
			children = (children != null? List.copyOf(children): List.of());
		}

	}

	/**
	 * A relationship to create.
	 *
	 * @param subjectId	The subject (for a child, the child).
	 * @param targetId	The target (for a child, the parent).
	 * @param type	The type.
	 */
	public record Relationship(String subjectId, String targetId, String type){}

	/**
	 * What was added to the model, and what is left to link.
	 *
	 * @param firstId	The id of the first person.
	 * @param recordIds	The ids of every record that was added, in order.
	 * @param relationships	The relationships that link the people (not created yet).
	 */
	public record Created(String firstId, List<String> recordIds, List<Relationship> relationships){

		public Created{
			recordIds = List.copyOf(recordIds);
			relationships = List.copyOf(relationships);
		}

	}


	private FamilyQuickEntry(){}


	/**
	 * Creates the people, their births and deaths, and the union event.
	 *
	 * @param model	The model to add the records to.
	 * @param input	What to create.
	 * @return	What was added, {@code null} if there is nothing to create.
	 * @throws IllegalArgumentException	If there is a partner or a child but no first person, if a person is said to be in the file and
	 * 	is not an individual of it, or a place is said to be in the file and is not (nothing is added).
	 */
	public static Created create(final FLEFModel model, final Input input){
		final Person first = (input.first() != null? input.first(): new Person(null, null, null, null));
		final Person partner = (input.partner() != null && !input.partner().isEmpty()? input.partner(): null);
		final Person father = (input.father() != null && !input.father().isEmpty()? input.father(): null);
		final Person mother = (input.mother() != null && !input.mother().isEmpty()? input.mother(): null);
		final List<Person> children = input.children().stream()
			.filter(child -> child != null && !child.isEmpty())
			.toList();
		if(first.isEmpty() && partner == null && children.isEmpty() && father == null && mother == null)
			return null;
		if(first.isEmpty() && (partner != null || !children.isEmpty() || father != null || mother != null))
			throw new IllegalArgumentException("A family needs a first person");

		for(final Person person : listOf(first, partner, father, mother, children)){
			requireIndividual(model, person);
			QuickEntryService.requireExistingPlace(model, person.birth().place());
			QuickEntryService.requireExistingPlace(model, person.death().place());
		}
		final QuickEntryService.LifeFact union = (input.union() != null? input.union(): QuickEntryService.LifeFact.NONE);
		QuickEntryService.requireExistingPlace(model, union.place());

		final List<FLEFRecord> records = new ArrayList<>();
		try{
			//a place with the same name is the same new place for the whole family
			final Map<String, String> newPlaces = new HashMap<>();
			final String firstId = idOf(model, records, first, newPlaces);
			final List<Relationship> relationships = new ArrayList<>();

			if(father != null)
				relationships.add(new Relationship(firstId, idOf(model, records, father, newPlaces),
					StringUtils.defaultIfBlank(input.fatherType(), RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)));
			if(mother != null)
				relationships.add(new Relationship(firstId, idOf(model, records, mother, newPlaces),
					StringUtils.defaultIfBlank(input.motherType(), RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)));

			String partnerId = null;
			if(partner != null){
				partnerId = idOf(model, records, partner, newPlaces);
				final String unionType = StringUtils.defaultIfBlank(input.unionType(), RelationshipReader.ENUM_TYPE_SPOUSE);
				relationships.add(new Relationship(firstId, partnerId, unionType));
				QuickEntryService.addEvent(model, records, List.of(firstId, partnerId), QuickEntryService.unionEventType(unionType), union,
					newPlaces);
			}

			final String childType = StringUtils.defaultIfBlank(input.childType(), RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD);
			for(final Person child : children){
				final String childId = idOf(model, records, child, newPlaces);
				relationships.add(new Relationship(childId, firstId, childType));
				if(partnerId != null)
					relationships.add(new Relationship(childId, partnerId, childType));
			}

			return new Created(firstId, QuickEntryService.commit(model, records), relationships);
		}
		catch(final RuntimeException re){
			QuickEntryService.release(model, records);
			throw re;
		}
	}

	/**
	 * Creates, in the model, the relationships of a creation: for whoever has no view that has to know them (the views create them through
	 * their own mutator, so that what they keep is up to date).
	 *
	 * @param model	The model.
	 * @param relationships	The relationships.
	 * @return	The ids of the records that were added.
	 */
	public static List<String> addRelationships(final FLEFModel model, final List<Relationship> relationships){
		final List<String> ids = new ArrayList<>(relationships.size());
		for(final Relationship relationship : relationships){
			final FLEFRecord record = FLEFRecord.createMainRecord(RelationshipReader.TYPE, RelationshipReader.ID_PREFIX, model)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, relationship.type()))
				.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, relationship.subjectId())))
				.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
					.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE, relationship.targetId())))
				.addChild(AuditStamp.now());
			model.addRecord(record);
			ids.add(record.getId());
		}
		return ids;
	}


	private static List<Person> listOf(final Person first, final Person partner, final Person father, final Person mother,
			final List<Person> children){
		final List<Person> people = new ArrayList<>();
		people.add(first);
		if(partner != null)
			people.add(partner);
		if(father != null)
			people.add(father);
		if(mother != null)
			people.add(mother);
		people.addAll(children);
		return people;
	}

	private static void requireIndividual(final FLEFModel model, final Person person){
		if(person.existingId() == null)
			return;

		final FLEFRecord record = model.getRecordById(person.existingId());
		if(record == null || !Strings.CI.equals(IndividualReader.TYPE, record.getTag()))
			throw new IllegalArgumentException("Not an individual of the file: " + person.existingId());
	}

	/** @return	The id of the person: the one of the file, or the one of the individual that is built. */
	private static String idOf(final FLEFModel model, final List<FLEFRecord> records, final Person person, final Map<String, String> newPlaces){
		return (person.existingId() != null
			? person.existingId()
			: QuickEntryService.buildIndividual(model, records, person.name(), person.sex(), person.birth(), person.death(),
				newPlaces));
	}

}
