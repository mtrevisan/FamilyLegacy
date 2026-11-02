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
