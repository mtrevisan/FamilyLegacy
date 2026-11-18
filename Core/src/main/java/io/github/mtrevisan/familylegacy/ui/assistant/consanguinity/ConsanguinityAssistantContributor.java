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
package io.github.mtrevisan.familylegacy.ui.assistant.consanguinity;

import io.github.mtrevisan.familylegacy.consanguinity.ConsanguineousUnionDetector;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.RelationshipReader;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;

import java.util.List;
import java.util.Set;


/**
 * The Consanguinity Assistant: it reports the couples whose partners are related, and the individuals whose parents are related (see
 * {@link ConsanguinityChecker}), as diagnostics of the Research Assistant (category Relationships). It only reports: nothing is
 * created or changed by it, and the records of a finding are opened in their usual editors.
 */
public final class ConsanguinityAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(new DiagnosticProvider(){
			@Override
			public List<Diagnostic> diagnose(final AssistantContext context){
				final FLEFModel model = context.model();
				if(model == null)
					return List.of();

				return ConsanguinityChecker.findings(ConsanguineousUnionDetector.detect(model),
					id -> ConsanguineousUnionDetector.nameOf(model, id));
			}

			/**
			 * The slowest of the assistants after the identity one (minutes on a big file): the descent is read from the individuals
			 * (their names too) and from the relationships, and from nothing else, so an edit of an event, a place or a source does not
			 * make it start again.
			 */
			@Override
			public Set<String> dependsOn(){
				return Set.of(IndividualReader.TYPE, RelationshipReader.TYPE);
			}
		});
	}

}
