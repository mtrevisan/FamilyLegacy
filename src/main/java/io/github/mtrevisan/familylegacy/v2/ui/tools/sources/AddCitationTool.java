package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "add citation" dialog. */
public final class AddCitationTool implements ToolOperation{

	@Override
	public String getName(){
		return "Add Citation…";
	}

	@Override
	public void run(final ToolContext context){
		new AddCitationDialog(context).setVisible(true);
	}

}
