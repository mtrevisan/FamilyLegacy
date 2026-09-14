package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the participant management dialog. */
public final class ManageParticipantsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Participants…";
	}

	@Override
	public void run(final ToolContext context){
		new EventParticipantsDialog(context).setVisible(true);
	}

}
