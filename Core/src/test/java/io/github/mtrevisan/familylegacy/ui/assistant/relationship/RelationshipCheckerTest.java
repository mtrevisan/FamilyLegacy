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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.EventParticipationReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


class RelationshipCheckerTest{

	private static DateRange day(final String gregorianDate){
		return DateRange.day(CalendarType.GREGORIAN.parseToJdn(gregorianDate));
	}

	/** A data set built step by step. */
	private static final class Builder{

		private final Map<String, RelationshipData.Person> persons = new LinkedHashMap<>();
		private final Map<String, String> groups = new LinkedHashMap<>();
		private final List<RelationshipData.Relationship> relationships = new ArrayList<>();
		private final List<RelationshipData.Event> events = new ArrayList<>();
		private int counter;

		Builder person(final String id, final String birth){
			persons.put(id, new RelationshipData.Person(id, "Person " + id, SexType.UNKNOWN, (birth != null? day(birth): null)));
			return this;
		}

		Builder person(final String id, final String birth, final String death){
			persons.put(id, new RelationshipData.Person(id, "Person " + id, SexType.UNKNOWN, (birth != null? day(birth): null),
				(death != null? day(death): null)));
			return this;
		}

		Builder group(final String id){
			groups.put(id, "Group " + id);
			return this;
		}

		Builder relationship(final String subject, final String target, final String type){
			return relationship(subject, target, type, null, null, null, false);
		}

		Builder relationship(final String subject, final String target, final String type, final String role, final String from,
				final String to, final boolean ended){
			final RelationshipData.Kind subjectKind = (groups.containsKey(subject)
				? RelationshipData.Kind.GROUP: RelationshipData.Kind.INDIVIDUAL);
			final RelationshipData.Kind targetKind = (groups.containsKey(target)
				? RelationshipData.Kind.GROUP: RelationshipData.Kind.INDIVIDUAL);
			relationships.add(new RelationshipData.Relationship("R" + (++ counter), subject, subjectKind, target, targetKind, type,
				role, (from != null? day(from): null), (to != null? day(to): null), (from != null? "from " + from: StringUtils.EMPTY), ended));
			return this;
		}

		Builder event(final String id, final String type, final String... people){
			final List<RelationshipData.Participant> participants = new ArrayList<>();
			for(final String person : people)
				participants.add(new RelationshipData.Participant(person, true));
			events.add(new RelationshipData.Event(id, type, null, "date of " + id, participants));
			return this;
		}

		Builder eventWithRoles(final String id, final String type, final RelationshipData.Participant... participants){
			events.add(new RelationshipData.Event(id, type, null, "date of " + id, List.of(participants)));
			return this;
		}

		private final Set<String> clergy = new LinkedHashSet<>();
		private final Set<String> churches = new LinkedHashSet<>();

		Builder clergy(final String personId){
			clergy.add(personId);
			return this;
		}

		Builder church(final String placeId){
			churches.add(placeId);
			return this;
		}

		private final Map<String, String> countries = new LinkedHashMap<>();

		Builder country(final String placeId, final String country){
			countries.put(placeId, country);
			return this;
		}

		/** A marriage at a place, on a day, with the titles of the sources that cite it. */
		Builder marriageAt(final String id, final String placeId, final String day, final List<String> sourceTitles, final String... people){
			final List<RelationshipData.Participant> participants = new ArrayList<>();
			for(final String person : people)
				participants.add(new RelationshipData.Participant(person, true));
			events.add(new RelationshipData.Event(id, EventReader.ENUM_TYPE_MARRIAGE, (day != null? day(day): null), "date of " + id,
				participants, placeId, sourceTitles));
			return this;
		}

		Builder eventAt(final String id, final String type, final String placeId, final RelationshipData.Participant... participants){
			events.add(new RelationshipData.Event(id, type, null, "date of " + id, List.of(participants), placeId));
			return this;
		}

		List<Diagnostic> check(){
			return RelationshipChecker.check(data());
		}

