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
package io.github.mtrevisan.familylegacy.ui.tools.research;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.ConclusionReader;
import io.github.mtrevisan.familylegacy.io.model.readers.ResearchQuestionReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;


/**
 * The Genealogical Proof Standard workflow: research question, evidence, conflicts, conclusion and proof statement, and what is still
 * missing before the conclusion can be considered reasonably proven (see {@link GpsWorkflowDialog}). It starts from the selected research
 * question or conclusion, if there is one. The tool does not modify the model, except for the note the user asks to save.
 */
public final class GpsWorkflowTool implements ToolOperation{

	@Override
	public UxLevel minimumLevel(){
		return UxLevel.STANDARD;
	}

	@Override
	public String getName(){
		return I18N.t("menu.research.gps_workflow");
	}

	@Override
	public void run(final ToolContext context){
		final FLEFRecord selected = (context.selectedEntityId() != null? context.model().getRecordById(context.selectedEntityId()): null);
		final boolean usable = ((ResearchQuestionReader.isOfType(selected) || ConclusionReader.isOfType(selected)));
		new GpsWorkflowDialog(context.owner(), context.model(), (usable? selected: null))
			.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
