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
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.UnlinkRelationshipsDialog;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.ProjectionMutator;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.Set;


/**
 * Opens the "Unlink Relationships" dialog for the currently selected entity
 * and removes the selected relationship records from the model.
 */
public final class UnlinkRelationshipsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Unlink Relationships…";
	}

	@Override
	public void run(final ToolContext context){
		if(context == null)
			return;

		final String id = context.selectedEntityId();
		if(id == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No entity is selected.", "Unlink Relationships", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final FLEFModel model = context.model();
		final FLEFRecord record = model.getRecordById(id);
		if(record == null)
			return;

		final Set<String> parentIds = IndividualHelper.parentIds(id, model);
		final Set<String> associateIds = IndividualHelper.associateIds(id, model);
		final Set<String> groupIds = IndividualHelper.groupIds(id, model);
		final Set<String> childIds = IndividualHelper.childIds(id, model);

		final UnlinkRelationshipsDialog dialog = new UnlinkRelationshipsDialog(
			context.owner(), model, id, parentIds, associateIds, groupIds, childIds);
		dialog.setVisible(true);

		final List<String> relationshipIdsToRemove = dialog.getSelectedRelationshipIds();
		if(relationshipIdsToRemove == null || relationshipIdsToRemove.isEmpty())
			return;

		final ProjectionMutator mutator = context.mutator();
		if(mutator != null){
			mutator.removeRelationships(relationshipIdsToRemove);

			// Force a complete structural cache wipe and tree reload
			mutator.invalidateAndNotifyTreeChanged(id);
		}
		else{
			for(final String relId : relationshipIdsToRemove)
				model.removeRecord(relId);

			context.loadRoot(id);
		}
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedEntity());
	}

}
