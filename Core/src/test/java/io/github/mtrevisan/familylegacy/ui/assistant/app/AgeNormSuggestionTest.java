package io.github.mtrevisan.familylegacy.ui.assistant.app;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantCategory;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.features.Suggestion;
import io.github.mtrevisan.familylegacy.ui.features.SuggestionKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class AgeNormSuggestionTest{

	private static Diagnostic report(final String kind, final String recordId){
		return new Diagnostic(Diagnostic.Severity.WARNING, AssistantCategory.DIAGNOSTICS, "t", "e", List.of(recordId), null,
			kind + "|" + recordId + "|");
	}

	@Test
	void anUnusualAgeSuggestsACustomEvenThoughTheChronologyOnlyReports(){
		final List<Suggestion> suggestions = AssistantContextualSuggestions.suggestions(List.of(
			report("chronology.marriage_too_young", "EV1"), report("chronology.child_before_parent", "EV1")), "EV1",
			new AssistantContext(null, null, null), UxLevel.STANDARD);

		Assertions.assertEquals(List.of(SuggestionKind.AGE_NORM), suggestions.stream().map(Suggestion::kind).toList());
	}

}
