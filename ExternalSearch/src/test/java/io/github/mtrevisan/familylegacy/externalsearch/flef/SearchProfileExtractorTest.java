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

package io.github.mtrevisan.familylegacy.externalsearch.flef;

import io.github.mtrevisan.familylegacy.events.EventSummary;
import io.github.mtrevisan.familylegacy.externalsearch.Placeholder;
import io.github.mtrevisan.familylegacy.externalsearch.SearchSubject;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.PlaceRelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.TextValueVariantReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DatePrecision;
import io.github.mtrevisan.familylegacy.io.model.readers.date.NormalizedDate;
import io.github.mtrevisan.familylegacy.io.model.readers.date.TemporalSpan;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.privacy.PrivacyService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.JulianFields;
import java.util.List;


/** What an individual gives to a search on an external site: every name, the years, the places, the relatives. */
class SearchProfileExtractorTest{

	private FLEFModel model;
	private int sequence;


	/**
	 * Giovanni Battista Rossi (I1) was born in 1805 in the church of San Nicolò, in Treviso, in Italia, and died in 1870 in a place
	 * written only as text; he has a birth name, a religious name with a Latin form, and a married name with no parts; his father is
	 * Antonio (I2), his mother Maria Bianchi (I3), his wife Anna Verdi (I4). Luca Rossi (I5) was born in 1990: he may be living.
	 */
	@BeforeEach
	void buildModel(){
		EventSummary.invalidate();
		PrivacyService.invalidate();
		model = new FLEFModel();
		sequence = 0;

		final FLEFRecord giovanni = FLEFRecord.createMainRecord("I1", IndividualReader.TYPE)
			.addChild(name(NameReader.ENUM_PERSONAL_TYPE_BIRTH, part(NameReader.ENUM_PART_TYPE_GIVEN, "Giovanni", null),
				part(NameReader.ENUM_PART_TYPE_GIVEN, "Battista", null), part(NameReader.ENUM_PART_TYPE_FAMILY, "Rossi", null),
				part(NameReader.ENUM_PART_TYPE_NICKNAME, "Nane", null)))
			.addChild(name(NameReader.ENUM_PERSONAL_TYPE_RELIGIOUS, part(NameReader.ENUM_PART_TYPE_GIVEN, "Giovanni", "Joannes"),
				part(NameReader.ENUM_PART_TYPE_FAMILY_PREFIX, "de", null), part(NameReader.ENUM_PART_TYPE_FAMILY, "Rubeis", null)))
			//the same as the first one: not offered twice
			.addChild(name(NameReader.ENUM_PERSONAL_TYPE_OFFICIAL, part(NameReader.ENUM_PART_TYPE_GIVEN, "Giovanni", null),
				part(NameReader.ENUM_PART_TYPE_GIVEN, "Battista", null), part(NameReader.ENUM_PART_TYPE_FAMILY, "Rossi", null),
				part(NameReader.ENUM_PART_TYPE_NICKNAME, "Nane", null)));
		model.addRecord(giovanni);
		model.addRecord(person("I2", "Antonio", "Rossi"));
		model.addRecord(person("I3", "Maria", "Bianchi"));
		model.addRecord(person("I4", "Anna", "Verdi"));
		model.addRecord(person("I5", "Luca", "Rossi"));
		model.addRecord(attribute("A1", "I1", "male"));
		model.addRecord(attribute("A2", "I2", "male"));
		model.addRecord(attribute("A3", "I3", "female"));

		model.addRecord(place("P1", "Chiesa di San Nicolò", PlaceReader.ENUM_TYPE_BUILDING));
		model.addRecord(place("P2", "Treviso", PlaceReader.ENUM_TYPE_CITY));
		model.addRecord(place("P3", "Italia", PlaceReader.ENUM_TYPE_COUNTRY));
		model.addRecord(partOf("PR1", "P1", "P2"));
		model.addRecord(partOf("PR2", "P2", "P3"));

		event("I1", EventReader.ENUM_TYPE_BIRTH, "1805-03-01").addChild(placeReference("P1"));
		event("I1", EventReader.ENUM_TYPE_DEATH, "1870-05-02")
			.addChild(FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
				.addChild(FLEFRecord.createChildWithTagAndValue("original_text", "Venezia, Veneto, Italy")));
		event("I2", EventReader.ENUM_TYPE_DEATH, "1840-01-01");
		event("I3", EventReader.ENUM_TYPE_DEATH, "1850-01-01");
		event("I4", EventReader.ENUM_TYPE_DEATH, "1880-01-01");
		event("I5", EventReader.ENUM_TYPE_BIRTH, "1990-01-01");

		model.addRecord(relationship("R1", "I1", "I2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(relationship("R2", "I1", "I3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		model.addRecord(relationship("R3", "I1", "I4", RelationshipReader.ENUM_TYPE_SPOUSE));
		model.addRecord(relationship("R4", "I5", "I1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
	}


	@Test
	void everyNameAndItsTranscriptionsAreOfferedOnce(){
		final SearchProfile profile = SearchProfileExtractor.extract(model, model.getRecordById("I1"));

		final List<NameCandidate> names = profile.names();
		Assertions.assertEquals(3, names.size(), names.toString());
		Assertions.assertEquals("Giovanni Battista", names.get(0).given());
		Assertions.assertEquals("Rossi", names.get(0).surname());
		//a nickname is not part of the name searched
		Assertions.assertEquals("Giovanni Battista Rossi", names.get(0).full());
		//the prefix is part of the surname
		Assertions.assertEquals("Giovanni", names.get(1).given());
		Assertions.assertEquals("de Rubeis", names.get(1).surname());
		Assertions.assertEquals("Joannes", names.get(2).given());
		Assertions.assertEquals("de Rubeis", names.get(2).surname());
		Assertions.assertTrue(names.get(2).label().contains("latinized"), names.get(2).label());
	}

	@Test
	void theYearsThePlacesAndTheRelativesAreTaken(){
		final SearchProfile profile = SearchProfileExtractor.extract(model, model.getRecordById("I1"));
		final SearchSubject facts = profile.facts();

		Assertions.assertEquals("male", facts.value(Placeholder.SEX));
		Assertions.assertEquals("1805", facts.value(Placeholder.BIRTH_YEAR));
		Assertions.assertEquals("1803", facts.value(Placeholder.BIRTH_YEAR_FROM));
		Assertions.assertEquals("1807", facts.value(Placeholder.BIRTH_YEAR_TO));
		//the church is in the town: the search is about the town
		Assertions.assertEquals("Treviso", facts.value(Placeholder.BIRTH_PLACE));
		Assertions.assertEquals("Chiesa di San Nicolò, Treviso, Italia", facts.value(Placeholder.BIRTH_PLACE_FULL));
		Assertions.assertEquals("IT", facts.value(Placeholder.BIRTH_COUNTRY));
		Assertions.assertEquals("1870", facts.value(Placeholder.DEATH_YEAR));
		Assertions.assertEquals("Venezia", facts.value(Placeholder.DEATH_PLACE));
		Assertions.assertEquals("IT", facts.value(Placeholder.DEATH_COUNTRY));
		Assertions.assertEquals("Antonio", facts.value(Placeholder.FATHER_GIVEN));
		Assertions.assertEquals("Rossi", facts.value(Placeholder.FATHER_SURNAME));
		Assertions.assertEquals("Maria", facts.value(Placeholder.MOTHER_GIVEN));
		Assertions.assertEquals("Bianchi", facts.value(Placeholder.MOTHER_SURNAME));
		Assertions.assertEquals("Anna", facts.value(Placeholder.SPOUSE_GIVEN));
		Assertions.assertEquals("Verdi", facts.value(Placeholder.SPOUSE_SURNAME));
		Assertions.assertFalse(facts.hasName(), "the name is chosen apart");
		Assertions.assertFalse(profile.needsConsent());

		final SearchSubject subject = profile.subjectFor(profile.names().get(2));
		Assertions.assertEquals("Joannes", subject.value(Placeholder.NAME_GIVEN));
		Assertions.assertEquals("Treviso", subject.value(Placeholder.BIRTH_PLACE));
	}

	@Test
	void aPersonThatMayBeLivingNeedsConsentAndIsNotARelativeSent(){
		final SearchProfile living = SearchProfileExtractor.extract(model, model.getRecordById("I5"));
		Assertions.assertTrue(living.mayBeLiving());
		Assertions.assertTrue(living.needsConsent());

		Assertions.assertThrows(IllegalArgumentException.class, () -> SearchProfileExtractor.extract(model, model.getRecordById("P1")));
	}

	@Test
	void theWindowOfAYearFollowsTheUncertaintyOfTheDate(){
		Assertions.assertArrayEquals(new int[]{1805, 1803, 1807},
			SearchProfileExtractor.years(TemporalSpan.point(NormalizedDate.exact(jdn(1805, 3, 1), DatePrecision.DAY))));
		Assertions.assertArrayEquals(new int[]{1805, 1800, 1810},
			SearchProfileExtractor.years(TemporalSpan.point(NormalizedDate.approximated(jdn(1805, 1, 1), DatePrecision.YEAR,
				"gregorian", null, null))));
		Assertions.assertArrayEquals(new int[]{1800, 1800, 1809},
			SearchProfileExtractor.years(TemporalSpan.point(NormalizedDate.exact(jdn(1800, 1, 1), DatePrecision.DECADE))));
		final int[] bounded = SearchProfileExtractor.years(TemporalSpan.bounded(NormalizedDate.exact(jdn(1815, 1, 1), DatePrecision.YEAR),
			NormalizedDate.exact(jdn(1820, 12, 31), DatePrecision.YEAR)));
		Assertions.assertEquals(1815, bounded[1]);
		Assertions.assertEquals(1820, bounded[2]);
		final int[] before = SearchProfileExtractor.years(TemporalSpan.bounded(null, NormalizedDate.exact(jdn(1820, 1, 1), DatePrecision.YEAR)));
		Assertions.assertArrayEquals(new int[]{1820, 1810, 1820}, before);
		Assertions.assertNull(SearchProfileExtractor.years(null));
	}


	private static long jdn(final int year, final int month, final int day){
		return LocalDate.of(year, month, day).getLong(JulianFields.JULIAN_DAY);
	}

	private static FLEFRecord part(final String type, final String value, final String latin){
		final FLEFRecord part = FLEFRecord.createChildWithTag(NameReader.TAG_PART)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_TYPE, type))
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_PART_VALUE, value));
		if(latin != null)
			part.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART_VARIANT)
				.addChild(FLEFRecord.createChildWithTag(TextValueVariantReader.TAG_TRANSCRIPTION)
					.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_TRANSCRIPTION_TYPE,
						TextValueVariantReader.ENUM_TRANSCRIPTION_TYPE_LATINIZED))
					.addChild(FLEFRecord.createChildWithTagAndValue(TextValueVariantReader.TAG_TRANSCRIPTION_VALUE, latin))));
		return part;
	}

	private static FLEFRecord name(final String type, final FLEFRecord... parts){
		final FLEFRecord name = FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
			.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, type));
		for(final FLEFRecord part : parts)
			name.addChild(part);
		return name;
	}

	private static FLEFRecord person(final String id, final String given, final String family){
		return FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(name(NameReader.ENUM_PERSONAL_TYPE_BIRTH, part(NameReader.ENUM_PART_TYPE_GIVEN, given, null),
				part(NameReader.ENUM_PART_TYPE_FAMILY, family, null)));
	}

	private static FLEFRecord attribute(final String id, final String personId, final String sex){
		return FLEFRecord.createMainRecord(id, IndividualAttributeReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TYPE.toLowerCase(), personId))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, sex));
	}

	private static FLEFRecord place(final String id, final String name, final String type){
		return FLEFRecord.createMainRecord(id, PlaceReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_NAME_VALUE, name)))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceReader.TAG_TYPE, type));
	}

	private static FLEFRecord partOf(final String id, final String childId, final String parentId){
		return FLEFRecord.createMainRecord(id, PlaceRelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, childId)))
			.addChild(FLEFRecord.createChildWithTag(PlaceRelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_PLACE, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(PlaceRelationshipReader.TAG_TYPE,
				PlaceRelationshipReader.ENUM_TYPE_ADMINISTRATIVE_PART_OF));
	}

	private static FLEFRecord placeReference(final String placeId){
		return FLEFRecord.createChildWithTag(EventReader.TAG_PLACE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_PLACE, placeId));
	}

	private FLEFRecord event(final String personId, final String type, final String date){
		sequence ++;
		final FLEFRecord event = FLEFRecord.createMainRecord("E" + sequence, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag("date")
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", date)))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
		return event;
	}

	private static FLEFRecord relationship(final String id, final String subjectId, final String targetId, final String type){
		return FLEFRecord.createMainRecord(id, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, subjectId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
	}

}
