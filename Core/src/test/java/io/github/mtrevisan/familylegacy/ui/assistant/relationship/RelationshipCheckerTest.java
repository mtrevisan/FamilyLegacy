package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.chronology.DateRange;
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
				role, (from != null? day(from): null), (to != null? day(to): null), (from != null? "from " + from: ""), ended));
			return this;
		}

		Builder event(final String id, final String type, final String... people){
			final List<RelationshipData.Participant> participants = new ArrayList<>();
			for(final String person : people)
				participants.add(new RelationshipData.Participant(person, true));
			events.add(new RelationshipData.Event(id, type, null, "date of " + id, participants));
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

		final List<Diagnostic> findings = builder.check();

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
			.check();

		Assertions.assertEquals(2, findings.size(), titles(findings).toString());
		Assertions.assertTrue(findings.stream().noneMatch(f -> f.title().contains("generic type")));
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
