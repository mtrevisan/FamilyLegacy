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

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class FindingLevelsTest{

	private static Diagnostic finding(final String kind){
		return new Diagnostic(Diagnostic.Severity.INFORMATION, AssistantCategory.RESEARCH, "t", "e", List.of("X1"), null, kind + "|X1|");
	}

	@Test
	void theResearchWorkflowIsForTheStandardLevelAndTheContextForTheAdvancedOne(){
		Assertions.assertEquals(UxLevel.STANDARD, FindingLevels.minimumLevel(finding("gap.question_without_activities")));
		Assertions.assertEquals(UxLevel.STANDARD, FindingLevels.minimumLevel(finding("planner.next_step")));
		Assertions.assertEquals(UxLevel.STANDARD, FindingLevels.minimumLevel(finding("conclusion.unresolved_identity")));
		Assertions.assertEquals(UxLevel.ADVANCED, FindingLevels.minimumLevel(finding("context.candidate")));
		Assertions.assertEquals(UxLevel.ADVANCED, FindingLevels.minimumLevel(finding("context.cluster")));
	}

	@Test
	void everythingElseIsForEveryone(){
		for(final String kind : List.of("chronology.child_before_parent", "relationship.self_relationship", "place.no_parent",
				"gap.no_birth", "gap.unsourced_vital_event", "conclusion.multiple_events", "identity.possible_duplicate",
				"context.norm_estimate"))
			Assertions.assertEquals(UxLevel.BASIC, FindingLevels.minimumLevel(finding(kind)), kind);
	}

	@Test
	void aHiddenFindingIsListedWhenTheLevelIsRaised(){
		final List<Diagnostic> findings = List.of(finding("gap.no_birth"), finding("planner.next_step"), finding("context.candidate"));

		Assertions.assertEquals(1, FindingLevels.visibleAt(findings, UxLevel.BASIC).size());
		Assertions.assertEquals(2, FindingLevels.visibleAt(findings, UxLevel.STANDARD).size());
		Assertions.assertEquals(3, FindingLevels.visibleAt(findings, UxLevel.ADVANCED).size());
	}

}
