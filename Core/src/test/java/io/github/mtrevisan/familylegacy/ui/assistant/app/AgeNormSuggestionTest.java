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
