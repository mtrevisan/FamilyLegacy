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
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


class FamilyQuickEntryTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
	}


	private static FamilyQuickEntry.Person person(final String name, final SexType sex, final String birth, final String place){
		return new FamilyQuickEntry.Person(null, FullNameParser.parse(name), sex,
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand(birth, null), QuickEntryService.PlaceRef.named(place)));
	}

	private static FamilyQuickEntry.Input input(final FamilyQuickEntry.Person first, final FamilyQuickEntry.Person partner,
			final String unionType, final QuickEntryService.LifeFact union, final String childType, final FamilyQuickEntry.Person... children){
		return new FamilyQuickEntry.Input(first, partner, unionType, union, childType, List.of(children));
	}

	private static String name(final FLEFModel model, final String id){
		return IndividualReader.extractPrimaryFullname(model.getRecordById(id));
	}


	@Test
	void aFamilyIsThePeopleTheirBirthsTheUnionAndTheRelationshipsToCreate(){
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input(
			person("Giovanni Rossi", SexType.MALE, "1850", "Padova"),
			person("Maria Bianchi", SexType.FEMALE, "1852", "Padova"),
			"civil_spouse", new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("5 May 1875", null),
				QuickEntryService.PlaceRef.named("Padova")),
			"biological_child",
			person("Anna Rossi", SexType.FEMALE, "1876", null), person("Carlo Rossi", SexType.MALE, null, null)));

		Assertions.assertEquals(4, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals("Giovanni Rossi", name(model, created.firstId()));
		//births of the three that have one, and the union
		Assertions.assertEquals(4, model.getRecordsByType(EventReader.TYPE).size());
		//the same new place for the whole family
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		//the union: one marriage with both partners as principals
		final FLEFRecord marriage = model.getRecordsByType(EventReader.TYPE).stream()
			.filter(event -> EventReader.ENUM_TYPE_MARRIAGE.equals(FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE)))
			.findFirst()
			.orElseThrow();
		final long principals = model.getRecordsByType(EventParticipationReader.TYPE).stream()
			.filter(participation -> marriage.getId().equals(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT)))
			.count();
		Assertions.assertEquals(2, principals);
		Assertions.assertEquals(created.recordIds().size(), model.getRecordCount());

		//the relationships are told, not created
		Assertions.assertEquals(0, model.getRecordsByType(RelationshipReader.TYPE).size());
		final List<FamilyQuickEntry.Relationship> relationships = created.relationships();
		Assertions.assertEquals(5, relationships.size());
		Assertions.assertEquals("civil_spouse", relationships.getFirst().type());
		Assertions.assertEquals(created.firstId(), relationships.getFirst().subjectId());
		//a child is the subject, its parents the targets
		Assertions.assertEquals("biological_child", relationships.get(1).type());
		Assertions.assertEquals(created.firstId(), relationships.get(1).targetId());
		Assertions.assertNotEquals(created.firstId(), relationships.get(1).subjectId());
		Assertions.assertEquals(relationships.get(1).subjectId(), relationships.get(2).subjectId());
		Assertions.assertEquals(relationships.getFirst().targetId(), relationships.get(2).targetId());
	}

	@Test
	void theDeathOfAPersonIsAnEventOfItsOwn(){
		final FamilyQuickEntry.Person dead = new FamilyQuickEntry.Person(null, FullNameParser.parse("Giovanni Rossi"), SexType.MALE,
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1850", null), QuickEntryService.PlaceRef.NONE),
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1920", null), QuickEntryService.PlaceRef.named("Padova")));

		FamilyQuickEntry.create(model, input(dead, person("Maria Bianchi", null, null, null), "spouse", null, null));

		final List<String> types = model.getRecordsByType(EventReader.TYPE).stream()
			.map(event -> FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE))
			.toList();
		Assertions.assertEquals(List.of("birth", "death"), types);
		//the death has its participation too: the person is the principal of both
		Assertions.assertEquals(2, model.getRecordsByType(EventParticipationReader.TYPE).size());
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
	}

	@Test
	void aDeathAloneIsSomethingOfThePerson(){
		final FamilyQuickEntry.Person onlyDeath = new FamilyQuickEntry.Person(null, null, null, null, new QuickEntryService.LifeFact(
			DateStructureBuilder.fromShorthand("1920", null), QuickEntryService.PlaceRef.NONE));

		Assertions.assertFalse(onlyDeath.isEmpty());
		Assertions.assertTrue(new FamilyQuickEntry.Person(null, null, null, null, null).isEmpty());
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, input(person("Giovanni", null, null, null),
			null, null, null, null, new FamilyQuickEntry.Person(null, null, null, null, new QuickEntryService.LifeFact(null,
				QuickEntryService.PlaceRef.existing("P404"))))));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theRelationshipsCanBeCreatedInTheModelWhenThereIsNoView(){
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input(person("Giovanni Rossi", null, null, null),
			person("Maria Bianchi", null, null, null), "spouse", null, null, person("Anna", null, null, null)));

		final List<String> ids = FamilyQuickEntry.addRelationships(model, created.relationships());

		Assertions.assertEquals(3, ids.size());
		Assertions.assertEquals(3, model.getRecordsByType(RelationshipReader.TYPE).size());
		final FLEFRecord spouse = model.getRecordById(ids.getFirst());
		Assertions.assertEquals("spouse", RelationshipReader.extractType(spouse));
		Assertions.assertEquals(created.firstId(), spouse.extractReferencedId(RelationshipReader.TAG_SUBJECT, IndividualReader.TYPE));
	}

	@Test
	void aPersonOfTheFileIsLinkedAndNotCreatedAgain(){
		final String id = QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE, null, null).individualId();
		final int before = model.getRecordCount();

		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input(FamilyQuickEntry.Person.existing(id),
			null, null, null, null, person("Anna Rossi", SexType.FEMALE, null, null)));

		Assertions.assertEquals(id, created.firstId());
		Assertions.assertEquals(2, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertTrue(model.getRecordCount() > before);
		//no partner: the child has one parent
		Assertions.assertEquals(List.of(new FamilyQuickEntry.Relationship(created.relationships().getFirst().subjectId(), id,
			"biological_child")), created.relationships());
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(model.getRecordById(id), model));
	}

	@Test
	void theUnionOfATypeWithNoEventHasNoEvent(){
		FamilyQuickEntry.create(model, input(person("Giovanni Rossi", null, null, null), person("Maria Bianchi", null, null, null),
			"cohabiting_partner", new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1875", null), null), null));

		Assertions.assertEquals(0, model.getRecordsByType(EventReader.TYPE).size());
	}

	@Test
	void whatIsEmptyIsNotCreated(){
		Assertions.assertNull(FamilyQuickEntry.create(model, input(null, null, null, null, null)));
		Assertions.assertNull(FamilyQuickEntry.create(model, input(person("", null, null, null), person("  ", null, null, null), null, null, null,
			person("", null, null, null))));
		Assertions.assertEquals(0, model.getRecordCount());

		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input(person("Giovanni Rossi", null, null, null),
			person("", null, null, null), null, null, null, person("", null, null, null), person("Anna", null, null, null)));

		Assertions.assertEquals(2, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals(1, created.relationships().size());
	}

	@Test
	void aPartnerOrAChildNeedsAFirstPerson(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, input(null,
			person("Maria Bianchi", null, null, null), null, null, null)));
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, input(person("", null, null, null), null,
			null, null, null, person("Anna", null, null, null))));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void aWrongPersonOrPlaceChangesNothing(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, input(
			person("Giovanni Rossi", null, "1850", "Padova"), FamilyQuickEntry.Person.existing("I404"), null, null, null)));
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, input(
			person("Giovanni Rossi", null, null, null), person("Maria", null, null, null), "spouse",
			new QuickEntryService.LifeFact(null, QuickEntryService.PlaceRef.existing("P404")), null)));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theParentsOfAPersonAreRelationshipsOfThePersonWithThem(){
		final FamilyQuickEntry.Input input = new FamilyQuickEntry.Input(person("Anna Rossi", SexType.FEMALE, "1876", null), null, null, null, null,
			List.of(), person("Giovanni Rossi", SexType.MALE, "1850", null), person("Maria Bianchi", SexType.FEMALE, null, null), null, null);

		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, input);

		Assertions.assertEquals(3, model.getRecordsByType(IndividualReader.TYPE).size());
		Assertions.assertEquals(2, created.relationships().size());
		for(final FamilyQuickEntry.Relationship relationship : created.relationships()){
			//the person is the child, the parents the targets, biological by default
			Assertions.assertEquals(created.firstId(), relationship.subjectId());
			Assertions.assertEquals("biological_child", relationship.type());
		}
		Assertions.assertEquals("Giovanni Rossi", name(model, created.relationships().getFirst().targetId()));
		Assertions.assertEquals("Maria Bianchi", name(model, created.relationships().get(1).targetId()));
	}

	@Test
	void aSingleParentIsEnoughAndAParentWithoutAPersonIsRefused(){
		final FamilyQuickEntry.Created created = FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(
			person("Anna Rossi", SexType.FEMALE, null, null), null, null, null, null, List.of(), null, person("Maria Bianchi", SexType.FEMALE, null, null),
			null, "adoptive_child"));

		Assertions.assertEquals(1, created.relationships().size());
		Assertions.assertEquals("adoptive_child", created.relationships().getFirst().type());

		final int count = model.getRecordCount();
		Assertions.assertThrows(IllegalArgumentException.class, () -> FamilyQuickEntry.create(model, new FamilyQuickEntry.Input(null, null, null,
			null, null, List.of(), person("Giovanni Rossi", SexType.MALE, null, null), null, null, null)));
		Assertions.assertEquals(count, model.getRecordCount());
	}

}
