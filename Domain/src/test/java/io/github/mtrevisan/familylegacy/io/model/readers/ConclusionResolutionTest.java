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
package io.github.mtrevisan.familylegacy.io.model.readers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;


/** One definition of "settled", for the merger and for the Conclusion Assistant. */
class ConclusionResolutionTest{

	private static final Set<String> COMPETING = Set.of("E1", "E2", "P1");


	private static ConclusionResolution.Evaluation evaluation(final Set<String> resolves, final String preferred, final String status){
		return new ConclusionResolution.Evaluation(resolves, preferred, status);
	}


	@Test
	void aConclusionThatPrefersOneOfThemSettlesTheConflict(){
		Assertions.assertTrue(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "E2"), "E1", "supported"))));
	}

	@Test
	void aConclusionThatSaysThatNonePrevailsSettlesTheConflict(){
		Assertions.assertTrue(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "E2"), null, "conflicting_evidence"))));
		Assertions.assertTrue(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "E2"), null, "disproven"))));
	}

	@Test
	void aDraftDoesNotSettleAnything(){
		Assertions.assertFalse(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "E2"), null, null))));
	}

	@Test
	void theConclusionMustEvaluateAtLeastTwoOfTheCompetingRecords(){
		Assertions.assertFalse(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "X9"), "E1", "proven"))));
		Assertions.assertFalse(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1"), null, "disproven"))));
	}

	@Test
	void thePreferredAssertionMustBeOneOfTheCompetingRecords(){
		Assertions.assertFalse(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "E2"), "X9", "supported"))));
	}

	@Test
	void theParticipationsCountAsCompetingRecords(){
		Assertions.assertTrue(ConclusionResolution.isResolved(COMPETING,
			List.of(evaluation(Set.of("E1", "P1"), "P1", "supported"))));
	}

	@Test
	void noConclusionsMeansNothingIsSettled(){
		Assertions.assertFalse(ConclusionResolution.isResolved(COMPETING, List.of()));
	}

}
