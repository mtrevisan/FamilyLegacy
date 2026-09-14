package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import javax.swing.JOptionPane;


/** Sets the currently selected individual as the root of the active projection. */
public final class SetAsRootTool implements ToolOperation{

	@Override
	public String getName(){
		return "Set as Root";
	}

	@Override
	public void run(final ToolContext context){
		final String id = context.selectedIndividualId();
		if(id == null){
			JOptionPane.showMessageDialog(context.owner(),
				"No individual is selected.",
				"Set as Root", JOptionPane.WARNING_MESSAGE);
			return;
		}
		context.loadRoot(id);
	}

}
