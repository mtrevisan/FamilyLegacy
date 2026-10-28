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

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;

import java.util.List;


/**
 * The Conclusion Assistant: it reports the conflicts of the research that no ConclusionRecord evaluates yet (see
 * {@link ConclusionChecker}) as diagnostics of the Research Assistant. It only reports and suggests: the assertions are never
 * changed, and a conclusion is always written by the researcher.
 */
public final class ConclusionAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> {
			if(context.model() == null)
				return List.of();

			//each finding comes with the action that starts the draft of the conclusion that would resolve it
			return ConclusionChecker.findings(ConclusionDataLoader.load(context.model())).stream()
				.map(finding -> {
					final Diagnostic diagnostic = finding.diagnostic();
					return new Diagnostic(diagnostic.severity(), diagnostic.category(), diagnostic.title(), diagnostic.explanation(),
						diagnostic.recordIds(), new CreateConclusionDraftAction(finding.draft()), diagnostic.key());
				})
				.toList();
		});
	}

}
