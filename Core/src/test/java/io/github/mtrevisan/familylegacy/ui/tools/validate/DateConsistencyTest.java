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
package io.github.mtrevisan.familylegacy.ui.tools.validate;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.NameReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateNormalizer;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.quality.ConsistencyService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/**
 * The dates that cannot be right, whatever the day: a date is the days it may stand for (an approximate one has a margin, a decade is ten
 * years, a century is a hundred, a bounded one lies between two days), and a problem is reported only if it holds for every one of them.
 */
class DateConsistencyTest{

	private static final Set<String> KINDS = Set.of("death_before_birth", "event_before_birth", "event_after_death", "child_before_parent",
		"born_after_mother_death", "born_after_father_death", "union_before_birth", "union_after_death", "membership_before_birth",
		"membership_after_death", "membership_ended_before_birth");

	private int counter;
	private final FLEFModel model = new FLEFModel();


	/* ---- the dates: exact, approximate, decade, century, bounded ---- */

	private static FLEFRecord point(final FLEFRecord value, final String margin, final boolean approximate){
		final FLEFRecord point = FLEFRecord.createChildWithTag(DateReader.TAG_POINT).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE).addChild(value));
		if(approximate){
			final FLEFRecord approx = FLEFRecord.createChildWithTag(DateReader.TAG_APPROXIMATE)
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_BASIS, "about"));
			if(margin != null)
				approx.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_MARGIN, margin));
			point.addChild(approx);
		}
		return point;
	}

	private static FLEFRecord date(final String tag, final FLEFRecord point){
		return FLEFRecord.createChildWithTag(tag).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE).addChild(point));
	}

	private static FLEFRecord exact(final String iso){
		return date(EventReader.TAG_DATE, point(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, iso), null, false));
	}

	private static FLEFRecord year(final int year){
		return exact(String.format("%04d", year));
	}

	/** About that day: two years either way unless a margin is given. */
	private static FLEFRecord about(final String iso, final String margin){
		return date(EventReader.TAG_DATE, point(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, iso), margin, true));
	}

	private static FLEFRecord decade(final int startYear){
		return date(EventReader.TAG_DATE, point(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_DECADE, Integer.toString(startYear)), null, false));
	}

	private static FLEFRecord century(final int ordinal){
		return date(EventReader.TAG_DATE, point(FLEFRecord.createChildWithTag(DateReader.TAG_CENTURY)
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_ORDINAL, Integer.toString(ordinal))), null, false));
	}

	private static FLEFRecord notAfter(final String iso){
		return FLEFRecord.createChildWithTag(EventReader.TAG_DATE).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE)
			.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_BOUNDED).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_NOT_AFTER)
				.addChild(FLEFRecord.createChildWithTag(DateReader.TAG_VALUE).addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_FULL_DATE, iso))))));
	}


	/* ---- the records ---- */

	private String person(final String id, final String sex){
		model.addRecord(FLEFRecord.createMainRecord(id, IndividualReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(IndividualReader.TAG_NAME).addChild(FLEFRecord.createChildWithTag(DateReader.TAG_PART)
				.addChild(FLEFRecord.createChildWithTagAndValue(NameReader.TAG_TYPE, NameReader.ENUM_PART_TYPE_GIVEN))
				.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_VALUE, "Name" + id))))
			.addChild(FLEFRecord.createChildWithTagAndValue(IndividualReader.TAG_SEX, sex)));
		return id;
	}

	/** An event of the type, with that date, in which the person has that role. */
	private void event(final String personId, final String type, final FLEFRecord date, final String role){
		counter ++;
		final String eventId = "E" + counter;
		model.addRecord(FLEFRecord.createMainRecord(eventId, EventReader.TYPE)
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type))
			.addChild(date));
		final FLEFRecord participation = FLEFRecord.createMainRecord("P" + counter, EventParticipationReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(EventParticipationReader.TAG_PARTICIPANT).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_EVENT, eventId));
		if(role != null)
			participation.addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_ROLE, role));
		model.addRecord(participation);
	}

	private void born(final String personId, final FLEFRecord date){
		event(personId, EventReader.ENUM_TYPE_BIRTH, date, EventParticipationReader.ENUM_ROLE_PRINCIPAL);
	}

	private void died(final String personId, final FLEFRecord date){
		event(personId, EventReader.ENUM_TYPE_DEATH, date, EventParticipationReader.ENUM_ROLE_PRINCIPAL);
	}

	private void link(final String type, final String subjectId, final String targetId, final FLEFRecord validFrom){
		counter ++;
		final FLEFRecord relationship = FLEFRecord.createMainRecord("R" + counter, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, subjectId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, targetId)))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, type));
		if(validFrom != null)
			relationship.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_VALID_FROM).addChild(validFrom.getChildren().getFirst()));
		model.addRecord(relationship);
	}

	private List<String> found(){
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> KINDS.contains(issue.kind()))
			.map(ConsistencyService.Issue::kind)
			.toList();
	}


	/* ---- a death before a birth ---- */

	@Test
	void aDeathCertainlyBeforeTheBirthIsAnError(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1840-01-01"));

		Assertions.assertEquals(List.of("death_before_birth"), found());
	}

	@Test
	void aDeathAfterTheBirthIsFine(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1900-01-01"));

		Assertions.assertTrue(found().isEmpty());
	}

	@Test
	void anApproximateDateIsAsWideAsItsMargin(){
		//about 1849 (two years either way when it says none) can be after the birth in 1850
		final String near = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(near, exact("1850-06-01"));
		died(near, about("1849-06-01", null));
		//about 1840 is two years either way: certainly before
		final String far = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(far, exact("1850-06-01"));
		died(far, about("1840-06-01", null));
		//about 1840, with a margin of twenty years, is not certainly before
		final String wide = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(wide, exact("1850-06-01"));
		died(wide, about("1840-06-01", "P20Y"));

		final List<String> all = found();

		Assertions.assertEquals(List.of("death_before_birth"), all, all.toString());
		//the one that is is the far one: the others have no error
		Assertions.assertTrue(ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.kind().equals("death_before_birth"))
			.allMatch(issue -> issue.recordId().equals("I2")));
	}

	@Test
	void aDecadeIsTenYearsNotTheFirstOne(){
		//died in the 1850s, born in 1855: it can be after
		final String overlapping = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(overlapping, exact("1855-03-01"));
		died(overlapping, decade(1850));
		//died in the 1840s, born in 1850: the whole decade is before
		final String before = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(before, exact("1850-01-01"));
		died(before, decade(1840));

		Assertions.assertEquals(List.of("death_before_birth"), found());
	}

	@Test
	void aCenturyIsAHundredYears(){
		//died in the 18th century, born in 1790: it can be after
		final String overlapping = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(overlapping, exact("1790-01-01"));
		died(overlapping, century(18));
		//died in the 17th century, born in 1790
		final String before = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(before, exact("1790-01-01"));
		died(before, century(17));
		//born and died in the 18th century: it cannot be said
		final String same = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(same, century(18));
		died(same, century(18));

		Assertions.assertEquals(List.of("death_before_birth"), found());
	}

	@Test
	void aDecadeAndACenturyAndAnApproximateDateTogetherOverlap(){
		final String one = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(one, decade(1850));
		died(one, decade(1850));
		final String two = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(two, century(19));
		died(two, about("1800-01-01", null));
		final String three = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(three, about("1850-01-01", null));
		died(three, about("1849-01-01", null));

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}

	@Test
	void aYearIsAWholeYear(){
		//born in 1850 and died in 1850: it can be after
		final String sameYear = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(sameYear, year(1850));
		died(sameYear, year(1850));
		//died in 1848, born in 1850
		final String before = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(before, year(1850));
		died(before, year(1848));
		//a year is 366 days whatever its length: the one before is not judged to be certainly before (it errs on the side of silence)
		final String adjacent = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(adjacent, year(1850));
		died(adjacent, year(1849));

		Assertions.assertEquals(List.of("death_before_birth"), found());
	}

	@Test
	void aBoundedDateWithOneEndOpenIsOnlyAsCertainAsItsEnd(){
		//died not after 1840, born in 1850: certainly before
		final String closed = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(closed, exact("1850-01-01"));
		died(closed, notAfter("1840-01-01"));
		//born not after 1860, died in 1855: the birth can be before it
		final String open = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(open, notAfter("1860-01-01"));
		died(open, exact("1855-01-01"));

		Assertions.assertEquals(List.of("death_before_birth"), found());
	}

	@Test
	void withSeveralBirthsTheDaysTheyAllCoverAreUsed(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		//two births, ten years apart: the death between them is not before the birth
		born(mario, exact("1850-01-01"));
		born(mario, exact("1860-01-01"));
		died(mario, exact("1855-01-01"));
		//a witness of a birth is not born in it: the death in 1800 is not before a birth, but the birth in 1850 is after the witness died
		final String witness = person("I2", IndividualReader.ENUM_SEX_MALE);
		died(witness, exact("1800-01-01"));
		event(witness, EventReader.ENUM_TYPE_BIRTH, exact("1850-01-01"), EventParticipationReader.ENUM_ROLE_WITNESS);

		Assertions.assertEquals(List.of("event_after_death"), found());
	}

	@Test
	void aDateThatCannotBeReadIsNotJudged(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("not a date"));
		//and one with no date at all
		final String anna = person("I2", IndividualReader.ENUM_SEX_FEMALE);
		born(anna, exact("1850-01-01"));
		event(anna, EventReader.ENUM_TYPE_DEATH, FLEFRecord.createChildWithTag(EventReader.TAG_DATE), EventParticipationReader.ENUM_ROLE_PRINCIPAL);

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}


	/* ---- the events ---- */

	@Test
	void anEventAfterTheDeathOrBeforeTheBirthIsAnErrorButTheEventsOfTheDeathAreNot(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1900-01-01"));
		event(mario, EventReader.ENUM_TYPE_MARRIAGE, exact("1910-01-01"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", exact("1840-01-01"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		//the burial, after the death, is the rule
		event(mario, EventReader.ENUM_TYPE_BURIAL, exact("1900-01-03"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		//an event in the decade of the death, or about it, cannot be said to be after
		event(mario, "residence", decade(1900), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", about("1901-01-01", null), EventParticipationReader.ENUM_ROLE_PRINCIPAL);

		Assertions.assertEquals(List.of("event_after_death", "event_before_birth"), found().stream().sorted().toList());
	}

	@Test
	void aWitnessOfAnEventAfterTheDeathIsAlsoAnError(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		died(mario, exact("1900-01-01"));
		event(mario, "baptism", exact("1910-01-01"), EventParticipationReader.ENUM_ROLE_WITNESS);

		Assertions.assertEquals(List.of("event_after_death"), found());
	}


	/* ---- the parents ---- */

	@Test
	void aChildBornBeforeTheParentIsAnErrorUnlessItIsNotTheirs(){
		final String parent = person("I1", IndividualReader.ENUM_SEX_MALE);
		final String child = person("I2", IndividualReader.ENUM_SEX_MALE);
		final String adopted = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(parent, exact("1850-01-01"));
		born(child, exact("1840-01-01"));
		born(adopted, exact("1840-01-01"));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, child, parent, null);
		link(RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD, adopted, parent, null);

		Assertions.assertEquals(List.of("child_before_parent"), found());
	}

	@Test
	void aChildAndAParentWhoseDatesOverlapAreNotJudged(){
		final String parent = person("I1", IndividualReader.ENUM_SEX_MALE);
		final String child = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(parent, decade(1850));
		born(child, year(1855));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, child, parent, null);

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}

	@Test
	void aChildBornAfterTheDeathOfTheMotherIsAnError(){
		final String mother = person("I1", IndividualReader.ENUM_SEX_FEMALE);
		final String child = person("I2", IndividualReader.ENUM_SEX_MALE);
		died(mother, exact("1850-06-01"));
		born(child, exact("1850-09-01"));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, child, mother, null);
		//about the same time, and the same year, cannot be said
		final String other = person("I3", IndividualReader.ENUM_SEX_FEMALE);
		final String otherChild = person("I4", IndividualReader.ENUM_SEX_MALE);
		died(other, about("1850-06-01", null));
		born(otherChild, exact("1850-09-01"));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, otherChild, other, null);
		final String sameYearMother = person("I5", IndividualReader.ENUM_SEX_FEMALE);
		final String sameYearChild = person("I6", IndividualReader.ENUM_SEX_MALE);
		died(sameYearMother, year(1850));
		born(sameYearChild, year(1850));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, sameYearChild, sameYearMother, null);

		Assertions.assertEquals(List.of("born_after_mother_death"), found());
	}

	@Test
	void aChildIsNotBornMoreThanThreeHundredDaysAfterTheDeathOfTheFather(){
		final String father = person("I1", IndividualReader.ENUM_SEX_MALE);
		died(father, exact("1850-01-01"));
		final String late = person("I2", IndividualReader.ENUM_SEX_MALE);
		born(late, exact("1851-06-01"));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, late, father, null);
		//nine months after: it happens
		final String posthumous = person("I3", IndividualReader.ENUM_SEX_MALE);
		born(posthumous, exact("1850-09-01"));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, posthumous, father, null);
		//a year of uncertainty on the birth: it can be inside the gestation
		final String uncertain = person("I4", IndividualReader.ENUM_SEX_MALE);
		born(uncertain, year(1850));
		link(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, uncertain, father, null);

		Assertions.assertEquals(List.of("born_after_father_death"), found());
	}


	/* ---- the unions ---- */

	@Test
	void aUnionThatBeginsBeforeTheBirthOrAfterTheDeathOfAPartnerIsAnError(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		final String anna = person("I2", IndividualReader.ENUM_SEX_FEMALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1900-01-01"));
		born(anna, exact("1855-01-01"));
		//she was not born yet
		link(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, mario, anna, exact("1852-01-01"));
		//he was dead
		final String luca = person("I3", IndividualReader.ENUM_SEX_MALE);
		link(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, luca, mario, exact("1905-01-01"));

		Assertions.assertEquals(List.of("union_before_birth", "union_after_death"), found());
	}

	@Test
	void aUnionInTheDecadeOfTheBirthIsNotJudged(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		final String anna = person("I2", IndividualReader.ENUM_SEX_FEMALE);
		born(mario, exact("1855-01-01"));
		born(anna, exact("1855-01-01"));
		link(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, mario, anna, decade(1850));

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}


	/* ---- the memberships of a group ---- */

	private void membership(final String personId, final FLEFRecord validFrom, final FLEFRecord validTo){
		counter ++;
		if(model.getRecordById("G1") == null)
			model.addRecord(FLEFRecord.createMainRecord("G1", GroupReader.TYPE));
		final FLEFRecord relationship = FLEFRecord.createMainRecord("R" + counter, RelationshipReader.TYPE)
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_SUBJECT).addChild(FLEFRecord.createChildWithTagAndValue(EventParticipationReader.TAG_PARTICIPANT_INDIVIDUAL, personId)))
			.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_TARGET).addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_GROUP, "G1")))
			.addChild(FLEFRecord.createChildWithTagAndValue(RelationshipReader.TAG_TYPE, RelationshipReader.ENUM_TYPE_GROUP_MEMBER));
		if(validFrom != null)
			relationship.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_VALID_FROM).addChild(validFrom.getChildren().getFirst()));
		if(validTo != null)
			relationship.addChild(FLEFRecord.createChildWithTag(RelationshipReader.TAG_VALID_TO).addChild(validTo.getChildren().getFirst()));
		model.addRecord(relationship);
	}

	@Test
	void aMembershipOutsideTheLifeOfTheMemberIsAnError(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1900-01-01"));
		membership(mario, exact("1840-01-01"), null);
		membership(mario, exact("1905-01-01"), null);
		//it ends before he was born, and does not say when it began
		membership(mario, null, exact("1845-01-01"));

		Assertions.assertEquals(List.of("membership_before_birth", "membership_after_death", "membership_ended_before_birth"), found());
	}

	@Test
	void aMembershipDuringTheLifeOfTheMemberIsFine(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1900-01-01"));
		membership(mario, exact("1870-01-01"), exact("1890-01-01"));
		//it goes on after the death: it is recorded as open, not wrong
		membership(mario, exact("1880-01-01"), exact("1950-01-01"));

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}

	@Test
	void aMembershipThatBeginsInTheDecadeOfTheBirthIsNotJudged(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1855-01-01"));
		membership(mario, decade(1850), null);

		Assertions.assertTrue(found().isEmpty(), found().toString());
	}


	/* ---- the report ---- */

	@Test
	void theseAreErrorsOfTheFileAndTheMessageNamesThePeopleAndTheDates(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		born(mario, exact("1850-01-01"));
		died(mario, exact("1840-01-01"));

		final ConsistencyService.Issue issue = ConsistencyService.check(model).issues().stream()
			.filter(i -> i.kind().equals("death_before_birth"))
			.findFirst()
			.orElseThrow();

		Assertions.assertEquals(ConsistencyService.Severity.ERROR, issue.severity());
		Assertions.assertTrue(issue.message().contains("NameI1 (I1)"), issue.message());
		Assertions.assertEquals("I1", issue.recordId());
	}


	/* ---- the dates that cannot be read ---- */

	/** The date with another calendar (the point of the date is its first descendant: value, point). */
	private static FLEFRecord inCalendar(final FLEFRecord date, final String calendar){
		date.getChildren().getFirst().getChildren().getFirst()
			.addChild(FLEFRecord.createChildWithTagAndValue(DateReader.TAG_CALENDAR, calendar));
		return date;
	}

	private List<String> unreadable(){
		return ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.kind().startsWith("date_"))
			.map(ConsistencyService.Issue::kind)
			.toList();
	}

	@Test
	void aDateThatIsNotValidInItsCalendarIsAWarningAndTheOthersAreNot(){
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		//an event with a date that does not exist, a relationship with one, and the dates that are fine
		born(mario, exact("1850-13-45"));
		event(mario, "residence", exact("1851-02-03"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", decade(1850), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", century(19), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", about("1851-02-03", "P1Y"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		event(mario, "residence", FLEFRecord.createChildWithTag(EventReader.TAG_DATE), EventParticipationReader.ENUM_ROLE_PRINCIPAL);
		final String anna = person("I2", IndividualReader.ENUM_SEX_FEMALE);
		link(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, mario, anna, exact("1860-02-30"));

		final List<ConsistencyService.Issue> found = ConsistencyService.check(model).issues().stream()
			.filter(issue -> issue.kind().equals("date_invalid"))
			.toList();

		Assertions.assertEquals(2, found.size(), found.toString());
		Assertions.assertTrue(found.stream().allMatch(issue -> issue.severity() == ConsistencyService.Severity.WARNING));
		Assertions.assertTrue(found.stream().anyMatch(issue -> issue.message().contains("1850-13-45")), found.toString());
		Assertions.assertTrue(found.stream().anyMatch(issue -> issue.message().contains("valid_from")), found.toString());
		Assertions.assertTrue(unreadable().stream().noneMatch("date_not_checked"::equals));
	}

	@Test
	void aDateOfACalendarThatNeedsThePluginIsNotCheckedAndIsNotAnErrorOfTheFile(){
		boolean pluginMissing;
		try{
			CalendarType.fromCode("chinese").parseToJdn("2020-01-01");
			pluginMissing = false;
		}
		catch(final UnsupportedOperationException e){
			pluginMissing = true;
		}
		catch(final IllegalArgumentException e){
			pluginMissing = false;
		}
		final String mario = person("I1", IndividualReader.ENUM_SEX_MALE);
		event(mario, "residence", inCalendar(exact("2020-01-01"), "chinese"), EventParticipationReader.ENUM_ROLE_PRINCIPAL);

		final List<String> kinds = unreadable();

		//without the plugin it is told apart from a date that is wrong; with it, the date is read (or is wrong in its own notation)
		if(pluginMissing)
			Assertions.assertEquals(List.of("date_not_checked"), kinds);
		else
			Assertions.assertFalse(kinds.contains("date_not_checked"), kinds.toString());
	}

	@Test
	void theNormalizerSaysWhyADateIsNotRead(){
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(null));
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(exact("1850-01-01")));
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(decade(1850)));
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(century(18)));
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(FLEFRecord.createChildWithTag(EventReader.TAG_DATE)));
		Assertions.assertEquals(DateNormalizer.Problem.INVALID, DateNormalizer.problem(exact("1850-02-31")));
		//one end of a bounded date that is wrong makes the date wrong
		Assertions.assertEquals(DateNormalizer.Problem.INVALID, DateNormalizer.problem(notAfter("1850-99-99")));
		Assertions.assertEquals(DateNormalizer.Problem.NONE, DateNormalizer.problem(notAfter("1850-01-01")));
	}

}
