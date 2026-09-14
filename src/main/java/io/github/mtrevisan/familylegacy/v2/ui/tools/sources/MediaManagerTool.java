package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the media manager dialog. */
public final class MediaManagerTool implements ToolOperation{

	@Override
	public String getName(){
		return "Media Manager…";
	}

	@Override
	public void run(final ToolContext context){
		new MediaManagerDialog(context).setVisible(true);
	}

}
