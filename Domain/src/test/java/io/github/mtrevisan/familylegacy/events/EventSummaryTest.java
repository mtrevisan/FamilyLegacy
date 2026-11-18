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
package io.github.mtrevisan.familylegacy.events;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.search.SearchCriteria;
import io.github.mtrevisan.familylegacy.search.SearchMode;
import io.github.mtrevisan.familylegacy.search.strategies.EventParticipationSearchStrategy;
import io.github.mtrevisan.familylegacy.search.strategies.EventSearchStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;


class EventSummaryTest{

	private FLEFModel model;


	@BeforeEach
	void buildModel(){
		EventSummary.invalidate();

		model = new FLEFModel();
		//Borgo is part of Treviso, that is part of Veneto
		model.addRecord(place("P1", "Borgo"));
		model.addRecord(place("P2", "Treviso"));
		model.addRecord(place("P3", "Veneto"));
		model.addRecord(relationship("PR1", "P1", "P2"));
		model.addRecord(relationship("PR2", "P2", "P3"));

		model.addRecord(FLEFRecord.createMainRecord("E1", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_BIRTH))
			.addChild(point("date", "2 MAR 1850"))
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, "P1")))
			.addChild(FLEFRecord.createChildWithTagAndValue("description", "Baptised at home")));
		model.addRecord(participation("EP1", "E1", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I1", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		model.addRecord(participation("EP2", "E1", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I2", EventParticipationReader.ENUM_ROLE_WITNESS));

		//no date, no place, no participants
		model.addRecord(FLEFRecord.createMainRecord("E2", EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_DEATH)));
	}


	private static FLEFRecord place(final String id, final String name){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue("value", name)));
	}

	private static FLEFRecord relationship(final String id, final String subject, final String target){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag("subject")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, subject)))
			.addChild(FLEFRecord.createChildWithTag("target")
				.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, target)));
	}

	private static FLEFRecord participation(final String id, final String eventId, final String entityTag, final String entityId,
			final String role){
		return FLEFRecord.createMainRecord(id, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(entityTag, entityId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
	}

	private static FLEFRecord point(final String tag, final String fullDate){
		return FLEFRecord.createChildWithTag(tag)
			.addChild(FLEFRecord.createChildWithTag("value")
				.addChild(FLEFRecord.createChildWithTag("point")
					.addChild(FLEFRecord.createChildWithTag("value")
						.addChild(FLEFRecord.createChildWithTagAndValue("full_date", fullDate)))
					.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian"))));
	}


	@Test
	void theDateOfAnEventIsNotEmpty(){
		final String date = EventHelper.eventDateRaw(model.getRecordById("E1"));

		Assertions.assertNotNull(date);
		Assertions.assertTrue(date.contains("1850"), date);
		Assertions.assertNull(EventHelper.eventDateRaw(model.getRecordById("E2")));
	}

	@Test
	void thePlaceIsTheCompleteHierarchy(){
		Assertions.assertEquals("Borgo, Treviso, Veneto", EventSummary.placeName(model.getRecordById("E1"), model));
		Assertions.assertNull(EventSummary.placeName(model.getRecordById("E2"), model));
	}

	@Test
	void aCycleInThePlacesIsCut(){
		model.addRecord(relationship("PR3", "P3", "P1"));
		EventSummary.invalidate();

		Assertions.assertEquals("Borgo, Treviso, Veneto", EventSummary.placeName(model.getRecordById("E1"), model));
	}

	@Test
	void thePrincipalParticipantsComeFirstAndTheOthersAreCounted(){
		model.addRecord(participation("EP3", "E1", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I3", EventParticipationReader.ENUM_ROLE_WITNESS));
		model.addRecord(participation("EP4", "E1", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I4", EventParticipationReader.ENUM_ROLE_WITNESS));
		model.addRecord(participation("EP5", "E1", EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, "I5", EventParticipationReader.ENUM_ROLE_WITNESS));
		EventSummary.invalidate();

		final String names = EventSummary.participantNames(model.getRecordById("E1"), model);

		//the principal first, then the first two witnesses, and the other two are only counted
		Assertions.assertEquals("I1, I2, I3 +2", names);
		Assertions.assertNull(EventSummary.participantNames(model.getRecordById("E2"), model));
	}

	@Test
	void theDescriptionHasEveryPart(){
		final String text = EventSummary.describe(model.getRecordById("E1"), model);

		Assertions.assertTrue(text.startsWith("birth ("), text);
		Assertions.assertTrue(text.contains("1850"), text);
		Assertions.assertTrue(text.contains("Borgo, Treviso, Veneto"), text);
		Assertions.assertTrue(text.contains("I1"), text);
		Assertions.assertTrue(text.contains("Baptised at home"), text);
		Assertions.assertTrue(text.endsWith("[E1]"), text);
		Assertions.assertEquals("death [E2]", EventSummary.describe(model.getRecordById("E2"), model));
	}

	@Test
	void theDescriptionForAParticipationDoesNotRepeatTheParticipants(){
		final String text = EventSummary.describeWithoutParticipants(model.getRecordById("E1"), model);

		Assertions.assertFalse(text.contains("I1"), text);
		Assertions.assertTrue(text.contains("Borgo, Treviso, Veneto"), text);
		Assertions.assertTrue(text.endsWith("[E1]"), text);
	}

	@Test
	void aParticipationIsFilteredByRoleEventTypeAndTypeOfParticipant(){
		model.addRecord(participation("EP3", "E2", EventParticipationReader.TAG_PARTICIPANT_GROUP, "G1", EventParticipationReader.ENUM_ROLE_PRINCIPAL));
		final EventParticipationSearchStrategy strategy = new EventParticipationSearchStrategy();
		final FLEFRecord principalOfBirth = model.getRecordById("EP1");
		final FLEFRecord witnessOfBirth = model.getRecordById("EP2");
		final FLEFRecord groupOfDeath = model.getRecordById("EP3");

		final SearchCriteria witnesses = criteria().withFilter(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_WITNESS);
		Assertions.assertFalse(strategy.buildPredicate(witnesses, model).test(principalOfBirth));
		Assertions.assertTrue(strategy.buildPredicate(witnesses, model).test(witnessOfBirth));

		final SearchCriteria deaths = criteria().withFilter(EventReader.TAG_TYPE, EventReader.ENUM_TYPE_DEATH);
		Assertions.assertFalse(strategy.buildPredicate(deaths, model).test(principalOfBirth));
		Assertions.assertTrue(strategy.buildPredicate(deaths, model).test(groupOfDeath));

		final SearchCriteria groups = criteria().withFilter(EventParticipationSearchStrategy.FILTER_PARTICIPANT_TYPE, EventParticipationReader.TAG_PARTICIPANT_GROUP);
		Assertions.assertFalse(strategy.buildPredicate(groups, model).test(principalOfBirth));
		Assertions.assertTrue(strategy.buildPredicate(groups, model).test(groupOfDeath));

		//no filter: every participation
		Assertions.assertTrue(strategy.buildPredicate(criteria(), model).test(witnessOfBirth));
	}

	private static SearchCriteria criteria(){
		return new SearchCriteria(EventParticipationReader.TYPE, "", EnumSet.noneOf(SearchMode.class));
	}

	@Test
	void anEventIsFoundByTheNameOfAPlaceThatContainsItsPlace(){
		final EventSearchStrategy strategy = new EventSearchStrategy();
		final FLEFRecord event = model.getRecordById("E1");
		final FLEFRecord noPlace = model.getRecordById("E2");

		final SearchCriteria region = new SearchCriteria(EventReader.TYPE, "", EnumSet.noneOf(SearchMode.class))
			.withFilter(EventReader.TAG_PLACE, "Veneto");
		Assertions.assertTrue(strategy.buildPredicate(region, model).test(event));
		Assertions.assertFalse(strategy.buildPredicate(region, model).test(noPlace));

		final SearchCriteria other = new SearchCriteria(EventReader.TYPE, "", EnumSet.noneOf(SearchMode.class))
			.withFilter(EventReader.TAG_PLACE, "Lombardia");
		Assertions.assertFalse(strategy.buildPredicate(other, model).test(event));
	}

}
