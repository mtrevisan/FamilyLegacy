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
package io.github.mtrevisan.familylegacy.ui.tools.groups;

import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.readers.GroupReader;
import io.github.mtrevisan.familylegacy.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.ui.components.projections.repository.ProjectionMutator;
import io.github.mtrevisan.familylegacy.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.ui.i18n.I18N;
import io.github.mtrevisan.familylegacy.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Opens the "add member" workflow for the currently selected group.
 */
public final class AddMemberTool implements ToolOperation{

	@Override
	public String getName(){
		return I18N.t("menu.group.add.members");
	}

	@Override
	public void run(final ToolContext context){
		final Window owner = context.owner();
		final FLEFModel model = context.model();
		final var dialog = GroupHandler.getInstance()
			.createNewDialog(owner, model);
		dialog.setVisible(true);

		if(!dialog.isSaved())
			return;

		final FLEFRecord memberRecord = dialog.getRecord();
		if(memberRecord == null)
			return;

		final Component sourcePanel = context.selectedComponent();
		final String targetId = context.selectedEntityId();
		final ProjectionMutator mutator = context.mutator();
		executeAddMemberWorkflow(owner, model, sourcePanel, targetId, memberRecord, mutator);
	}

	public static void executeAddMemberWorkflow(final Window owner, final FLEFModel model,
			final Component sourcePanel, final String targetId, final FLEFRecord memberRecord,
			final ProjectionMutator mutator){
		if(memberRecord == null || memberRecord.getId() == null)
			return;

		final String memberId = memberRecord.getId();
		if(memberId.equals(targetId)){
			JOptionPane.showMessageDialog(owner,
				I18N.t("confirmation.add.member.message"),
				I18N.t("confirmation.add.member.title"), JOptionPane.WARNING_MESSAGE);

			return;
		}

//		final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>();
//		final FLEFRecord targetRecord = model.getRecordById(targetId);
//		final String targetLabel = (targetRecord != null
//			? GroupReader.extractPrimaryFullname(targetRecord) + " [" + targetId + "]"
//			: targetId);
//		items.add(new RelationshipTypeSelectionDialog.Item(targetId, targetLabel,
//			RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
//
//		final String partnerId = findShownPartner(sourcePanel, targetId);
//		if(partnerId != null){
//			final FLEFRecord partnerRecord = model.getRecordById(partnerId);
//			final String partnerLabel = (partnerRecord != null
//				? GroupReader.extractPrimaryFullname(partnerRecord) + " [" + partnerId + "]"
//				: partnerId);
//			items.add(new RelationshipTypeSelectionDialog.Item(partnerId, partnerLabel,
//				RelationshipReader.ENUM_TYPE_BIOLOGICAL_CHILD));
//		}
//
//		final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(
//			owner, items, RelationshipReader.FAMILY, null, model);
//		if(selectedTypes == null || selectedTypes.isEmpty())
//			return;
//
//		// Route the mutation through the projection mutator, so that the
//		// shared repository and the tree service caches are invalidated
//		// (notifyRelationshipAdded) and the incremental update path is
//		// exercised. Writing through the model directly would leave the
//		// caches stale and the tree would keep showing the old structure.
//		final String firstType = selectedTypes.get(0);
//		if(mutator != null)
//			mutator.createRelationship(memberId, targetId, firstType);
//		else
//			GroupHelper.createRelationship(memberId, targetId, firstType, RelationshipHandler.ID_PREFIX, model);
//
//		if(partnerId != null && selectedTypes.size() > 1){
//			final String secondType = selectedTypes.get(1);
//			mutator.createRelationship(memberId, partnerId, secondType);
//		}
//
//		// Force the tree to rebuild from the updated model. The focus id is
//		// the target that the user acted on; passing it keeps the current
//		// root and scroll position stable.
//		mutator.invalidateAndNotifyTreeChanged(targetId);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedGroup());
	}

}
