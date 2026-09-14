package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the research log dialog. */
public final class ResearchLogTool implements ToolOperation{

	@Override
	public String getName(){
		return "Research Log…";
	}

	@Override
	public void run(final ToolContext context){
		new ResearchLogDialog(context).setVisible(true);
	}

}
