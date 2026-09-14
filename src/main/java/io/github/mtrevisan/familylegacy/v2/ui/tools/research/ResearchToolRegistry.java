package io.github.mtrevisan.familylegacy.v2.ui.tools.research;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Research}
 * menu.
 */
public final class ResearchToolRegistry{

	private ResearchToolRegistry(){
	}


	/** Planning and execution tools, in menu order. */
	public static List<ToolOperation> planningTools(){
		return List.of(
			new ManageResearchQuestionsTool(),
			new ManageResearchActivitiesTool(),
			new ManageResearchTasksTool()
		);
	}

	/** Analysis tools. */
	public static List<ToolOperation> analysisTools(){
		return List.of(
			new ManageConclusionsTool(),
			new ManageIdentityHypothesesTool()
		);
	}

	/** Report tools. */
	public static List<ToolOperation> reportTools(){
		return List.of(
			new EvidenceQualifiersTool(),
			new ProofSummaryTool(),
			new ResearchLogTool()
		);
	}

}
