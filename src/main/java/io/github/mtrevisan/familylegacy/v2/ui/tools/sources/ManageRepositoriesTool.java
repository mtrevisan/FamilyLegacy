package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the repository management dialog. */
public final class ManageRepositoriesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Repositories…";
	}

	@Override
	public void run(final ToolContext context){
		new RepositoryManagementDialog(context).setVisible(true);
	}

}
