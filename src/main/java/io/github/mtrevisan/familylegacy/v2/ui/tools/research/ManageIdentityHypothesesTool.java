package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the identity hypothesis management dialog. */
public final class ManageIdentityHypothesesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Identity Hypotheses…";
	}

	@Override
	public void run(final ToolContext context){
		new IdentityHypothesesDialog(context).setVisible(true);
	}

}
