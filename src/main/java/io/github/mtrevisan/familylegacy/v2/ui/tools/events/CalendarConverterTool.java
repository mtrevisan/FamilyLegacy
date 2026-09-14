package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolContext;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;


/** Opens the calendar converter dialog. */
public final class CalendarConverterTool implements ToolOperation{

	@Override
	public String getName(){
		return "Calendar Converter…";
	}

	@Override
	public void run(final ToolContext context){
		new CalendarConverterDialog(context).setVisible(true);
	}

}
