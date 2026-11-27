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

import io.github.mtrevisan.familylegacy.events.EventHelper;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PreferredSex;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


class LifeFactUpdaterTest{

	private FLEFModel model;
	private String personId;


	@BeforeEach
	void setUp(){
		model = new FLEFModel();
		final QuickEntryService.Created created = QuickEntryService.createIndividual(model, "Giovanni Rossi", SexType.MALE,
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1850", null), QuickEntryService.PlaceRef.named("Padova")),
			new QuickEntryService.LifeFact(DateStructureBuilder.fromShorthand("1920", null), QuickEntryService.PlaceRef.NONE));
		personId = created.individualId();
	}


	private FLEFRecord birth(){
		return LifeFactUpdater.principalEvents(model, personId, EventReader.ENUM_TYPE_BIRTH).getFirst();
	}

	private static String dateOf(final FLEFRecord event){
		return EventHelper.eventDateRaw(event);
	}

	private static List<String> tags(final FLEFRecord event){
		return event.getChildren().stream()
			.map(FLEFRecord::getTag)
			.toList();
	}

	private void addWitnessedEvent(final String type, final String role){
		final FLEFRecord event = FLEFRecord.createMainRecord(EventReader.TYPE, EventReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type));
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord(EventParticipationReader.TYPE, EventParticipationReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role)));
	}


	@Test
	void theEventsThePersonIsThePrincipalOfAreFound(){
		Assertions.assertEquals(1, LifeFactUpdater.principalEvents(model, personId, "birth").size());
		Assertions.assertEquals(1, LifeFactUpdater.principalEvents(model, personId, "death").size());
		Assertions.assertTrue(LifeFactUpdater.principalEvents(model, personId, "burial").isEmpty());
		Assertions.assertTrue(LifeFactUpdater.principalEvents(model, "I404", "birth").isEmpty());
		Assertions.assertTrue(LifeFactUpdater.principalEvents(model, null, "birth").isEmpty());
	}

	@Test
	void aBirthTheWitnessIsNotThePrincipalOfIsNotTheirs(){
		addWitnessedEvent("birth", EventParticipationReader.ENUM_ROLE_WITNESS);

		Assertions.assertEquals(1, LifeFactUpdater.principalEvents(model, personId, "birth").size());
	}

	@Test
	void aParticipationWithoutRoleIsThePrincipalOne(){
		final FLEFRecord event = FLEFRecord.createMainRecord(EventReader.TYPE, EventReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, "birth"));
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord(EventParticipationReader.TYPE, EventParticipationReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId())));

		Assertions.assertEquals(2, LifeFactUpdater.principalEvents(model, personId, "birth").size());
	}


	@Test
	void theDateIsReplacedInPlaceAndTheEventKeepsItsOrder(){
		final FLEFRecord event = birth();

		final LifeFactUpdater.Applied applied = LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(true,
			DateStructureBuilder.fromShorthand("12 March 1851", null), false, null));

		Assertions.assertEquals("12 March 1851", dateOf(event));
		Assertions.assertEquals(List.of("type", "date", "place", "audit"), tags(event).stream().distinct().toList());
		Assertions.assertTrue(applied.addedIds().isEmpty());
		Assertions.assertEquals(1, FLEFRecordHelper.findChildren(event, "date").size());
	}

	@Test
	void theDateIsRemovedWhenClearedAndPutBackByTheUndo(){
		final FLEFRecord event = birth();
		final String before = dateOf(event);

		final LifeFactUpdater.Applied applied = LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(true, null, false, null));
		Assertions.assertNull(dateOf(event));

		applied.undo().run();
		Assertions.assertEquals(before, dateOf(event));
	}

	@Test
	void aDateIsAddedToAnEventThatHasNone(){
		final FLEFRecord death = LifeFactUpdater.principalEvents(model, personId, "death").getFirst();
		LifeFactUpdater.update(model, death, new LifeFactUpdater.Edit(true, null, false, null));

		LifeFactUpdater.update(model, death, new LifeFactUpdater.Edit(true, DateStructureBuilder.fromShorthand("1921", null), false, null));

		Assertions.assertEquals("1921", dateOf(death));
		Assertions.assertEquals(List.of("type", "date", "audit"), tags(death));
	}

	@Test
	void theUnchangedIsNotTouched(){
		final FLEFRecord event = birth();
		final FLEFRecord copy = FLEFRecord.createEmpty();
		event.deepCopyTo(copy);

		Assertions.assertSame(LifeFactUpdater.Applied.NOTHING, LifeFactUpdater.update(model, event,
			new LifeFactUpdater.Edit(false, null, false, null)));

		Assertions.assertEquals(copy.getChildren().size(), event.getChildren().size());
	}

	@Test
	void theAuditTellsThatTheEventWasUpdated(){
		final FLEFRecord event = birth();
		final long before = event.getChildren().stream()
			.filter(child -> "audit".equals(child.getTag()))
			.count();

		LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(true, DateStructureBuilder.fromShorthand("1851", null), false, null));

		Assertions.assertEquals(before, event.getChildren().stream()
			.filter(child -> "audit".equals(child.getTag()))
			.count());
		final FLEFRecord audit = FLEFRecordHelper.findChild(event, "audit");
		Assertions.assertNotNull(FLEFRecordHelper.findChild(audit, "update"));
	}

	@Test
	void thePlaceIsReplacedByAnExistingOneOrANewOneOrRemoved(){
		final FLEFRecord event = birth();
		model.addRecord(FLEFRecord.createMainRecord("P900", PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, "Venezia"))));

		LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(false, null, true, QuickEntryService.PlaceRef.existing("P900")));
		Assertions.assertEquals("P900", EventHelper.eventPlaceId(event));
		Assertions.assertEquals(List.of("type", "date", "place", "audit"), tags(event).stream().distinct().toList());

		final int places = model.getRecordsByType(PlaceReader.TYPE).size();
		final LifeFactUpdater.Applied created = LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(false, null, true,
			QuickEntryService.PlaceRef.named("Verona")));
		Assertions.assertEquals(places + 1, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals(1, created.addedIds().size());
		Assertions.assertEquals(created.addedIds().getFirst(), EventHelper.eventPlaceId(event));

		created.undo().run();
		Assertions.assertEquals(places, model.getRecordsByType(PlaceReader.TYPE).size());
		Assertions.assertEquals("P900", EventHelper.eventPlaceId(event));

		LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(false, null, true, null));
		Assertions.assertNull(EventHelper.eventPlaceId(event));
	}

	@Test
	void aPlaceThatIsNotInTheFileChangesNothing(){
		final FLEFRecord event = birth();
		final String place = EventHelper.eventPlaceId(event);

		Assertions.assertThrows(IllegalArgumentException.class, () -> LifeFactUpdater.update(model, event, new LifeFactUpdater.Edit(true,
			DateStructureBuilder.fromShorthand("1851", null), true, QuickEntryService.PlaceRef.existing("P404"))));

		Assertions.assertEquals(place, EventHelper.eventPlaceId(event));
		Assertions.assertEquals("1850", dateOf(event));
	}

	@Test
	void aCitedDateOrPlaceIsNotEditable(){
		final FLEFRecord event = birth();
		Assertions.assertTrue(LifeFactUpdater.isDateEditable(event));
		Assertions.assertTrue(LifeFactUpdater.isPlaceEditable(event));

		FLEFRecordHelper.findChild(event, "date")
			.addChild(FLEFRecord.createChildWithTag("source")
				.addChild(FLEFRecord.createChildWithTagAndValue("source", "S1")));
		FLEFRecordHelper.findChild(event, "place")
			.addChild(FLEFRecord.createChildWithTagAndValue("confidence", "high"));

		Assertions.assertFalse(LifeFactUpdater.isDateEditable(event));
		Assertions.assertFalse(LifeFactUpdater.isPlaceEditable(event));
		Assertions.assertThrows(IllegalStateException.class, () -> LifeFactUpdater.update(model, event,
			new LifeFactUpdater.Edit(true, null, false, null)));
		Assertions.assertThrows(IllegalStateException.class, () -> LifeFactUpdater.update(model, event,
			new LifeFactUpdater.Edit(false, null, true, null)));
		Assertions.assertEquals("1850", dateOf(event));
	}


	private void addOccupation(final String value){
		QuickEntryService.addOccupation(model, personId, value);
	}

	@Test
	void theOccupationsOfAPersonAreFound(){
		Assertions.assertTrue(LifeFactUpdater.occupations(model, personId).isEmpty());

		addOccupation("blacksmith");
		addOccupation("farmer");

		Assertions.assertEquals(2, LifeFactUpdater.occupations(model, personId).size());
		Assertions.assertTrue(LifeFactUpdater.occupations(model, "I404").isEmpty());
		Assertions.assertTrue(LifeFactUpdater.occupations(model, null).isEmpty());
		//the sex is an attribute of the person, not an occupation
		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).stream()
			.filter(attribute -> "sex".equals(FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_TYPE)))
			.count());
	}

	@Test
	void theOccupationIsAddedWhenThereIsNoneAndPutBackByTheUndo(){
		final int before = model.getRecordCount();

		final LifeFactUpdater.Applied applied = LifeFactUpdater.setOccupation(model, personId, "blacksmith");

		Assertions.assertEquals(1, applied.addedIds().size());
		Assertions.assertEquals("blacksmith", FLEFRecordHelper.getChildValue(LifeFactUpdater.occupations(model, personId).getFirst(),
			IndividualAttributeReader.TAG_VALUE));

		applied.undo().run();
		Assertions.assertEquals(before, model.getRecordCount());
		Assertions.assertSame(LifeFactUpdater.Applied.NOTHING, LifeFactUpdater.setOccupation(model, personId, "  "));
	}

	@Test
	void theOccupationIsChangedInPlaceAndPutBackByTheUndo(){
		addOccupation("blacksmith");
		final FLEFRecord attribute = LifeFactUpdater.occupations(model, personId).getFirst();
		final int records = model.getRecordCount();

		final LifeFactUpdater.Applied applied = LifeFactUpdater.setOccupation(model, personId, "farmer");

		Assertions.assertEquals("farmer", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));
		Assertions.assertEquals(records, model.getRecordCount());
		Assertions.assertNotNull(FLEFRecordHelper.findChild(FLEFRecordHelper.findChild(attribute, "audit"), "update"));

		applied.undo().run();
		Assertions.assertEquals("blacksmith", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));
		Assertions.assertSame(LifeFactUpdater.Applied.NOTHING, LifeFactUpdater.setOccupation(model, personId, " blacksmith "));
	}

	@Test
	void aBlankOccupationRemovesTheAttributeAndTheUndoPutsItBack(){
		addOccupation("blacksmith");
		final FLEFRecord attribute = LifeFactUpdater.occupations(model, personId).getFirst();

		final LifeFactUpdater.Applied applied = LifeFactUpdater.setOccupation(model, personId, "");

		Assertions.assertTrue(LifeFactUpdater.occupations(model, personId).isEmpty());
		Assertions.assertFalse(model.hasRecord(attribute.getId()));

		applied.undo().run();
		Assertions.assertTrue(model.hasRecord(attribute.getId()));
		Assertions.assertEquals(1, LifeFactUpdater.occupations(model, personId).size());
	}

	@Test
	void severalOccupationsAndAnOccupationWithDetailsAreLeftToTheAttributesTab(){
		addOccupation("blacksmith");
		final FLEFRecord attribute = LifeFactUpdater.occupations(model, personId).getFirst();
		Assertions.assertTrue(LifeFactUpdater.isOccupationEditable(attribute));
		attribute.addChild(FLEFRecord.createChildWithTagAndValue("valid_from", "x"));
		Assertions.assertFalse(LifeFactUpdater.isOccupationEditable(attribute));
		Assertions.assertThrows(IllegalStateException.class, () -> LifeFactUpdater.setOccupation(model, personId, "farmer"));
		Assertions.assertEquals("blacksmith", FLEFRecordHelper.getChildValue(attribute, IndividualAttributeReader.TAG_VALUE));

		addOccupation("farmer");
		Assertions.assertThrows(IllegalStateException.class, () -> LifeFactUpdater.setOccupation(model, personId, "miller"));
	}

	@Test
	void theSexAttributeIsChangedInPlaceAndPutBackByTheUndo(){
		final LifeFactUpdater.Applied applied = LifeFactUpdater.setSex(model, personId, SexType.FEMALE);

		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(model.getRecordById(personId), model));
		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());

		applied.undo().run();
		Assertions.assertEquals(SexType.MALE, PreferredSex.of(model.getRecordById(personId), model));
	}

	@Test
	void anUnknownSexKeepsTheAttributeWithTheValueUnknown(){
		LifeFactUpdater.setSex(model, personId, SexType.UNKNOWN);

		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(model.getRecordById(personId), model));
	}

	@Test
	void theSexIsAddedWhenThereIsNoneAndNothingIsAddedForTheUnknownOne(){
		final QuickEntryService.Created other = QuickEntryService.createIndividual(model, "Maria Rossi", null, null, null);

		Assertions.assertSame(LifeFactUpdater.Applied.NOTHING, LifeFactUpdater.setSex(model, other.individualId(), SexType.UNKNOWN));
		final LifeFactUpdater.Applied applied = LifeFactUpdater.setSex(model, other.individualId(), SexType.FEMALE);

		Assertions.assertEquals(SexType.FEMALE, PreferredSex.of(model.getRecordById(other.individualId()), model));
		applied.undo().run();
		Assertions.assertEquals(SexType.UNKNOWN, PreferredSex.of(model.getRecordById(other.individualId()), model));
		Assertions.assertEquals(1, model.getRecordsByType(IndividualAttributeReader.TYPE).size());
	}

	@Test
	void theSameSexChangesNothingAndSeveralSexAttributesAreForTheUserToSettle(){
		Assertions.assertSame(LifeFactUpdater.Applied.NOTHING, LifeFactUpdater.setSex(model, personId, SexType.MALE));

		model.addRecord(FLEFRecord.createMainRecord(IndividualAttributeReader.TYPE, IndividualAttributeReader.ID_PREFIX, model)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, personId))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, "female")));

		Assertions.assertThrows(IllegalStateException.class, () -> LifeFactUpdater.setSex(model, personId, SexType.FEMALE));
	}

}
