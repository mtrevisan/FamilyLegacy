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
package io.github.mtrevisan.familylegacy.assistant.chronology;

import io.github.mtrevisan.familylegacy.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.assistant.AssistantTuning;
import io.github.mtrevisan.familylegacy.assistant.Diagnostic;
import io.github.mtrevisan.familylegacy.assistant.DiagnosticProvider;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;


/**
 * The Chronology Assistant: it reports the chronological inconsistencies of the research (see {@link ChronologyChecker}) as
 * diagnostics of the Research Assistant. It only reports and explains: it never changes the data.
 */
public final class ChronologyAssistantContributor implements AssistantContributor{

	private final Supplier<AssistantTuning> tuning;


	public ChronologyAssistantContributor(){
		this(AssistantTuning::load);
	}

	/** @param tuning gives the numbers of the user (the ages and the gestation: the tests give the defaults, not the preferences) */
	public ChronologyAssistantContributor(final Supplier<AssistantTuning> tuning){
		this.tuning = tuning;
	}


	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(new DiagnosticProvider(){
			@Override
			public List<Diagnostic> diagnose(final AssistantContext context){
				return diagnose(context, fraction -> {});
			}

			/** The reading of the model is the first third of the work (it cannot tell how far it is), the checks are the rest. */
			@Override
			public List<Diagnostic> diagnose(final AssistantContext context, final DoubleConsumer progress){
				if(context.model() == null)
					return List.of();

				progress.accept(0.);
				final ChronologyData data = ChronologyDataLoader.load(context.model());
				return ChronologyChecker.check(data, ChronologyChecker.Rules.of(tuning.get()), 1. / 3., 1., progress);
			}
		});
	}

}
