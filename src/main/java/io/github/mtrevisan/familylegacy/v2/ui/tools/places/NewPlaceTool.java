package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.BaseRecordDialog;
import io.github.mtrevisan.familylegacy.v2.ui.handlers.PlaceHandler;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the "new place" dialog. */
public final class NewPlaceTool implements ToolOperation{

	@Override
	public String getName(){
		return "New Place…";
	}

	@Override
	public void run(final ToolContext context){
		final BaseRecordDialog dialog = PlaceHandler.getInstance()
			.createNewDialog(context.owner(), context.model());
		dialog.setVisible(true);
	}

}
