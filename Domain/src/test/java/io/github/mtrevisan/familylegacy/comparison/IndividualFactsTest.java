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
package io.github.mtrevisan.familylegacy.comparison;

import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** The facts of some individuals one under the other, and how they stand on each. */
class IndividualFactsTest{

	private FLEFModel model;
	private int sequence;


	/**
	 * Giovanni Masutti (I1) was born in 1850 in Borgo, son of Antonio Masutti (I10), and is a farmer; Giovanni Masutti (I2) was born in 1851,
	 * son of antonio masutti (I11, another record), with no place, and is a farmer too; Giovanni Masutti (I3) was born in 1850 in Borgo,
	 * baptised in Treviso, and has no parents.
	 */
	@BeforeEach
	void buildModel(){
		EventSummary.invalidate();
		model = new FLEFModel();
		sequence = 0;
		model.addRecord(person("I1", "Giovanni", "Masutti"));
		model.addRecord(person("I2", "Giovanni", "Masutti"));
		model.addRecord(person("I3", "Giovanni", "Masutti"));
		model.addRecord(person("I10", "Antonio", "Masutti"));
		model.addRecord(person("I11", "antonio", "masutti"));
		model.addRecord(place("P1", "Borgo"));
		model.addRecord(place("P2", "Treviso"));

		event("I1", EventReader.ENUM_TYPE_BIRTH, "P1", "1850-03-01");
		event("I2", EventReader.ENUM_TYPE_BIRTH, null, "1851-04-02");
		event("I3", EventReader.ENUM_TYPE_BIRTH, "P1", "1850-03-01");
		event("I3", EventReader.CUSTOM_TYPE_BAPTISM, "P2", "1850-03-10");
		//a witness of the birth of I2 is not born in it
		event("I1", EventReader.ENUM_TYPE_BIRTH, "P2", "1700-01-01", EventParticipationReader.ENUM_ROLE_WITNESS, "I2");

		model.addRecord(child("R1", "I1", "I10"));
		model.addRecord(child("R2", "I2", "I11"));
		model.addRecord(attribute("A1", "I1", IndividualAttributeReader.ENUM_TYPE_OCCUPATION, "Farmer"));
		model.addRecord(attribute("A2", "I2", IndividualAttributeReader.ENUM_TYPE_OCCUPATION, "farmer"));
		model.addRecord(attribute("A3", "I2", IndividualAttributeReader.ENUM_TYPE_RESIDENCE, "Borgo"));
	}


