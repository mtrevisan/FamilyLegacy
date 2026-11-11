package io.github.mtrevisan.familylegacy.ui.features;

import io.github.mtrevisan.familylegacy.preferences.UxLevel;
import io.github.mtrevisan.familylegacy.ui.assistant.AssistantAction;
import io.github.mtrevisan.familylegacy.ui.assistant.features.ResearchToolsAssistantContributor;
import io.github.mtrevisan.familylegacy.ui.components.projections.actions.ToolOperation;
import io.github.mtrevisan.familylegacy.ui.tools.research.ResearchToolRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


class FeatureVisibilityServiceTest{

	@Test
	void levelsIncludeThePreviousOnes(){
		Assertions.assertTrue(UxLevel.ADVANCED.includes(UxLevel.BASIC));
		Assertions.assertTrue(UxLevel.ADVANCED.includes(UxLevel.STANDARD));
		Assertions.assertTrue(UxLevel.STANDARD.includes(UxLevel.STANDARD));
		Assertions.assertFalse(UxLevel.STANDARD.includes(UxLevel.ADVANCED));
		Assertions.assertFalse(UxLevel.BASIC.includes(UxLevel.STANDARD));
	}

	@Test
	void registeredFeatureFollowsItsMinimumLevel(){
		final FeatureVisibilityService service = FeatureVisibilityService.inMemory(UxLevel.BASIC);
		service.register("conclusion.editor", UxLevel.ADVANCED);

		Assertions.assertFalse(service.isVisible("conclusion.editor"));
		service.setLevel(UxLevel.STANDARD);
		Assertions.assertFalse(service.isVisible("conclusion.editor"));
		service.setLevel(UxLevel.ADVANCED);
		Assertions.assertTrue(service.isVisible("conclusion.editor"));
	}

	@Test
	void unregisteredFeatureIsAlwaysVisible(){
		Assertions.assertTrue(FeatureVisibilityService.inMemory(UxLevel.BASIC).isVisible("unknown"));
	}

	@Test
	void changingTheLevelNotifiesAndSavesOnlyWhenItChanges(){
		final List<UxLevel> saved = new ArrayList<>();
		final FeatureVisibilityService service = new FeatureVisibilityService(() -> UxLevel.BASIC, saved::add);
		final AtomicInteger notified = new AtomicInteger();
		service.addListener(notified::incrementAndGet);

		service.setLevel(UxLevel.BASIC);
		Assertions.assertEquals(0, notified.get());

		service.setLevel(UxLevel.STANDARD);
		Assertions.assertEquals(1, notified.get());
		Assertions.assertEquals(List.of(UxLevel.STANDARD), saved);
	}

	@Test
	void researchToolsAreHiddenInBasicAndEveryOneIsReachableFromTheAssistant(){
		final List<ToolOperation> tools = new ArrayList<>();
		tools.addAll(ResearchToolRegistry.planningTools());
		tools.addAll(ResearchToolRegistry.contextTools());
		tools.addAll(ResearchToolRegistry.analysisTools());
		tools.addAll(ResearchToolRegistry.reportTools());

		for(final ToolOperation tool : tools)
			Assertions.assertTrue(tool.minimumLevel() != UxLevel.BASIC, tool.getClass().getSimpleName());
		Assertions.assertEquals(UxLevel.ADVANCED, new io.github.mtrevisan.familylegacy.ui.tools.research.ManageConclusionsTool().minimumLevel());
		Assertions.assertEquals(UxLevel.STANDARD, new io.github.mtrevisan.familylegacy.ui.tools.research.ManageResearchQuestionsTool().minimumLevel());

		final List<AssistantAction> actions = new ResearchToolsAssistantContributor().actions();
		Assertions.assertEquals(tools.size(), actions.size());
		Assertions.assertEquals(tools.size(), actions.stream().map(AssistantAction::id).distinct().count());
	}


	@Test
	void theToolsOfTheEvidenceAndTheVocabularyAreFromTheStandardLevel(){
		Assertions.assertEquals(UxLevel.STANDARD,
			new io.github.mtrevisan.familylegacy.ui.components.projections.actions.provenance.ProvenanceTool().minimumLevel());
		Assertions.assertEquals(UxLevel.STANDARD, new io.github.mtrevisan.familylegacy.ui.tools.events.ManageEventTypesTool().minimumLevel());
		//the ordinary genealogy tools stay in the basic menus
		Assertions.assertEquals(UxLevel.STANDARD, new io.github.mtrevisan.familylegacy.ui.tools.sources.CitationAnalysisTool().minimumLevel());
		Assertions.assertEquals(UxLevel.BASIC, new io.github.mtrevisan.familylegacy.ui.tools.events.ManageParticipantsTool().minimumLevel());
	}

}
