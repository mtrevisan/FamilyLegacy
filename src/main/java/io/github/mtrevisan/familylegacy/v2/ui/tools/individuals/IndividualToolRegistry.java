package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/** Static catalogue of the operations exposed by the {@code Individual} menu. */
public final class IndividualToolRegistry{

	private IndividualToolRegistry(){
	}


	/** CRUD tools. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new NewIndividualTool(),
			new EditIndividualTool(),
			new DeleteIndividualTool()
		);
	}

	/** Relationship tools. */
	public static List<ToolOperation> relationshipTools(){
		return List.of(
			new AddParentTool(),
			new AddChildTool(),
			new AddSiblingTool(),
			new LinkExistingIndividualTool()
		);
	}

	/** Advanced tools. */
	public static List<ToolOperation> advancedTools(){
		return List.of(
			new MergeIndividualsTool(),
			new FindSimilarIndividualsTool()
		);
	}

	/** Navigation tools. */
	public static List<ToolOperation> navigationTools(){
		return List.of(
			new SetAsRootTool(),
			new KinshipCalculatorTool()
		);
	}

}
