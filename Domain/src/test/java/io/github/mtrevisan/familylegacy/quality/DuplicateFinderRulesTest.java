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
package io.github.mtrevisan.familylegacy.quality;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


/** What the duplicate finder knows that two records of one person cannot be, and what it takes points away for. */
class DuplicateFinderRulesTest{

	private FLEFModel model;
	private int sequence;


	@BeforeEach
	void buildModel(){
		model = new FLEFModel();
		sequence = 0;
	}


	private void person(final String id, final String given, final String family, final Integer birthYear){
		person(id, given, family, birthYear, IndividualReader.ENUM_SEX_MALE);
	}

	/** @param sex the sex, or {@code null} for an individual of unknown sex. */
	private void person(final String id, final String given, final String family, final Integer birthYear, final String sex){
		model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME)
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", given)))
				.addChild(FLEFRecord.createChildWithTag(NameReader.TAG_PART)
					.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_FAMILY))
					.addChild(FLEFRecord.createChildWithTagAndValue("value", family)))));
		if(sex != null)
			model.addRecord(FLEFRecord.createMainRecord("IA" + id, IndividualAttributeReader.TYPE)
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_INDIVIDUAL, id))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_TYPE, IndividualAttributeReader.ENUM_TYPE_SEX))
				.addChild(FLEFRecord.createChildWithTagAndValue(IndividualAttributeReader.TAG_VALUE, sex)));
		if(birthYear != null)
			event(id, EventReader.ENUM_TYPE_BIRTH, birthYear);
	}

	private void event(final String personId, final String type, final int year){
		sequence ++;
		final FLEFRecord event = FLEFRecord.createMainRecord("E" + sequence, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(EventReader.TAG_TYPE, type))
			.addChild(FLEFRecord.createChildWithTag("date")
				.addChild(FLEFRecord.createChildWithTag("value")
					.addChild(FLEFRecord.createChildWithTag("point")
						.addChild(FLEFRecord.createChildWithTag("value")
							.addChild(FLEFRecord.createChildWithTagAndValue("full_date", year + "-06-15")))
						.addChild(FLEFRecord.createChildWithTagAndValue("calendar", "gregorian")))));
		model.addRecord(event);
		model.addRecord(FLEFRecord.createMainRecord("EP" + sequence, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT)
				.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, event.getId()))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, EventParticipationReader.ENUM_ROLE_PRINCIPAL)));
	}

	private void childOf(final String childId, final String parentId){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("R" + sequence, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, childId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, parentId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)));
	}

	private void spouses(final String first, final String second){
		sequence ++;
		model.addRecord(FLEFRecord.createMainRecord("R" + sequence, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, first)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET)
				.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_INDIVIDUAL, second)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_SPOUSE)));
	}

	/** The pairs the finder gives, with the threshold at zero so that the penalties show too. */
	private List<DuplicateFinderService.Candidate> find(){
		final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
		options.threshold = 0;
		options.requireSameSex = false;
		return DuplicateFinderService.find(model, options);
	}

	private DuplicateFinderService.Candidate pair(final String first, final String second){
		return find().stream()
			.filter(candidate -> List.of(candidate.left().id(), candidate.right().id()).containsAll(List.of(first, second)))
			.findFirst()
			.orElse(null);
	}


	@Test
	void twoRecordsOfTheSameNameAndYearAreCandidates(){
		person("I1", "Giovanni", "Masutti", 1850);
		person("I2", "Giovanni", "Masutti", 1851);

		final DuplicateFinderService.Candidate candidate = pair("I1", "I2");

		Assertions.assertNotNull(candidate);
		Assertions.assertTrue(candidate.score() > 0, "" + candidate.score());
	}

	@Test
	void aParentAndAChildAreNeverTheSamePerson(){
		person("I1", "Giovanni", "Masutti", 1820);
		person("I2", "Giovanni", "Masutti", 1850);
		childOf("I2", "I1");

		Assertions.assertNull(pair("I1", "I2"));
	}

	@Test
	void twoSpousesAreNeverTheSamePerson(){
		person("I1", "Maria", "Rossi", 1850);
		person("I2", "Maria", "Rossi", 1851);
		spouses("I1", "I2");

		Assertions.assertNull(pair("I1", "I2"));
	}

	@Test
	void aGrandfatherAndAGrandsonOfTheSameNameAreTwoPeople(){
		person("I1", "Giovanni", "Masutti", 1790);
		person("I2", "Antonio", "Masutti", 1820);
		person("I3", "Giovanni", "Masutti", 1850);
		childOf("I2", "I1");
		childOf("I3", "I2");

		Assertions.assertNull(pair("I1", "I3"));
	}

	@Test
	void twoChildrenOfTheSameParentsWithDifferentGivenNamesAreSiblings(){
		person("I1", "Antonio", "Masutti", 1820);
		person("I2", "Domenico", "Trevisan", 1850);
		person("I3", "Valentino", "Trevisan", 1851);
		childOf("I2", "I1");
		childOf("I3", "I1");

		Assertions.assertNull(pair("I2", "I3"));
	}

	@Test
	void theSameGivenNameGivenToTwoSiblingsYearsApartTakesPointsAway(){
		person("I1", "Antonio", "Masutti", 1790);
		person("I2", "Beatrice", "Trevisan", 1820);
		person("I3", "Beatrice", "Trevisan", 1821);
		childOf("I2", "I1");
		childOf("I3", "I1");
		final int close = pair("I2", "I3").score();

		//the same pair with the second one born six years after the first: the first one died, the name was given again
		model = new FLEFModel();
		person("I1", "Antonio", "Masutti", 1790);
		person("I2", "Beatrice", "Trevisan", 1820);
		person("I3", "Beatrice", "Trevisan", 1826);
		childOf("I2", "I1");
		childOf("I3", "I1");
		final DuplicateFinderService.Candidate apart = pair("I2", "I3");

		Assertions.assertTrue(apart == null || apart.score() < close - 25, close + " / " + (apart != null? apart.score(): null));
		if(apart != null)
			Assertions.assertTrue(apart.differences().stream().anyMatch(text -> text.contains("named after")), apart.differences().toString());
	}

	@Test
	void birthYearsFarApartTakePointsAway(){
		person("I1", "Giovanni", "Masutti", 1850);
		person("I2", "Giovanni", "Masutti", 1851);
		person("I3", "Giovanni", "Masutti", 1900);

		final DuplicateFinderService.Candidate near = pair("I1", "I2");
		final DuplicateFinderService.Candidate far = pair("I1", "I3");

		Assertions.assertTrue(far == null || far.score() < near.score() - 40, near.score() + " / " + (far != null? far.score(): null));
	}

	@Test
	void aGivenNameThatDoesNotAgreeTakesPointsAway(){
		person("I1", "Giovanni", "Masutti", 1850);
		person("I2", "Giovanni", "Masutti", 1851);
		person("I3", "Costante", "Masutti", 1851);

		final DuplicateFinderService.Candidate same = pair("I1", "I2");
		final DuplicateFinderService.Candidate other = pair("I1", "I3");

		Assertions.assertTrue(other == null || other.score() < same.score() - 30, same.score() + " / " + (other != null? other.score(): null));
	}

	@Test
	void aCommonNameIsWorthLessThanARareOne(){
		//sixty Giovanni Rossi make the name common; two Ermenegildo Quaglia make it rare
		for(int i = 0; i < 60; i ++)
			person("C" + i, "Giovanni", "Rossi", 1700 + i * 3);
		person("I1", "Giovanni", "Rossi", 1950);
		person("I2", "Giovanni", "Rossi", 1950);
		person("I3", "Ermenegildo", "Quaglia", 1950);
		person("I4", "Ermenegildo", "Quaglia", 1950);

		final DuplicateFinderService.Candidate common = pair("I1", "I2");
		final DuplicateFinderService.Candidate rare = pair("I3", "I4");

		Assertions.assertNotNull(common);
		Assertions.assertNotNull(rare);
		Assertions.assertTrue(rare.score() > common.score(), rare.score() + " / " + common.score());
		Assertions.assertTrue(common.matches().getFirst().detail().contains("×"), common.matches().getFirst().detail());
	}

	@Test
	void aFileTooSmallToTellHowCommonANameIsWeighsTheNameAsItIs(){
		person("I1", "Giovanni", "Rossi", 1950);
		person("I2", "Giovanni", "Rossi", 1950);
		person("I3", "Ermenegildo", "Quaglia", 1950);
		person("I4", "Ermenegildo", "Quaglia", 1950);

		Assertions.assertEquals(pair("I3", "I4").score(), pair("I1", "I2").score());
	}

	@Test
	void anIndividualOfUnknownSexIsComparedWithOneOfKnownSexButKnownSexesStayApart(){
		person("I1", "Giovanni", "Masutti", 1850, IndividualReader.ENUM_SEX_MALE);
		person("I2", "Giovanni", "Masutti", 1850, null);
		person("I3", "Giovanni", "Masutti", 1850, IndividualReader.ENUM_SEX_FEMALE);
		final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
		options.threshold = 0;
		options.requireSameSex = true;

		final List<DuplicateFinderService.Candidate> candidates = DuplicateFinderService.find(model, options);

		Assertions.assertTrue(candidates.stream().anyMatch(c -> List.of(c.left().id(), c.right().id()).containsAll(List.of("I1", "I2"))));
		Assertions.assertTrue(candidates.stream().anyMatch(c -> List.of(c.left().id(), c.right().id()).containsAll(List.of("I2", "I3"))));
		Assertions.assertTrue(candidates.stream().noneMatch(c -> List.of(c.left().id(), c.right().id()).containsAll(List.of("I1", "I3"))));
	}

}
