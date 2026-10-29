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
package io.github.mtrevisan.familylegacy.ui.assistant.planner;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.CreateResearchDraftAction;
import io.github.mtrevisan.familylegacy.ui.assistant.gap.GapDataLoader;

import java.time.LocalDate;
import java.util.List;


/**
 * The Research Planner: it proposes the next steps of the open research questions and the brick walls worth working on, each with the
 * reason and an estimated usefulness (see {@link PlannerChecker}), as diagnostics of the Research Assistant. It only suggests: the
 * draft of a research task (a step) or of a research question (a brick wall) is the start of the usual editor, and nothing is saved
 * unless the user saves it.
 */
public final class PlannerAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> {
			if(context.model() == null)
				return List.of();

			return PlannerChecker.findings(GapDataLoader.load(context.model(), LocalDate.now())).stream()
				.map(finding -> {
					final Diagnostic diagnostic = finding.diagnostic();
					return (finding.draft() != null
						? diagnostic.withAction(new CreateResearchDraftAction(finding.draft()))
						: diagnostic);
				})
				.toList();
		});
	}

}
