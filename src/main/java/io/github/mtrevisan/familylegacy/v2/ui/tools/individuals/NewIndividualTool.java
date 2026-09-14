package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.IndividualHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "new individual" dialog. */
public final class NewIndividualTool implements ToolOperation{

	@Override
	public String getName(){
		return "New Individual…";
	}

	@Override
	public void run(final ToolContext context){
		final BaseRecordDialog dialog = IndividualHandler.getInstance()
			.createNewDialog(context.owner(), context.model());
		dialog.setVisible(true);
	}

}
