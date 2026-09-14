package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the dialog for merging two individuals. */
public final class MergeIndividualsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Merge Individuals…";
	}

	@Override
	public void run(final ToolContext context){
		new MergeIndividualsDialog(context).setVisible(true);
	}

}
