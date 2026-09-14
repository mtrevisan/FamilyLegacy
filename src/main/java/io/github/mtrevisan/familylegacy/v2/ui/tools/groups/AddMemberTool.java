package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "add member" dialog. */
public final class AddMemberTool implements ToolOperation{

	@Override
	public String getName(){
		return "Add Members…";
	}

	@Override
	public void run(final ToolContext context){
		new AddMemberDialog(context).setVisible(true);
	}

}
