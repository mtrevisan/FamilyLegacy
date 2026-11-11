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
