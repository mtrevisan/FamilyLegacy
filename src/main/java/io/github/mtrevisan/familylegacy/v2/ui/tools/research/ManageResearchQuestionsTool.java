package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the research question management dialog. */
public final class ManageResearchQuestionsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Research Questions…";
	}

	@Override
	public void run(final ToolContext context){
		new ResearchQuestionsDialog(context).setVisible(true);
	}

}
