package io.github.mtrevisan.familylegacy.v2.ui.tools.groups;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Group} menu.
 */
public final class GroupToolRegistry{

	private GroupToolRegistry(){
	}


	/** Primary CRUD tools, in menu order. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new ManageGroupsTool(),
			new NewGroupTool()
		);
	}

	/** Membership tools. */
	public static List<ToolOperation> membershipTools(){
		return List.of(
			new AddMemberTool(),
			new RemoveMemberTool()
		);
	}

	/** Advanced tools. */
	public static List<ToolOperation> advancedTools(){
		return List.of(
			new CreateFamilyFromSelectionTool(),
			new MergeGroupsTool()
		);
	}

}
