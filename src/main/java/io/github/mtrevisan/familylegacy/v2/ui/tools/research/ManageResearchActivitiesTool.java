package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the research activity management dialog. */
public final class ManageResearchActivitiesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Research Activities…";
	}

	@Override
	public void run(final ToolContext context){
		new ResearchActivitiesDialog(context).setVisible(true);
	}

}
