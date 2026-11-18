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
package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;


class ToolGroupsByLevelTest{

	private static ToolOperation tool(final String name, final UxLevel level){
		return new ToolOperation(){
			@Override
			public String getName(){
				return name;
			}

			@Override
			public UxLevel minimumLevel(){
				return level;
			}

			@Override
			public void run(final ToolContext context){}
		};
	}

	private static List<List<String>> names(final List<List<ToolOperation>> groups){
		return groups.stream()
			.map(group -> group.stream().map(ToolOperation::getName).toList())
			.toList();
	}

	private static final List<List<ToolOperation>> GROUPS = List.of(
		List.of(tool("a", UxLevel.BASIC), tool("b", UxLevel.BASIC)),
		List.of(tool("c", UxLevel.STANDARD)),
		List.of(tool("d", UxLevel.ADVANCED), tool("e", UxLevel.ADVANCED)));

	@Test
	void emptyGroupsAreDroppedAndTheOthersKeepTheirTools(){
		final FeatureVisibilityService basic = FeatureVisibilityService.inMemory(UxLevel.BASIC);
		Assertions.assertEquals(List.of(List.of("a", "b")), names(ApplicationMenuBar.visibleGroups(GROUPS, basic, false)));
		Assertions.assertEquals(List.of(List.of("a", "b"), List.of("c")),
			names(ApplicationMenuBar.visibleGroups(GROUPS, FeatureVisibilityService.inMemory(UxLevel.STANDARD), false)));
	}

	@Test
	void aGroupWithOneToolLeftJoinsThePreviousOneWhenAskedTo(){
		Assertions.assertEquals(List.of(List.of("a", "b", "c")),
			names(ApplicationMenuBar.visibleGroups(GROUPS, FeatureVisibilityService.inMemory(UxLevel.STANDARD), true)));
		Assertions.assertEquals(List.of(List.of("a", "b", "c"), List.of("d", "e")),
			names(ApplicationMenuBar.visibleGroups(GROUPS, FeatureVisibilityService.inMemory(UxLevel.ADVANCED), true)));
	}

	@Test
	void theResearchMenuHasNoToolAloneBetweenSeparatorsAtAnyLevel(){
		final List<List<ToolOperation>> research = List.of(ResearchToolRegistry.planningTools(), ResearchToolRegistry.analysisTools(),
			ResearchToolRegistry.contextTools(), ResearchToolRegistry.reportTools());

		Assertions.assertTrue(ApplicationMenuBar.visibleGroups(research, FeatureVisibilityService.inMemory(UxLevel.BASIC), true).isEmpty());
		for(final UxLevel level : List.of(UxLevel.STANDARD, UxLevel.ADVANCED))
			for(final List<ToolOperation> group : ApplicationMenuBar.visibleGroups(research, FeatureVisibilityService.inMemory(level), true))
				Assertions.assertTrue(group.size() > 1, level + StringUtils.SPACE + names(List.of(group)));
	}

}
