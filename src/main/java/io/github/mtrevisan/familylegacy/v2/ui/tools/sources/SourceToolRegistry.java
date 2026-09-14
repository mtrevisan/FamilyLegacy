package io.github.mtrevisan.familylegacy.v2.ui.tools.sources;

import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Source} menu.
 * <p>
 * Adding a new tool means adding one line here and one class in this
 * package: the menu bar itself does not change.
 */
public final class SourceToolRegistry{

	private SourceToolRegistry(){
	}


	/** Sources subgroup, in menu order. */
	public static List<ToolOperation> sourceTools(){
		return List.of(
			new ManageSourcesTool(),
			new NewSourceTool()
		);
	}

	/** Repositories subgroup. */
	public static List<ToolOperation> repositoryTools(){
		return List.of(
			new ManageRepositoriesTool()
		);
	}

	/** Documents subgroup. */
	public static List<ToolOperation> documentTools(){
		return List.of(
			new ManageDocumentsTool(),
			new MediaManagerTool()
		);
	}

	/** Citation subgroup. */
	public static List<ToolOperation> citationTools(){
		return List.of(
			new AddCitationTool(),
			new ExtractFromImageTool(),
			new CitationAnalysisTool()
		);
	}

}
