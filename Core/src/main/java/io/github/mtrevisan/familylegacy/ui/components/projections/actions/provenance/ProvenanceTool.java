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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.components.searches.service.RecordSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.ui.handlers.SourceHandler;


/**
 * Shows the provenance of the selected record: where it comes from, citation by citation (source, locator, stance, extract, evidence,
 * repository). For a source it shows the other direction: everything that cites it. If nothing is selected, the record is asked for.
 * <p>
 * The tool only reads the model.
 */
public final class ProvenanceTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.source_citation.provenance");
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

		final ProvenanceDialog dialog = (SourceHandler.isOfType(target)
			? ProvenanceDialog.forSource(context.owner(), context.model(), target.getId())
			: ProvenanceDialog.forRecord(context.owner(), context.model(), target.getId()));
		dialog.setVisible(true);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasModel());
	}

}
