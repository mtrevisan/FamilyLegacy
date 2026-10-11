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
package io.github.mtrevisan.familylegacy.ui.tools.groups;

import io.github.mtrevisan.familylegacy.ui.tools.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.individuals.AddChildTool;
import io.github.mtrevisan.familylegacy.ui.tools.individuals.EditIndividualTool;
import io.github.mtrevisan.familylegacy.ui.tools.individuals.UnlinkRelationshipsTool;

import java.util.List;


/** Static catalogue of the operations exposed by the {@code Group} menu. */
public final class GroupToolRegistry{

	private GroupToolRegistry(){}


	/** Primary CRUD tools, in menu order. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new EditGroupTool(),
			new AddGroupTool()
		);
	}

	/** Relationship tools. */
	public static List<ToolOperation> relationshipTools(){
		return List.of(
			new AddMemberTool(),
			new RemoveMemberTool()
		);
	}

	/** CRUD tools. */
	public static List<ToolOperation> secondaryTools(){
		return List.of(
			new UnlinkRelationshipsTool()
		);
	}

	/** Advanced tools. */
	public static List<ToolOperation> advancedTools(){
		return List.of(
			new MergeGroupsTool()
		);
	}

	/** Navigation tools. */
	public static List<ToolOperation> navigationTools(){
		return List.of(
			new SetAsRootTool()
		);
	}

}
