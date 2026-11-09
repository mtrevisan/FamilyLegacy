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
package io.github.mtrevisan.familylegacy.ui.assistant.conclusion;

import io.github.mtrevisan.calendars.CalendarType;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ConfidenceReader;
import io.github.mtrevisan.familylegacy.io.model.readers.EventReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualAttributeReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.io.model.readers.SexType;
import io.github.mtrevisan.familylegacy.io.model.readers.date.DateRange;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


class ConclusionCheckerTest{

	private static DateRange day(final String gregorianDate){
		return DateRange.day(CalendarType.GREGORIAN.parseToJdn(gregorianDate));
	}

	private static ConclusionData.PrincipalEvent event(final String id, final String personId, final String type,
			final DateRange date, final int sources, final String confidence){
		return new ConclusionData.PrincipalEvent(id, personId, type, date, "date of " + id, Set.of(id, "part-" + id), sources,
			confidence, null);
	}

	/** A data set built step by step. */
	private static final class Builder{

		private final Map<String, ConclusionData.Person> persons = new LinkedHashMap<>();
		private final List<ConclusionData.PrincipalEvent> events = new ArrayList<>();
		private final List<ConclusionData.ParentLink> parents = new ArrayList<>();
		private final List<ConclusionData.Attribute> attributes = new ArrayList<>();
		private final List<ConclusionData.Hypothesis> hypotheses = new ArrayList<>();
		private final List<ConclusionData.Conclusion> conclusions = new ArrayList<>();
		private final Map<String, Set<String>> placeAncestors = new LinkedHashMap<>();

		Builder person(final String id, final SexType sex){
			persons.put(id, new ConclusionData.Person(id, "Person " + id, sex));
			return this;
		}

		Builder event(final ConclusionData.PrincipalEvent event){
			events.add(event);
			return this;
		}

		Builder parent(final String child, final String parent, final String type){
			parents.add(new ConclusionData.ParentLink("rel-" + child + "-" + parent + "-" + type, child, parent, type));
			return this;
		}

		Builder attribute(final String id, final String personId, final String type, final String value){
			attributes.add(new ConclusionData.Attribute(id, personId, type, value));
			return this;
		}

		Builder hypothesis(final String id, final String first, final String second){
			hypotheses.add(new ConclusionData.Hypothesis(id, first, second));
			return this;
		}

		Builder conclusion(final String id, final Set<String> resolves, final String preferred, final String status){
			conclusions.add(new ConclusionData.Conclusion(id, resolves, preferred, status));
			return this;
		}

		List<Diagnostic> check(){
			return ConclusionChecker.check(new ConclusionData(persons, events, parents, attributes, hypotheses, conclusions, placeAncestors));
		}

	}

	private static ConclusionData.PrincipalEvent eventAt(final String id, final String personId, final String type, final DateRange date,
			final String placeId){
		return new ConclusionData.PrincipalEvent(id, personId, type, date, "date of " + id, Set.of(id, "part-" + id), 1, null, placeId);
	}

