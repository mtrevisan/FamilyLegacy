package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.TreeService;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.individualtree.services.kinship.KinshipDialog;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/**
 * Opens the kinship calculator with the currently selected individual
 * pre-filled as the first side of the comparison.
 * <p>
 * The kinship dialog lives in the {@code individualtree} package and
 * needs a {@code TreeService}. The service is created on demand, with
 * the same relationship filter the tree uses, so the calculator sees the
 * same graph the tree does.
 */
public final class KinshipCalculatorTool implements ToolOperation{

	@Override
	public String getName(){
		return "Kinship Calculator…";
	}

	@Override
	public void run(final ToolContext context){
		final String selected = context.selectedIndividualId();
		final TreeService treeService = new TreeService(
			type -> "biological_child".equalsIgnoreCase(type),
			context.model());
		final io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord initialA =
			(selected != null? context.model().getRecordById(selected): null);
		final KinshipDialog dialog = new KinshipDialog(context.owner(),
			context.model(), treeService, initialA, null);
		dialog.setVisible(true);
	}

}
