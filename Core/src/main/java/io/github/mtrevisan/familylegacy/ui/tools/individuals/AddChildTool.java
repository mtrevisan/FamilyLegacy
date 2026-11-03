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
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.ui.dialogs.PanelKey;
import io.github.mtrevisan.familylegacy.ui.dialogs.records.IndividualRecordDialog;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;

import java.awt.Window;


/**
 * Opens the editor of the currently selected individual on the relationships it has as a parent, where the child is added.
 */
public final class AddChildTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.individual.add.child");
	}

	@Override
	public void run(final ToolContext context){
		final Window owner = context.owner();
		final FLEFModel model = context.model();
		final FLEFRecord record = context.getSelectedEntity();
		final IndividualRecordDialog dialog = IndividualRecordDialog
			.createEdit(owner, model, record);
		dialog.showSelectedTabAndComponent(BaseRecordDialog.TAB_NAME_RELATIONSHIPS, PanelKey.RELATIONSHIP_ON_OBJECT);
		dialog.setVisible(true);

		if(!dialog.isSaved())
			return;

		//the relationships were saved by the editor (the child is added in the "Inverse Relationships" panel): the tree has only
		//to be rebuilt from the updated model, keeping the current root and scroll position
		final ProjectionMutator mutator = context.mutator();
		if(mutator != null)
			mutator.invalidateAndNotifyTreeChanged(context.selectedEntityId());
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedIndividual());
	}

}
