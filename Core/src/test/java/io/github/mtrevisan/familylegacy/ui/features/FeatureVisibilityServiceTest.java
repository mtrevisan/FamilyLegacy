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
