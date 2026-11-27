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
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceCitationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


class QuickEntryServiceTest{

	private FLEFModel model;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
	}


	private static QuickEntryService.LifeFact fact(final String date, final QuickEntryService.PlaceRef place){
		return new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand(date, null), place);
	}

	private FLEFRecord only(final String type){
		final List<FLEFRecord> records = model.getRecordsByType(type);
		Assertions.assertEquals(1, records.size(), type);
		return records.getFirst();
	}

	private static String placeIdOf(final FLEFRecord event){
		return FLEFRecordHelper.getChildValue(event, FLEFRecordHelper.composePath(EventReader.TAG_PLACE, PlaceCitationReader.TAG_PLACE));
	}


	@Test
	void anIndividualWithEverythingHasTheRecordsTheFullEditorsWrite(){
		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, "Giovanni de Rossi", SexType.MALE,
			fact("12 March 1850", QuickEntryService.PlaceRef.named("Padova")), fact("abt 1920", QuickEntryService.PlaceRef.named("Venezia")));

		Assertions.assertNotNull(created);
		final FLEFRecord individual = model.getRecordById(created.individualId());
		Assertions.assertEquals("Giovanni de Rossi", IndividualReader.extractPrimaryFullname(individual));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(individual, "audit"));

		//the sex is the attribute, as the editor writes it
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(individual, model));
		final FLEFRecord sex = only(IndividualAttributeReader.TYPE);
		Assertions.assertEquals(created.individualId(), FLEFRecordHelper.getChildValue(sex, IndividualAttributeReader.TAG_INDIVIDUAL));
		Assertions.assertEquals(IndividualAttributeReader.ENUM_TYPE_SEX, FLEFRecordHelper.getChildValue(sex, IndividualAttributeReader.TAG_TYPE));

		Assertions.assertEquals(2, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(2, model.getRecordsByType(PlaceReader.TYPE).size());
		for(final FLEFRecord event : model.getRecordsByType(EventReader.TYPE)){
			final String type = FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE);
			Assertions.assertTrue(EventReader.ENUM_TYPE_BIRTH.equals(type) || EventReader.ENUM_TYPE_DEATH.equals(type));
			Assertions.assertNotNull(FLEFRecordHelper.findChild(event, EventReader.TAG_DATE));
			Assertions.assertNotNull(model.getRecordById(placeIdOf(event)));
			Assertions.assertNotNull(FLEFRecordHelper.findChild(event, "audit"));
		}
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			Assertions.assertEquals(created.individualId(), FLEFRecordHelper.getChildValue(participation,
				FLEFRecordHelper.composePath(EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL)));
			Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_PRINCIPAL, FLEFRecordHelper.getChildValue(participation,
				EventParticipationReader.TAG_ROLE));
			Assertions.assertTrue(model.hasRecord(FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT)));
		}
		Assertions.assertEquals(2, model.getRecordsByType(EventParticipationReader.TYPE).size());

		//everything that was added is told, the individual first
		Assertions.assertEquals(created.individualId(), created.recordIds().getFirst());
		Assertions.assertEquals(model.getRecordCount(), created.recordIds().size());
		for(final String id : created.recordIds())
			Assertions.assertTrue(model.hasRecord(id), id);
	}

	@Test
	void theNameIsSplitIntoPartsAndTheConfirmedPartsAreWrittenAsTheyAre(){
		final ParsedName confirmed = new ParsedName(List.of(new ParsedName.Part("given", "Maria"), new ParsedName.Part("family_prefix", "de la"),
			new ParsedName.Part("family", "Cruz")));

		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, confirmed, null, null, null);

		final FLEFRecord name = FLEFRecordHelper.findChild(model.getRecordById(created.individualId()), IndividualReader.TAG_NAME);
		Assertions.assertEquals(3, FLEFRecordHelper.findChildren(name, "part").size());
		Assertions.assertEquals("Maria de la Cruz", IndividualReader.extractPrimaryFullname(model.getRecordById(created.individualId())));
	}

	@Test
	void anUnknownSexWritesNoAttribute(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.UNKNOWN, null, null);
		QuickEntryService.createIndividual(model, "Maria Rossi", null, null, null);

		Assertions.assertTrue(model.getRecordsByType(IndividualAttributeReader.TYPE).isEmpty());
		Assertions.assertEquals(2, model.getRecordsByType(IndividualReader.TYPE).size());
	}

	@Test
	void aFactWithNeitherDateNorPlaceWritesNoEvent(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, QuickEntryService.LifeFact.NONE,
			new QuickEntryService.LifeFact(null, QuickEntryService.PlaceRef.named("   ")));

		Assertions.assertTrue(model.getRecordsByType(EventReader.TYPE).isEmpty());
		Assertions.assertTrue(model.getRecordsByType(EventParticipationReader.TYPE).isEmpty());
		Assertions.assertTrue(model.getRecordsByType(PlaceReader.TYPE).isEmpty());
	}

	@Test
	void aDateAloneOrAPlaceAloneIsEnough(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, fact("1850", null), QuickEntryService.LifeFact.NONE);
		QuickEntryService.createIndividual(model, "Maria Rossi", null, QuickEntryService.LifeFact.NONE,
			new QuickEntryService.LifeFact(null, QuickEntryService.PlaceRef.named("Padova")));

		Assertions.assertEquals(2, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
	}

	@Test
	void nothingToCreateCreatesNothing(){
		Assertions.assertNull(QuickEntryService.createIndividual(model, "  ", null, null, null));
		Assertions.assertNull(QuickEntryService.createIndividual(model, ParsedName.EMPTY, SexType.UNKNOWN, QuickEntryService.LifeFact.NONE,
			QuickEntryService.LifeFact.NONE));

		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void aSexAloneIsAnIndividualWithNoName(){
		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, "", SexType.FEMALE, null, null);

		Assertions.assertNotNull(created);
		Assertions.assertNull(FLEFRecordHelper.findChild(model.getRecordById(created.individualId()), IndividualReader.TAG_NAME));
		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(model.getRecordById(created.individualId()), model));
	}

	@Test
	void anExistingPlaceIsUsedAndNoPlaceIsCreated(){
		model.addRecord(FLEFRecord.createMainRecord("P9", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, "Padova"))));

		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, fact("1850", QuickEntryService.PlaceRef.existing("P9")), null);

		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals("P9", placeIdOf(only(EventReader.TYPE)));
	}

	@Test
	void aBirthAndADeathInPlacesWithTheSameNameShareTheNewPlace(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, fact("1850", QuickEntryService.PlaceRef.named("Padova")),
			fact("1920", QuickEntryService.PlaceRef.named("  padova ")));

		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		final List<FLEFRecord> events = model.getRecordsByType(EventReader.TYPE);
		Assertions.assertEquals(placeIdOf(events.get(0)), placeIdOf(events.get(1)));
	}

	@Test
	void aPlaceThatIsNotInTheFileIsRefusedAndNothingIsAdded(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.createIndividual(model, "Giovanni Rossi",
			SexType.MALE, fact("1850", QuickEntryService.PlaceRef.named("Padova")), fact("1920", QuickEntryService.PlaceRef.existing("P404"))));

		Assertions.assertEquals(0, model.getRecordCount());

		//an individual is not a place
		final QuickEntryService.Created person = QuickEntryService.createIndividual(model, "Maria Rossi", null, null, null);
		final int count = model.getRecordCount();
		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.createIndividual(model, "Giovanni Rossi", null,
			fact("1850", QuickEntryService.PlaceRef.existing(person.individualId())), null));
		Assertions.assertEquals(count, model.getRecordCount());
	}

	@Test
	void theLifeEventsOfAnIndividualThatIsBeingEditedAreAddedWithoutTheIndividual(){
		final List<String> ids = QuickEntryService.addLifeEvents(model, "I77", fact("1850", QuickEntryService.PlaceRef.named("Padova")),
			fact("1920", QuickEntryService.PlaceRef.named("Padova")));

		//two events, two participations, one shared place; the individual is not in the model (the editor adds it when it is confirmed)
		Assertions.assertEquals(5, ids.size());
		Assertions.assertEquals(5, model.getRecordCount());
		Assertions.assertEquals(0, model.getRecordsByType(IndividualReader.TYPE).size());
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE))
			Assertions.assertEquals("I77", FLEFRecordHelper.getChildValue(participation,
				FLEFRecordHelper.composePath(EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL)));
	}

	@Test
	void noKnownLifeFactAddsNothing(){
		Assertions.assertTrue(QuickEntryService.addLifeEvents(model, "I77", null, QuickEntryService.LifeFact.NONE).isEmpty());
		Assertions.assertEquals(0, model.getRecordCount());

		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.addLifeEvents(model, "I77", fact("1850",
			QuickEntryService.PlaceRef.existing("P404")), null));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theKindsOfSpouseBeganWithAMarriageAndTheEngagedWithAnEngagement(){
		for(final String type : new String[]{"spouse", "civil_spouse", "religious_spouse", "customary_spouse"})
			Assertions.assertEquals(EventReader.ENUM_TYPE_MARRIAGE, QuickEntryService.unionEventType(type), type);

		Assertions.assertEquals(EventReader.ENUM_TYPE_ENGAGEMENT, QuickEntryService.unionEventType("engaged_partner"));
		Assertions.assertNull(QuickEntryService.unionEventType("cohabiting_partner"));
		Assertions.assertNull(QuickEntryService.unionEventType("biological_child"));
		Assertions.assertNull(QuickEntryService.unionEventType(null));
	}

	@Test
	void aUnionIsOneEventWithBothPartnersAsPrincipals(){
		final List<String> ids = QuickEntryService.addUnionEvent(model, "I1", "I2", "civil_spouse",
			fact("5 May 1875", QuickEntryService.PlaceRef.named("Padova")));

		//the event, the place, the two participations
		Assertions.assertEquals(4, ids.size());
		final FLEFRecord event = only(EventReader.TYPE);
		Assertions.assertEquals(EventReader.ENUM_TYPE_MARRIAGE, FLEFRecordHelper.getChildValue(event, EventReader.TAG_TYPE));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(event, EventReader.TAG_DATE));
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertNotNull(model.getRecordById(placeIdOf(event)));
		final List<String> partners = new java.util.ArrayList<>();
		for(final FLEFRecord participation : model.getRecordsByType(EventParticipationReader.TYPE)){
			partners.add(FLEFRecordHelper.getChildValue(participation,
				FLEFRecordHelper.composePath(EventParticipationReader.TAG_PARTICIPANT, EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL)));
			Assertions.assertEquals(EventParticipationReader.ENUM_ROLE_PRINCIPAL, FLEFRecordHelper.getChildValue(participation,
				EventParticipationReader.TAG_ROLE));
			Assertions.assertEquals(event.getId(), FLEFRecordHelper.getChildValue(participation, EventParticipationReader.TAG_EVENT));
		}
		Assertions.assertEquals(List.of("I1", "I2"), partners);
	}

	@Test
	void anEngagementIsAnEngagementEvent(){
		QuickEntryService.addUnionEvent(model, "I1", "I2", "engaged_partner", fact("1874", null));

		Assertions.assertEquals(EventReader.ENUM_TYPE_ENGAGEMENT, FLEFRecordHelper.getChildValue(only(EventReader.TYPE), EventReader.TAG_TYPE));
	}

	@Test
	void aUnionWithNoEventOrNothingKnownAddsNothing(){
		Assertions.assertTrue(QuickEntryService.addUnionEvent(model, "I1", "I2", "cohabiting_partner", fact("1874", null)).isEmpty());
		Assertions.assertTrue(QuickEntryService.addUnionEvent(model, "I1", "I2", "spouse", QuickEntryService.LifeFact.NONE).isEmpty());
		Assertions.assertTrue(QuickEntryService.addUnionEvent(model, "I1", "I2", "spouse", null).isEmpty());
		Assertions.assertEquals(0, model.getRecordCount());

		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.addUnionEvent(model, "I1", "I2", "spouse",
			fact("1874", QuickEntryService.PlaceRef.existing("P404"))));
		Assertions.assertEquals(0, model.getRecordCount());
	}

	/** @return	The id of the place that the place is part of, {@code null} if none. */
	private String parentOf(final String placeId){
		for(final FLEFRecord relationship : model.getRecordsByType(PlaceRelationshipReader.TYPE))
			if(placeId.equals(relationship.extractReferencedId(PlaceRelationshipReader.TAG_SUBJECT, PlaceReader.TYPE))){
				Assertions.assertEquals(PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF, PlaceRelationshipReader.extractType(relationship));
				return relationship.extractReferencedId(PlaceRelationshipReader.TAG_TARGET, PlaceReader.TYPE);
			}
		return null;
	}

	private String nameOf(final String placeId){
		return PlaceReader.extractPrimaryName(model.getRecordById(placeId));
	}

	@Test
	void aNewPlaceInsideOthersIsAChainOfPlacesEachPartOfTheNext(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, fact("1850", QuickEntryService.PlaceRef.inside("Padova",
			List.of("Veneto", "Italy"), null)), null);

		Assertions.assertEquals(3, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals(2, model.getRecordsByType(PlaceRelationshipReader.TYPE).size());
		final String padova = placeIdOf(only(EventReader.TYPE));
		Assertions.assertEquals("Padova", nameOf(padova));
		final String veneto = parentOf(padova);
		Assertions.assertEquals("Veneto", nameOf(veneto));
		final String italy = parentOf(veneto);
		Assertions.assertEquals("Italy", nameOf(italy));
		Assertions.assertNull(parentOf(italy));
	}

	@Test
	void aNewPlaceInsideAPlaceOfTheFileIsPartOfIt(){
		model.addRecord(FLEFRecord.createMainRecord("P9", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, "Italy"))));

		QuickEntryService.createIndividual(model, "Giovanni Rossi", null, fact("1850", QuickEntryService.PlaceRef.inside("Padova",
			List.of("Veneto"), "P9")), null);

		Assertions.assertEquals(3, model.getRecordsByType(PlaceReader.TYPE).size());
		final String padova = placeIdOf(only(EventReader.TYPE));
		Assertions.assertEquals("P9", parentOf(parentOf(padova)));

		//directly inside it
		QuickEntryService.createIndividual(model, "Maria Bianchi", null, fact("1852", QuickEntryService.PlaceRef.inside("Roma", List.of(), "P9")), null);
		Assertions.assertEquals(4, model.getRecordsByType(PlaceReader.TYPE).size());
	}

	@Test
	void theFactsOfOneCreationShareTheNewPlacesThatTheyNameTheSameWayInTheSamePlaces(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null,
			fact("1850", QuickEntryService.PlaceRef.inside("Padova", List.of("Veneto", "Italy"), null)),
			fact("1920", QuickEntryService.PlaceRef.inside("Venezia", List.of("veneto", "ITALY"), null)));

		//Padova, Venezia, and a single Veneto and a single Italy
		Assertions.assertEquals(4, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals(3, model.getRecordsByType(PlaceRelationshipReader.TYPE).size());
		final List<FLEFRecord> events = model.getRecordsByType(EventReader.TYPE);
		Assertions.assertEquals(parentOf(placeIdOf(events.get(0))), parentOf(placeIdOf(events.get(1))));
	}

	@Test
	void thePlacesWithTheSameNameInsideOtherPlacesAreOtherPlaces(){
		QuickEntryService.createIndividual(model, "Giovanni Rossi", null,
			fact("1850", QuickEntryService.PlaceRef.inside("Padova", List.of("Veneto"), null)),
			fact("1920", QuickEntryService.PlaceRef.inside("Padova", List.of("Lombardia"), null)));

		Assertions.assertEquals(4, model.getRecordsByType(PlaceReader.TYPE).size());
	}

	@Test
	void aPlaceInsideAPlaceThatIsNotInTheFileChangesNothing(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.createIndividual(model, "Giovanni Rossi", null,
			fact("1850", QuickEntryService.PlaceRef.inside("Padova", List.of(), "P404")), null));

		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theBurialIsAnEventOfItsOwnWithThePersonAsPrincipal(){
		final List<String> ids = QuickEntryService.addLifeEvents(model, "I77", null, null,
			fact("1920", QuickEntryService.PlaceRef.named("Padova")));

		//the event, the place, the participation
		Assertions.assertEquals(3, ids.size());
		Assertions.assertEquals(EventReader.ENUM_TYPE_BURIAL, FLEFRecordHelper.getChildValue(only(EventReader.TYPE), EventReader.TAG_TYPE));
		Assertions.assertEquals(1, model.getRecordsByType(EventParticipationReader.TYPE).size());
	}

	@Test
	void theBirthTheDeathAndTheBurialAreThreeEventsThatShareTheNewPlace(){
		QuickEntryService.addLifeEvents(model, "I77", fact("1850", QuickEntryService.PlaceRef.named("Padova")),
			fact("1920", QuickEntryService.PlaceRef.named("Padova")), fact("1921", QuickEntryService.PlaceRef.named("padova")));

		Assertions.assertEquals(3, model.getRecordsByType(EventReader.TYPE).size());
		Assertions.assertEquals(1, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals(3, model.getRecordsByType(EventParticipationReader.TYPE).size());
	}

	@Test
	void aBurialInAPlaceThatIsNotInTheFileChangesNothing(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> QuickEntryService.addLifeEvents(model, "I77", fact("1850", null), null,
			fact("1920", QuickEntryService.PlaceRef.existing("P404"))));

		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theOccupationIsAnAttributeOfTheTypeOccupation(){
		final List<String> ids = QuickEntryService.addOccupation(model, "I77", "  blacksmith  and   farrier ");

		Assertions.assertEquals(1, ids.size());
		final FLEFRecord attribute = only(IndividualAttributeReader.TYPE);
		Assertions.assertEquals("I77", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_INDIVIDUAL));
		Assertions.assertEquals(IndividualAttributeReader.ENUM_TYPE_OCCUPATION, FLEFRecordHelper.getChildValue(attribute,
			IndividualAttributeReader.TAG_TYPE));
		Assertions.assertEquals("blacksmith and farrier", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));
		Assertions.assertNotNull(FLEFRecordHelper.findChild(attribute, "audit"));
	}

	@Test
	void aBlankOccupationAddsNothing(){
		Assertions.assertTrue(QuickEntryService.addOccupation(model, "I77", "   ").isEmpty());
		Assertions.assertTrue(QuickEntryService.addOccupation(model, "I77", null).isEmpty());
		Assertions.assertEquals(0, model.getRecordCount());
	}

	@Test
	void theIdsOfTwoCreationsDoNotClash(){
		final QuickEntryService.Created first = QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE,
			fact("1850", null), null);
		final QuickEntryService.Created second = QuickEntryService.createIndividual(model, "Maria Bianchi", SexType.FEMALE,
			fact("1852", null), null);

		Assertions.assertNotEquals(first.individualId(), second.individualId());
		Assertions.assertEquals(first.recordIds().size() + second.recordIds().size(), model.getRecordCount());
	}

}
