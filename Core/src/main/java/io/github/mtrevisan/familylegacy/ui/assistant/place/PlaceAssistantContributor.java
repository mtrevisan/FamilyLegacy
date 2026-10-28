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
package io.github.mtrevisan.familylegacy.ui.assistant.place;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;
import io.github.mtrevisan.familylegacy.ui.assistant.identity.CompareRecordsAction;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.List;


/**
 * The Place Assistant: it reports the places that may be the same place, the ones that are part of nothing, the parishes that belong
 * to no diocese and the relationships that cannot be right (see {@link PlaceChecker}), as diagnostics of the Research Assistant. For
 * a missing relationship it offers the draft of a PlaceRelationshipRecord, for two possible duplicates the side-by-side comparison. It
 * only reports and suggests: no place is merged or changed, nothing is saved unless the user saves it in the usual editor.
 */
public final class PlaceAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> {
			if(context.model() == null)
				return List.of();

			return PlaceChecker.findings(PlaceDataLoader.load(context.model())).stream()
				.map(PlaceAssistantContributor::withActions)
				.toList();
		});
	}

	private static Diagnostic withActions(final PlaceFinding finding){
		Diagnostic diagnostic = finding.diagnostic();
		if(finding.draft() != null)
			diagnostic = diagnostic.withAction(new CreatePlaceRelationshipDraftAction(finding.draft()));
		else if(finding.compare().size() == 2)
			diagnostic = diagnostic.withAction(new CompareRecordsAction(finding.compare().get(0), finding.compare().get(1),
				I18N.t("place.compare.title")));
		return diagnostic;
	}

}
