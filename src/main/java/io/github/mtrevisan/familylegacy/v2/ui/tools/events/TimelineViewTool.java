package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the timeline view dialog. */
public final class TimelineViewTool implements ToolOperation{

	@Override
	public String getName(){
		return "Timeline View…";
	}

	@Override
	public void run(final ToolContext context){
		new TimelineViewDialog(context).setVisible(true);
	}

}
