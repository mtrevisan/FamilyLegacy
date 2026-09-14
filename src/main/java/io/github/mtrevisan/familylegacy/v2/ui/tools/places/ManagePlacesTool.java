package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the place management dialog. */
public final class ManagePlacesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Places…";
	}

	@Override
	public void run(final ToolContext context){
		new PlaceManagementDialog(context).setVisible(true);
	}

}
