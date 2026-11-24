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
package io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance;

import io.github.mtrevisan.familylegacy.i18n.I18N;
import io.github.mtrevisan.familylegacy.io.model.readers.ExtractReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;


/**
 * "Explain This": why do we believe the selected record? It shows the chain assertion - citation - extract - source, the evidence for
 * and against, the competing assertions, the conclusions and the reasoning path (see
 * {@link io.github.mtrevisan.familylegacy.sources.EvidenceTrace}). For an individual it explains every fact about them; for a source
 * it opens the Source Workspace, the way back from the source. If nothing is selected, the record is asked for. The tool only reads
 * the model.
 */
public final class EvidenceTraceTool implements ToolOperation{

	@Override
	public UxLevel minimumLevel(){
		return UxLevel.STANDARD;
	}

	@Override
	public String getName(){
		return I18N.t("menu.source.evidence_trace");
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(final ToolContext context){
		FLEFRecord target = (context.hasSelectedEntity()? context.getSelectedEntity(): null);
		if(target == null){
			final RecordSelectionDialog dialog = RecordSelectionDialog.create(context.owner(), context.model(), (record, handler) -> {},
				IndividualHandler.class, GroupHandler.class, EventHandler.class, SourceHandler.class);
			dialog.setVisible(true);
			target = (dialog.isConfirmed()? dialog.getSelectedRecord(): null);
			if(target == null)
				return;
		}

		if(SourceHandler.isOfType(target))
			new SourceWorkspaceDialog(context.owner(), context.model(), target.getId())
				.setVisible(true);
		else if(ExtractReader.isOfType(target) && ExtractReader.extractSource(target) != null)
			new SourceWorkspaceDialog(context.owner(), context.model(), ExtractReader.extractSource(target))
				.setVisible(true);
		else
			EvidenceTraceDialog.open(context.owner(), context.model(), target.getId());
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
