package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "create family from selection" dialog. */
public final class CreateFamilyFromSelectionTool implements ToolOperation{

	@Override
	public String getName(){
		return "Create Family From Selection…";
	}

	@Override
	public void run(final ToolContext context){
		new CreateFamilyFromSelectionDialog(context).setVisible(true);
	}

}
