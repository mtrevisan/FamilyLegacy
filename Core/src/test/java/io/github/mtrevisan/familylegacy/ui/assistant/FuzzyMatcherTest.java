package io.github.mtrevisan.familylegacy.ui.assistant;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class FuzzyMatcherTest{

	@Test
	void emptyQueryMatchesEverything(){
		Assertions.assertTrue(FuzzyMatcher.score("", "Historical Date Assistant") > 0);
		Assertions.assertTrue(FuzzyMatcher.score("  ", "anything") > 0);
		Assertions.assertTrue(FuzzyMatcher.score(null, "anything") > 0);
	}

	@Test
	void charactersMustAppearInOrder(){
		Assertions.assertTrue(FuzzyMatcher.score("hda", "Historical Date Assistant") > 0);
		Assertions.assertEquals(FuzzyMatcher.NO_MATCH, FuzzyMatcher.score("adh", "Historical Date Assistant"));
		Assertions.assertEquals(FuzzyMatcher.NO_MATCH, FuzzyMatcher.score("xyz", "Historical Date Assistant"));
		Assertions.assertEquals(FuzzyMatcher.NO_MATCH, FuzzyMatcher.score("a", null));
	}

	@Test
	void caseIsIgnored(){
		Assertions.assertEquals(FuzzyMatcher.score("EASTER", "easter calculator"), FuzzyMatcher.score("easter", "EASTER CALCULATOR"));
	}

	@Test
	void consecutiveCharactersScoreMore(){
		Assertions.assertTrue(FuzzyMatcher.score("date", "Date Assistant") > FuzzyMatcher.score("date", "Data Estimator"));
		Assertions.assertTrue(FuzzyMatcher.score("age", "Age Estimator") > FuzzyMatcher.score("age", "Calendar Gregorian Era"));
	}

	@Test
	void wordStartsScoreMore(){
		Assertions.assertTrue(FuzzyMatcher.score("ass", "Date Assistant") > FuzzyMatcher.score("ass", "Date Classification"));
	}

	@Test
	void whitespaceInTheQueryIsIgnored(){
		Assertions.assertTrue(FuzzyMatcher.score("date ass", "Date Assistant") > 0);
	}

}