		List<RelationshipFinding> findings(){
			return RelationshipChecker.findings(data());
		}

		private RelationshipData data(){
			return new RelationshipData(persons, groups, relationships, events, clergy, churches, countries);
		}

	}

	private static List<String> titles(final List<Diagnostic> findings){
		return findings.stream()
			.map(Diagnostic::title)
			.toList();
	}

	private static Diagnostic only(final List<Diagnostic> findings){
		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		return findings.getFirst();
	}

	private static Builder family(){
		return new Builder()
			.person("c", "1 June 1870").person("p", "1 June 1840").person("q", "1 June 1845").group("g");
	}


	@Test
	void aConsistentFamilyHasNothingToReport(){
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.relationship("c", "g", RelationshipReader.ENUM_TYPE_GROUP_MEMBER).relationship("p", "g", RelationshipReader.ENUM_TYPE_GROUP_MEMBER)
			.check().isEmpty());
	}

	@Test
	void aRelationshipWithItself(){
		final Diagnostic finding = only(family().relationship("c", "c", RelationshipReader.ENUM_TYPE_CHILD).check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertEquals(AssistantCategory.RELATIONSHIPS, finding.category());
		Assertions.assertTrue(finding.title().startsWith("Relationship with itself: Person c"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "R1")));
	}

	@Test
	void aTypeThatDoesNotFitTheParticipants(){
		final List<Diagnostic> findings = family()
			.group("g2")
			.relationship("c", "g", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_GROUP_MEMBER)
			.relationship("g", "c", RelationshipReader.ENUM_TYPE_PART_OF)
			.relationship("g", "g2", RelationshipReader.ENUM_TYPE_PART_OF)
			.check();

		//a spouse that is a group, a membership between individuals, a group that is part of an individual; a group that is part of
		//another group is fine
		Assertions.assertEquals(3, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(f -> f.title().startsWith("Type not valid for what it links")));
	}

	@Test
	void theGenericSpouseIsValidBetweenIndividuals(){
		final List<Diagnostic> findings = family().relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE).check();

		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().startsWith("Type not valid")), titles(findings).toString());
	}

	@Test
	void aDuplicateRelationship(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Duplicate relationship: Person c and Person p"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("R1", "R2")));
	}

	@Test
	void aCoupleWrittenFromBothSidesIsADuplicate(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE).relationship("q", "p", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Duplicate relationship"));
		//a civil and a religious marriage of the same couple are not duplicates, nor is a child written from the other side
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE).relationship("q", "p", RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE)
			.check().isEmpty());
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("p", "c", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().isEmpty());
	}

	@Test
	void associatesWithDifferentRolesFromBothSidesAreNotDuplicates(){
		//the role is the one of the subject: a birth witness and the newborn are two different relationships
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ASSOCIATE, "newborn", null, null, false)
			.relationship("q", "p", RelationshipReader.ENUM_TYPE_ASSOCIATE, "birth witness", null, null, false)
			.check().isEmpty());
		//the same role written twice in the same direction is still a duplicate
		Assertions.assertTrue(only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ASSOCIATE, "friend", null, null, false)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ASSOCIATE, "Friend", null, null, false)
			.check()).title().startsWith("Duplicate relationship"));
	}

	@Test
	void aRelationshipThatEndsBeforeItBegins(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", "1 June 1860", true)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Relationship ends before it begins"));
		//a relationship that ends at the same day it begins is not reversed
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", "1 June 1865", true)
			.check().isEmpty());
	}

	@Test
	void aRoleOnARelationshipOfTheFamily(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD, "heir", null, null, false)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Role on a biological_child relationship"));
		//a role is expected on a membership
		Assertions.assertTrue(family()
			.relationship("c", "g", RelationshipReader.ENUM_TYPE_GROUP_MEMBER, "president", null, null, false)
			.check().isEmpty());
	}

	@Test
	void aChildOfAnAdoptionIsAdoptive(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "p")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Child relationship that looks adoptive: Person c"));
		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD));
		Assertions.assertTrue(finding.explanation().contains("E1"));
		Assertions.assertTrue(finding.recordIds().contains("E1"));
	}

	@Test
	void aBiologicalChildOfAnAdoptionIsAConflict(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "p")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Biological child with an adoption"));
		//an adoption that does not name the parent is no evidence about it
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "q")
			.check().isEmpty());
	}

	@Test
	void aParentMarriedToTheOtherAfterTheBirthIsAStepParent(){
		final Diagnostic finding = only(family()
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1875", null, false)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Child relationship that looks like a step-parent: Person c"));
		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_STEP_CHILD));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "p", "q")));
	}

	@Test
	void aMarriageBeforeTheBirthOrWithoutDateIsNoEvidenceOfAStepParent(){
		//married before the child was born: a parent of the child, as expected
		Assertions.assertTrue(family()
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//no date of the marriage
		Assertions.assertTrue(family()
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//the child has no date of birth
		Assertions.assertTrue(new Builder().person("c", null).person("p", "1 June 1840").person("q", "1 June 1845")
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1875", null, false)
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//the relationship of the child is not the generic one: the researcher already chose
		Assertions.assertTrue(family()
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1875", null, false)
			.check().isEmpty());
	}

	@Test
	void theBannsSayTheMarriageIsReligious(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE_BANN, "p", "q")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Marriage type: Person p and Person q"));
		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE));
		Assertions.assertTrue(finding.explanation().contains(EventReader.ENUM_TYPE_MARRIAGE_BANN));
	}

	@Test
	void aMarriageInAPlaceOfTheChurchPointsToReligious(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.church("P1")
			.eventAt("E1", EventReader.ENUM_TYPE_MARRIAGE, "P1", new RelationshipData.Participant("p", true),
				new RelationshipData.Participant("q", true))
			.check());

		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE));
		Assertions.assertTrue(finding.explanation().contains("place of the church"));
		Assertions.assertTrue(finding.explanation().contains("only a hint"));
	}

	@Test
	void anOfficiantOfTheClergyPointsToReligious(){
		final Diagnostic finding = only(family()
			.person("x", "1 June 1800")
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.clergy("x")
			.eventAt("E1", EventReader.ENUM_TYPE_MARRIAGE, null, new RelationshipData.Participant("p", true),
				new RelationshipData.Participant("q", true),
				new RelationshipData.Participant("x", false, EventParticipationReader.ENUM_ROLE_OFFICIANT))
			.check());

		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE));
		Assertions.assertTrue(finding.explanation().contains("is of the clergy"));
	}

	@Test
	void anOfficiantWhoIsNotOfTheClergyOrAnOrdinaryPlaceDoNotDecide(){
		final List<Diagnostic> findings = family()
			.person("x", "1 June 1800")
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.eventAt("E1", EventReader.ENUM_TYPE_MARRIAGE, "P2", new RelationshipData.Participant("p", true),
				new RelationshipData.Participant("q", true),
				new RelationshipData.Participant("x", false, EventParticipationReader.ENUM_ROLE_OFFICIANT))
			.check();

		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.getFirst().title().contains("generic type spouse"));
	}

	@Test
	void theChurchAndTheLicenseTogetherDoNotDecide(){
		final List<Diagnostic> findings = family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.church("P1")
			.eventAt("E1", EventReader.ENUM_TYPE_MARRIAGE, "P1", new RelationshipData.Participant("p", true),
				new RelationshipData.Participant("q", true))
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE_LICENSE, "p", "q")
			.check();

		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.getFirst().title().contains("generic type spouse"));
	}

	@Test
	void theLicenseSaysTheMarriageIsCivil(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE_LICENSE, "p", "q")
			.check());

		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE));
	}

	@Test
	void bannsAndLicenseTogetherDoNotDecide(){
		final List<Diagnostic> findings = family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE_BANN, "p", "q")
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE_LICENSE, "p", "q")
			.check();

		//the generic type stays in the finding about the marriage
		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.getFirst().title().contains("generic type spouse"));
	}

	@Test
	void theGenericTypesAreAFindingForEachRelationship(){
		final Builder builder = new Builder();
		for(int i = 0; i < 40; i ++)
			builder.person("a" + i, null).person("b" + i, null).relationship("a" + i, "b" + i, RelationshipReader.ENUM_TYPE_SPOUSE);
		builder.person("kid", null);
		for(int i = 0; i < 3; i ++)
			builder.relationship("kid", "a" + i, RelationshipReader.ENUM_TYPE_CHILD);

		//the partners of the parents of the kid are asked about on their own, and are not what is tested here
		final List<Diagnostic> findings = builder.check().stream()
			.filter(f -> !f.title().startsWith("Partner of a parent"))
			.toList();

		//40 marriages and 3 children: one finding for each, none summed up
		Assertions.assertEquals(43, findings.size(), titles(findings).toString());
		Assertions.assertEquals(40, findings.stream().filter(f -> f.title().startsWith("Marriage with the generic type spouse")).count());
		Assertions.assertEquals(3, findings.stream().filter(f -> f.title().startsWith("Child relationship with the generic type")).count());
		Assertions.assertTrue(findings.stream().allMatch(f -> f.recordIds().size() == 3), findings.toString());
		Assertions.assertEquals(findings.size(), findings.stream().map(Diagnostic::key).distinct().count());
	}

	@Test
	void everyRelationshipOfAGenericTypeIsListed(){
		final Builder builder = new Builder();
		for(int i = 0; i < 250; i ++)
			builder.person("a" + i, null).person("b" + i, null).relationship("a" + i, "b" + i, RelationshipReader.ENUM_TYPE_SPOUSE);

		Assertions.assertEquals(250, builder.check().size());
	}

	@Test
	void aRelationshipWithASuggestionIsNotInTheGenericFinding(){
		final List<Diagnostic> findings = family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "p")
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE_BANN, "p", "q")
			.check().stream()
			.filter(f -> !f.title().startsWith("Partner of a parent"))
			.toList();

		Assertions.assertEquals(2, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().contains("generic type")));
	}

	@Test
	void theFindingsThatSuggestAnotherTypeCarryTheChangeOfType(){
		final RelationshipFinding adoptive = family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "p")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD), adoptive.typeChange());
		Assertions.assertNull(adoptive.draft());

		final RelationshipFinding conflict = family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "p")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD), conflict.typeChange());

		final RelationshipFinding step = family()
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1875", null, false)
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R2", RelationshipReader.ENUM_TYPE_STEP_CHILD), step.typeChange());

		final RelationshipFinding spouse = family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE_BANN, "p", "q")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE), spouse.typeChange());

		//a finding that is not about the type has none
		Assertions.assertNull(family().relationship("c", "c", RelationshipReader.ENUM_TYPE_CHILD).findings().getFirst().typeChange());
	}

	@Test
	void thePartnerOfAParentWithoutARelationshipWithTheChildIsAQuestionWithADraft(){
		final List<RelationshipFinding> findings = family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.findings();

		Assertions.assertEquals(1, findings.size(), titles(findings.stream().map(RelationshipFinding::diagnostic).toList()).toString());
		final RelationshipFinding finding = findings.getFirst();
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.diagnostic().severity());
		Assertions.assertTrue(finding.diagnostic().title().startsWith("Partner of a parent without a relationship: Person c and Person q"));
		Assertions.assertTrue(finding.diagnostic().recordIds().containsAll(List.of("c", "q", "p", "R2")));
		//the draft starts the relationship between the child and the partner, and leaves the type to the researcher
		Assertions.assertEquals("c", finding.draft().subjectId());
		Assertions.assertEquals("q", finding.draft().targetId());
		Assertions.assertNull(finding.draft().type());
	}

	@Test
	void thePartnerOfAParentIsNotAskedAboutIfTheChildIsLinkedToThemOrTheUnionEndedBeforeTheBirth(){
		//the partner is already a parent
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.check().isEmpty());
		//the partner is already a step-parent
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_STEP_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1875", null, false)
			.check().isEmpty());
		//the union ended before the child was born (1 June 1870)
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1860", "1 June 1865", true)
			.check().isEmpty());
	}

	@Test
	void anAssociationWithoutARoleBetweenRelativesAddsNothing(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "c", RelationshipReader.ENUM_TYPE_ASSOCIATE, null, null, null, false)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Association between relatives: Person p and Person c"));
		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
		//between two that are not relatives it is what it should be
		Assertions.assertTrue(family().relationship("c", "q", RelationshipReader.ENUM_TYPE_ASSOCIATE, null, null, null, false).check().isEmpty());
	}

	@Test
	void anAssociationWithARoleBetweenRelativesSaysSomethingMore(){
		//a witness, a mentor, the birth it took part in: the role is not what the tie of the family says
		for(final String role : List.of("mentor", "witness", "birth"))
			Assertions.assertTrue(family()
				.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
				.relationship("p", "c", RelationshipReader.ENUM_TYPE_ASSOCIATE, role, null, null, false)
				.check().isEmpty(), role);
	}

	@Test
	void aMarriageWithoutARelationshipComesWithADraft(){
		final List<RelationshipFinding> findings = family()
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.findings();

		Assertions.assertEquals(1, findings.size());
		final RelationshipFinding finding = findings.getFirst();
		Assertions.assertTrue(finding.diagnostic().title().startsWith("Marriage without a relationship: Person p and Person q"));
		Assertions.assertEquals(List.of("p", "q", "E1"), finding.diagnostic().recordIds());
		Assertions.assertEquals("p", finding.draft().subjectId());
		Assertions.assertEquals("q", finding.draft().targetId());
		//the evidence does not tell the type: the researcher chooses
		Assertions.assertNull(finding.draft().type());
	}

	@Test
	void theDraftOfAMarriageStartsFromTheEventThatIsItsEvidence(){
		final RelationshipDraft draft = family()
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.findings().getFirst().draft();

		//the date of the marriage is where the relationship begins, and its sources are the evidence
		Assertions.assertEquals("E1", draft.eventId());
		Assertions.assertTrue(draft.copyDate());
	}

	@Test
	void theInformantOfABirthWithoutARelationshipWithTheChildIsAQuestionWithADraft(){
		final List<RelationshipFinding> findings = family()
			.eventWithRoles("E1", EventReader.ENUM_TYPE_BIRTH, new RelationshipData.Participant("c", true, EventParticipationReader.ENUM_ROLE_PRINCIPAL),
				new RelationshipData.Participant("p", false, EventParticipationReader.ENUM_ROLE_INFORMANT))
			.findings();

		Assertions.assertEquals(1, findings.size());
		final RelationshipFinding finding = findings.getFirst();
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.diagnostic().severity());
		Assertions.assertTrue(finding.diagnostic().title().startsWith("Informant of a birth without a relationship: Person c and Person p"));
		Assertions.assertEquals(List.of("c", "p", "E1"), finding.diagnostic().recordIds());
		//the draft starts the relationship with no type, cites what the birth cites, and does not take the date of the birth
		Assertions.assertEquals("c", finding.draft().subjectId());
		Assertions.assertEquals("p", finding.draft().targetId());
		Assertions.assertNull(finding.draft().type());
		Assertions.assertEquals("E1", finding.draft().eventId());
		Assertions.assertFalse(finding.draft().copyDate());
	}

	@Test
	void theInformantOfABirthIsNotAskedAboutWhenTheRoleSaysNothingOrTheyAreLinkedOrCertainlyBornLater(){
		//a witness or an officiant says nothing about kinship, and without a role there is no informant
		Assertions.assertTrue(family()
			.eventWithRoles("E1", EventReader.ENUM_TYPE_BIRTH, new RelationshipData.Participant("c", true, EventParticipationReader.ENUM_ROLE_PRINCIPAL),
				new RelationshipData.Participant("p", false, EventParticipationReader.ENUM_ROLE_WITNESS), new RelationshipData.Participant("q", false, EventParticipationReader.ENUM_ROLE_OFFICIANT))
			.check().isEmpty());
		//the informant is already related to the child
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_STEP_CHILD)
			.eventWithRoles("E1", EventReader.ENUM_TYPE_BIRTH, new RelationshipData.Participant("c", true, EventParticipationReader.ENUM_ROLE_PRINCIPAL),
				new RelationshipData.Participant("p", false, EventParticipationReader.ENUM_ROLE_INFORMANT))
			.check().isEmpty());
		//the informant was born (1900) after the child (1 June 1870): not a parent, but nothing wrong either
		Assertions.assertTrue(new Builder().person("c", "1 June 1870").person("y", "1 June 1900")
			.eventWithRoles("E1", EventReader.ENUM_TYPE_BIRTH, new RelationshipData.Participant("c", true, EventParticipationReader.ENUM_ROLE_PRINCIPAL),
				new RelationshipData.Participant("y", false, EventParticipationReader.ENUM_ROLE_INFORMANT))
			.check().isEmpty());
		//the informant of another event is not the declarant of a birth
		Assertions.assertTrue(family()
			.eventWithRoles("E1", EventReader.ENUM_TYPE_DEATH, new RelationshipData.Participant("c", true, EventParticipationReader.ENUM_ROLE_PRINCIPAL),
				new RelationshipData.Participant("p", false, EventParticipationReader.ENUM_ROLE_INFORMANT))
			.check().isEmpty());
	}

	@Test
	void theDraftTakesTheTypeFromTheBanns(){
		final RelationshipFinding finding = family()
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE_BANN, "q", "p")
			.findings().getFirst();

		Assertions.assertEquals(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE, finding.draft().type());
	}

	@Test
	void aMarriageThatHasItsRelationshipIsComplete(){
		for(final String type : List.of(RelationshipReader.ENUM_TYPE_SPOUSE, RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE, RelationshipReader.ENUM_TYPE_CUSTOMARY_SPOUSE))
			Assertions.assertTrue(family()
				.relationship("q", "p", type)
				.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
				.check().stream().noneMatch(f -> f.title().startsWith("Marriage without")), type);
		//a cohabitation is not a marriage
		Assertions.assertEquals(1, family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_COHABITING_PARTNER)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.findings().stream().filter(f -> f.draft() != null).count());
	}

	@Test
	void theMarriageOfOtherThanTwoIsNotTouched(){
		//a witness is not a principal, and one principal alone cannot be a couple
		final List<Diagnostic> findings = new Builder().person("a", null).person("b", null).person("w", null)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "a")
			.check();
		Assertions.assertTrue(findings.isEmpty(), titles(findings).toString());

		final Builder withWitness = new Builder().person("a", null).person("b", null).person("w", null);
		withWitness.events.add(new RelationshipData.Event("E2", EventReader.ENUM_TYPE_MARRIAGE, null, "",
			List.of(new RelationshipData.Participant("a", true), new RelationshipData.Participant("b", true),
				new RelationshipData.Participant("w", false))));
		Assertions.assertEquals(1, withWitness.findings().size());
		Assertions.assertEquals(List.of("a", "b", "E2"), withWitness.findings().getFirst().diagnostic().recordIds());
	}

	@Test
	void aMarriageOfACoupleWithTwoEventsIsOneFinding(){
		final List<RelationshipFinding> findings = family()
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE, "q", "p")
			.findings();

		Assertions.assertEquals(1, findings.size());
		Assertions.assertTrue(findings.getFirst().diagnostic().recordIds().containsAll(List.of("E1", "E2")));
	}

	@Test
	void anEngagementThatTheMarriageDidNotClose(){
		final Diagnostic byRelationship = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.check());
		Assertions.assertTrue(byRelationship.title().startsWith("Engagement of a married couple: Person p and Person q"));

		final List<RelationshipFinding> byEvent = family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER)
			.event("E1", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.findings();
		Assertions.assertTrue(byEvent.stream().anyMatch(f -> f.diagnostic().title().startsWith("Engagement of a married couple")));
	}

	@Test
	void anEngagementThatEndedOrThatIsStillOnIsFine(){
		//closed by a date or by the status
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER, null, "1 June 1860", "1 June 1865", true)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.check().isEmpty());
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER, null, null, null, true)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.check().isEmpty());
		//the couple did not marry
		Assertions.assertTrue(family().relationship("p", "q", RelationshipReader.ENUM_TYPE_ENGAGED_PARTNER).check().isEmpty());
	}

	@Test
	void theKeyOfAFindingFollowsItsData(){
		final Diagnostic finding = only(family().relationship("c", "c", RelationshipReader.ENUM_TYPE_CHILD).check());

		Assertions.assertTrue(finding.isIgnorable());
		Assertions.assertEquals(finding.key(), only(family().relationship("c", "c", RelationshipReader.ENUM_TYPE_CHILD).check()).key());
		//another type of relationship, another finding
		Assertions.assertNotEquals(finding.key(), only(family().relationship("c", "c", RelationshipReader.ENUM_TYPE_SPOUSE).check()).key());
	}

	@Test
	void everyFindingHasAKeyAndTheyAreDistinct(){
		final List<Diagnostic> findings = family()
			.relationship("c", "c", RelationshipReader.ENUM_TYPE_CHILD)
			.relationship("c", "g", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE)
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, "head", "1 June 1865", "1 June 1860", true)
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_CHILD).event("E1", EventReader.ENUM_TYPE_ADOPTION, "c", "q")
			.event("E2", EventReader.ENUM_TYPE_MARRIAGE, "p", "q")
			.check();

		Assertions.assertTrue(findings.size() >= 6, titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(Diagnostic::isIgnorable));
		Assertions.assertEquals(findings.size(), findings.stream().map(Diagnostic::key).distinct().count());
	}


	@Test
	void aTypeThatDoesNotFitTheParticipantsIsAnError(){
		final Diagnostic finding = only(family().relationship("c", "g", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE).check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.key().startsWith("relationship.invalid_for_participants|"));
	}

	@Test
	void aBiologicalChildMarriedToTheParentIsAnError(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "c", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1890", null, false)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.key().startsWith("relationship.child_married_to_parent|"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "p", "R1", "R2")));
	}

	@Test
	void anAdoptiveParentMarriedToTheChildIsNotReported(){
		Assertions.assertTrue(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD)
			.relationship("p", "c", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1890", null, false)
			.check().stream().noneMatch(finding -> finding.key().startsWith("relationship.child_married_to_parent")));
	}

	@Test
	void aSiblingThatIsAlsoAParentIsAnError(){
		//c is a biological child of p and q, and p is a biological child of q
		final Diagnostic finding = only(family()
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.key().startsWith("relationship.sibling_and_parent|"));
	}

	@Test
	void aParentWithAnotherParentOfItsOwnIsNotASibling(){
		Assertions.assertTrue(family()
			.person("r", "1 June 1815")
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "q", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "r", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().isEmpty());
	}


	@Test
	void aPartnerOfAParentIsNotAskedAboutIfTheChildHasTwoBiologicalParents(){
		Assertions.assertTrue(family()
			.person("r", "1 June 1842")
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("c", "r", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.check().isEmpty());
	}

	@Test
	void aPartnerOfAParentWhoDiedBeforeTheBirthOfTheChildIsNotAskedAbout(){
		//q died in 1860, the child was born in 1870: q is neither another parent nor a step-parent
		Assertions.assertTrue(new Builder()
			.person("c", "1 June 1870").person("p", "1 June 1840").person("q", "1 June 1835", "1 June 1860")
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1855", null, false)
			.check().isEmpty());
	}

	@Test
	void aPartnerOfAParentWhoDiedAfterTheBirthOfTheChildIsStillAskedAbout(){
		Assertions.assertEquals(1, new Builder()
			.person("c", "1 June 1870").person("p", "1 June 1840").person("q", "1 June 1835", "1 June 1880")
			.relationship("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.check().size());
	}


	@Test
	void theTitleOfASourceOfTheMarriageSaysItsKind(){
		final Diagnostic religious = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.marriageAt("E1", null, null, List.of("marriage Rossi Mario + Bianchi Anna 18500112 religious"), "p", "q")
			.check());

		Assertions.assertTrue(religious.key().startsWith("relationship.suggest_spouse|"), religious.key());
		Assertions.assertTrue(religious.explanation().contains(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE), religious.explanation());
		Assertions.assertTrue(religious.explanation().contains("marriage Rossi Mario + Bianchi Anna 18500112 religious"));

		final Diagnostic civil = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE)
			.marriageAt("E1", null, null, List.of("marriage Verdi Luigi + Neri Maria 19100203 civil"), "p", "q")
			.check());
		Assertions.assertTrue(civil.explanation().contains(RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE), civil.explanation());
	}

	@Test
	void aMarriageInItalyBeforeTheKingdomIsAWeakHintForReligious(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE, null, "1 June 1850", null, false)
			.country("P1", "italia")
			.marriageAt("E1", "P1", "1 June 1850", List.of(), "p", "q")
			.check());

		Assertions.assertTrue(finding.key().startsWith("relationship.suggest_spouse|"), finding.key());
		Assertions.assertTrue(finding.explanation().contains(RelationshipReader.ENUM_TYPE_RELIGIOUS_SPOUSE), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("weak hint"), finding.explanation());
	}

	@Test
	void theWeakHintIsNotGivenOutsideItalyDuringTheNapoleonicCodeOrAfterTheKingdom(){
		//another country
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE, null, "1 June 1850", null, false)
			.country("P1", "france").marriageAt("E1", "P1", "1 June 1850", List.of(), "p", "q")
			.check().stream().noneMatch(f -> f.key().startsWith("relationship.suggest_spouse|")));
		//the years of the Napoleonic code, when the civil marriage existed
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE, null, "1 June 1810", null, false)
			.country("P1", "italia").marriageAt("E1", "P1", "1 June 1810", List.of(), "p", "q")
			.check().stream().noneMatch(f -> f.key().startsWith("relationship.suggest_spouse|")));
		//after the Kingdom of Italy
		Assertions.assertTrue(family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE, null, "1 June 1900", null, false)
			.country("P1", "italia").marriageAt("E1", "P1", "1 June 1900", List.of(), "p", "q")
			.check().stream().noneMatch(f -> f.key().startsWith("relationship.suggest_spouse|")));
	}


	@Test
	void theTextsGiveTheIdentifierOfTheRelationship(){
		final Diagnostic finding = only(family().relationship("p", "q", RelationshipReader.ENUM_TYPE_SPOUSE).check());

		//the identifier of the generic marriage is in the text, whatever else the finding says
		Assertions.assertTrue(finding.explanation().contains("[R1]"), finding.explanation());
	}

	@Test
	void ofSeveralRelationshipsTheTextGivesEveryIdentifier(){
		final Diagnostic duplicate = family()
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.relationship("p", "q", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.relationship("q", "p", RelationshipReader.ENUM_TYPE_CIVIL_SPOUSE, null, "1 June 1865", null, false)
			.check().stream().filter(f -> f.key().startsWith("relationship.duplicate_relationship|")).findFirst().orElseThrow();

		//none is left out, none is chosen over the others
		Assertions.assertTrue(duplicate.explanation().contains("[R1, R2, R3]"), duplicate.explanation());
	}

}
