package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "merge groups" dialog. */
public final class MergeGroupsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Merge Groups…";
	}

	@Override
	public void run(final ToolContext context){
		new MergeGroupsDialog(context).setVisible(true);
	}

}
