package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the document management dialog. */
public final class ManageDocumentsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Manage Documents…";
	}

	@Override
	public void run(final ToolContext context){
		new DocumentManagementDialog(context).setVisible(true);
	}

}