	private static FLEFRecord person(final String id, final String given, final String family){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given)))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", family))));
	}

	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private void event(final String personId, final String type, final String placeId, final String date){
		event(personId, type, placeId, date, EventParticipationReader.ENUM_ROLE_PRINCIPAL, personId);
	}

	/** An event, in which {@code personId} has the given role (the main one of the event is the first person given in {@code owner}). */
	private void event(final String ownerId, final String type, final String placeId, final String date, final String role,
			final String personId){
		sequence ++;
		final FLEFRecord event = FLEFRecord.createMainRecord("E" + sequence, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		if(date != null)
			event.addChild(FLEFRecord.createChildWithTag("date")
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", date)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
		if(placeId != null)
			event.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, placeId)));
		model.addRecord(event);
		if(!ownerId.equals(personId))
			model.addRecord(participation("EQ" + sequence, event.getId(), ownerId, EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		model.addRecord(participation("EP" + sequence, event.getId(), personId, role));
	}

	private static FLEFRecord participation(final String id, final String eventId, final String personId, final String role){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
	}

	private static FLEFRecord child(final String id, final String childId, final String parentId){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
	}

	private static FLEFRecord attribute(final String id, final String personId, final String type, final String value){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE.toLowerCase(), personId))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, value));
	}

	private static IndividualFacts.Row row(final IndividualFacts.Comparison comparison, final String key){
		return comparison.rows().stream()
			.filter(row -> key.equals(row.key()))
			.findFirst()
			.orElse(null);
	}


	@Test
	void theFactsOfTwoIndividualsAreOneUnderTheOther(){
		final IndividualFacts.Comparison comparison = IndividualFacts.compare(model, List.of("I1", "I2"));

		Assertions.assertEquals(List.of("I1", "I2"), comparison.individuals());
		Assertions.assertEquals(List.of("Giovanni Masutti [I1]", "Giovanni Masutti [I2]"), comparison.names());
		//the same name, written the same: nothing to tell them apart by it
		Assertions.assertEquals(IndividualFacts.State.SAME, row(comparison, "name").state());
		//different dates
		final IndividualFacts.Row born = row(comparison, "birth_date");
		Assertions.assertEquals(IndividualFacts.State.DIFFERENT, born.state());
		Assertions.assertEquals(2, born.values().size());
		Assertions.assertFalse(born.values().get(0).isBlank());
		Assertions.assertNotEquals(born.values().get(0), born.values().get(1));
	}

	@Test
	void aPlaceOnOneSideOnlyIsMissingNotDifferent(){
		final IndividualFacts.Row place = row(IndividualFacts.compare(model, List.of("I1", "I2")), "birth_place");

		Assertions.assertEquals(IndividualFacts.State.MISSING, place.state());
		Assertions.assertTrue(place.values().get(0).contains("Borgo"), place.values().toString());
		Assertions.assertTrue(place.values().get(1).isEmpty());
	}

	@Test
	void parentsAreComparedByNameNotByIdentifier(){
		//Antonio Masutti (I10) and antonio masutti (I11) are two records with the same name
		final IndividualFacts.Row parents = row(IndividualFacts.compare(model, List.of("I1", "I2")), "parents");

		Assertions.assertEquals(IndividualFacts.State.SAME, parents.state());
		//and occupations, written with other capitals
		Assertions.assertEquals(IndividualFacts.State.SAME, row(IndividualFacts.compare(model, List.of("I1", "I2")), "occupations").state());
	}

	@Test
	void theWitnessOfABirthIsNotBornInIt(){
		//I1 is a witness at the birth E5 of I2 in 1700, and is the principal of E1 in 1850
		final IndividualFacts.Row born = row(IndividualFacts.compare(model, List.of("I1", "I2")), "birth_date");

		Assertions.assertFalse(born.values().get(0).contains("1700"), born.values().toString());
	}

	@Test
	void moreThanTwoIndividualsAndAFactNoOneHasIsNotARow(){
		final IndividualFacts.Comparison comparison = IndividualFacts.compare(model, List.of("I1", "I2", "I3"));

		Assertions.assertEquals(3, comparison.individuals().size());
		//I1 and I3 are born the same day, in the same place; I2 has no place
		Assertions.assertEquals(IndividualFacts.State.DIFFERENT, row(comparison, "birth_date").state());
		Assertions.assertEquals(IndividualFacts.State.MISSING, row(comparison, "birth_place").state());
		//only I3 was baptised
		Assertions.assertEquals(IndividualFacts.State.MISSING, row(comparison, "baptism").state());
		//no one has a death, a spouse or a sex
		Assertions.assertNull(row(comparison, "death_date"));
		Assertions.assertNull(row(comparison, "spouses"));
		Assertions.assertNull(row(comparison, "sex"));
		//only I2 has a residence
		Assertions.assertEquals(IndividualFacts.State.MISSING, row(comparison, "residences").state());
	}

	@Test
	void whatIsNotAnIndividualOrIsRepeatedIsLeftOut(){
		Assertions.assertTrue(IndividualFacts.compare(model, List.of("I1")).isEmpty());
		Assertions.assertTrue(IndividualFacts.compare(model, List.of("I1", "I1")).isEmpty());
		Assertions.assertTrue(IndividualFacts.compare(model, List.of("I1", "P1")).isEmpty());
		Assertions.assertEquals(List.of("I1", "I2"), IndividualFacts.compare(model, List.of("I1", "X9", "I2", "I1")).individuals());
	}

	@Test
	void theStateOfAFactIsToldWithoutCapitalsAndSpaces(){
		Assertions.assertEquals(IndividualFacts.State.SAME, IndividualFacts.stateOf(List.of("Borgo,  Treviso", "borgo, treviso")));
		Assertions.assertEquals(IndividualFacts.State.DIFFERENT, IndividualFacts.stateOf(List.of("1850", "1851", "")));
		Assertions.assertEquals(IndividualFacts.State.MISSING, IndividualFacts.stateOf(List.of("1850", "")));
		Assertions.assertEquals(IndividualFacts.State.SAME, IndividualFacts.stateOf(List.of("", "")));
	}

}
