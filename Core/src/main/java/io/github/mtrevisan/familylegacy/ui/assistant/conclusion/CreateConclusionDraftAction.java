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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ConclusionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


/**
 * "Create Conclusion Draft": opens the editor of a new ConclusionRecord with the competing records of the finding already in its
 * RESOLVES and the description of the conflict as the start of the narrative.
 * <p>
 * It is a draft, and only a draft: the proof status is {@code unresearched}, no assertion is preferred, and nothing is saved unless
 * the researcher completes the conclusion and confirms it. Cancelling the editor leaves the model untouched.
 * </p>
 */
final class CreateConclusionDraftAction implements DiagnosticAction{

	private static final String PROOF_STATUS_UNRESEARCHED = "unresearched";

	private final ConclusionDraft draft;


	CreateConclusionDraftAction(final ConclusionDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t("conclusion.draft.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		final ConclusionRecordDialog dialog = ConclusionRecordDialog.createNew(context.owner(), model)
			.withDraft(PROOF_STATUS_UNRESEARCHED, draft.narrative());
		//the records of the finding may have changed since it was computed: the ones that are gone are left out
		for(final String id : draft.resolves()){
			final FLEFRecord record = model.getRecordById(id);
			if(record != null)
				dialog.withResolvedTarget(record, draft.issue());
		}
		dialog.setVisible(true);
	}

}
