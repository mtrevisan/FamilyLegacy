/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.v2.ui.tools.individuals;

import io.github.mtrevisan.familylegacy.v2.ui.components.projections.repository.GenealogyRepository;
import io.github.mtrevisan.familylegacy.v2.ui.tools.ToolOperation;

import java.util.List;


/** Static catalogue of the operations exposed by the {@code Individual} menu. */
public final class IndividualToolRegistry{

	private IndividualToolRegistry(){}


	/** CRUD tools. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new EditIndividualTool(),
			new AddIndividualTool()
		);
	}

	/** Relationship tools. */
	public static List<ToolOperation> relationshipTools(){
		return List.of(
			new AddChildTool()
		);
	}

	/** CRUD tools. */
	public static List<ToolOperation> secondaryTools(){
		return List.of(
			new RelocateIndividualTool(),
			new DeleteIndividualTool()
		);
	}

	/** CRUD tools. */
	public static List<ToolOperation> tertiaryTools(){
		return List.of(
			new UnlinkRelationshipsTool()
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
	public static List<ToolOperation> navigationTools(final GenealogyRepository repository){
		return List.of(
			new SetAsRootTool(),
			new KinshipCalculatorTool(repository)
		);
	}

}
