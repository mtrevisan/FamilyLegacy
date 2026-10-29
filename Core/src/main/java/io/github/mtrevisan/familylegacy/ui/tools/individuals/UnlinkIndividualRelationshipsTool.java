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
package io.github.mtrevisan.familylegacy.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship.UnlinkDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;

import java.util.List;


/**
 * Opens the "Unlink Relationships" dialog for the currently selected entity
 * and removes the selected relationship records from the model.
 */
public final class UnlinkIndividualRelationshipsTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.individual.unlink.relationships");
	}

	@Override
	public void run(final ToolContext context){
		final String recordId = context.selectedEntityId();

		final FLEFModel model = context.model();
		final UnlinkDialog dialog = new UnlinkDialog(context.owner(), model, recordId);
		dialog.setVisible(true);

		final List<String> relationshipIdsToRemove = dialog.getSelectedLinkIds();
		if(relationshipIdsToRemove.isEmpty())
			return;

		final ProjectionMutator mutator = context.mutator();
		if(mutator != null){
			mutator.removeRelationships(relationshipIdsToRemove);

			// Force a complete structural cache wipe and tree reload
			mutator.invalidateAndNotifyTreeChanged(recordId);
		}
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedIndividual());
	}

}
