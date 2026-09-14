package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the date calculator dialog. */
public final class DateCalculatorTool implements ToolOperation{

	@Override
	public String getName(){
		return "Date Calculator…";
	}

	@Override
	public void run(final ToolContext context){
		new DateCalculatorDialog(context).setVisible(true);
	}

}
