package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the research task management dialog. */
public final class ManageResearchTasksTool implements ToolOperation{

	@Override
	public String getName(){
		return "Research Tasks…";
	}

	@Override
	public void run(final ToolContext context){
		new ResearchTasksDialog(context).setVisible(true);
	}

}
