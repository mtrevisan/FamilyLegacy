package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.v2.ui.tools.duplicates.DuplicateFinderDialog;


/** Opens the duplicate finder (already implemented in the {@code duplicates} package). */
public final class FindSimilarIndividualsTool implements ToolOperation{

	@Override
	public String getName(){
		return "Find Similar Individuals…";
	}

	@Override
	public void run(final ToolContext context){
		new DuplicateFinderDialog(context).setVisible(true);
	}

}
