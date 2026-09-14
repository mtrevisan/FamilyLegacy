package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the conclusion management dialog. */
public final class ManageConclusionsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Conclusions…";
	}

	@Override
	public void run(final ToolContext context){
		new ConclusionsDialog(context).setVisible(true);
	}

}
