package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.GroupHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "new group" dialog. */
public final class NewGroupTool implements ToolOperation{

	@Override
	public String getName(){
		return "New Group…";
	}

	@Override
	public void run(final ToolContext context){
		final BaseRecordDialog dialog = GroupHandler.getInstance()
			.createNewDialog(context.owner(), context.model());
		dialog.setVisible(true);
	}

}
