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
package io.github.mtrevisan.familylegacy.ui.assistant.chronology;

import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContributor;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticProvider;

import java.util.List;


/**
 * The Chronology Assistant: it reports the chronological inconsistencies of the research (see {@link ChronologyChecker}) as
 * diagnostics of the Research Assistant. It only reports and explains: it never changes the data.
 */
public final class ChronologyAssistantContributor implements AssistantContributor{

	@Override
	public List<AssistantAction> actions(){
		return List.of();
	}

	@Override
	public List<DiagnosticProvider> diagnosticProviders(){
		return List.of(context -> (context.model() != null
			? ChronologyChecker.check(ChronologyDataLoader.load(context.model()))
			: List.of()));
	}

}
