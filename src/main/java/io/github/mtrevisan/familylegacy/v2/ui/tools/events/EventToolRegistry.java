package io.github.mtrevisan.familylegacy.v2.ui.tools.events;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Event} menu.
 */
public final class EventToolRegistry{

	private EventToolRegistry(){
	}


	/** Primary operations, in menu order. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new ManageEventsTool(),
			new NewEventTool()
		);
	}

	/** Reference-data tools. */
	public static List<ToolOperation> referenceTools(){
		return List.of(
			new ManageEventTypesTool(),
			new ManageParticipantsTool()
		);
	}

	/** Date tools and views. */
	public static List<ToolOperation> analysisTools(){
		return List.of(
			new CalendarConverterTool(),
			new DateCalculatorTool(),
			new TimelineViewTool()
		);
	}

}
