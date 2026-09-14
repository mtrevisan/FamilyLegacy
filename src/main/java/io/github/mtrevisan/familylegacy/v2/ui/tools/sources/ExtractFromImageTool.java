package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the extract-from-image dialog. */
public final class ExtractFromImageTool implements ToolOperation{

	@Override
	public String getName(){
		return "Extract from Image…";
	}

	@Override
	public void run(final ToolContext context){
		new ExtractFromImageDialog(context).setVisible(true);
	}

}
