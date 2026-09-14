package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the source management dialog. */
public final class ManageSourcesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Sources…";
	}

	@Override
	public void run(final ToolContext context){
		new SourceManagementDialog(context).setVisible(true);
	}

}
