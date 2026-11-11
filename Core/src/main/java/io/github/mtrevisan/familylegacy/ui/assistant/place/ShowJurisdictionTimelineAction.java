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

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantContext;
import io.github.mtrevisan.familylegacy.ui.assistant.DiagnosticAction;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContexts;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.groups.GroupViewDialog;


/**
 * "Review the Timeline": opens the timeline of the jurisdictions of the place of the finding (the one of the Place menu). It only
 * shows: nothing is created or changed.
 */
final class ShowJurisdictionTimelineAction implements DiagnosticAction{

	private final String placeId;


	ShowJurisdictionTimelineAction(final String placeId){
		this.placeId = placeId;
	}


	@Override
	public String title(){
		return I18N.t("place.jurisdiction_changed.action");
	}

	@Override
	public void execute(final AssistantContext context){
		final FLEFModel model = context.model();
		//the place may have been deleted since the finding was computed
		if(model == null || model.getRecordById(placeId) == null)
			return;

		GroupViewDialog.forPlace(ToolContexts.custom(model, placeId, context.window(), null), placeId)
			.setVisible(true);
	}

}