	@Test
	void twoBurialsInUnrelatedPlacesConflictEvenWithTheSameDate(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(eventAt("u1", "a", EventReader.ENUM_TYPE_BURIAL, day("1 June 1800"), "P1"))
			.event(eventAt("u2", "a", EventReader.ENUM_TYPE_BURIAL, day("1 June 1800"), "P2"))
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.explanation().contains("@ P1"));
	}

	@Test
	void aPlaceAndTheOneThatContainsItAgree(){
		final Builder builder = new Builder()
			.person("a", SexType.MALE)
			.event(eventAt("d1", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1800"), "Town"))
			.event(eventAt("d2", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1800"), "Province"));
		builder.placeAncestors.put("Town", Set.of("Province"));

		final Diagnostic finding = only(builder.check());

		//two deaths that agree: information, not a conflict
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
	}

	@Test
	void twoBaptismsOnDifferentDaysConflict(){
		Assertions.assertEquals(Diagnostic.Severity.WARNING, only(new Builder()
			.person("a", SexType.MALE)
			.event(eventAt("p1", "a", "baptism", day("1 June 1800"), null))
			.event(eventAt("p2", "a", "baptism", day("3 June 1800"), null))
			.check()).severity());
	}

	private static ConclusionData.PrincipalEvent weighted(final String id, final int sources, final int weight){
		return new ConclusionData.PrincipalEvent(id, "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), "date of " + id,
			Set.of(id), sources, null, null, weight);
	}

	@Test
	void theEvidenceQualifiersDecideBetweenAsManySources(){
		//one source each, but the second is original, primary and direct: it weighs 4 against 1
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(weighted("b1", 1, 1))
			.event(new ConclusionData.PrincipalEvent("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), "date of b2", Set.of("b2"), 1,
				null, null, 4))
			.check());

		Assertions.assertTrue(finding.explanation().contains("Candidate for the preferred assertion: b2"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("evidence weight 4"), finding.explanation());
	}

	@Test
	void theSameWeightAndConfidenceGiveNoCandidate(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(weighted("b1", 2, 3))
			.event(new ConclusionData.PrincipalEvent("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), "date of b2", Set.of("b2"), 1,
				null, null, 3))
			.check());

		Assertions.assertTrue(finding.explanation().contains("No assertion is better supported"), finding.explanation());
	}

	@Test
	void theConfidenceOfTheResearcherComesBeforeTheWeight(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(new ConclusionData.PrincipalEvent("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), "date of b1", Set.of("b1"), 1,
				ConfidenceReader.ENUM_HIGH, null, 1))
			.event(new ConclusionData.PrincipalEvent("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), "date of b2", Set.of("b2"), 5,
				ConfidenceReader.ENUM_LOW, null, 9))
			.check());

		Assertions.assertTrue(finding.explanation().contains("Candidate for the preferred assertion: b1"), finding.explanation());
	}

	@Test
	void conflictingAttributesAndRelationshipsHaveACandidateToo(){
		final Builder builder = new Builder()
			.person("a", SexType.MALE)
			.person("p", SexType.FEMALE);
		builder.attributes.add(new ConclusionData.Attribute("n1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "AAA",
			new ConclusionData.Backing(null, 1, 1)));
		builder.attributes.add(new ConclusionData.Attribute("n2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "BBB",
			new ConclusionData.Backing(null, 1, 3)));
		builder.parents.add(new ConclusionData.ParentLink("r1", "a", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD,
			new ConclusionData.Backing(null, 1, 1)));
		builder.parents.add(new ConclusionData.ParentLink("r2", "a", "p", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD,
			new ConclusionData.Backing(null, 1, 2)));

		final List<String> explanations = builder.check().stream()
			.map(Diagnostic::explanation)
			.toList();

		Assertions.assertTrue(explanations.stream().anyMatch(e -> e.contains("Candidate for the preferred assertion: n2")), explanations.toString());
		Assertions.assertTrue(explanations.stream().anyMatch(e -> e.contains("Candidate for the preferred assertion: r2")), explanations.toString());
	}

	/** Two births of the same person, one year apart. */
	private static Builder twoBirths(){
		return new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), 1, null));
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


	@Test
	void theKeyOfAFindingFollowsTheConflictNotTheSuggestion(){
		final Diagnostic finding = only(twoBirths().check());
		Assertions.assertTrue(finding.isIgnorable());

		//a source is added to one of the events: the suggestion of the candidate changes, the conflict does not
		final Diagnostic withSupport = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), 4, ConfidenceReader.ENUM_HIGH))
			.check());
		Assertions.assertNotEquals(finding.explanation(), withSupport.explanation());
		Assertions.assertEquals(finding.key(), withSupport.key());

		//a third birth, or another date: it is another conflict
		Assertions.assertNotEquals(finding.key(), only(twoBirths()
			.event(event("b3", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1802"), 1, null)).check()).key());
		Assertions.assertNotEquals(finding.key(), only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(new ConclusionData.PrincipalEvent("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1805"), "1 JUN 1805", Set.of("b2", "part-b2"), 1,
				null, null))
			.check()).key());
	}

	@Test
	void everyKindOfFindingHasAKey(){
		final List<Diagnostic> findings = new Builder()
			.person("a", SexType.MALE).person("c", SexType.MALE).person("m1", SexType.FEMALE).person("m2", SexType.FEMALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), 1, null))
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "m2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD)
			.parent("a", "c", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "a", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.attribute("t1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X1").attribute("t2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X2")
			.hypothesis("h", "a", "c")
			.check();

		Assertions.assertTrue(findings.size() >= 6, titles(findings).toString());
		Assertions.assertTrue(findings.stream().allMatch(Diagnostic::isIgnorable));
		Assertions.assertEquals(findings.size(), findings.stream().map(Diagnostic::key).distinct().count());
	}

	@Test
	void aSingleBirthHasNothingToResolve(){
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("d1", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1860"), 1, null))
			.parent("a", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().isEmpty());
	}

	@Test
	void birthsWithDifferentDatesConflict(){
		final Diagnostic finding = only(twoBirths().check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertEquals(AssistantCategory.CONCLUSIONS, finding.category());
		Assertions.assertTrue(finding.title().startsWith("Conflicting birth dates: Person a"));
		Assertions.assertEquals(List.of("a", "b1", "b2"), finding.recordIds());
		//the draft of the conclusion is suggested, and it is only a suggestion
		Assertions.assertTrue(finding.explanation().contains("ConclusionRecord"));
		Assertions.assertTrue(finding.explanation().contains("b1") && finding.explanation().contains("b2"));
	}

	@Test
	void deathsWithDifferentDatesConflict(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("d1", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1800"), 1, null))
			.event(event("d2", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1810"), 1, null))
			.check());

		Assertions.assertTrue(finding.title().startsWith("Conflicting death dates"));
	}

	@Test
	void birthsWithCompatibleDatesAreOnlyReported(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Several birth events"));
	}

	@Test
	void anUndatedEventDoesNotMakeAConflict(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, null))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, null, 1, null))
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
	}

	@Test
	void theBetterSupportedAssertionIsSuggested(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 1, ConfidenceReader.ENUM_LOW))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), 3, ConfidenceReader.ENUM_HIGH))
			.check());

		Assertions.assertTrue(finding.explanation().contains("Candidate for the preferred assertion: b2"), finding.explanation());

		//with the same support there is no candidate
		final Diagnostic tie = only(new Builder()
			.person("a", SexType.MALE)
			.event(event("b1", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1800"), 2, ConfidenceReader.ENUM_MEDIUM))
			.event(event("b2", "a", EventReader.ENUM_TYPE_BIRTH, day("1 June 1801"), 2, ConfidenceReader.ENUM_MEDIUM))
			.check());
		Assertions.assertTrue(tie.explanation().contains("No assertion is better supported"), tie.explanation());
		Assertions.assertFalse(tie.explanation().contains("Candidate for the preferred assertion"));
	}

	@Test
	void aConclusionSettlesTheConflict(){
		//it can point to the events or to the participations
		Assertions.assertTrue(twoBirths().conclusion("c1", Set.of("b1", "b2"), "b1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED).check().isEmpty());
		Assertions.assertTrue(twoBirths().conclusion("c1", Set.of("part-b1", "part-b2"), "part-b2", ConclusionReader.ENUM_PROOF_STATUS_PROVEN).check().isEmpty());
	}

	@Test
	void aConclusionThatDoesNotSettleTheConflict(){
		//it evaluates only one of the assertions
		Assertions.assertEquals(1, twoBirths().conclusion("c1", Set.of("b1"), "b1", ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED).check().size());
		//it does not prefer one of them
		Assertions.assertEquals(1, twoBirths().conclusion("c1", Set.of("b1", "b2"), null, ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED).check().size());
		//the evidence is conflicting: the researcher has evaluated them, and none prevails
		Assertions.assertTrue(twoBirths().conclusion("c1", Set.of("b1", "b2"), null, ConclusionReader.ENUM_PROOF_STATUS_CONFLICTING_EVIDENCE).check().isEmpty());
		//a conclusion without a proof status is not an evaluation
		Assertions.assertEquals(1, twoBirths().conclusion("c1", Set.of("b1", "b2"), null, null).check().size());
	}

	@Test
	void moreThanTwoBiologicalParents(){
		final Diagnostic finding = only(new Builder()
			.person("c", SexType.MALE).person("p1", SexType.MALE).person("p2", SexType.FEMALE).person("p3", SexType.MALE)
			.parent("c", "p1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p3", RelationshipReader.ENUM_TYPE_CHILD)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("More than two biological parents: Person c"));
		Assertions.assertTrue(finding.recordIds().contains("rel-c-p3-child"));
	}

	@Test
	void adoptiveAndStepParentsDoNotCount(){
		Assertions.assertTrue(new Builder()
			.person("c", SexType.MALE).person("p1", SexType.MALE).person("p2", SexType.FEMALE).person("p3", SexType.MALE)
			.parent("c", "p1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p3", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD)
			.parent("c", "p3", RelationshipReader.ENUM_TYPE_STEP_CHILD)
			.check().isEmpty());
	}

	@Test
	void twoBiologicalMothers(){
		final Diagnostic finding = only(new Builder()
			.person("c", SexType.MALE).person("m1", SexType.FEMALE).person("m2", SexType.FEMALE)
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "m2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check());

		Assertions.assertTrue(finding.title().startsWith("Two biological mothers: Person c"));
		//a mother and a parent of unknown sex are possible
		Assertions.assertTrue(new Builder()
			.person("c", SexType.MALE).person("m1", SexType.FEMALE).person("m2", SexType.UNKNOWN)
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "m2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().isEmpty());
	}

	@Test
	void aConclusionSettlesTheParentage(){
		Assertions.assertTrue(new Builder()
			.person("c", SexType.MALE).person("p1", SexType.MALE).person("p2", SexType.FEMALE).person("p3", SexType.MALE)
			.parent("c", "p1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.conclusion("k", Set.of("rel-c-p1-biological_child", "rel-c-p3-biological_child"), "rel-c-p1-biological_child",
				ConclusionReader.ENUM_PROOF_STATUS_SUPPORTED)
			.check().isEmpty());
	}

	@Test
	void biologicalAndAdoptiveRelationshipOfTheSamePair(){
		final Diagnostic finding = only(new Builder()
			.person("c", SexType.MALE).person("p", SexType.MALE)
			.parent("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.WARNING, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Conflicting relationships: Person c and Person p"));
		//an adoptive and a step relationship can coexist
		Assertions.assertTrue(new Builder()
			.person("c", SexType.MALE).person("p", SexType.MALE)
			.parent("c", "p", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD).parent("c", "p", RelationshipReader.ENUM_TYPE_STEP_CHILD)
			.check().isEmpty());
	}

	@Test
	void anIndividualCannotBeTheirOwnAncestor(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE).person("c", SexType.MALE).person("x", SexType.MALE)
			.parent("a", "b", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("b", "c", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "a", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD)
			.parent("x", "a", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check());

		Assertions.assertEquals(Diagnostic.Severity.ERROR, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Own ancestor"));
		//the three of the cycle, not the descendant that hangs from it
		Assertions.assertTrue(finding.recordIds().containsAll(List.of("a", "b", "c")));
		Assertions.assertFalse(finding.recordIds().contains("x"));
	}

	@Test
	void aCycleIsReportedOnce(){
		Assertions.assertEquals(1, new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE)
			.parent("a", "b", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("b", "a", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().stream().filter(f -> f.title().startsWith("Own ancestor")).count());
	}

	@Test
	void stepRelationshipsDoNotMakeACycle(){
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE)
			.parent("a", "b", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("b", "a", RelationshipReader.ENUM_TYPE_STEP_CHILD)
			.check().isEmpty());
	}

	@Test
	void conflictingNationalIds(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE)
			.attribute("t1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "RSS MRA 80A01 H501Z")
			.attribute("t2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "RSS MRA 80A01 H501X")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Conflicting national_id: Person a"));
		//the same value written differently is the same value; other attributes can have several values
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE)
			.attribute("t1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "RSSMRA80A01H501Z")
			.attribute("t2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "rss-mra 80a01 h501z")
			.attribute("o1", "a", IndividualAttributeReader.ENUM_TYPE_OCCUPATION, "farmer")
			.attribute("o2", "a", IndividualAttributeReader.ENUM_TYPE_OCCUPATION, "miller")
			.check().isEmpty());
	}

	@Test
	void conflictingSexAssertions(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.UNKNOWN)
			.attribute("s1", "a", IndividualAttributeReader.ENUM_TYPE_SEX, "male")
			.attribute("s2", "a", IndividualAttributeReader.ENUM_TYPE_SEX, "female")
			.check());

		Assertions.assertTrue(finding.title().startsWith("Conflicting sex: Person a"));
		//an unknown sex says nothing, so it does not contradict a known one; the same sex stated twice is not a conflict
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE)
			.attribute("s1", "a", IndividualAttributeReader.ENUM_TYPE_SEX, "male")
			.attribute("s2", "a", IndividualAttributeReader.ENUM_TYPE_SEX, "unknown")
			.attribute("s3", "a", IndividualAttributeReader.ENUM_TYPE_SEX, "Male")
			.check().isEmpty());
	}

	@Test
	void unresolvedIdentityHypothesis(){
		final Diagnostic finding = only(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE)
			.hypothesis("h", "a", "b")
			.check());

		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, finding.severity());
		Assertions.assertTrue(finding.title().startsWith("Unresolved identity hypothesis: Person a and Person b"));
		Assertions.assertEquals(List.of("h", "a", "b"), finding.recordIds());

		//any evaluation of the hypothesis, whatever its outcome, resolves it
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE)
			.hypothesis("h", "a", "b")
			.conclusion("k", Set.of("h"), null, ConclusionReader.ENUM_PROOF_STATUS_DISPROVEN)
			.check().isEmpty());
	}

	private static ConclusionData data(final Builder builder){
		return new ConclusionData(builder.persons, builder.events, builder.parents, builder.attributes, builder.hypotheses,
			builder.conclusions, Map.of());
	}

	@Test
	void theDraftOfAConflictedEvent(){
		final ConclusionFinding finding = ConclusionChecker.findings(data(twoBirths())).getFirst();

		Assertions.assertEquals("birth_date", finding.draft().issue());
		//the events, not their participations
		Assertions.assertEquals(List.of("b1", "b2"), finding.draft().resolves());
		Assertions.assertTrue(finding.draft().narrative().startsWith("Conflicting birth dates: Person a."));
		//the draft starts the narrative with the reasoning of the finding, without the sentence that proposes the draft itself
		Assertions.assertTrue(finding.draft().narrative().contains("b1"));
		Assertions.assertFalse(finding.draft().narrative().contains("A ConclusionRecord can resolve it"));
		Assertions.assertTrue(finding.diagnostic().explanation().contains("A ConclusionRecord can resolve it"));

		//a death, and events that agree
		Assertions.assertEquals(EventReader.ENUM_TYPE_DEATH, ConclusionChecker.findings(data(new Builder()
			.person("a", SexType.MALE)
			.event(event("d1", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1800"), 1, null))
			.event(event("d2", "a", EventReader.ENUM_TYPE_DEATH, day("1 June 1800"), 1, null)))).getFirst().draft().issue());
	}

	@Test
	void theDraftsOfTheOtherConflicts(){
		final ConclusionFinding parentage = ConclusionChecker.findings(data(new Builder()
			.person("c", SexType.MALE).person("m1", SexType.FEMALE).person("m2", SexType.FEMALE)
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "m2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD))).getFirst();
		Assertions.assertEquals("parentage", parentage.draft().issue());
		Assertions.assertEquals(List.of("rel-c-m1-biological_child", "rel-c-m2-biological_child"), parentage.draft().resolves());

		final ConclusionFinding relationship = ConclusionChecker.findings(data(new Builder()
			.person("c", SexType.MALE).person("p", SexType.MALE)
			.parent("c", "p", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "p", RelationshipReader.ENUM_TYPE_ADOPTIVE_CHILD))).getFirst();
		Assertions.assertEquals("relationship_type", relationship.draft().issue());
		Assertions.assertEquals(2, relationship.draft().resolves().size());

		final ConclusionFinding cycle = ConclusionChecker.findings(data(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE)
			.parent("a", "b", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("b", "a", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD))).getFirst();
		Assertions.assertEquals("parentage", cycle.draft().issue());
		Assertions.assertEquals(2, cycle.draft().resolves().size());

		final ConclusionFinding attribute = ConclusionChecker.findings(data(new Builder()
			.person("a", SexType.MALE)
			.attribute("t1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X1").attribute("t2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X2"))).getFirst();
		Assertions.assertEquals(IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, attribute.draft().issue());
		Assertions.assertEquals(List.of("t1", "t2"), attribute.draft().resolves());

		final ConclusionFinding hypothesis = ConclusionChecker.findings(data(new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE).hypothesis("h", "a", "b"))).getFirst();
		Assertions.assertEquals("identity", hypothesis.draft().issue());
		Assertions.assertEquals(List.of("h"), hypothesis.draft().resolves());
	}

	@Test
	void aDraftSavedAsItIsDoesNotSettleTheConflict(){
		//the draft has no preferred assertion and no proof status: until the researcher judges, the conflict is open
		Assertions.assertEquals(1, twoBirths().conclusion("draft", Set.of("b1", "b2"), null, null).check().size());
		Assertions.assertEquals(1, new Builder()
			.person("a", SexType.MALE).person("b", SexType.MALE).hypothesis("h", "a", "b")
			.check().size());
	}

	@Test
	void everyFindingHasADraft(){
		final Builder builder = twoBirths()
			.person("c", SexType.MALE).person("m1", SexType.FEMALE).person("m2", SexType.FEMALE)
			.parent("c", "m1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("c", "m2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.attribute("t1", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X1").attribute("t2", "a", IndividualAttributeReader.ENUM_TYPE_NATIONAL_ID, "X2")
			.hypothesis("h", "a", "c");

		final List<ConclusionFinding> findings = ConclusionChecker.findings(data(builder));

		Assertions.assertEquals(4, findings.size());
		for(final ConclusionFinding finding : findings){
			Assertions.assertFalse(finding.draft().issue().isBlank());
			Assertions.assertFalse(finding.draft().resolves().isEmpty());
			Assertions.assertFalse(finding.draft().narrative().isBlank());
		}
		Assertions.assertEquals(findings.stream().map(f -> f.diagnostic().title()).toList(), titles(builder.check()));
	}

	@Test
	void peopleLeftOutOfTheDataAreNeverNamed(){
		//a person hidden by privacy is not loaded: the findings about it are not reported
		Assertions.assertTrue(new Builder()
			.person("a", SexType.MALE)
			.hypothesis("h", "a", "hidden")
			.parent("a", "hidden1", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD).parent("a", "hidden2", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.parent("a", "hidden3", RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD)
			.check().isEmpty());
	}

}
