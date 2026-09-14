package io.github.mtrevisan.familylegacy.v2.ui.tools.places;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the place relationship management dialog. */
public final class PlaceRelationshipsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Place Relationships…";
	}

	@Override
	public void run(final ToolContext context){
		new PlaceRelationshipsDialog(context).setVisible(true);
	}

}
