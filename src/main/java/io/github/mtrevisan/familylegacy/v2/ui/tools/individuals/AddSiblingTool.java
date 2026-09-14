package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;


/** Opens the "add sibling" dialog for the currently selected individual. */
public final class AddSiblingTool implements ToolOperation{

	@Override
	public String getName(){
		return "Add Sibling…";
	}

	@Override
	public void run(final ToolContext context){
		final String id = context.selectedIndividualId();
		if(id == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No individual is selected.", "Add Sibling", JOptionPane.WARNING_MESSAGE);
			return;
		}
		new AddRelativeDialog(context, AddRelativeDialog.Role.SIBLING, id).setVisible(true);
	}

}
