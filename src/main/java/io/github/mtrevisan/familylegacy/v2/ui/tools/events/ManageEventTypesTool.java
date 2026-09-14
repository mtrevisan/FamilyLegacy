package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the event type catalogue dialog. */
public final class ManageEventTypesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Event Types…";
	}

	@Override
	public void run(final ToolContext context){
		new EventTypesDialog(context).setVisible(true);
	}

}
