package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the dialog for linking two existing individuals. */
public final class LinkExistingIndividualTool implements ToolOperation{

	@Override
	public String getName(){
		return "Link Existing Individual…";
	}

	@Override
	public void run(final ToolContext context){
		new LinkExistingIndividualDialog(context).setVisible(true);
	}

}
