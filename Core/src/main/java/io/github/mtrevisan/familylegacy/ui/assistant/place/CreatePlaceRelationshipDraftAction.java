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

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.PlaceRelationshipRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;


/**
 * "Create Place Relationship Draft": opens the editor of a new PlaceRelationshipRecord with the place, the place that contains it (if
 * the assistant found one) and the type of the relationship already in it.
 * <p>
 * It is a draft, and only a draft: nothing is saved unless the researcher completes it and confirms it, and cancelling the editor
 * leaves the model untouched. The places are never changed.
 * </p>
 */
final class CreatePlaceRelationshipDraftAction implements DiagnosticAction{

	private final PlaceDraft draft;


	CreatePlaceRelationshipDraftAction(final PlaceDraft draft){
		this.draft = draft;
	}


	@Override
	public String title(){
		return I18N.t("place.draft.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		if(model == null)
			return;

		//the records may have changed since the finding was computed
		final FLEFRecord subject = model.getRecordById(draft.subjectId());
		if(subject == null)
			return;

		final FLEFRecord target = (draft.targetId() != null? model.getRecordById(draft.targetId()): null);
		PlaceRelationshipRecordDialog.createNew(context.owner(), model)
			.withDraft(subject, target, draft.type())
			.setVisible(true);
	}

}
