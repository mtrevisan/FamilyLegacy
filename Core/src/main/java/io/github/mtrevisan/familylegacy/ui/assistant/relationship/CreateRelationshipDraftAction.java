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
package io.github.mtrevisan.familylegacy.ui.assistant.relationship;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.RelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


/**
 * "Create Relationship Draft": opens the editor of a new RelationshipRecord with the two individuals already in it (and the type, if
 * the evidence points to one).
 * <p>
 * It is a draft, and only a draft: nothing is saved unless the researcher completes the relationship and confirms it, and cancelling
 * the editor leaves the model untouched. The assistant never creates a relationship on its own.
 * </p>
 */
final class CreateRelationshipDraftAction implements DiagnosticAction{

	private final RelationshipDraft draft;


	CreateRelationshipDraftAction(final RelationshipDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t("relationship.draft.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records may have changed since the finding was computed: if one is gone there is nothing to start from
		final FLEFRecord subject = model.getRecordById(draft.subjectId());
		final FLEFRecord target = model.getRecordById(draft.targetId());
		if(subject == null || target == null)
			return;

		RelationshipRecordDialog.createNew(context.owner(), model)
			.withParticipants(subject, target, draft.type())
			.setVisible(true);
	}

}
