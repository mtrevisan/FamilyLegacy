package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;
import java.util.List;


/**
 * Deletes the currently selected individual, together with every
 * relationship that involves it. The deletion is confirmed with a
 * dialog that lists how many relationships will be removed.
 */
public final class DeleteIndividualTool implements ToolOperation{

	@Override
	public String getName(){
		return "Delete Individual…";
	}

	@Override
	public void run(final ToolContext context){
		final String id = context.selectedIndividualId();
		if(id == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No individual is selected.",
				"Delete Individual", JOptionPane.WARNING_MESSAGE);
			return;
		}

		final FLEFRecord individual = context.model().getRecordById(id);
		if(individual == null)
			return;

		final List<String> relIds = IndividualHelper.relationshipIdsForIndividual(
			context.model(), id);
		final String message = "Delete individual "
			+ IndividualHelper.displayName(individual) + " [" + id + "]?\n"
			+ relIds.size() + " relationship(s) will also be removed.";
		final int confirm = JOptionPane.showConfirmDialog(context.owner(), message,
			"Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(confirm != JOptionPane.YES_OPTION)
			return;

		IndividualHelper.deleteIndividualCascade(context.model(), id);
	}

}
