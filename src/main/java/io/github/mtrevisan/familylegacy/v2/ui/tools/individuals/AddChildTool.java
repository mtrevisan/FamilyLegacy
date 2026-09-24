package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.relationship.RelationshipTypeSelectionDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.RelationshipHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;


/**
 * Opens the "add child" workflow for the currently selected individual.
 */
public final class AddChildTool implements ToolOperation{

	@Override
	public String getName(){
		return "Add Child…";
	}

	@Override
	public void run(final ToolContext context){
		final String targetId = context.selectedEntityId();
		if(targetId == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No individual is selected.", "Add Child", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final FLEFModel model = context.model();
		final Window owner = context.owner();

		final var dialog = IndividualHandler.getInstance()
			.createNewDialog(owner, model);
		dialog.setVisible(true);

		if(!dialog.isSaved() || dialog.getRecord() == null)
			return;

		final FLEFRecord childRecord = dialog.getRecord();
		executeAddChildWorkflow(owner, model, targetId, childRecord);
	}

	public static void executeAddChildWorkflow(final Window owner, final FLEFModel model,
		final String targetId, final FLEFRecord childRecord){
		if(childRecord == null || childRecord.getId() == null)
			return;

		final String childId = childRecord.getId();
		if(childId.equals(targetId)){
			JOptionPane.showMessageDialog(owner,
				"The two individuals must be different.",
				"Add Child", JOptionPane.WARNING_MESSAGE);

			return;
		}

		final String partnerId = IndividualHelper.firstSpouseId(model, targetId);

		final List<RelationshipTypeSelectionDialog.Item> items = new ArrayList<>();

		final FLEFRecord targetRecord = model.getRecordById(targetId);
		final String targetLabel = (targetRecord != null
			? IndividualHelper.displayName(targetRecord) + " [" + targetId + "]"
			: targetId);
		items.add(new RelationshipTypeSelectionDialog.Item(targetId, targetLabel,
			IndividualHelper.CHILD_RELATION_TYPES.getFirst()));

		if(partnerId != null){
			final FLEFRecord partnerRecord = model.getRecordById(partnerId);
			final String partnerLabel = (partnerRecord != null
				? IndividualHelper.displayName(partnerRecord) + " [" + partnerId + "]"
				: partnerId);
			items.add(new RelationshipTypeSelectionDialog.Item(partnerId, partnerLabel,
				IndividualHelper.CHILD_RELATION_TYPES.getFirst()));
		}


		final List<String> selectedTypes = RelationshipTypeSelectionDialog.selectRelationshipType(
			owner, items, IndividualHelper.CHILD_RELATION_TYPES.toArray(String[]::new), null, model);

		if(selectedTypes == null || selectedTypes.isEmpty())
			return;

		IndividualHelper.createRelationship(model, childId, targetId, selectedTypes.get(0),
			RelationshipHandler.ID_PREFIX);

		if(partnerId != null && selectedTypes.size() > 1)
			IndividualHelper.createRelationship(model, childId, partnerId, selectedTypes.get(1),
				RelationshipHandler.ID_PREFIX);
	}

	@Override
	public boolean isEnabled(final ToolContext context){
		return (context != null && context.hasSelectedEntity());
	}

}
