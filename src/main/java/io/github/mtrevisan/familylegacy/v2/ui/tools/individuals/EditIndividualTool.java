package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/**
 * Opens the editor for the currently selected individual.
 * <p>
 * The tool does not have direct access to the selection: it delegates
 * to the enclosing frame's {@code editCurrentSelection}, which knows
 * the active projection and the currently selected entity.
 */
public final class EditIndividualTool implements ToolOperation{

	@Override
	public String getName(){
		return "Edit Individual…";
	}

	@Override
	public void run(final ToolContext context){
		context.editCurrentSelection();
	}

}
