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

import io.github.mtrevisan.familylegacy.ui.assistant.Diagnostic;


/**
 * A finding of the Context Assistant, with the draft that starts what it suggests (only one of the drafts is present).
 *
 * @param diagnostic the finding
 * @param impact     the start of a context impact, or {@code null}
 * @param historic   the start of a historic event, or {@code null}
 */
record ContextFinding(Diagnostic diagnostic, ImpactDraft impact, HistoricDraft historic){

	/**
	 * The start of a ContextImpactRecord.
	 *
	 * @param contextId  the historic event or cultural norm
	 * @param targetId   the record it may influence
	 * @param impactType the type of impact to propose
	 * @param rationale  why, as the first text of the explanation of the impact
	 */
	record ImpactDraft(String contextId, String targetId, String impactType, String rationale){}

	/**
	 * The start of a HistoricEventRecord.
	 *
	 * @param type  the type of historic event to propose
	 * @param title the title
	 */
	record HistoricDraft(String type, String title){}

}
