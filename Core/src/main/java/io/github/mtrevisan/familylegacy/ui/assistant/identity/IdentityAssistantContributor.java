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
package io.github.mtrevisan.familylegacy.ui.assistant.identity;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.tools.DuplicateFinderService;

import java.util.List;


/**
 * The Identity Assistant: it proposes the pairs of individuals that may be the same person (see {@link IdentityChecker}) as
 * diagnostics of the Research Assistant, each with the reasons, a draft of the identity hypothesis that would record the possibility
 * and the side by side comparison of the two records. It never merges records and never creates a hypothesis on its own: the editor
 * it opens is the usual one and nothing is saved unless the user saves it.
 */
public final class IdentityAssistantContributor implements AssistantContributor{

	/** The similarity score from which a pair is proposed (a little above the default of the duplicate finder: fewer, better pairs). */
	static final int THRESHOLD = 60;
	/** The most pairs proposed: the ones with the highest score. */
	static final int MAX_CANDIDATES = 100;


	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> {
			if(context.model() == null)
				return List.of();

			final DuplicateFinderService.Options options = new DuplicateFinderService.Options();
			options.threshold = THRESHOLD;
			options.maxResults = MAX_CANDIDATES;
			//a pair that already has an identity hypothesis is not proposed again: the hypothesis has its own findings
			options.skipExistingHypotheses = true;
			final List<DuplicateFinderService.Candidate> candidates = DuplicateFinderService.find(context.model(), options);

			return IdentityChecker.findings(candidates).stream()
				.map(finding -> {
					final Diagnostic diagnostic = finding.diagnostic();
					final IdentityDraft draft = finding.draft();
					final DiagnosticAction compare = new CompareRecordsAction(draft.leftId(), draft.rightId(),
						I18N.tf("identity.compare.title", name(candidates, draft.leftId()), name(candidates, draft.rightId())));
					return diagnostic.withAction(new CreateIdentityHypothesisDraftAction(draft))
						.withAlternatives(List.of(compare));
				})
				.toList();
		});
	}


	private static String name(final List<DuplicateFinderService.Candidate> candidates, final String id){
		for(final DuplicateFinderService.Candidate candidate : candidates){
			if(candidate.left().id().equals(id))
				return candidate.left().displayName();
			if(candidate.right().id().equals(id))
				return candidate.right().displayName();
		}
		return id;
	}

}
