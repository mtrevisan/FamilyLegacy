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
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


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

		List<Diagnostic> check(){
			return RelationshipChecker.check(data());
		}

		List<RelationshipFinding> findings(){
			return RelationshipChecker.findings(data());
		}

		private RelationshipData data(){
			return new RelationshipData(persons, groups, relationships, events);
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
			.relationship("c", "p", "biological_child").relationship("c", "q", "biological_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", null, false)
			.relationship("c", "g", "group_member").relationship("p", "g", "group_member")
			.check().isEmpty());
	}

	@Test
	void aRelationshipWithItself(){
		final Diagnostic finding = only(family().relationship("c", "c", "child").check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertEquals(AssistantCategory.RELATIONSHIPS, finding.category());
		Assertions.assertTrue(finding.title().startsWith("Relationship with itself: Person c"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "R1")));
	}

	@Test
	void aTypeThatDoesNotFitTheParticipants(){
		final List<Diagnostic> findings = family()
			.group("g2")
			.relationship("c", "g", "civil_spouse")
			.relationship("c", "p", "group_member")
			.relationship("g", "c", "part_of")
			.relationship("g", "g2", "part_of")
			.check();

		//a spouse that is a group, a membership between individuals, a group that is part of an individual; a group that is part of
		//another group is fine
		Assertions.assertEquals(3, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(f -> f.title().startsWith("Type not valid for what it links")));
	}

	@Test
	void theGenericSpouseIsValidBetweenIndividuals(){
		final List<Diagnostic> findings = family().relationship("p", "q", "spouse").check();

		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().startsWith("Type not valid")), titles(findings).toString());
	}

	@Test
	void aDuplicateRelationship(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "biological_child").relationship("c", "p", "biological_child")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Duplicate relationship: Person c and Person p"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("R1", "R2")));
	}

	@Test
	void aCoupleWrittenFromBothSidesIsADuplicate(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", "civil_spouse").relationship("q", "p", "civil_spouse")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Duplicate relationship"));
		//a civil and a religious marriage of the same couple are not duplicates, nor is a child written from the other side
		Assertions.assertTrue(family()
			.relationship("p", "q", "civil_spouse").relationship("q", "p", "religious_spouse")
			.check().isEmpty());
		Assertions.assertTrue(family()
			.relationship("c", "p", "biological_child").relationship("p", "c", "biological_child")
			.check().isEmpty());
	}

	@Test
	void aRelationshipThatEndsBeforeItBegins(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", "1 June 1860", true)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Relationship ends before it begins"));
		//a relationship that ends at the same day it begins is not reversed
		Assertions.assertTrue(family()
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", "1 June 1865", true)
			.check().isEmpty());
	}

	@Test
	void aRoleOnARelationshipOfTheFamily(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "biological_child", "heir", null, null, false)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Role on a biological_child relationship"));
		//a role is expected on a membership
		Assertions.assertTrue(family()
			.relationship("c", "g", "group_member", "president", null, null, false)
			.check().isEmpty());
	}

	@Test
	void aChildOfAnAdoptionIsAdoptive(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "child")
			.event("E1", "adoption", "c", "p")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Child relationship that looks adoptive: Person c"));
		Assertions.assertTrue(finding.explanation().contains("adoptive_child"));
		Assertions.assertTrue(finding.explanation().contains("E1"));
		Assertions.assertTrue(finding.recordIds().contains("E1"));
	}

	@Test
	void aBiologicalChildOfAnAdoptionIsAConflict(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "biological_child")
			.event("E1", "adoption", "c", "p")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Biological child with an adoption"));
		//an adoption that does not name the parent is no evidence about it
		Assertions.assertTrue(family()
			.relationship("c", "p", "biological_child")
			.event("E1", "adoption", "c", "q")
			.check().isEmpty());
	}

	@Test
	void aParentMarriedToTheOtherAfterTheBirthIsAStepParent(){
		final Diagnostic finding = only(family()
			.relationship("c", "q", "biological_child")
			.relationship("c", "p", "child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1875", null, false)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Child relationship that looks like a step-parent: Person c"));
		Assertions.assertTrue(finding.explanation().contains("step_child"));
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("c", "p", "q")));
	}

	@Test
	void aMarriageBeforeTheBirthOrWithoutDateIsNoEvidenceOfAStepParent(){
		//married before the child was born: a parent of the child, as expected
		Assertions.assertTrue(family()
			.relationship("c", "q", "biological_child").relationship("c", "p", "child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", null, false)
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//no date of the marriage
		Assertions.assertTrue(family()
			.relationship("c", "q", "biological_child").relationship("c", "p", "child")
			.relationship("p", "q", "civil_spouse")
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//the child has no date of birth
		Assertions.assertTrue(new Builder().person("c", null).person("p", "1 June 1840").person("q", "1 June 1845")
			.relationship("c", "q", "biological_child").relationship("c", "p", "child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1875", null, false)
			.check().stream().noneMatch(f -> f.title().contains("step-parent")));
		//the relationship of the child is not the generic one: the researcher already chose
		Assertions.assertTrue(family()
			.relationship("c", "q", "biological_child").relationship("c", "p", "biological_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1875", null, false)
			.check().isEmpty());
	}

	@Test
	void theBannsSayTheMarriageIsReligious(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", "spouse")
			.event("E1", "marriage_bann", "p", "q")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Marriage type: Person p and Person q"));
		Assertions.assertTrue(finding.explanation().contains("religious_spouse"));
		Assertions.assertTrue(finding.explanation().contains("marriage_bann"));
	}

	@Test
	void theLicenseSaysTheMarriageIsCivil(){
		final Diagnostic finding = only(family()
			.relationship("p", "q", "spouse")
			.event("E1", "marriage_license", "p", "q")
			.check());

		Assertions.assertTrue(finding.explanation().contains("civil_spouse"));
	}

	@Test
	void bannsAndLicenseTogetherDoNotDecide(){
		final List<Diagnostic> findings = family()
			.relationship("p", "q", "spouse")
			.event("E1", "marriage_bann", "p", "q")
			.event("E2", "marriage_license", "p", "q")
			.check();

		//the generic type stays in the finding about all of them
		Assertions.assertEquals(1, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.getFirst().title().contains("generic type spouse"));
	}

	@Test
	void theGenericTypesAreOneFindingNotOneEach(){
		final Builder builder = new Builder();
		for(int i = 0; i < 40; i ++)
			builder.person("a" + i, null).person("b" + i, null).relationship("a" + i, "b" + i, "spouse");
		builder.person("kid", null);
		for(int i = 0; i < 3; i ++)
			builder.relationship("kid", "a" + i, "child");

		//the partners of the parents of the kid are asked about on their own, and are not what is tested here
		final List<Diagnostic> findings = builder.check().stream()
			.filter(f -> !f.title().startsWith("Partner of a parent"))
			.toList();

		Assertions.assertEquals(2, findings.size(), titles(findings).toString());
		Assertions.assertEquals("40 marriage(s) with the generic type spouse", findings.get(0).title());
		Assertions.assertEquals("3 child relationship(s) with the generic type", findings.get(1).title());
		Assertions.assertEquals(40, findings.get(0).recordIds().size());
	}

	@Test
	void theListOfTheGenericTypesIsCapped(){
		final Builder builder = new Builder();
		for(int i = 0; i < RelationshipChecker.MAX_LISTED + 50; i ++)
			builder.person("a" + i, null).person("b" + i, null).relationship("a" + i, "b" + i, "spouse");

		final Diagnostic finding = only(builder.check());

		Assertions.assertEquals(RelationshipChecker.MAX_LISTED, finding.recordIds().size());
		Assertions.assertTrue(finding.title().startsWith((RelationshipChecker.MAX_LISTED + 50) + ""));
	}

	@Test
	void aRelationshipWithASuggestionIsNotInTheGenericFinding(){
		final List<Diagnostic> findings = family()
			.relationship("c", "p", "child")
			.relationship("p", "q", "spouse")
			.event("E1", "adoption", "c", "p")
			.event("E2", "marriage_bann", "p", "q")
			.check().stream()
			.filter(f -> !f.title().startsWith("Partner of a parent"))
			.toList();

		Assertions.assertEquals(2, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().contains("generic type")));
	}

	@Test
	void theFindingsThatSuggestAnotherTypeCarryTheChangeOfType(){
		final RelationshipFinding adoptive = family()
			.relationship("c", "p", "child")
			.event("E1", "adoption", "c", "p")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", "adoptive_child"), adoptive.typeChange());
		Assertions.assertNull(adoptive.draft());

		final RelationshipFinding conflict = family()
			.relationship("c", "p", "biological_child")
			.event("E1", "adoption", "c", "p")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", "adoptive_child"), conflict.typeChange());

		final RelationshipFinding step = family()
			.relationship("c", "q", "biological_child")
			.relationship("c", "p", "child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1875", null, false)
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R2", "step_child"), step.typeChange());

		final RelationshipFinding spouse = family()
			.relationship("p", "q", "spouse")
			.event("E1", "marriage_bann", "p", "q")
			.findings().getFirst();
		Assertions.assertEquals(new RelationshipTypeChange("R1", "religious_spouse"), spouse.typeChange());

		//a finding that is not about the type has none
		Assertions.assertNull(family().relationship("c", "c", "child").findings().getFirst().typeChange());
	}

	@Test
	void thePartnerOfAParentWithoutARelationshipWithTheChildIsAQuestionWithADraft(){
		final List<RelationshipFinding> findings = family()
			.relationship("c", "p", "biological_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", null, false)
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
			.relationship("c", "p", "biological_child")
			.relationship("c", "q", "biological_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1865", null, false)
			.check().isEmpty());
		//the partner is already a step-parent
		Assertions.assertTrue(family()
			.relationship("c", "p", "biological_child")
			.relationship("c", "q", "step_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1875", null, false)
			.check().isEmpty());
		//the union ended before the child was born (1 June 1870)
		Assertions.assertTrue(family()
			.relationship("c", "p", "biological_child")
			.relationship("p", "q", "civil_spouse", null, "1 June 1860", "1 June 1865", true)
			.check().isEmpty());
	}

	@Test
	void aHalfSiblingRoleThatTheParentsDoNotSupportIsAWarning(){
		//a and b are written as half-siblings, but have no parent in common
		final List<Diagnostic> none = family()
			.person("a", "1 June 1870").person("b", "1 June 1872")
			.relationship("a", "p", "biological_child").relationship("b", "q", "biological_child")
			.relationship("a", "b", "associate", "half-brother", null, null, false)
			.check();
		final Diagnostic finding = only(none);
		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Sibling role that the parents do not support: Person a and Person b"));
		Assertions.assertTrue(finding.explanation().contains("no biological parent in common"));

		//both parents in common: they are full siblings, not half-siblings
		final Diagnostic full = only(family()
			.person("a", "1 June 1870").person("b", "1 June 1872")
			.relationship("a", "p", "biological_child").relationship("a", "q", "biological_child")
			.relationship("b", "p", "biological_child").relationship("b", "q", "biological_child")
			.relationship("a", "b", "associate", "half-brother", null, null, false)
			.check());
		Assertions.assertTrue(full.explanation().contains("both biological parents in common"));
	}

	@Test
	void aSiblingRoleThatTheParentsSupportOrCannotJudgeIsNotReported(){
		//a full sibling with both parents in common, a half-sibling with one in common
		Assertions.assertTrue(family()
			.person("a", "1 June 1870").person("b", "1 June 1872")
			.relationship("a", "p", "biological_child").relationship("a", "q", "biological_child")
			.relationship("b", "p", "biological_child").relationship("b", "q", "biological_child")
			.relationship("a", "b", "associate", "sister", null, null, false)
			.check().isEmpty());
		Assertions.assertTrue(family()
			.person("a", "1 June 1870").person("b", "1 June 1872").person("r", "1 June 1846")
			.relationship("a", "p", "biological_child").relationship("a", "q", "biological_child")
			.relationship("b", "p", "biological_child").relationship("b", "r", "biological_child")
			.relationship("a", "b", "associate", "fratellastro", null, null, false)
			.check().isEmpty());
		//one of them has no recorded parent: nothing can be said
		Assertions.assertTrue(family()
			.person("a", "1 June 1870").person("b", "1 June 1872")
			.relationship("a", "p", "biological_child")
			.relationship("a", "b", "associate", "brother", null, null, false)
			.check().isEmpty());
		//a step-sibling is not a sibling by blood
		Assertions.assertTrue(family()
			.person("a", "1 June 1870").person("b", "1 June 1872")
			.relationship("a", "p", "biological_child").relationship("b", "q", "biological_child")
			.relationship("a", "b", "associate", "step-brother", null, null, false)
			.check().isEmpty());
	}

	@Test
	void anAssociationThatSaysAFamilyTieTheProtocolHasATypeForIsReported(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "associate", "father", null, null, false)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Association that says a family tie: Person c and Person p"));
		Assertions.assertTrue(finding.explanation().contains("the type child"));
		Assertions.assertTrue(only(family().relationship("c", "p", "associate", "wife", null, null, false).check())
			.explanation().contains("the type spouse"));
		//a godfather, a grandfather, a father-in-law, a servant: not the plain tie, or not a tie the types have
		for(final String role : List.of("godfather", "grandfather", "father-in-law", "step father", "servant"))
			Assertions.assertTrue(family().relationship("c", "p", "associate", role, null, null, false).check().isEmpty(), role);
	}

	@Test
	void anAssociationBetweenRelativesAddsNothing(){
		final Diagnostic finding = only(family()
			.relationship("c", "p", "biological_child")
			.relationship("p", "c", "associate", "mentor", null, null, false)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Association between relatives: Person p and Person c"));
		Assertions.assertTrue(finding.explanation().contains("biological_child"));
		//between two that are not relatives it is what it should be
		Assertions.assertTrue(family().relationship("c", "q", "associate", "mentor", null, null, false).check().isEmpty());
	}

	@Test
	void aMarriageWithoutARelationshipComesWithADraft(){
		final List<RelationshipFinding> findings = family()
			.event("E1", "marriage", "p", "q")
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
			.event("E1", "marriage", "p", "q")
			.findings().getFirst().draft();

		//the date of the marriage is where the relationship begins, and its sources are the evidence
		Assertions.assertEquals("E1", draft.eventId());
		Assertions.assertTrue(draft.copyDate());
	}

	@Test
	void theInformantOfABirthWithoutARelationshipWithTheChildIsAQuestionWithADraft(){
		final List<RelationshipFinding> findings = family()
			.eventWithRoles("E1", "birth", new RelationshipData.Participant("c", true, "principal"),
				new RelationshipData.Participant("p", false, "informant"))
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
			.eventWithRoles("E1", "birth", new RelationshipData.Participant("c", true, "principal"),
				new RelationshipData.Participant("p", false, "witness"), new RelationshipData.Participant("q", false, "officiant"))
			.check().isEmpty());
		//the informant is already related to the child
		Assertions.assertTrue(family()
			.relationship("c", "p", "step_child")
			.eventWithRoles("E1", "birth", new RelationshipData.Participant("c", true, "principal"),
				new RelationshipData.Participant("p", false, "informant"))
			.check().isEmpty());
		//the informant was born (1900) after the child (1 June 1870): not a parent, but nothing wrong either
		Assertions.assertTrue(new Builder().person("c", "1 June 1870").person("y", "1 June 1900")
			.eventWithRoles("E1", "birth", new RelationshipData.Participant("c", true, "principal"),
				new RelationshipData.Participant("y", false, "informant"))
			.check().isEmpty());
		//the informant of another event is not the declarant of a birth
		Assertions.assertTrue(family()
			.eventWithRoles("E1", "death", new RelationshipData.Participant("c", true, "principal"),
				new RelationshipData.Participant("p", false, "informant"))
			.check().isEmpty());
	}

	@Test
	void theDraftTakesTheTypeFromTheBanns(){
		final RelationshipFinding finding = family()
			.event("E1", "marriage", "p", "q")
			.event("E2", "marriage_bann", "q", "p")
			.findings().getFirst();

		Assertions.assertEquals("religious_spouse", finding.draft().type());
	}

	@Test
	void aMarriageThatHasItsRelationshipIsComplete(){
		for(final String type : List.of("spouse", "civil_spouse", "religious_spouse", "customary_spouse"))
			Assertions.assertTrue(family()
				.relationship("q", "p", type)
				.event("E1", "marriage", "p", "q")
				.check().stream().noneMatch(f -> f.title().startsWith("Marriage without")), type);
		//a cohabitation is not a marriage
		Assertions.assertEquals(1, family()
			.relationship("p", "q", "cohabiting_partner")
			.event("E1", "marriage", "p", "q")
			.findings().stream().filter(f -> f.draft() != null).count());
	}

	@Test
	void theMarriageOfOtherThanTwoIsNotTouched(){
		//a witness is not a principal, and one principal alone cannot be a couple
		final List<Diagnostic> findings = new Builder().person("a", null).person("b", null).person("w", null)
			.event("E1", "marriage", "a")
			.check();
		Assertions.assertTrue(findings.isEmpty(), titles(findings).toString());

		final Builder withWitness = new Builder().person("a", null).person("b", null).person("w", null);
		withWitness.events.add(new RelationshipData.Event("E2", "marriage", null, "",
			List.of(new RelationshipData.Participant("a", true), new RelationshipData.Participant("b", true),
				new RelationshipData.Participant("w", false))));
		Assertions.assertEquals(1, withWitness.findings().size());
		Assertions.assertEquals(List.of("a", "b", "E2"), withWitness.findings().getFirst().diagnostic().recordIds());
	}

	@Test
	void aMarriageOfACoupleWithTwoEventsIsOneFinding(){
		final List<RelationshipFinding> findings = family()
			.event("E1", "marriage", "p", "q")
			.event("E2", "marriage", "q", "p")
			.findings();

		Assertions.assertEquals(1, findings.size());
		Assertions.assertTrue(findings.getFirst().diagnostic().recordIds().containsAll(List.of("E1", "E2")));
	}

	@Test
	void anEngagementThatTheMarriageDidNotClose(){
		final Diagnostic byRelationship = only(family()
			.relationship("p", "q", "engaged_partner")
			.relationship("p", "q", "civil_spouse")
			.check());
		Assertions.assertTrue(byRelationship.title().startsWith("Engagement of a married couple: Person p and Person q"));

		final List<RelationshipFinding> byEvent = family()
			.relationship("p", "q", "engaged_partner")
			.event("E1", "marriage", "p", "q")
			.findings();
		Assertions.assertTrue(byEvent.stream().anyMatch(f -> f.diagnostic().title().startsWith("Engagement of a married couple")));
	}

	@Test
	void anEngagementThatEndedOrThatIsStillOnIsFine(){
		//closed by a date or by the status
		Assertions.assertTrue(family()
			.relationship("p", "q", "engaged_partner", null, "1 June 1860", "1 June 1865", true)
			.relationship("p", "q", "civil_spouse")
			.check().isEmpty());
		Assertions.assertTrue(family()
			.relationship("p", "q", "engaged_partner", null, null, null, true)
			.relationship("p", "q", "civil_spouse")
			.check().isEmpty());
		//the couple did not marry
		Assertions.assertTrue(family().relationship("p", "q", "engaged_partner").check().isEmpty());
	}

	@Test
	void theKeyOfAFindingFollowsItsData(){
		final Diagnostic finding = only(family().relationship("c", "c", "child").check());

		Assertions.assertTrue(finding.isIgnorable());
		Assertions.assertEquals(finding.key(), only(family().relationship("c", "c", "child").check()).key());
		//another type of relationship, another finding
		Assertions.assertNotEquals(finding.key(), only(family().relationship("c", "c", "spouse").check()).key());
	}

	@Test
	void everyFindingHasAKeyAndTheyAreDistinct(){
		final List<Diagnostic> findings = family()
			.relationship("c", "c", "child")
			.relationship("c", "g", "civil_spouse")
			.relationship("c", "p", "biological_child").relationship("c", "p", "biological_child")
			.relationship("p", "q", "civil_spouse", "head", "1 June 1865", "1 June 1860", true)
			.relationship("c", "q", "child").event("E1", "adoption", "c", "q")
			.event("E2", "marriage", "p", "q")
			.check();

		Assertions.assertTrue(findings.size() >= 6, titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(Diagnostic::isIgnorable));
		Assertions.assertEquals(findings.size(), findings.stream().map(Diagnostic::key).distinct().count());
	}

}
