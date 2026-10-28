package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


class IdentityCheckerTest{

	private static DuplicateFinderService.Profile profile(final String id, final String name){
		return new DuplicateFinderService.Profile(id, name, "M", Set.of(), Set.of(), Set.of(), 1850, null, Set.of(), Set.of(),
			Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
	}

	private static DuplicateFinderService.Candidate candidate(final String leftId, final String rightId, final int score){
		return new DuplicateFinderService.Candidate(profile(leftId, "Mario " + leftId), profile(rightId, "Mario " + rightId), score,
			List.of(new DuplicateFinderService.FeatureMatch("Name", 40, "same full name"),
				new DuplicateFinderService.FeatureMatch("Birth year", 15, "1850 and 1851")),
			List.of("Death year: 1900 vs 1920"));
	}

	private static Diagnostic only(final List<DuplicateFinderService.Candidate> candidates){
		final List<Diagnostic> findings = IdentityChecker.check(candidates);
		Assertions.assertEquals(1, findings.size());
		return findings.getFirst();
	}


	@Test
	void theConfidenceFollowsTheScore(){
		Assertions.assertEquals("high", IdentityChecker.confidence(95));
		Assertions.assertEquals("high", IdentityChecker.confidence(IdentityChecker.HIGH_SCORE));
		Assertions.assertEquals("medium", IdentityChecker.confidence(IdentityChecker.HIGH_SCORE - 1));
		Assertions.assertEquals("medium", IdentityChecker.confidence(IdentityChecker.MEDIUM_SCORE));
		Assertions.assertEquals("low", IdentityChecker.confidence(IdentityChecker.MEDIUM_SCORE - 1));
		Assertions.assertEquals("low", IdentityChecker.confidence(60));
	}

	@Test
	void aStrongCandidateIsAWarningAndTheOthersInformation(){
		Assertions.assertEquals(Diagnostic.Severity.WARNING, only(List.of(candidate("I1", "I2", 95))).severity());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, only(List.of(candidate("I1", "I2", 75))).severity());
		Assertions.assertEquals(Diagnostic.Severity.INFORMATION, only(List.of(candidate("I1", "I2", 62))).severity());
	}

	@Test
	void theFindingExplainsTheSimilarityAndTheDifferences(){
		final Diagnostic finding = only(List.of(candidate("I1", "I2", 75)));

		Assertions.assertEquals(AssistantCategory.IDENTITY, finding.category());
		Assertions.assertEquals("Possible duplicate: Mario I1 and Mario I2 (similarity 75)", finding.title());
		Assertions.assertEquals(List.of("I1", "I2"), finding.recordIds());
		Assertions.assertTrue(finding.explanation().contains("similarity score 75"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("medium confidence"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("Name: same full name (+40)"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("Birth year: 1850 and 1851 (+15)"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("Death year: 1900 vs 1920"), finding.explanation());
		//it is said that it is only a possibility, and that nothing is merged
		Assertions.assertTrue(finding.explanation().contains("a possibility, not a probability"));
		Assertions.assertTrue(finding.explanation().contains("nothing is merged"));
	}

	@Test
	void aPairWithNothingToSayIsStillExplained(){
		final DuplicateFinderService.Candidate bare = new DuplicateFinderService.Candidate(profile("I1", "A"), profile("I2", "B"), 61,
			List.of(), List.of());

		final Diagnostic finding = only(List.of(bare));

		Assertions.assertTrue(finding.explanation().contains("look alike in: -"), finding.explanation());
		Assertions.assertTrue(finding.explanation().contains("differ in: -"), finding.explanation());
	}

	@Test
	void theDraftOfTheHypothesisStartsFromTheFinding(){
		final IdentityFinding finding = IdentityChecker.findings(List.of(candidate("I1", "I2", 75))).getFirst();

		Assertions.assertEquals("I1", finding.draft().leftId());
		Assertions.assertEquals("I2", finding.draft().rightId());
		Assertions.assertEquals("medium", finding.draft().confidence());
		Assertions.assertTrue(finding.draft().rationale().startsWith("Proposed by the Identity Assistant (similarity score 75)"),
			finding.draft().rationale());
		Assertions.assertTrue(finding.draft().rationale().contains("Name: same full name (+40)"));
		Assertions.assertTrue(finding.draft().rationale().contains("Death year: 1900 vs 1920"));
	}

	@Test
	void theKeyIsThePairNotTheScore(){
		final String key = only(List.of(candidate("I1", "I2", 75))).key();

		Assertions.assertNotNull(key);
		//the score changes with every source added: the pair the user ignored does not come back for that
		Assertions.assertEquals(key, only(List.of(candidate("I1", "I2", 88))).key());
		//the pair is the same whichever of the two is first
		Assertions.assertEquals(key, only(List.of(candidate("I2", "I1", 75))).key());
		Assertions.assertNotEquals(key, only(List.of(candidate("I1", "I3", 75))).key());
	}

	@Test
	void theRankIsTheScore(){
		Assertions.assertEquals(75., only(List.of(candidate("I1", "I2", 75))).rank());
		Assertions.assertEquals(95., only(List.of(candidate("I1", "I2", 95))).rank());
	}

	@Test
	void anEmptyListOfCandidatesHasNothingToReport(){
		Assertions.assertTrue(IdentityChecker.check(List.of()).isEmpty());
	}

}
