package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "remove member" dialog. */
public final class RemoveMemberTool implements ToolOperation{

	@Override
	public String getName(){
		return "Remove Members…";
	}

	@Override
	public void run(final ToolContext context){
		new RemoveMemberDialog(context).setVisible(true);
	}

}
