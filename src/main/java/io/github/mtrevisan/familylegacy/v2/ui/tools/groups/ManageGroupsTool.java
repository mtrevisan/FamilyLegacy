package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the group management dialog. */
public final class ManageGroupsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Groups…";
	}

	@Override
	public void run(final ToolContext context){
		new GroupManagementDialog(context).setVisible(true);
	}

}
