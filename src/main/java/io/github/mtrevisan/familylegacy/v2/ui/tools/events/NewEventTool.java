package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.EventHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "new event" dialog. */
public final class NewEventTool implements ToolOperation{

	@Override
	public String getName(){
		return "New Event…";
	}

	@Override
	public void run(final ToolContext context){
		final BaseRecordDialog dialog = EventHandler.getInstance()
			.createNewDialog(context.owner(), context.model());
		dialog.setVisible(true);
	}

}
