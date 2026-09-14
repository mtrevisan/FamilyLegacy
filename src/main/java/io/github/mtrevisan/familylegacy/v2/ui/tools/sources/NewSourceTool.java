package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.SourceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "new source" dialog. */
public final class NewSourceTool implements ToolOperation{

	@Override
	public String getName(){
		return "New Source…";
	}

	@Override
	public void run(final ToolContext context){
		final BaseRecordDialog dialog = SourceHandler.getInstance()
			.createNewDialog(context.owner(), context.model());
		dialog.setVisible(true);
	}

}
