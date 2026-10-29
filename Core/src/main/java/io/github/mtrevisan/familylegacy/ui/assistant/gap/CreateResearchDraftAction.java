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
package io.github.mtrevisan.familylegacy.ui.assistant.gap;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchQuestionRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.ResearchTaskRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;

import java.util.List;


/**
 * "Create Research Question Draft" or "Create Research Task Draft": opens the editor of a new ResearchQuestionRecord (what to find out)
 * or ResearchTaskRecord (what to do next) with what the gap suggests already in it: the title and the question or the description,
 * the individuals the question is about, the questions the task supports and its priority.
 * <p>
 * It is a draft, and only a draft: nothing is saved unless the researcher completes it and confirms it, and cancelling the editor
 * leaves the model untouched. The assistant never creates a question or a task on its own.
 * </p>
 */
public final class CreateResearchDraftAction implements DiagnosticAction{

	private final GapDraft draft;


	public CreateResearchDraftAction(final GapDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t(draft.kind() == GapDraft.Kind.QUESTION? "gap.draft.question.action": "gap.draft.task.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records may have changed since the finding was computed: the ones that are gone are left out
		final List<FLEFRecord> records = draft.records().stream()
			.map(model::getRecordById)
			.filter(record -> record != null)
			.toList();
		if(draft.kind() == GapDraft.Kind.QUESTION)
			ResearchQuestionRecordDialog.createNew(context.owner(), model)
				.withDraft(draft.title(), draft.text(), records)
				.setVisible(true);
		else
			ResearchTaskRecordDialog.createNew(context.owner(), model)
				.withDraft(draft.text(), draft.priority(), records)
				.setVisible(true);
	}

}
