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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IdentityHypothesisRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


/**
 * "Create Identity Hypothesis Draft": opens the editor of a new IdentityHypothesisRecord with the two individuals, the reasons they
 * look alike as the start of the rationale, and the confidence the similarity suggests.
 * <p>
 * It is a draft, and only a draft: a hypothesis expresses a possibility, not that the two are the same person, and nothing is saved
 * unless the researcher confirms it. Cancelling the editor leaves the model untouched. Nothing is ever merged: the evaluation of the
 * hypothesis is a conclusion of the researcher.
 * </p>
 */
final class CreateIdentityHypothesisDraftAction implements DiagnosticAction{

	private final IdentityDraft draft;


	CreateIdentityHypothesisDraftAction(final IdentityDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t("identity.draft.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records may have changed since the finding was computed: if one is gone there is nothing to start from
		final FLEFRecord left = model.getRecordById(draft.leftId());
		final FLEFRecord right = model.getRecordById(draft.rightId());
		if(left == null || right == null)
			return;

		IdentityHypothesisRecordDialog.createNew(context.owner(), model)
			.withCandidates(left, right, draft.rationale(), draft.confidence())
			.setVisible(true);
	}

}
