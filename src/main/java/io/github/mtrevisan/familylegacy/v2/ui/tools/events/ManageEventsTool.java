package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the event management dialog. */
public final class ManageEventsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Events…";
	}

	@Override
	public void run(final ToolContext context){
		new EventManagementDialog(context).setVisible(true);
	}

}
