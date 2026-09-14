package io.github.mtrevisan.familylegacy.v2.ui.tools.duplicates;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the duplicate finder dialog. */
public final class FindDuplicatesTool implements ToolOperation{

	@Override
	public String getName(){
		return "Find Similar Individuals…";
	}

	@Override
	public void run(final ToolContext context){
		new DuplicateFinderDialog(context).setVisible(true);
	}

}
