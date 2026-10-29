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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ContextImpactRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


/**
 * "Create Context Impact Draft": opens the editor of a new ContextImpactRecord with the historic event or cultural norm, the event of
 * the tree, the type of impact and the start of the explanation already in it.
 * <p>
 * It is a draft, and only a draft: nothing is saved unless the researcher completes it (the sources are theirs to give) and confirms it,
 * and cancelling the editor leaves the model untouched. The assistant never creates an impact on its own.
 * </p>
 */
final class CreateContextImpactDraftAction implements DiagnosticAction{

	private final ContextFinding.ImpactDraft draft;


	CreateContextImpactDraftAction(final ContextFinding.ImpactDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t("context.impact.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records may have changed since the finding was computed
		final FLEFRecord source = model.getRecordById(draft.contextId());
		final FLEFRecord target = model.getRecordById(draft.targetId());
		if(source == null || target == null)
			return;

		ContextImpactRecordDialog.createNew(context.owner(), model)
			.withDraft(source, target, draft.impactType(), draft.rationale())
			.setVisible(true);
	}

}
