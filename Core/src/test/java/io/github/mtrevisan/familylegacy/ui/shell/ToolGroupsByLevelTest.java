package io.github.mtrevisan.familylegacy.ui.shell;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolContext;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.features.FeatureVisibilityService;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;
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
				Assertions.assertTrue(group.size() > 1, level + " " + names(List.of(group)));
	}

}
