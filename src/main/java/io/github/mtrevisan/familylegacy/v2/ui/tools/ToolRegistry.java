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
package io.github.mtrevisan.familylegacy.v2.ui.tools;

import io.github.mtrevisan.familylegacy.v2.ui.tools.compare.CompareFilesTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.consistency.CheckConsistencyTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.duplicates.FindDuplicatesTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.placeholder.PlaceholderTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.statistics.StatisticsTool;
import io.github.mtrevisan.familylegacy.v2.ui.tools.validate.ValidateFileTool;

import java.util.List;


/**
 * Static catalogue of the operations exposed by the {@code Tools} menu.
 * <p>
 * The registry is the single point that knows about every tool. Adding
 * a new tool means adding one line here and one class in the appropriate
 * sub-package: the menu bar itself does not change.
 * <p>
 * The order of the returned list is the order in which the tools appear
 * in the menu, so the entries are grouped by kind rather than
 * alphabetically.
 */
public final class ToolRegistry{

	private ToolRegistry(){
	}


	/** Tools that perform real work, in menu order. */
	public static List<ToolOperation> primaryTools(){
		return List.of(
			new ValidateFileTool(),
			new CheckConsistencyTool(),
			new FindDuplicatesTool(),
			new StatisticsTool(),
			new CompareFilesTool()
		);
	}

	/**
	 * Tools that only reserve their place in the menu, in menu order.
	 * Each name matches a planned feature; the tool shows a "not
	 * implemented yet" dialog when invoked.
	 */
	public static List<ToolOperation> reportPlaceholders(){
		return List.of(
			new PlaceholderTool("Ancestor Report…"),
			new PlaceholderTool("Descendant Report…"),
			new PlaceholderTool("Family Group Sheet…"),
			new PlaceholderTool("Individual Summary…"),
			new PlaceholderTool("Relationship Report…"),
			new PlaceholderTool("Bibliography…"),
			new PlaceholderTool("Research Progress…")
		);
	}

	public static List<ToolOperation> chartPlaceholders(){
		return List.of(
			new PlaceholderTool("Ancestor Chart…"),
			new PlaceholderTool("Descendant Chart…"),
			new PlaceholderTool("Hourglass Chart…"),
			new PlaceholderTool("Fan Chart…"),
			new PlaceholderTool("Bowtie Chart…"),
			new PlaceholderTool("Relationship Chart…"),
			new PlaceholderTool("Map Chart…")
		);
	}

	public static List<ToolOperation> maintenancePlaceholders(){
		return List.of(
			new PlaceholderTool("Data Cleanup…"),
			new PlaceholderTool("Recompute Derived Data"),
			new PlaceholderTool("Backup…"),
			new PlaceholderTool("Restore from Backup…"),
			new PlaceholderTool("Plugins…")
		);
	}

}
