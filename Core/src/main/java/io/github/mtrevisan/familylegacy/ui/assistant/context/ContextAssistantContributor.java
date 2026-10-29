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
package io.github.mtrevisan.familylegacy.ui.assistant.context;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;

import java.util.List;


/**
 * The Context Assistant: it reports the historic events and the cultural norms of the model that may have influenced the events of the
 * tree, and the deaths in a place that no historic event of the model explains (see {@link ContextChecker}), as diagnostics of the
 * Research Assistant. For an impact it offers the draft of a ContextImpactRecord, for a crisis the draft of a HistoricEventRecord. It
 * only suggests: no record is created or changed, nothing is saved unless the user saves it in the usual editor.
 */
public final class ContextAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> {
			if(context.model() == null)
				return List.of();

			return ContextChecker.findings(ContextDataLoader.load(context.model())).stream()
				.map(ContextAssistantContributor::withAction)
				.toList();
		});
	}

	private static Diagnostic withAction(final ContextFinding finding){
		final Diagnostic diagnostic = finding.diagnostic();
		if(finding.impact() != null)
			return diagnostic.withAction(new CreateContextImpactDraftAction(finding.impact()));
		if(finding.historic() != null)
			return diagnostic.withAction(new CreateHistoricEventDraftAction(finding.historic()));
		return diagnostic;
	}

}
